//
//  ListModelTests.swift
//  tagmasterTests
//
//  Tests for ListModel and CustomListsModel.
//

import XCTest
@testable import tagmaster

class ListModelTests: XCTestCase {

    override func setUp() {
        super.setUp()
        ListModel.setTestMode(true)
        CustomListsModel.setTestMode(true)
        ListModel.resetModelInstances()
        CustomListsModel.resetForTesting()
        UserDefaults.standard.removeObject(forKey: ListModel.listsKey)
    }

    override func tearDown() {
        ListModel.resetModelInstances()
        CustomListsModel.resetForTesting()
        UserDefaults.standard.removeObject(forKey: ListModel.listsKey)
        super.tearDown()
    }

    // MARK: - ListModel.get() factory

    func testGetReturnsSameInstanceForSameName() {
        let a = ListModel.get("favorite")
        let b = ListModel.get("favorite")
        XCTAssertTrue(a === b)
    }

    func testGetReturnsDifferentInstancesForDifferentNames() {
        let a = ListModel.get("favorite")
        let b = ListModel.get("teachable")
        XCTAssertFalse(a === b)
    }

    // MARK: - add()

    func testAddAppendsId() {
        let model = ListModel.get("favorite")
        model.add(42)
        XCTAssertEqual(model.ids, [42])
    }

