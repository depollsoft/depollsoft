//
//  TestKeyboard.swift
//  Shared by the pitchperfectTests and tagmasterTests bundles.
//
//  An alert with a text field makes that field first responder as it appears,
//  which brings up the software keyboard. On a loaded CI simulator the keyboard's
//  task queue can stall for tens of seconds ("Keyboard queue task timeout") and
//  take the test's time budget with it. Hosted tests type into alert fields by
//  setting their text and sending .editingChanged, so they never need the
//  keyboard; this keeps alert fields from becoming first responder in the test
//  process. Text fields outside alerts are untouched.
//

import ObjectiveC
import UIKit

@MainActor
enum TestKeyboard {
    private static var installed = false

    static func keepAlertFieldsFromRaisingTheKeyboard() {
        guard !installed, let fieldClass = NSClassFromString("_UIAlertControllerTextField") else { return }
        installed = true
        let selector = #selector(UIResponder.becomeFirstResponder)
        guard let original = class_getInstanceMethod(UITextField.self, selector) else { return }
        let refuse: @convention(block) (UITextField) -> Bool = { _ in false }
        // Added on the private subclass only, so every other text field keeps UIKit's behaviour.
        let implementation = imp_implementationWithBlock(refuse)
        if !class_addMethod(fieldClass, selector, implementation, method_getTypeEncoding(original)),
           let own = class_getInstanceMethod(fieldClass, selector) {
            method_setImplementation(own, implementation)
        }
    }
}
