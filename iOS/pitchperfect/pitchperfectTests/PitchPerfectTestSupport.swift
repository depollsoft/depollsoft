//
//  PitchPerfectTestSupport.swift
//  pitchperfectTests
//
//  Mounting the app for hosted tests, and answering the alerts it presents.
//

import FirebaseCore
import SwiftUI
import UIKit
import XCTest
@testable import pitchperfect

extension UIAlertController {
    /// Types into the alert's field the way the keyboard would, so the
    /// validation that gates the confirming action actually runs.
    func pp_type(_ text: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let field = textFields?.first else {
            return XCTFail("This alert has no text field", file: file, line: line)
        }
        field.text = text
        field.sendActions(for: .editingChanged)
        NotificationCenter.default.post(name: UITextField.textDidChangeNotification, object: field)
        ScreenCatalog.settle(0.1)
    }

    /// Runs the handler of the named action, the way tapping it would.
    func pp_fire(_ title: String, file: StaticString = #filePath, line: UInt = #line) {
        guard let action = actions.first(where: { $0.title == title }) else {
            return XCTFail("No “\(title)” action in this alert", file: file, line: line)
        }
        XCTAssertTrue(action.isEnabled, "“\(title)” is disabled", file: file, line: line)
        typealias Handler = @convention(block) (UIAlertAction) -> Void
        guard action.responds(to: Selector(("handler"))), let raw = action.value(forKey: "handler") else {
            return XCTFail("UIAlertAction no longer exposes its handler", file: file, line: line)
        }
        unsafeBitCast(raw as AnyObject, to: Handler.self)(action)
    }
}
