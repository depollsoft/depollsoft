//
//  TMCatalog.swift
//  tagmaster
//
//  The catalog queries the list screens run (Browse, Search results, Random
//  Tag), the filters they take, and the stored Random Tag filters. Screens
//  hold a `TMCatalog` so tests can answer queries without the network.
//

import Foundation

/// A catalog query: what Browse, Search and Random Tag ask the catalog for.
struct TMTagQuery: Equatable {
    var text: String?
    var sortBy: DPTagSortOptions = DPTagSortNone
    var collection: DPTagCollection = DPTagCollectionNone
    var parts: Int?
    var learningTracks: Bool?
    var sheetMusic: Bool?
    var minimumRating: Double?
    var minimumDownloads: Int?
    /// Only the fields to fetch; nil asks for everything a row shows.
    var fieldList: String?

    static func == (lhs: TMTagQuery, rhs: TMTagQuery) -> Bool {
        lhs.text == rhs.text && lhs.sortBy.rawValue == rhs.sortBy.rawValue
            && lhs.collection.rawValue == rhs.collection.rawValue && lhs.parts == rhs.parts
            && lhs.learningTracks == rhs.learningTracks && lhs.sheetMusic == rhs.sheetMusic
            && lhs.minimumRating == rhs.minimumRating && lhs.minimumDownloads == rhs.minimumDownloads
            && lhs.fieldList == rhs.fieldList
    }
}

/// Runs catalog queries. `query` is called off the main thread and returns nil
/// when the catalog could not be reached, which is not the same as no matches.
struct TMCatalog: Sendable {
    var query: @Sendable (_ query: TMTagQuery, _ count: Int, _ start: Int) -> DPTagQueryResult?

    static let live = TMCatalog { query, count, start in
        let result = TMObjC.catching {
            let rating = query.minimumRating.map { NSNumber(value: $0) }
            let downloads = query.minimumDownloads.map { NSNumber(value: $0) }
            let parts = query.parts.map { NSNumber(value: $0) }
            let tracks = query.learningTracks.map { NSNumber(value: $0) }
            let sheet = query.sheetMusic.map { NSNumber(value: $0) }
            if let fields = query.fieldList {
                return DPTag.query(query.text, numberOfResults: Int32(count), start: Int32(start), parts: parts,
                                   learningTracks: tracks, sheetMusic: sheet, collection: query.collection,
                                   sortBy: query.sortBy, minimumRating: rating, minimumDownloads: downloads,
                                   cache: false, fieldList: fields)
            }
            return DPTag.query(query.text, numberOfResults: Int32(count), start: Int32(start), parts: parts,
                               learningTracks: tracks, sheetMusic: sheet, collection: query.collection,
                               sortBy: query.sortBy, minimumRating: rating, minimumDownloads: downloads)
        } as? DPTagQueryResult
        guard let result, !result.failed else { return nil }
        return result
    }
}

/// The filters Random Tag applies, stored where Settings has always kept them.
/// Each is the index of its segmented control.
enum TMRandomTagFilters {
    static let minimumRatingChoices = ["Any", "1", "2", "3", "4"]
    static let minimumDownloadsChoices = ["Any", "50", "100", "500", "1000"]
    static let presenceChoices = ["Not Important", "Yes", "No"]

    private static let defaults = UserDefaults.standard

    static var minimumRatingIndex: Int {
        get {
            defaults.register(defaults: ["random.minRating": 2])
            return defaults.integer(forKey: "random.minRating")
        }
        set { defaults.set(newValue, forKey: "random.minRating") }
    }

    static var minimumDownloadsIndex: Int {
        get {
            defaults.register(defaults: ["random.minDownloads": 2])
            return defaults.integer(forKey: "random.minDownloads")
        }
        set { defaults.set(newValue, forKey: "random.minDownloads") }
    }

    static var sheetMusicIndex: Int {
        get {
            defaults.register(defaults: ["random.sheetMusic": 1])
            return defaults.integer(forKey: "random.sheetMusic")
        }
        set { defaults.set(newValue, forKey: "random.sheetMusic") }
    }

    static var learningTracksIndex: Int {
        get { defaults.integer(forKey: "random.learningTracks") }
        set { defaults.set(newValue, forKey: "random.learningTracks") }
    }

    static var minimumRating: Double? { [nil, 1.0, 2.0, 3.0, 4.0][clamped(minimumRatingIndex, 5)] }
    static var minimumDownloads: Int? { [nil, 50, 100, 500, 1000][clamped(minimumDownloadsIndex, 5)] }
    static var sheetMusic: Bool? { presence(sheetMusicIndex) }
    static var learningTracks: Bool? { presence(learningTracksIndex) }

    /// "Not Important" is nil, "Yes" true, "No" false.
    static func presence(_ index: Int) -> Bool? { [nil, true, false][clamped(index, 3)] }

    private static func clamped(_ index: Int, _ count: Int) -> Int { index >= 0 && index < count ? index : 0 }

    /// The query that picks from every tag matching the filters.
    static func query(fieldList: String = "id") -> TMTagQuery {
        TMTagQuery(learningTracks: learningTracks, sheetMusic: sheetMusic,
                   minimumRating: minimumRating, minimumDownloads: minimumDownloads, fieldList: fieldList)
    }
}
