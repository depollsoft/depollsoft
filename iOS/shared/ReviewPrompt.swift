//
//  ReviewPrompt.swift
//  Pitch Perfect and Tag Master
//
//  When the apps ask the App Store for a review, and when they never do.
//  Android applies the same rules (depollsoft.lib.review.ReviewPolicy and
//  ReviewPrompt); docs/analytics.md describes both.
//

import StoreKit
import SwiftUI
import UIKit

/// Whether this person may be asked for a review at all.
///
/// The counts stay on the device and are never sent anywhere. An app records
/// `recordUse()` each time someone does its main job (plays a pitch, opens a tag)
/// and `recordSound()` each time it makes a sound for them (a pitch, a learning
/// track). Asking is deliberately rare: someone has to have used the app on
/// `minActiveDays` different days across at least `minDaysSinceFirstUse` days,
/// and after an ask the app waits `minDaysBetweenAsks` days and a new version,
/// and the active days start over. Nothing is asked within
/// `quietSecondsAfterSound` of a sound, because someone may be rehearsing or
/// singing with others. The store may still decide not to show anything.
struct ReviewPolicy {
    static let minActiveDays = 5
    static let minDaysSinceFirstUse = 14
    static let minDaysBetweenAsks = 180
    static let quietSecondsAfterSound: TimeInterval = 5 * 60

    private static let firstUseDayKey = "review.firstUseDay"
    private static let lastActiveDayKey = "review.lastActiveDay"
    private static let activeDaysKey = "review.activeDays"
    private static let lastAskDayKey = "review.lastAskDay"
    private static let lastAskVersionKey = "review.lastAskVersion"
    private static let lastSoundAtKey = "review.lastSoundAt"

    let defaults: UserDefaults
    var now: () -> Date = Date.init
    var timeZone: () -> TimeZone = { TimeZone.current }

    /// Today's date in the device's time zone, as days since 1970-01-01.
    private func today() -> Int {
        let date = now()
        let local = date.timeIntervalSince1970 + Double(timeZone().secondsFromGMT(for: date))
        return Int((local / 86_400).rounded(.down))
    }

    private func integer(_ key: String) -> Int? {
        (defaults.object(forKey: key) as? NSNumber)?.intValue
    }

    var activeDays: Int { defaults.integer(forKey: Self.activeDaysKey) }

    func recordUse() {
        let today = today()
        if integer(Self.firstUseDayKey) == nil { defaults.set(today, forKey: Self.firstUseDayKey) }
        if integer(Self.lastActiveDayKey) != today {
            defaults.set(today, forKey: Self.lastActiveDayKey)
            defaults.set(activeDays + 1, forKey: Self.activeDaysKey)
        }
    }

    func recordSound() {
        defaults.set(now().timeIntervalSince1970, forKey: Self.lastSoundAtKey)
    }

    func shouldAsk(version: String) -> Bool {
        let today = today()
        guard let firstUse = integer(Self.firstUseDayKey), today - firstUse >= Self.minDaysSinceFirstUse else { return false }
        guard activeDays >= Self.minActiveDays else { return false }
        guard defaults.string(forKey: Self.lastAskVersionKey) != version else { return false }
        if let lastAsk = integer(Self.lastAskDayKey), today - lastAsk < Self.minDaysBetweenAsks { return false }
        guard let lastSound = (defaults.object(forKey: Self.lastSoundAtKey) as? NSNumber)?.doubleValue else { return true }
        // A clock set back leaves the sound ahead of now: recent until the clock catches up, or the
        // next sound records the time afresh.
        return now().timeIntervalSince1970 - lastSound >= Self.quietSecondsAfterSound
    }

    /// Records an ask; the next one needs a new version, the wait, and fresh active days.
    func recordAsk(version: String) {
        defaults.set(today(), forKey: Self.lastAskDayKey)
        defaults.set(version, forKey: Self.lastAskVersionKey)
        defaults.set(0, forKey: Self.activeDaysKey)
        defaults.removeObject(forKey: Self.lastActiveDayKey)
    }
}

/// Asks the App Store for a review, rarely, and never while someone is using the app.
///
/// `ReviewPolicy` decides whether this person may be asked at all. Beyond that,
/// an ask needs all of:
///
/// - a task away from the music that someone just finished (`taskFinished()`:
///   saving a new song, putting a tag on a list), within `taskWindow`;
/// - a calm screen in front (`.reviewCalmScreen(_:)`): one that nobody reads or
///   plays from while singing, such as Pitch Perfect's Songs tab or Tag Master's
///   Home, never a pitch pipe, a tag or sheet music;
/// - `calmSeconds` with no touch, press or pointer movement, after which the app
///   is still active, nothing is presented over the window, no keyboard or text
///   field is in use and nothing is sounding (`isBusy`).
///
/// Each finished task gives one chance: a touch during the wait, or the app
/// going inactive, spends it. Nothing is asked at launch.
@MainActor
final class ReviewPrompt {
    static var shared = ReviewPrompt()

