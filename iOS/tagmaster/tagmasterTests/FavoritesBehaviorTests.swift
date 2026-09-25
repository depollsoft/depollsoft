//
//  FavoritesBehaviorTests.swift
//  tagmasterTests
//
//  Home: its destinations, the Lists group, the Favorites and the credits.
//  The model is exercised directly for its rules; the mounted SwiftUI screen
//  is driven through its accessibility tree for what the user sees and taps.
//  Favourites and lists are seeded through the same defaults keys the app
//  reads, so the storage path under test is the production one.
//

import XCTest
import UIKit
@testable import tagmaster

@MainActor
final class FavoritesBehaviorTests: TMBehaviorTestCase {

    private var navigator: RecordingNavigator!
    private var controller: TMHostedScreen!
    private var driver: UIDriver!

    private var model: TMHomeModel { controller.listing as! TMHomeModel }

    private static let twoLists: [(key: String, name: String, ids: [Int])] = [
        (key: "afterglow-set-k3f9", name: "Afterglow set", ids: [669, 1478]),
        (key: "chorus-warmups-list", name: "Chorus warmups", ids: [122])
    ]

    nonisolated override func tearDown() {
        MainActor.assumeIsolated {
            if window != nil { dismissPresented() }
            driver = nil
            controller = nil
            navigator = nil
        }
        super.tearDown()
    }

    @discardableResult
    private func home(favorites: [Int] = [],
                      lists: [(key: String, name: String, ids: [Int])] = [],
                      catalog: TMCatalog = TMFixtureCatalog().catalog) -> TMHomeModel {
        // Every favourite row resolves from the cache, never the catalog.
        favorites.forEach { seedCachedTag(id: Int32($0), title: "Tag \($0)") }
        if !favorites.isEmpty || !lists.isEmpty { seedLists(favorite: favorites, lists: lists) }
        navigator = RecordingNavigator()
        controller = TMScreens.home(navigator: navigator, catalog: catalog)
        driver = mountScreen(controller)
        return model
    }

    private func index(of label: String) -> Int? { driver.labels.firstIndex(of: label) }

    // MARK: - Home list contents

    func testHomeListsEveryNavigationRowInOrder() {
        home()
        XCTAssertEqual(TMHomeModel.navigationTitles, ["Browse", "Random Tag", "Open Tag"])
        let positions = TMHomeModel.navigationTitles.compactMap(index(of:))
        XCTAssertEqual(positions.count, 3)
        XCTAssertEqual(positions, positions.sorted(), "Browse, Random Tag, Open Tag, top to bottom")
    }

    func testHomeNamesItsGroups() {
        home(favorites: [669])
        let lists = index(of: "Lists"), favorites = index(of: "Favorites"), open = index(of: "Open Tag")
        XCTAssertNotNil(lists)
        XCTAssertNotNil(favorites)
        XCTAssertLessThan(open ?? .max, lists ?? 0, "The destinations have no header of their own")
        XCTAssertLessThan(lists ?? .max, favorites ?? 0)
    }

    func testHomeExplainsHowToAddFavoritesWhenThereAreNone() {
        home()
        XCTAssertTrue(model.favorites.isEmpty)
        XCTAssertTrue(driver.exists(label: TMHomeModel.noFavoritesFooter))
        XCTAssertEqual(TMHomeModel.noFavoritesFooter,
                       "No favorites yet. Open a tag and use Favorite and Teachable options to add a favorite.")
    }

    func testTheFavoritesExplanationGoesOnceThereIsAFavorite() {
        home(favorites: [669])
        XCTAssertFalse(driver.exists(label: TMHomeModel.noFavoritesFooter))
    }

    // MARK: - The Lists group

    func testListsGroupHoldsTeachableThenEveryCustomListThenNewList() {
        home(lists: FavoritesBehaviorTests.twoLists)
        let ids = driver.identifiers.filter { $0.hasPrefix("home.list") }
        XCTAssertEqual(ids, ["home.lists.teachable", "home.list.afterglow-set-k3f9",
                             "home.list.chorus-warmups-list", "home.lists.new"])
        XCTAssertEqual(model.listNames["afterglow-set-k3f9"], "Afterglow set")
        XCTAssertEqual(model.listCounts, ["afterglow-set-k3f9": 2, "chorus-warmups-list": 1])
        XCTAssertEqual(model.teachableCount, 0)
        XCTAssertEqual(driver.label(id: "home.lists.teachable")?.contains("0 tags"), true)
        XCTAssertEqual(driver.label(id: "home.list.afterglow-set-k3f9")?.contains("2 tags"), true)
        XCTAssertEqual(driver.label(id: "home.list.chorus-warmups-list")?.contains("1 tag"), true)
        XCTAssertEqual(TMHomeModel.countLabel(1), "1 tag")
        XCTAssertEqual(TMHomeModel.countLabel(0), "0 tags")
    }

