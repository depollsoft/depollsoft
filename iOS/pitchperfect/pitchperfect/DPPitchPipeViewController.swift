//
//  DPPitchPipeViewController.swift
//  pitchperfect
//
//  Created by David Poll on 8/26/22.
//  Copyright © 2022 DepollSoft. All rights reserved.
//

import Foundation
import UIKit

let SHARP_STRING = "ì"
let FLAT_STRING = "í"

public extension DPPitchPipeViewController {
    @objc var sharpFlatString: NSMutableAttributedString {
        let str = NSMutableAttributedString()
        let sharp = NSAttributedString(string: SHARP_STRING, attributes: [NSAttributedString.Key.font: UIFont(name: "NoteHedz", size: 40)!])
        let slash = NSAttributedString(string: "/", attributes: [NSAttributedString.Key.baselineOffset: 10])
        let flat = NSAttributedString(string: FLAT_STRING, attributes: [NSAttributedString.Key.font: UIFont(name: "NoteHedz", size: 40)!])
        str.append(sharp)
        str.append(slash)
        str.append(flat)
        return str
    }
}
