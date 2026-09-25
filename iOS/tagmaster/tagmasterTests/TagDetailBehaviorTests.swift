//
//  TagDetailBehaviorTests.swift
//  tagmasterTests
//
//  In-process replacements for TagDetailUITests and the detail-screen half of
//  TagMasterPolishUITests. The XCUITests reached the detail screen over the
//  live catalog; these seed the production tag cache instead, so the same
//  controller renders the same content deterministically.
//

import XCTest
import UIKit
@testable import tagmaster

/// Captures what the detail screen tries to present instead of presenting it,
/// so the action sheets and the share sheet can be inspected in-process.
private final class TMPresentationCapturingDetail: DPTagViewController {
    var presented: [UIViewController] = []
    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool,
                          completion: (() -> Void)? = nil) {
        presented.append(viewControllerToPresent)
        completion?()
    }
}

private final class TMPresentationCapturingSummary: DPTagSummaryController {
    var presented: [UIViewController] = []
    override func present(_ viewControllerToPresent: UIViewController, animated flag: Bool,
                          completion: (() -> Void)? = nil) {
        presented.append(viewControllerToPresent)
        completion?()
    }
}

final class TagDetailBehaviorTests: TMBehaviorTestCase {

    // MARK: - Reaching the detail screen
    // Replaces TagDetailUITests.testCanNavigateToTagDetail /
    // testTagDetailHasContent, which skipped whenever navigation failed and
    // otherwise only asserted the app was still in the foreground.

    func testDetailLeavesItsLoadingStateAndShowsItsPagesOnceTheTagIsAvailable() {
        seedCachedTag(id: 1809, title: "Lost")
        let detail = loadedDetail(tagId: 1809)

        XCTAssertEqual(detail.tagId, 1809)
        XCTAssertEqual((detail.value(forKey: "tag") as? DPTag)?.title, "Lost")
        XCTAssertFalse(detail.rootView.isHidden, "The pages replace the loading state")
        XCTAssertFalse(detail.tabBar.isHidden)
        XCTAssertEqual(detail.viewControllers.count, 4)
        let loading = detail.value(forKey: "initialLoadingView") as? UIView
        XCTAssertEqual(loading?.isHidden, true)
    }

    func testDetailDescribesAnUnavailableTagInsteadOfShowingEmptyPages() {
        // Nothing cached for this id and the catalog boundary is blocked (see
        // TMBlockedNetwork), so the real load path - cache miss, query, parse,
        // failure - runs and resolves at once. The screen must say so rather
        // than present four blank pages.
        let detail = DPTagViewController()
        detail.tagId = 999_999
        mountInNavigation(detail)
        spinUntil("the load resolves") {
            (detail.value(forKey: "tagFetchPending") as? Bool) == false
        }
        settle()

        XCTAssertFalse(network.attemptedURLs.isEmpty,
                       "The failure came from the app's own catalog request, not from a short-circuit")
        XCTAssertEqual(Set(network.attemptedURLs.compactMap(\.host)), ["www.barbershoptags.com"])

        XCTAssertTrue(detail.rootView.isHidden)
        let heading = detail.value(forKey: "loadingHeading") as! UILabel
        let status = detail.value(forKey: "loadingStatus") as! UILabel
        let retry = detail.value(forKey: "retryButton") as! UIButton
        XCTAssertEqual(heading.text, "Tag unavailable")
        XCTAssertEqual(status.text,
                       "Couldn't load tag 999999. Check your connection and tag ID, then try again.")
        XCTAssertFalse(retry.isHidden, "Retry stays available")
    }

    // MARK: - Detail navigation bar
    // Replaces testDetailContentControls' navigation-bar assertions.

    func testDetailNavigationBarOffersShareTagActionsAndRefresh() {
        seedCachedTag()
        let detail = loadedDetail()
        let labels = detail.navigationItem.rightBarButtonItems?.map { $0.accessibilityLabel ?? "" }
        XCTAssertEqual(labels, ["Share", "Favorite and Teachable options", "Refresh"])
        XCTAssertEqual(detail.navigationItem.largeTitleDisplayMode, .never)
    }

    // MARK: - Favourite and teachable
    // Replaces TagDetailUITests.testFavoriteButtonIfExists, which tapped a
    // guessed button twice and asserted only that the app survived.

