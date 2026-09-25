//
//  SettingsBehaviorTests.swift
//  tagmasterTests
//
//  Settings: the account row either way, clearing the saved lists, the Random
//  Tag filters and where they are kept, privacy choices and the private-build
//  rows. The account is a fake, so no Firebase app is needed.
//

import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

@MainActor
final class SettingsBehaviorTests: TMBehaviorTestCase {

    private var navigator: RecordingNavigator!
    private var signedIn = false
    private var signOuts = 0
    private static let filterKeys = ["random.minRating", "random.minDownloads", "random.sheetMusic", "random.learningTracks"]

    nonisolated override func setUp() {
        super.setUp()
        MainActor.assumeIsolated {
            SettingsBehaviorTests.filterKeys.forEach { UserDefaults.standard.removeObject(forKey: $0) }
            navigator = RecordingNavigator()
            signedIn = false
            signOuts = 0
        }
    }

    nonisolated override func tearDown() {
        MainActor.assumeIsolated {
            if window != nil { dismissPresented() }
            SettingsBehaviorTests.filterKeys.forEach { UserDefaults.standard.removeObject(forKey: $0) }
        }
        super.tearDown()
    }

    private var account: TMAccount {
        TMAccount(isSignedIn: { [unowned self] in signedIn }, signOut: { [unowned self] in
            signOuts += 1
            signedIn = false
        })
    }

    private func settings(build: TMBuildInfo = TMBuildInfo(number: "", pullRequest: "?"))
        -> (TMHostedScreen, TMSettingsModel, UIDriver) {
        let controller = TMScreens.settings(navigator: navigator, account: account, build: build)
        let driver = mountScreen(controller, size: CGSize(width: 375, height: 1600))
        return (controller, controller.settingsModel!, driver)
    }

    // MARK: - Account

    func testSettingsExplainsLoggingInAndOffersItsRandomTagFilters() {
        let (controller, model, driver) = settings()
        XCTAssertEqual(controller.navigationItem.title, "Settings")
        XCTAssertTrue(driver.exists(label: "Account"))
        XCTAssertTrue(driver.exists(label: "Log in to back up and synchronize your tag lists."))
        XCTAssertEqual(model.accountTitle, "Log In")
        XCTAssertEqual(model.accountHint, "Opens the sign-in options")
        XCTAssertEqual(driver.element(label: "Log In")?.accessibilityHint, "Opens the sign-in options")
        XCTAssertEqual(TMSettingsModel.filters.map(\.title),
                       ["Minimum Rating", "Minimum Downloads", "Sheet Music", "Learning Tracks"])
        XCTAssertEqual(TMSettingsModel.filters.first?.choices, ["Any", "1", "2", "3", "4"])
    }

    func testSignedInSettingsOffersLogOutInstead() {
        signedIn = true
        let (_, model, driver) = settings()
        XCTAssertEqual(model.accountTitle, "Log Out")
        XCTAssertEqual(driver.element(label: "Log Out")?.accessibilityHint, "Signs out of Tag Master on this device")
        driver.tap(label: "Log Out")
        XCTAssertEqual(signOuts, 1)
        XCTAssertFalse(model.isSignedIn)
        ScreenCatalog.settle(0.1)
        XCTAssertTrue(driver.exists(label: "Log In"))
    }

    func testLoggingInOpensTheSignInOptions() {
        let (_, model, _) = settings()
        model.accountTapped()
        XCTAssertTrue(model.showingSignIn)
        XCTAssertEqual(signOuts, 0)
        signedIn = true
        model.signInFinished()
        XCTAssertFalse(model.showingSignIn)
        XCTAssertTrue(model.isSignedIn)
        XCTAssertEqual(model.accountTitle, "Log Out")
    }

    // MARK: - Saved tags

    func testClearingIsOfferedOnlyForListsWithTags() {
        let (_, model, driver) = settings()
        XCTAssertEqual(model.favoritesCount, 0)
        XCTAssertEqual(driver.elements(labelPrefix: "Clear Favorites").count, 1)
        model.clearTapped(.favorites)
        XCTAssertNil(model.clearing, "An empty list has nothing to clear")
        XCTAssertEqual(TMSettingsModel.tagCount(1), "1 tag")
        XCTAssertEqual(TMSettingsModel.tagCount(2), "2 tags")
    }

