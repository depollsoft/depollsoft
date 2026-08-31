import SwiftUI
import FirebaseAuth
#if canImport(FirebaseAuthUI)
import FirebaseAuthUI
#endif
#if canImport(FirebaseEmailAuthUI)
import FirebaseEmailAuthUI
#endif

struct LoginView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var showFirebaseUI = false
    @State private var errorMessage: String?

    let onFinish: () -> Void

    init(onFinish: @escaping () -> Void = {}) {
        self.onFinish = onFinish
    }

    var body: some View {
        NavigationView {
            ZStack {
                PitchPerfectBackground()
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        Text(
                            "Recommended: Log in to Pitch Perfect and we'll save your settings and song list to the cloud."
                            + "\n\nWhen you log in, we'll synchronize your settings and song list across your devices."
                            + " Whether you want a backup or use multiple phones or tablets, your data goes where you go."
                            + "\n\nPitch Perfect does not collect any of your personal data for this free service."
                        )
                        .font(.body)

                        Button("Sign up or log in") {
                            showFirebaseUI = true
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                        .frame(maxWidth: .infinity)
                        .accessibilityIdentifier("SignIn")
                    }
                    .padding()
                    .frame(maxWidth: 680)
                    .frame(maxWidth: .infinity)
                }
            }
            .navigationBarTitle("Log In To Pitch Perfect", displayMode: .inline)
            .navigationBarItems(trailing: Button("Skip", action: finish))
            .sheet(isPresented: $showFirebaseUI) {
                #if canImport(FirebaseAuthUI)
                FirebaseUIAuthView { result, error in
                    showFirebaseUI = false
                    if let error {
                        errorMessage = error.localizedDescription
                    } else if result != nil {
                        DispatchQueue.main.async { finish() }
                    }
                }
                #else
                Text("Firebase Auth UI is not available.")
                #endif
            }
            .alert("Could Not Log In", isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )) {
                Button("OK", role: .cancel) { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "Please try again.")
            }
        }
        .navigationViewStyle(.stack)
    }

    private func finish() {
        onFinish()
        dismiss()
    }
}

extension Color {
    static let systemBackground = Color(uiColor: .systemBackground)
}

#if canImport(FirebaseAuthUI)
struct FirebaseUIAuthView: UIViewControllerRepresentable {
    let onCompletion: (AuthDataResult?, Error?) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        guard let authUI = FUIAuth.defaultAuthUI() else { return UIViewController() }
        authUI.delegate = context.coordinator
        #if canImport(FirebaseEmailAuthUI)
        authUI.providers = [
            FUIEmailAuth(
                authAuthUI: authUI,
                signInMethod: EmailPasswordAuthSignInMethod,
                forceSameDevice: false,
                allowNewEmailAccounts: true,
                requireDisplayName: false,
                actionCodeSetting: ActionCodeSettings()
            )
        ]
        #endif
        return authUI.authViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator(onCompletion: onCompletion) }

    final class Coordinator: NSObject, FUIAuthDelegate {
        let onCompletion: (AuthDataResult?, Error?) -> Void

        init(onCompletion: @escaping (AuthDataResult?, Error?) -> Void) {
            self.onCompletion = onCompletion
        }

        func authUI(_ authUI: FUIAuth, didSignInWith authDataResult: AuthDataResult?, error: Error?) {
            onCompletion(authDataResult, error)
        }
    }
}
#endif
