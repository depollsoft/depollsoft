//
//  DPSettingsController.swift
//  tagmaster
//
//  Created by David Poll on 6/15/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import Firebase
#if canImport(FirebaseAuthUI)
import FirebaseAuthUI
#endif
#if canImport(FirebaseEmailAuthUI)
import FirebaseEmailAuthUI
#endif

#if canImport(FirebaseAuthUI)
extension DPSettingsController: FUIAuthDelegate {
    @objc func logInClick() {
        guard Auth.auth().currentUser == nil else {
            try? Auth.auth().signOut()
            self.refreshLoginButton()
            return
        }

        guard let authUI = FUIAuth.defaultAuthUI() else {
            return
        }

        var providers: [FUIAuthProvider] = []

        #if canImport(FirebaseEmailAuthUI)
        let emailProvider = FUIEmailAuth(authAuthUI: authUI,
                                         signInMethod: EmailPasswordAuthSignInMethod,
                                         forceSameDevice: false,
                                         allowNewEmailAccounts: true,
                                         requireDisplayName: false,
                                         actionCodeSetting: ActionCodeSettings())
        providers.append(emailProvider)
        #endif

        guard !providers.isEmpty else {
            return
        }

        authUI.providers = providers
        authUI.delegate = self
        present(authUI.authViewController(), animated: true)
    }

    public func authUI(_ authUI: FUIAuth, didSignInWith authDataResult: AuthDataResult?, error: Error?) {
        refreshLoginButton()
    }

    public func authUI(_ authUI: FUIAuth, didFinish operation: FUIAccountSettingsOperationType, error: Error?) {
        refreshLoginButton()
    }
}
#endif
