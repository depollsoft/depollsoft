//
//  ReviewPromptTests.swift
//  tagmasterTests
//
//  The rules both apps ask for a review under (iOS/shared/ReviewPrompt.swift)
//  and how screens are reported (iOS/shared/UsageAnalytics.swift). They are
//  shared code, tested here once, as PrivacyChoicesTests is.
//

import SwiftUI
import UIKit
import XCTest
@testable import tagmaster

/// Records what would have gone to Google Analytics.
@MainActor
final class AnalyticsRecorder {
    struct Event: Equatable {
        let name: String
        let parameters: [String: String]
    }

    private(set) var events: [Event] = []
    private(set) var properties: [String: String] = [:]

    init() {
        UsageAnalytics.sink = UsageAnalytics.Sink(
            logEvent: { [unowned self] name, parameters in events.append(Event(name: name, parameters: parameters)) },
            setUserProperty: { [unowned self] name, value in properties[name] = value })
    }

    func stop() { UsageAnalytics.sink = .nowhere }

    func named(_ name: String) -> [Event] { events.filter { $0.name == name } }

    var screens: [String] { named("screen_view").compactMap { $0.parameters["screen_name"] } }

    func clear() { events = [] }
}

/// A policy whose clock and storage the test owns.
@MainActor
final class TestReviewClock {
    let suiteName = "ReviewPromptTests.\(UUID().uuidString)"
    let defaults: UserDefaults
    var now = Date(timeIntervalSince1970: 1_800_000_000) // 2027-01-15, a Friday, 08:00 UTC
    var timeZone = TimeZone(identifier: "America/Los_Angeles")!

    init() {
        defaults = UserDefaults(suiteName: suiteName)!
    }

    var policy: ReviewPolicy {
        var policy = ReviewPolicy(defaults: defaults)
        policy.now = { [unowned self] in now }
        policy.timeZone = { [unowned self] in timeZone }
        return policy
    }

    func advance(days: Double) { now = now.addingTimeInterval(days * 86_400) }
    func advance(seconds: TimeInterval) { now = now.addingTimeInterval(seconds) }

    /// Uses the app on `days` different days, one a day, ending today.
    func useOnDays(_ days: Int) {
        for day in 0..<days {
            if day > 0 { advance(days: 1) }
            policy.recordUse()
        }
    }

    /// Makes this person eligible: first use 20 days ago, five days of use.
    func makeEligible() {
        policy.recordUse()
        advance(days: 16)
        useOnDays(4)
    }

    func tearDown() { defaults.removePersistentDomain(forName: suiteName) }
}

@MainActor
final class ReviewPolicyTests: XCTestCase {
    private var clock: TestReviewClock!

    override func setUp() async throws {
        try await super.setUp()
        clock = TestReviewClock()
    }

    override func tearDown() async throws {
        clock.tearDown()
        clock = nil
        try await super.tearDown()
    }

    func testANewInstallationIsNeverAsked() {
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.0"))
    }

    func testNeedsFiveDaysOfUseAcrossTwoWeeks() {
        clock.useOnDays(5)
        XCTAssertEqual(clock.policy.activeDays, 5)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.0"), "five days in a row, but only four days since the first")
        clock.advance(days: 9)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.0"), "13 days since the first use")
        clock.advance(days: 1)
        XCTAssertTrue(clock.policy.shouldAsk(version: "1.0"), "14 days since the first use")
    }

    func testFourDaysOfUseAreNotEnoughHoweverLongAgo() {
        clock.useOnDays(4)
        clock.advance(days: 60)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.0"))
        clock.policy.recordUse()
        XCTAssertTrue(clock.policy.shouldAsk(version: "1.0"))
    }

    func testUsesOnOneDayCountOnce() {
        for _ in 0..<20 {
            clock.policy.recordUse()
            clock.advance(seconds: 60)
        }
        XCTAssertEqual(clock.policy.activeDays, 1)
    }

