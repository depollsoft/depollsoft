import XCTest
@testable import tagmaster

final class SwiftUIMigrationTests: XCTestCase {
    func testCopyrightYearDoesNotUseThousandsSeparator() {
        XCTAssertEqual(MainAppView.copyrightText(for: 2026), "Depollsoft © 2026")
    }

    func testRandomFilterDefaultsMatchLegacyBehavior() {
        let filters = TagFilterValues()

        XCTAssertNil(filters.learningTracks)
        XCTAssertEqual(filters.sheetMusic, true)
        XCTAssertEqual(filters.minimumRating, 2)
        XCTAssertEqual(filters.minimumDownloads, 100)
    }

    func testTrackDownloadsUseStableTemporaryPath() {
        let url = TagTracksView.temporaryURL(for: "track.mp3")

        XCTAssertEqual(url.lastPathComponent, "track.mp3")
        XCTAssertTrue(url.path.hasPrefix(FileManager.default.temporaryDirectory.path))
    }

    func testTriStateFilterMapping() {
        XCTAssertNil(TagFilterValues.triState(0))
        XCTAssertEqual(TagFilterValues.triState(1), true)
        XCTAssertEqual(TagFilterValues.triState(2), false)
        XCTAssertNil(TagFilterValues.triState(99))
    }

    func testSearchOptionsMapPersistedIndexes() {
        let options = TagSearchOptions(
            query: "hello", sortIndex: 3, sheetMusicIndex: 1,
            learningTracksIndex: 2, partsIndex: 2, collectionIndex: 1
        )

        XCTAssertEqual(options.sort.rawValue, 4)
        XCTAssertEqual(options.sheetMusic, true)
        XCTAssertEqual(options.learningTracks, false)
        XCTAssertEqual(options.parts, 4)
        XCTAssertEqual(options.collection.rawValue, 1)
    }

    @MainActor
    func testQueryLoaderUsesNextPageStartAndStopsAtAvailableCount() {
        var starts: [Int32] = []
        let loader = TagQueryLoader { start in
            starts.append(start)
            let result = DPTagQueryResult()
            result.start = start
            result.count = 1
            result.available = 2
            let tag = DPTag()
            tag.tagId = start + 1
            result.tags = [tag]
            return result
        }

        let first = expectation(description: "first page")
        loader.loadMore()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { first.fulfill() }
        wait(for: [first], timeout: 1)

        XCTAssertEqual(starts, [0])
        XCTAssertEqual(loader.tags.map(\.tagId), [1])
        XCTAssertTrue(loader.hasMore)

        let second = expectation(description: "second page")
        loader.loadMore()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { second.fulfill() }
        wait(for: [second], timeout: 1)

        XCTAssertEqual(starts, [0, 1])
        XCTAssertEqual(loader.tags.map(\.tagId), [1, 2])
        XCTAssertFalse(loader.hasMore)
    }
}
