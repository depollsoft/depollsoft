//
//  SettingsScreen.swift
//  pitchperfect
//
//  Settings: the pitch pipe's behaviour and theme, the account, privacy
//  choices and, in private builds, the build number and a log copier.
//

import FirebaseAuth
import FirebaseFunctions
import ObjectiveC
import SwiftUI
import UIKit

/// Whom Settings talks to about the account; tests swap in a fake.
struct AccountService {
    var isSignedIn: () -> Bool
    var userDescription: () -> String
    var signOut: () -> Void
    /// Deletes the account server-side; reports nil or the failure.
    var deleteAccount: (@escaping (Error?) -> Void) -> Void

    static let firebase = AccountService(
        isSignedIn: { Auth.auth().currentUser != nil },
        userDescription: { DPSettingsModel.sharedInstance.userString },
        signOut: { try? Auth.auth().signOut() },
        deleteAccount: { completion in
            Functions.functions().httpsCallable("deleteUser").call { _, error in completion(error) }
        }
    )
}

@Observable
@MainActor
final class SettingsModel {
    private let settings: DPSettingsModel
    private let songs: DPSongsModel
    private let account: AccountService
    private let bundle: Bundle
    @ObservationIgnored private var settingsObserver: NSObjectProtocol?

    private(set) var toggleNotes = false
    private(set) var wakeLock = false
    private(set) var referencePitch = DPSettingsModel.standardReferencePitch
    private(set) var theme = 0
    private(set) var isSignedIn = false

    /// A sign-in sheet is up (from Log in).
    var showingSignIn = false
    /// The Log in row's spinner.
    private(set) var signingIn = false
    /// The Delete Account row's spinner.
    private(set) var deleting = false
    var confirmingDelete = false
    /// The confirmation's title, named when the delete is asked for.
    private(set) var deleteTitle = ""
    var deleteError: String?
    var showingPrivacy = false

    init(settings: DPSettingsModel = .sharedInstance,
         songs: DPSongsModel = .sharedInstance,
         account: AccountService = .firebase,
         bundle: Bundle = .main) {
        self.settings = settings
        self.songs = songs
        self.account = account
        self.bundle = bundle
        reload()
        settingsObserver = NotificationCenter.default.addObserver(
            forName: .settingsChanged, object: settings, queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated { self?.reload() }
        }
    }

    deinit {
        if let settingsObserver { NotificationCenter.default.removeObserver(settingsObserver) }
    }

    func reload() {
        toggleNotes = settings.toggleNotes
        wakeLock = settings.wakeLock
        referencePitch = settings.referencePitch
        theme = DPTheme.storedTheme
        isSignedIn = account.isSignedIn()
    }

    func setToggleNotes(_ on: Bool) { settings.toggleNotes = on }
    func setWakeLock(_ on: Bool) { settings.wakeLock = on }
    func setReferencePitch(_ hz: Int) { settings.referencePitch = hz }

    /// A choice as the tuning menu names it.
    static func tuningLabel(_ hz: Int) -> String {
        switch hz {
        case 415: "\(hz) Hz (Baroque)"
        case 430: "\(hz) Hz (Classical)"
        case 440: "\(hz) Hz (Standard)"
        default: "\(hz) Hz"
        }
    }

    func setTheme(_ value: Int) {
        DPTheme.storedTheme = value
        theme = value
    }

    // MARK: Account

    var accountTitle: String { isSignedIn ? "Log out" : "Log in" }

    func logInOrOut() {
        if isSignedIn {
            account.signOut()
            reload()
        } else {
            signingIn = true
            showingSignIn = true
        }
    }

    /// The sign-in sheet finished with an account (sync has started; see `SignIn`).
    func signedIn(isNewUser: Bool) {
        showingSignIn = false
        signingIn = false
        reload()
    }

    /// The sign-in sheet went away without an account. (The UIKit screen left its
    /// spinner turning forever in this case.)
    func signInDismissed() {
        showingSignIn = false
        signingIn = false
        reload()
    }

    func requestDelete() {
        // Read once, when asked, as the UIKit screen did: not on every redraw.
        deleteTitle = "Delete Account \(account.userDescription())"
        deleting = true
        confirmingDelete = true
    }

