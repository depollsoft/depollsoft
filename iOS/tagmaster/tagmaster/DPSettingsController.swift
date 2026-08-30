//
//  DPSettingsController.swift
//  tagmaster
//
//  Created by David Poll on 6/15/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

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
#if canImport(FirebaseAppleSwiftUI)
import FirebaseAppleSwiftUI
#endif
#if canImport(FirebasePhoneAuthSwiftUI)
import FirebasePhoneAuthSwiftUI
#endif

// MARK: - SwiftUI Auth View for UIKit Integration

#if canImport(FirebaseAuthSwiftUI)
/// SwiftUI view that wraps FirebaseUI's AuthPickerView for use in UIKit
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

        var authService = AuthService(configuration: configuration)
            .withEmailSignIn()
            .withGoogleSignIn()
            .withFacebookSignIn()
            .withAppleSignIn()
        #if canImport(FirebasePhoneAuthSwiftUI)
        authService = authService.withPhoneSignIn()
        #endif

        self.authService = authService
        self.onAuthStateChanged = onAuthStateChanged
        self.onDismiss = onDismiss
    }

    var body: some View {
        AuthPickerView {
            // This is shown when authenticated - we immediately dismiss
            Color.clear
                .onAppear {
                    onAuthStateChanged()
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

// MARK: - UIKit Extension for Settings

extension DPSettingsController {

    /// Handles login/logout button tap
    @objc func logInClick() {
        // If user is already signed in, sign out
        guard Auth.auth().currentUser == nil else {
            do {
                try Auth.auth().signOut()
                self.refreshLoginButton()
            } catch {
                print("Error signing out: \(error.localizedDescription)")
            }
            return
        }

        // Present sign-in UI
        #if canImport(FirebaseAuthSwiftUI)
        let authView = TagMasterAuthView(
            onAuthStateChanged: { [weak self] in
                self?.refreshLoginButton()
            },
            onDismiss: { [weak self] in
                self?.dismiss(animated: true)
            }
        )

        let hostingController = UIHostingController(rootView: authView)
        hostingController.modalPresentationStyle = .pageSheet
        present(hostingController, animated: true)
        #else
        // Fallback: Direct Firebase Auth if FirebaseAuthSwiftUI not available
        print("FirebaseAuthSwiftUI not available - implement fallback auth")
        #endif
    }
}
