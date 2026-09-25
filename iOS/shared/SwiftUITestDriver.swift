//
//  SwiftUITestDriver.swift
//  Shared by the pitchperfectTests and tagmasterTests bundles.
//
//  Drives real SwiftUI screens in-process through the accessibility tree, the
//  same tree VoiceOver and XCUITest read. SwiftUI only builds that tree while
//  accessibility automation is on, so `enableAutomation()` turns it on for the
//  test process (a private libAccessibility switch, test bundles only; the
//  Hammer and AccessibilitySnapshot libraries rely on the same call).
//
//  Elements are found by accessibility identifier or label and activated with
//  `accessibilityActivate()`, which runs the control's real action: a Button's
//  closure, a Toggle flip, a toolbar item, a NavigationLink.
//

import UIKit
import XCTest

@MainActor
struct UIDriver {
    let root: UIView

    init(_ root: UIView) {
        UIDriver.enableAutomation()
        self.root = root
    }

    init(_ window: UIWindow) {
        UIDriver.enableAutomation()
        self.root = window
    }

    private static var automationEnabled = false

    static func enableAutomation() {
        guard !automationEnabled else { return }
        automationEnabled = true
        guard let handle = dlopen("/usr/lib/libAccessibility.dylib", RTLD_NOW),
              let symbol = dlsym(handle, "_AXSSetAutomationEnabled") else { return }
        typealias SetEnabled = @convention(c) (Int32) -> Void
        unsafeBitCast(symbol, to: SetEnabled.self)(1)
    }

    // MARK: - Reading

    /// Every accessibility element under the root, depth first, in reading order.
    var elements: [NSObject] {
        var result: [NSObject] = []
        UIDriver.walk(root, depth: 0) { element in
            if element.isAccessibilityElement || UIDriver.identifier(of: element) != nil {
                result.append(element)
            }
        }
        return result
    }

    static func identifier(of element: NSObject) -> String? {
        let id: String?
        if let identified = element as? UIAccessibilityIdentification {
            id = identified.accessibilityIdentifier
        } else if element.responds(to: NSSelectorFromString("accessibilityIdentifier")) {
            // SwiftUI's accessibility nodes answer the selector without adopting the
            // protocol. Call it directly: KVC cannot read every implementation.
            id = element.perform(NSSelectorFromString("accessibilityIdentifier"))?.takeUnretainedValue() as? String
        } else {
            id = nil
        }
        return (id?.isEmpty ?? true) ? nil : id
    }

    /// Labels of the elements VoiceOver would visit, in order.
    var labels: [String] {
        elements.filter(\.isAccessibilityElement).compactMap(\.accessibilityLabel).filter { !$0.isEmpty }
    }

    func element(id: String) -> NSObject? {
        // SwiftUI hosts a toolbar item's node inside a UIKit host carrying the same
        // identifier; the innermost (last visited) one is the actionable node.
        elements.last { UIDriver.identifier(of: $0) == id }
    }

    func element(label: String) -> NSObject? {
        elements.first { $0.isAccessibilityElement && $0.accessibilityLabel == label }
    }

    func elements(labelPrefix prefix: String) -> [NSObject] {
        elements.filter { $0.isAccessibilityElement && ($0.accessibilityLabel ?? "").hasPrefix(prefix) }
    }

    func exists(id: String) -> Bool { element(id: id) != nil }
    func exists(label: String) -> Bool { element(label: label) != nil }

    func label(id: String) -> String? { element(id: id)?.accessibilityLabel }
    func value(id: String) -> String? { element(id: id)?.accessibilityValue }
    func traits(id: String) -> UIAccessibilityTraits { element(id: id)?.accessibilityTraits ?? [] }
    func isSelected(id: String) -> Bool { traits(id: id).contains(.selected) }
    func isEnabled(id: String) -> Bool { !traits(id: id).contains(.notEnabled) }

    // MARK: - Acting

    @discardableResult
    func tap(id: String, file: StaticString = #filePath, line: UInt = #line) -> Bool {
        guard let element = element(id: id) else {
            XCTFail("No element with identifier \(id). Present: \(identifiers)", file: file, line: line)
            return false
        }
        return activate(element, file: file, line: line)
    }

