//
//  TagDetailModelTests.swift
//  tagmasterTests
//
//  The tag detail's behaviour, driven through its models with a controlled tag
//  loader: loading, refreshing and retrying (including requests that finish out
//  of order), stepping through the list that opened the tag, the saved-list
//  toggles and actions, sharing, and the Summary's key note, rating, sheet music
//  and list chips.
//

import XCTest
@testable import tagmaster

/// Records every tag request and lets the test finish each one when it chooses.
final class TMControlledTagLoader: TMTagLoading {
    struct Request {
        let tagId: Int32
        let refresh: Bool
        let finish: @MainActor (DPTag?) -> Void
    }

    private(set) var requests: [Request] = []

    func load(_ tagId: Int32, refresh: Bool, completion: @escaping @MainActor (DPTag?) -> Void) {
        requests.append(Request(tagId: tagId, refresh: refresh, finish: completion))
    }

    @MainActor func finish(_ index: Int, with tag: DPTag?) {
        requests[index].finish(tag)
    }
}

/// A list that opened the detail, as Home or a query list would be.
final class TMTestSource: NSObject, TMTagListSource {
    var ids: [Int]
    private(set) var steppedTo: [Int32] = []
    init(_ ids: [Int]) { self.ids = ids }
    func tm_listedTagIds() -> [NSNumber] { ids.map { NSNumber(value: $0) } }
    func tm_didStep(toTagId tagId: Int32) { steppedTo.append(tagId) }
}

@MainActor
final class TagDetailModelTests: XCTestCase {
    private var loader: TMControlledTagLoader!
    private var model: TagDetailModel!
    private var shownTags: [Int32] = []
    private var shownLists: [String] = []
    private var sheets: [TMSheetMusicDocument] = []
    private var files: [String] = []

    override func setUp() {
        super.setUp()
        UserDefaults.standard.removeObject(forKey: TMListsDefaultsKey)
        UserDefaults.standard.removeObject(forKey: TMListsInfoDefaultsKey)
        loader = TMControlledTagLoader()
        model = TagDetailModel(loader: loader)
        model.navigator = TMDetailNavigator(
            showTag: { [unowned self] in self.shownTags.append($0) },
            showList: { [unowned self] in self.shownLists.append($0) },
            showSheetMusic: { [unowned self] in self.sheets.append($0) })
    }

    override func tearDown() {
        TagSummaryModel.timedKeyNoteDuration = 1.5
        files.forEach { try? FileManager.default.removeItem(atPath: $0) }
        UserDefaults.standard.removeObject(forKey: TMListsDefaultsKey)
        UserDefaults.standard.removeObject(forKey: TMListsInfoDefaultsKey)
        model = nil
        loader = nil
        super.tearDown()
    }

    private func tag(_ id: Int32, title: String? = nil) -> DPTag {
        let tag = DPTag()
        tag.tagId = id
        tag.title = title ?? "Tag \(id)"
        tag.writtenKey = "Bb"
        tag.lyrics = "And I will wait"
        return tag
    }

