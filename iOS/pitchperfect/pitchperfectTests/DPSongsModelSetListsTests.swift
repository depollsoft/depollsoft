//
//  DPSongsModelSetListsTests.swift
//  pitchperfectTests
//
//  The set-list rules the design doc pins down: naming, slug ids, ordering,
//  duplicate naming, deep copies, the delete fallback, clear-all, "addable
//  songs" filtering, and what survives a trip through UserDefaults.
//

import XCTest
@testable import pitchperfect

final class DPSongsModelSetListsTests: XCTestCase {
    private var originalLists: Any?
    private var originalCurrent: Any?
    private var originalLegacy: Any?

    private let defaults = UserDefaults.standard

    override func setUp() {
        super.setUp()
        originalLegacy = defaults.object(forKey: DPSongsModel.legacySongsKey)
        originalLists = defaults.object(forKey: DPSongsModel.songListsKey)
        originalCurrent = defaults.object(forKey: DPSongsModel.currentListKey)
        defaults.removeObject(forKey: DPSongsModel.legacySongsKey)
        defaults.removeObject(forKey: DPSongsModel.songListsKey)
        defaults.removeObject(forKey: DPSongsModel.currentListKey)
    }

    override func tearDown() {
        for (key, value) in [(DPSongsModel.legacySongsKey, originalLegacy),
                             (DPSongsModel.songListsKey, originalLists),
                             (DPSongsModel.currentListKey, originalCurrent)] {
            if let value {
                defaults.set(value, forKey: key)
            } else {
                defaults.removeObject(forKey: key)
            }
        }
        super.tearDown()
    }

    // MARK: - Helpers

    private func song(_ name: String, keyIndex: Int = 6) -> DPPitchedSong {
        let song = DPPitchedSong()
        song.name = name
        song.key = DPKey.majorKeys()[keyIndex] as? DPKey
        return song
    }

    // MARK: - Names

    func testNameValidationFollowsTheDocumentedOrder() {
        let model = DPSongsModel()
        XCTAssertNotNil(model.createList(named: "Saturday show"))

        XCTAssertEqual(model.nameErrorMessage("", excluding: nil), "Give the set list a name.")
        XCTAssertEqual(model.nameErrorMessage("   ", excluding: nil), "Give the set list a name.")
        XCTAssertEqual(model.nameErrorMessage(String(repeating: "x", count: 61), excluding: nil),
                       "Keep the name under 61 characters.")
        XCTAssertNil(model.nameErrorMessage(String(repeating: "x", count: 60), excluding: nil))
        XCTAssertEqual(model.nameErrorMessage("Default", excluding: nil), "That name is reserved.")
        XCTAssertEqual(model.nameErrorMessage("dEfAuLt", excluding: nil), "That name is reserved.")
        // The home list collides under the name it shows, not the one it stores.
        XCTAssertEqual(model.nameErrorMessage("my songs", excluding: nil),
                       "You already have a set list with that name.")
        XCTAssertEqual(model.nameErrorMessage("SATURDAY SHOW", excluding: nil),
                       "You already have a set list with that name.")
        XCTAssertNil(model.nameErrorMessage("Afterglow", excluding: nil))
    }

    func testRenamingAListMayKeepItsOwnName() {
        let model = DPSongsModel()
        let list = try! XCTUnwrap(model.createList(named: "Saturday show"))
        XCTAssertNil(model.nameErrorMessage("Saturday show", excluding: list.id))
        XCTAssertNil(model.nameErrorMessage("  saturday   SHOW ", excluding: list.id))
        XCTAssertEqual(model.nameErrorMessage("My Songs", excluding: list.id),
                       "You already have a set list with that name.")
    }

    func testNamesAreTrimmedAndCollapsed() {
        XCTAssertEqual(DPSongsModel.normalizeName("  Saturday    show \n"), "Saturday show")
        let model = DPSongsModel()
        let list = try! XCTUnwrap(model.createList(named: "  Saturday    show "))
        XCTAssertEqual(list.name, "Saturday show")
    }

