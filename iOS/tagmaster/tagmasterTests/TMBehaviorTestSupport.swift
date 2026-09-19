//
//  TMBehaviorTestSupport.swift
//  tagmasterTests
//
//  Shared rig for the in-process behaviour tests that replaced the XCUITest
//  suites. Mounts real controllers in a sized window and seeds the same
//  fixtures the UI tests seeded through launch arguments.
//

import XCTest
import UIKit
import ObjectiveC
@testable import tagmaster

/// Every catalog fetch the app makes - `+[DPTag loadTagById:refresh:]`,
/// `+[DPTag query...]`, `-[DPTagXMLParser parseWithUrl:]`, the sheet-music and
/// video-thumbnail downloads - funnels through this one synchronous method, so
/// replacing it is enough to take the whole test bundle off the network.
///
/// Without it a screen asked for a tag that is not in the cache reaches
/// www.barbershoptags.com and waits out `DPRemoteRequestTimeout` (15s), which
/// makes the outcome depend on the machine's connection. With it, the very same
/// production path runs - cache miss, query, parse, failure - and resolves in
/// microseconds with a controlled `NSURLErrorNotConnectedToInternet`.
final class TMBlockedNetwork {
    private static let lock = NSLock()
    private static var attempts: [URL] = []
    private static var outstanding = 0

    private let method: Method
    private var original: IMP?

    init() {
        TMBlockedNetwork.lock.lock()
        TMBlockedNetwork.attempts = []
        TMBlockedNetwork.outstanding = 0
        TMBlockedNetwork.lock.unlock()

        method = class_getClassMethod(DPRemoteLocation.self,
                                      #selector(DPRemoteLocation.data(withContentsOf:)))!
        let refusal: @convention(block) (AnyObject, NSURL?, NSErrorPointer) -> NSData? = { _, url, errorPointer in
            TMBlockedNetwork.began(url as URL?)
            defer { TMBlockedNetwork.finished() }
            errorPointer?.pointee = NSError(domain: NSURLErrorDomain,
                                            code: NSURLErrorNotConnectedToInternet,
                                            userInfo: nil)
            return nil
        }
        original = method_setImplementation(method, imp_implementationWithBlock(refusal))
    }

    func uninstall() {
        guard let original else { return }
        method_setImplementation(method, original)
        self.original = nil
    }

    /// The catalog URLs the app asked for while blocked. Lets a test prove it
    /// exercised the real load path rather than a short-circuit.
    var attemptedURLs: [URL] {
        TMBlockedNetwork.lock.lock()
        defer { TMBlockedNetwork.lock.unlock() }
        return TMBlockedNetwork.attempts
    }

    /// Requests that entered the boundary and have not returned yet.
    static var outstandingRequests: Int {
        lock.lock()
        defer { lock.unlock() }
        return outstanding
    }

    private static func began(_ url: URL?) {
        lock.lock()
        if let url { attempts.append(url) }
        outstanding += 1
        lock.unlock()
    }

    private static func finished() {
        lock.lock()
        outstanding -= 1
        lock.unlock()
    }
}

/// The defaults key the UI tests wrote with `-depollsoft.pitchperfect.lists`.
/// A launch argument beginning with `-` is only an NSUserDefaults override, so
/// writing the key here reproduces that fixture exactly.
let TMListsDefaultsKey = "depollsoft.pitchperfect.lists"

class TMBehaviorTestCase: XCTestCase {
    var window: UIWindow!
    private var previousKeyWindow: UIWindow?
    private var seededTagIds: [Int32] = []

    /// Installed for every test in these suites: the screens under test are
    /// driven from seeded fixtures, so any request that escapes to the live
    /// catalog is a bug in the test, not a condition worth waiting out.
    private(set) var network: TMBlockedNetwork!

    /// iPhone SE (3rd generation) portrait, the device the UI tests ran on.
    static let portrait = CGSize(width: 375, height: 667)
    static let landscape = CGSize(width: 667, height: 375)