    func testTagActionsOfferAddFavoriteThenRemoveFavorite() {
        seedCachedTag(id: 1809)
        let detail = TMPresentationCapturingDetail()
        detail.tagId = 1809
        mountInNavigation(detail)
        waitUntil("detail loads") { detail.value(forKey: "tag") != nil }
        settle()

        detail.perform(Selector(("showActions")))
        let sheet = try? XCTUnwrap(detail.presented.last as? UIAlertController)
        XCTAssertEqual(sheet?.actions.map { $0.title ?? "" },
                       ["Add Favorite", "Mark as Teachable", "Add to List…", "Cancel"])
        XCTAssertEqual(sheet?.actions.last?.style, .cancel)

        detail.perform(Selector(("toggleFavorite")))
        XCTAssertEqual(DPAppDelegate.favorites(), [1809])

        detail.presented.removeAll()
        detail.perform(Selector(("showActions")))
        let afterFavoriting = try? XCTUnwrap(detail.presented.last as? UIAlertController)
        XCTAssertEqual(afterFavoriting?.actions.map { $0.title ?? "" },
                       ["Remove Favorite", "Mark as Teachable", "Add to List…", "Cancel"])

        detail.perform(Selector(("toggleFavorite")))
        XCTAssertEqual(DPAppDelegate.favorites(), [])
    }

    func testTogglingTeachableFromTheDetailScreenUpdatesTheStoredList() {
        seedCachedTag(id: 1809)
        let detail = loadedDetail(tagId: 1809)

        detail.perform(Selector(("toggleTeachable")))
        XCTAssertEqual(DPAppDelegate.teachable(), [1809])
        detail.perform(Selector(("toggleTeachable")))
        XCTAssertEqual(DPAppDelegate.teachable(), [])
    }

    // MARK: - Add to list

    func testTheTagActionsOpenTheListPicker() {
        seedCachedTag(id: 1809)
        let detail = TMPresentationCapturingDetail()
        detail.tagId = 1809
        mountInNavigation(detail)
        waitUntil("detail loads") { detail.value(forKey: "tag") != nil }
        settle()

        detail.perform(Selector(("showActions")))
        let sheet = try? XCTUnwrap(detail.presented.last as? UIAlertController)
        sheet?.tm_fire("Add to List…")

        let picker = (detail.presented.last as? UINavigationController)?.viewControllers.first
        XCTAssertTrue(picker is TMListPickerController)
        XCTAssertEqual((picker as? TMListPickerController)?.tagId, 1809)
        detail.presented.removeAll()
    }

    func testTheDetailCarriesAnAddToListButtonForTheWideLayout() {
        seedCachedTag(id: 1809)
        let detail = loadedDetail(tagId: 1809)
        let item = try? XCTUnwrap(detail.value(forKey: "addToListBarButton") as? UIBarButtonItem)

        XCTAssertEqual(item?.accessibilityLabel, "Add to list")
        XCTAssertEqual(item?.isEnabled, true)
        XCTAssertNotNil(item?.image)
    }

    func testTheWideLayoutHeartAndPeopleButtonsFollowChangesMadeFromThePickerOrElsewhere() throws {
        seedCachedTag(id: 1809)
        let detail = loadedDetail(tagId: 1809)
        let heart = try XCTUnwrap(detail.value(forKey: "favoriteBarButton") as? UIBarButtonItem)
        let people = try XCTUnwrap(detail.value(forKey: "teachableBarButton") as? UIBarButtonItem)
        XCTAssertEqual(heart.accessibilityLabel, "Add Favorite")
        XCTAssertEqual(people.accessibilityLabel, "Mark as Teachable")

        // The picker, a chip or another device adds the tag: no toolbar toggle is involved.
        TMTagLists.add(1809, to: TMTagLists.favoriteKey)
        TMTagLists.add(1809, to: TMTagLists.teachableKey)
        settle()
        XCTAssertEqual(heart.accessibilityLabel, "Remove Favorite")
        XCTAssertEqual(people.accessibilityLabel, "Unmark as Teachable")

        TMTagLists.remove(1809, from: TMTagLists.favoriteKey)
        settle()
        XCTAssertEqual(heart.accessibilityLabel, "Add Favorite")
        XCTAssertEqual(people.accessibilityLabel, "Unmark as Teachable")
    }

