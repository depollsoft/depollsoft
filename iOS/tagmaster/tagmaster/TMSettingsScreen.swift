//
//  TMSettingsScreen.swift
//  tagmaster
//
//  Settings: the account, clearing the saved lists, the filters Random Tag
//  applies, privacy choices, and on private builds the build and its logs.
//

import SwiftUI
import UIKit
import FirebaseAuth

/// Who is signed in, behind a seam so tests need no Firebase app.
struct TMAccount {
    var isSignedIn: () -> Bool
    var signOut: () -> Void

    static let firebase = TMAccount(
        isSignedIn: { Auth.auth().currentUser != nil },
        signOut: {
            do { try Auth.auth().signOut() } catch { DPAppLog.log("Settings: sign out failed: \(error.localizedDescription)") }
        })
}

/// The build a private (preview) install came from, from its Info.plist.
struct TMBuildInfo {
    var number: String
    var pullRequest: String

    static var bundle: TMBuildInfo {
        TMBuildInfo(number: Bundle.main.object(forInfoDictionaryKey: "PrivateBuildNumber") as? String ?? "",
                    pullRequest: Bundle.main.object(forInfoDictionaryKey: "PrivatePRNumber") as? String ?? "?")
    }

    var isPrivate: Bool { !number.isEmpty }
    var summary: String { "Build \(number) · PR #\(pullRequest)" }
}

@MainActor
@Observable
final class TMSettingsModel {
    static let filters = [
        TMFilter(title: "Minimum Rating", choices: TMRandomTagFilters.minimumRatingChoices),
        TMFilter(title: "Minimum Downloads", choices: TMRandomTagFilters.minimumDownloadsChoices),
        TMFilter(title: "Sheet Music", choices: TMRandomTagFilters.presenceChoices),
        TMFilter(title: "Learning Tracks", choices: TMRandomTagFilters.presenceChoices),
    ]

    enum Clearing: Identifiable {
        case favorites, teachable
        var id: Self { self }
    }

    var navigator: TMNavigator?
    let account: TMAccount
    let build: TMBuildInfo

    private(set) var isSignedIn = false
    private(set) var favoritesCount = 0
    private(set) var teachableCount = 0
    var showingSignIn = false
    var clearing: Clearing?

    /// Random Tag's filters, as the index of each choice, in `filters` order.
    var filterSelections: [Int] {
        didSet {
            TMRandomTagFilters.minimumRatingIndex = filterSelections[0]
            TMRandomTagFilters.minimumDownloadsIndex = filterSelections[1]
            TMRandomTagFilters.sheetMusicIndex = filterSelections[2]
            TMRandomTagFilters.learningTracksIndex = filterSelections[3]
        }
    }

    @ObservationIgnored private var observer: NSObjectProtocol?

    init(navigator: TMNavigator? = nil, account: TMAccount = .firebase, build: TMBuildInfo = .bundle,
         center: NotificationCenter = .default) {
        self.navigator = navigator
        self.account = account
        self.build = build
        filterSelections = [TMRandomTagFilters.minimumRatingIndex, TMRandomTagFilters.minimumDownloadsIndex,
                            TMRandomTagFilters.sheetMusicIndex, TMRandomTagFilters.learningTracksIndex]
        refresh()
        observer = center.addObserver(forName: .userDataChanged, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.refresh() }
        }
    }

    deinit {
        if let observer { NotificationCenter.default.removeObserver(observer) }
    }

    func refresh() {
        isSignedIn = account.isSignedIn()
        favoritesCount = TMTagLists.ids(for: TMTagLists.favoriteKey).count
        teachableCount = TMTagLists.ids(for: TMTagLists.teachableKey).count
    }

    // MARK: - Account

    var accountTitle: String { isSignedIn ? "Log Out" : "Log In" }
    var accountHint: String { isSignedIn ? "Signs out of Tag Master on this device" : "Opens the sign-in options" }

    /// Signs out when signed in; otherwise opens the sign-in options.
    func accountTapped() {
        if isSignedIn {
            account.signOut()
            refresh()
        } else {
            showingSignIn = true
        }
    }

    func signInFinished() {
        showingSignIn = false
        refresh()
    }

    // MARK: - Saved tags

    func count(for list: Clearing) -> Int { list == .favorites ? favoritesCount : teachableCount }

    func clearTapped(_ list: Clearing) {
        guard count(for: list) > 0 else { return }
        clearing = list
    }

    func confirmClear(_ list: Clearing) {
        clearing = nil
        TMTagLists.setIds([], for: list == .favorites ? TMTagLists.favoriteKey : TMTagLists.teachableKey)
        refresh()
    }

    // MARK: - Privacy and build

    func privacyChoices() { navigator?.presentPrivacyChoices() }

    func copyLogs() {
        DPAppLog.log("Settings: copied app logs")
        UIPasteboard.general.string = "\(build.summary)\n\n\(DPAppLog.contents())"
    }

    static func tagCount(_ count: Int) -> String { "\(count) \(count == 1 ? "tag" : "tags")" }
}