    func testClearingFavoritesAsksFirstThenEmptiesTheList() {
        seedLists(favorite: [1, 2], teachable: [3])
        let (_, model, _) = settings()
        XCTAssertEqual(model.favoritesCount, 2)
        XCTAssertEqual(model.teachableCount, 1)
        model.clearTapped(.favorites)
        let alert = presentedAlert()
        XCTAssertEqual(alert?.title, "Clear Favorites?")
        XCTAssertEqual(alert?.message, "Every favorite will be removed from your list. You can add tags again from any tag's Favorite and Teachable options.")
        XCTAssertEqual(Set(alert?.actions.map { $0.title ?? "" } ?? []), ["Clear Favorites", "Cancel"])
        XCTAssertEqual(alert?.actions.first { $0.title == "Clear Favorites" }?.style, .destructive)
        XCTAssertEqual(DPAppDelegate.favorites(), [1, 2], "Nothing goes before the confirmation")
        alert?.tm_fire("Clear Favorites")
        XCTAssertEqual(DPAppDelegate.favorites(), [])
        XCTAssertEqual(DPAppDelegate.teachable(), [3])
        XCTAssertEqual(model.favoritesCount, 0)
    }

    func testClearingTeachableTagsAsksInItsOwnWords() {
        seedLists(teachable: [3])
        let (_, model, _) = settings()
        model.clearTapped(.teachable)
        let alert = presentedAlert()
        XCTAssertEqual(alert?.title, "Clear Teachable Tags?")
        alert?.tm_fire("Clear Teachable Tags")
        XCTAssertEqual(DPAppDelegate.teachable(), [])
    }

    func testCountsFollowChangesMadeElsewhere() {
        let (_, model, _) = settings()
        TMTagLists.add(9, to: TMTagLists.favoriteKey)
        XCTAssertEqual(model.favoritesCount, 1)
    }

    // MARK: - Random Tag filters

    func testTheFiltersStartAtTheirOldDefaultsAndAreKept() {
        let (_, model, _) = settings()
        XCTAssertEqual(model.filterSelections, [2, 2, 1, 0], "Rating 2, 100 downloads, with sheet music")
        XCTAssertEqual(TMRandomTagFilters.minimumRating, 2.0)
        XCTAssertEqual(TMRandomTagFilters.minimumDownloads, 100)
        XCTAssertEqual(TMRandomTagFilters.sheetMusic, true)
        XCTAssertNil(TMRandomTagFilters.learningTracks)

        model.filterSelections = [0, 4, 2, 1]
        XCTAssertNil(TMRandomTagFilters.minimumRating)
        XCTAssertEqual(TMRandomTagFilters.minimumDownloads, 1000)
        XCTAssertEqual(TMRandomTagFilters.sheetMusic, false)
        XCTAssertEqual(TMRandomTagFilters.learningTracks, true)
        XCTAssertEqual(TMRandomTagFilters.query(),
                       TMTagQuery(learningTracks: true, sheetMusic: false, minimumDownloads: 1000, fieldList: "id"))
    }

    // MARK: - Privacy and private builds

    func testPrivacyChoicesOpensTheConsentScreen() {
        let (_, _, driver) = settings()
        XCTAssertTrue(driver.exists(label: "Privacy"))
        driver.tap(label: "Privacy choices")
        XCTAssertEqual(navigator.privacyPresentations, 1)
    }

    func testPrivateBuildsShowTheirBuildAndCanCopyLogs() {
        let (_, model, driver) = settings(build: TMBuildInfo(number: "4321", pullRequest: "83"))
        XCTAssertTrue(model.build.isPrivate)
        XCTAssertTrue(driver.exists(label: "Build 4321 · PR #83"))
        UIPasteboard.general.string = ""
        model.copyLogs()
        XCTAssertEqual(UIPasteboard.general.string?.hasPrefix("Build 4321 · PR #83\n\n"), true)
    }

    func testStoreBuildsHideThePrivateBuildRows() {
        let (_, model, driver) = settings()
        XCTAssertFalse(model.build.isPrivate)
        XCTAssertFalse(driver.exists(label: "Copy Logs"))
    }
}
