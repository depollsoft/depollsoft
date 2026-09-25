//
//  TMBalanceAudioPlayer.swift
//  tagmaster
//
//  Plays a decoded learning track with an independent gain on each channel.
//  This is a balance control, not a pan: the slider only lowers one side, so a
//  part recorded on the left never migrates into the right ear.
//

import AVFoundation
import os

/// Stereo gains for a balance in 0...1 (0 = left only, 0.5 = both, 1 = right only).
/// The louder side always stays at unity, matching the Android MediaPlayerView.
@objc final class TMBalanceGains: NSObject {
    @objc let left: Float
    @objc let right: Float

    @objc init(balance: Float) {
        let clamped = max(0, min(1, balance.isFinite ? balance : 0.5))
        var right = clamped
        var left = 1 - clamped
        let loudest = max(left, right)
        left /= loudest
        right /= loudest
        self.left = left
        self.right = right
        super.init()
    }
}

/// Mutable state shared between the main thread and the render thread.
///
/// The render thread never blocks: it takes the lock with a try-lock and emits
/// silence for that quantum if the main thread happens to hold it. The lock is
/// `OSAllocatedUnfairLock`, so its storage has a stable address.
private final class TMRenderState {
    private struct Shared {
        var buffer: AVAudioPCMBuffer?
        var position = 0
        var leftGain: Float = 1
        var rightGain: Float = 1
        var playing = false
        var reachedEnd = false
        var frameLength: Int { Int(buffer?.frameLength ?? 0) }
    }

    private let lock = OSAllocatedUnfairLock(initialState: Shared())

    var frameLength: Int { lock.withLock { $0.frameLength } }

    var currentFrame: Int {
        get { lock.withLock { $0.position } }
        set {
            lock.withLock {
                $0.position = max(0, min(newValue, $0.frameLength))
                $0.reachedEnd = false
            }
        }
    }

    var isPlaying: Bool {
        get { lock.withLock { $0.playing } }
        set { lock.withLock { $0.playing = newValue } }
    }

    var hasReachedEnd: Bool { lock.withLock { $0.reachedEnd } }

    func setGains(_ gains: TMBalanceGains) {
        lock.withLock {
            $0.leftGain = gains.left
            $0.rightGain = gains.right
        }
    }

    func load(_ buffer: AVAudioPCMBuffer?) {
        lock.withLock {
            $0.buffer = buffer
            $0.position = 0
            $0.playing = false
            $0.reachedEnd = false
        }
    }

    /// Fills `abl` with the next `frameCount` frames. Silence when paused, past the end,
    /// or when the lock is momentarily unavailable.
    func render(frameCount: AVAudioFrameCount, into abl: UnsafeMutablePointer<AudioBufferList>) {
        let out = UnsafeMutableAudioBufferListPointer(abl)
        let count = Int(frameCount)
        guard out.count >= 1, let outLeft = out[0].mData?.assumingMemoryBound(to: Float.self) else { return }
        let outRight = out.count > 1 ? out[1].mData?.assumingMemoryBound(to: Float.self) : nil

        // Claim the frames under the lock; copy samples outside it. The buffer is
        // immutable once loaded and stays alive while any render holds a reference.
        let claim: (buffer: AVAudioPCMBuffer?, start: Int, available: Int, left: Float, right: Float)? =
            lock.withLockIfAvailable { shared in
                let active = shared.playing && shared.buffer != nil
                let total = shared.frameLength
                let available = active ? max(0, min(count, total - shared.position)) : 0
                let start = shared.position
                if active {
                    shared.position = start + available
                    if shared.position >= total { shared.reachedEnd = true }
                }
                return (shared.buffer, start, available, shared.leftGain, shared.rightGain)
            }

        let available = claim?.available ?? 0
        if let claim, available > 0, let buffer = claim.buffer, let channels = buffer.floatChannelData {
            let inLeft = channels[0] + claim.start
            let inRight = (buffer.format.channelCount > 1 ? channels[1] : channels[0]) + claim.start
            let l = claim.left, r = claim.right
            for i in 0..<available {
                outLeft[i] = inLeft[i] * l
                outRight?[i] = inRight[i] * r
            }
        }
        if available < count {
            (outLeft + available).update(repeating: 0, count: count - available)
            outRight.map { ($0 + available).update(repeating: 0, count: count - available) }
        }
    }
}

