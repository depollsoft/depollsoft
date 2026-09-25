//
//  DPCommon.swift
//  pitchperfect
//
//  Created by David Poll on 6/25/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

import Combine
import Foundation

enum DPCommon {
    private static let barButtonLabels = [
        "checkmark": "Done",
        "xmark": "Close",
        "gearshape": "Settings",
        "plus": "Add",
        "pencil": "Edit",
        "square.and.arrow.up": "Share",
        "magnifyingglass": "Search",
        "arrow.clockwise": "Refresh",
        "ellipsis.circle": "More",
        "list.bullet": "Set lists",
    ]

    /// What VoiceOver (and the UI tests) call a bar button drawn from `systemName`.
    static func accessibilityLabel(forSymbol systemName: String) -> String {
        barButtonLabels[systemName] ?? systemName
    }
}

/// Holds a view's model for as long as the view lives, building it once.
/// `@State`'s initial value is evaluated every time the view struct is
/// recreated (and then discarded), which for a model that observes
/// notifications or starts an auth listener is real work; `@StateObject`
/// evaluates its initial value only once.
final class ModelBox<Model: AnyObject>: ObservableObject {
    let model: Model
    init(_ model: Model) { self.model = model }
}