    func cancelDelete() {
        confirmingDelete = false
        deleting = false
    }

    func confirmDelete() {
        confirmingDelete = false
        songs.detachFromFirestore()
        settings.detachFromFirestore()
        // The account is signed out once the server has deleted it even if
        // Settings has been closed meanwhile (UIKit's Settings was a singleton
        // that outlived its sheet, so its completion always ran).
        let account = self.account
        account.deleteAccount { [weak self] error in
            MainActor.assumeIsolated {
                if error == nil { account.signOut() }
                guard let self else { return }
                self.deleting = false
                if let error {
                    self.deleteError = "Something went wrong and your account was not deleted. Please try again. (\(error.localizedDescription))"
                } else {
                    self.reload()
                }
            }
        }
    }

    // MARK: Private builds

    var privateBuildNumber: String { bundle.object(forInfoDictionaryKey: "PrivateBuildNumber") as? String ?? "" }
    var privatePRNumber: String { bundle.object(forInfoDictionaryKey: "PrivatePRNumber") as? String ?? "?" }
    var isPrivateBuild: Bool { !privateBuildNumber.isEmpty }
    var buildDescription: String { "Build \(privateBuildNumber) · PR #\(privatePRNumber)" }

    func copyLogs() {
        DPAppLog.log("Settings: copied app logs")
        UIPasteboard.general.string = "\(buildDescription)\n\n\(DPAppLog.contents())"
    }
}

struct SettingsScreen: View {
    @StateObject private var box = ModelBox(SettingsModel())
    @Environment(\.dismiss) private var dismiss
    @State private var cellHeights = SettingsCellHeights.lastMeasured

    var body: some View {
        @Bindable var model = box.model
        InstrumentPage(showsBanner: UIDevice.current.userInterfaceIdiom == .phone, tableStyle: .grouped) {
            List {
                Section {
                    SwitchRow(title: "Toggle Notes", detail: "Notes play until pressed again",
                              isOn: Binding(get: { model.toggleNotes }, set: model.setToggleNotes))
                    SwitchRow(title: "Wake Lock", detail: "Prevent device from sleeping",
                              isOn: Binding(get: { model.wakeLock }, set: model.setWakeLock))
                    TuningRow(selection: Binding(get: { model.referencePitch }, set: model.setReferencePitch))
                    HStack {
                        Text("Theme")
                        Spacer(minLength: 16)
                        Picker("Theme", selection: Binding(get: { model.theme }, set: model.setTheme)) {
                            Text("Default").tag(0)
                            Text("Light").tag(1)
                            Text("Dark").tag(2)
                        }
                        .pickerStyle(.segmented)
                        .fixedSize()
                        .offset(y: -2.0 / 3.0)
                        .accessibilityIdentifier("settings.theme")
                    }
                    // UIKit's accessory control sat ⅔ pt higher in its cell.
                    .settingsRow(height: cellHeights?.theme ?? 52)
                } header: {
                    PlateHeader("Pitch Pipe").settingsHeader()
                }

                Section {
                    ActionRow(title: model.accountTitle, busy: model.signingIn, action: model.logInOrOut)
                    if model.isSignedIn {
                        ActionRow(title: "Delete Account", busy: model.deleting, action: model.requestDelete)
                    }
                } header: {
                    // UIKit's header and footer below the first section sat ⅓ and ⅔ pt higher.
                    PlateHeader("Account").settingsHeader().padding(.bottom, -1.0 / 3.0)
                } footer: {
                    Text("Log in to back up and synchronize your song list and settings.")
                        .settingsHeader()
                        .offset(y: -2.0 / 3.0)
                }

                Section {
                    Button { model.showingPrivacy = true } label: {
                        HStack {
                            Text("Privacy choices").foregroundStyle(Color(uiColor: .label))
                            Spacer()
                            DisclosureIndicator()
                                .accessibilityHidden(true)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(UnhighlightedRowStyle())
                    .settingsRow(height: cellHeights?.plain ?? 51)
                } header: {
                    PlateHeader("Privacy").settingsHeader().padding(.bottom, 1.0 / 3.0)
                }

                if model.isPrivateBuild {
                    Section {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Private Build")
                            Text(model.buildDescription)
                                .font(.subheadline)
                                .foregroundStyle(Color(uiColor: .secondaryLabel))
                        }
                        .settingsRow()
                        ActionRow(title: "Copy Logs", busy: false, action: model.copyLogs)
                    } header: {
                        PlateHeader("Private Build").settingsHeader()
                    }
                }
            }
            .listStyle(.grouped)
            .scrollContentBackground(.hidden)
            .environment(\.settingsCellHeights, cellHeights)
            .background(SettingsCellHeights.Probe { if $0 != cellHeights { cellHeights = $0 } })
            .background(StaffBackground())
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .instrumentChrome()
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                BarSymbolButton(systemName: "checkmark") { dismiss() }
            }
        }
        .onAppear { model.reload() }
        .alert(model.deleteTitle, isPresented: $model.confirmingDelete) {
            Button("Cancel", role: .cancel) { model.cancelDelete() }
            Button("Yes", role: .destructive) { model.confirmDelete() }
        } message: {
            Text("Are you sure you want to delete your account and all associated data?  This cannot be undone.  Locally-saved songs and preferences will not be deleted.")
        }
        .alert("Couldn't Delete Account",
               isPresented: Binding(get: { model.deleteError != nil }, set: { if !$0 { model.deleteError = nil } })) {
            Button("OK") {}
        } message: {
            Text(model.deleteError ?? "")
        }
        .sheet(isPresented: $model.showingPrivacy, onDismiss: { TelemetryConsent.onDismiss?() }) {
            PrivacyChoicesSheet()
        }
        .signInSheet(isPresented: $model.showingSignIn,
                     onSignIn: model.signedIn(isNewUser:),
                     onDismiss: model.signInDismissed)
    }
}

