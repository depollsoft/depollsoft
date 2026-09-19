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

    @objc static func present(from presenter: UIViewController) {
        let controller = PrivacyViewController(style: .insetGrouped)
        let navigation = UINavigationController(rootViewController: controller)
        navigation.view.tintColor = .label
        navigation.navigationBar.tintColor = .label
        presenter.present(navigation, animated: true)
        navigation.presentationController?.delegate = controller
    }
}

private final class PrivacyViewController: UITableViewController, UIAdaptivePresentationControllerDelegate {
    private let analytics = UISwitch()
    private let crashes = UISwitch()
    private let choices = PrivacyChoices()
    private var hasAdPrivacy: Bool { TelemetryConsent.adPrivacyRequired() }

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Privacy choices"
        analytics.isOn = choices.analytics
        crashes.isOn = choices.crashes
        analytics.accessibilityLabel = "Usage analytics"
        crashes.accessibilityLabel = "Crash reports"
        navigationItem.rightBarButtonItem = UIBarButtonItem(title: "Save choices", style: .plain,
                                                            target: self, action: #selector(save))
        navigationItem.leftBarButtonItem = UIBarButtonItem(barButtonSystemItem: .cancel,
                                                           target: self, action: #selector(cancel))
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 56
    }

    override func numberOfSections(in tableView: UITableView) -> Int { 4 }
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        section == 3 ? (hasAdPrivacy ? 3 : 2) : 1
    }
    override func tableView(_ tableView: UITableView, titleForFooterInSection section: Int) -> String? {
        switch section {
        case 1: return "Share screens visited, sessions, and app and device information with Google Analytics to understand app usage."
        case 2: return "Send crash reports, including stack traces and app and device information, to Google Firebase Crashlytics to help fix problems. Turning this off takes full effect the next time you start the app."
        default: return nil
        }
    }
    override func tableView(_ tableView: UITableView, willDisplayFooterView view: UIView, forSection section: Int) {
        guard let footer = view as? UITableViewHeaderFooterView else { return }
        footer.textLabel?.textColor = .label
        footer.textLabel?.adjustsFontForContentSizeCategory = true
    }
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
        cell.textLabel?.numberOfLines = 0
        cell.textLabel?.font = .preferredFont(forTextStyle: .body)
        cell.textLabel?.adjustsFontForContentSizeCategory = true
        cell.selectionStyle = .none
        switch indexPath.section {
        case 0:
            cell.textLabel?.text = "Choose whether to share optional data to help improve this app. Both choices are off until you enable them. The app works either way. You can change these choices in Settings."
        case 1:
            cell.textLabel?.text = "Usage analytics"
            cell.accessoryView = analytics
        case 2:
            cell.textLabel?.text = "Crash reports"
            cell.accessoryView = crashes
        default:
            cell.textLabel?.text = indexPath.row == 0 ? "Decline both" : indexPath.row == 1 ? "Privacy policy" : "Ad privacy choices"
            cell.textLabel?.textColor = view.tintColor
            cell.selectionStyle = .default
            cell.accessibilityTraits = .button
        }
        return cell
    }
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.section == 3 else { return }
        switch indexPath.row {
        case 0:
            analytics.isOn = false
            crashes.isOn = false
            save()
        case 1: UIApplication.shared.open(URL(string: "https://apps.depoll.com/privacy/")!)
        default: TelemetryConsent.showAdPrivacy?(self)
        }
    }
    @objc private func save() {
        choices.save(analytics: analytics.isOn, crashes: crashes.isOn)
        TelemetryConsent.applySavedChoices()
        dismiss(animated: true) { TelemetryConsent.onDismiss?() }
    }
    @objc private func cancel() {
        dismiss(animated: true) { TelemetryConsent.onDismiss?() }
    }
    func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
        TelemetryConsent.onDismiss?()
    }
}
