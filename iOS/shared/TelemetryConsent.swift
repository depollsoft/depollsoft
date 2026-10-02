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
    /// Runs when saved choices allow usage analytics, so an app can send what it
    /// reported while collection was still off (its user properties).
    static var analyticsAllowed: (() -> Void)?
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
        if choices.analytics { analyticsAllowed?() }
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
    /// `onDismiss` runs once the sheet has fully gone, however it closed.
    @objc static func present(from presenter: UIViewController) {
        presenter.present(PrivacyChoicesHost(), animated: true)
    }
}

/// Reports the dismissal only once it has finished: the ad-consent flow that
/// follows presents from the root and gives up while anything is still
/// presented. A form shown over the sheet (Ad privacy choices) is not a dismissal.
final class PrivacyChoicesHost: UIHostingController<PrivacyChoicesSheet> {
    init() { super.init(rootView: PrivacyChoicesSheet()) }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if isBeingDismissed || presentingViewController == nil { TelemetryConsent.onDismiss?() }
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
/// Whoever presents it reports the dismissal (see PrivacyChoicesHost).
struct PrivacyChoicesSheet: View {
    var body: some View {
        NavigationStack { PrivacyChoicesView() }
            .tint(Color(uiColor: .label))
    }
}

struct PrivacyChoicesView: View {
    @Environment(\.displayScale) private var scale
    @State private var model = PrivacyChoicesModel()
    @State private var cardMargin = InsetGroupedMargin.lastMeasured
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL

    var body: some View {
        List {
            Section {
                Text("Choose whether to share optional data to help improve this app. Both choices are off until you enable them. The app works either way. You can change these choices in Settings.")
                    // A multi-line UITableViewCell label sat closer to the card's edges.
                    .padding(.top, -3)
                    .padding(.bottom, TableNudge.at(scale, threeX: -10.0 / 3.0, twoX: -3.5))
            }
            Section {
                Toggle("Usage analytics", isOn: $model.analytics)
                    // UISwitch kept its green; the sheet's label tint is for its bar items.
                    .tint(nil)
                    // A cell's accessory switch sat 6 pt further in than SwiftUI's.
                    .padding(.trailing, 6)
            } footer: {
                Text("Share screens visited, features used, sessions, and app and device information with Google Analytics to understand app usage.")
                    .foregroundStyle(Color(uiColor: .label))
                    // UITableView's phone footers: this text ⅓ pt higher, the next card ⅔ pt lower.
                    .offset(y: FooterNudge.phone(TableNudge.at(scale, threeX: -1.0 / 3.0, twoX: -5.0 / 6.0)))
                    .padding(.bottom, FooterNudge.phone(TableNudge.at(scale, threeX: 2.0 / 3.0, twoX: -1.0 / 3.0)))
            }
            Section {
                Toggle("Crash reports", isOn: $model.crashes)
                    // UISwitch kept its green; the sheet's label tint is for its bar items.
                    .tint(nil)
                    // A cell's accessory switch sat 6 pt further in than SwiftUI's.
                    .padding(.trailing, 6)
            } footer: {
                Text("Send crash reports, including stack traces and app and device information, to Google Firebase Crashlytics to help fix problems. Turning this off takes full effect the next time you start the app.")
                    .foregroundStyle(Color(uiColor: .label))
                    .offset(y: FooterNudge.phone(-2.0 / 3.0))
                    .padding(.bottom, FooterNudge.phone(TableNudge.at(scale, threeX: 1.0 / 3.0, twoX: -1.0 / 6.0)))
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
        // UIKit's inset-grouped cards sat at its controller's system margin
        // (16 pt on an iPhone SE, 20 pt on larger phones).
        .contentMargins(.horizontal, cardMargin, for: .scrollContent)
        .background(InsetGroupedMargin { cardMargin = $0 })
        .navigationTitle("Privacy choices")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                CancelButton { dismiss() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    model.save()
                    dismiss()
                } label: {
                    // A plain UIBarButtonItem's title is set in the medium weight.
                    Text("Save choices").fontWeight(.medium)
                }
            }
        }
    }
}

/// Where UIKit put an inset-grouped table's cards in the controller showing a view:
/// the layout margin of a UITableViewController's own inset-grouped table, in a
/// navigation controller of the same width and size classes, as the UIKit
/// Privacy choices screen was built (16 pt on an iPhone SE, 20 pt on larger phones).
private struct InsetGroupedMargin: UIViewRepresentable {
    struct Key: Hashable {
        var width: CGFloat
        var horizontal: UIUserInterfaceSizeClass
        var vertical: UIUserInterfaceSizeClass
    }

    @MainActor static var lastMeasured: CGFloat = 20
    @MainActor private static var measured: [Key: CGFloat] = [:]
    let changed: (CGFloat) -> Void

    @MainActor static func measure(_ key: Key, in window: UIWindow) -> CGFloat {
        if let margin = measured[key] { return margin }
        let screen = UITableViewController(style: .insetGrouped)
        let navigation = UINavigationController(rootViewController: screen)
        navigation.traitOverrides.horizontalSizeClass = key.horizontal
        navigation.traitOverrides.verticalSizeClass = key.vertical
        navigation.view.frame = CGRect(x: 0, y: 0, width: key.width, height: 600)
        navigation.view.isHidden = true
        window.addSubview(navigation.view)
        defer { navigation.view.removeFromSuperview() }
        navigation.view.layoutIfNeeded()
        screen.tableView.layoutIfNeeded()
        let margin = screen.tableView.layoutMargins.left - screen.tableView.safeAreaInsets.left
        measured[key] = margin
        return margin
    }

    final class View: UIView {
        var changed: (CGFloat) -> Void = { _ in }
        private var measuredFor: Key?

        override func didMoveToWindow() {
            super.didMoveToWindow()
            report()
        }

        override func layoutSubviews() {
            super.layoutSubviews()
            report()
        }

        private func report() {
            var responder: UIResponder? = next
            while let current = responder, !(current is UIViewController) { responder = current.next }
            guard let window, let container = (responder as? UIViewController)?.view,
                  container.bounds.width > 0 else { return }
            let traits = container.traitCollection
            let key = Key(width: container.bounds.width, horizontal: traits.horizontalSizeClass,
                          vertical: traits.verticalSizeClass)
            guard key != measuredFor else { return }
            measuredFor = key
            let margin = InsetGroupedMargin.measure(key, in: window)
            InsetGroupedMargin.lastMeasured = margin
            let changed = self.changed
            DispatchQueue.main.async { changed(margin) }
        }
    }

    func makeUIView(context: Context) -> View {
        let view = View()
        view.isUserInteractionEnabled = false
        view.isAccessibilityElement = false
        return view
    }

    func updateUIView(_ view: View, context: Context) { view.changed = changed }
}

/// UIKit's table rounds its rows and footers to the screen's pixels, so its
/// sub-point positions differ at 2x and 3x; these corrections were measured at both.
private enum TableNudge {
    static func at(_ scale: CGFloat, threeX: CGFloat, twoX: CGFloat) -> CGFloat {
        scale >= 3 ? threeX : twoX
    }
}

/// Sub-point corrections measured against UIKit's table on a phone; an iPad's
/// table sets its footers differently.
private enum FooterNudge {
    static func phone(_ value: CGFloat) -> CGFloat {
        UIDevice.current.userInterfaceIdiom == .phone ? value : 0
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