    override func setUp() {
        super.setUp()
        // Animated table updates (Home ends editing animatedly when its last
        // favourite goes) stay queued past the end of a test and then fire into
        // a torn-down hierarchy. Running the rig without animations makes those
        // updates synchronous; it changes no assertion, only their timing.
        UIView.setAnimationsEnabled(false)
        network = TMBlockedNetwork()
        clearLists()
    }

    override func tearDown() {
        dismantleWindow()
        // Nothing may still be inside the network boundary when the real
        // implementation goes back: a request that outlived its test would run
        // against the live catalog during the next one.
        drainOutstandingRequests()
        network.uninstall()
        network = nil
        previousKeyWindow?.makeKey()
        previousKeyWindow = nil
        // Only now that nothing is mounted: a live table asked to build a row
        // for an evicted tag would send DPTagCell to the network.
        seededTagIds.forEach(TMBehaviorTestCase.evictCachedTag)
        seededTagIds = []
        clearLists()
        UIView.setAnimationsEnabled(true)
        super.tearDown()
    }

    /// Spins the run loop until no blocked request is still in flight, so every
    /// completion has landed before the test's fixtures and window go away.
    private func drainOutstandingRequests(timeout: TimeInterval = 5) {
        let deadline = Date(timeIntervalSinceNow: timeout)
        while TMBlockedNetwork.outstandingRequests > 0 && Date() < deadline {
            RunLoop.current.run(mode: .default, before: Date(timeIntervalSinceNow: 0.01))
        }
        XCTAssertEqual(TMBlockedNetwork.outstandingRequests, 0,
                       "A catalog request outlived the test that started it")
    }

    /// Tears the mounted hierarchy down and lets it actually deallocate. Several
    /// app controllers observe `userDataChanged` for their whole lifetime, so a
    /// controller left alive here would react to a later test's list changes.
    private func dismantleWindow() {
        // Let any update already in flight finish against a live data source.
        if window != nil { RunLoop.current.run(until: Date()) }
        if let root = window?.rootViewController {
            // Home, Teachable and the query lists observe `userDataChanged` and
            // `TMTagListDidChange` for their whole lifetime and never unregister.
            // A controller that outlives its test would otherwise react to the
            // next test's list changes with a stale table. Test-only cleanup;
            // the app's own registration is untouched.
            for controller in [root] + (root as? UINavigationController).map({ $0.viewControllers }).orEmpty
                + (root.children) {
                NotificationCenter.default.removeObserver(controller)
                for child in controller.children { NotificationCenter.default.removeObserver(child) }
            }
        }
        if let navigation = window?.rootViewController as? UINavigationController {
            navigation.setViewControllers([], animated: false)
        }
        window?.rootViewController = nil
        window?.isHidden = true
        window = nil
        // Drain the autorelease pool and one run-loop turn so the controllers
        // released above are gone before the next test seeds its lists.
        autoreleasepool { RunLoop.current.run(until: Date()) }
    }

    func clearLists() {
        UserDefaults.standard.removeObject(forKey: TMListsDefaultsKey)
    }

    /// Writes the favourites/teachable lists exactly as the UI tests' launch
    /// argument did, through UserDefaults rather than through the setters, so
    /// the read path under test is the production one.
    func seedLists(favorite: [Int] = [], teachable: [Int] = []) {
        var lists: [String: Any] = [:]
        if !favorite.isEmpty { lists["favorite"] = favorite }
        if !teachable.isEmpty { lists["teachable"] = teachable }
        UserDefaults.standard.set(lists, forKey: TMListsDefaultsKey)
    }

    // MARK: - Mounting

    @discardableResult
    func mount(_ controller: UIViewController, size: CGSize = TMBehaviorTestCase.portrait) -> UIWindow {
        if previousKeyWindow == nil {
            previousKeyWindow = UIApplication.shared.connectedScenes
                .compactMap { ($0 as? UIWindowScene)?.windows.first { $0.isKeyWindow } }.first
        }
        dismantleWindow()
        window = UIWindow(frame: CGRect(origin: .zero, size: size))
        window.backgroundColor = .systemBackground
        window.tintColor = .systemBlue
        window.rootViewController = controller
        window.makeKeyAndVisible()
        settle()
        return window
    }

