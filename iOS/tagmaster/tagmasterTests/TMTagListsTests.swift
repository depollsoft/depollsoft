//
//  TMTagListsTests.swift
//  tagmasterTests
//
//  The tag-list registry on its own: naming rules, key generation, ordering, membership, the
//  cloud shape and what a remote snapshot does to the local copy. No Firebase app is configured
//  here, so `TMTagLists.userDoc` stays nil and every write lands in UserDefaults only.
//

import XCTest
@testable import tagmaster

final class TMTagListsTests: XCTestCase {

    override func setUp() {
        super.setUp()
        clearStoredLists()
    }

    override func tearDown() {
        clearStoredLists()
        super.tearDown()
    }

    private func clearStoredLists() {
        TMTagLists.userDoc = nil
        UserDefaults.standard.removeObject(forKey: TMTagLists.listsDefaultsKey)
        UserDefaults.standard.removeObject(forKey: TMTagLists.infoDefaultsKey)
    }

    // MARK: - Names

    func testNormalizeNameTrimsAndCollapsesWhitespace() {
        XCTAssertEqual(TMTagLists.normalizeName("  Afterglow   set \n"), "Afterglow set")
        XCTAssertEqual(TMTagLists.normalizeName("   \t "), "")
    }

    func testValidateNameAcceptsAnOrdinaryName() {
        XCTAssertEqual(TMTagLists.validateName("Afterglow set", excluding: nil), TMListNameError.none)
        XCTAssertEqual(TMTagLists.validateName(String(repeating: "x", count: TMTagLists.maxNameLength),
                                               excluding: nil), TMListNameError.none)
        XCTAssertNil(TMTagLists.nameErrorMessage("Afterglow set", excluding: nil))
    }

    func testValidateNameRejectsEmptyAndOverlongNames() {
        XCTAssertEqual(TMTagLists.validateName("", excluding: nil), TMListNameError.empty)
        XCTAssertEqual(TMTagLists.validateName("   \n ", excluding: nil), TMListNameError.empty)
        XCTAssertEqual(TMTagLists.validateName(String(repeating: "x", count: TMTagLists.maxNameLength + 1),
                                               excluding: nil), .tooLong)
        XCTAssertEqual(TMTagLists.nameErrorMessage("", excluding: nil), "Give the list a name.")
        XCTAssertEqual(TMTagLists.nameErrorMessage(String(repeating: "x", count: 61), excluding: nil),
                       "Keep the name under 61 characters.")
    }

    func testValidateNameRejectsTheBuiltInNamesInAnyCase() {
        for name in ["Favorites", "favorites", "FAVORITE", " teachable tags ", "Teachable"] {
            XCTAssertEqual(TMTagLists.validateName(name, excluding: nil), .reserved, "\"\(name)\"")
        }
        XCTAssertEqual(TMTagLists.nameErrorMessage("Favorites", excluding: nil),
                       "That name is used by a built-in list.")
    }

    func testValidateNameRejectsDuplicatesIgnoringCaseAndSpacing() {
        XCTAssertNotNil(TMTagLists.createList(named: "Afterglow set"))
        XCTAssertEqual(TMTagLists.validateName("afterglow set", excluding: nil), TMListNameError.duplicate)
        XCTAssertEqual(TMTagLists.validateName("  AFTERGLOW   SET  ", excluding: nil), TMListNameError.duplicate)
        XCTAssertEqual(TMTagLists.validateName("Afterglow sets", excluding: nil), TMListNameError.none)
        XCTAssertEqual(TMTagLists.nameErrorMessage("afterglow set", excluding: nil),
                       "You already have a list with that name.")
    }

