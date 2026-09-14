//
//  TMTrackPlayerView.swift
//  tagmaster
//
//  The inline learning-track player: play/pause, stop, a scrub bar with an
//  elapsed counter, and a balance slider. Mirrors Android's MediaPlayerView.
//

import UIKit
import AVFoundation

@objc final class TMTrackPlayerView: UIView {
    @objc let player = TMBalanceAudioPlayer()

    @objc let titleLabel = UILabel()
    @objc let playPauseButton = UIButton(type: .system)
    @objc let stopButton = UIButton(type: .system)
    @objc let scrubSlider = UISlider()
    @objc let counterLabel = UILabel()
    @objc let balanceSlider = UISlider()
    @objc let balanceLabel = UILabel()

    private var scrubbing = false
    private var currentTrack: DPTrack?
    private var showingPause: Bool?
    private weak var controls: UIStackView?

    override init(frame: CGRect) {
        super.init(frame: frame)
        build()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        build()
    }

    // MARK: Tracks

    /// Shows a decoded track. Playback does not start until `play()`.
    @objc func load(track: DPTrack, buffer: AVAudioPCMBuffer) {
        currentTrack = track
        titleLabel.text = track.title
        player.load(buffer)
        scrubSlider.maximumValue = Float(max(player.duration, 0.001))
        scrubSlider.value = 0
        scrubbing = false
        refresh()
    }

    @objc func play() {
        _ = player.play()
        refresh()
    }

    @objc func stop() {
        player.stop()
        refresh()
    }

    @objc func unload() {
        currentTrack = nil
        player.unload()
        titleLabel.text = nil
        scrubSlider.value = 0
        refresh()
    }

    @objc var track: DPTrack? { currentTrack }

    // MARK: Actions

    @objc private func togglePlayPause() {
        if player.isPlaying { player.pause() } else { _ = player.play() }
        refresh()
    }

    @objc private func scrubBegan() { scrubbing = true }

    @objc private func scrubChanged() {
        player.seek(to: TimeInterval(scrubSlider.value))
        counterLabel.text = TMTrackPlayerView.counterText(position: player.currentTime, length: player.duration)
        // VoiceOver adjusts the slider without a touch sequence, so keep the spoken value current.
        scrubSlider.accessibilityValue = counterLabel.text
    }

    @objc private func scrubEnded() {
        scrubbing = false
        refresh()
    }

    @objc private func balanceChanged() {
        player.setBalance(balanceSlider.value)
        updateBalanceAccessibility()
    }

    @objc func centerBalance() {
        balanceSlider.setValue(TMBalanceAudioPlayer.centeredBalance, animated: true)
        balanceChanged()
    }

    @objc private func centerBalanceAction(_ action: UIAccessibilityCustomAction) -> Bool {
        centerBalance()
        return true
    }

    // MARK: Presentation

    /// Android's `%1.1f/%1.1fs` counter.
    @objc static func counterText(position: TimeInterval, length: TimeInterval) -> String {
        String(format: "%1.1f/%1.1fs", max(0, position), max(0, length))
    }

    @objc func refresh() {
        let playing = player.isPlaying
        let loaded = player.isLoaded
        if showingPause != playing {
            // The progress timer calls refresh often; only touch the button when the state flips.
            showingPause = playing
            let symbol = playing ? "pause.fill" : "play.fill"
            playPauseButton.setImage(UIImage(systemName: symbol, withConfiguration: TMTrackPlayerView.symbolConfiguration), for: .normal)
            playPauseButton.accessibilityLabel = playing ? "Pause" : "Play"
        }
        playPauseButton.isEnabled = loaded
        stopButton.isEnabled = loaded && (playing || player.currentTime > 0)
        scrubSlider.isEnabled = loaded
        balanceSlider.isEnabled = loaded
        if !scrubbing { scrubSlider.value = Float(player.currentTime) }
        counterLabel.text = TMTrackPlayerView.counterText(position: player.currentTime, length: player.duration)
        scrubSlider.accessibilityValue = counterLabel.text
        updateBalanceAccessibility()
    }

    private func updateBalanceAccessibility() {
        let gains = player.gains()
        let left = Int((gains.left * 100).rounded()), right = Int((gains.right * 100).rounded())
        balanceSlider.accessibilityValue = left == right ? "Centered" : "Left \(left) percent, right \(right) percent"
    }

    private static let symbolConfiguration = UIImage.SymbolConfiguration(textStyle: .title2)

    /// At accessibility text sizes the counter and balance captions get wide, so the
    /// transport buttons move above the sliders instead of squeezing them.
    private func applyContentSizeLayout() {
        guard let controls else { return }
        let accessible = traitCollection.preferredContentSizeCategory.isAccessibilityCategory
        controls.axis = accessible ? .vertical : .horizontal
        controls.alignment = accessible ? .fill : .center
    }

