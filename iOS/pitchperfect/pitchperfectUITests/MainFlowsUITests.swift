//
//  MainFlowsUITests.swift
//  pitchperfectUITests
//
//  The app's main flows in the real app process, through real touches: the
//  pitch pipe, Notes and Keys, a song's life (add, edit, delete), a set list's
//  (create, manage, delete) and a setting that outlives its sheet. The hosted
//  unit tests cover each screen's rules; these prove the wiring survives the
//  real launch path and real input.
//

import XCTest

final class MainFlowsUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
        app = XCUIApplication()
        app.launchArguments = ["--uitesting",
                               "-telemetry.chosen", "YES", "-telemetry.analytics", "NO", "-telemetry.crashes", "NO"]
        app.launch()
    }

    private func tab(_ name: String) {
        let item = app.tabBars.buttons[name]
        XCTAssertTrue(item.waitForExistence(timeout: 10), "\(name) tab")
        item.tap()
    }

    private func element(_ label: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }

    private func element(beginningWith prefix: String) -> XCUIElement {
        app.descendants(matching: .any).matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
    }

    func testThePitchPipeSwitchesRangeByTouch() {
        tab("Pitch Pipe")
        let low = element("Octave range C to C")
        let high = element("Octave range F to F")
        XCTAssertTrue(high.waitForExistence(timeout: 10))
        high.tap()
        XCTAssertTrue(high.isSelected, "a touch on F TO F selects it")
        XCTAssertFalse(low.isSelected)
        XCTAssertTrue(element("F, octave 5").exists, "the cells now run F to F")
        low.tap()
        XCTAssertTrue(low.isSelected)
        XCTAssertTrue(element("C, octave 5").exists)
    }

    func testNotesAndKeysListTheirRowsAndSwitchMode() {
        tab("Notes")
        XCTAssertTrue(element(beginningWith: "C 4").waitForExistence(timeout: 10), "Notes lists C 4")
        tab("Keys")
        let minor = app.navigationBars.buttons["Minor"]
        XCTAssertTrue(minor.waitForExistence(timeout: 10))
        XCTAssertTrue(element("C major, no sharps or flats").exists)
        minor.tap()
        XCTAssertTrue(element("A minor, no sharps or flats").waitForExistence(timeout: 5), "Minor lists the minor keys")
        app.navigationBars.buttons["Major"].tap()
        XCTAssertTrue(element("C major, no sharps or flats").waitForExistence(timeout: 5))
    }

    func testASongIsAddedEditedAndDeleted() {
        tab("Songs")
        let title = "Flow \(Int(Date().timeIntervalSince1970) % 100_000)"
        app.navigationBars.buttons["Edit"].tap()
        app.navigationBars.buttons["Add"].tap()
        let field = app.textFields["songTitleField"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText(title + "\n")
        let g = app.buttons["key-G1"]
        XCTAssertTrue(g.waitForExistence(timeout: 5))
        g.tap()
        app.navigationBars["Add Song"].buttons["Done"].tap()
        let row = app.buttons["\(title), G"]
        XCTAssertTrue(row.waitForExistence(timeout: 10), "the new song's row, in G")

        // Edit: the row's More Info opens the editor on it.
        let rows = app.buttons.matching(identifier: "song.edit")
        rows.element(boundBy: rows.count - 1).tap()
        let editField = app.textFields["songTitleField"]
        XCTAssertTrue(app.navigationBars["Edit Song"].waitForExistence(timeout: 10))
        editField.tap()
        editField.typeText(" Renamed\n")
        app.navigationBars["Edit Song"].buttons["Done"].tap()
        let renamed = app.buttons["\(title) Renamed, G"]
        XCTAssertTrue(renamed.waitForExistence(timeout: 10), "the row follows the edit")
        app.navigationBars.buttons["Done"].tap()

        renamed.swipeLeft()
        let delete = app.buttons["Delete"].firstMatch
        XCTAssertTrue(delete.waitForExistence(timeout: 5))
        delete.tap()
        XCTAssertTrue(renamed.waitForNonExistence(timeout: 5))
    }

    func testASetListIsCreatedManagedAndDeleted() {
        tab("Songs")
        let name = "Gig \(Int(Date().timeIntervalSince1970) % 100_000)"
        app.buttons["setlist.new"].tap()
        let field = app.textFields["setlist.name.field"]
        XCTAssertTrue(field.waitForExistence(timeout: 5))
        field.typeText(name)
        app.alerts.buttons["Create"].tap()
        let position = element(beginningWith: "\(name), ")
        XCTAssertTrue(position.waitForExistence(timeout: 5))
        XCTAssertTrue(position.isSelected, "the new set list is the current one")
        XCTAssertTrue(element(beginningWith: "Nothing in this set list").exists, "its empty state")

        // Manage: the Set Lists screen names it as current, and deletes it.
        app.navigationBars.buttons["Edit"].tap()
        app.navigationBars.buttons["More"].tap()
        app.buttons["Manage set lists…"].tap()
        XCTAssertTrue(app.navigationBars["Set Lists"].waitForExistence(timeout: 5))
        XCTAssertTrue(element(beginningWith: "\(name), no songs, current").exists)
        app.buttons["Actions for \(name)"].tap()
        app.buttons["Delete set list…"].tap()
        app.alerts.buttons["Delete"].tap()
        XCTAssertTrue(element(beginningWith: "\(name), ").waitForNonExistence(timeout: 5))
        app.navigationBars.buttons.element(boundBy: 0).tap()
        XCTAssertTrue(element(beginningWith: "My Songs").waitForExistence(timeout: 5), "back on My Songs")
    }

    func testASettingOutlivesItsSheet() {
        tab("Pitch Pipe")
        app.navigationBars.buttons["Settings"].tap()
        let toggle = app.switches.matching(NSPredicate(format: "label BEGINSWITH %@", "Toggle Notes")).firstMatch
        XCTAssertTrue(toggle.waitForExistence(timeout: 5))
        let before = toggle.value as? String
        toggle.tap()
        XCTAssertNotEqual(toggle.value as? String, before)
        app.navigationBars["Settings"].buttons["Done"].tap()

        app.navigationBars.buttons["Settings"].tap()
        XCTAssertTrue(toggle.waitForExistence(timeout: 5))
        XCTAssertNotEqual(toggle.value as? String, before, "the choice was kept")
        toggle.tap()
        XCTAssertEqual(toggle.value as? String, before)
        app.navigationBars["Settings"].buttons["Done"].tap()
    }
}
