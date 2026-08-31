//
//  DPLoginViewController.swift
//  pitchperfect
//
//  Created by David Poll on 6/22/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

import AuthenticationServices
import CryptoKit
import Foundation
import FirebaseAuth
import SwiftUI

#if canImport(FirebaseAuthSwiftUI)
import FirebaseAuthSwiftUI
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

// MARK: - SwiftUI Auth View for UIKit Integration

#if canImport(FirebaseAuthSwiftUI)
private enum PitchPerfectAppleNonce {
    static func random() throws -> String {
        let characters = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var bytes = [UInt8](repeating: 0, count: 32)
        let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        guard status == errSecSuccess else {
            throw NSError(
                domain: "PitchPerfectAppleSignIn",
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

private final class PitchPerfectAppleProvider: AuthProviderSwift {}

private final class PitchPerfectAppleProviderUI: AuthProviderUI {
    let id = "apple.com"
    let displayName = "Apple"
    let provider: AuthProviderSwift = PitchPerfectAppleProvider()

    @MainActor func authButton() -> AnyView {
        AnyView(PitchPerfectAppleSignInButton())
    }
}

private struct PitchPerfectAppleSignInButton: View {
    @Environment(AuthService.self) private var authService
    @State private var nonce: String?
    @State private var errorMessage: String?

    var body: some View {
        SignInWithAppleButton(.signIn) { request in
            do {
                let nonce = try PitchPerfectAppleNonce.random()
                self.nonce = nonce
                request.requestedScopes = [.fullName, .email]
                request.nonce = PitchPerfectAppleNonce.hash(nonce)
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
                            domain: "PitchPerfectAppleSignIn",
                            code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "Apple did not return valid credentials."]
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
        .frame(height: 50)
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


/// SwiftUI view that wraps FirebaseUI's AuthPickerView for use in UIKit
struct FirebaseAuthView: View {
    let authService: AuthService
    let onSignIn: (Bool) -> Void
    let onDismiss: () -> Void

    init(onSignIn: @escaping (Bool) -> Void, onDismiss: @escaping () -> Void) {
        let configuration = AuthConfiguration(
            logo: ImageResource(name: "AuthLogo", bundle: .main),
            shouldHideCancelButton: false,
            interactiveDismissEnabled: true,
            customStringsBundle: .main,
            mfaIssuer: "Pitch Perfect"
        )

        let authService = AuthService(configuration: configuration)
            .withEmailSignIn()
            .withGoogleSignIn()
            .withFacebookSignIn()
        authService.registerProvider(
            providerWithButton: PitchPerfectAppleProviderUI()
        )
        #if canImport(FirebasePhoneAuthSwiftUI)
        _ = authService.withPhoneSignIn()
        #endif

        self.authService = authService
        self.onSignIn = onSignIn
        self.onDismiss = onDismiss
    }

    var body: some View {
        AuthPickerView {
            // This is shown when authenticated - we immediately dismiss
            Color.clear
                .onAppear {
                    guard let currentUser = authService.currentUser,
                          Auth.auth().currentUser?.uid == currentUser.uid else {
                        return
                    }

                    // Use time-based comparison with tolerance since date equality can be unreliable
                    let metadata = currentUser.metadata
                    let isNewUser: Bool
                    if let creationDate = metadata.creationDate,
                       let lastSignInDate = metadata.lastSignInDate {
                        isNewUser = abs(creationDate.timeIntervalSince(lastSignInDate)) <= 1.0
                    } else {
                        isNewUser = false
                    }
                    onSignIn(isNewUser)
                    onDismiss()
                }
        }
        .environment(authService)
        .onAppear {
            authService.isPresented = true
        }
    }
}
#endif

// MARK: - UIKit Extension for Login

extension DPLoginViewController {

    /// Presents the Firebase authentication UI
    /// - Parameter viewController: The view controller to present from
    @objc public func logIn(_ viewController: UIViewController) {
        #if canImport(FirebaseAuthSwiftUI)
        let authView = FirebaseAuthView(
            onSignIn: { [weak self] isNewUser in
                self?.completeLogIn(isNewUser)
            },
            onDismiss: { [weak viewController] in
                viewController?.dismiss(animated: true)
            }
        )

        let hostingController = UIHostingController(rootView: authView)
        hostingController.modalPresentationStyle = .pageSheet
        viewController.present(hostingController, animated: true)
        #else
        // Fallback: Direct Firebase Auth if FirebaseAuthSwiftUI not available
        print("FirebaseAuthSwiftUI not available - implement fallback auth")
        #endif
    }

    @objc func logInClick() {
        logIn(self)
    }
}