/// An AVAudioEngine player for one decoded track with a stereo balance.
///
/// Callers hand it a PCM buffer, then drive play/pause/stop/seek from the main
/// thread. `onProgress` fires on the main thread while playing; `onEnded` fires
/// once when the track runs out and the player has stopped at the end.
@objc final class TMBalanceAudioPlayer: NSObject {
    /// 0 = left only, 0.5 = centered, 1 = right only. Mirrors Android's 0...1000 slider.
    @objc static let centeredBalance: Float = 0.5

    @objc var onProgress: (() -> Void)?
    @objc var onEnded: (() -> Void)?
    /// Called when the audio system pauses playback on its own (an interruption or a route change).
    @objc var onInterrupted: (() -> Void)?

    let engine = AVAudioEngine()
    private var sourceNode: AVAudioSourceNode?
    private let state = TMRenderState()
    private var format: AVAudioFormat?
    private var sampleRate: Double = 44100
    private var progressTimer: Timer?
    private var observers: [NSObjectProtocol] = []

    override init() {
        super.init()
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: .main) { [weak self] note in
            guard let self, self.isPlaying,
                  let raw = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
                  AVAudioSession.InterruptionType(rawValue: raw) == .began else { return }
            self.pause()
            self.onInterrupted?()
        })
        observers.append(center.addObserver(forName: .AVAudioEngineConfigurationChange, object: engine, queue: .main) { [weak self] _ in
            guard let self, self.isPlaying else { return }
            // The output route changed under a running engine; it stops itself, so resume it.
            if !self.startEngine() {
                self.state.isPlaying = false
                self.stopProgressTimer()
                self.onInterrupted?()
            }
        })
    }

    deinit {
        observers.forEach { NotificationCenter.default.removeObserver($0) }
        progressTimer?.invalidate()
        engine.stop()
    }

    // MARK: State

    /// Current balance in 0...1. Set through `setBalance(_:)` so the render state follows.
    @objc private(set) var balance: Float = TMBalanceAudioPlayer.centeredBalance

    @objc var isPlaying: Bool { state.isPlaying }
    @objc var isLoaded: Bool { format != nil }
    @objc var duration: TimeInterval { TimeInterval(state.frameLength) / sampleRate }
    @objc var currentTime: TimeInterval {
        get { TimeInterval(state.currentFrame) / sampleRate }
        set { seek(to: newValue) }
    }

    @objc func gains() -> TMBalanceGains { TMBalanceGains(balance: balance) }

    @objc(applyBalance:) func setBalance(_ value: Float) {
        balance = max(0, min(1, value.isFinite ? value : TMBalanceAudioPlayer.centeredBalance))
        state.setGains(TMBalanceGains(balance: balance))
    }

    // MARK: Loading

    /// Installs a decoded buffer. Playback starts paused at the beginning.
    @objc func load(_ buffer: AVAudioPCMBuffer) {
        unload()
        let rate = buffer.format.sampleRate
        guard rate > 0, buffer.format.commonFormat == .pcmFormatFloat32, !buffer.format.isInterleaved,
              let stereo = AVAudioFormat(standardFormatWithSampleRate: rate, channels: 2) else { return }
        sampleRate = rate
        format = stereo
        state.setGains(TMBalanceGains(balance: balance))
        state.load(buffer)
        let node = AVAudioSourceNode(format: stereo) { [state] _, _, frameCount, abl -> OSStatus in
            state.render(frameCount: frameCount, into: abl)
            return noErr
        }
        sourceNode = node
        engine.attach(node)
        // Accessing mainMixerNode connects it to the output in whatever mode the engine is in
        // (the device, or offline rendering in tests). Preparation waits for the first play.
        engine.connect(node, to: engine.mainMixerNode, format: stereo)
    }

    @objc func unload() {
        stop()
        engine.stop()
        if let sourceNode {
            engine.disconnectNodeOutput(sourceNode)
            engine.detach(sourceNode)
        }
        sourceNode = nil
        format = nil
        state.load(nil)
    }

    /// Longest track the player will decode into memory. Learning tracks are a minute or
    /// two; this keeps a stray long file from taking hundreds of megabytes of PCM.
    @objc static let maximumDecodedDuration: TimeInterval = 8 * 60
    /// Hard ceiling on decoded PCM, whatever the sample rate or channel count.
    @objc static let maximumDecodedBytes = 256 * 1024 * 1024

    /// Decodes an audio file into a non-interleaved float buffer this player can render.
    /// Reads in chunks and stops early when `shouldCancel` returns true.
    @objc static func decode(fileAt url: URL, shouldCancel: () -> Bool = { false }) throws -> AVAudioPCMBuffer {
        let file = try AVAudioFile(forReading: url, commonFormat: .pcmFormatFloat32, interleaved: false)
        let rate = file.processingFormat.sampleRate
        guard rate > 0, file.length > 0 else { throw decodeError(1, "The track contains no audio.") }
        let channels = Int(file.processingFormat.channelCount)
        let decodedBytes = file.length * Int64(channels) * Int64(MemoryLayout<Float>.size)
        guard TimeInterval(file.length) / rate <= maximumDecodedDuration, channels > 0,
              decodedBytes <= Int64(maximumDecodedBytes) else {
            throw decodeError(3, "The track is too long to load.")
        }
        let frames = AVAudioFrameCount(clamping: file.length)
        guard let buffer = AVAudioPCMBuffer(pcmFormat: file.processingFormat, frameCapacity: frames) else {
            throw decodeError(2, "The track could not be decoded.")
        }
        let chunkFrames: AVAudioFrameCount = 65_536
        guard let chunk = AVAudioPCMBuffer(pcmFormat: file.processingFormat, frameCapacity: chunkFrames) else {
            throw decodeError(2, "The track could not be decoded.")
        }
        while buffer.frameLength < frames {
            if shouldCancel() { throw decodeError(4, "Loading was cancelled.") }
            try file.read(into: chunk, frameCount: min(chunkFrames, frames - buffer.frameLength))
            guard chunk.frameLength > 0, let source = chunk.floatChannelData, let target = buffer.floatChannelData else { break }
            for channel in 0..<channels {
                (target[channel] + Int(buffer.frameLength)).update(from: source[channel], count: Int(chunk.frameLength))
            }
            buffer.frameLength += chunk.frameLength
        }
        guard buffer.frameLength > 0 else { throw decodeError(2, "The track could not be decoded.") }
        return buffer
    }

    private static func decodeError(_ code: Int, _ message: String) -> NSError {
        NSError(domain: "TMBalanceAudioPlayer", code: code, userInfo: [NSLocalizedDescriptionKey: message])
    }

    // MARK: Transport

    /// Starts or resumes playback. Returns false when the engine cannot start.
    @discardableResult
    @objc func play() -> Bool {
        guard isLoaded else { return false }
        if state.hasReachedEnd || state.currentFrame >= state.frameLength { state.currentFrame = 0 }
        guard startEngine() else { return false }
        state.isPlaying = true
        startProgressTimer()
        return true
    }

    @objc func pause() {
        state.isPlaying = false
        stopProgressTimer()
        if engine.isRunning { engine.pause() }
    }

    /// Pauses, rewinds to the beginning, and gives the audio session back so audio
    /// that was interrupted in another app can resume.
    @objc func stop() {
        pause()
        state.currentFrame = 0
        releaseSession()
    }

    private func releaseSession() {
        guard !engine.isInManualRenderingMode else { return }
        engine.stop()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    @objc func seek(to time: TimeInterval) {
        guard isLoaded, time.isFinite else { return }
        state.currentFrame = Int((max(0, time) * sampleRate).rounded())
    }

    /// Off in the unit-test bundle: CI simulators have no audio device, and starting a
    /// hardware engine there deadlocks the audio server. Offline (manual rendering)
    /// engines still start, so the render tests exercise the real graph.
    nonisolated(unsafe) static var usesAudioHardware = true

    private func startEngine() -> Bool {
        guard !engine.isRunning else { return true }
        if !Self.usesAudioHardware && !engine.isInManualRenderingMode { return true }
        do {
            if !engine.isInManualRenderingMode { try? AVAudioSession.sharedInstance().setActive(true) }
            try engine.start()
            return true
        } catch {
            return false
        }
    }

    private func startProgressTimer() {
        stopProgressTimer()
        let timer = Timer(timeInterval: 0.04, repeats: true) { [weak self] _ in self?.tick() }
        RunLoop.main.add(timer, forMode: .common)
        progressTimer = timer
    }

    private func stopProgressTimer() {
        progressTimer?.invalidate()
        progressTimer = nil
    }

    private func tick() {
        guard state.isPlaying else { return }
        if state.hasReachedEnd {
            state.isPlaying = false
            stopProgressTimer()
            if engine.isRunning { engine.pause() }
            onProgress?()
            onEnded?()
            return
        }
        onProgress?()
    }
}