    func testDisplayNameFallsBackToMySongsForTheHomeList() {
        let model = DPSongsModel()
        XCTAssertEqual(model.defaultSongList.name, "Default")
        XCTAssertEqual(model.displayName(for: model.defaultSongList), "My Songs")
        model.defaultSongList.name = ""
        XCTAssertEqual(model.displayName(for: model.defaultSongList), "My Songs")
        model.defaultSongList.name = "Rehearsal"
        XCTAssertEqual(model.displayName(for: model.defaultSongList), "Rehearsal")

        let custom = try! XCTUnwrap(model.createList(named: "Saturday show"))
        XCTAssertEqual(model.displayName(for: custom), "Saturday show")
    }

    // MARK: - Ids

    func testSlugIdsAreStableAndUnique() {
        let taken: Set<String> = []
        let id = DPSongsModel.makeListId(for: "Saturday Show!", existing: taken)
        XCTAssertTrue(id.hasPrefix("saturday-show-"), id)
        XCTAssertEqual(id.count, "saturday-show-".count + 4)
        XCTAssertTrue(id.dropFirst("saturday-show-".count).allSatisfy { $0.isLowercase || $0.isNumber })

        // A name with no slug-safe characters still gets a usable key.
        XCTAssertTrue(DPSongsModel.makeListId(for: "♯♭", existing: taken).hasPrefix("list-"))
        XCTAssertTrue(DPSongsModel.makeListId(for: "", existing: taken).hasPrefix("list-"))

        var seen = Set<String>()
        for _ in 0..<50 {
            let next = DPSongsModel.makeListId(for: "Set", existing: seen)
            XCTAssertFalse(seen.contains(next))
            XCTAssertNotEqual(next, "default")
            seen.insert(next)
        }
    }

    func testRenamingNeverChangesTheId() {
        let model = DPSongsModel()
        let list = try! XCTUnwrap(model.createList(named: "Saturday show"))
        let id = list.id
        XCTAssertTrue(model.renameList(list, to: "Sunday show"))
        XCTAssertEqual(list.id, id)
        XCTAssertEqual(model.songLists[id]?.name, "Sunday show")
        XCTAssertFalse(model.renameList(list, to: "My Songs"))
        XCTAssertEqual(list.name, "Sunday show")
    }

    // MARK: - Ordering

    func testOrderedListsPinTheHomeListAndSortTheRest() {
        let model = DPSongsModel()
        let first = try! XCTUnwrap(model.createList(named: "Saturday show"))
        let second = try! XCTUnwrap(model.createList(named: "Afterglow"))
        XCTAssertEqual(first.order, 0)
        XCTAssertEqual(second.order, 1)
        XCTAssertEqual(model.orderedLists.map(\.id), ["default", first.id, second.id])

        // Legacy documents carry no order and sort after every ordered list, by name.
        let zeta = DPSongList(id: "zeta")
        zeta.name = "Zeta"
        let alpha = DPSongList(id: "alpha")
        alpha.name = "Alpha"
        model.songLists["zeta"] = zeta
        model.songLists["alpha"] = alpha
        XCTAssertEqual(model.orderedLists.map(\.id),
                       ["default", first.id, second.id, "alpha", "zeta"])
    }

    func testReorderingRewritesADenseOrderForEveryCustomList() {
        let model = DPSongsModel()
        let a = try! XCTUnwrap(model.createList(named: "A list"))
        let b = try! XCTUnwrap(model.createList(named: "B list"))
        let c = try! XCTUnwrap(model.createList(named: "C list"))

        XCTAssertTrue(model.reorderLists(["default", c.id, a.id, b.id]))
        XCTAssertEqual(c.order, 0)
        XCTAssertEqual(a.order, 1)
        XCTAssertEqual(b.order, 2)
        XCTAssertEqual(model.orderedLists.map(\.id), ["default", c.id, a.id, b.id])

        // A partial sequence still leaves every custom list with a dense order.
        XCTAssertTrue(model.reorderLists([b.id]))
        XCTAssertEqual(b.order, 0)
        XCTAssertEqual(Set([a.order, c.order]), [1, 2])
    }

