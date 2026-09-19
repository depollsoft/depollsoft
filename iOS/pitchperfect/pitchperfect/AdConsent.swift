import UIKit
import UserMessagingPlatform
import AppTrackingTransparency
import GoogleMobileAds

@objc final class AdConsent: NSObject {
    private static var gathering = false
    private static var gathered = false
    private static var adsStarted = false
    @objc static var onConsentFlowFinished: (() -> Void)?
    @objc static var canRequestAds: Bool { gathered && ConsentInformation.shared.canRequestAds }

    @objc static func configure() {
        TelemetryConsent.adPrivacyRequired = {
            ConsentInformation.shared.privacyOptionsRequirementStatus == .required
        }
        TelemetryConsent.showAdPrivacy = { presenter in
            ConsentForm.presentPrivacyOptionsForm(from: presenter) { error in
                if error != nil {
                    let alert = UIAlertController(title: "Ad privacy choices unavailable",
                        message: "Please try again when you have an internet connection.", preferredStyle: .alert)
                    alert.addAction(UIAlertAction(title: "OK", style: .default))
                    presenter.present(alert, animated: true)
                }
                startAdsIfAllowed()
            }
        }
        TelemetryConsent.onDismiss = { gather() }
    }

    private static var presenter: UIViewController? {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows).first(where: \.isKeyWindow)?.rootViewController
    }

    private static func gather() {
#if DEBUG
        if ProcessInfo.processInfo.environment["STORE_SCREENSHOTS"] == "1" { return }
#endif
        guard !gathering, !gathered, let presenter, presenter.presentedViewController == nil else { return }
        gathering = true
        ConsentInformation.shared.requestConsentInfoUpdate(with: RequestParameters()) { error in
            if error != nil {
                finish()
                return
            }
            ConsentForm.loadAndPresentIfRequired(from: presenter) { _ in finish() }
        }
    }

    private static func finish() {
        let complete = {
            gathered = true
            gathering = false
            startAdsIfAllowed()
            onConsentFlowFinished?()
        }
        if ConsentInformation.shared.canRequestAds && ATTrackingManager.trackingAuthorizationStatus == .notDetermined {
            ATTrackingManager.requestTrackingAuthorization { _ in DispatchQueue.main.async(execute: complete) }
        } else { complete() }
    }

    private static func startAdsIfAllowed() {
        guard canRequestAds else { return }
        if !adsStarted {
            adsStarted = true
            MobileAds.shared.start { _ in reloadVisibleBanners() }
        } else { reloadVisibleBanners() }
    }

    private static func reloadVisibleBanners() {
        guard canRequestAds, let view = presenter?.view else { return }
        func reload(_ view: UIView) {
            if let banner = view as? BannerView, banner.window != nil { banner.load(Request()) }
            view.subviews.forEach(reload)
        }
        reload(view)
    }
}
