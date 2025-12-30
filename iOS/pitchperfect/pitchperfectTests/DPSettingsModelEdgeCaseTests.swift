import XCTest
@testable import pitchperfect

// MARK: - DPSettingsModel Additional Tests

final class DPSettingsModelEdgeCaseTests: XCTestCase {
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
        model.detachFromFirestore()
        super.tearDown()
    }
    
    // MARK: - Rapid Toggle Tests
    
    func testRapidWakeLockToggle() {
        let model = DPSettingsModel.sharedInstance
        let initial = model.wakeLock
        
        for _ in 0..<20 {
            model.wakeLock.toggle()
        }
        
        // After even number of toggles, should be back to initial
        XCTAssertEqual(model.wakeLock, initial)
    }
    
    func testRapidToggleNotesToggle() {
        let model = DPSettingsModel.sharedInstance
        let initial = model.toggleNotes
        
        for _ in 0..<20 {
            model.toggleNotes.toggle()
        }
        
        // After even number of toggles, should be back to initial
        XCTAssertEqual(model.toggleNotes, initial)
    }
    
    // MARK: - Concurrent Access Tests
    
    func testConcurrentWakeLockAccess() {
        let model = DPSettingsModel.sharedInstance
        let expectation = self.expectation(description: "concurrent access")
        expectation.expectedFulfillmentCount = 10
        
        for _ in 0..<10 {
            DispatchQueue.global().async {
                model.wakeLock = true
                _ = model.wakeLock
                model.wakeLock = false
                expectation.fulfill()
            }
        }
        
        waitForExpectations(timeout: 5.0)
    }
    
    func testConcurrentToggleNotesAccess() {
        let model = DPSettingsModel.sharedInstance
        let expectation = self.expectation(description: "concurrent access")
        expectation.expectedFulfillmentCount = 10
        
        for _ in 0..<10 {
            DispatchQueue.global().async {
                model.toggleNotes = true
                _ = model.toggleNotes
                model.toggleNotes = false
                expectation.fulfill()
            }
        }
        
        waitForExpectations(timeout: 5.0)
    }
    
    // MARK: - UserDefaults Key Tests
    
    func testWakeLockUsesCorrectUserDefaultsKey() {
        let model = DPSettingsModel.sharedInstance
        
        model.wakeLock = true
        XCTAssertTrue(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.WakeLock"))
        
        model.wakeLock = false
        XCTAssertFalse(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.WakeLock"))
    }
    
    func testToggleNotesUsesCorrectUserDefaultsKey() {
        let model = DPSettingsModel.sharedInstance
        
        model.toggleNotes = true
        XCTAssertTrue(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.ToggleNote"))
        
        model.toggleNotes = false
        XCTAssertFalse(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.ToggleNote"))
    }
    
    // MARK: - Notification Name Tests
    
    func testSettingsChangedNotificationNameConstant() {
        XCTAssertEqual(DPSettingsModel.settingsChangedNotificationName, 
                      Notification.Name("pitchPerfect.settingsChanged"))
    }
    
    func testSettingsChangedNotificationNameMatchesExtension() {
        XCTAssertEqual(DPSettingsModel.settingsChangedNotificationName, 
                      Notification.Name.settingsChanged)
    }
    
    // MARK: - Shared Instance Behavior
    
    func testSharedInstanceIsSingleton() {
        let instance1 = DPSettingsModel.sharedInstance
        let instance2 = DPSettingsModel.sharedInstance
        let instance3 = DPSettingsModel.sharedInstance
        
        XCTAssertTrue(instance1 === instance2)
        XCTAssertTrue(instance2 === instance3)
    }
    
    func testChangesToSharedInstancePersist() {
        let instance1 = DPSettingsModel.sharedInstance
        instance1.wakeLock = true
        
        let instance2 = DPSettingsModel.sharedInstance
        XCTAssertTrue(instance2.wakeLock)
        
        instance2.wakeLock = false
        XCTAssertFalse(instance1.wakeLock)
    }
    
    // MARK: - Detach Firestore Tests
    
    func testDetachFromFirestoreMultipleTimes() {
        let model = DPSettingsModel.sharedInstance
        
        // Should not crash when called multiple times
        for _ in 0..<10 {
            model.detachFromFirestore()
        }
        
        XCTAssertTrue(true, "Multiple detach calls completed")
    }
    
    func testDetachFromFirestoreIsIdempotent() {
        let model = DPSettingsModel.sharedInstance
        
        model.detachFromFirestore()
        
        // Should be safe to modify settings after detach
        model.wakeLock = true
        XCTAssertTrue(model.wakeLock)
        
        model.detachFromFirestore()
        
        // Should still be able to modify
        model.wakeLock = false
        XCTAssertFalse(model.wakeLock)
    }
    
    // MARK: - Notification Object Tests
    
    func testWakeLockNotificationIncludesSelf() {
        let model = DPSettingsModel.sharedInstance
        
        let expectation = self.expectation(description: "notification")
        var receivedObject: Any?
        
        let token = NotificationCenter.default.addObserver(
            forName: DPSettingsModel.settingsChangedNotificationName,
            object: model,
            queue: nil
        ) { notification in
            receivedObject = notification.object
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }
        
        model.wakeLock.toggle()
        
        waitForExpectations(timeout: 1.0)
        XCTAssertTrue(receivedObject as AnyObject === model)
    }
    
    func testToggleNotesNotificationIncludesSelf() {
        let model = DPSettingsModel.sharedInstance
        
        let expectation = self.expectation(description: "notification")
        var receivedObject: Any?
        
        let token = NotificationCenter.default.addObserver(
            forName: DPSettingsModel.settingsChangedNotificationName,
            object: model,
            queue: nil
        ) { notification in
            receivedObject = notification.object
            expectation.fulfill()
        }
        defer { NotificationCenter.default.removeObserver(token) }
        
        model.toggleNotes.toggle()
        
        waitForExpectations(timeout: 1.0)
        XCTAssertTrue(receivedObject as AnyObject === model)
    }
    
    // MARK: - Initial State Tests
    
    func testDefaultWakeLockValue() {
        // Clear any existing value
        UserDefaults.standard.removeObject(forKey: "depollsoft.pitchperfect.WakeLock")
        
        // UserDefaults returns false for unset bools
        let value = UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.WakeLock")
        XCTAssertFalse(value)
    }
    
    func testDefaultToggleNotesValue() {
        // Clear any existing value
        UserDefaults.standard.removeObject(forKey: "depollsoft.pitchperfect.ToggleNote")
        
        // UserDefaults returns false for unset bools
        let value = UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.ToggleNote")
        XCTAssertFalse(value)
    }
}