    // MARK: - Duplicating

    func testDuplicateNamesItselfCopyThenCopy2() {
        let model = DPSongsModel()
        let list = try! XCTUnwrap(model.createList(named: "Saturday show"))
        list.songs = [song("Blue Skies"), song("Shenandoah")]

        let first = try! XCTUnwrap(model.duplicateList(list))
        XCTAssertEqual(first.name, "Saturday show copy")
        let second = try! XCTUnwrap(model.duplicateList(list))
        XCTAssertEqual(second.name, "Saturday show copy 2")
        let third = try! XCTUnwrap(model.duplicateList(list))
        XCTAssertEqual(third.name, "Saturday show copy 3")
        XCTAssertEqual(third.order, 3, "a copy lands after every existing list")
    }

    func testDuplicateDeepCopiesEverySongWithAFreshId() {
        let model = DPSongsModel()
        let list = try! XCTUnwrap(model.createList(named: "Saturday show"))
        list.songs = [song("Blue Skies"), song("Shenandoah")]

        let copy = try! XCTUnwrap(model.duplicateList(list))
        XCTAssertEqual(copy.songs.map(\.name), ["Blue Skies", "Shenandoah"])
        XCTAssertEqual(copy.songs.count, 2)
        for (original, duplicated) in zip(list.songs, copy.songs) {
            XCTAssertFalse(original === duplicated, "a copy is an independent song")
            XCTAssertNotEqual(original.id, duplicated.id, "song ids are identity inside one list")
            XCTAssertTrue(duplicated.key.isEqual(original.key))
        }
        // Editing the copy leaves the original alone.
        copy.songs.first?.name = "Blue Skies (cut)"
        XCTAssertEqual(list.songs.first?.name, "Blue Skies")
    }

    func testDuplicatingTheHomeListIsAllowedAndNamedFromItsDisplayName() {
        let model = DPSongsModel()
        let copy = try! XCTUnwrap(model.duplicateList(model.defaultSongList))
        XCTAssertEqual(copy.name, "My Songs copy")
        XCTAssertNotEqual(copy.id, "default")
    }

    // MARK: - Deleting and clearing

    func testDeletingTheCurrentListFallsBackToMySongs() {
        let model = DPSongsModel()
        let list = try! XCTUnwrap(model.createList(named: "Saturday show"))
        model.currentListId = list.id
        XCTAssertEqual(model.currentListId, list.id)

        XCTAssertTrue(model.deleteList(list))
        XCTAssertNil(model.songLists[list.id])
        XCTAssertEqual(model.currentListId, "default")
        XCTAssertTrue(model.currentList === model.defaultSongList)

        let stored = defaults.dictionary(forKey: DPSongsModel.songListsKey)
        XCTAssertNil(stored?[list.id], "a deleted list leaves no local entry behind")
    }

    func testTheHomeListCannotBeDeleted() {
        let model = DPSongsModel()
        XCTAssertFalse(model.deleteList(model.defaultSongList))
        XCTAssertNotNil(model.songLists["default"])
    }

    func testClearAllEmptiesMySongsAndDeletesEveryOtherList() {
        let model = DPSongsModel()
        model.defaultSongList.songs = [song("Blue Skies")]
        let list = try! XCTUnwrap(model.createList(named: "Saturday show"))
        list.songs = [song("Shenandoah")]
        model.currentListId = list.id

        model.clearAll()

        XCTAssertEqual(model.songLists.keys.sorted(), ["default"])
        XCTAssertTrue(model.defaultSongList.songs.isEmpty)
        XCTAssertEqual(model.currentListId, "default")
    }