/// Engraved section label: tracked monospaced capitals in secondary ink.
struct PlateHeader: View {
    let title: String
    let uppercased: Bool
    init(_ title: String, uppercased: Bool = true) {
        self.title = title
        self.uppercased = uppercased
    }

    var body: some View {
        Text(uppercased ? title.uppercased() : title)
            .font(Plate.mono(12))
            .tracking(12 * 0.14)
            .foregroundStyle(Plate.inkSecondary)
            .textCase(nil)
            .accessibilityLabel(title)
            .accessibilityAddTraits(.isHeader)
    }
}

private struct SwitchRow: View {
    let title: String
    let detail: String
    @Binding var isOn: Bool

    @Environment(\.settingsCellHeights) private var heights

    var body: some View {
        Toggle(isOn: $isOn) {
            VStack(alignment: .leading, spacing: SettingsMetrics.subtitleSpacing) {
                Text(title)
                Text(detail)
                    .font(.subheadline)
                    .foregroundStyle(Color(uiColor: .secondaryLabel))
            }
            .padding(.top, SettingsMetrics.subtitleTop)
            .padding(.bottom, SettingsMetrics.subtitleBottom)
        }
        .tint(nil)
        // A cell's accessory view sits a little further in than its content.
        .padding(.trailing, 4.0 / 3.0)
        // UIKit's own row height for this cell on this screen: the insets above
        // add up to it at 3x but round a pixel taller at 2x.
        .frame(height: heights?.subtitle)
        .settingsRow()
    }
}

/// The tuning: a title and detail as the switch rows have, then a menu of
/// choices for A4.
private struct TuningRow: View {
    @Binding var selection: Int
    @Environment(\.settingsCellHeights) private var heights

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: SettingsMetrics.subtitleSpacing) {
                Text("Tuning")
                Text("Frequency of A4")
                    .font(.subheadline)
                    .foregroundStyle(Color(uiColor: .secondaryLabel))
            }
            .padding(.top, SettingsMetrics.subtitleTop)
            .padding(.bottom, SettingsMetrics.subtitleBottom)
            Spacer(minLength: 16)
            Picker("Tuning", selection: $selection) {
                ForEach(DPSettingsModel.commonReferencePitches, id: \.self) { hz in
                    Text(SettingsModel.tuningLabel(hz)).tag(hz)
                }
            }
            .pickerStyle(.menu)
            .labelsHidden()
            .fixedSize()
            .accessibilityIdentifier("settings.tuning")
        }
        .frame(height: heights?.subtitle)
        .settingsRow()
    }
}