    // MARK: - The chips under a tag's title

    private func chips(in summary: DPTagSummaryController) -> TMListChipsView {
        summary.value(forKey: "listChips") as! TMListChipsView
    }

    private func capsules(_ view: TMListChipsView) -> [TMListChipView] {
        view.subviews.compactMap { $0 as? TMListChipView }
    }

    private func chipTitles(_ view: TMListChipsView) -> [String] {
        capsules(view).map(\.title)
    }

    /// A capsule is two controls now - the name opens the list, the button beside
    /// it removes the tag - so a chip is found by descending into it.
    private func chip(_ view: TMListChipsView, identifier: String) -> UIButton? {
        firstDescendant(of: view) { $0.accessibilityIdentifier == identifier } as? UIButton
    }

    func testTheChipsNameEveryListTheTagIsOnAndAlwaysOfferToAddAnother() {
        seedLists(favorite: [1809], lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809])])
        let tag = seedCachedTag(id: 1809, title: "Lost")
        let summary = self.summary(for: tag)
        let chips = self.chips(in: summary)

        XCTAssertEqual(chipTitles(chips), ["Favorites", "Afterglow set", "Add to list"])
        XCTAssertEqual(chips.accessibilityLabel, "In lists")
        XCTAssertEqual(chips.accessibilityIdentifier, "summary.chips")
        XCTAssertNotNil(chip(chips, identifier: "summary.chip.favorite"))
        XCTAssertNotNil(chip(chips, identifier: "summary.chip.afterglow-set-k3f9"))
        XCTAssertNotNil(chip(chips, identifier: "summary.chip.add"))
        for capsule in chips.subviews {
            XCTAssertGreaterThanOrEqual(capsule.bounds.height, 44, "Every chip is a full touch target")
        }
    }

    func testATagOnNoListStillOffersAddToList() {
        let tag = seedCachedTag(id: 1809)
        let chips = self.chips(in: self.summary(for: tag))
        XCTAssertEqual(chipTitles(chips), ["Add to list"])
        XCTAssertFalse(chips.isHidden)
    }

    func testTheChipsFollowChangesMadeAnywhereElse() {
        let tag = seedCachedTag(id: 1809)
        let chips = self.chips(in: self.summary(for: tag))

        DPAppDelegate.addTeachable(1809)
        XCTAssertEqual(chipTitles(chips), ["Teachable Tags", "Add to list"])

        DPAppDelegate.removeTeachable(1809)
        XCTAssertEqual(chipTitles(chips), ["Add to list"])
    }

    func testTappingAChipOpensThatList() {
        seedLists(teachable: [1809], lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809])])
        let tag = seedCachedTag(id: 1809)
        let summary = DPTagSummaryController()
        summary.busyIndicator = DPBusyIndicator()
        summary.tag = tag
        let navigation = mountCapturingPushes(summary)
        settle()
        let chips = self.chips(in: summary)

        chip(chips, identifier: "summary.chip.teachable")?.sendActions(for: .touchUpInside)
        XCTAssertTrue(navigation.pushed.last.map { TMScreens.isListScreen($0, key: TMTagLists.teachableKey) } ?? false)

        chip(chips, identifier: "summary.chip.afterglow-set-k3f9")?.sendActions(for: .touchUpInside)
        XCTAssertTrue(navigation.pushed.last.map { TMScreens.isListScreen($0, key: "afterglow-set-k3f9") } ?? false)
    }

    func testAChipRemovesItsTagFromThatListThroughItsAccessibilityAction() {
        seedLists(favorite: [1809])
        let tag = seedCachedTag(id: 1809)
        let chips = self.chips(in: self.summary(for: tag))
        let favorite = try? XCTUnwrap(chip(chips, identifier: "summary.chip.favorite"))

        let action = try? XCTUnwrap(favorite?.accessibilityCustomActions?.first)
        XCTAssertEqual(action?.name, "Remove from Favorites")
        _ = action?.actionHandler?(action!)

        XCTAssertEqual(DPAppDelegate.favorites(), [])
        XCTAssertEqual(chipTitles(chips), ["Add to list"])
    }

    func testTheFavoritesChipGoesBackToHomeWhereFavoritesLive() {
        seedLists(favorite: [1809])
        let tag = seedCachedTag(id: 1809)
        let home = TMScreens.home()
        let summary = DPTagSummaryController()
        summary.busyIndicator = DPBusyIndicator()
        summary.tag = tag
        let navigation = UINavigationController(rootViewController: home)
        navigation.pushViewController(summary, animated: false)
        mount(navigation)
        settle()

        let chips = self.chips(in: summary)
        chip(chips, identifier: "summary.chip.favorite")?.sendActions(for: .touchUpInside)
        waitUntil("Home comes back to the top of the stack") { navigation.topViewController === home }
    }

    func testTheAddChipOpensThePicker() {
        let tag = seedCachedTag(id: 1809)
        let summary = TMPresentationCapturingSummary()
        summary.busyIndicator = DPBusyIndicator()
        summary.tag = tag
        mountInNavigation(summary)
        settle()

        let chips = summary.value(forKey: "listChips") as! TMListChipsView
        chip(chips, identifier: "summary.chip.add")?.sendActions(for: .touchUpInside)

        let picker = (summary.presented.last as? UINavigationController)?.viewControllers.first
        XCTAssertEqual((picker as? TMListPickerController)?.tagId, 1809)
        summary.presented.removeAll()
    }

    func testTheChipsWrapOntoMoreThanOneLineWhenTheyHaveTo() {
        seedLists(favorite: [1809], teachable: [1809], lists: [
            (key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809]),
            (key: "chorus-warmups-list", name: "Chorus warmups for Tuesday evening", ids: [1809])
        ])
        let tag = seedCachedTag(id: 1809)
        let chips = self.chips(in: self.summary(for: tag))

        XCTAssertEqual(chipTitles(chips).count, 5)
        let lines = Set(chips.subviews.map { $0.frame.minY })
        XCTAssertGreaterThan(lines.count, 1, "Five capsules do not fit one phone-width line")
        for capsule in chips.subviews {
            XCTAssertLessThanOrEqual(capsule.frame.maxX, chips.bounds.width + 0.5, "No capsule overflows the row")
        }
        XCTAssertGreaterThan(chips.bounds.height, 44, "The row grew to hold both lines")
    }

    func testEveryMembershipChipCarriesItsOwnVisibleRemoveButton() throws {
        seedLists(favorite: [1809], lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809])])
        let tag = seedCachedTag(id: 1809)
        let chips = self.chips(in: self.summary(for: tag))

        for (key, name) in [("favorite", "Favorites"), ("afterglow-set-k3f9", "Afterglow set")] {
            let remove = try XCTUnwrap(chip(chips, identifier: "summary.chip.\(key).remove"),
                                       "\(name) needs a remove button anyone can see")
            XCTAssertEqual(remove.accessibilityLabel, "Remove from \(name)")
            XCTAssertFalse(remove.isHidden)
            XCTAssertEqual(remove.alpha, 1)
            XCTAssertNotNil(remove.configuration?.image)
            XCTAssertGreaterThanOrEqual(remove.bounds.width, 44, "A remove button is a full target")
            XCTAssertGreaterThanOrEqual(remove.bounds.height, 44)
        }
        XCTAssertNil(chip(chips, identifier: "summary.chip.add.remove"),
                     "Nothing is removed from the assist chip")
    }

    func testTheRemoveButtonTakesTheTagOutOfThatListAndUndoPutsItBackWhereItWas() throws {
        seedLists(lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [669, 1809, 122])])
        let tag = seedCachedTag(id: 1809)
        let summary = self.summary(for: tag)
        let chips = self.chips(in: summary)

        let remove = try XCTUnwrap(chip(chips, identifier: "summary.chip.afterglow-set-k3f9.remove"))
        remove.sendActions(for: .touchUpInside)

        XCTAssertEqual(TMTagLists.ids(for: "afterglow-set-k3f9"), [669, 122])
        XCTAssertEqual(chipTitles(chips), ["Add to list"])

        let undo = try XCTUnwrap(summary.undoManager)
        XCTAssertTrue(undo.canUndo, "A removal is undoable")
        XCTAssertEqual(undo.undoActionName, "Remove from Afterglow set")

        undo.undo()
        settle()
        XCTAssertEqual(TMTagLists.ids(for: "afterglow-set-k3f9"), [669, 1809, 122],
                       "The tag comes back at the position it held")
        XCTAssertEqual(chipTitles(chips), ["Afterglow set", "Add to list"])
        XCTAssertTrue(undo.canRedo, "And can be taken out again")
    }

    func testAMembershipChipStillOffersItsRemovalOnALongPress() throws {
        seedLists(favorite: [1809])
        let tag = seedCachedTag(id: 1809)
        let chips = self.chips(in: self.summary(for: tag))
        let capsule = try XCTUnwrap(capsules(chips).first { $0.listKey == "favorite" })
        let interaction = try XCTUnwrap(capsule.interactions.compactMap { $0 as? UIContextMenuInteraction }.first)

        XCTAssertNotNil(chips.contextMenuInteraction(interaction, configurationForMenuAtLocation: .zero),
                        "The long press still offers Remove from Favorites")
        let add = try XCTUnwrap(capsules(chips).first { $0.listKey == nil })
        XCTAssertTrue(add.interactions.compactMap { $0 as? UIContextMenuInteraction }.isEmpty,
                      "There is nothing to remove from the assist chip")
    }

    func testMembershipChipsAreOutlinedAndNeutralWhileOnlyTheAddChipTakesTheAccent() throws {
        seedLists(favorite: [1809])
        let tag = seedCachedTag(id: 1809)
        let chips = self.chips(in: self.summary(for: tag))

        for capsule in capsules(chips) {
            XCTAssertEqual(capsule.layer.borderWidth, 1, "Every capsule is outlined, none of them filled")
            XCTAssertEqual(capsule.layer.borderColor,
                           UIColor.separator.resolvedColor(with: capsule.traitCollection).cgColor)
            XCTAssertEqual(capsule.layer.cornerRadius, capsule.bounds.height / 2)
            XCTAssertEqual(capsule.nameButton.configuration?.background.backgroundColor, .clear,
                           "A capsule has no fill behind its name")
        }
        let favorite = try XCTUnwrap(chip(chips, identifier: "summary.chip.favorite"))
        XCTAssertEqual(favorite.configuration?.baseForegroundColor, .label)
        let add = try XCTUnwrap(chip(chips, identifier: "summary.chip.add"))
        XCTAssertEqual(add.configuration?.baseForegroundColor, DPAppDelegate.accentColor())
    }

    func testTheChipsStayInsideTheRowAndStayTargetsAtAnAccessibilityTextSize() {
        seedLists(favorite: [1809], teachable: [1809],
                  lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809])])
        let tag = seedCachedTag(id: 1809)
        let summary = self.summary(for: tag)
        window.traitOverrides.preferredContentSizeCategory = .accessibilityExtraExtraExtraLarge
        settle()
        let chips = self.chips(in: summary)

        XCTAssertEqual(chipTitles(chips).count, 4)
        for capsule in capsules(chips) {
            XCTAssertLessThanOrEqual(capsule.frame.maxX, chips.bounds.width + 0.5,
                                     "\(capsule.title) overflows the row at the largest text size")
            XCTAssertGreaterThanOrEqual(capsule.bounds.height, 44)
            guard let remove = capsule.removeButton else { continue }
            XCTAssertGreaterThanOrEqual(remove.bounds.width, 44)
            XCTAssertGreaterThanOrEqual(remove.bounds.height, 44)
            XCTAssertGreaterThan(capsule.nameButton.bounds.width, 0,
                                 "The name keeps room of its own next to the remove button")
        }
        window.traitOverrides.preferredContentSizeCategory = .large
    }

    func testCaptureTheSummaryChips() {
        seedLists(favorite: [1809], lists: [(key: "afterglow-set-k3f9", name: "Afterglow set", ids: [1809])])
        let tag = seedCachedTag(id: 1809, title: "Lost")
        let summary = self.summary(for: tag)
        capture("ios-summary-chips")
        XCTAssertFalse(self.chips(in: summary).isHidden)
    }

    // MARK: - Share
    // Replaces TagDetailUITests.testShareButtonIfExists and the app-side half of
    // testDetailShareDismissal. What the share sheet itself does once open is
    // system UI and stays in the UITests.

    func testSharingOffersTheTagTitleAndItsCatalogLink() {
        let tag = seedCachedTag(id: 1809, title: "Lost")
        let detail = TMPresentationCapturingDetail()
        detail.tagId = 1809
        mountInNavigation(detail)
        waitUntil("detail loads") { detail.value(forKey: "tag") != nil }
        settle()

        detail.perform(Selector(("sendTag")))
        let share = try? XCTUnwrap(detail.presented.last as? UIActivityViewController)
        XCTAssertNotNil(share)
        XCTAssertNotNil(share?.popoverPresentationController?.barButtonItem,
                        "The sheet is anchored to the Share button")

        // The two things the app contributes to the sheet: how the tag is
        // described, and the link people receive.
        let items = share?.value(forKey: "activityItems") as? [Any]
        XCTAssertEqual(items?.count, 2)
        XCTAssertEqual(items?.first as? String, "Lost - Tag Master for iOS")
        XCTAssertEqual(items?.last as? URL, tag.tagUri())
        XCTAssertEqual(tag.tagUri()?.absoluteString.contains("1809"), true)
    }

    func testSharingIsRefusedUntilTheTagIsAvailable() {
        let detail = TMPresentationCapturingDetail()
        detail.tagId = 999_998
        mountInNavigation(detail)
        settle()

        // Before the tag is available.
        detail.perform(Selector(("sendTag")))
        XCTAssertTrue(detail.presented.isEmpty,
                      "There is nothing to share before the tag loads")

        // And once it has resolved without a tag. Waiting here also means the
        // request is finished rather than still running past the test.
        spinUntil("the load resolves") {
            (detail.value(forKey: "tagFetchPending") as? Bool) == false
        }
        settle()
        XCTAssertNil(detail.value(forKey: "tag"))
        detail.perform(Selector(("sendTag")))
        XCTAssertTrue(detail.presented.isEmpty,
                      "There is still nothing to share after the load fails")
    }

    // MARK: - Summary page controls
    // Replaces testDetailContentControls' content assertions and
    // testSummaryLyricsRemainReachableAfterChangingPages.

    private func summary(for tag: DPTag) -> DPTagSummaryController {
        let summary = DPTagSummaryController()
        summary.busyIndicator = DPBusyIndicator()
        summary.tag = tag
        mountInNavigation(summary)
        settle()
        return summary
    }

    func testSummaryOffersSheetMusicRatingAndTheKeyNoteAsFullSizeTargets() {
        let tag = seedCachedTag(id: 1809, title: "Lost")
        let summary = self.summary(for: tag)

        let sheetMusic = summary.value(forKey: "sheetMusicButton") as! UIButton
        let rating = summary.value(forKey: "ratingButton") as! UIButton
        let keyRow = summary.value(forKey: "keyButton") as AnyObject
        let key = keyRow.value(forKey: "button") as! UIButton

        XCTAssertEqual(sheetMusic.currentTitle, "Sheet Music")
        XCTAssertEqual(rating.accessibilityLabel, "Rate tag")
        XCTAssertEqual(key.accessibilityLabel?.hasPrefix("Play key note"), true)

        for button in [sheetMusic, rating, key] {
            XCTAssertGreaterThanOrEqual(button.bounds.height, 44)
            XCTAssertGreaterThanOrEqual(button.bounds.width, 44)
        }
    }

    func testSummaryShowsTheLyricsItWasGiven() {
        let tag = seedCachedTag(id: 1809, lyrics: "And I will wait to face the skies")
        let summary = self.summary(for: tag)
        let lyrics = summary.value(forKey: "lyricsLabel") as! UILabel
        let section = summary.value(forKey: "lyricsSection") as! UIStackView

        XCTAssertEqual(lyrics.text, "And I will wait to face the skies")
        XCTAssertFalse(section.isHidden)
        XCTAssertGreaterThan(lyrics.bounds.height, 0, "The lyrics occupy real space")
    }

    func testSummaryHidesTheLyricsSectionWhenTheTagHasNone() {
        let tag = seedCachedTag(id: 4243, lyrics: nil)
        let summary = self.summary(for: tag)
        XCTAssertTrue((summary.value(forKey: "lyricsSection") as! UIStackView).isHidden)
    }

    func testLyricsRemainOnScreenAfterSwitchingPagesAndBack() {
        seedCachedTag(id: 1809, lyrics: "And I will wait to face the skies")
        let detail = loadedDetail(tagId: 1809)

        detail.selectedIndex = 1
        settle()
        detail.selectedIndex = 0
        settle()

        let summary = detail.viewControllers[0] as! DPTagSummaryController
        let lyrics = summary.value(forKey: "lyricsLabel") as! UILabel
        XCTAssertEqual(lyrics.text, "And I will wait to face the skies")
        XCTAssertFalse((summary.value(forKey: "lyricsSection") as! UIStackView).isHidden)
        waitUntil("the summary page is remounted") { lyrics.window != nil }
        XCTAssertGreaterThan(lyrics.bounds.height, 0, "The lyrics still occupy real space")
    }

    // MARK: - Rating
    // Replaces testLayoutNativeRatingScrollAndCancel and
    // testRatingAfterReturningFromDetails. Scrolling a native action sheet is
    // UIKit's behaviour; the app's contribution is the set of choices and that
    // cancelling rates nothing.

    func testRatingOffersFiveDownToOneStarPlusCancel() {
        let tag = seedCachedTag(id: 1809)
        let summary = TMPresentationCapturingSummary()
        summary.busyIndicator = DPBusyIndicator()
        summary.tag = tag
        mountInNavigation(summary)
        settle()

        summary.perform(Selector(("rate")))
        let sheet = try? XCTUnwrap(summary.presented.last as? UIAlertController)
        XCTAssertEqual(sheet?.title, "Rating")
        XCTAssertEqual(sheet?.message, "Rate the tag on a scale of 1-5 stars")
        XCTAssertEqual(sheet?.actions.map { $0.title ?? "" },
                       ["5 stars", "4 stars", "3 stars", "2 stars", "1 star", "Cancel"])
        XCTAssertEqual(sheet?.actions.last?.style, .cancel)
        XCTAssertNotNil(sheet?.popoverPresentationController?.sourceView,
                        "The sheet is anchored to the rating button")
    }

    func testRatingIsStillOfferedAfterVisitingAnotherPage() {
        seedCachedTag(id: 1809)
        let detail = loadedDetail(tagId: 1809)
        detail.selectedIndex = 1
        settle()
        detail.selectedIndex = 0
        settle()

        let summary = detail.viewControllers[0] as! DPTagSummaryController
        let rating = summary.value(forKey: "ratingButton") as! UIButton
        XCTAssertEqual(rating.accessibilityLabel, "Rate tag")
        XCTAssertTrue(rating.isEnabled)
        XCTAssertGreaterThanOrEqual(rating.bounds.height, 44)
    }

    // MARK: - Details, Tracks and Videos pages
    // Replaces testDetailMediaTabs and the Details half of testDetailLayout*.

    func testDetailsPageShowsWhenTheTagWasLastRefreshed() {
        seedCachedTag(id: 1809)
        let detail = loadedDetail(tagId: 1809)
        detail.selectedIndex = 1
        settle()

        let details = detail.viewControllers[1] as! DPTagDetailController
        XCTAssertNotNil(label(in: details.view, text: "Last Refreshed"),
                        "The Details page labels the refresh time")
    }

    func testTracksPageApologisesWhenTheTagHasNoTracks() {
        let tag = seedCachedTag(id: 4244, withTracks: false)
        let tracks = DPTagTracksController()
        tracks.busyIndicator = DPBusyIndicator()
        tracks.tag = tag
        mountInNavigation(tracks)
        settle()

        XCTAssertEqual(tag.tracks.count, 0)
        let apology = tracks.value(forKey: "apology") as! UILabel
        XCTAssertEqual(apology.text, "Sorry, no tracks could be found for this tag.")
        XCTAssertFalse(apology.isHidden)
    }

    func testTracksPageListsEveryVoicePartTheTagProvides() {
        let tag = seedCachedTag(id: 1809, withTracks: true)
        let tracks = DPTagTracksController()
        tracks.busyIndicator = DPBusyIndicator()
        tracks.tag = tag
        mountInNavigation(tracks)
        settle()

        let table = tracks.value(forKey: "partsTable") as! UITableView
        XCTAssertEqual(table.numberOfRows(inSection: 0), tag.tracks.count)
        XCTAssertEqual(tag.tracks.map(\.title), ["All Parts", "Tenor", "Lead", "Baritone", "Bass"])
    }
}
