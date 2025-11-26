import XCTest
@testable import pitchperfect

final class DPSettingsModelTests: XCTestCase {
    private var originalWakeLock: Bool = false
    private var originalToggleNotes: Bool = false

    override func setUp() {
        super.setUp()
        let model = DPSettingsModel.sharedInstance
        originalWakeLock = model.wakeLock
        originalToggleNotes = model.toggleNotes
    }

    override func tearDown() {
        let model = DPSettingsModel.sharedInstance
        model.wakeLock = originalWakeLock
        model.toggleNotes = originalToggleNotes
        super.tearDown()
    }

    func testWakeLockPersistsAndPostsNotification() {
        let model = DPSettingsModel.sharedInstance
        let expectation = self.expectation(description: "wakeLock notification")
        let token = NotificationCenter.default.addObserver(forName: DPSettingsModel.settingsChangedNotificationName,
                                                           object: model,
                                                           queue: nil) { _ in
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }

        let newValue = !model.wakeLock
        model.wakeLock = newValue

        waitForExpectations(timeout: 1.0)
        XCTAssertEqual(model.wakeLock, newValue)

        // Re-reading ensures the change persisted through UserDefaults.
        XCTAssertEqual(DPSettingsModel.sharedInstance.wakeLock, newValue)
    }

    func testToggleNotesPersistsAndPostsNotification() {
        let model = DPSettingsModel.sharedInstance
        let expectation = self.expectation(description: "toggle notification")
        let token = NotificationCenter.default.addObserver(forName: DPSettingsModel.settingsChangedNotificationName,
                                                           object: model,
                                                           queue: nil) { _ in
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }

        let newValue = !model.toggleNotes
        model.toggleNotes = newValue

        waitForExpectations(timeout: 1.0)
        XCTAssertEqual(model.toggleNotes, newValue)
        XCTAssertEqual(DPSettingsModel.sharedInstance.toggleNotes, newValue)
    }
}

