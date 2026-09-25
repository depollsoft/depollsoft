import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

/// An account the tests control.
@MainActor
private final class FakeAccount {
    var signedIn = false
    var signOuts = 0
    var deleteResult: Error?
    var pendingDelete: ((Error?) -> Void)?

    var service: AccountService {
        AccountService(
            isSignedIn: { [self] in self.signedIn },
            userDescription: { "Google: singer@example.com" },
            signOut: { [self] in self.signedIn = false; self.signOuts += 1 },
            deleteAccount: { [self] completion in self.pendingDelete = completion }
        )
    }
}

@MainActor
final class SettingsModelTests: PitchPerfectTestCase {
    func testSwitchesWriteThroughAndFollowTheStore() {
        let model = SettingsModel(account: FakeAccount().service)
        XCTAssertFalse(model.toggleNotes)
        model.setToggleNotes(true)
        XCTAssertTrue(DPSettingsModel.sharedInstance.toggleNotes)
        XCTAssertTrue(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.ToggleNote"))
        model.setWakeLock(true)
        XCTAssertTrue(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.WakeLock"))
        // A change from elsewhere (another device, through Firestore) shows at once.
        DPSettingsModel.sharedInstance.toggleNotes = false
        XCTAssertFalse(model.toggleNotes)
    }

    func testTheThemeIsStored() {
        let model = SettingsModel(account: FakeAccount().service)
        model.setTheme(2)
        XCTAssertEqual(DPTheme.storedTheme, 2)
        XCTAssertEqual(model.theme, 2)
        model.setTheme(0)
    }

    func testLoggingInOpensSignInAndADismissedSheetStopsTheSpinner() {
        let account = FakeAccount()
        let model = SettingsModel(account: account.service)
        XCTAssertEqual(model.accountTitle, "Log in")
        model.logInOrOut()
        XCTAssertTrue(model.showingSignIn)
        XCTAssertTrue(model.signingIn)
        // The UIKit screen left this spinner turning when sign-in was cancelled.
        model.signInDismissed()
        XCTAssertFalse(model.signingIn)
        XCTAssertFalse(model.showingSignIn)
    }

    func testSigningInShowsLogOutAndDeleteAccount() {
        let account = FakeAccount()
        let model = SettingsModel(account: account.service)
        model.logInOrOut()
        account.signedIn = true
        model.signedIn(isNewUser: true)
        XCTAssertTrue(model.isSignedIn)
        XCTAssertEqual(model.accountTitle, "Log out")
        XCTAssertEqual(model.deleteTitle, "Delete Account Google: singer@example.com")
        model.logInOrOut()
        XCTAssertEqual(account.signOuts, 1)
        XCTAssertFalse(model.isSignedIn)
    }

    func testDeletingTheAccountSignsOutOnSuccess() throws {
        let account = FakeAccount()
        account.signedIn = true
        let model = SettingsModel(account: account.service)
        model.requestDelete()
        XCTAssertTrue(model.confirmingDelete)
        XCTAssertTrue(model.deleting)
        model.confirmDelete()
        XCTAssertFalse(model.confirmingDelete)
        try XCTUnwrap(account.pendingDelete)(nil)
        XCTAssertFalse(model.deleting)
        XCTAssertEqual(account.signOuts, 1)
        XCTAssertFalse(model.isSignedIn)
        XCTAssertNil(model.deleteError)
    }

    func testAFailedDeleteExplainsAndKeepsTheAccount() throws {
        let account = FakeAccount()
        account.signedIn = true
        let model = SettingsModel(account: account.service)
        model.requestDelete()
        model.confirmDelete()
        try XCTUnwrap(account.pendingDelete)(NSError(domain: "test", code: 1, userInfo: [NSLocalizedDescriptionKey: "Offline"]))
        XCTAssertEqual(model.deleteError,
                       "Something went wrong and your account was not deleted. Please try again. (Offline)")
        XCTAssertTrue(model.isSignedIn)
        XCTAssertEqual(account.signOuts, 0)
    }

    func testCancellingADeleteStopsItsSpinner() {
        let model = SettingsModel(account: FakeAccount().service)
        model.requestDelete()
        model.cancelDelete()
        XCTAssertFalse(model.deleting)
        XCTAssertFalse(model.confirmingDelete)
    }

    func testPrivateBuildsShowTheirBuildAndPR() throws {
        let model = SettingsModel(account: FakeAccount().service)
        XCTAssertEqual(model.isPrivateBuild, !(Bundle.main.object(forInfoDictionaryKey: "PrivateBuildNumber") as? String ?? "").isEmpty)
        XCTAssertTrue(model.buildDescription.hasPrefix("Build "))
        XCTAssertTrue(model.buildDescription.contains(" · PR #"))
    }

    func testWakeLockKeepsTheScreenOn() {
        DPSettingsModel.sharedInstance.wakeLock = true
        WakeLock.apply()
        XCTAssertTrue(UIApplication.shared.isIdleTimerDisabled)
        DPSettingsModel.sharedInstance.wakeLock = false
        WakeLock.apply()
        XCTAssertFalse(UIApplication.shared.isIdleTimerDisabled)
    }
}

/// Settings as every tab opens it.
@MainActor
final class PitchPerfectSettingsPresentationTests: PitchPerfectTestCase {
    func testEveryTabOpensSettingsAndDoneClosesIt() throws {
        let app = try launch()
        for index in 0..<4 {
            app.show(tab: index)
            if index == 3 { XCTAssertEqual(app.ui.label(id: "gearshape"), "Settings") }
            app.ui.tap(id: "gearshape")
            settle { app.topPresented !== app.host }
            let sheet = app.sheet
            XCTAssertTrue(app.navigationTitles.contains("Settings"))
            for label in ["Toggle Notes", "Wake Lock", "Theme", "Log in", "Privacy choices"] {
                XCTAssertTrue(sheet.elements.contains { ($0.accessibilityLabel ?? "").contains(label) }, label)
            }
            XCTAssertEqual(sheet.label(id: "checkmark"), "Done")
            sheet.tap(id: "checkmark")
            settle { app.topPresented === app.host }
            XCTAssertEqual(app.models.tab, [.pitchPipe, .notes, .keys, .songs][index])
        }
    }

    func testTheSwitchesPersistAndShowTheirStateWhenReopened() throws {
        let app = try launch()
        app.ui.tap(id: "gearshape")
        settle { app.topPresented !== app.host }
        for label in ["Toggle Notes", "Wake Lock"] {
            let toggle = try XCTUnwrap(app.sheet.elements.first { ($0.accessibilityLabel ?? "").hasPrefix(label) })
            XCTAssertEqual(toggle.accessibilityValue, "0")
            XCTAssertTrue(toggle.accessibilityActivate())
        }
        ScreenCatalog.settle(0.2)
        XCTAssertTrue(DPSettingsModel.sharedInstance.toggleNotes)
        XCTAssertTrue(DPSettingsModel.sharedInstance.wakeLock)
        app.sheet.tap(id: "checkmark")
        settle { app.topPresented === app.host }
        app.ui.tap(id: "gearshape")
        settle { app.topPresented !== app.host }
        for label in ["Toggle Notes", "Wake Lock"] {
            XCTAssertEqual(app.sheet.elements.first { ($0.accessibilityLabel ?? "").hasPrefix(label) }?.accessibilityValue, "1")
        }
        app.sheet.tap(id: "checkmark")
        settle { app.topPresented === app.host }
    }

    func testPrivacyChoicesOpensFromSettings() throws {
        let app = try launch()
        app.ui.tap(id: "gearshape")
        settle { app.topPresented !== app.host }
        app.sheet.tap(label: "Privacy choices")
        settle { app.navigationTitles.contains("Privacy choices") }
        XCTAssertTrue(app.sheet.exists(label: "Usage analytics"))
        XCTAssertTrue(app.sheet.exists(label: "Crash reports"))
        XCTAssertTrue(app.sheet.exists(label: "Save choices"))
    }

    func testTheLoginScreenOffersSignInAndSkips() throws {
        let app = try launch()
        app.showLogin()
        settle { app.navigationTitles.contains("Log In To Pitch Perfect") }
        XCTAssertTrue(app.sheet.exists(label: "Sign up or log in"))
        app.sheet.tap(label: "Skip")
        settle { app.topPresented === app.host }
    }
}

@MainActor
final class PrivacyChoicesModelTests: XCTestCase {
    private var suite: String!
    private var defaults: UserDefaults!

    override func setUp() async throws {
        suite = "PrivacyChoicesModelTests.\(UUID().uuidString)"
        defaults = UserDefaults(suiteName: suite)
    }

    override func tearDown() async throws {
        defaults.removePersistentDomain(forName: suite)
    }

    func testChoicesStartOffAndSaveWhatWasSet() {
        let model = PrivacyChoicesModel(choices: PrivacyChoices(defaults: defaults), adPrivacyRequired: false)
        XCTAssertFalse(model.analytics)
        XCTAssertFalse(model.crashes)
        XCTAssertFalse(model.showsAdPrivacy)
        model.analytics = true
        model.save()
        let saved = PrivacyChoices(defaults: defaults)
        XCTAssertTrue(saved.hasChosen)
        XCTAssertTrue(saved.analytics)
        XCTAssertFalse(saved.crashes)
    }

    func testDecliningBothRecordsAChoice() {
        defaults.set(true, forKey: "telemetry.chosen")
        defaults.set(true, forKey: "telemetry.analytics")
        defaults.set(true, forKey: "telemetry.crashes")
        let model = PrivacyChoicesModel(choices: PrivacyChoices(defaults: defaults), adPrivacyRequired: true)
        XCTAssertTrue(model.analytics)
        XCTAssertTrue(model.showsAdPrivacy)
        model.declineBoth()
        let saved = PrivacyChoices(defaults: defaults)
        XCTAssertTrue(saved.hasChosen)
        XCTAssertFalse(saved.analytics)
        XCTAssertFalse(saved.crashes)
    }
}

@MainActor
final class SongEditorModelTests: XCTestCase {
    func testANewSongStartsInCMajor() {
        let model = SongEditorModel(title: "", key: nil)
        XCTAssertEqual(model.selectedKey.friendlyName(), "C")
        XCTAssertFalse(model.isMinor)
    }

    func testFlippingTheModeKeepsTheSignature() {
        let majors = DPKey.majorKeys() as! [DPKey]
        let model = SongEditorModel(title: "Blue Skies", key: majors[8])
        XCTAssertEqual(model.selectedKey.numAccidentals, 2)
        model.isMinor = true
        XCTAssertEqual(model.selectedKey.numAccidentals, 2, "the relative minor shares the signature")
        XCTAssertTrue(model.minorKeys.contains { $0.isEqual(model.selectedKey) })
    }

    func testABlankTitleIsRequiredAndTypingClearsTheError() {
        let model = SongEditorModel(title: "   ", key: nil)
        let focus = model.titleFocusRequest
        XCTAssertFalse(model.requireTitle())
        XCTAssertTrue(model.titleErrorVisible)
        XCTAssertEqual(model.titleFocusRequest, focus + 1, "the field is focused")
        model.title = "Blue Skies"
        model.titleChanged()
        XCTAssertFalse(model.titleErrorVisible)
        XCTAssertTrue(model.requireTitle())
        XCTAssertEqual(model.trimmedTitle, "Blue Skies")
    }

    func testTappingTheChosenKeyChangesNothing() {
        let majors = DPKey.majorKeys() as! [DPKey]
        let model = SongEditorModel(title: "", key: majors[6])
        model.tap(majors[6])
        XCTAssertTrue(model.selectedKey.isEqual(majors[6]))
        model.tap(majors[7])
        XCTAssertTrue(model.selectedKey.isEqual(majors[7]))
    }
}