private struct ActionRow: View {
    let title: String
    let busy: Bool
    let action: () -> Void
    @Environment(\.settingsCellHeights) private var heights

    var body: some View {
        Button(action: action) {
            HStack {
                Text(title).foregroundStyle(Color(uiColor: .label))
                Spacer()
                if busy { ActivitySpinner() }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(UnhighlightedRowStyle())
        .settingsRow(height: heights?.plain ?? 51)
    }
}

/// The UIKit rows took taps through a gesture recogniser on a table that allowed
/// no selection: nothing highlighted when pressed.
struct UnhighlightedRowStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View { configuration.label }
}

/// UIKit's medium activity indicator, in its own grey (SwiftUI's would take the
/// label tint the instrument chrome sets).
struct ActivitySpinner: UIViewRepresentable {
    func makeUIView(context: Context) -> UIActivityIndicatorView {
        let spinner = UIActivityIndicatorView(style: .medium)
        spinner.startAnimating()
        return spinner
    }

    func updateUIView(_ spinner: UIActivityIndicatorView, context: Context) {}
}

/// UIKit's disclosure indicator, from the cell accessory itself.
struct DisclosureIndicator: UIViewRepresentable {
    func makeUIView(context: Context) -> UIView {
        let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
        cell.accessoryType = .disclosureIndicator
        cell.frame = CGRect(x: 0, y: 0, width: 200, height: 44)
        cell.layoutIfNeeded()
        // The accessory image view the cell lays out, lifted out on its own.
        let image = cell.subviews.compactMap { $0 as? UIButton }.first?.image(for: .normal)
            ?? UIImage(systemName: "chevron.forward")
        let view = UIImageView(image: image)
        view.tintColor = .tertiaryLabel
        view.contentMode = .center
        return view
    }

    func updateUIView(_ view: UIView, context: Context) {}

    func sizeThatFits(_ proposal: ProposedViewSize, uiView: UIView, context: Context) -> CGSize? {
        uiView.intrinsicContentSize
    }
}

extension View {
    /// A UITableViewCell's frame: clear, 20 pt margins, and (for single-line
    /// rows) the cell's height.
    /// UIKit set section headers and footers at the table margin; a grouped List
    /// sets them at 16 pt.
    fileprivate func settingsHeader() -> some View { modifier(SettingsHeaderInset()) }

    fileprivate func settingsRow(height: CGFloat? = nil) -> some View {
        modifier(SettingsRow(height: height))
    }

    /// Settings as every tab presents it: a sheet with its own navigation bar.
    func settingsSheet(isPresented: Binding<Bool>) -> some View {
        background(SettingsPresenter(isPresented: isPresented).frame(width: 0, height: 0))
    }
}

/// Settings as the UIKit app presented it: one controller for the app's life,
/// presented from whichever tab asks, so reopening it (from any tab) finds it
/// as it was left, scroll position and all.
final class SettingsHost: UIHostingController<SettingsSheet> {
    /// Address-only key for the window's association.
    nonisolated(unsafe) private static var windowKey: UInt8 = 0

    /// The Settings `window` already made, if any.
    static func existing(for window: UIWindow) -> SettingsHost? {
        objc_getAssociatedObject(window, &windowKey) as? SettingsHost
    }

    /// The Settings for `window`: one per window, owned by the window, so it goes
    /// when the window does.
    static func shared(for window: UIWindow) -> SettingsHost {
        if let host = existing(for: window) { return host }
        let host = SettingsHost()
        objc_setAssociatedObject(window, &windowKey, host, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
        return host
    }

    /// Called once the sheet has gone, however it was closed.
    var onClose: () -> Void = {}

    private init() {
        super.init(rootView: SettingsSheet())
    }

    @available(*, unavailable)
    @MainActor required dynamic init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        if isBeingDismissed || presentingViewController == nil {
            let close = onClose
            onClose = {}
            close()
        }
    }
}

/// Settings with its own navigation bar.
struct SettingsSheet: View {
    var body: some View {
        NavigationStack { SettingsScreen() }
    }
}

/// Presents the window's `SettingsHost` while `isPresented` is true.
private struct SettingsPresenter: UIViewControllerRepresentable {
    @Binding var isPresented: Bool

