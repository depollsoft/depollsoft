import AuthenticationServices
import CryptoKit
import FirebaseAuth
import SwiftUI

#if canImport(FirebaseAuthSwiftUI)
import FirebaseAuthSwiftUI
import FirebaseAuthUIComponents
#endif
#if canImport(FirebaseGoogleSwiftUI)
import FirebaseGoogleSwiftUI
#endif
#if canImport(FirebaseFacebookSwiftUI)
import FirebaseFacebookSwiftUI
#endif
#if canImport(FirebasePhoneAuthSwiftUI)
import FirebasePhoneAuthSwiftUI
#endif

#if canImport(FirebaseAuthSwiftUI)
private enum TagMasterAppleNonce {
    static func random() throws -> String {
        let characters = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var bytes = [UInt8](repeating: 0, count: 32)
        let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        guard status == errSecSuccess else {
            throw NSError(
                domain: "TagMasterAppleSignIn",
                code: Int(status),
                userInfo: [NSLocalizedDescriptionKey: "Unable to create a secure sign-in request."]
            )
        }
        return String(bytes.map { characters[Int($0) % characters.count] })
    }

    static func hash(_ value: String) -> String {
        SHA256.hash(data: Data(value.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
    }
}

private final class TagMasterAppleProvider: AuthProviderSwift {}

private final class TagMasterAppleProviderUI: AuthProviderUI {
    let id = "apple.com"
    let displayName = "Apple"
    let provider: AuthProviderSwift = TagMasterAppleProvider()

    @MainActor func authButton() -> AnyView {
        AnyView(TagMasterAppleSignInButton())
    }
}

private struct TagMasterAppleSignInButton: View {
    @Environment(AuthService.self) private var authService
    @State private var nonce: String?
    @State private var errorMessage: String?

    var body: some View {
        AuthProviderButton(
            label: "Sign in with Apple",
            style: .apple,
            accessibilityId: "sign-in-with-apple-visual"
        ) {}
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .overlay {
            SignInWithAppleButton(.signIn) { request in
                prepare(request)
            } onCompletion: { result in
                complete(result)
            }
            .signInWithAppleButtonStyle(.black)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .opacity(0.001)
        }
        .contentShape(Capsule())
        .alert("Apple sign-in failed", isPresented: errorPresented) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage ?? "Please try again.")
        }
    }

    private var errorPresented: Binding<Bool> {
        Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } }
        )
    }

    private func prepare(_ request: ASAuthorizationAppleIDRequest) {
        do {
            let nonce = try TagMasterAppleNonce.random()
            self.nonce = nonce
            request.requestedScopes = [.fullName, .email]
            request.nonce = TagMasterAppleNonce.hash(nonce)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func complete(_ result: Result<ASAuthorization, Error>) {
        Task { @MainActor in
            do {
                let authorization = try result.get()
                guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                      let tokenData = credential.identityToken,
                      let token = String(data: tokenData, encoding: .utf8),
                      let nonce else {
                    throw NSError(
                        domain: "TagMasterAppleSignIn",
                        code: -1,
                        userInfo: [NSLocalizedDescriptionKey: "Apple did not return valid credentials."]
                    )
                }
                let firebaseCredential = OAuthProvider.appleCredential(
                    withIDToken: token,
                    rawNonce: nonce,
                    fullName: credential.fullName
                )
                _ = try await authService.signIn(credentials: firebaseCredential)
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }
}

struct TagMasterAuthView: View {
    @State private var authService: AuthService
    let onAuthStateChanged: () -> Void
    let onDismiss: () -> Void
    @State private var didFinish = false

    init(onAuthStateChanged: @escaping () -> Void, onDismiss: @escaping () -> Void) {
        let configuration = AuthConfiguration(
            logo: ImageResource(name: "AuthLogo", bundle: .main),
            shouldHideCancelButton: false,
            interactiveDismissEnabled: true,
            customStringsBundle: .main,
            mfaIssuer: "Tag Master"
        )
        let service = AuthService(configuration: configuration)
            .withEmailSignIn()
            .withGoogleSignIn()
            .withFacebookSignIn()
        service.registerProvider(providerWithButton: TagMasterAppleProviderUI())
        #if canImport(FirebasePhoneAuthSwiftUI)
        _ = service.withPhoneSignIn()
        #endif
        _authService = State(initialValue: service)
        self.onAuthStateChanged = onAuthStateChanged
        self.onDismiss = onDismiss
    }

    var body: some View {
        AuthPickerView { Color.clear }
            .environment(authService)
            .onAppear {
                DispatchQueue.main.async { authService.isPresented = true }
            }
            .onChange(of: authService.currentUser?.uid) { _, userID in
                guard !didFinish,
                      userID == Auth.auth().currentUser?.uid,
                      userID != nil else { return }
                didFinish = true
                onAuthStateChanged()
            }
            .onChange(of: authService.isPresented) { _, isPresented in
                if !isPresented && !didFinish { onDismiss() }
            }
    }
}
#else
struct TagMasterAuthView: View {
    let onAuthStateChanged: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        ContentUnavailableView(
            "Sign-in unavailable",
            systemImage: "person.crop.circle.badge.exclamationmark"
        )
    }
}
#endif
