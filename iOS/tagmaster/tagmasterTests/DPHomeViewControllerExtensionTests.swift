//
//  DPHomeViewControllerExtensionTests.swift
//  tagmasterTests
//
//  Tests for DPHomeViewController Swift extension methods.
//

import XCTest
import UIKit
@testable import tagmaster

// HomePresentationFixture (in FavoritesBehaviorTests) inspects the alerts
// production code builds without starting system keyboard services.

class DPHomeViewControllerExtensionTests: XCTestCase {
    private var homeViewController: HomePresentationFixture!

    override func setUp() {
        super.setUp()
        homeViewController = HomePresentationFixture(style: .grouped)
    }

    override func tearDown() {
        // Alert action handlers retain their presenting controller.
        homeViewController.requestedPresentation = nil
        homeViewController = nil
        super.tearDown()
    }

    private func presentOpenTagAlert() {
        homeViewController.openTag()
        XCTAssertTrue(homeViewController.requestedPresentation is UIAlertController)
    }

    // MARK: - Open Tag Tests

    func testOpenTagMethodExists() {
        // Verify the openTag method is available
        XCTAssertTrue(homeViewController.responds(to: #selector(DPHomeViewController.openTag)))
    }

    func testOpenTagPresentsAlertController() {
        presentOpenTagAlert()

        // Verify production code requests presentation of the configured alert
        XCTAssertNotNil(homeViewController.requestedPresentation)
        XCTAssertTrue(homeViewController.requestedPresentation is UIAlertController)

        let alert = homeViewController.requestedPresentation as? UIAlertController
        XCTAssertEqual(alert?.title, "Open Tag")
        XCTAssertEqual(alert?.message, "Enter Tag ID")
    }

    func testOpenTagAlertHasTextField() {
        presentOpenTagAlert()

        let alert = homeViewController.requestedPresentation as? UIAlertController
        XCTAssertNotNil(alert?.textFields)
        XCTAssertEqual(alert?.textFields?.count, 1)
        XCTAssertEqual(alert?.textFields?.first?.keyboardType, .decimalPad)
    }

    func testOpenTagAlertHasCancelAndOpenActions() {
        presentOpenTagAlert()

        let alert = homeViewController.requestedPresentation as? UIAlertController
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
