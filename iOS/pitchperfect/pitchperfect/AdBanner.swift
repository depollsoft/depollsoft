//
//  AdBanner.swift
//  pitchperfect
//
//  The anchored adaptive banner every instrument screen docks at its foot.
//  The slot always reserves the adaptive height for its width, so a screen's
//  layout does not jump when an ad arrives (or never does).
//

import GoogleMobileAds
import SwiftUI
import UIKit

enum AdBanner {
    static var unitID: String {
#if DEBUG
        return "ca-app-pub-3940256099942544/2934735716"
#else
        if Bundle.main.bundleIdentifier?.hasSuffix(".private") == true {
            return "ca-app-pub-3940256099942544/2934735716"
        }
        return "a14fd7eba4542f0"
#endif
    }

    /// Store capture keeps the no-fill layout without requesting a test creative.
    /// The switch is absent from release builds.
    static var suppressed: Bool {
#if DEBUG
        return ProcessInfo.processInfo.environment["STORE_SCREENSHOTS"] == "1"
#else
        return false
#endif
    }

    static func size(width: CGFloat, landscape: Bool) -> AdSize {
        landscape ? landscapeAnchoredAdaptiveBanner(width: width) : portraitAnchoredAdaptiveBanner(width: width)
    }
}

/// Hosts the SDK's banner view: sizes it to the slot, loads once it is on screen
/// and consent allows, and reloads when the width or orientation changes.
final class BannerHostView: UIView, BannerViewDelegate {
    let banner = BannerView()
    private var loadedWidth: CGFloat = 0

    override init(frame: CGRect) {
        super.init(frame: frame)
        clipsToBounds = true
        backgroundColor = .clear
        banner.adUnitID = AdBanner.unitID
        banner.delegate = self
        banner.backgroundColor = .clear
        banner.clipsToBounds = true
        addSubview(banner)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    var isLandscape: Bool { window?.windowScene?.interfaceOrientation.isLandscape ?? false }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        banner.rootViewController = window?.rootViewController
        reloadIfNeeded(force: true)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        banner.frame = bounds
        reloadIfNeeded(force: false)
    }

    private func reloadIfNeeded(force: Bool) {
        guard window != nil, bounds.width > 0 else { return }
        guard force || bounds.width != loadedWidth else { return }
        loadedWidth = bounds.width
        banner.adSize = AdBanner.size(width: bounds.width, landscape: isLandscape)
        guard !AdBanner.suppressed, AdConsent.canRequestAds else { return }
        banner.load(Request())
    }

    func bannerViewDidReceiveAd(_ bannerView: BannerView) {
        DPAppLog.log("Pitch Perfect banner ad loaded: \(bannerView.adUnitID ?? "")")
    }

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        DPAppLog.log("Pitch Perfect banner ad failed (\(bannerView.adUnitID ?? "")): \(error.localizedDescription)")
    }
}

struct BannerAdSlot: UIViewRepresentable {
    func makeUIView(context: Context) -> BannerHostView { BannerHostView() }

    func updateUIView(_ view: BannerHostView, context: Context) {}

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: BannerHostView, context: Context) -> CGSize? {
        let width = proposal.width ?? uiView.window?.bounds.width ?? UIScreen.main.bounds.width
        let size = AdBanner.size(width: width, landscape: uiView.isLandscape).size
        return CGSize(width: width, height: size.height)
    }
}

/// A screen on the instrument: content over the staff, docked banner below.
struct InstrumentPage<Content: View>: View {
    var showsBanner = true
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(spacing: 0) {
            content()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            if showsBanner {
                BannerAdSlot()
                    .accessibilityHidden(true)
            }
        }
        .staffScreenBackground()
    }
}
