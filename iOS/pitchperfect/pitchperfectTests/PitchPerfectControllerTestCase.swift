import FirebaseCore
import UIKit
import XCTest
@testable import pitchperfect

/// Hosts the shipping storyboard and navigation setup, using local state only.
/// The app does not consume --uitesting; UI tests previously used ordinary defaults.
@MainActor
class PitchPerfectControllerTestCase: XCTestCase {
    func withApp(_ body: (UITabBarController) throws -> Void) throws {
        let defaults = UserDefaults.standard
        let prefix = "depollsoft.pitchperfect."
        let savedDefaults = defaults.dictionaryRepresentation().filter { $0.key.hasPrefix(prefix) }
        let settings = DPSettingsModel.sharedInstance
        let songs = DPSongsModel.sharedInstance
        let savedLists = songs.songLists
        let savedAnimations = UIView.areAnimationsEnabled
        let savedAdEnvironment = ProcessInfo.processInfo.environment["STORE_SCREENSHOTS"]
        let previousKeyWindow = UIApplication.shared.windows.first { $0.isKeyWindow }
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 375, height: 667))
        defer {
            window.rootViewController?.dismiss(animated: false)
            window.isHidden = true
            window.rootViewController = nil
            previousKeyWindow?.makeKey()
            for note in DPNote.commonNotes() as! [DPNote] { note.stop() }
            songs.songLists = savedLists
            for key in defaults.dictionaryRepresentation().keys where key.hasPrefix(prefix) {
                defaults.removeObject(forKey: key)
            }
            for (key, value) in savedDefaults { defaults.set(value, forKey: key) }
            NotificationCenter.default.post(name: .settingsChanged, object: settings)
            UIView.setAnimationsEnabled(savedAnimations)
            if let savedAdEnvironment {
                setenv("STORE_SCREENSHOTS", savedAdEnvironment, 1)
            } else {
                unsetenv("STORE_SCREENSHOTS")
            }
        }
        // Reuse the capture suite's existing no-ad-request switch. No SDK/network
        // work is needed to verify controller content or actions.
        setenv("STORE_SCREENSHOTS", "1", 1)
        UIView.setAnimationsEnabled(false)
        if FirebaseApp.app() == nil { FirebaseApp.configure() }
        settings.detachFromFirestore()
        settings.toggleNotes = false
        settings.wakeLock = false
        DPPitchPipeModel().isFromFToF = false
        for note in DPNote.commonNotes() as! [DPNote] { note.stop() }
        songs.songLists = ["default": DPSongList(id: "default")]

        let storyboard = UIStoryboard(name: "MainStoryboard_iPhone", bundle: Bundle(for: DPAppDelegate.self))
        let tabs = try XCTUnwrap(storyboard.instantiateInitialViewController() as? UITabBarController)
        window.rootViewController = tabs
        let delegate = DPAppDelegate()
        delegate.window = window
        // This is the same method called by application:didFinishLaunchingWithOptions:.
        delegate.perform(NSSelectorFromString("configureRootNavigationControllers"))
        window.makeKeyAndVisible()
        tabs.loadViewIfNeeded()
        tabs.view.layoutIfNeeded()
        // A test-hosted window never runs UIKit's own appearance machinery, so
        // the container is told it appeared exactly as a parent controller
        // would; `select` then hands the transitions on to each tab.
        tabs.beginAppearanceTransition(true, animated: false)
        tabs.endAppearanceTransition()
        defer {
            tabs.beginAppearanceTransition(false, animated: false)
            tabs.endAppearanceTransition()
        }
        try body(tabs)
    }

    func select<T: UIViewController>(_ index: Int, in tabs: UITabBarController, as type: T.Type) throws -> T {
        let outgoing = tabs.selectedIndex == index ? nil : tabs.selectedViewController
        outgoing?.beginAppearanceTransition(false, animated: false)
        tabs.selectedIndex = index
        outgoing?.endAppearanceTransition()
        let navigation = try XCTUnwrap(tabs.selectedViewController as? UINavigationController)
        let controller = try XCTUnwrap(navigation.topViewController as? T)
        controller.loadViewIfNeeded()
        tabs.view.layoutIfNeeded()
        controller.view.layoutIfNeeded()
        // viewDidAppear is where the shipping controllers load their live
        // content (the pitch pipe builds its instrument there), so the tab that
        // just became selected is driven through the same transition UIKit runs.
        navigation.beginAppearanceTransition(true, animated: false)
        navigation.endAppearanceTransition()
        controller.view.layoutIfNeeded()
        XCTAssertEqual(tabs.selectedIndex, index)
        XCTAssertNotNil(controller.view.window)
        return controller
    }

    func table(in controller: UIViewController) throws -> UITableView {
        let table = try XCTUnwrap(controller.value(forKey: "tableView") as? UITableView)
        table.reloadData()
        table.layoutIfNeeded()
        XCTAssertNotNil(table.window)
        XCTAssertFalse(table.isHidden)
        return table
    }

    func press(_ item: UIBarButtonItem?, file: StaticString = #filePath, line: UInt = #line) throws {
        let item = try XCTUnwrap(item, file: file, line: line)
        let action = try XCTUnwrap(item.action, file: file, line: line)
        XCTAssertTrue(item.isEnabled, file: file, line: line)
        XCTAssertTrue(UIApplication.shared.sendAction(action, to: item.target, from: item, for: nil),
                      file: file, line: line)
    }

    func settle(until condition: () -> Bool, file: StaticString = #filePath, line: UInt = #line) {
        let deadline = Date().addingTimeInterval(1)
        while !condition(), Date() < deadline {
            RunLoop.main.run(until: Date().addingTimeInterval(0.005))
        }
        XCTAssertTrue(condition(), file: file, line: line)
    }

    func descendants<T: UIView>(of type: T.Type, in root: UIView) -> [T] {
        var result = (root as? T).map { [$0] } ?? []
        for child in root.subviews { result += descendants(of: type, in: child) }
        return result
    }

    func labels(in root: UIView) -> [String] {
        descendants(of: UILabel.self, in: root).compactMap(\.text)
    }

    func seedSongs(_ names: [String]) {
        DPSongsModel.sharedInstance.defaultSongList.songs = names.map { name in
            let song = DPPitchedSong()
            song.name = name
            song.key = DPKey.majorKeys()[6] as? DPKey
            return song
        }
    }

    func cell(_ row: Int, in table: UITableView) throws -> UITableViewCell {
        let source = try XCTUnwrap(table.dataSource)
        return source.tableView(table, cellForRowAt: IndexPath(row: row, section: 0))
    }

    func assertPressAndRelease(_ cell: UITableViewCell, note: DPNote,
                               file: StaticString = #filePath, line: UInt = #line) {
        note.stop()
        // These cells implement touch handling directly, not didSelectRowAt.
        cell.touchesBegan([], with: nil)
        XCTAssertTrue(note.isPlaying, file: file, line: line)
        XCTAssertTrue(cell.isHighlighted, file: file, line: line)
        cell.touchesEnded([], with: nil)
        XCTAssertFalse(note.isPlaying, file: file, line: line)
        XCTAssertFalse(cell.isHighlighted, file: file, line: line)
    }
}

/// Only supplies a location; the real instrument's touch handlers do all work.
final class PitchPerfectTestTouch: UITouch {
    var point: CGPoint = .zero
    override func location(in view: UIView?) -> CGPoint { point }
}
