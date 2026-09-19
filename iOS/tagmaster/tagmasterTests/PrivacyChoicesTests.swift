import XCTest
@testable import tagmaster

final class PrivacyChoicesTests: XCTestCase {
    func testDefaultsPersistenceAndIndependentRevocation() {
        let suite = "PrivacyChoicesTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        defer { defaults.removePersistentDomain(forName: suite) }
        let choices = PrivacyChoices(defaults: defaults)
        XCTAssertFalse(choices.hasChosen)
        XCTAssertFalse(choices.analytics)
        XCTAssertFalse(choices.crashes)
        defaults.set(true, forKey: "telemetry.analytics")
        XCTAssertFalse(choices.analytics, "A stored flag is not consent without an explicit choice")
        choices.save(analytics: true, crashes: false)
        let restored = PrivacyChoices(defaults: UserDefaults(suiteName: suite)!)
        XCTAssertTrue(restored.hasChosen)
        XCTAssertTrue(restored.analytics)
        XCTAssertFalse(restored.crashes)
        choices.save(analytics: false, crashes: true)
        XCTAssertFalse(restored.analytics)
        XCTAssertTrue(restored.crashes)
        choices.save(analytics: false, crashes: false)
        XCTAssertTrue(restored.hasChosen)
        XCTAssertFalse(restored.analytics)
        XCTAssertFalse(restored.crashes)
    }
}