    func testValidateNameLetsAListKeepItsOwnName() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        XCTAssertEqual(TMTagLists.validateName("Afterglow set", excluding: key), TMListNameError.none)
        XCTAssertEqual(TMTagLists.validateName("AFTERGLOW SET", excluding: key), TMListNameError.none)
        XCTAssertEqual(TMTagLists.validateName("Favorites", excluding: key), TMListNameError.reserved)
    }

    // MARK: - Create / rename / delete

    func testCreateRegistersTheListAndReturnsItsKey() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        XCTAssertTrue(TMTagLists.isCustom(key))
        XCTAssertEqual(TMTagLists.customKeys(), [key])
        XCTAssertEqual(TMTagLists.name(for: key), "Afterglow set")
        XCTAssertEqual(TMTagLists.allKeys(), [TMTagLists.favoriteKey, TMTagLists.teachableKey, key])
        XCTAssertEqual(TMTagLists.ids(for: key), [])
    }

    func testCreateNormalizesTheNameItStores() {
        let key = TMTagLists.createList(named: "  Chorus   warmups  ")!
        XCTAssertEqual(TMTagLists.name(for: key), "Chorus warmups")
    }

    func testCreateRejectsAnInvalidName() {
        for name in ["", "   ", "Favorites", String(repeating: "x", count: 61)] {
            XCTAssertNil(TMTagLists.createList(named: name), "\"\(name)\"")
        }
        XCTAssertEqual(TMTagLists.customKeys(), [])
    }

    func testCreatePostsUserDataChanged() {
        expectation(forNotification: .userDataChanged, object: nil, handler: nil)
        XCTAssertNotNil(TMTagLists.createList(named: "Afterglow set"))
        waitForExpectations(timeout: 1)
    }

    func testRenameKeepsTheKeyAndTheTags() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        TMTagLists.add(1809, to: key)
        XCTAssertTrue(TMTagLists.renameList(key, to: "  Afterglow   list "))
        XCTAssertEqual(TMTagLists.name(for: key), "Afterglow list")
        XCTAssertEqual(TMTagLists.customKeys(), [key])
        XCTAssertEqual(TMTagLists.ids(for: key), [1809])
    }

    func testRenameRejectsInvalidNamesAndTheBuiltInLists() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        XCTAssertNotNil(TMTagLists.createList(named: "Chorus warmups"))
        for name in ["", "Chorus warmups", "Teachable Tags"] {
            XCTAssertFalse(TMTagLists.renameList(key, to: name), "\"\(name)\"")
        }
        XCTAssertEqual(TMTagLists.name(for: key), "Afterglow set")
        XCTAssertFalse(TMTagLists.renameList(TMTagLists.favoriteKey, to: "Anything"))
        XCTAssertFalse(TMTagLists.renameList(TMTagLists.teachableKey, to: "Anything"))
        XCTAssertEqual(TMTagLists.name(for: TMTagLists.favoriteKey), "Favorites")
        XCTAssertEqual(TMTagLists.name(for: TMTagLists.teachableKey), "Teachable Tags")
    }

    func testRenameKeepsThePositionOfTheList() {
        let keys = ["One", "Two", "Three"].map { TMTagLists.createList(named: $0)! }
        XCTAssertTrue(TMTagLists.renameList(keys[1], to: "Zulu"))
        XCTAssertEqual(TMTagLists.customKeys(), keys)
    }

    func testDeleteRemovesTheListAndItsTags() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        let kept = TMTagLists.createList(named: "Chorus warmups")!
        TMTagLists.add(1809, to: key)
        TMTagLists.add(42, to: kept)
        TMTagLists.deleteList(key)
        XCTAssertEqual(TMTagLists.customKeys(), [kept])
        XCTAssertEqual(TMTagLists.name(for: key), key)
        XCTAssertEqual(TMTagLists.ids(for: key), [])
        XCTAssertEqual(TMTagLists.ids(for: kept), [42])
        XCTAssertEqual(TMTagLists.validateName("Afterglow set", excluding: nil), TMListNameError.none)
    }

    func testDeleteIgnoresTheBuiltInLists() {
        TMTagLists.add(1809, to: TMTagLists.favoriteKey)
        TMTagLists.deleteList(TMTagLists.favoriteKey)
        TMTagLists.deleteList(TMTagLists.teachableKey)
        XCTAssertEqual(TMTagLists.ids(for: TMTagLists.favoriteKey), [1809])
    }

    // MARK: - Ordering

    func testCustomKeysFollowCreationOrder() {
        let keys = ["One", "Two", "Three"].map { TMTagLists.createList(named: $0)! }
        XCTAssertEqual(TMTagLists.customKeys(), keys)
    }

    func testMoveListReordersAndReportsWhetherAnythingMoved() {
        let keys = ["One", "Two", "Three"].map { TMTagLists.createList(named: $0)! }
        XCTAssertTrue(TMTagLists.moveList(from: 2, to: 0))
        XCTAssertEqual(TMTagLists.customKeys(), [keys[2], keys[0], keys[1]])
        XCTAssertFalse(TMTagLists.moveList(from: 0, to: 0))
        XCTAssertFalse(TMTagLists.moveList(from: 0, to: 3))
        XCTAssertFalse(TMTagLists.moveList(from: -1, to: 0))
        XCTAssertEqual(TMTagLists.customKeys(), [keys[2], keys[0], keys[1]])
    }

    func testReorderListsAcceptsOnlyPermutations() {
        let keys = ["One", "Two", "Three"].map { TMTagLists.createList(named: $0)! }
        XCTAssertFalse(TMTagLists.reorderLists(keys), "an unchanged order is not a change")
        XCTAssertFalse(TMTagLists.reorderLists(Array(keys.prefix(2))))
        XCTAssertFalse(TMTagLists.reorderLists(keys + ["extra"]))
        XCTAssertFalse(TMTagLists.reorderLists([keys[0], keys[0], keys[1]]))
        XCTAssertEqual(TMTagLists.customKeys(), keys)
        XCTAssertTrue(TMTagLists.reorderLists(Array(keys.reversed())))
        XCTAssertEqual(TMTagLists.customKeys(), Array(keys.reversed()))
    }

    // MARK: - Keys

    func testNewKeySlugsTheName() {
        XCTAssertEqual(slug(of: TMTagLists.createList(named: "Afterglow set")!), "afterglow-set")
        XCTAssertEqual(slug(of: TMTagLists.createList(named: "Tags to teach — Tuesday!")!),
                       "tags-to-teach-tuesday")
        XCTAssertEqual(slug(of: TMTagLists.createList(named: "!!! ??? ...")!), "list")
    }

    func testNewKeyTruncatesLongSlugsAndNeverCollides() {
        let key = TMTagLists.createList(named: "A very long list name that keeps on going and going")!
        XCTAssertLessThanOrEqual(slug(of: key).count, 24)
        XCTAssertFalse(slug(of: key).hasSuffix("-"))
        let first = TMTagLists.createList(named: "Warm ups")!
        let second = TMTagLists.createList(named: "Warm-ups")!
        XCTAssertEqual(slug(of: first), "warm-ups")
        XCTAssertEqual(slug(of: second), "warm-ups")
        XCTAssertNotEqual(first, second)
    }

    func testNewKeyEndsInFourLowercaseAlphanumerics() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        let suffix = key.split(separator: "-").last.map(String.init) ?? ""
        XCTAssertEqual(suffix.count, 4)
        XCTAssertNil(suffix.rangeOfCharacter(from: CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyz0123456789").inverted))
    }

    func testNewKeyNeverReturnsAKeyAlreadyInUse() {
        var taken: Set<String> = [TMTagLists.favoriteKey, TMTagLists.teachableKey]
        for _ in 0..<200 {
            let key = TMTagLists.newKey(for: "Notes", existing: taken)
            XCTAssertFalse(taken.contains(key), "\(key) was already taken")
            XCTAssertEqual(slug(of: key), "notes")
            taken.insert(key)
        }
    }

    /// A `listInfo` entry as the spec spells it: `{ name, order }` and nothing else.
    func assertInfo(_ entry: Any?, name: String, order: Int,
                    file: StaticString = #filePath, line: UInt = #line) {
        guard let entry = entry as? [String: Any] else {
            return XCTFail("expected a listInfo entry, got \(String(describing: entry))", file: file, line: line)
        }
        XCTAssertEqual(entry["name"] as? String, name, file: file, line: line)
        XCTAssertEqual((entry["order"] as? NSNumber)?.intValue, order, file: file, line: line)
        XCTAssertEqual(Set(entry.keys), ["name", "order"], file: file, line: line)
    }

    private func slug(of key: String) -> String {
        key.split(separator: "-").dropLast().joined(separator: "-")
    }

    // MARK: - Membership

    func testMembershipEditing() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        XCTAssertFalse(TMTagLists.contains(1809, in: key))
        XCTAssertTrue(TMTagLists.toggle(1809, in: key))
        XCTAssertTrue(TMTagLists.contains(1809, in: key))
        TMTagLists.add(1809, to: key)
        XCTAssertEqual(TMTagLists.ids(for: key), [1809], "adding twice adds once")
        TMTagLists.add(42, to: key)
        TMTagLists.move(in: key, from: 1, to: 0)
        XCTAssertEqual(TMTagLists.ids(for: key), [42, 1809])
        TMTagLists.move(in: key, from: 1, to: 5)
        XCTAssertEqual(TMTagLists.ids(for: key), [42, 1809], "an out-of-range move changes nothing")
        XCTAssertFalse(TMTagLists.toggle(1809, in: key))
        XCTAssertEqual(TMTagLists.ids(for: key), [42])
        TMTagLists.remove(9999, from: key)
        XCTAssertEqual(TMTagLists.ids(for: key), [42])
    }

    func testKeysContainingListsEveryListHoldingTheTagInDisplayOrder() {
        let key = TMTagLists.createList(named: "Afterglow set")!
        TMTagLists.add(1809, to: TMTagLists.favoriteKey)
        TMTagLists.add(1809, to: TMTagLists.teachableKey)
        TMTagLists.add(1809, to: key)
        TMTagLists.add(42, to: key)
        XCTAssertEqual(TMTagLists.keysContaining(1809),
                       [TMTagLists.favoriteKey, TMTagLists.teachableKey, key])
        XCTAssertEqual(TMTagLists.keysContaining(42), [key])
        XCTAssertEqual(TMTagLists.keysContaining(9999), [])
    }

    // MARK: - Legacy data

    func testListsWithoutMetadataSurfaceUnderTheirKey() {
        UserDefaults.standard.set(["legacy-key": [1, 2]], forKey: TMTagLists.listsDefaultsKey)
        XCTAssertEqual(TMTagLists.customKeys(), ["legacy-key"])
        XCTAssertEqual(TMTagLists.name(for: "legacy-key"), "legacy-key")
        XCTAssertEqual(TMTagLists.ids(for: "legacy-key"), [1, 2])
        XCTAssertEqual(TMTagLists.validateName("Legacy-Key", excluding: nil), TMListNameError.duplicate)
    }

    func testNamedListsComeBeforeUnnamedOnes() {
        let named = TMTagLists.createList(named: "Afterglow set")!
        var lists = TMTagLists.storedLists()
        lists["zzz-legacy"] = [7]
        lists["aaa-legacy"] = [8]
        UserDefaults.standard.set(lists, forKey: TMTagLists.listsDefaultsKey)
        XCTAssertEqual(TMTagLists.customKeys(), [named, "aaa-legacy", "zzz-legacy"])
    }

    // MARK: - Cloud shape

    func testRemotePayloadCarriesEveryListAndTheCustomMetadata() {
        let first = TMTagLists.createList(named: "Afterglow set")!
        let second = TMTagLists.createList(named: "Chorus warmups")!
        TMTagLists.add(1809, to: TMTagLists.favoriteKey)
        TMTagLists.add(42, to: first)

        let payload = TMTagLists.remotePayload()
        let lists = payload["lists"] as? [String: [Int]]
        XCTAssertEqual(lists?[TMTagLists.favoriteKey], [1809])
        XCTAssertEqual(lists?[TMTagLists.teachableKey], [])
        XCTAssertEqual(lists?[first], [42])
        XCTAssertEqual(lists?[second], [])

        let info = payload["listInfo"] as? [String: Any]
        XCTAssertEqual(Set(info?.keys.map { $0 } ?? []), [first, second])
        assertInfo(info?[first], name: "Afterglow set", order: 0)
        assertInfo(info?[second], name: "Chorus warmups", order: 1)
    }

    func testRemotePayloadNamesAnUnnamedListAfterItsKey() {
        UserDefaults.standard.set(["legacy-key": [1]], forKey: TMTagLists.listsDefaultsKey)
        let info = TMTagLists.remotePayload()["listInfo"] as? [String: Any]
        assertInfo(info?["legacy-key"], name: "legacy-key", order: 0)
    }

    func testApplyRemoteOrdersByOrderThenName() {
        TMTagLists.applyRemote(
            lists: ["a-key": [1], "b-key": [2], "z-key": [3]],
            info: [
                "b-key": ["name": "Bravo", "order": 1],
                "a-key": ["name": "Alpha", "order": 0],
                "z-key": ["name": "Zulu", "order": 1],
                TMTagLists.favoriteKey: ["name": "Nope", "order": 0]
            ]
        )
        XCTAssertEqual(TMTagLists.customKeys(), ["a-key", "b-key", "z-key"])
        XCTAssertEqual(TMTagLists.name(for: "a-key"), "Alpha")
        XCTAssertEqual(TMTagLists.name(for: TMTagLists.favoriteKey), "Favorites")
    }

    func testApplyRemoteKeepsLocalBuiltInListsTheRemoteDoesNotMention() {
        TMTagLists.add(1809, to: TMTagLists.favoriteKey)
        TMTagLists.add(42, to: TMTagLists.teachableKey)
        TMTagLists.applyRemote(lists: ["remote-key": [7]],
                               info: ["remote-key": ["name": "Remote", "order": 0]])
        XCTAssertEqual(TMTagLists.ids(for: TMTagLists.favoriteKey), [1809])
        XCTAssertEqual(TMTagLists.ids(for: TMTagLists.teachableKey), [42])
        XCTAssertEqual(TMTagLists.ids(for: "remote-key"), [7])
    }

    func testApplyRemoteReplacesBuiltInListsTheRemoteDoesMention() {
        TMTagLists.add(1809, to: TMTagLists.favoriteKey)
        TMTagLists.applyRemote(lists: [TMTagLists.favoriteKey: [1, 2]], info: nil)
        XCTAssertEqual(TMTagLists.ids(for: TMTagLists.favoriteKey), [1, 2])
    }

    func testApplyRemoteDropsLocalOnlyCustomLists() {
        let localOnly = TMTagLists.createList(named: "Local only")!
        TMTagLists.add(5, to: localOnly)
        TMTagLists.applyRemote(lists: ["remote-key": [7], "remote-legacy": [8]],
                               info: ["remote-key": ["name": "Remote", "order": 0]])
        XCTAssertEqual(TMTagLists.customKeys(), ["remote-key", "remote-legacy"])
        XCTAssertFalse(TMTagLists.customKeys().contains(localOnly))
        XCTAssertEqual(TMTagLists.ids(for: localOnly), [])
        XCTAssertEqual(TMTagLists.name(for: "remote-legacy"), "remote-legacy")
    }

    func testApplyRemoteReturnsEveryIdAndSurvivesMalformedEntries() {
        let ids = TMTagLists.applyRemote(
            lists: ["good": [1, 2], "junk": "nonsense"],
            info: [
                "good": ["name": "Good", "order": 0],
                "no-name": ["order": 1],
                "not-a-map": "nonsense"
            ]
        )
        XCTAssertEqual(Set(ids), [1, 2])
        XCTAssertEqual(TMTagLists.ids(for: "junk"), [])
        XCTAssertEqual(TMTagLists.customKeys(), ["good", "no-name", "junk"])
        XCTAssertEqual(TMTagLists.name(for: "no-name"), "no-name")
    }

    func testApplyRemotePostsUserDataChangedOnlyWhenSomethingChanged() {
        TMTagLists.applyRemote(lists: ["a-key": [1]], info: ["a-key": ["name": "Alpha", "order": 0]])
        var posts = 0
        let token = NotificationCenter.default.addObserver(forName: .userDataChanged,
                                                           object: nil, queue: nil) { _ in posts += 1 }
        defer { NotificationCenter.default.removeObserver(token) }
        TMTagLists.applyRemote(lists: ["a-key": [1]], info: ["a-key": ["name": "Alpha", "order": 0]])
        XCTAssertEqual(posts, 0, "an identical snapshot is not a change")
        TMTagLists.applyRemote(lists: ["a-key": [1, 2]], info: ["a-key": ["name": "Alpha", "order": 0]])
        XCTAssertEqual(posts, 1)
    }

    // MARK: - Storage

    func testTheRegistryRoundTripsThroughUserDefaults() {
        let first = TMTagLists.createList(named: "Afterglow set")!
        let second = TMTagLists.createList(named: "Chorus warmups")!
        XCTAssertTrue(TMTagLists.reorderLists([second, first]))
        TMTagLists.add(1809, to: first)

        // Exactly the two keys the app stores, and nothing else.
        let lists = UserDefaults.standard.dictionary(forKey: TMTagLists.listsDefaultsKey)
        XCTAssertEqual(lists?[first] as? [Int], [1809])
        let info = UserDefaults.standard.dictionary(forKey: TMTagLists.infoDefaultsKey) as? [String: [String: Any]]
        XCTAssertEqual(Set(info?.keys.map { $0 } ?? []), [first, second])
        assertInfo(info?[first], name: "Afterglow set", order: 1)
        assertInfo(info?[second], name: "Chorus warmups", order: 0)

        // A fresh read of those defaults rebuilds the same registry.
        XCTAssertEqual(TMTagLists.customKeys(), [second, first])
        XCTAssertEqual(TMTagLists.name(for: first), "Afterglow set")
        XCTAssertEqual(TMTagLists.ids(for: first), [1809])
    }

    func testDPAppDelegateFavoritesAndTeachableRouteThroughTheRegistry() {
        DPAppDelegate.setFavorites([1, 2])
        DPAppDelegate.setTeachable([3])
        XCTAssertEqual(TMTagLists.ids(for: TMTagLists.favoriteKey), [1, 2])
        XCTAssertEqual(TMTagLists.ids(for: TMTagLists.teachableKey), [3])
        TMTagLists.add(9, to: TMTagLists.favoriteKey)
        XCTAssertEqual(DPAppDelegate.favorites(), [1, 2, 9])
        XCTAssertEqual(DPAppDelegate.teachable(), [3])
    }
}