struct TMSettingsScreen: View {
    @Bindable var model: TMSettingsModel
    @Environment(\.tmTintDimmed) private var dimmed

    var body: some View {
        List {
            Section {
                actionRow(model.accountTitle) { model.accountTapped() }
                    .accessibilityHint(model.accountHint)
            } header: { TMSectionHeader("Account") } footer: { TMSectionFooter("Log in to back up and synchronize your tag lists.") }

            Section {
                clearRow("Clear Favorites", .favorites)
                clearRow("Clear Teachable Tags", .teachable)
            } header: { TMSectionHeader("Saved Tags") } footer: {
                TMSectionFooter("Clearing a list removes every tag from it on this device and, when logged in, on your other devices.")
            }

            Section {
                ForEach(Array(TMSettingsModel.filters.enumerated()), id: \.offset) { index, filter in
                    TMFilterRow(filter: filter, selection: $model.filterSelections[index])
                }
            } header: { TMSectionHeader("Random Tag Filters") } footer: { TMSectionFooter("Random Tag only picks tags that match these filters.") }

            Section {
                actionRow("Privacy choices") { model.privacyChoices() }
            } header: { TMSectionHeader("Privacy") }

            if model.build.isPrivate {
                Section {
                    Text(model.build.summary).font(TMTheme.font(.body))
                    actionRow("Copy Logs") { model.copyLogs() }
                } header: { TMSectionHeader("Private Build") }
            }
        }
        .listStyle(.insetGrouped)
        .tmInsetGroupedMetrics()
        .scrollContentBackground(.hidden)
        .background { TMScreenBackground(grouped: true) }
        .onAppear { model.refresh() }
        .sheet(isPresented: $model.showingSignIn) { TMSignInSheet(finished: model.signInFinished) }
        .alert(model.clearing == .teachable ? "Clear Teachable Tags?" : "Clear Favorites?",
               isPresented: Binding(get: { model.clearing != nil }, set: { if !$0 { model.clearing = nil } }),
               presenting: model.clearing) { list in
            Button(list == .favorites ? "Clear Favorites" : "Clear Teachable Tags", role: .destructive) {
                model.confirmClear(list)
            }
            Button("Cancel", role: .cancel) { model.clearing = nil }
        } message: { list in
            Text(list == .favorites
                 ? "Every favorite will be removed from your list. You can add tags again from any tag's Favorite and Teachable options."
                 : "Every teachable tag will be removed from your list. You can mark tags as teachable again from any tag's Favorite and Teachable options.")
        }
    }

    private func actionRow(_ title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(TMTheme.font(.body))
                .foregroundStyle(TMTheme.tint(DPAppDelegate.accentColor(), dimmed: dimmed))
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
        }
        .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
    }

    private func clearRow(_ title: String, _ list: TMSettingsModel.Clearing) -> some View {
        let count = model.count(for: list)
        return Button { model.clearTapped(list) } label: {
            HStack {
                Text(title)
                    .foregroundStyle(count > 0 ? TMTheme.tint(.systemRed, dimmed: dimmed) : Color(uiColor: .tertiaryLabel))
                Spacer()
                Text(TMSettingsModel.tagCount(count)).foregroundStyle(Color(uiColor: .secondaryLabel))
            }
            .font(TMTheme.font(.body))
            .contentShape(Rectangle())
        }
        .disabled(count == 0)
        .listRowInsets(EdgeInsets(top: 0, leading: 20, bottom: 0, trailing: 20))
    }
}

/// The sign-in options, closing once someone has signed in or the user cancels.
struct TMSignInSheet: View {
    let finished: () -> Void

    var body: some View {
        #if canImport(FirebaseAuthSwiftUI)
        TagMasterAuthView(onAuthStateChanged: finished, onDismiss: finished)
        #else
        Text("Sign-in is unavailable in this build.").onAppear(perform: finished)
        #endif
    }
}
