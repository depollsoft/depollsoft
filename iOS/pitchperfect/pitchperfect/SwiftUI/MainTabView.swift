import SwiftUI
import FirebaseAuth

extension View {
    @ViewBuilder
    func pitchPerfectTabBarStyle() -> some View {
        if #available(iOS 18.0, *) {
            tabViewStyle(.tabBarOnly)
        } else {
            self
        }
    }
}

struct MainTabView: View {
    @State private var showLogin = false
    @AppStorage("depollsoft.pitchperfect.LoginShown") private var loginShown = false

    private var isUITesting: Bool {
        ProcessInfo.processInfo.arguments.contains("--uitesting")
    }

    var body: some View {
        TabView {
            PitchPipeView()
                .tabItem { Label("Pitch Pipe", image: "pitchpipe") }
            NotesView()
                .tabItem { Label("Notes", image: "notes") }
            KeysView()
                .tabItem { Label("Keys", image: "keys") }
            SongListView()
                .tabItem { Label("Songs", image: "songs") }
        }
        .pitchPerfectTabBarStyle()
        .onAppear {
            guard !isUITesting, !loginShown, Auth.auth().currentUser == nil else { return }
            DispatchQueue.main.async { showLogin = true }
        }
        .fullScreenCover(isPresented: $showLogin) {
            LoginView {
                loginShown = true
                showLogin = false
            }
        }
    }
}
