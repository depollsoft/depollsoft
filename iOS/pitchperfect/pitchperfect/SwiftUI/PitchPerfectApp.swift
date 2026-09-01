import SwiftUI

@main
struct PitchPerfectApp: App {
    @UIApplicationDelegateAdaptor(DPAppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            if NSClassFromString("XCTestCase") != nil {
                EmptyView()
            } else {
                MainTabView()
            }
        }
    }
}
