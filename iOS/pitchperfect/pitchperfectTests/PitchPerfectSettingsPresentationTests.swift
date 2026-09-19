import UIKit
import XCTest
@testable import pitchperfect

@MainActor
final class PitchPerfectSettingsPresentationTests: PitchPerfectControllerTestCase {
    private func openSettings(from controller: UIViewController) throws -> DPSettingsViewController {
        let gear = try XCTUnwrap(controller.navigationItem.rightBarButtonItem)
        XCTAssertEqual(gear.accessibilityLabel, "Settings")
        XCTAssertEqual(gear.accessibilityIdentifier, "gearshape")
        try press(gear)
        settle { controller.presentedViewController != nil }
        let navigation = try XCTUnwrap(controller.presentedViewController as? UINavigationController)
        let settings = try XCTUnwrap(navigation.topViewController as? DPSettingsViewController)
        settle { settings.viewIfLoaded?.window != nil }
        settings.view.layoutIfNeeded()
        XCTAssertEqual(settings.navigationItem.title, "Settings")
        XCTAssertFalse(navigation.navigationBar.isHidden)
        XCTAssertNotNil(settings.view.window)
        return settings
    }

    func testEveryTabOpensSettingsWithContentAndDoneDismissesIt() throws {
        try withApp { tabs in
            for index in 0..<4 {
                let controller = try select(index, in: tabs, as: UIViewController.self)
                let settings = try openSettings(from: controller)
                let settingsTable = try table(in: settings)
                XCTAssertGreaterThanOrEqual(settingsTable.numberOfSections, 2)
                XCTAssertEqual(settingsTable.numberOfRows(inSection: 0), 3)
                XCTAssertEqual(try cell(0, in: settingsTable).textLabel?.text, "Toggle Notes")
                XCTAssertEqual(try cell(1, in: settingsTable).textLabel?.text, "Wake Lock")
                XCTAssertEqual(try cell(2, in: settingsTable).textLabel?.text, "Theme")
                XCTAssertEqual(settings.navigationItem.rightBarButtonItem?.accessibilityLabel, "Done")
                try press(settings.navigationItem.rightBarButtonItem)
                settle { controller.presentedViewController == nil }
                XCTAssertNotNil(controller.view.window)
                XCTAssertEqual(tabs.selectedIndex, index)
            }
        }
    }

    func testRepeatedSettingsPresentationUsesTheSameWorkingDoneAction() throws {
        try withApp { tabs in
            let pipe = try select(0, in: tabs, as: DPPitchPipeViewController.self)
            for _ in 0..<3 {
                let settings = try openSettings(from: pipe)
                XCTAssertEqual(try table(in: settings).numberOfRows(inSection: 0), 3)
                try press(settings.navigationItem.rightBarButtonItem)
                settle { pipe.presentedViewController == nil }
                XCTAssertNotNil(pipe.view.window)
            }
        }
    }

    func testSettingsSwitchesPersistAndReloadTheirVisibleState() throws {
        try withApp { tabs in
            let pipe = try select(0, in: tabs, as: DPPitchPipeViewController.self)
            let settings = try openSettings(from: pipe)
            let settingsTable = try table(in: settings)
            for (row, key) in ["ToggleNote", "WakeLock"].enumerated() {
                let control = try XCTUnwrap(try cell(row, in: settingsTable).accessoryView as? UISwitch)
                XCTAssertFalse(control.isOn)
                control.isOn = true
                control.sendActions(for: .valueChanged)
                XCTAssertTrue(UserDefaults.standard.bool(forKey: "depollsoft.pitchperfect.\(key)"))
                let reloaded = try XCTUnwrap(try cell(row, in: settingsTable).accessoryView as? UISwitch)
                XCTAssertTrue(reloaded.isOn)
            }
            try press(settings.navigationItem.rightBarButtonItem)
            settle { pipe.presentedViewController == nil }
            let reopened = try openSettings(from: pipe)
            let reopenedTable = try table(in: reopened)
            for row in 0...1 {
                XCTAssertTrue(try XCTUnwrap(try cell(row, in: reopenedTable).accessoryView as? UISwitch).isOn)
            }
            try press(reopened.navigationItem.rightBarButtonItem)
            settle { pipe.presentedViewController == nil }
        }
    }
}