    @discardableResult
    func tap(label: String, file: StaticString = #filePath, line: UInt = #line) -> Bool {
        guard let element = element(label: label) else {
            XCTFail("No element labelled \(label). Present: \(labels)", file: file, line: line)
            return false
        }
        return activate(element, file: file, line: line)
    }

    /// Runs a named custom action (a swipe action, "Move up", a context menu item VoiceOver exposes).
    @discardableResult
    func perform(action name: String, id: String, file: StaticString = #filePath, line: UInt = #line) -> Bool {
        guard let element = element(id: id) else {
            XCTFail("No element with identifier \(id)", file: file, line: line)
            return false
        }
        guard let action = element.accessibilityCustomActions?.first(where: { $0.name == name }) else {
            XCTFail("\(id) has no action \(name); has \(element.accessibilityCustomActions?.map(\.name) ?? [])",
                    file: file, line: line)
            return false
        }
        let handled: Bool
        if let handler = action.actionHandler {
            handled = handler(action)
        } else if let target = action.target as? NSObject {
            handled = target.perform(action.selector, with: action) != nil
        } else {
            handled = false
        }
        ScreenCatalog.settle(0.05)
        return handled
    }

    func increment(id: String) { element(id: id)?.accessibilityIncrement(); ScreenCatalog.settle(0.05) }
    func decrement(id: String) { element(id: id)?.accessibilityDecrement(); ScreenCatalog.settle(0.05) }

    var identifiers: [String] { elements.compactMap(UIDriver.identifier(of:)) }

    private func activate(_ element: NSObject, file: StaticString, line: UInt) -> Bool {
        // SwiftUI's nodes act on activation. UIKit controls (a UIBarButtonItem's
        // button, a UIButton) leave activation to VoiceOver's synthesized tap,
        // so the control nearest the element is sent the tap's action instead.
        var handled = element.accessibilityActivate()
        if !handled, let control = UIDriver.control(for: element) {
            // Whichever event the control answers: a bar button's primary action
            // or a classic touch-up target.
            var answered: UInt = 0
            control.enumerateEventHandlers { _, _, event, _ in answered |= event.rawValue }
            let primary = UIControl.Event.primaryActionTriggered
            let event: UIControl.Event = answered & primary.rawValue != 0 ? primary : .touchUpInside
            control.sendActions(for: event)
            handled = true
        }
        XCTAssertTrue(handled, "\(element.accessibilityLabel ?? "element") did not activate", file: file, line: line)
        ScreenCatalog.settle(0.05)
        return handled
    }

    private static func control(for element: NSObject) -> UIControl? {
        var view = element as? UIView
        while let current = view {
            // The first control that acts: a bar button's inner button only draws.
            if let control = current as? UIControl, control.isEnabled {
                var answers = false
                control.enumerateEventHandlers { _, _, _, stop in answers = true; stop = true }
                if answers { return control }
            }
            view = current.superview
        }
        return nil
    }

    /// Waits (spinning the run loop) until `condition` holds or the deadline passes.
    func wait(_ seconds: TimeInterval = 3, file: StaticString = #filePath, line: UInt = #line,
              until condition: () -> Bool) {
        let deadline = Date().addingTimeInterval(seconds)
        while !condition(), Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.01))
        }
        XCTAssertTrue(condition(), "Timed out waiting", file: file, line: line)
    }

    // MARK: - Walking

    private static func walk(_ element: NSObject, depth: Int, visit: (NSObject) -> Void) {
        if let view = element as? UIView, view.isHidden || view.alpha == 0 { return }
        if element.accessibilityElementsHidden { return }
        visit(element)
        guard depth < 60 else { return }
        var children: [NSObject] = []
        if let list = element.accessibilityElements as? [NSObject] {
            children = list
        } else {
            let count = element.accessibilityElementCount()
            if count != NSNotFound, count > 0 {
                children = (0..<count).compactMap { element.accessibilityElement(at: $0) as? NSObject }
            }
        }
        if children.isEmpty, let view = element as? UIView { children = view.subviews }
        for child in children { walk(child, depth: depth + 1, visit: visit) }
    }
}