    func testADayEndsAtLocalMidnight() {
        // 23:59 and 00:01 in Los Angeles are two days, though both are 07:59/08:01 UTC.
        clock.now = ISO8601DateFormatter().date(from: "2027-01-15T07:59:00Z")!
        clock.policy.recordUse()
        clock.advance(seconds: 120)
        clock.policy.recordUse()
        XCTAssertEqual(clock.policy.activeDays, 2)
        // The same two minutes in UTC are one day.
        let utc = TestReviewClock()
        defer { utc.tearDown() }
        utc.timeZone = TimeZone(identifier: "UTC")!
        utc.now = ISO8601DateFormatter().date(from: "2027-01-15T07:59:00Z")!
        utc.policy.recordUse()
        utc.advance(seconds: 120)
        utc.policy.recordUse()
        XCTAssertEqual(utc.policy.activeDays, 1)
    }

    func testADayRevisitedAfterTheClockGoesBackIsNotCountedAgain() {
        clock.policy.recordUse()
        clock.advance(days: 1)
        clock.policy.recordUse()
        clock.advance(days: -1)
        clock.policy.recordUse()
        XCTAssertEqual(clock.policy.activeDays, 2)
        clock.advance(days: 1)
        clock.policy.recordUse()
        XCTAssertEqual(clock.policy.activeDays, 2, "the latest day was counted already")
        clock.advance(days: 1)
        clock.policy.recordUse()
        XCTAssertEqual(clock.policy.activeDays, 3)
    }

    func testAnAskedVersionIsNotAskedAgain() {
        clock.makeEligible()
        XCTAssertTrue(clock.policy.shouldAsk(version: "1.0"))
        clock.policy.recordAsk(version: "1.0")
        clock.advance(days: 400)
        clock.useOnDays(5)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.0"), "never twice in one version")
        XCTAssertTrue(clock.policy.shouldAsk(version: "1.1"))
    }

    func testANewVersionWaitsHalfAYearAndFreshDaysOfUse() {
        clock.makeEligible()
        clock.policy.recordAsk(version: "1.0")
        XCTAssertEqual(clock.policy.activeDays, 0, "the days of use start over")
        clock.useOnDays(5) // the ask's day and the four after it
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.1"), "four days since the ask")
        clock.advance(days: 175)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.1"), "179 days since the ask")
        clock.advance(days: 1)
        XCTAssertTrue(clock.policy.shouldAsk(version: "1.1"), "180 days since the ask")
    }

    func testTheDaysOfUseAfterAnAskMustBeFresh() {
        clock.makeEligible()
        clock.policy.recordAsk(version: "1.0")
        clock.advance(days: 200)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.1"))
        clock.useOnDays(4)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.1"))
        clock.advance(days: 1)
        clock.policy.recordUse()
        XCTAssertTrue(clock.policy.shouldAsk(version: "1.1"))
    }

    func testNothingIsAskedWithinFiveMinutesOfASound() {
        clock.makeEligible()
        clock.policy.recordSound()
        clock.advance(seconds: 4 * 60 + 59)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.0"))
        clock.advance(seconds: 1)
        XCTAssertTrue(clock.policy.shouldAsk(version: "1.0"))
    }

    func testASoundAheadOfAClockSetBackStaysRecentUntilTheNextSound() {
        clock.makeEligible()
        clock.policy.recordSound()
        clock.advance(seconds: -3600)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.0"), "a sound ahead of the clock is recent")
        clock.policy.recordSound()
        clock.advance(seconds: ReviewPolicy.quietSecondsAfterSound)
        XCTAssertTrue(clock.policy.shouldAsk(version: "1.0"), "the next sound records the time afresh")
    }
}

@MainActor
final class ReviewPromptTests: XCTestCase {
    private var clock: TestReviewClock!
    private var prompt: ReviewPrompt!
    private var analytics: AnalyticsRecorder!
    private var window: UIWindow!
    private var uptime: TimeInterval = 1000
    private var pending: [@MainActor () -> Void] = []
    private var asked: [UIWindow] = []
    private var busy = false