    // MARK: - The current list

    func testCurrentListFallsBackWhenTheStoredIdIsStale() {
        defaults.set("gone-1234", forKey: DPSongsModel.currentListKey)
        let model = DPSongsModel()
        XCTAssertEqual(model.currentListId, "default")
        XCTAssertTrue(model.currentList === model.defaultSongList)

        let list = try! XCTUnwrap(model.createList(named: "Saturday show"))
        model.currentListId = list.id
        XCTAssertEqual(defaults.string(forKey: DPSongsModel.currentListKey), list.id)
        // Setting an id that names nothing is the same as asking for My Songs.
        model.currentListId = "not-a-list"
        XCTAssertEqual(model.currentListId, "default")
    }

    // MARK: - Addable songs

    func testAddableSongsOmitsWhatTheTargetAlreadyHasAndGroupsBySourceList() {
        let model = DPSongsModel()
        let target = model.defaultSongList
        target.songs = [song("Blue Skies", keyIndex: 6)]

        let saturday = try! XCTUnwrap(model.createList(named: "Saturday show"))
        saturday.songs = [
            song("blue skies", keyIndex: 6),   // same title and key: omitted
            song("Blue Skies", keyIndex: 8),   // same title, other key: offered
            song("Shenandoah", keyIndex: 6),
        ]
        let afterglow = try! XCTUnwrap(model.createList(named: "Afterglow"))
        afterglow.songs = [song("Lida Rose", keyIndex: 6)]
        let empty = try! XCTUnwrap(model.createList(named: "Empty"))
        XCTAssertTrue(empty.songs.isEmpty)

        let groups = model.addableSongs(for: target)
        XCTAssertEqual(groups.map { $0.list.id }, [saturday.id, afterglow.id],
                       "sections follow list order, and an empty list has no section")
        XCTAssertEqual(groups.first?.songs.map(\.name), ["Blue Skies", "Shenandoah"])
        XCTAssertEqual(groups.last?.songs.map(\.name), ["Lida Rose"])
        XCTAssertTrue(model.hasAddableSongs(for: target))
        XCTAssertEqual(model.addableSongs(for: saturday).map { $0.list.id }, [afterglow.id],
                       "the target's own list is never a section")

        // Nothing is addable to a list that already holds every song there is.
        let everything = try! XCTUnwrap(model.createList(named: "Everything"))
        model.copySongs(model.orderedLists.flatMap(\.songs), to: everything)
        XCTAssertFalse(model.hasAddableSongs(for: everything))
        XCTAssertTrue(model.addableSongs(for: everything).isEmpty)
    }

    func testCopySongsAppendsDeepCopiesInSourceOrder() {
        let model = DPSongsModel()
        let source = try! XCTUnwrap(model.createList(named: "Saturday show"))
        source.songs = [song("Blue Skies"), song("Shenandoah")]
        let target = model.defaultSongList
        target.songs = [song("Lida Rose")]

        let copies = model.copySongs(source.songs, to: target)
        XCTAssertEqual(target.songs.map(\.name), ["Lida Rose", "Blue Skies", "Shenandoah"])
        XCTAssertEqual(copies.count, 2)
        XCTAssertTrue(Set(source.songs.map(\.id)).isDisjoint(with: Set(copies.map(\.id))))
    }

    // MARK: - Local persistence