    /// How long the screen has to stay untouched before asking.
    static let calmSeconds: TimeInterval = 3
    /// How often a covered calm screen is looked at again, to start the wait once it's clear.
    static let calmCheckInterval: TimeInterval = 0.25
    /// How long a finished task waits for a calm screen.
    static let taskWindow: TimeInterval = 2 * 60
    /// Reported to analytics each time the app asks the store (which may still show nothing).
    static let askedEvent = "review_prompt_requested"

    /// Whether the app is sounding or otherwise in live use; each app sets it.
    var isBusy: () -> Bool = { false }
    /// Asks the store; tests replace it. Debug builds and test runs never ask.
    var presentStoreReview: (UIWindow) -> Void = ReviewPrompt.requestStoreReview
    /// The window an ask would cover: the foreground-active scene's key window.
    var activeWindow: () -> UIWindow? = ReviewPrompt.foregroundKeyWindow
    /// The version an ask is recorded against.
    var version: () -> String? = { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String }
    /// The clock the task window is measured on; tests replace it.
    var uptime: () -> TimeInterval = { ProcessInfo.processInfo.systemUptime }
    /// Runs the end of the wait after `calmSeconds`; tests run it when they choose.
    var after: (TimeInterval, @escaping @MainActor () -> Void) -> Void = { delay, work in
        DispatchQueue.main.asyncAfter(deadline: .now() + delay) { MainActor.assumeIsolated(work) }
    }

    /// The policy, once the app has called `install`; tests and previews have none and never ask.
    private(set) var policy: ReviewPolicy?
    private var calmScreen: UUID?
    private var taskFinishedAt: TimeInterval?
    private var waiting: Waiting?
    private var generation = 0
    private(set) var keyboardShown = false
    private var observers: [NSObjectProtocol] = []

    init() {
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.keyboardShown = true }
        })
        observers.append(center.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.keyboardShown = false }
        })
        // Leaving the app spends the chance: coming back is like a launch.
        observers.append(center.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated {
                self?.cancelWaiting()
                self?.taskFinishedAt = nil
            }
        })
    }

    isolated deinit {
        observers.forEach(NotificationCenter.default.removeObserver)
    }

    /// Starts keeping the counts in `defaults`; each app calls it at launch.
    func install(defaults: UserDefaults = .standard) {
        policy = ReviewPolicy(defaults: defaults)
    }

    /// Replaces the stored policy (and its clock); for tests.
    func install(_ policy: ReviewPolicy) {
        self.policy = policy
    }

    /// Someone did the app's main job (played a pitch, opened a tag).
    func recordUse() { policy?.recordUse() }

    /// The app made a sound for someone (a pitch, a learning track).
    func recordSound() { policy?.recordSound() }

    /// Someone just finished a task away from the music.
    func taskFinished() {
        taskFinishedAt = uptime()
        if calmScreen != nil { startWaiting() }
    }

    /// The calm screen `id` is now in front.
    func calmScreenShown(_ id: UUID) {
        calmScreen = id
        startWaiting()
    }

    /// The calm screen `id` is no longer in front.
    func calmScreenHidden(_ id: UUID) {
        guard calmScreen == id else { return }
        calmScreen = nil
        cancelWaiting()
    }

    private var taskIsFresh: Bool {
        guard let finishedAt = taskFinishedAt else { return false }
        let elapsed = uptime() - finishedAt
        return elapsed >= 0 && elapsed <= Self.taskWindow
    }

    private func startWaiting() {
        cancelWaiting()
        guard taskIsFresh, let policy, let version = version(), policy.shouldAsk(version: version),
              let window = activeWindow() else { return }
        generation += 1
        let token = generation
        guard isCalm(window) else {
            // The three untouched seconds start once nothing covers the screen: the task's
            // sheet still closing, the keyboard, a sound.
            after(Self.calmCheckInterval) { [weak self] in
                guard let self, token == self.generation, self.calmScreen != nil else { return }
                self.startWaiting()
            }
            return
        }
        let next = Waiting(window: window, version: version, screen: calmScreen, watcher: InteractionWatcher(window))
        waiting = next
        after(Self.calmSeconds) { [weak self] in self?.finishWaiting(token) }
    }

    private func cancelWaiting() {
        // Also voids a pending look at a covered calm screen.
        generation += 1
        guard let current = waiting else { return }
        waiting = nil
        // A touch while the screen was going away still spends the chance.
        if current.watcher.stop() { taskFinishedAt = nil }
    }

    private func finishWaiting(_ token: Int) {
        guard token == generation, let done = waiting else { return }
        waiting = nil
        if done.watcher.stop() {
            taskFinishedAt = nil
            return
        }
        guard let window = done.window, calmScreen == done.screen, taskIsFresh, isCalm(window) else { return }
        guard let policy, policy.shouldAsk(version: done.version) else { return }
        taskFinishedAt = nil
        policy.recordAsk(version: done.version)
        UsageAnalytics.event(Self.askedEvent)
        presentStoreReview(window)
    }

    /// Whether `window` is still the active one with nothing over it, no
    /// keyboard or text field is in use, and the app is quiet.
    func isCalm(_ window: UIWindow) -> Bool {
        guard activeWindow() === window, let root = window.rootViewController else { return false }
        if root.presentedViewController != nil || root.transitionCoordinator != nil
            || root.isBeingPresented || root.isBeingDismissed { return false }
        if keyboardShown || UIResponder.currentFirstResponder is UITextInput { return false }
        return !isBusy()
    }

    /// For tests: someone touched the screen during the wait.
    func interactionForTesting() { waiting?.watcher.simulateTouch() }

    /// Whether an ask is waiting out `calmSeconds`; for tests.
    var isWaiting: Bool { waiting != nil }

    /// Whether a finished task is still waiting for its chance; for tests.
    var hasFreshTaskForTesting: Bool { taskIsFresh }

    /// Whether a calm screen is in front; for tests.
    var hasCalmScreenForTesting: Bool { calmScreen != nil }

    /// Forgets the wait, the task and the calm screen; for tests.
    func reset() {
        cancelWaiting()
        calmScreen = nil
        taskFinishedAt = nil
        keyboardShown = false
    }

    // MARK: Defaults

    static func foregroundKeyWindow() -> UIWindow? {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }?.keyWindow
    }

    static func requestStoreReview(in window: UIWindow) {
#if DEBUG
        print("ReviewPrompt: a release build would ask the App Store for a review now")
#else
        guard NSClassFromString("XCTestCase") == nil, let scene = window.windowScene else { return }
        AppStore.requestReview(in: scene)
#endif
    }

    private struct Waiting {
        weak var window: UIWindow?
        let version: String
        let screen: UUID?
        let watcher: InteractionWatcher
    }
}