    func testListsGroupIsJustTeachableAndNewListWhenNoListsExist() {
        home()
        XCTAssertEqual(driver.identifiers.filter { $0.hasPrefix("home.list") },
                       ["home.lists.teachable", "home.lists.new"])
        XCTAssertEqual(driver.label(id: "home.lists.new")?.contains("New list…"), true)
    }

    func testTappingTeachableAndACustomListOpensThoseScreens() {
        home(lists: FavoritesBehaviorTests.twoLists)
        driver.tap(id: "home.lists.teachable")
        driver.tap(id: "home.list.chorus-warmups-list")
        XCTAssertEqual(navigator.destinations, [.teachable, .list("chorus-warmups-list")])
    }

    func testTheRealNavigatorPushesTheListScreens() {
        seedLists(lists: FavoritesBehaviorTests.twoLists)
        let home = TMScreens.home()
        mountInNavigation(home)
        ScreenCatalog.settle(0.2)
        let driver = UIDriver(window)
        driver.tap(id: "home.lists.teachable")
        driver.tap(id: "home.list.chorus-warmups-list")
        let path = home.router?.path ?? []
        XCTAssertEqual(path.count, 2)
        XCTAssertTrue(path[0].opensList(key: TMTagLists.teachableKey))
        XCTAssertTrue(path[1].opensList(key: "chorus-warmups-list"))
        XCTAssertTrue(TMScreens.isHome(home))
    }

    func testReorderingAListRowReordersTheStoredLists() {
        home(lists: FavoritesBehaviorTests.twoLists)
        // SwiftUI reports a drop as the gap below the last custom list.
        model.moveList(from: [0], to: 2)
        XCTAssertEqual(TMTagLists.customKeys(), ["chorus-warmups-list", "afterglow-set-k3f9"])
        XCTAssertEqual(model.customKeys, ["chorus-warmups-list", "afterglow-set-k3f9"])
        model.moveList(from: [1], to: 0)
        XCTAssertEqual(TMTagLists.customKeys(), ["afterglow-set-k3f9", "chorus-warmups-list"])
    }

    func testDeletingAListRowAsksBeforeAnythingIsLost() {
        home(lists: FavoritesBehaviorTests.twoLists)
        model.confirmDelete("afterglow-set-k3f9")
        XCTAssertEqual(model.deletePrompt?.title, "Delete “Afterglow set”?")
        XCTAssertEqual(model.deletePrompt?.message,
                       "This removes the list and its 2 tags from your lists. Tags stay in the catalog.")
        XCTAssertEqual(TMTagLists.customKeys().count, 2, "Nothing goes before the confirmation is answered")

        let alert = presentedAlert()
        XCTAssertEqual(alert?.title, "Delete “Afterglow set”?")
        XCTAssertEqual(Set(alert?.actions.map { $0.title ?? "" } ?? []), ["Cancel", "Delete"])
        XCTAssertEqual(alert?.actions.first { $0.title == "Delete" }?.style, .destructive)
        XCTAssertEqual(alert?.actions.first { $0.title == "Cancel" }?.style, .cancel)
        alert?.tm_fire("Delete")
        XCTAssertEqual(TMTagLists.customKeys(), ["chorus-warmups-list"])
        XCTAssertEqual(model.customKeys, ["chorus-warmups-list"])
    }

    func testCancellingADeleteKeepsTheList() {
        home(lists: FavoritesBehaviorTests.twoLists)
        model.confirmDelete("afterglow-set-k3f9")
        presentedAlert()?.tm_fire("Cancel")
        ScreenCatalog.settle(0.1)
        XCTAssertNil(model.deletePrompt)
        XCTAssertEqual(TMTagLists.customKeys().count, 2)
    }