    final class Presenter: UIViewController {
        /// Whether this tab's Settings is the one up: only its own tab closes it.
        private var presenting = false
        /// The live request, read when the presentation actually happens rather
        /// than when it was asked for.
        private var wanted: () -> Bool = { false }
        private var close: () -> Void = {}
        private var scheduled = false

        func sync(wanted: @escaping () -> Bool, close: @escaping () -> Void) {
            self.wanted = wanted
            self.close = close
            guard wanted() != presenting, !scheduled else { return }
            // Not during SwiftUI's update: a hosting controller made there never
            // builds its NavigationStack's navigation controller.
            scheduled = true
            DispatchQueue.main.async { [weak self] in
                self?.scheduled = false
                self?.reconcile(attempt: 0)
            }
        }

        /// Brings the sheet in line with the request: waits out any presentation
        /// or dismissal in flight, and hands the request back if UIKit refuses it.
        func reconcile(attempt: Int) {
            guard let window = view.window else { return }
            if wanted() {
                guard !presenting else { return }
                let host = SettingsHost.shared(for: window)
                // Already up (asked for by another tab): nothing to do.
                guard host.presentingViewController == nil else { return }
                // From the window's root, not from this controller: a NavigationStack
                // presented from a controller inside another NavigationStack hands
                // its title and toolbar to the presenting screen's bar.
                var presenter = window.rootViewController ?? self
                while let next = presenter.presentedViewController { presenter = next }
                if presenter.isBeingPresented || presenter.isBeingDismissed,
                   let coordinator = presenter.transitionCoordinator, attempt < 20 {
                    coordinator.animate(alongsideTransition: nil) { [weak self] _ in
                        DispatchQueue.main.async { self?.reconcile(attempt: attempt + 1) }
                    }
                    return
                }
                if presenter.isBeingDismissed, let below = presenter.presentingViewController {
                    presenter = below
                }
                presenting = true
                host.onClose = { [weak self] in
                    self?.presenting = false
                    self?.close()
                }
                presenter.present(host, animated: true)
                if host.presentingViewController == nil {
                    // Refused (UIKit logs and drops a presentation it cannot make
                    // now): hand the request back so the next tap tries again.
                    presenting = false
                    host.onClose = {}
                    close()
                }
            } else if presenting, let host = SettingsHost.existing(for: window),
                      host.presentingViewController != nil, !host.isBeingDismissed {
                host.dismiss(animated: true)
            }
        }
    }

    func makeUIViewController(context: Context) -> Presenter {
        let presenter = Presenter()
        presenter.view.isHidden = true
        return presenter
    }

    func updateUIViewController(_ presenter: Presenter, context: Context) {
        let binding = $isPresented
        presenter.sync(wanted: { binding.wrappedValue }, close: { binding.wrappedValue = false })
    }
}

/// A UITableViewCell's frame: clear, the table's margins, and its height.
private struct SettingsRow: ViewModifier {
    let height: CGFloat?
    @Environment(\.tableMargin) private var margin

    func body(content: Content) -> some View {
        content
            .frame(minHeight: height)
            .listRowInsets(EdgeInsets(top: 0, leading: margin, bottom: 0, trailing: margin))
            .listRowBackground(Color.clear)
    }
}

private struct SettingsHeaderInset: ViewModifier {
    @Environment(\.tableMargin) private var margin

    func body(content: Content) -> some View {
        content.padding(.horizontal, max(0, margin - 16))
    }
}

extension EnvironmentValues {
    /// UIKit's Settings row heights on this device, once measured.
    @Entry var settingsCellHeights: SettingsCellHeights.Heights?
}

/// The heights UIKit's grouped table gives the Settings screen's own cells (a
/// subtitle cell with a switch, a cell with the theme control, a plain cell) in
/// a screen of this width, text size and size classes. Rows take these rather
/// than adding up insets, which round differently at 2x and 3x.
@MainActor
enum SettingsCellHeights {
    struct Heights: Equatable {
        var subtitle: CGFloat
        var theme: CGFloat
        var plain: CGFloat
    }