    /// Mounts inside a navigation controller, which several screens need in
    /// order to push, and returns that navigation controller.
    @discardableResult
    func mountInNavigation(_ controller: UIViewController,
                           size: CGSize = TMBehaviorTestCase.portrait) -> UINavigationController {
        let navigation = UINavigationController(rootViewController: controller)
        mount(navigation, size: size)
        return navigation
    }

    /// Records what a screen asks to push without actually pushing it. Several
    /// destinations (Browse and the query results lists) start live catalog
    /// requests as soon as they appear, which a unit test has no reason to run;
    /// capturing the push keeps the routing assertion exact and synchronous.
    final class PushCapturingNavigation: UINavigationController {
        /// Off until the root is installed: UINavigationController pushes its own
        /// root controller through this method.
        var capturing = false
        var pushed: [UIViewController] = []
        override func pushViewController(_ viewController: UIViewController, animated: Bool) {
            guard capturing else { return super.pushViewController(viewController, animated: animated) }
            pushed.append(viewController)
        }
    }

    /// Mounts inside a navigation controller that records pushes instead of
    /// performing them.
    @discardableResult
    func mountCapturingPushes(_ controller: UIViewController,
                              size: CGSize = TMBehaviorTestCase.portrait) -> PushCapturingNavigation {
        let navigation = PushCapturingNavigation(rootViewController: controller)
        mount(navigation, size: size)
        navigation.capturing = true
        return navigation
    }

    /// Stops a page-based screen's query lists from fetching from the live
    /// catalog when they appear. `isLoading` is the controller's own guard, so
    /// this suppresses the request without altering anything else.
    func quiesceQueries(in pages: TMPageViewController) {
        pages.loadViewIfNeeded()
        for page in pages.viewControllers {
            (page as? DPTagQueryViewController)?.isLoading = true
        }
    }

    func resize(to size: CGSize) {
        window.frame = CGRect(origin: .zero, size: size)
        settle()
    }

    /// Lets UIKit apply traits, lay out and finish its current transaction.
    func settle(file: StaticString = #filePath, line: UInt = #line) {
        window?.rootViewController?.view.updateTraitsIfNeeded()
        window?.setNeedsLayout()
        window?.layoutIfNeeded()
        let flushed = expectation(description: "transaction flushed")
        CATransaction.begin()
        CATransaction.setCompletionBlock { flushed.fulfill() }
        CATransaction.commit()
        wait(for: [flushed], timeout: 5)
        window?.layoutIfNeeded()
    }

    /// Waits for a condition that a background queue will satisfy, without
    /// sleeping when it already holds.
    func waitUntil(_ description: String,
                   timeout: TimeInterval = 10,
                   file: StaticString = #filePath,
                   line: UInt = #line,
                   _ condition: @escaping () -> Bool) {
        if condition() { return }
        let predicate = XCTNSPredicateExpectation(predicate: NSPredicate { _, _ in
            self.window?.layoutIfNeeded()
            return condition()
        }, object: nil)
        XCTAssertEqual(XCTWaiter.wait(for: [predicate], timeout: timeout), .completed,
                       "Timed out waiting for \(description)", file: file, line: line)
    }

    /// Waits by pumping the main run loop instead of through
    /// `XCTNSPredicateExpectation`, whose one-second polling interval would
    /// otherwise dominate a wait that resolves in milliseconds. Use it for work
    /// that completes back on the main queue, such as a blocked catalog fetch.
    func spinUntil(_ description: String,
                   timeout: TimeInterval = 2,
                   file: StaticString = #filePath,
                   line: UInt = #line,
                   _ condition: () -> Bool) {
        let deadline = Date(timeIntervalSinceNow: timeout)
        while !condition() && Date() < deadline {
            RunLoop.current.run(mode: .default, before: Date(timeIntervalSinceNow: 0.005))
        }
        XCTAssertTrue(condition(), "Timed out waiting for \(description)", file: file, line: line)
    }