    func testDeletingFromTheEditButtonLeavesEditModeAlone() {
        home(lists: FavoritesBehaviorTests.twoLists)
        driver.tap(label: "Edit")
        XCTAssertTrue(model.isEditing)
        model.deleteList("afterglow-set-k3f9")
        XCTAssertTrue(model.isEditing, "A red-circle delete does not end the edit session")
        XCTAssertEqual(TMTagLists.customKeys(), ["chorus-warmups-list"])
        model.deleteList("chorus-warmups-list")
        XCTAssertFalse(model.isEditing, "…until nothing is left to edit")
        ScreenCatalog.settle(0.1)
        XCTAssertFalse(driver.isEnabled(label: "Edit"))
        XCTAssertFalse(driver.exists(label: "Done"))
    }

    func testAListRowOffersRenameAndDeleteFromItsMenu() {
        home(lists: FavoritesBehaviorTests.twoLists)
        model.rename("afterglow-set-k3f9")
        XCTAssertEqual(model.namePrompt?.title, "Rename list")
        XCTAssertEqual(model.namePrompt?.text, "Afterglow set")
        model.namePrompt = nil
        model.confirmDelete("chorus-warmups-list")
        XCTAssertEqual(model.deletePrompt?.key, "chorus-warmups-list")
    }

    func testNewListRowNamesAndCreatesTheListInPlace() {
        home()
        driver.tap(id: "home.lists.new")
        let alert = presentedAlert()
        XCTAssertEqual(alert?.title, "New list")
        XCTAssertEqual(alert?.message, "For example “Easy tags” or “High and lows”")
        XCTAssertEqual(alert?.textFields?.first?.placeholder, "List name")
        XCTAssertEqual(alert?.actions.map { $0.title ?? "" }, ["Cancel", "Create"])

        alert?.tm_type("Afterglow set")
        ScreenCatalog.settle(0.1)
        alert?.tm_fire("Create")
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(TMTagLists.customKeys().map { TMTagLists.name(for: $0) }, ["Afterglow set"])
        XCTAssertEqual(model.customKeys.map { model.listNames[$0] }, ["Afterglow set"])
        XCTAssertFalse(model.pendingRefresh, "Home's own change leaves nothing waiting behind it")
    }

    func testRenamingAListFromHomeRewritesItsRow() {
        home(lists: FavoritesBehaviorTests.twoLists)
        model.rename("afterglow-set-k3f9")
        let alert = presentedAlert()
        XCTAssertEqual(alert?.title, "Rename list")
        XCTAssertEqual(alert?.textFields?.first?.text, "Afterglow set")
        alert?.tm_type("Afterglow encore")
        ScreenCatalog.settle(0.1)
        alert?.tm_fire("Rename")
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(TMTagLists.name(for: "afterglow-set-k3f9"), "Afterglow encore")
        XCTAssertEqual(driver.label(id: "home.list.afterglow-set-k3f9")?.contains("Afterglow encore"), true)
    }

    // MARK: - Seeded favourites

    func testFavoritesSeededInDefaultsBecomeHomeRowsInOrder() {
        let seeded = [669, 1478, 122]
        home(favorites: seeded)
        XCTAssertEqual(DPAppDelegate.favorites(), seeded,
                       "The lists defaults key is the app's own favourites storage")
        XCTAssertEqual(model.favorites, seeded)
        // The list is lazy: bring the last favourite into view before reading the rows.
        model.didStep(to: 122)
        ScreenCatalog.settle(0.3)
        let rows = driver.elements(labelPrefix: "Tag ").compactMap(\.accessibilityLabel)
        let shown = seeded.filter { id in rows.contains { $0.contains("Tag ID \(id)") } }
        XCTAssertTrue(shown.contains(122), "The last favourite can be brought into view")
        let order = shown.compactMap { id in rows.firstIndex { $0.contains("Tag ID \(id)") } }
        XCTAssertEqual(order, order.sorted(), "Rows keep the stored order")
    }

    func testAFavoriteRowAnnouncesItsTagBeforeTheTagHasLoaded() {
        XCTAssertEqual(TMTagRowContent(tagId: 990_001, tag: nil).accessibilityLabel,
                       "Tag 990001. Open to load details.")
        XCTAssertEqual(TMTagRowContent(tagId: 990_001, tag: nil).details, "Open tag to load details")
        XCTAssertEqual(TMTagRowContent(tagId: 990_001, tag: nil).title, "Tag")
    }