/// Notes any touch, press or pointer movement in a window until `stop()`, without
/// taking part in it: it never recognizes, cancels or delays anything.
@MainActor
fileprivate final class InteractionWatcher: NSObject {
    private final class Recognizer: UIGestureRecognizer, UIGestureRecognizerDelegate {
        var interacted = false

        init() {
            super.init(target: nil, action: nil)
            cancelsTouchesInView = false
            delaysTouchesBegan = false
            delaysTouchesEnded = false
            delegate = self
        }

        override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent) {
            interacted = true
            state = .failed
        }

        override func pressesBegan(_ presses: Set<UIPress>, with event: UIPressesEvent) {
            interacted = true
            state = .failed
        }

        func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer,
                               shouldRecognizeSimultaneouslyWith other: UIGestureRecognizer) -> Bool { true }
    }

    private weak var window: UIWindow?
    private let recognizer = Recognizer()
    private let hover: UIHoverGestureRecognizer
    private var hovered = false

    init(_ window: UIWindow) {
        self.window = window
        hover = UIHoverGestureRecognizer()
        super.init()
        hover.cancelsTouchesInView = false
        hover.addTarget(self, action: #selector(pointerMoved))
        window.addGestureRecognizer(recognizer)
        window.addGestureRecognizer(hover)
    }

    @objc private func pointerMoved() { hovered = true }

    /// Takes the recognizers off the window and says whether anything happened.
    func stop() -> Bool {
        window?.removeGestureRecognizer(recognizer)
        window?.removeGestureRecognizer(hover)
        return recognizer.interacted || hovered
    }

    /// For tests: a touch landed.
    func simulateTouch() { recognizer.interacted = true }
}

extension UIResponder {
    private static weak var foundFirstResponder: UIResponder?

    /// The window's first responder, found by sending an action to no one in particular.
    static var currentFirstResponder: UIResponder? {
        foundFirstResponder = nil
        UIApplication.shared.sendAction(#selector(reviewPromptCaptureFirstResponder), to: nil, from: nil, for: nil)
        return foundFirstResponder
    }

    @objc private func reviewPromptCaptureFirstResponder() {
        UIResponder.foundFirstResponder = self
    }
}

private struct ReviewCalmScreen: ViewModifier {
    let isCalm: Bool
    @State private var id = UUID()
    @State private var visible = false

    func body(content: Content) -> some View {
        content
            .onAppear {
                visible = true
                update()
            }
            .onDisappear {
                visible = false
                update()
            }
            .onChange(of: isCalm) { _, _ in update() }
    }

    private func update() {
        if visible && isCalm {
            ReviewPrompt.shared.calmScreenShown(id)
        } else {
            ReviewPrompt.shared.calmScreenHidden(id)
        }
    }
}

extension View {
    /// Marks this screen as calm while it is on screen and `isCalm` holds: one
    /// nobody reads or plays from while singing, where a review may be asked for
    /// after a finished task (see ReviewPrompt).
    func reviewCalmScreen(_ isCalm: Bool = true) -> some View {
        modifier(ReviewCalmScreen(isCalm: isCalm))
    }
}
