//
//  TagTracksPlaybackTests.swift
//  tagmasterTests
//
//  Bringing a learning track into the Tracks page's inline player: one busy
//  settlement per attempt, main-thread delivery, readiness timeouts, retries
//  that belong to their own attempt and track, and cancellation when the page
//  or the tag goes away. Controlled loaders drive the edge cases; silent local
//  WAVs exercise the real loader, decoder and balance player.
//

import AVFoundation
import XCTest
@testable import tagmaster

/// A track source whose cache key the test chooses.
final class TMTestTrackLocation: DPRemoteLocation {
    var testCacheKey = "tracks-\(UUID().uuidString)"
    override var cacheKey: String { testCacheKey }
}

@MainActor
final class TagTracksPlaybackTests: XCTestCase {
    private var tagLoader: TMControlledTagLoader!
    private var detail: TagDetailModel!
    private var tracks: TagTracksModel { detail.tracks }
    private var loaders: [TMReviewLoader] = []
    private var presentations: [(DPTrack, AVAudioPCMBuffer)] = []
    private var errors = 0
    private var realPlayback = false
    private var files: [URL] = []
    private var cachePaths: [String] = []

    override func setUp() {
        super.setUp()
        tagLoader = TMControlledTagLoader()
        detail = TagDetailModel(loader: tagLoader)
        configure(tracks)
    }

    override func tearDown() {
        detail?.tracks.stopPlayback()
        detail = nil
        files.forEach { try? FileManager.default.removeItem(at: $0) }
        cachePaths.forEach { try? FileManager.default.removeItem(atPath: $0) }
        super.tearDown()
    }

    private func configure(_ model: TagTracksModel) {
        model.makeLoader = { [unowned self] url, key in
            if self.realPlayback {
                let loader = TMTrackLoader(url: url, cacheKey: key)
                return loader
            }
            let loader = TMReviewLoader(url: url, cacheKey: key)
            self.loaders.append(loader)
            return loader
        }
        if !realPlayback {
            model.presentPlayer = { [unowned self] track, buffer in
                XCTAssertTrue(Thread.isMainThread)
                self.presentations.append((track, buffer))
            }
        }
    }

    /// Loads a tag with one tenor track into the detail.
    @discardableResult
    private func loadTag(id: Int32 = 44) -> DPTag {
        let tag = DPTag()
        tag.tagId = id
        let location = TMTestTrackLocation()
        location.uri = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("\(location.testCacheKey).wav")
        location.type = "wav"
        tag.tenorTrackUri = location
        files.append(location.uri)
        if let path = DPFileCache.path(forKey: location.cacheKey) { cachePaths.append(path) }
        detail.show(tagId: id)
        tagLoader.finish(tagLoader.requests.count - 1, with: tag)
        tracks.appeared()
        return tag
    }

    private var lastLoader: TMReviewLoader { loaders.last! }
    private var track: DPTrack { tracks.tracks[0] }

    private func observeErrors() {
        if detail.error != nil {
            errors += 1
            retry = detail.error?.retry
            detail.error = nil
        }
    }

    private var retry: (() -> Void)?

    private func drain() {
        let done = expectation(description: "main drained")
        DispatchQueue.main.async { done.fulfill() }
        wait(for: [done], timeout: 2)
        observeErrors()
    }

    private func waitPastTimeout() {
        let done = expectation(description: "deadline passed")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { done.fulfill() }
        wait(for: [done], timeout: 2)
        observeErrors()
    }