    func testFavoriteRowDescribesItsTagOnceLoadedFromCache() {
        seedCachedTag(id: 669, title: "Cheer Up, Charlie")
        seedLists(favorite: [669])
        navigator = RecordingNavigator()
        controller = TMScreens.home(navigator: navigator)
        driver = mountScreen(controller)
        let label = driver.elements(labelPrefix: "Cheer Up, Charlie").first?.accessibilityLabel
        XCTAssertEqual(label?.contains("Tag ID 669"), true)
        XCTAssertEqual(label?.contains("Sheet music available"), true)
        XCTAssertEqual(label?.contains("Learning tracks available"), true)
    }

    // MARK: - Editing

    func testDeletingAFavoriteRowRemovesThatFavorite() {
        home(favorites: [669, 1478, 122])
        model.removeFavorites(at: [1])
        XCTAssertEqual(DPAppDelegate.favorites(), [669, 122])
        XCTAssertEqual(model.favorites, [669, 122])
    }

    func testReorderingAFavoriteRowReordersTheStoredList() {
        home(favorites: [669, 1478, 122])
        model.moveFavorite(from: [0], to: 3)
        XCTAssertEqual(DPAppDelegate.favorites(), [1478, 122, 669])
        model.moveFavorite(from: [2], to: 0)
        XCTAssertEqual(DPAppDelegate.favorites(), [669, 1478, 122])
    }

    func testEditButtonIsEnabledForFavoritesOrForListsOfTheirOwn() {
        home()
        XCTAssertFalse(driver.isEnabled(label: "Edit"))
        clearLists()
        home(favorites: [669])
        XCTAssertTrue(driver.isEnabled(label: "Edit"))
        clearLists()
        home(lists: FavoritesBehaviorTests.twoLists)
        XCTAssertTrue(driver.isEnabled(label: "Edit"), "Lists alone are worth an Edit button")
    }

    func testTheEditButtonDrivesTheListsEditMode() {
        home(favorites: [669])
        driver.tap(label: "Edit")
        XCTAssertTrue(model.isEditing)
        driver.tap(label: "Done")
        XCTAssertFalse(model.isEditing)
    }

    // MARK: - Navigation

    func testHomeOffersSearchAndSettingsInItsNavigationBar() {
        home()
        XCTAssertEqual(controller.navigationItem.title, "Tag Master")
        XCTAssertEqual(controller.navigationItem.largeTitleDisplayMode, .always)
        let edit = driver.element(label: "Edit")?.accessibilityFrame ?? .null
        let search = driver.element(label: "Search")?.accessibilityFrame ?? .null
        let settings = driver.element(label: "Settings")?.accessibilityFrame ?? .null
        XCTAssertLessThan(edit.midX, settings.midX, "Edit leads")
        XCTAssertLessThan(settings.midX, search.midX, "Search stays outermost")
        driver.tap(label: "Search")
        driver.tap(label: "Settings")
        XCTAssertEqual(navigator.destinations, [.search, .settings])
        XCTAssertEqual(controller.navigationItem.backButtonTitle, "Home")
        XCTAssertFalse(driver.exists(label: "Settings") && TMHomeModel.navigationTitles.contains("Settings"))
    }

    func testTappingTheBarIconsOpensSettingsAndSearch() {
        home()
        driver.tap(label: "Settings")
        driver.tap(label: "Search")
        XCTAssertEqual(navigator.destinations, [.settings, .search])
    }

    func testBrowseRowOpensBrowse() {
        home()
        driver.tap(label: "Browse")
        XCTAssertEqual(navigator.destinations, [.browse])
    }

    func testTappingAFavoriteOpensThatTag() {
        home(favorites: [669, 1478])
        let row = driver.elements(labelPrefix: "Tag 669").first
        XCTAssertNotNil(row)
        XCTAssertTrue(row?.accessibilityActivate() ?? false)
        ScreenCatalog.settle(0.05)
        XCTAssertEqual(navigator.shownTags, [669])
    }

    // MARK: - Open Tag

