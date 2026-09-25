//
//  LoginScreen.swift
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


/// FirebaseUI's sign-in picker, hosted on the screen that asks for it: the
/// picker presents itself as a sheet whenever `isPresented` is set.
struct FirebaseAuthHost: View {
    @Binding var isPresented: Bool
    let onSignIn: (Bool) -> Void
    let onDismiss: () -> Void

    @State private var authService = FirebaseAuthHost.makeService()
    @State private var didFinish = false

    private static func makeService() -> AuthService {
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
        authService.registerProvider(providerWithButton: PitchPerfectAppleProviderUI())
        #if canImport(FirebasePhoneAuthSwiftUI)
        _ = authService.withPhoneSignIn()
        #endif
        return authService
    }

    /// Whether the account just signed in was created by this sign-in.
    static func isNewUser(_ metadata: UserMetadata) -> Bool {
        guard let created = metadata.creationDate, let lastSignIn = metadata.lastSignInDate else { return false }
        return abs(created.timeIntervalSince(lastSignIn)) <= 1.0
    }

    var body: some View {
        AuthPickerView { Color.clear }
            .environment(authService)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
            .onAppear {
                if isPresented { authService.isPresented = true }
            }
            .onChange(of: isPresented) { _, show in
                if show {
                    didFinish = false
                    authService.isPresented = true
                } else if authService.isPresented {
                    authService.isPresented = false
                }
            }
            .onChange(of: authService.currentUser?.uid) { _, userID in
                guard !didFinish, let currentUser = authService.currentUser,
                      userID == Auth.auth().currentUser?.uid else { return }
                didFinish = true
                onSignIn(Self.isNewUser(currentUser.metadata))
                authService.isPresented = false
            }
            .onChange(of: authService.isPresented) { _, shown in
                guard !shown else { return }
                if !didFinish { onDismiss() }
                isPresented = false
            }
    }
}
#endif

// MARK: - Signing in

enum SignIn {
    /// Starts syncing once an account is known: a new account uploads this
    /// device's songs; an existing one takes the cloud's.
    @MainActor
    static func completed(isNewUser: Bool) {
        DPSettingsModel.sharedInstance.attachToFirestore()
        DPSongsModel.sharedInstance.attachToFirestore(store: isNewUser)
    }
}

private struct SignInSheet: ViewModifier {
    @Binding var isPresented: Bool
    let onSignIn: (Bool) -> Void
    let onDismiss: () -> Void

    func body(content: Content) -> some View {
        #if canImport(FirebaseAuthSwiftUI)
        content.background {
            FirebaseAuthHost(isPresented: $isPresented,
                             onSignIn: { isNewUser in
                                 SignIn.completed(isNewUser: isNewUser)
                                 onSignIn(isNewUser)
                             },
                             onDismiss: onDismiss)
        }
        #else
        content
        #endif
    }
}

extension View {
    /// The Firebase sign-in picker as a page sheet. `onSignIn` receives whether the
    /// account is new (sync has already started); `onDismiss` runs when it closes
    /// without one.
    func signInSheet(isPresented: Binding<Bool>,
                     onSignIn: @escaping (Bool) -> Void,
                     onDismiss: @escaping () -> Void = {}) -> some View {
        modifier(SignInSheet(isPresented: isPresented, onSignIn: onSignIn, onDismiss: onDismiss))
    }
}

// MARK: - The optional login screen

/// Offered once, on the second launch, to anyone not signed in.
struct LoginIntroScreen: View {
    @Environment(\.dismiss) private var dismiss
    @State private var showingSignIn = false

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                LoginExplanation()
                    .padding(.horizontal, 5)
                    .padding(.vertical, 8)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .scrollBounceBehavior(.basedOnSize)
            Button { showingSignIn = true } label: {
                Text("Sign up or log in")
                    .font(.system(size: 15))
                    .frame(minHeight: 30)
            }
            .buttonStyle(.borderless)
        }
        .staffScreenBackground()
        .navigationTitle("Log In To Pitch Perfect")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Skip") { dismiss() }
            }
        }
        .signInSheet(isPresented: $showingSignIn, onSignIn: { _ in dismiss() })
    }
}

private struct LoginExplanation: View {
    var body: some View {
        // UIKit fonts rather than SwiftUI's .system(size:), which tracks its
        // text differently from the HTML-typeset UIKit screen.
        VStack(alignment: .leading, spacing: 50.0 / 3.0) {
            Text("Recommended:").font(Font(UIFont.boldSystemFont(ofSize: 17) as CTFont))
                + Text(" Log in to Pitch Perfect and we'll save your settings and song list to the cloud.")
            Text("When you log in to Pitch Perfect, we'll automatically synchronize your settings and song list from device to device. Whether you just want to back up your songs or are working with multiple phones or tablets, logging in ensures that your data goes where you go.")
            Text("Signing in syncs your song list and settings. You control optional analytics and crash reports in Privacy choices.")
        }
        .font(Font(UIFont.systemFont(ofSize: 17) as CTFont))
        .foregroundStyle(Color(uiColor: .label))
    }
}
