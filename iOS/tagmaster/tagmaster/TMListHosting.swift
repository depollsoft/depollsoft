//
//  TMListHosting.swift
//  tagmaster
//
//  How the SwiftUI list screens reach the rest of the app. A screen's model
//  talks to a `TMNavigator` (open a tag, push a list, pop itself) instead of to
//  the shell, so tests hand it a recording fake; the app hands it a
//  `TMRouteNavigator` (TMRouter.swift).
//

import SwiftUI
import UIKit

/// A screen another screen can open.
enum TMDestination: Equatable {
    case browse
    case search
    case settings
    case teachable
    case list(String)
    case results(TMTagQuery)
}

/// Everything a list screen asks of the app around it.
@MainActor
protocol TMNavigator: AnyObject {
    /// True beside an open tag on iPad: rows drop their chevrons and the open tag's row stays lit.
    var isExpandedSplit: Bool { get }
    /// The tag the detail column shows, when expanded.
    var currentSplitTagId: Int? { get }
    func showTag(_ tagId: Int)
    func show(_ destination: TMDestination)
    /// Takes this screen off the stack without disturbing anything pushed above it.
    func removeScreen()
    func openURL(_ url: URL)
    func presentPrivacyChoices()
}

/// Implemented by models that list tags, so the detail can step through them.
@MainActor
protocol TMTagListing: AnyObject {
    var listedTagIds: [Int] { get }
    /// The row lit for the tag open beside the list, if any.
    var selectedTagId: Int? { get }
    func didStep(to tagId: Int)
}