    func testOpenTagAsksForAnIdAndOpensIt() {
        home()
        driver.tap(label: "Open Tag")
        let alert = presentedAlert()
        XCTAssertEqual(alert?.title, "Open Tag")
        XCTAssertEqual(alert?.message, "Enter Tag ID")
        XCTAssertEqual(alert?.textFields?.count, 1)
        XCTAssertEqual(alert?.textFields?.first?.keyboardType, .decimalPad)
        XCTAssertEqual(Set(alert?.actions.map { $0.title ?? "" } ?? []), ["Cancel", "Open"])
        XCTAssertEqual(alert?.actions.first { $0.title == "Cancel" }?.style, .cancel)
        alert?.tm_type("1809")
        ScreenCatalog.settle(0.1)
        alert?.tm_fire("Open")
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(navigator.shownTags, [1809])
    }

    func testOpenTagKeepsOnlyDigitsAndIgnoresAnEmptyOrZeroId() {
        XCTAssertEqual(TMOpenTagPrompt.filter("12a3-4 "), "1234")
        XCTAssertEqual(TMOpenTagPrompt.filter("abc"), "")
        XCTAssertNil(TMOpenTagPrompt(text: "").tagId)
        XCTAssertNil(TMOpenTagPrompt(text: "0").tagId)
        XCTAssertNil(TMOpenTagPrompt(text: "99999999999").tagId, "Beyond a tag id's range")
        XCTAssertEqual(TMOpenTagPrompt(text: "42").tagId, 42)

        home()
        model.openTagPrompt = TMOpenTagPrompt(text: "")
        model.commitOpenTag()
        XCTAssertEqual(navigator.shownTags, [])
        XCTAssertNil(model.openTagPrompt)
    }

    // MARK: - Random Tag

    func testRandomTagPicksFromEverythingMatchingTheFilters() {
        let fixtures = TMFixtureCatalog(available: 45)
        home(catalog: fixtures.catalog)
        model.randomTag()
        spinUntil("a tag opens") { !self.navigator.shownTags.isEmpty }
        XCTAssertEqual(fixtures.queries.count, 2, "One query counts the matches, one fetches the pick")
        XCTAssertEqual(fixtures.queries.first?.count, 0)
        XCTAssertEqual(fixtures.queries.last?.count, 1)
        XCTAssertEqual(fixtures.queries.first?.query, TMRandomTagFilters.query())
        XCTAssertEqual(fixtures.queries.first?.query.fieldList, "id")
        let start = fixtures.queries.last?.start ?? -1
        XCTAssertTrue((0..<45).contains(start))
        XCTAssertEqual(navigator.shownTags, [3000 + start])
        XCTAssertFalse(model.randomBusy)
    }

    func testRandomTagShowsItsProgressOnItsOwnRowAndIgnoresRepeatTaps() {
        let fixtures = TMFixtureCatalog(available: 45)
        let gate = DispatchSemaphore(value: 0)
        fixtures.gate = gate
        home(catalog: fixtures.catalog)
        driver.tap(label: "Random Tag")
        XCTAssertTrue(model.randomBusy)
        spinUntil("the row says it is loading") { self.driver.exists(label: "Random Tag, loading") }
        model.randomTag()
        gate.signal()
        gate.signal()
        spinUntil("a tag opens") { !self.navigator.shownTags.isEmpty }
        XCTAssertEqual(fixtures.queries.count, 2, "A second tap while busy starts nothing")
        spinUntil("the row settles") { self.driver.exists(label: "Random Tag") }
    }

    func testRandomTagExplainsWhenNothingMatchesAndCanRetry() {
        let fixtures = TMFixtureCatalog(available: 0)
        home(catalog: fixtures.catalog)
        model.randomTag()
        spinUntil("a recovery is offered") { self.model.recovery != nil }
        XCTAssertEqual(model.recovery?.message,
                       "No tag could be selected. Check your connection or adjust Random Tag Filters in Settings, then try again.")
        let alert = presentedAlert()
        XCTAssertEqual(alert?.title, "Couldn't complete request")
        XCTAssertEqual(alert?.actions.map { $0.title ?? "" }.sorted(), ["Cancel", "Retry"])
        fixtures.available = 3
        alert?.tm_fire("Retry")
        spinUntil("the retry opens a tag") { !self.navigator.shownTags.isEmpty }
    }

    func testRandomTagSaysWhenTheCatalogCannotBeReached() {
        home(catalog: TMFixtureCatalog(available: nil).catalog)
        model.randomTag()
        spinUntil("a recovery is offered") { self.model.recovery != nil }
        XCTAssertEqual(model.recovery?.message,
                       "A random tag couldn't be loaded. Check your connection and try again.")
    }

    // MARK: - Credits