    private func spin(until condition: () -> Bool, file: StaticString = #filePath, line: UInt = #line) {
        let deadline = Date().addingTimeInterval(5)
        while !condition(), Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.005))
            observeErrors()
        }
        XCTAssertTrue(condition(), "Condition never held", file: file, line: line)
    }

    private func buffer() -> AVAudioPCMBuffer {
        let format = AVAudioFormat(standardFormatWithSampleRate: 44100, channels: 2)!
        let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: 4410)!
        buffer.frameLength = 4410
        return buffer
    }

    private func assertSettled(_ loader: TMReviewLoader, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertEqual(detail.busy.count, 0, file: file, line: line)
        XCTAssertEqual(detail.busy.settled, 2, "The tag's load and one track attempt", file: file, line: line)
        XCTAssertEqual(loader.cancels, 1, file: file, line: line)
    }

    // MARK: - Controlled loads

    func testReadySettlesTheRowOnceAndPresentsTheBuffer() {
        loadTag()
        tracks.select(track)
        let loader = lastLoader
        XCTAssertTrue(tracks.loadingTrack === track, "The row shows it is loading")
        XCTAssertEqual(detail.busy.count, 1)
        let decoded = buffer()
        loader.succeed(with: decoded)
        drain()
        XCTAssertEqual(presentations.count, 1)
        XCTAssertTrue(presentations[0].0 === track)
        XCTAssertTrue(presentations[0].1 === decoded)
        XCTAssertNil(tracks.session)
        XCTAssertNil(tracks.loadingTrack)
        XCTAssertEqual(detail.busy.count, 0)
        XCTAssertEqual(detail.busy.settled, 2)

        loader.succeed(with: decoded)
        loader.fail()
        drain()
        tracks.cancelLoading()
        XCTAssertEqual(presentations.count, 1)
        XCTAssertEqual(errors, 0)
        XCTAssertEqual(detail.busy.settled, 2)
        XCTAssertEqual(loader.cancels, 0)
    }

    func testAFailureBeforeReadyOffersOneRetryAndCancelsTheLoader() {
        loadTag()
        tracks.select(track)
        let loader = lastLoader
        let session = tracks.session
        loader.fail()
        loader.fail()
        loader.succeed(with: buffer())
        drain()
        XCTAssertEqual(errors, 1)
        XCTAssertNotNil(retry)
        XCTAssertTrue(tracks.session === session, "The failed attempt stays current until its retry")
        XCTAssertEqual(presentations.count, 0)
        tracks.cancelLoading()
        assertSettled(loader)
    }

    func testReleasingThePageWhileLoadingCancelsWithoutKeepingItAlive() {
        weak var weakDetail: TagDetailModel?
        weak var weakSession: TMTrackPlaybackSession?
        var loader: TMReviewLoader!
        var busy: TMBusyCount!
        autoreleasepool {
            loadTag()
            tracks.readyTimeout = 0.05
            tracks.select(track)
            weakDetail = detail
            weakSession = tracks.session
            loader = lastLoader
            busy = detail.busy
            detail = nil
        }
        XCTAssertNil(weakDetail)
        XCTAssertNil(weakSession)
        XCTAssertEqual(busy.count, 0)
        XCTAssertEqual(loader.cancels, 1)
        loader.succeed(with: buffer())
        loader.fail()
        let done = expectation(description: "deadline passed")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) { done.fulfill() }
        wait(for: [done], timeout: 2)
        XCTAssertEqual(busy.count, 0)
        XCTAssertEqual(busy.settled, 2)
        detail = TagDetailModel(loader: tagLoader)
    }

    func testLeavingThePageCancelsReadyFailureAndTimeout() {
        loadTag()
        tracks.readyTimeout = 0.05
        tracks.select(track)
        let loader = lastLoader
        tracks.disappeared()
        XCTAssertNil(tracks.session)
        loader.succeed(with: buffer())
        loader.fail()
        waitPastTimeout()
        XCTAssertEqual(presentations.count, 0)
        XCTAssertEqual(errors, 0)
        assertSettled(loader)
    }

    func testChangingTheTagCancelsAQueuedReady() {
        loadTag()
        tracks.select(track)
        let loader = lastLoader
        loader.succeed(with: buffer())
        detail.show(tagId: 45)
        drain()
        XCTAssertNil(tracks.session)
        XCTAssertEqual(presentations.count, 0)
        XCTAssertEqual(errors, 0)
        XCTAssertEqual(loader.cancels, 1)
    }

    func testATimeoutOffersRetryAndALateReadyCannotPresent() {
        loadTag()
        tracks.readyTimeout = 0.01
        tracks.select(track)
        let loader = lastLoader
        spin(until: { errors == 1 })
        loader.succeed(with: buffer())
        drain()
        XCTAssertNotNil(retry)
        XCTAssertEqual(presentations.count, 0)
        assertSettled(loader)
    }

    func testReadyCancelsThePreparationTimeout() {
        loadTag()
        tracks.readyTimeout = 0.05
        tracks.select(track)
        lastLoader.succeed(with: buffer())
        drain()
        waitPastTimeout()
        XCTAssertEqual(errors, 0)
        XCTAssertEqual(presentations.count, 1)
        XCTAssertNil(tracks.session)
        XCTAssertEqual(detail.busy.settled, 2)
    }

    func testRetryStartsAFreshAttemptAndIgnoresTheOldOnesEvents() {
        loadTag()
        tracks.select(track)
        let oldLoader = lastLoader
        oldLoader.fail()
        drain()
        let oldSession = tracks.session
        let oldRetry = retry
        oldRetry?()
        XCTAssertFalse(tracks.session === oldSession)
        XCTAssertFalse(lastLoader === oldLoader)
        let newSession = tracks.session
        oldRetry?()
        XCTAssertTrue(tracks.session === newSession, "A spent retry does nothing")
        oldLoader.succeed(with: buffer())
        oldLoader.fail()
        drain()
        XCTAssertEqual(presentations.count, 0)
        XCTAssertEqual(errors, 1)
        lastLoader.succeed(with: buffer())
        drain()
        XCTAssertEqual(presentations.count, 1)
        XCTAssertEqual(detail.busy.settled, 3)
    }

    func testRetryFindsTheChosenTrackAfterTheRowsReorder() throws {
        let tag = loadTag()
        let items = try XCTUnwrap(tag.value(forKey: "tracks") as? NSMutableArray)
        let chosen = track
        let other = TMTestTrackLocation()
        other.uri = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(other.testCacheKey)
        items.add(DPTrack(title: "Other", source: other))
        tracks.select(chosen)
        lastLoader.fail()
        drain()
        items.exchangeObject(at: 0, withObjectAt: 1)
        retry?()
        XCTAssertEqual(lastLoader.url, chosen.source.uri, "Track identity, not the old row index")
        XCTAssertTrue(tracks.loadingTrack === chosen)
        XCTAssertEqual(detail.busy.count, 1)
        tracks.cancelLoading()
    }

    func testChoosingAnotherTrackRetiresTheOldRetryAndItsEvents() throws {
        let tag = loadTag()
        let items = try XCTUnwrap(tag.value(forKey: "tracks") as? NSMutableArray)
        let first = track
        let other = TMTestTrackLocation()
        other.uri = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent(other.testCacheKey)
        items.add(DPTrack(title: "Other", source: other))
        tracks.select(first)
        let oldLoader = lastLoader
        oldLoader.fail()
        drain()
        let oldRetry = retry
        tracks.select(tracks.tracks[1])
        let newLoader = lastLoader
        oldRetry?()
        oldLoader.succeed(with: buffer())
        oldLoader.fail()
        drain()
        XCTAssertTrue(lastLoader === newLoader)
        XCTAssertEqual(lastLoader.url, other.uri)
        XCTAssertEqual(errors, 1)
        XCTAssertEqual(presentations.count, 0)
        tracks.cancelLoading()
        XCTAssertEqual(detail.busy.settled, 3)
    }

    func testRetryCannotPlayATrackThatIsGone() throws {
        let tag = loadTag()
        tracks.select(track)
        lastLoader.fail()
        drain()
        let oldLoader = lastLoader
        (try XCTUnwrap(tag.value(forKey: "tracks") as? NSMutableArray)).removeAllObjects()
        retry?()
        XCTAssertTrue(lastLoader === oldLoader)
        XCTAssertEqual(detail.busy.count, 0)
    }

    func testRetryAfterTheTagChangesOrThePageLeavesDoesNothing() {
        for changeTag in [true, false] {
            loadTag(id: changeTag ? 44 : 46)
            tracks.select(track)
            lastLoader.fail()
            drain()
            let oldLoader = lastLoader
            if changeTag { detail.show(tagId: 45) } else { tracks.disappeared() }
            retry?()
            XCTAssertTrue(lastLoader === oldLoader)
            XCTAssertNil(tracks.session)
            XCTAssertEqual(detail.busy.count, changeTag ? 1 : 0, "Only the new tag's own load is in flight")
            if changeTag { tagLoader.finish(tagLoader.requests.count - 1, with: nil) }
            errors = 0
            retry = nil
        }
    }

    func testARemoteTrackKeepsItsURLAndCacheKeyAndACachedCopyIsPreferred() throws {
        loadTag()
        let source = track.source!
        source.uri = URL(string: "https://example.invalid/learning-track.wav")
        tracks.select(track)
        XCTAssertEqual(lastLoader.url, source.uri)
        XCTAssertEqual(lastLoader.cacheKey, source.cacheKey)
        tracks.cancelLoading()
        DPFileCache.write(Data("local fixture".utf8), forKey: source.cacheKey)
        tracks.select(track)
        XCTAssertEqual(lastLoader.url.path, DPFileCache.path(forKey: source.cacheKey))
        XCTAssertTrue(lastLoader.url.isFileURL)
        XCTAssertEqual(lastLoader.cacheKey, source.cacheKey)
        tracks.cancelLoading()
    }

    func testBackgroundCompletionsAreDeliveredOnTheMainThread() {
        for succeed in [true, false] {
            presentations = []
            errors = 0
            loadTag(id: succeed ? 44 : 47)
            tracks.select(track)
            let loader = lastLoader
            loader.backgroundDelivery = true
            if succeed { loader.succeed(with: buffer()) } else { loader.fail() }
            spin(until: { presentations.count + errors == 1 })
            XCTAssertEqual(presentations.count, succeed ? 1 : 0)
            XCTAssertEqual(errors, succeed ? 0 : 1)
            XCTAssertEqual(detail.busy.count, 0)
            tracks.cancelLoading()
        }
    }

    // MARK: - Real local playback

    /// Silent 16-bit mono PCM, a quarter of a second long.
    private func writeWAV(_ url: URL) throws {
        var data = Data()
        func append<T>(_ value: T) { withUnsafeBytes(of: value) { data.append(contentsOf: $0) } }
        let samples: UInt32 = 8000 / 4, dataSize = samples * 2
        data.append(contentsOf: Array("RIFF".utf8)); append(UInt32(dataSize + 36))
        data.append(contentsOf: Array("WAVEfmt ".utf8)); append(UInt32(16)); append(UInt16(1)); append(UInt16(1))
        append(UInt32(8000)); append(UInt32(16000)); append(UInt16(2)); append(UInt16(16))
        data.append(contentsOf: Array("data".utf8)); append(dataSize)
        data.append(Data(count: Int(dataSize)))
        try data.write(to: url)
    }

    private func loadLocalTrack() throws {
        realPlayback = true
        tracks.presentPlayer = nil
        loadTag()
        try writeWAV(track.source.uri)
        tracks.select(track)
        spin(until: { tracks.playerVisible })
        XCTAssertTrue(tracks.player.isLoaded)
        XCTAssertTrue(tracks.player.track === track)
        XCTAssertEqual(tracks.player.duration, 0.25, accuracy: 0.001)
        XCTAssertNil(tracks.session)
        XCTAssertEqual(errors, 0)
        XCTAssertEqual(detail.busy.count, 0)
    }

    private func assertUnloaded(file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertFalse(tracks.playerVisible, file: file, line: line)
        XCTAssertFalse(tracks.player.isLoaded, file: file, line: line)
        XCTAssertFalse(tracks.player.isPlaying, file: file, line: line)
        XCTAssertNil(tracks.player.track, file: file, line: line)
        XCTAssertNil(tracks.session, file: file, line: line)
    }

    func testStoppingUnloadsAndHidesThePlayer() throws {
        try loadLocalTrack()
        tracks.cancelLoading()
        XCTAssertTrue(tracks.player.isLoaded, "Cancelling a load leaves playback alone")
        tracks.stopPlayback()
        assertUnloaded()
    }

    func testLeavingThePageUnloadsThePlayerAndNothingMoreLoads() throws {
        try loadLocalTrack()
        tracks.disappeared()
        XCTAssertTrue(tracks.hasLeft)
        assertUnloaded()
        tracks.select(track)
        XCTAssertNil(tracks.session, "An off-screen page starts nothing")
    }

    func testChangingTheTagUnloadsThePlayer() throws {
        try loadLocalTrack()
        detail.show(tagId: 99)
        assertUnloaded()
    }

    func testChoosingTheLoadedTrackAgainRestartsItWithoutReloading() throws {
        try loadLocalTrack()
        tracks.player.pause()
        tracks.player.seek(to: 0.2)
        let settled = detail.busy.settled
        tracks.select(track)
        XCTAssertNil(tracks.session)
        XCTAssertEqual(detail.busy.settled, settled)
        XCTAssertLessThan(tracks.player.position, 0.2)
        tracks.stopPlayback()
    }

    func testATrackThatWillNotDecodeOffersRetryAndPlaysOnceFixed() throws {
        realPlayback = true
        tracks.presentPlayer = nil
        loadTag()
        let url = track.source.uri!
        try Data("not audio".utf8).write(to: url)
        tracks.select(track)
        spin(until: { errors == 1 })
        XCTAssertFalse(tracks.playerVisible)
        XCTAssertFalse(tracks.player.isLoaded)
        try writeWAV(url)
        retry?()
        spin(until: { tracks.playerVisible })
        XCTAssertTrue(tracks.player.isLoaded)
        XCTAssertEqual(errors, 1)
        tracks.stopPlayback()
    }

    // MARK: - The player's own controls

    func testThePlayerReportsItsPositionAndBalanceTheWayAndroidDoes() throws {
        try loadLocalTrack()
        let player = tracks.player
        player.pause()
        player.seek(to: 0.1)
        XCTAssertEqual(player.counterText, "0.1/0.2s")
        XCTAssertTrue(player.canStop)
        XCTAssertEqual(player.balanceDescription, "Centered")
        player.setBalance(0.25)
        XCTAssertEqual(player.balanceDescription, "Left 100 percent, right 33 percent")
        player.centerBalance()
        XCTAssertEqual(player.balanceDescription, "Centered")
        player.togglePlayPause()
        XCTAssertTrue(player.isPlaying)
        player.togglePlayPause()
        XCTAssertFalse(player.isPlaying)
        player.stop()
        XCTAssertEqual(player.position, 0)
        tracks.stopPlayback()
    }
}
