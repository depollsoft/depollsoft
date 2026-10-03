//
//  UsageAnalytics.swift
//  Pitch Perfect and Tag Master
//
//  The screens and feature use both apps report to Google Analytics, under the
//  names docs/analytics.md lists; Android reports the same names
//  (depollsoft.lib.analytics.UsageAnalytics).
//

import FirebaseAnalytics
import FirebaseAuth
import SwiftUI

/// Each app points `sink` at Firebase Analytics at launch (`connectToFirebase()`),
/// which collects nothing unless the person allowed usage analytics in Privacy
/// choices. Values are short fixed words, never anything someone typed or named.
@MainActor
enum UsageAnalytics {
    struct Sink {
        var logEvent: (_ name: String, _ parameters: [String: String]) -> Void
        var setUserProperty: (_ name: String, _ value: String) -> Void

        static let nowhere = Sink(logEvent: { _, _ in }, setUserProperty: { _, _ in })

        /// Firebase, while the person allows usage analytics (Firebase's own
        /// collection switch follows the same choice).
        static let firebase = Sink(
            logEvent: { name, parameters in
                guard PrivacyChoices().analytics else { return }
                FirebaseAnalytics.Analytics.logEvent(name, parameters: parameters)
            },
            setUserProperty: { name, value in
                guard PrivacyChoices().analytics else { return }
                FirebaseAnalytics.Analytics.setUserProperty(value, forName: name)
            })
    }

    /// Where events go: nowhere until the app connects it, and a recorder in tests.
    static var sink = Sink.nowhere

    static func connectToFirebase() { sink = .firebase }

    /// Reports that `name` is now the screen in front. Screens report through
    /// `.analyticsScreen(_:)`, which decides when that is.
    static func screen(_ name: String) {
        sink.logEvent(AnalyticsEventScreenView, [AnalyticsParameterScreenName: name, AnalyticsParameterScreenClass: name])
    }

    static func event(_ name: String, _ parameters: [String: String] = [:]) {
        sink.logEvent(name, parameters)
    }

    static func userProperty(_ name: String, _ value: String) {
        sink.setUserProperty(name, value)
    }

    /// Sign-in succeeded with the Firebase provider `providerId` (`google.com`, `password`, ...).
    static func login(providerId: String?) {
        event(AnalyticsEventLogin, [AnalyticsParameterMethod: signInMethod(providerId)])
    }

    /// Reports how `user` just signed in: the provider its new ID token names.
    static func login(_ user: User?) {
        guard let user else { return }
        user.getIDTokenResult { result, _ in
            MainActor.assumeIsolated { login(providerId: result?.signInProvider) }
        }
    }

    static func signedIn(_ signedIn: Bool) {
        userProperty(signedInProperty, signedIn ? "yes" : "no")
    }

    nonisolated static func signInMethod(_ providerId: String?) -> String {
        switch providerId {
        case "google.com": "google"
        case "apple.com": "apple"
        case "facebook.com": "facebook"
        case "password", "emailLink": "email"
        case "phone": "phone"
        default: "other"
        }
    }

    // Names both apps use.
    static let pitchPlayed = "pitch_played"
    static let source = "source"
    static let signedInProperty = "signed_in"
}

// MARK: - Screens

/// Decides which screen is in front, from the screens' own appearances, and
/// reports it each time that changes: a screen opening, its tab or page being
/// chosen, returning to it from a screen or sheet that covered it, or the app
/// coming back from the background.
///
/// SwiftUI reports a screen covered by a pushed screen or a tab switch as gone
/// (onDisappear), but one under a sheet as still there; the sheet's own screen
/// appears on top of it, and when the sheet goes the one under it is in front
/// again. Changes are settled on the next turn of the run loop, so a pair of
/// appear and disappear calls for one navigation reports once.
@MainActor
final class ScreenTracker {
    static var shared = ScreenTracker()

    private var shown: [(id: UUID, name: String)] = []
    private var lastReported: String?
    private var settling = false
    private var wentToBackground = false

    /// Runs the settling step; tests run it at once.
    var settle: (@escaping @MainActor () -> Void) -> Void = { work in
        DispatchQueue.main.async { MainActor.assumeIsolated(work) }
    }

    var front: String? { shown.last?.name }

    /// The screen `id` came to the front showing `name`: it appeared, or a page
    /// of it was chosen. `again` reports it even under the name last reported
    /// (another tag in a reused detail).
    func appeared(_ id: UUID, _ name: String, again: Bool = false) {
        shown.removeAll { $0.id == id }
        shown.append((id, name))
        if again { lastReported = nil }
        scheduleReport()
    }

    /// A tab bar's chosen tab is `name`. The tab sits under every screen shown
    /// from it (a pushed screen, a sheet), so it is renamed in place: a screen
    /// over it stays in front.
    func tabChosen(_ name: String) {
        if let index = shown.firstIndex(where: { $0.id == Self.tabSlot }) {
            shown[index].name = name
        } else {
            shown.insert((Self.tabSlot, name), at: 0)
        }
        scheduleReport()
    }

    private static let tabSlot = UUID()

    func disappeared(_ id: UUID) {
        shown.removeAll { $0.id == id }
        scheduleReport()
    }

    /// The scene's phase changed; coming back from the background reports the
    /// screen in front again (an inactive spell, such as Control Center, does not).
    func scenePhaseChanged(_ phase: ScenePhase) {
        switch phase {
        case .background:
            wentToBackground = true
        case .active where wentToBackground:
            wentToBackground = false
            lastReported = nil
            scheduleReport()
        default:
            break
        }
    }

    private func scheduleReport() {
        guard !settling else { return }
        settling = true
        settle { [weak self] in
            guard let self else { return }
            self.settling = false
            guard let front = self.front, front != self.lastReported else { return }
            self.lastReported = front
            UsageAnalytics.screen(front)
        }
    }
}

private struct AnalyticsScreen: ViewModifier {
    let name: String?
    let subject: AnyHashable?
    @State private var id = UUID()
    @State private var visible = false

    func body(content: Content) -> some View {
        content
            .onAppear {
                visible = true
                if let name { ScreenTracker.shared.appeared(id, name) }
            }
            .onDisappear {
                visible = false
                ScreenTracker.shared.disappeared(id)
            }
            .onChange(of: name) { _, name in
                guard visible else { return }
                if let name {
                    ScreenTracker.shared.appeared(id, name)
                } else {
                    ScreenTracker.shared.disappeared(id)
                }
            }
            .onChange(of: subject) { _, _ in
                guard visible, let name else { return }
                ScreenTracker.shared.appeared(id, name, again: true)
            }
    }
}

extension View {
    /// Reports this view as the screen `name` while it is in front (see
    /// ScreenTracker); nil while it is not a screen yet. A change of `subject`
    /// (another tag in the same detail) counts as coming to the front again.
    func analyticsScreen(_ name: String?, subject: AnyHashable? = nil) -> some View {
        modifier(AnalyticsScreen(name: name, subject: subject))
    }
}
