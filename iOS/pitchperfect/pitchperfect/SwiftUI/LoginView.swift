import SwiftUI

struct LoginView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var showAuth = false

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

                        Button("Sign up or log in") { showAuth = true }
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
            .background {
                if showAuth {
                    PitchPerfectAuthView(
                        onSignIn: finish,
                        onDismiss: { showAuth = false }
                    )
                }
            }
        }
        .navigationViewStyle(.stack)
    }

    private func finish() {
        showAuth = false
        onFinish()
        dismiss()
    }
}

extension Color {
    static let systemBackground = Color(uiColor: .systemBackground)
}