    private func spin(until condition: () -> Bool, timeout: TimeInterval = 3,
                      file: StaticString = #filePath, line: UInt = #line) {
        let deadline = Date().addingTimeInterval(timeout)
        while !condition(), Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.005))
        }
        XCTAssertTrue(condition(), "Condition never held", file: file, line: line)
    }

    // MARK: - Loading

    func testAPendingFirstLoadShowsTheQuartetAndThenThePages() {
        model.show(tagId: 1809)
        XCTAssertTrue(model.initialPending)
        XCTAssertTrue(model.isEmpty)
        XCTAssertEqual(model.busy.count, 1)
        XCTAssertEqual(model.loadingHeading, "Gathering the quartet…")
        XCTAssertEqual(model.loadingStatus, "Loading tag 1809…")
        XCTAssertEqual(model.loadingStatusSpoken, "Gathering the quartet… Loading tag 1809…")
        XCTAssertEqual(model.title, "Tag")

        let loaded = tag(1809, title: "Lost")
        loader.finish(0, with: loaded)
        XCTAssertTrue(model.tag === loaded)
        XCTAssertFalse(model.isEmpty)
        XCTAssertFalse(model.initialPending)
        XCTAssertEqual(model.title, "Lost")
        XCTAssertEqual(model.busy.count, 0)
    }

    func testRepeatingTheSameTagOrLoadWhilePendingDoesNotFetchTwice() {
        model.show(tagId: 1809)
        model.show(tagId: 1809)
        model.load(refresh: false)
        XCTAssertEqual(loader.requests.count, 1, "Mounting or repeating the same pending request must not fetch twice")
    }

    func testAFailedFirstLoadSaysSoAndRetryAsksForTheCurrentTag() {
        model.show(tagId: 1809)
        loader.finish(0, with: nil)
        XCTAssertTrue(model.loadFailed)
        XCTAssertFalse(model.initialPending)
        XCTAssertEqual(model.loadingHeading, "Tag unavailable")
        XCTAssertEqual(model.loadingStatus,
                       "Couldn't load tag 1809. Check your connection and tag ID, then try again.")
        XCTAssertNil(model.error, "The inline Retry, not an alert, handles a failed first load")
        XCTAssertEqual(model.busy.count, 0)

        model.retryInitialLoad()
        XCTAssertEqual(loader.requests.last?.tagId, 1809)
        XCTAssertTrue(model.initialPending)
        loader.finish(1, with: nil)
        XCTAssertTrue(model.loadFailed)

        model.retryInitialLoad()
        let loaded = tag(1809)
        loader.finish(2, with: loaded)
        XCTAssertTrue(model.tag === loaded)
        XCTAssertFalse(model.loadFailed)
        XCTAssertEqual(model.busy.count, 0)
    }

    func testARefreshThatFailsKeepsThePagesAndItsRetryReplacesTheTag() {
        model.screenVisible = true
        model.show(tagId: 1809)
        let first = tag(1809)
        loader.finish(0, with: first)
        model.selectedPage = .details

        model.refresh()
        XCTAssertTrue(loader.requests[1].refresh)
        XCTAssertTrue(model.fetchPending)
        XCTAssertFalse(model.initialPending, "A refresh keeps the pages up")
        loader.finish(1, with: nil)
        XCTAssertTrue(model.tag === first)
        let error = try? XCTUnwrap(model.error)
        XCTAssertEqual(error?.message,
                       "The tag couldn't be refreshed. Check your connection and try again. Your saved tags are unchanged.")

        error?.retry?()
        XCTAssertTrue(loader.requests[2].refresh)
        let updated = tag(1809, title: "Updated tag")
        loader.finish(2, with: updated)
        XCTAssertTrue(model.tag === updated)
        XCTAssertEqual(model.title, "Updated tag")
        XCTAssertEqual(model.selectedPage, .details, "The open page survives the refresh")
        XCTAssertEqual(model.busy.count, 0)
    }

    func testRefreshWaitsWhileOtherWorkIsInFlight() {
        model.show(tagId: 1809)
        loader.finish(0, with: tag(1809))
        model.busy.begin()
        model.refresh()
        XCTAssertEqual(loader.requests.count, 1)
        model.busy.end()
        model.refresh()
        XCTAssertEqual(loader.requests.count, 2)
    }

    func testAnOutOfOrderCompletionOrAnOldRetryCannotPopulateAnotherTag() {
        model.screenVisible = true
        model.show(tagId: 1809)
        model.show(tagId: 42)
        loader.finish(0, with: tag(1809))
        XCTAssertTrue(model.initialPending, "The first tag's late answer is ignored")
        let current = tag(42)
        loader.finish(1, with: current)
        XCTAssertTrue(model.tag === current)

        model.refresh()
        loader.finish(2, with: nil)
        let oldRetry = model.error?.retry
        model.error = nil
        model.show(tagId: 99)
        oldRetry?()
        XCTAssertEqual(loader.requests.count, 4, "The old retry belongs to a tag that is no longer shown")
        let next = tag(99)
        loader.finish(3, with: next)
        XCTAssertTrue(model.tag === next)
        XCTAssertEqual(model.busy.count, 0)
    }

    func testANewSuccessThenAnOldFailureLeavesTheScreenAlone() {
        model.show(tagId: 1809)
        model.show(tagId: 42)
        let current = tag(42)
        loader.finish(1, with: current)
        loader.finish(0, with: nil)
        XCTAssertTrue(model.tag === current)
        XCTAssertFalse(model.loadFailed)
        XCTAssertEqual(model.busy.count, 0)
    }

    func testTheQuartetMovesOnlyWhilePendingOnScreenAndActive() {
        model.show(tagId: 1809)
        model.applicationActive = true
        XCTAssertFalse(model.quartetMoving, "Not on screen yet")
        model.screenVisible = true
        XCTAssertTrue(model.quartetMoving)
        model.applicationActive = false
        XCTAssertFalse(model.quartetMoving)
        NotificationCenter.default.post(Notification(name: UIApplication.didBecomeActiveNotification))
        XCTAssertTrue(model.quartetMoving)
        NotificationCenter.default.post(Notification(name: UIApplication.willResignActiveNotification))
        XCTAssertFalse(model.quartetMoving)
        model.applicationActive = true
        model.screenVisible = false
        XCTAssertFalse(model.quartetMoving)
        model.screenVisible = true
        loader.finish(0, with: tag(1809))
        XCTAssertFalse(model.quartetMoving, "The quartet rests once the tag is here")
    }

    func testTheCatalogLoaderReturnsCachedTagsOnTheMainQueue() {
        let cached = tag(31_337, title: "Cached")
        cached.cache()
        defer { TMBehaviorTestCase.evictCachedTag(31_337) }
        let loaded = expectation(description: "loaded")
        TMCatalogTagLoader().load(31_337, refresh: false) { result in
            XCTAssertTrue(Thread.isMainThread)
            XCTAssertEqual(result?.title, "Cached")
            loaded.fulfill()
        }
        wait(for: [loaded], timeout: 5)
    }

    // MARK: - Stepping

    func testSteppingNeedsASourceAndTheSplitAndStopsAtTheEnds() {
        let source = TMTestSource([4243, 1809, 122])
        model.show(tagId: 1809)
        loader.finish(0, with: tag(1809))
        XCTAssertFalse(model.canStep, "No list opened this tag")
        model.source = source
        XCTAssertFalse(model.canStep, "Collapsed (iPhone): the list is not beside the tag")
        model.expanded = true
        XCTAssertTrue(model.canStep)
        XCTAssertTrue(model.showsSteppers)
        XCTAssertTrue(model.hasPreviousTag)
        XCTAssertTrue(model.hasNextTag)

        model.stepToNextTag()
        model.stepToPreviousTag()
        XCTAssertEqual(shownTags, [122, 4243])

        source.ids = [1809, 122]
        NotificationCenter.default.post(Notification(name: .TMTagListDidChange, object: source))
        XCTAssertFalse(model.hasPreviousTag, "A change in the list is picked up at once")
        XCTAssertTrue(model.hasNextTag)
        source.ids = [1809]
        NotificationCenter.default.post(Notification(name: .TMTagListDidChange, object: source))
        XCTAssertFalse(model.hasNextTag)
        model.stepToNextTag()
        XCTAssertEqual(shownTags, [122, 4243], "Nothing past the end")
    }

    // MARK: - Saved lists and actions

    func testFavoriteAndTeachableTogglesFollowTheListsWhereverTheyChange() {
        model.toggleFavorite()
        XCTAssertEqual(DPAppDelegate.favorites(), [], "Nothing is saved before the tag arrives")
        model.show(tagId: 1809)
        loader.finish(0, with: tag(1809))

        model.toggleFavorite()
        XCTAssertEqual(DPAppDelegate.favorites(), [1809])
        XCTAssertTrue(model.isFavorite)
        model.toggleTeachable()
        XCTAssertEqual(DPAppDelegate.teachable(), [1809])
        XCTAssertTrue(model.isTeachable)

        // The picker, a chip or another device changes the lists: no toggle is involved.
        TMTagLists.remove(1809, from: TMTagLists.favoriteKey)
        XCTAssertFalse(model.isFavorite)
        XCTAssertTrue(model.isTeachable)
        model.toggleTeachable()
        XCTAssertEqual(DPAppDelegate.teachable(), [])
    }

    func testTheTagActionsOfferTheOppositeOfEachSavedStateThenAddToListAndCancel() {
        model.showActions()
        XCTAssertFalse(model.actionsPresented, "No actions before the tag arrives")
        model.show(tagId: 1809)
        loader.finish(0, with: tag(1809))
        model.showActions()
        XCTAssertTrue(model.actionsPresented)
        XCTAssertEqual(model.tagActions.map(\.title), ["Add Favorite", "Mark as Teachable", "Add to List…", "Cancel"])
        XCTAssertEqual(model.tagActions.last?.style, .cancel)

        model.tagActions[0].handler()
        XCTAssertEqual(model.tagActions.map(\.title), ["Remove Favorite", "Mark as Teachable", "Add to List…", "Cancel"])
        model.tagActions[1].handler()
        XCTAssertEqual(model.tagActions.map(\.title), ["Remove Favorite", "Unmark as Teachable", "Add to List…", "Cancel"])

        model.tagActions[2].handler()
        XCTAssertEqual(model.pickerSource, .actions)
        XCTAssertTrue(model.pickerPresented(from: .actions))
        XCTAssertFalse(model.pickerPresented(from: .toolbar))
    }

    func testSharingOffersTheTitleAndCatalogLinkOnlyOnceTheTagIsHere() {
        XCTAssertNil(model.shareURL)
        XCTAssertNil(model.shareMessage)
        model.show(tagId: 1809)
        XCTAssertNil(model.shareURL)
        let lost = tag(1809, title: "Lost")
        loader.finish(0, with: lost)
        XCTAssertEqual(model.shareMessage, "Lost - Tag Master for iOS")
        XCTAssertEqual(model.shareURL?.absoluteString, "http://tags.depoll.com/tag.php?id=1809")
    }

    // MARK: - Key note

    func testHoldingTheKeySoundsItUntilReleased() throws {
        model.show(tagId: 1809)
        loader.finish(0, with: tag(1809))
        let note = try XCTUnwrap(model.summary.keyNote)
        model.summary.pressKey()
        XCTAssertTrue(note.isPlaying)
        XCTAssertTrue(model.summary.keyNotePlaying)
        model.summary.releaseKey()
        XCTAssertFalse(note.isPlaying)
        XCTAssertFalse(model.summary.keyNotePlaying)
    }

    func testATimedKeyNoteStopsByItselfButNeverStopsALaterHold() throws {
        TagSummaryModel.timedKeyNoteDuration = 0.05
        model.show(tagId: 1809)
        loader.finish(0, with: tag(1809))
        let note = try XCTUnwrap(model.summary.keyNote)

        model.summary.playTimedKeyNote()
        XCTAssertTrue(note.isPlaying)
        spin(until: { !note.isPlaying })

        model.summary.playTimedKeyNote()
        model.summary.pressKey()
        RunLoop.main.run(until: Date().addingTimeInterval(0.15))
        XCTAssertTrue(note.isPlaying, "The timed note's deadline no longer owns the note")
        model.summary.releaseKey()

        model.summary.playTimedKeyNote()
        RunLoop.main.run(until: Date().addingTimeInterval(0.02))
        model.summary.playTimedKeyNote()
        RunLoop.main.run(until: Date().addingTimeInterval(0.04))
        XCTAssertTrue(note.isPlaying, "A second activation keeps its own deadline")
        spin(until: { !note.isPlaying })
        model.summary.stopKeyNote()
    }

    // MARK: - Rating

    func testRatingIsOfferedOnceAndAFailedRatingCanBeRetried() {
        model.show(tagId: 1809)
        loader.finish(0, with: tag(1809))
        model.summary.showRating()
        XCTAssertTrue(model.summary.ratingDialogPresented)
        model.summary.ratingDialogPresented = false

        var attempts: [Int] = []
        var succeed = false
        model.summary.rate(4) { _, stars in
            attempts.append(stars)
            return succeed
        }
        XCTAssertTrue(model.summary.ratingBusy)
        XCTAssertEqual(model.busy.count, 1)
        spin(until: { model.error != nil })
        XCTAssertEqual(model.error?.message, "Your rating couldn't be sent. Check your connection and try again.")
        XCTAssertFalse(model.summary.ratingBusy)
        XCTAssertFalse(model.summary.rated)
        XCTAssertEqual(model.busy.count, 0)

        succeed = true
        let retry = model.error?.retry
        model.error = nil
        retry?()
        spin(until: { model.summary.rated })
        XCTAssertEqual(attempts, [4, 4])
        XCTAssertEqual(model.busy.count, 0)
        model.summary.showRating()
        XCTAssertFalse(model.summary.ratingDialogPresented, "A rated tag cannot be rated again")

        model.show(tagId: 42)
        loader.finish(1, with: tag(42))
        XCTAssertFalse(model.summary.rated, "Another tag can be rated")
    }

    // MARK: - Sheet music

    func testMissingSheetMusicIsNotCachedAndRetryOpensTheReaderOnceItArrives() throws {
        let sheet = DPRemoteLocation()
        sheet.uri = URL(string: "https://example.invalid/sheet-\(UUID().uuidString).pdf")
        sheet.type = "pdf"
        let loaded = tag(1809, title: "Lost")
        loaded.sheetMusicUri = sheet
        model.show(tagId: 1809)
        loader.finish(0, with: loaded)
        let path = try XCTUnwrap(DPFileCache.path(forKey: sheet.cacheKey))
        files.append(path)

        var data: Data?
        model.summary.openSheetMusic { _ in data }
        XCTAssertTrue(model.summary.sheetMusicBusy)
        spin(until: { model.error != nil })
        XCTAssertEqual(model.error?.message, "Sheet music couldn't be opened. Check your connection and try again.")
        XCTAssertFalse(FileManager.default.fileExists(atPath: path), "Nothing is cached for missing sheet music")
        XCTAssertTrue(sheets.isEmpty)
        XCTAssertEqual(model.busy.count, 0)

        data = Data("%PDF-1.4 sheet".utf8)
        let retry = model.error?.retry
        model.error = nil
        retry?()
        spin(until: { !sheets.isEmpty })
        XCTAssertTrue(FileManager.default.fileExists(atPath: path))
        XCTAssertEqual(sheets.first?.fileURL.path, path)
        XCTAssertEqual(sheets.first?.title, "Lost")
        XCTAssertEqual(sheets.first?.writtenKey, "Bb", "The reader carries the key note beside the page")
        XCTAssertEqual(model.busy.count, 0)

        // Cached now: opening again does not fetch.
        var fetched = false
        model.summary.openSheetMusic { _ in fetched = true; return nil }
        spin(until: { sheets.count == 2 })
        XCTAssertFalse(fetched)
    }

    // MARK: - Chips

    func testChipsNameTheTagsListsOpenThemAndOpenThePicker() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        TMTagLists.add(1809, to: key)
        TMTagLists.add(1809, to: TMTagLists.favoriteKey)
        model.show(tagId: 1809)
        XCTAssertEqual(model.summary.memberships, [], "Nothing until the tag arrives")
        loader.finish(0, with: tag(1809))
        XCTAssertEqual(model.summary.memberships, [TMTagLists.favoriteKey, key])

        model.summary.openList(key)
        XCTAssertEqual(shownLists, [key])
        model.summary.showPicker()
        XCTAssertEqual(model.pickerSource, .chip)

        DPAppDelegate.addTeachable(1809)
        XCTAssertEqual(model.summary.memberships, [TMTagLists.favoriteKey, TMTagLists.teachableKey, key])
    }

    func testRemovingATagFromAListIsUndoneBackToTheSamePosition() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        TMTagLists.setIds([669, 1809, 122], for: key)
        model.show(tagId: 1809)
        loader.finish(0, with: tag(1809))

        model.summary.remove(from: key)
        XCTAssertEqual(TMTagLists.ids(for: key), [669, 122])
        XCTAssertEqual(model.summary.memberships, [])
        XCTAssertTrue(model.undoManager.canUndo)
        XCTAssertEqual(model.undoManager.undoActionName, "Remove from Afterglow set")

        model.undoManager.undo()
        XCTAssertEqual(TMTagLists.ids(for: key), [669, 1809, 122], "The tag comes back at the position it held")
        XCTAssertEqual(model.summary.memberships, [key])
        XCTAssertTrue(model.undoManager.canRedo)

        model.summary.remove(from: TMTagLists.favoriteKey)
        XCTAssertEqual(TMTagLists.ids(for: key), [669, 1809, 122], "Removing from a list the tag is not on does nothing")
    }
}