    private func build() {
        accessibilityIdentifier = "tagmaster.trackPlayer"
        backgroundColor = .secondarySystemBackground
        layer.cornerRadius = 12
        layer.cornerCurve = .continuous
        directionalLayoutMargins = NSDirectionalEdgeInsets(top: 12, leading: 12, bottom: 12, trailing: 12)

        titleLabel.font = UIFont.preferredFont(forTextStyle: .headline)
        titleLabel.adjustsFontForContentSizeCategory = true
        titleLabel.numberOfLines = 0
        titleLabel.accessibilityIdentifier = "tagmaster.trackPlayer.title"

        playPauseButton.accessibilityIdentifier = "tagmaster.trackPlayer.playPause"
        playPauseButton.addTarget(self, action: #selector(togglePlayPause), for: .touchUpInside)
        stopButton.setImage(UIImage(systemName: "stop.fill", withConfiguration: TMTrackPlayerView.symbolConfiguration), for: .normal)
        stopButton.accessibilityLabel = "Stop"
        stopButton.accessibilityIdentifier = "tagmaster.trackPlayer.stop"
        stopButton.addTarget(self, action: #selector(stop), for: .touchUpInside)
        for button in [playPauseButton, stopButton] {
            button.widthAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
            button.heightAnchor.constraint(greaterThanOrEqualToConstant: 44).isActive = true
        }

        scrubSlider.accessibilityLabel = "Position"
        scrubSlider.accessibilityIdentifier = "tagmaster.trackPlayer.position"
        scrubSlider.addTarget(self, action: #selector(scrubBegan), for: .touchDown)
        scrubSlider.addTarget(self, action: #selector(scrubChanged), for: .valueChanged)
        scrubSlider.addTarget(self, action: #selector(scrubEnded), for: [.touchUpInside, .touchUpOutside, .touchCancel])

        counterLabel.font = UIFont.monospacedDigitSystemFont(ofSize: UIFont.preferredFont(forTextStyle: .caption1).pointSize, weight: .regular)
        counterLabel.adjustsFontForContentSizeCategory = true
        counterLabel.textColor = .secondaryLabel
        counterLabel.textAlignment = .right
        counterLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        counterLabel.accessibilityIdentifier = "tagmaster.trackPlayer.counter"
        counterLabel.widthAnchor.constraint(greaterThanOrEqualToConstant: 72).isActive = true

        balanceSlider.minimumValue = 0
        balanceSlider.maximumValue = 1
        balanceSlider.value = TMBalanceAudioPlayer.centeredBalance
        balanceSlider.minimumValueImage = UIImage(systemName: "l.circle")
        balanceSlider.maximumValueImage = UIImage(systemName: "r.circle")
        balanceSlider.accessibilityLabel = "Balance"
        balanceSlider.accessibilityHint = "Lowers one side to bring a part in or out."
        balanceSlider.accessibilityCustomActions = [
            UIAccessibilityCustomAction(name: "Center balance", target: self, selector: #selector(centerBalanceAction(_:)))
        ]
        balanceSlider.accessibilityIdentifier = "tagmaster.trackPlayer.balance"
        balanceSlider.addTarget(self, action: #selector(balanceChanged), for: .valueChanged)
        let center = UITapGestureRecognizer(target: self, action: #selector(centerBalance))
        center.numberOfTapsRequired = 2
        center.numberOfTouchesRequired = 2
        balanceSlider.addGestureRecognizer(center)

        balanceLabel.text = "Balance"
        balanceLabel.font = UIFont.preferredFont(forTextStyle: .caption1)
        balanceLabel.adjustsFontForContentSizeCategory = true
        balanceLabel.textColor = .secondaryLabel
        balanceLabel.textAlignment = .right
        balanceLabel.setContentCompressionResistancePriority(.required, for: .horizontal)
        balanceLabel.isAccessibilityElement = false

        let transport = UIStackView(arrangedSubviews: [playPauseButton, stopButton])
        transport.spacing = 4
        transport.alignment = .center

        let scrubRow = UIStackView(arrangedSubviews: [scrubSlider, counterLabel])
        scrubRow.spacing = 8
        scrubRow.alignment = .center
        let balanceRow = UIStackView(arrangedSubviews: [balanceSlider, balanceLabel])
        balanceRow.spacing = 8
        balanceRow.alignment = .center
        let sliders = UIStackView(arrangedSubviews: [scrubRow, balanceRow])
        sliders.axis = .vertical
        sliders.spacing = 4

        let controls = UIStackView(arrangedSubviews: [transport, sliders])
        controls.spacing = 8
        controls.alignment = .center
        self.controls = controls
        applyContentSizeLayout()
        registerForTraitChanges([UITraitPreferredContentSizeCategory.self]) { (view: TMTrackPlayerView, _) in
            view.applyContentSizeLayout()
        }

        let column = UIStackView(arrangedSubviews: [titleLabel, controls])
        column.axis = .vertical
        column.spacing = 8
        column.translatesAutoresizingMaskIntoConstraints = false
        addSubview(column)
        NSLayoutConstraint.activate([
            column.leadingAnchor.constraint(equalTo: layoutMarginsGuide.leadingAnchor),
            column.trailingAnchor.constraint(equalTo: layoutMarginsGuide.trailingAnchor),
            column.topAnchor.constraint(equalTo: layoutMarginsGuide.topAnchor),
            column.bottomAnchor.constraint(equalTo: layoutMarginsGuide.bottomAnchor),
            // Both sliders end at the same x so their thumbs line up.
            balanceLabel.widthAnchor.constraint(equalTo: counterLabel.widthAnchor)
        ])

        player.onProgress = { [weak self] in self?.refresh() }
        player.onEnded = { [weak self] in
            self?.player.stop()
            self?.refresh()
        }
        player.onInterrupted = { [weak self] in self?.refresh() }
        refresh()
    }
}