    func testOrderSurvivesARoundTripThroughUserDefaults() {
        let model = DPSongsModel()
        let first = try! XCTUnwrap(model.createList(named: "Saturday show"))
        let second = try! XCTUnwrap(model.createList(named: "Afterglow"))
        second.songs = [song("Lida Rose")]
        model.storeAll()

        let stored = defaults.dictionary(forKey: DPSongsModel.songListsKey) as? [String: Any]
        let entry = stored?[first.id] as? [String: Any]
        XCTAssertEqual(entry?["name"] as? String, "Saturday show")
        XCTAssertEqual((entry?["order"] as? NSNumber)?.intValue, 0)
        XCTAssertNil((stored?["default"] as? [String: Any])?["order"],
                     "the home list is pinned first and stores no order")

        let reloaded = DPSongsModel()
        XCTAssertEqual(reloaded.orderedLists.map(\.id), ["default", first.id, second.id])
        XCTAssertEqual(reloaded.songLists[second.id]?.order, 1)
        XCTAssertEqual(reloaded.songLists[second.id]?.songs.map(\.name), ["Lida Rose"])
    }

    func testACopyOfAMaximumLengthNameStillFitsTheLimit() throws {
        let model = DPSongsModel()
        let longest = String(repeating: "S", count: DPSongsModel.nameLengthLimit)
        let list = try XCTUnwrap(model.createList(named: longest))
        let copy = try XCTUnwrap(model.duplicateList(list))
        XCTAssertEqual(copy.name.count, DPSongsModel.nameLengthLimit)
        XCTAssertTrue(copy.name.hasSuffix(" copy"))
        XCTAssertNil(model.validateName(copy.name, excluding: copy.id),
                     "a generated name passes the same validation as a typed one")
        let second = try XCTUnwrap(model.duplicateList(list))
        XCTAssertEqual(second.name.count, DPSongsModel.nameLengthLimit)
        XCTAssertTrue(second.name.hasSuffix(" copy 2"))
    }

    func testRemoteWinsKeepsMySongsAndTheAccountsListsOnly() throws {
        let model = DPSongsModel()
        let kept = try XCTUnwrap(model.createList(named: "Saturday show"))
        let dropped = try XCTUnwrap(model.createList(named: "Made offline"))
        model.currentListId = dropped.id

        let gone = model.applyRemoteWins(remoteIds: [kept.id, "unknown-elsewhere"])

        XCTAssertEqual(gone, [dropped.id])
        XCTAssertEqual(Set(model.songLists.keys), [DPSongsModel.defaultListId, kept.id])
        XCTAssertTrue(dropped.isDeleted, "a discarded list can never write itself back")
        XCTAssertFalse(kept.isDeleted)
        XCTAssertEqual(model.currentListId, DPSongsModel.defaultListId)
        let stored = defaults.dictionary(forKey: DPSongsModel.songListsKey) ?? [:]
        XCTAssertNil(stored[dropped.id])
    }

    func testAnAccountCreatedByThisSignInIsNew() {
        let created = Date()
        XCTAssertTrue(DPSongsModel.isNewAccount(createdAt: created, lastSignInAt: created.addingTimeInterval(0.8)))
        XCTAssertFalse(DPSongsModel.isNewAccount(createdAt: created, lastSignInAt: created.addingTimeInterval(120)))
        XCTAssertFalse(DPSongsModel.isNewAccount(createdAt: nil, lastSignInAt: created), "unknown stamps never count as new")
    }

    func testADeletedListNeverWritesItselfBack() throws {
        let model = DPSongsModel()
        let stale = try XCTUnwrap(model.createList(named: "Saturday show"))
        stale.addSong(song("Blue Skies"))
        model.currentListId = stale.id

        XCTAssertTrue(model.deleteList(stale))
        // An editor that was open on one of its songs saves after the delete landed.
        stale.addSong(song("Shenandoah"))
        stale.name = "Renamed too late"
        stale.storeValue()

        XCTAssertTrue(stale.isDeleted)
        XCTAssertNil(model.songLists[stale.id])
        XCTAssertEqual(model.currentListId, DPSongsModel.defaultListId)
        let stored = defaults.dictionary(forKey: DPSongsModel.songListsKey) ?? [:]
        XCTAssertNil(stored[stale.id], "a deleted list must not come back through UserDefaults")
    }
}
