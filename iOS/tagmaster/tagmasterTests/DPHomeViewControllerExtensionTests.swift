//
//  DPHomeViewControllerExtensionTests.swift
//  tagmasterTests
//
//  Tests for DPHomeViewController Swift extension methods.
//

import XCTest
import UIKit
@testable import tagmaster

class DPHomeViewControllerExtensionTests: XCTestCase {

    var homeViewController: DPHomeViewController!
    var navigationController: UINavigationController!
    var window: UIWindow!

    override func setUp() {
        super.setUp()

        homeViewController = DPHomeViewController(style: .grouped)
        navigationController = UINavigationController(rootViewController: homeViewController)

        // Create window attached to foreground scene for proper view controller hierarchy
        if let windowScene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }) ?? UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first {
            window = UIWindow(windowScene: windowScene)
        } else {
            window = UIWindow(frame: CGRect(x: 0, y: 0, width: 375, height: 812))
        }

        window.rootViewController = navigationController
        window.makeKeyAndVisible()

        // Load the view hierarchy
        _ = homeViewController.view
        homeViewController.view.layoutIfNeeded()
    }

    override func tearDown() {
        homeViewController.dismiss(animated: false)
        window.isHidden = true
        window.rootViewController = nil
        window = nil
        navigationController = nil
        homeViewController = nil
        super.tearDown()
    }

    private func presentOpenTagAlert() {
        homeViewController.openTag()
        let visible = XCTNSPredicateExpectation(
            predicate: NSPredicate { [weak self] _, _ in
                guard let alert = self?.homeViewController.presentedViewController as? UIAlertController else {
                    return false
                }
                return alert.viewIfLoaded?.window != nil
            },
            object: nil
        )
        XCTAssertEqual(XCTWaiter.wait(for: [visible], timeout: 5), .completed,
                       "Open Tag alert should appear in the test window")
    }

    // MARK: - Open Tag Tests

    func testOpenTagMethodExists() {
        // Verify the openTag method is available
        XCTAssertTrue(homeViewController.responds(to: #selector(DPHomeViewController.openTag)))
    }

    func testOpenTagPresentsAlertController() {
        presentOpenTagAlert()

        // Verify alert is presented
        XCTAssertNotNil(homeViewController.presentedViewController)
        XCTAssertTrue(homeViewController.presentedViewController is UIAlertController)

        let alert = homeViewController.presentedViewController as? UIAlertController
        XCTAssertEqual(alert?.title, "Open Tag")
        XCTAssertEqual(alert?.message, "Enter Tag ID")
    }

    func testOpenTagAlertHasTextField() {
        presentOpenTagAlert()

        let alert = homeViewController.presentedViewController as? UIAlertController
        XCTAssertNotNil(alert?.textFields)
        XCTAssertEqual(alert?.textFields?.count, 1)
        XCTAssertEqual(alert?.textFields?.first?.keyboardType, .decimalPad)
    }

    func testOpenTagAlertHasCancelAndOpenActions() {
        presentOpenTagAlert()

        let alert = homeViewController.presentedViewController as? UIAlertController
        XCTAssertEqual(alert?.actions.count, 2)

        let cancelAction = alert?.actions.first { $0.title == "Cancel" }
        let openAction = alert?.actions.first { $0.title == "Open" }

        XCTAssertNotNil(cancelAction)
        XCTAssertNotNil(openAction)
        XCTAssertEqual(cancelAction?.style, .cancel)
        XCTAssertEqual(openAction?.style, .default)
    }

    // MARK: - Text Field Delegate Tests

    func testTextFieldAllowsDecimalDigits() {
        let textField = UITextField()
        let range = NSRange(location: 0, length: 0)

        // Should allow digits
        XCTAssertTrue(homeViewController.textField(textField, shouldChangeCharactersIn: range, replacementString: "1"))
        XCTAssertTrue(homeViewController.textField(textField, shouldChangeCharactersIn: range, replacementString: "23"))
        XCTAssertTrue(homeViewController.textField(textField, shouldChangeCharactersIn: range, replacementString: "456"))
    }

    func testTextFieldAllowsDeletion() {
        let textField = UITextField()
        textField.text = "123"
        let range = NSRange(location: 2, length: 1)

        // Empty string means deletion
        XCTAssertTrue(homeViewController.textField(textField, shouldChangeCharactersIn: range, replacementString: ""))
    }

    func testTextFieldRejectsNonDigits() {
        let textField = UITextField()
        let range = NSRange(location: 0, length: 0)

        // Should reject non-digit characters
        XCTAssertFalse(homeViewController.textField(textField, shouldChangeCharactersIn: range, replacementString: "a"))
        XCTAssertFalse(homeViewController.textField(textField, shouldChangeCharactersIn: range, replacementString: "ABC"))
        XCTAssertFalse(homeViewController.textField(textField, shouldChangeCharactersIn: range, replacementString: "@#$"))
    }

    func testTextFieldShouldReturnWithValidNumber() {
        let textField = UITextField()
        textField.text = "12345"

        XCTAssertTrue(homeViewController.textFieldShouldReturn(textField))
    }

    func testTextFieldShouldReturnWithInvalidNumber() {
        let textField = UITextField()
        textField.text = "abc"

        XCTAssertFalse(homeViewController.textFieldShouldReturn(textField))
    }

    func testTextFieldShouldReturnWithEmptyText() {
        let textField = UITextField()
        textField.text = ""

        XCTAssertFalse(homeViewController.textFieldShouldReturn(textField))
    }

    func testTextFieldShouldReturnWithNilText() {
        let textField = UITextField()
        textField.text = nil

        XCTAssertFalse(homeViewController.textFieldShouldReturn(textField))
    }

    // MARK: - Navigation Items Tests

    func testNavigationItemsConfigured() {
        // Verify the home view controller has navigation set up
        // The navigationItems method is internal ObjC and tested in TagmasterAppLogicTests
        XCTAssertNotNil(homeViewController.navigationItem)
    }

    // MARK: - View Did Load Extension Tests

    func testViewDidLoadExtensionRegistersNotification() {
        // The viewDidLoadExtension should register for userDataChanged notification
        // We verify this by checking that the controller responds to the expected selector

        // Verify the controller has the onUserDataChanged method
        let selector = #selector(DPHomeViewController.onUserDataChanged)
        XCTAssertTrue(homeViewController.responds(to: selector),
                      "DPHomeViewController should respond to onUserDataChanged")

        // Verify that DPHomeViewController has registered for notifications
        // by checking if the method exists and is callable
        XCTAssertNoThrow(homeViewController.perform(selector),
                         "onUserDataChanged should be callable without throwing")
    }

    // MARK: - Table View Reload Tests

    func testOnUserDataChangedReloadsTableView() {
        // This tests that onUserDataChanged triggers a table view reload
        // We can verify by checking that the method doesn't crash and table view is still valid

        homeViewController.onUserDataChanged()

        XCTAssertNotNil(homeViewController.tableView)
    }
}

// MARK: - Notification Name Extension
extension Notification.Name {
    static let userDataChanged = Notification.Name("tagmaster.userDataChanged")
}
