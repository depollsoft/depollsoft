import SwiftUI

@main
struct TagMasterApp: App {
    @UIApplicationDelegateAdaptor(DPAppDelegate.self) private var appDelegate
    @StateObject private var appState = TagMasterAppState.shared

    var body: some Scene {
        WindowGroup {
            if NSClassFromString("XCTestCase") != nil {
                EmptyView()
            } else {
                MainAppView()
                    .environmentObject(appState)
            }
        }
    }
}