    func testAddMultipleIds() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        model.add(3)
        XCTAssertEqual(model.ids, [1, 2, 3])
    }

    func testAddIsIdempotent() {
        let model = ListModel.get("favorite")
        model.add(42)
        model.add(42)
        XCTAssertEqual(model.ids, [42])
    }

    // MARK: - remove()

    func testRemoveRemovesId() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        model.remove(1)
        XCTAssertEqual(model.ids, [2])
    }

    func testRemoveIsNoOpForMissingId() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.remove(99)
        XCTAssertEqual(model.ids, [1])
    }

    // MARK: - contains()

    func testContainsReturnsTrueForPresentId() {
        let model = ListModel.get("favorite")
        model.add(42)
        XCTAssertTrue(model.contains(42))
    }

    func testContainsReturnsFalseForAbsentId() {
        let model = ListModel.get("favorite")
        XCTAssertFalse(model.contains(42))
    }

    // MARK: - moveUp() / moveDown()

    func testMoveUpReordersCorrectly() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        model.add(3)
        model.moveUp(2)
        XCTAssertEqual(model.ids, [2, 1, 3])
    }

    func testMoveUpAtTopIsNoOp() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        model.moveUp(1)
        XCTAssertEqual(model.ids, [1, 2])
    }

    func testMoveDownReordersCorrectly() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        model.add(3)
        model.moveDown(2)
        XCTAssertEqual(model.ids, [1, 3, 2])
    }

    func testMoveDownAtBottomIsNoOp() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        model.moveDown(2)
        XCTAssertEqual(model.ids, [1, 2])
    }

    // MARK: - canMoveUp() / canMoveDown()

    func testCanMoveUpReturnsFalseForFirstElement() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        XCTAssertFalse(model.canMoveUp(1))
    }

    func testCanMoveUpReturnsTrueForNonFirstElement() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        XCTAssertTrue(model.canMoveUp(2))
    }

    func testCanMoveUpReturnsFalseForMissingId() {
        let model = ListModel.get("favorite")
        XCTAssertFalse(model.canMoveUp(99))
    }

    func testCanMoveDownReturnsFalseForLastElement() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        XCTAssertFalse(model.canMoveDown(2))
    }

    func testCanMoveDownReturnsTrueForNonLastElement() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        XCTAssertTrue(model.canMoveDown(1))
    }

    func testCanMoveDownReturnsFalseForMissingId() {
        let model = ListModel.get("favorite")
        XCTAssertFalse(model.canMoveDown(99))
    }

    // MARK: - reset()

    func testResetClearsList() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.add(2)
        model.reset()
        XCTAssertTrue(model.ids.isEmpty)
    }

    // MARK: - setIds()

    func testSetIdsReplacesWholeList() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.setIds([10, 20, 30])
        XCTAssertEqual(model.ids, [10, 20, 30])
    }

    func testSetIdsWithEmptyArrayClearsList() {
        let model = ListModel.get("favorite")
        model.add(1)
        model.setIds([])
        XCTAssertTrue(model.ids.isEmpty)
    }

    // MARK: - deleteList()

    func testDeleteListRemovesInstanceAndPreferences() {
        let model = ListModel.get("mylist")
        model.add(42)
        XCTAssertEqual(ListModel.preferences["mylist"], [42])

        ListModel.deleteList("mylist")

        XCTAssertNil(ListModel.preferences["mylist"])
        // Getting the same key again should return a fresh instance
        let fresh = ListModel.get("mylist")
        XCTAssertTrue(fresh.ids.isEmpty)
    }

    // MARK: - CustomListsModel: createList()

    func testCreateListReturnsUUIDKey() {
        let key = CustomListsModel.createList("My List")
        XCTAssertFalse(key.isEmpty)
        XCTAssertNotNil(UUID(uuidString: key))
    }

    func testCreateListAddsToAllLists() {
        CustomListsModel.createList("Contest Tags")
        let all = CustomListsModel.allLists()
        XCTAssertEqual(all.count, 3)
        XCTAssertEqual(all[2].name, "Contest Tags")
    }

    func testCreateListGeneratesUniqueKeys() {
        let key1 = CustomListsModel.createList("List 1")
        let key2 = CustomListsModel.createList("List 2")
        XCTAssertNotEqual(key1, key2)
    }

    func testCreateListMultipleHaveIncreasingOrder() {
        CustomListsModel.createList("First")
        CustomListsModel.createList("Second")
        CustomListsModel.createList("Third")
        let custom = CustomListsModel.customLists()
        XCTAssertEqual(custom.count, 3)
        XCTAssertTrue(custom[0].order < custom[1].order)
        XCTAssertTrue(custom[1].order < custom[2].order)
    }

    // MARK: - CustomListsModel: renameList()

    func testRenameListChangesName() {
        let key = CustomListsModel.createList("Old Name")
        CustomListsModel.renameList(key, newName: "New Name")
        XCTAssertEqual(CustomListsModel.getMetadata(key)?.name, "New Name")
    }

    func testRenameListIsNoOpForBuiltInKeys() {
        CustomListsModel.renameList("favorite", newName: "Not Favorites")
        XCTAssertEqual(CustomListsModel.getMetadata("favorite")?.name, "Favorites")
    }

    func testRenameListIsNoOpForNonExistentKey() {
        CustomListsModel.renameList("nonexistent", newName: "Name")
        XCTAssertNil(CustomListsModel.getMetadata("nonexistent"))
    }

    // MARK: - CustomListsModel: deleteList()

    func testDeleteListRemovesFromCustomLists() {
        let key = CustomListsModel.createList("To Delete")
        XCTAssertEqual(CustomListsModel.customLists().count, 1)

        CustomListsModel.deleteList(key)

        XCTAssertEqual(CustomListsModel.customLists().count, 0)
        XCTAssertNil(CustomListsModel.getMetadata(key))
    }

    func testDeleteListIsNoOpForFavorite() {
        CustomListsModel.deleteList("favorite")
        XCTAssertNotNil(CustomListsModel.getMetadata("favorite"))
        XCTAssertEqual(CustomListsModel.allLists().count, 2)
    }

    func testDeleteListIsNoOpForTeachable() {
        CustomListsModel.deleteList("teachable")
        XCTAssertNotNil(CustomListsModel.getMetadata("teachable"))
    }

    func testDeleteListDoesNotAffectOtherCustomLists() {
        let key1 = CustomListsModel.createList("Keep")
        let key2 = CustomListsModel.createList("Delete")

        CustomListsModel.deleteList(key2)

        XCTAssertEqual(CustomListsModel.customLists().count, 1)
        XCTAssertEqual(CustomListsModel.getMetadata(key1)?.name, "Keep")
    }

    // MARK: - CustomListsModel: allLists()

    func testAllListsIncludesBuiltInsFirst() {
        CustomListsModel.createList("Custom")
        let all = CustomListsModel.allLists()
        XCTAssertEqual(all[0].key, "favorite")
        XCTAssertEqual(all[1].key, "teachable")
        XCTAssertTrue(all[0].isBuiltIn)
        XCTAssertTrue(all[1].isBuiltIn)
        XCTAssertFalse(all[2].isBuiltIn)
    }

    func testAllListsNoCustomReturnsOnlyBuiltIn() {
        let lists = CustomListsModel.allLists()
        XCTAssertEqual(lists.count, 2)
    }

    func testCustomListsReturnsEmptyWhenNone() {
        XCTAssertTrue(CustomListsModel.customLists().isEmpty)
    }

    // MARK: - CustomListsModel: getMetadata()

    func testGetMetadataReturnsFavoriteBuiltIn() {
        let meta = CustomListsModel.getMetadata("favorite")
        XCTAssertNotNil(meta)
        XCTAssertEqual(meta?.name, "Favorites")
        XCTAssertTrue(meta!.isBuiltIn)
    }

    func testGetMetadataReturnsTeachableBuiltIn() {
        let meta = CustomListsModel.getMetadata("teachable")
        XCTAssertNotNil(meta)
        XCTAssertEqual(meta?.name, "Teachable Tags")
        XCTAssertTrue(meta!.isBuiltIn)
    }

    func testGetMetadataReturnsNilForUnknownKey() {
        XCTAssertNil(CustomListsModel.getMetadata("does-not-exist"))
    }

    func testGetMetadataReturnsCustomMetadata() {
        let key = CustomListsModel.createList("Custom")
        let meta = CustomListsModel.getMetadata(key)
        XCTAssertNotNil(meta)
        XCTAssertEqual(meta?.name, "Custom")
        XCTAssertFalse(meta!.isBuiltIn)
    }

    // MARK: - ListModel.fromFirestore()

    func testFromFirestoreWithFavoriteAndTeachableOnly() {
        let data: [String: Any] = [
            "favorite": [1, 2, 3],
            "teachable": [10, 20]
        ]

        ListModel.fromFirestore(data)

        let fav = ListModel.get("favorite")
        let teach = ListModel.get("teachable")
        XCTAssertEqual(fav.ids, [1, 2, 3])
        XCTAssertEqual(teach.ids, [10, 20])
    }

    func testFromFirestoreWithCustomListsPresent() {
        let customKey = "custom-list-1"
        let data: [String: Any] = [
            "favorite": [1],
            customKey: [100, 200]
        ]

        ListModel.fromFirestore(data)

        let custom = ListModel.get(customKey)
        XCTAssertEqual(custom.ids, [100, 200])
    }

    func testFromFirestoreRemovesListsNoLongerOnServer() {
        // Set up a local list
        let model = ListModel.get("old-list")
        model.add(99)
        ListModel.preferences["old-list"] = [99]

        // Firestore data doesn't include "old-list"
        let data: [String: Any] = [
            "favorite": [1]
        ]

        ListModel.fromFirestore(data)

        XCTAssertTrue(model.ids.isEmpty)
        XCTAssertNil(ListModel.preferences["old-list"])
    }

    func testFromFirestoreWithNSNumberArray() {
        let data: [String: Any] = [
            "favorite": [NSNumber(value: 5), NSNumber(value: 10)]
        ]

        ListModel.fromFirestore(data)

        let fav = ListModel.get("favorite")
        XCTAssertEqual(fav.ids, [5, 10])
    }

    // MARK: - CustomListsModel.fromFirestore()

    func testCustomListsFromFirestoreWithValidData() {
        let data: [String: Any] = [
            "list-1": ["name": "Contest Tags", "order": 0],
            "list-2": ["name": "My Quartet", "order": 1]
        ]

        CustomListsModel.fromFirestore(data)

        let custom = CustomListsModel.customLists()
        XCTAssertEqual(custom.count, 2)
        XCTAssertEqual(custom[0].name, "Contest Tags")
        XCTAssertEqual(custom[1].name, "My Quartet")
    }

    func testCustomListsFromFirestoreWithNilIsNoOp() {
        CustomListsModel.createList("Existing")
        CustomListsModel.fromFirestore(nil)
        XCTAssertEqual(CustomListsModel.customLists().count, 1)
    }

    func testCustomListsFromFirestoreEmptyMapClearsCustom() {
        CustomListsModel.createList("To Clear")
        XCTAssertEqual(CustomListsModel.customLists().count, 1)

        CustomListsModel.fromFirestore([:])

        XCTAssertEqual(CustomListsModel.customLists().count, 0)
    }

    // MARK: - fromFirestore() does not corrupt built-in lists

    func testFromFirestoreDoesNotCorruptBuiltInMetadata() {
        let favModel = ListModel.get("favorite")
        favModel.add(42)

        CustomListsModel.fromFirestore([
            "custom-1": ["name": "New", "order": 0]
        ])

        XCTAssertEqual(CustomListsModel.getMetadata("favorite")?.name, "Favorites")
        XCTAssertTrue(favModel.contains(42))
    }

    func testFromFirestoreDoesNotCorruptTeachable() {
        let teachModel = ListModel.get("teachable")
        teachModel.add(55)

        ListModel.fromFirestore([
            "favorite": [1],
            "teachable": [55]
        ])

        XCTAssertEqual(CustomListsModel.getMetadata("teachable")?.name, "Teachable Tags")
        XCTAssertTrue(teachModel.contains(55))
    }

    // MARK: - toFirestoreMap()

    func testToFirestoreMapEmptyWhenNoCustomLists() {
        XCTAssertTrue(CustomListsModel.toFirestoreMap().isEmpty)
    }

    func testToFirestoreMapContainsCustomListMetadata() {
        let key = CustomListsModel.createList("Test List")
        let map = CustomListsModel.toFirestoreMap()
        XCTAssertEqual(map.count, 1)
        XCTAssertEqual(map[key]?["name"] as? String, "Test List")
    }

    func testToFirestoreMapDoesNotContainBuiltInLists() {
        CustomListsModel.createList("Custom")
        let map = CustomListsModel.toFirestoreMap()
        XCTAssertNil(map["favorite"])
        XCTAssertNil(map["teachable"])
    }
}
