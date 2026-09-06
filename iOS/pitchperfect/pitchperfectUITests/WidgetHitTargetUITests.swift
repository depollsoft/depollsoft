import XCTest

extension XCUIApplication {
    /// A widget cell by its spoken name. Cells are toggles; their value is "1" while sounding.
    func pitchCell(_ label: String) -> XCUIElement {
        descendants(matching: .any).matching(NSPredicate(format: "label == %@", label)).firstMatch
    }
}

extension XCUIElement {
    var isSounding: Bool { (value as? String) == "1" }
}

extension XCTestCase {
    /// Adds the Pitch Perfect widget to the Home Screen and leaves it showing C to C.
    func addPitchPipeWidget(to springboard: XCUIApplication) {
        let emptyHome = springboard.windows.firstMatch.coordinate(
            withNormalizedOffset: CGVector(dx: 0.5, dy: 0.72)
        )
        emptyHome.press(forDuration: 1.3)
        let editHome = springboard.buttons["Edit Home Screen"]
        if editHome.waitForExistence(timeout: 2) {
            editHome.tap()
        } else {
            let edit = springboard.buttons["Edit"].firstMatch
            if edit.waitForExistence(timeout: 2) {
                edit.tap()
            }
        }
        let addWidget = springboard.buttons["Add Widget"].firstMatch
        XCTAssertTrue(addWidget.waitForExistence(timeout: 5), "SpringBoard should expose Add Widget")
        addWidget.tap()

        let search = springboard.searchFields.firstMatch
        XCTAssertTrue(search.waitForExistence(timeout: 5), "Widget gallery should expose search")
        search.tap()
        search.typeText("Pitch Perfect")

        let pitchPerfect = springboard.staticTexts["Pitch Perfect"].firstMatch
        XCTAssertTrue(pitchPerfect.waitForExistence(timeout: 5), "Pitch Perfect should appear in the widget gallery")
        let resultCell = springboard.cells.containing(.staticText, identifier: "Pitch Perfect").firstMatch
        if resultCell.exists {
            resultCell.tap()
        } else {
            pitchPerfect.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        }
        sleep(2)

        let addButton = springboard.buttons.matching(
            NSPredicate(format: "label CONTAINS %@", "Add Widget")
        ).firstMatch
        XCTAssertTrue(addButton.waitForExistence(timeout: 5))
        addButton.tap()
        XCTAssertTrue(springboard.wait(for: .runningForeground, timeout: 5))
        let done = springboard.buttons["Done"].firstMatch
        if done.waitForExistence(timeout: 3) {
            done.tap()
        }

        // Shared widget state survives reinstalls, so start every run from C to C.
        let cToC = springboard.buttons["Octave range C to C"].firstMatch
        XCTAssertTrue(cToC.waitForExistence(timeout: 8), "The widget should expose its range control")
        if !cToC.isSelected {
            cToC.tap()
            XCTAssertTrue(springboard.pitchCell("C, octave 4").waitForExistence(timeout: 8))
        }
    }
}

final class WidgetHitTargetUITests: XCTestCase {
    private let noteLabels = [
        "C, octave 4", "C sharp, octave 4", "D, octave 4", "D sharp, octave 4", "E, octave 4",
        "F, octave 4", "F sharp, octave 4", "G, octave 4", "G sharp, octave 4", "A, octave 4",
        "A sharp, octave 4", "B, octave 4", "C, octave 5",
    ]

    func testEveryCellHasItsOwnCompactHitTarget() throws {
        let app = XCUIApplication()
        app.launchArguments = ["--uitesting"]
        app.launch()
        XCUIDevice.shared.press(.home)
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        addPitchPipeWidget(to: springboard)

        var centers: [String: CGPoint] = [:]
        for label in noteLabels {
            let cell = springboard.pitchCell(label)
            XCTAssertTrue(cell.waitForExistence(timeout: 5), "\(label) should be on the face")
            let frame = cell.frame
            XCTAssertGreaterThanOrEqual(min(frame.width, frame.height), 44, "\(label) is below the 44pt floor")
            XCTAssertLessThanOrEqual(max(frame.width, frame.height), 72, "\(label) hit target spans more than one cell")
            centers[label] = CGPoint(x: frame.midX, y: frame.midY)
        }
        let points = Array(centers.values)
        for (index, point) in points.enumerated() {
            for other in points[(index + 1)...] where index + 1 < points.count {
                XCTAssertGreaterThan(hypot(point.x - other.x, point.y - other.y), 36, "Two cells share a hit target")
            }
        }

        func playing() -> [String] {
            noteLabels.filter { springboard.pitchCell($0).isSounding }
        }
        func waitUntilPlaying(_ expected: [String]) {
            let deadline = Date().addingTimeInterval(4)
            while Date() < deadline, playing() != expected {
                usleep(200_000)
            }
            XCTAssertEqual(expected, playing())
        }

        springboard.pitchCell("D, octave 4").tap()
        waitUntilPlaying(["D, octave 4"])
        springboard.pitchCell("A, octave 4").tap()
        waitUntilPlaying(["D, octave 4", "A, octave 4"])
        springboard.pitchCell("A, octave 4").tap()
        waitUntilPlaying(["D, octave 4"])
        springboard.pitchCell("D, octave 4").tap()
        waitUntilPlaying([])
    }
}
