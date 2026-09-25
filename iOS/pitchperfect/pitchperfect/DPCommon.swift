//
//  DPCommon.swift
//  pitchperfect
//
//  Created by David Poll on 6/25/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

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