    // MARK: - Deterministic tag fixtures

    /// Builds a fully populated tag and puts it in the production cache, so
    /// every screen under test loads it through `+[DPTag loadTagById:refresh:]`
    /// without touching the network.
    @discardableResult
    func seedCachedTag(id: Int32 = 1809,
                       title: String = "Lost",
                       lyrics: String? = "And I will wait to face the skies",
                       withTracks: Bool = true,
                       withSheetMusic: Bool = true) -> DPTag {
        let tag = DPTag()
        tag.tagId = id
        tag.title = title
        tag.alternativeTitle = "Lost (Tag)"
        tag.lyrics = lyrics
        tag.notes = "A four-part tag."
        tag.writtenKey = "Bb"
        tag.parts = 4
        tag.tagType = "Classic"
        tag.arranger = "A. Arranger"
        tag.rating = 4.5
        tag.downloadCount = 1234
        tag.posted = Date(timeIntervalSince1970: 1_600_000_000)
        tag.lastRefreshed = Date(timeIntervalSince1970: 1_700_000_000)
        if withSheetMusic {
            let sheet = DPRemoteLocation()
            sheet.uri = URL(string: "https://example.invalid/sheet-\(id).pdf")
            sheet.type = "pdf"
            tag.sheetMusicUri = sheet
        }
        if withTracks {
            for keyPath in [\DPTag.allPartsTrackUri, \DPTag.tenorTrackUri,
                            \DPTag.leadTrackUri, \DPTag.baritoneTrackUri, \DPTag.bassTrackUri] {
                let track = DPRemoteLocation()
                track.uri = URL(string: "https://example.invalid/track-\(id).mp3")
                track.type = "mp3"
                tag[keyPath: keyPath] = track
            }
        }
        tag.cache()
        if !seededTagIds.contains(id) { seededTagIds.append(id) }
        return tag
    }

    /// Removes a seeded tag from both halves of the production cache, so a
    /// fixture never leaks into another test in the same run.
    static func evictCachedTag(_ id: Int32) {
        // `+[DPTag tagCache]` is private; the ObjC suites reach it the same way.
        if let cache = DPTag.perform(Selector(("tagCache")))?.takeUnretainedValue() as? NSMutableDictionary {
            cache.removeObject(forKey: NSNumber(value: id))
        }
        if let path = DPFileCache.path(forKey: "DPTag.\(id)") {
            try? FileManager.default.removeItem(atPath: path)
        }
    }

    /// Drives a detail controller to its loaded state from the seeded cache.
    func loadedDetail(tagId: Int32 = 1809,
                      size: CGSize = TMBehaviorTestCase.portrait) -> DPTagViewController {
        let detail = DPTagViewController()
        detail.tagId = tagId
        mountInNavigation(detail, size: size)
        waitUntil("detail leaves its loading state") { detail.value(forKey: "tag") != nil }
        settle()
        return detail
    }
}

private extension Optional where Wrapped == [UIViewController] {
    var orEmpty: [UIViewController] { self ?? [] }
}

extension XCTestCase {
    /// Depth-first search for a descendant matching a predicate.
    func firstDescendant(of root: UIView, where matches: (UIView) -> Bool) -> UIView? {
        if matches(root) { return root }
        for subview in root.subviews {
            if let found = firstDescendant(of: subview, where: matches) { return found }
        }
        return nil
    }

    func descendants(of root: UIView, where matches: (UIView) -> Bool) -> [UIView] {
        var found: [UIView] = []
        if matches(root) { found.append(root) }
        for subview in root.subviews { found += descendants(of: subview, where: matches) }
        return found
    }

    func label(in root: UIView, text: String) -> UILabel? {
        firstDescendant(of: root) { ($0 as? UILabel)?.text == text } as? UILabel
    }

    func button(in root: UIView, identifier: String) -> UIButton? {
        firstDescendant(of: root) { $0.accessibilityIdentifier == identifier } as? UIButton
    }
}
