//
//  TMSignIn.swift
//  tagmaster
//
//  FirebaseUI's sign-in picker with Tag Master's Apple button, shown from Settings.
//

import AuthenticationServices
import CryptoKit
import Foundation
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
#if canImport(FirebaseOAuthSwiftUI)
import FirebaseOAuthSwiftUI
#endif
#if canImport(FirebasePhoneAuthSwiftUI)
import FirebasePhoneAuthSwiftUI
#endif

// MARK: - Sign-in

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
                do {
                    let nonce = try TagMasterAppleNonce.random()
                    self.nonce = nonce
                    request.requestedScopes = [.fullName, .email]
                    request.nonce = TagMasterAppleNonce.hash(nonce)
                } catch {
                    errorMessage = error.localizedDescription
                }
            } onCompletion: { result in
                Task { @MainActor in
                    do {
                        let authorization = try result.get()
                        guard let appleCredential =
                                authorization.credential as? ASAuthorizationAppleIDCredential,
                              let tokenData = appleCredential.identityToken,
                              let token = String(data: tokenData, encoding: .utf8),
                              let nonce else {
                            throw NSError(
                                domain: "TagMasterAppleSignIn",
                                code: -1,
                                userInfo: [
                                    NSLocalizedDescriptionKey:
                                        "Apple did not return valid credentials."
                                ]
                            )
                        }
                        let credential = OAuthProvider.appleCredential(
                            withIDToken: token,
                            rawNonce: nonce,
                            fullName: appleCredential.fullName
                        )
                        _ = try await authService.signIn(credentials: credential)
                    } catch {
                        errorMessage = error.localizedDescription
                    }
                }
            }
            .signInWithAppleButtonStyle(.black)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .opacity(0.001)
        }
        .contentShape(Capsule())
        .alert(
            "Apple sign-in failed",
            isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )
        ) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage ?? "Please try again.")
        }
    }
}


/// FirebaseUI's AuthPickerView, reporting a sign-in or a dismissal.
struct TagMasterAuthView: View {
    let authService: AuthService
    let onAuthStateChanged: () -> Void
    let onDismiss: () -> Void

    init(onAuthStateChanged: @escaping () -> Void, onDismiss: @escaping () -> Void) {
        let configuration = AuthConfiguration(
            logo: ImageResource(name: "AuthLogo", bundle: .main),
            shouldHideCancelButton: false,
            interactiveDismissEnabled: true,
            customStringsBundle: .main,
            mfaIssuer: "Tag Master"
        )

        let authService = AuthService(configuration: configuration)
            .withEmailSignIn()
            .withGoogleSignIn()
            .withFacebookSignIn()
        authService.registerProvider(
            providerWithButton: TagMasterAppleProviderUI()
        )
        #if canImport(FirebasePhoneAuthSwiftUI)
        _ = authService.withPhoneSignIn()
        #endif

        self.authService = authService
        self.onAuthStateChanged = onAuthStateChanged
        self.onDismiss = onDismiss
    }

    @State private var didFinish = false

    var body: some View {
        AuthPickerView { Color.clear }
            .environment(authService)
            .onAppear {
                authService.isPresented = true
            }
            .onChange(of: authService.currentUser?.uid) { _, userID in
                guard !didFinish,
                      let userID,
                      userID == Auth.auth().currentUser?.uid else {
                    return
                }
                didFinish = true
                onAuthStateChanged()
            }
            .onChange(of: authService.isPresented) { _, isPresented in
                if !isPresented && !didFinish {
                    onDismiss()
                }
            }
    }
}
#endif