    func testCreditsLinkToTheirPages() {
        home()
        let expected = ["home.credit.attribution": "https://www.barbershoptags.com",
                        "home.credit.developer": "https://apps.depoll.com",
                        "home.credit.terms": "https://apps.depoll.com/terms-of-use",
                        "home.credit.donate": "https://www.davidpoll.com/applications/tag-master/donate"]
        for (id, url) in expected.sorted(by: { $0.key < $1.key }) {
            XCTAssertTrue(driver.traits(id: id).contains(.link), "\(id) reads as a link")
            driver.tap(id: id)
            XCTAssertEqual(navigator.openedURLs.last?.absoluteString, url)
        }
        let year = Calendar.current.component(.year, from: Date())
        XCTAssertEqual(driver.label(id: "home.credit.developer"), "DepollSoft © \(year)")
    }

    // MARK: - Reacting to changes elsewhere

    func testHomeReportsItsFavoritesAsTheListSourceForStepping() {
        home(favorites: [669, 1478, 122])
        XCTAssertEqual(controller.tm_listedTagIds().map(\.intValue), [669, 1478, 122])
        controller.tm_didStep(toTagId: 1478)
        XCTAssertEqual(model.selectedTagId, 1478)
        XCTAssertEqual(model.scrollTarget, 1478)
    }

    func testAListArrivingMidScrollIsShownOnceTheScrollEnds() {
        home(lists: [FavoritesBehaviorTests.twoLists[0]])
        model.isScrolling = true
        _ = TMTagLists.createList(named: "Chorus warmups")
        XCTAssertEqual(model.customKeys.count, 1, "Home leaves its rows alone while the user scrolls")
        XCTAssertTrue(model.pendingRefresh, "…but remembers what it has not shown")
        model.isScrolling = false
        XCTAssertFalse(model.pendingRefresh)
        XCTAssertEqual(model.customKeys.map { model.listNames[$0] }, ["Afterglow set", "Chorus warmups"])
    }

    func testAListAddedElsewhereAppearsOnHome() {
        home(lists: [FavoritesBehaviorTests.twoLists[0]])
        _ = TMTagLists.createList(named: "Chorus warmups")
        ScreenCatalog.settle(0.1)
        XCTAssertEqual(model.customKeys.count, 2)
        XCTAssertTrue(driver.identifiers.contains { $0.hasPrefix("home.list.chorus-warmups") })
    }

    func testTheOpenFavoriteStaysLitBesideTheDetail() {
        home(favorites: [669, 1478])
        navigator.isExpandedSplit = true
        navigator.currentSplitTagId = 1478
        NotificationCenter.default.post(name: .TMTagSelectionDidChange, object: nil)
        XCTAssertTrue(model.expanded)
        XCTAssertEqual(model.selectedTagId, 1478)
        controller.tm_didStep(toTagId: 1478)
        ScreenCatalog.settle(0.3)
        XCTAssertEqual(driver.elements(labelPrefix: "Tag 1478").first?.accessibilityTraits.contains(.selected), true)
        XCTAssertEqual(driver.elements(labelPrefix: "Tag 669").first?.accessibilityTraits.contains(.selected), false)
        navigator.currentSplitTagId = 42
        model.syncSelection()
        XCTAssertNil(model.selectedTagId, "A tag that is not a favorite lights no row")
    }
}

extension UIAlertController {
    /// Types into the alert's field the way the keyboard would, so the
    /// validation that gates the confirming action actually runs.
    func tm_type(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let field = textFields?.first else {
            XCTFail("This alert has no text field", file: file, line: line)
            return
        }
        field.text = text
        field.sendActions(for: .editingChanged)
    }

    /// Runs the handler of the named action, the way tapping it would.
    func tm_fire(_ title: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let action = actions.first(where: { $0.title == title }) else {
            XCTFail("No “\(title)” action in this alert", file: file, line: line)
            return
        }
        XCTAssertTrue(action.isEnabled, "“\(title)” is disabled", file: file, line: line)
        typealias Handler = @convention(block) (UIAlertAction) -> Void
        guard action.responds(to: Selector(("handler"))), let raw = action.value(forKey: "handler") else {
            XCTFail("UIAlertAction no longer exposes its handler", file: file, line: line)
            return
        }
        let handler = unsafeBitCast(raw as AnyObject, to: Handler.self)
        handler(action)
    }
}