    struct Key: Hashable {
        var width: CGFloat
        var category: UIContentSizeCategory
        var horizontal: UIUserInterfaceSizeClass
        var vertical: UIUserInterfaceSizeClass
    }

    private static var measured: [Key: Heights] = [:]
    static var lastMeasured: Heights?

    private final class Cells: NSObject, UITableViewDataSource {
        func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { 3 }

        func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
            // Built as DPSettingsViewController built them.
            switch indexPath.row {
            case 0:
                let cell = UITableViewCell(style: .subtitle, reuseIdentifier: nil)
                cell.textLabel?.text = "Toggle Notes"
                cell.detailTextLabel?.text = "Notes play until pressed again"
                cell.accessoryView = UISwitch()
                return cell
            case 1:
                let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
                cell.textLabel?.text = "Theme"
                cell.accessoryView = UISegmentedControl(items: ["Default", "Light", "Dark"])
                return cell
            default:
                let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
                cell.textLabel?.text = "Log in"
                cell.accessoryView = UIActivityIndicatorView(style: .medium)
                return cell
            }
        }
    }

    static func measure(_ key: Key, traits: UITraitCollection, in window: UIWindow) -> Heights {
        if let heights = measured[key] {
            lastMeasured = heights
            return heights
        }
        let screen = UIViewController()
        let navigation = UINavigationController(rootViewController: screen)
        navigation.traitOverrides.horizontalSizeClass = key.horizontal
        navigation.traitOverrides.verticalSizeClass = key.vertical
        navigation.traitOverrides.preferredContentSizeCategory = key.category
        navigation.view.frame = CGRect(x: 0, y: 0, width: key.width, height: 800)
        navigation.view.isHidden = true
        window.addSubview(navigation.view)
        defer { navigation.view.removeFromSuperview() }
        let cells = Cells()
        let table = UITableView(frame: CGRect(x: 0, y: 0, width: key.width, height: 800), style: .grouped)
        table.dataSource = cells
        screen.view.addSubview(table)
        navigation.view.layoutIfNeeded()
        table.reloadData()
        table.layoutIfNeeded()
        let heights = Heights(subtitle: table.rectForRow(at: IndexPath(row: 0, section: 0)).height,
                              theme: table.rectForRow(at: IndexPath(row: 1, section: 0)).height,
                              plain: table.rectForRow(at: IndexPath(row: 2, section: 0)).height)
        measured[key] = heights
        lastMeasured = heights
        return heights
    }

    /// Measures for the screen's own container, again when its width, text size
    /// or size classes change.
    struct Probe: UIViewRepresentable {
        let measured: (Heights) -> Void

        final class View: UIView {
            var measured: (Heights) -> Void = { _ in }
            private var measuredFor: Key?

            override func didMoveToWindow() {
                super.didMoveToWindow()
                measure()
            }

            override func layoutSubviews() {
                super.layoutSubviews()
                measure()
            }

            override func traitCollectionDidChange(_ previous: UITraitCollection?) {
                super.traitCollectionDidChange(previous)
                measure()
            }

            private func measure() {
                guard let window, let container = owningViewController?.view, container.bounds.width > 0 else { return }
                let traits = container.traitCollection
                let key = Key(width: container.bounds.width, category: traits.preferredContentSizeCategory,
                              horizontal: traits.horizontalSizeClass, vertical: traits.verticalSizeClass)
                guard key != measuredFor else { return }
                measuredFor = key
                let heights = SettingsCellHeights.measure(key, traits: traits, in: window)
                let report = measured
                DispatchQueue.main.async { report(heights) }
            }
        }

        func makeUIView(context: Context) -> View {
            let view = View()
            view.isUserInteractionEnabled = false
            view.isAccessibilityElement = false
            return view
        }

        func updateUIView(_ view: View, context: Context) { view.measured = measured }
    }
}

/// UIKit's subtitle cell, measured.
private enum SettingsMetrics {
    static let subtitleTop: CGFloat = 9
    static let subtitleBottom: CGFloat = 35.0 / 3.0
    static let subtitleSpacing: CGFloat = 3
}