    override func setUp() async throws {
        try await super.setUp()
        clock = TestReviewClock()
        clock.makeEligible()
        analytics = AnalyticsRecorder()
        window = ScreenCatalog.makeWindow()
        window.rootViewController = UIViewController()
        window.makeKeyAndVisible()
        prompt = ReviewPrompt()
        prompt.install(clock.policy)
        prompt.activeWindow = { [unowned self] in window }
        prompt.version = { "1.0" }
        prompt.uptime = { [unowned self] in uptime }
        prompt.after = { [unowned self] _, work in pending.append(work) }
        prompt.presentStoreReview = { [unowned self] in asked.append($0) }
        prompt.isBusy = { [unowned self] in busy }
    }

    override func tearDown() async throws {
        prompt.reset()
        window.rootViewController?.dismiss(animated: false)
        window.isHidden = true
        window = nil
        analytics.stop()
        clock.tearDown()
        prompt = nil
        try await super.tearDown()
    }

    /// Lets the three calm seconds pass.
    private func finishWait() {
        let work = pending
        pending = []
        work.forEach { $0() }
    }

    /// Runs the run loop until `condition` holds (UIKit finishing a presentation).
    private func spin(file: StaticString = #filePath, line: UInt = #line, until condition: () -> Bool) {
        let deadline = Date(timeIntervalSinceNow: 10)
        while !condition(), Date() < deadline {
            RunLoop.main.run(until: Date(timeIntervalSinceNow: 0.01))
        }
        XCTAssertTrue(condition(), "Timed out waiting", file: file, line: line)
    }

    private let screen = UUID()

    func testAsksAfterAFinishedTaskOnACalmScreen() {
        prompt.calmScreenShown(screen)
        XCTAssertFalse(prompt.isWaiting, "no task, no ask")
        prompt.taskFinished()
        XCTAssertTrue(prompt.isWaiting)
        finishWait()
        XCTAssertEqual(asked, [window])
        XCTAssertEqual(analytics.named("review_prompt_requested").count, 1)
        XCTAssertFalse(clock.policy.shouldAsk(version: "1.0"), "the ask is recorded")
    }

    func testATaskFinishedElsewhereWaitsForTheCalmScreen() {
        prompt.taskFinished()
        XCTAssertFalse(prompt.isWaiting)
        uptime += 30
        prompt.calmScreenShown(screen)
        XCTAssertTrue(prompt.isWaiting)
        finishWait()
        XCTAssertEqual(asked.count, 1)
    }

    func testATaskOlderThanTwoMinutesIsForgotten() {
        prompt.taskFinished()
        uptime += 121
        prompt.calmScreenShown(screen)
        XCTAssertFalse(prompt.isWaiting)
        XCTAssertTrue(asked.isEmpty)
    }

    func testATouchDuringTheWaitSpendsTheChance() {
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        prompt.interactionForTesting()
        finishWait()
        XCTAssertTrue(asked.isEmpty)
        prompt.calmScreenHidden(screen)
        prompt.calmScreenShown(screen)
        XCTAssertFalse(prompt.isWaiting, "the task's one chance is gone")
        prompt.taskFinished()
        XCTAssertTrue(prompt.isWaiting, "the next task brings a new chance")
    }

    func testLeavingTheCalmScreenCancelsTheWait() {
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        prompt.calmScreenHidden(screen)
        XCTAssertFalse(prompt.isWaiting)
        finishWait()
        XCTAssertTrue(asked.isEmpty)
        prompt.calmScreenShown(screen)
        XCTAssertTrue(prompt.isWaiting, "an untouched task survives a screen going by")
    }

    func testTheThreeSecondsStartOnceNothingCoversTheScreen() {
        // The task's sheet is still up when the task finishes.
        let root = window.rootViewController
        root?.present(UIViewController(), animated: false)
        spin { root?.presentedViewController != nil && root?.transitionCoordinator == nil }
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        XCTAssertFalse(prompt.isWaiting, "nothing counts while the sheet covers the screen")
        root?.dismiss(animated: false)
        spin { root?.presentedViewController == nil && root?.transitionCoordinator == nil }
        finishWait()
        XCTAssertTrue(prompt.isWaiting, "the next look finds the screen clear and starts the wait")
        finishWait()
        XCTAssertEqual(asked, [window])
    }

