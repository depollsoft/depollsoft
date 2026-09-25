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
    var deleteTitle: String { "Delete Account \(account.userDescription())" }

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
        account.deleteAccount { [weak self] error in
            MainActor.assumeIsolated {
                guard let self else { return }
                self.deleting = false
                if let error {
                    self.deleteError = "Something went wrong and your account was not deleted. Please try again. (\(error.localizedDescription))"
                } else {
                    self.account.signOut()
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
    @State private var model = SettingsModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        InstrumentPage(showsBanner: UIDevice.current.userInterfaceIdiom == .phone) {
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
                    PlateHeader("Pitch Pipe")
                }

                Section {
                    ActionRow(title: model.accountTitle, busy: model.signingIn, action: model.logInOrOut)
                    if model.isSignedIn {
                        ActionRow(title: "Delete Account", busy: model.deleting, action: model.requestDelete)
                    }
                } header: {
                    PlateHeader("Account")
                } footer: {
                    Text("Log in to back up and synchronize your song list and settings.")
                }

                Section {
                    Button { model.showingPrivacy = true } label: {
                        HStack {
                            Text("Privacy choices").foregroundStyle(Color(uiColor: .label))
                            Spacer()
                            Image(systemName: "chevron.forward")
                                .font(.system(size: 17, weight: .semibold))
                                .foregroundStyle(Color(uiColor: .tertiaryLabel))
                        }
                    }
                    .settingsRow(height: 51)
                } header: {
                    PlateHeader("Privacy")
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
                        PlateHeader("Private Build")
                    }
                }
            }
            .listStyle(.grouped)
            .scrollContentBackground(.hidden)
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
        .alert("Delete Account \(DPSettingsModel.sharedInstance.userString)", isPresented: $model.confirmingDelete) {
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
        .sheet(isPresented: $model.showingPrivacy) { PrivacyChoicesSheet() }
        .signInSheet(isPresented: $model.showingSignIn,
                     onSignIn: model.signedIn(isNewUser:),
                     onDismiss: model.signInDismissed)
    }
}

/// Engraved section label: tracked monospaced capitals in secondary ink.
struct PlateHeader: View {
    let title: String
    init(_ title: String) { self.title = title }

    var body: some View {
        Text(title.uppercased())
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
                if busy { ProgressView() }
            }
        }
        .settingsRow(height: 51)
    }
}

extension View {
    /// A UITableViewCell's frame: clear, 20 pt margins, and (for single-line
    /// rows) the cell's height.
    fileprivate func settingsRow(height: CGFloat? = nil) -> some View {
        frame(minHeight: height)
            .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
            .listRowBackground(Color.clear)
    }

    /// Settings as every tab presents it: a sheet with its own navigation bar.
    func settingsSheet(isPresented: Binding<Bool>) -> some View {
        sheet(isPresented: isPresented) {
            NavigationStack { SettingsScreen() }
        }
    }
}

/// UIKit's subtitle cell, measured.
private enum SettingsMetrics {
    static let subtitleTop: CGFloat = 9
    static let subtitleBottom: CGFloat = 35.0 / 3.0
    static let subtitleSpacing: CGFloat = 3
}
