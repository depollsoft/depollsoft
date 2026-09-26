//
//  SettingsScreen.swift
//  pitchperfect
//
//  Settings: the pitch pipe's behaviour and theme, the account, privacy
//  choices and, in private builds, the build number and a log copier.
//

import FirebaseAuth
import FirebaseFunctions
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
        theme = DPTheme.storedTheme
        isSignedIn = account.isSignedIn()
    }

    func setToggleNotes(_ on: Bool) { settings.toggleNotes = on }
    func setWakeLock(_ on: Bool) { settings.wakeLock = on }

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

    var body: some View {
        @Bindable var model = box.model
        InstrumentPage(showsBanner: UIDevice.current.userInterfaceIdiom == .phone, tableStyle: .grouped) {
            List {
                Section {
                    SwitchRow(title: "Toggle Notes", detail: "Notes play until pressed again",
                              isOn: Binding(get: { model.toggleNotes }, set: model.setToggleNotes))
                    SwitchRow(title: "Wake Lock", detail: "Prevent device from sleeping",
                              isOn: Binding(get: { model.wakeLock }, set: model.setWakeLock))
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
                        .accessibilityIdentifier("settings.theme")
                    }
                    .settingsRow(height: 52)
                } header: {
                    PlateHeader("Pitch Pipe").settingsHeader()
                }

                Section {
                    ActionRow(title: model.accountTitle, busy: model.signingIn, action: model.logInOrOut)
                    if model.isSignedIn {
                        ActionRow(title: "Delete Account", busy: model.deleting, action: model.requestDelete)
                    }
                } header: {
                    PlateHeader("Account").settingsHeader()
                } footer: {
                    Text("Log in to back up and synchronize your song list and settings.")
                        .settingsHeader()
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
                    .settingsRow(height: 51)
                } header: {
                    PlateHeader("Privacy").settingsHeader()
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
            .background(StaffBackground())
            // The List's own scroll view, whichever rows happen to be built.
            .background(EnclosedListFinder { SettingsScrollMemory.attach($0) }.frame(width: 0, height: 0))
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
        .settingsRow()
    }
}

private struct ActionRow: View {
    let title: String
    let busy: Bool
    let action: () -> Void

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
        .settingsRow(height: 51)
    }
}

/// Where Settings was scrolled to. UIKit presented one Settings controller for
/// the app's lifetime, so reopening it (from any tab) found the table where it
/// was left; each SwiftUI presentation builds a new List. The offset is filed as
/// it changes and put back as the new List's content grows tall enough for it,
/// unless the user scrolls first.
@MainActor
enum SettingsScrollMemory {
    private static weak var list: UIScrollView?
    private(set) static var offset: CGFloat?
    /// The offset to hold the new list at while it settles.
    private static var pending: CGFloat?
    private static var attachedAt = Date.distantPast
    /// How long a new List may still be laying out (and scrolling itself back to
    /// the top) after it appears.
    static let settlingTime: TimeInterval = 1
    private static var restoring = false
    private static var observations: [NSKeyValueObservation] = []

    static func attach(_ scrollView: UIScrollView) {
        guard list !== scrollView else { return }
        list = scrollView
        pending = offset
        attachedAt = Date()
        observations = [
            scrollView.observe(\.contentSize) { view, _ in MainActor.assumeIsolated { restore(view) } },
            scrollView.observe(\.contentOffset) { view, _ in MainActor.assumeIsolated { changed(view) } },
        ]
        restore(scrollView)
    }

    /// While settling, the list is held at the old place against its own layout;
    /// the user's first drag (or the end of settling) hands it over.
    private static var settling: Bool {
        pending != nil && Date().timeIntervalSince(attachedAt) < settlingTime
    }

    private static func restore(_ view: UIScrollView) {
        guard view === list, let target = pending, view.window != nil, view.bounds.height > 0 else { return }
        guard settling, !view.isTracking, !view.isDragging else {
            pending = nil
            return
        }
        let top = -view.adjustedContentInset.top
        let bottom = max(top, view.contentSize.height - view.bounds.height + view.adjustedContentInset.bottom)
        let y = min(max(target, top), bottom)
        guard view.contentOffset.y != y else { return }
        restoring = true
        view.contentOffset.y = y
        restoring = false
    }

    /// A scroll: the user's, filed; the list's own while settling, undone.
    private static func changed(_ view: UIScrollView) {
        guard view === list, !restoring, view.window != nil else { return }
        if pending != nil {
            restore(view)
            if pending != nil { return }
        }
        offset = view.contentOffset.y
    }

    static func forget() {
        list = nil
        offset = nil
        pending = nil
        observations = []
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
        sheet(isPresented: isPresented) {
            NavigationStack { SettingsScreen() }
        }
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

/// UIKit's subtitle cell, measured.
private enum SettingsMetrics {
    static let subtitleTop: CGFloat = 9
    static let subtitleBottom: CGFloat = 35.0 / 3.0
    static let subtitleSpacing: CGFloat = 3
}