    func testNothingAskedOverASheet() {
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        window.rootViewController?.present(UIViewController(), animated: false)
        finishWait()
        XCTAssertTrue(asked.isEmpty)
    }

    func testNothingAskedWhileSounding() {
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        busy = true
        finishWait()
        XCTAssertTrue(asked.isEmpty)
    }

    func testNothingAskedWithTheKeyboardUp() {
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        NotificationCenter.default.post(name: UIResponder.keyboardWillShowNotification, object: nil)
        finishWait()
        XCTAssertTrue(asked.isEmpty)
        NotificationCenter.default.post(name: UIResponder.keyboardWillHideNotification, object: nil)
        XCTAssertFalse(prompt.keyboardShown)
    }

    func testNothingAskedWhileATextFieldIsEditing() {
        let field = UITextField(frame: CGRect(x: 0, y: 0, width: 200, height: 44))
        // An empty input view keeps the software keyboard (and its stalls) out of the test.
        field.inputView = UIView(frame: .zero)
        window.rootViewController?.view.addSubview(field)
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        field.becomeFirstResponder()
        defer { field.resignFirstResponder() }
        finishWait()
        XCTAssertTrue(asked.isEmpty)
    }

    func testLeavingTheAppSpendsTheChance() {
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        NotificationCenter.default.post(name: UIApplication.willResignActiveNotification, object: nil)
        XCTAssertFalse(prompt.isWaiting)
        prompt.calmScreenHidden(screen)
        prompt.calmScreenShown(screen)
        XCTAssertFalse(prompt.isWaiting, "coming back is like a launch")
    }

    func testSomeoneWhoMayNotBeAskedNeverWaits() {
        let fresh = TestReviewClock()
        defer { fresh.tearDown() }
        prompt.install(fresh.policy)
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        XCTAssertFalse(prompt.isWaiting)
    }

    func testAPromptTheAppNeverInstalledCountsNothingAndNeverAsks() {
        let bare = ReviewPrompt()
        bare.activeWindow = { [unowned self] in window }
        bare.after = { [unowned self] _, work in pending.append(work) }
        bare.presentStoreReview = { [unowned self] in asked.append($0) }
        bare.recordUse()
        bare.recordSound()
        bare.calmScreenShown(screen)
        bare.taskFinished()
        XCTAssertFalse(bare.isWaiting)
        XCTAssertNil(bare.policy)
    }

    func testOnlyOneAskPerVersion() {
        prompt.calmScreenShown(screen)
        prompt.taskFinished()
        finishWait()
        prompt.taskFinished()
        XCTAssertFalse(prompt.isWaiting)
        XCTAssertEqual(asked.count, 1)
    }

    func testTheStoreIsNeverAskedFromATestRun() {
        // Debug builds (every test run) only log; the real call is release-only.
        ReviewPrompt.requestStoreReview(in: window)
        XCTAssertNil(window.rootViewController?.presentedViewController)
    }

    func testTheCalmModifierRegistersWhileShownAndCalm() {
        let previous = ReviewPrompt.shared
        ReviewPrompt.shared = prompt
        defer { ReviewPrompt.shared = previous }
        let calm = CalmFlag()
        window.rootViewController = UIHostingController(rootView: CalmHost(flag: calm))
        ScreenCatalog.settle(0.2)
        prompt.taskFinished()
        XCTAssertTrue(prompt.isWaiting, "a calm screen on screen")
        calm.isCalm = false
        ScreenCatalog.settle(0.2)
        XCTAssertFalse(prompt.isWaiting, "no longer calm")
    }
}

@Observable @MainActor
private final class CalmFlag {
    var isCalm = true
}

private struct CalmHost: View {
    let flag: CalmFlag
    var body: some View {
        Color.clear.reviewCalmScreen(flag.isCalm)
    }
}

@MainActor
final class ScreenTrackerTests: XCTestCase {
    private var analytics: AnalyticsRecorder!
    private var tracker: ScreenTracker!
    private var pending: [@MainActor () -> Void] = []

    override func setUp() async throws {
        try await super.setUp()
        analytics = AnalyticsRecorder()
        tracker = ScreenTracker()
        tracker.settle = { [unowned self] work in pending.append(work) }
    }

