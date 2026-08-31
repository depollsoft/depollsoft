import XCTest
@testable import pitchperfect

final class SwiftUIMigrationTests: XCTestCase {
    private let wakeLockKey = "depollsoft.pitchperfect.WakeLock"
    private let toggleNoteKey = "depollsoft.pitchperfect.ToggleNote"

    override func tearDown() {
        UserDefaults.standard.removeObject(forKey: wakeLockKey)
        UserDefaults.standard.removeObject(forKey: toggleNoteKey)
        super.tearDown()
    }

    func testSettingsSingletonPreservesStoredValues() {
        UserDefaults.standard.set(true, forKey: wakeLockKey)
        UserDefaults.standard.set(true, forKey: toggleNoteKey)

        let settings = DPSettingsModel.sharedInstance

        XCTAssertTrue(settings.wakeLock)
        XCTAssertTrue(settings.toggleNotes)
    }

    func testSettingsChangesPersistAndNotify() {
        let settings = DPSettingsModel.sharedInstance
        let notification = expectation(
            forNotification: DPSettingsModel.settingsChangedNotificationName,
            object: settings
        )

        settings.toggleNotes = true

        wait(for: [notification], timeout: 1)
        XCTAssertTrue(UserDefaults.standard.bool(forKey: toggleNoteKey))
    }

    func testKeySignatureGlyphsCoverNaturalsSharpsAndFlats() {
        XCTAssertEqual(KeySignatureDisplay(numAccidentals: 0).signatureText, "&")
        XCTAssertEqual(KeySignatureDisplay(numAccidentals: 1).signatureText, "&\u{00A1}")
        XCTAssertEqual(KeySignatureDisplay(numAccidentals: -1).signatureText, "&\u{00A8}")
    }

    func testBannerBridgeCreatesAdaptiveAdContainer() {
        let banner = DPBannerAdView(frame: CGRect(x: 0, y: 0, width: 390, height: 50))

        XCTAssertFalse(banner.subviews.isEmpty)
        XCTAssertEqual(banner.intrinsicContentSize.height, 50)
    }

    func testEditingSongPreservesStableIdentity() {
        let song = DPPitchedSong()
        let originalID = song.id
        let keys = DPKey.minorKeys()?.compactMap { $0 as? DPKey } ?? []

        song.name = "Updated"
        song.key = keys.first

        XCTAssertEqual(song.id, originalID)
        XCTAssertEqual(song.name, "Updated")
        XCTAssertTrue(song.key === keys.first)
    }
}
