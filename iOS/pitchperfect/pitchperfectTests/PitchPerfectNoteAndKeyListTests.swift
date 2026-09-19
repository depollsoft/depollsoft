import UIKit
import XCTest
@testable import pitchperfect

@MainActor
final class PitchPerfectNoteAndKeyListTests: PitchPerfectControllerTestCase {
    func testNotesListDisplaysNamesAndFrequenciesAndPlaysThePressedRow() throws {
        try withApp { tabs in
            let controller = try select(1, in: tabs, as: DPNotesViewController.self)
            let table = try table(in: controller)
            let notes = try XCTUnwrap(DPNote.prunedNotes() as? [DPNote])
            XCTAssertGreaterThan(notes.count, 0)
            XCTAssertEqual(table.numberOfRows(inSection: 0), notes.count)
            let firstCell = try cell(0, in: table)
            XCTAssertEqual(firstCell.detailTextLabel?.text, String(format: "%.2f Hz", notes[0].frequency))
            XCTAssertTrue(labels(in: firstCell).contains { $0.hasPrefix(notes[0].friendlyName) })
            assertPressAndRelease(firstCell, note: notes[0])
        }
    }

    func testMajorAndMinorListsDisplayAllKeySignatures() throws {
        try withApp { tabs in
            let controller = try select(2, in: tabs, as: DPKeysViewController.self)
            let table = try table(in: controller)
            let chooser = try XCTUnwrap(controller.navigationItem.leftBarButtonItem?.customView as? UISegmentedControl)
            XCTAssertEqual(chooser.titleForSegment(at: 0), "Major")
            XCTAssertEqual(chooser.titleForSegment(at: 1), "Minor")
            let signatures = ["&€", "&¬", "&«", "&ª", "&©", "&¨", "&", "&¡", "&¢", "&£", "&¤", "&¥", "&¦"]
            let names = [
                ["G", "D", "A", "E", "B", "F", "C", "G", "D", "A", "E", "B", "F"],
                ["e", "b", "f", "c", "g", "d", "a", "e", "b", "f", "c", "g", "d"],
            ]
            for mode in 0...1 {
                chooser.selectedSegmentIndex = mode
                chooser.sendActions(for: .valueChanged)
                XCTAssertEqual(table.numberOfRows(inSection: 0), 13)
                for row in 0..<13 {
                    let rowLabels = labels(in: try cell(row, in: table))
                    XCTAssertTrue(rowLabels.contains(names[mode][row]), "Missing key name at \(mode):\(row)")
                    XCTAssertTrue(rowLabels.contains(signatures[row]), "Wrong key signature at \(mode):\(row)")
                }
            }
        }
    }

    func testDifferentKeysRespondToRapidPressAndRelease() throws {
        try withApp { tabs in
            let controller = try select(2, in: tabs, as: DPKeysViewController.self)
            let table = try table(in: controller)
            let keys = try XCTUnwrap(DPKey.majorKeys() as? [DPKey])
            XCTAssertEqual(table.numberOfRows(inSection: 0), 13)
            for row in 0..<5 {
                let keyCell = try cell(row, in: table)
                XCTAssertEqual(keyCell.value(forKey: "key") as? DPKey, keys[row])
                assertPressAndRelease(keyCell, note: keys[row].note)
            }
        }
    }

    func testKeyListScrollsToBothEndsWithoutLosingRows() throws {
        try withApp { tabs in
            let controller = try select(2, in: tabs, as: DPKeysViewController.self)
            let table = try table(in: controller)
            for row in [12, 0] {
                let path = IndexPath(row: row, section: 0)
                table.scrollToRow(at: path, at: .middle, animated: false)
                table.layoutIfNeeded()
                XCTAssertNotNil(table.cellForRow(at: path))
                XCTAssertEqual(table.numberOfRows(inSection: 0), 13)
            }
        }
    }
}