    override func tearDown() async throws {
        analytics.stop()
        try await super.tearDown()
    }

    private func settle() {
        let work = pending
        pending = []
        work.forEach { $0() }
    }

    func testAScreenViewCarriesItsNameAsNameAndClass() {
        let id = UUID()
        tracker.appeared(id, "home")
        settle()
        XCTAssertEqual(analytics.events, [.init(name: "screen_view", parameters: ["screen_name": "home", "screen_class": "home"])])
    }

    func testAPushReportsTheNewScreenOnce() {
        let home = UUID(), browse = UUID()
        tracker.appeared(home, "home")
        settle()
        // SwiftUI may report the covered screen gone before or after the new one appears.
        tracker.disappeared(home)
        tracker.appeared(browse, "browse")
        settle()
        tracker.appeared(home, "home")
        tracker.disappeared(browse)
        settle()
        XCTAssertEqual(analytics.screens, ["home", "browse", "home"])
    }

    func testClosingASheetReportsTheScreenUnderIt() {
        let songs = UUID(), editor = UUID()
        tracker.appeared(songs, "songs")
        settle()
        tracker.appeared(editor, "song_editor")
        settle()
        tracker.disappeared(editor)
        settle()
        XCTAssertEqual(analytics.screens, ["songs", "song_editor", "songs"])
    }

    func testTheSameScreenAppearingAgainIsNotASecondVisit() {
        let home = UUID()
        tracker.appeared(home, "home")
        settle()
        tracker.appeared(home, "home")
        settle()
        XCTAssertEqual(analytics.screens, ["home"])
    }

    func testAgainReportsTheSameNameOnceMore() {
        let tag = UUID()
        tracker.appeared(tag, "tag_summary")
        settle()
        tracker.appeared(tag, "tag_summary", again: true)
        settle()
        XCTAssertEqual(analytics.screens, ["tag_summary", "tag_summary"])
    }

    func testComingBackFromTheBackgroundReportsTheScreenAgain() {
        tracker.appeared(UUID(), "pitch_pipe")
        settle()
        tracker.scenePhaseChanged(.inactive)
        tracker.scenePhaseChanged(.active)
        settle()
        XCTAssertEqual(analytics.screens, ["pitch_pipe"], "an inactive spell is not leaving")
        tracker.scenePhaseChanged(.inactive)
        tracker.scenePhaseChanged(.background)
        tracker.scenePhaseChanged(.inactive)
        tracker.scenePhaseChanged(.active)
        settle()
        XCTAssertEqual(analytics.screens, ["pitch_pipe", "pitch_pipe"])
    }

    func testNoScreenNoReport() {
        let id = UUID()
        tracker.appeared(id, "home")
        tracker.disappeared(id)
        settle()
        XCTAssertTrue(analytics.events.isEmpty)
    }
}

@MainActor
final class UsageAnalyticsTests: XCTestCase {
    func testSignInMethodsAreNamedAsOnAndroid() {
        XCTAssertEqual(UsageAnalytics.signInMethod("google.com"), "google")
        XCTAssertEqual(UsageAnalytics.signInMethod("apple.com"), "apple")
        XCTAssertEqual(UsageAnalytics.signInMethod("facebook.com"), "facebook")
        XCTAssertEqual(UsageAnalytics.signInMethod("password"), "email")
        XCTAssertEqual(UsageAnalytics.signInMethod("emailLink"), "email")
        XCTAssertEqual(UsageAnalytics.signInMethod("phone"), "phone")
        XCTAssertEqual(UsageAnalytics.signInMethod("github.com"), "other")
        XCTAssertEqual(UsageAnalytics.signInMethod(nil), "other")
    }

    func testLoginAndSignedInUseGoogleAnalyticsNames() {
        let analytics = AnalyticsRecorder()
        defer { analytics.stop() }
        UsageAnalytics.login(providerId: "apple.com")
        UsageAnalytics.signedIn(true)
        XCTAssertEqual(analytics.events, [.init(name: "login", parameters: ["method": "apple"])])
        XCTAssertEqual(analytics.properties, ["signed_in": "yes"])
    }
}
