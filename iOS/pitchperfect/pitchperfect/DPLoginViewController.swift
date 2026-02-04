//
//  DPLoginViewController.swift
//  pitchperfect
//
//  Created by David Poll on 6/22/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

import Foundation
import FirebaseAuth
import SwiftUI

#if canImport(FirebaseAuthSwiftUI)
import FirebaseAuthSwiftUI
#endif

// MARK: - SwiftUI Auth View for UIKit Integration

#if canImport(FirebaseAuthSwiftUI)
/// SwiftUI view that wraps FirebaseUI's AuthPickerView for use in UIKit
struct FirebaseAuthView: View {
    let authService: AuthService
    let onSignIn: (Bool) -> Void
    let onDismiss: () -> Void
    
    init(onSignIn: @escaping (Bool) -> Void, onDismiss: @escaping () -> Void) {
        let configuration = AuthConfiguration(
            shouldHideCancelButton: false,
            interactiveDismissEnabled: true
        )
        
        self.authService = AuthService(configuration: configuration)
            .withEmailSignIn()
        self.onSignIn = onSignIn
        self.onDismiss = onDismiss
    }
    
    var body: some View {
        AuthPickerView {
            // This is shown when authenticated - we immediately dismiss
            Color.clear
                .onAppear {
                    // Use time-based comparison with tolerance since date equality can be unreliable
                    let metadata = authService.currentUser?.metadata
                    let isNewUser: Bool
                    if let creationDate = metadata?.creationDate,
                       let lastSignInDate = metadata?.lastSignInDate {
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
