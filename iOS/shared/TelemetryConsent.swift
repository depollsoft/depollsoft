import SwiftUI
import UIKit
import FirebaseAnalytics
import FirebaseCrashlytics

struct PrivacyChoices {
    let defaults: UserDefaults
    init(defaults: UserDefaults = .standard) { self.defaults = defaults }
    var hasChosen: Bool { defaults.bool(forKey: "telemetry.chosen") }
    var analytics: Bool { hasChosen && defaults.bool(forKey: "telemetry.analytics") }
    var crashes: Bool { hasChosen && defaults.bool(forKey: "telemetry.crashes") }
    func save(analytics: Bool, crashes: Bool) {
        defaults.set(analytics, forKey: "telemetry.analytics")
        defaults.set(crashes, forKey: "telemetry.crashes")
        defaults.set(true, forKey: "telemetry.chosen")
    }
}

@objc final class TelemetryConsent: NSObject {
    private static var promptedThisSession = false
    static var adPrivacyRequired: () -> Bool = { false }
    static var showAdPrivacy: ((UIViewController) -> Void)?
    static var onDismiss: (() -> Void)?
    @objc static var hasChosen: Bool { PrivacyChoices().hasChosen }

    @objc static func configure() {
#if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--reset-privacy-for-testing") {
            ["telemetry.chosen", "telemetry.analytics", "telemetry.crashes"].forEach {
                UserDefaults.standard.removeObject(forKey: $0)
            }
        }
#endif
        applySavedChoices()
    }

    @objc static func applySavedChoices() {
        let choices = PrivacyChoices()
        Analytics.setConsent([
            .analyticsStorage: choices.analytics ? .granted : .denied,
            .adStorage: .denied, .adUserData: .denied, .adPersonalization: .denied,
        ])
        Analytics.setAnalyticsCollectionEnabled(choices.analytics)
        if !choices.analytics { Analytics.resetAnalyticsData() }
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(choices.crashes)
        if !choices.crashes { Crashlytics.crashlytics().deleteUnsentReports() }
    }

    @objc static func presentIfNeeded(from presenter: UIViewController) {
        guard !hasChosen, !promptedThisSession else {
            onDismiss?()
            return
        }
        guard presenter.presentedViewController == nil else { return }
        promptedThisSession = true
        present(from: presenter)
    }

    /// Presents Privacy choices from UIKit: a sheet with its own navigation bar.
    @objc static func present(from presenter: UIViewController) {
        let host = UIHostingController(rootView: PrivacyChoicesSheet())
        presenter.present(host, animated: true)
    }
}

/// What the Privacy choices screen shows and saves.
@Observable
@MainActor
final class PrivacyChoicesModel {
    private let choices: PrivacyChoices
    var analytics: Bool
    var crashes: Bool
    let showsAdPrivacy: Bool

    init(choices: PrivacyChoices = PrivacyChoices(),
         adPrivacyRequired: Bool = TelemetryConsent.adPrivacyRequired()) {
        self.choices = choices
        analytics = choices.analytics
        crashes = choices.crashes
        showsAdPrivacy = adPrivacyRequired
    }

    func save() {
        choices.save(analytics: analytics, crashes: crashes)
        TelemetryConsent.applySavedChoices()
    }

    func declineBoth() {
        analytics = false
        crashes = false
        save()
    }
}

/// Privacy choices in its own navigation stack, as both apps present it.
/// However it closes (Save, Cancel or a swipe), TelemetryConsent.onDismiss runs
/// once, so the ad-consent flow can follow.
struct PrivacyChoicesSheet: View {
    var body: some View {
        NavigationStack { PrivacyChoicesView() }
            .tint(Color(uiColor: .label))
            .onDisappear { TelemetryConsent.onDismiss?() }
    }
}

struct PrivacyChoicesView: View {
    @State private var model = PrivacyChoicesModel()
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    var body: some View {
        List {
            Section {
                Text("Choose whether to share optional data to help improve this app. Both choices are off until you enable them. The app works either way. You can change these choices in Settings.")
                    // A multi-line UITableViewCell label sat closer to the card's edges.
                    .padding(.top, -3)
                    .padding(.bottom, -10.0 / 3.0)
            }
            Section {
                Toggle("Usage analytics", isOn: $model.analytics)
            } footer: {
                Text("Share screens visited, sessions, and app and device information with Google Analytics to understand app usage.")
                    .foregroundStyle(Color(uiColor: .label))
            }
            Section {
                Toggle("Crash reports", isOn: $model.crashes)
            } footer: {
                Text("Send crash reports, including stack traces and app and device information, to Google Firebase Crashlytics to help fix problems. Turning this off takes full effect the next time you start the app.")
                    .foregroundStyle(Color(uiColor: .label))
            }
            Section {
                Button("Decline both") {
                    model.declineBoth()
                    dismiss()
                }
                Button("Privacy policy") { openURL(URL(string: "https://apps.depoll.com/privacy/")!) }
                if model.showsAdPrivacy {
                    Button("Ad privacy choices") { TelemetryConsent.presentAdPrivacy() }
                }
            }
            .foregroundStyle(.tint)
        }
        .listStyle(.insetGrouped)
        // UITableView's inset-grouped cards sit 20 pt in on a phone.
        .contentMargins(.horizontal, 20, for: .scrollContent)
        .navigationTitle("Privacy choices")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                CancelButton { dismiss() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("Save choices") {
                    model.save()
                    dismiss()
                }
            }
        }
    }
}

extension TelemetryConsent {
    /// The ad SDK's own privacy form, presented over whatever is frontmost.
    static func presentAdPrivacy() {
        guard let presenter = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene }).flatMap(\.windows)
            .first(where: \.isKeyWindow)?.rootViewController else { return }
        var top = presenter
        while let next = top.presentedViewController { top = next }
        showAdPrivacy?(top)
    }
}

/// The system Cancel bar item: an ✕ on iOS 26, the word before it.
private struct CancelButton: View {
    let action: () -> Void

    var body: some View {
        if #available(iOS 26.0, *) {
            Button(role: .close, action: action)
                .accessibilityLabel("Cancel")
        } else {
            Button("Cancel", role: .cancel, action: action)
        }
    }
}
