//
//  DPLoginViewController.swift
//  pitchperfect
//
//  Created by David Poll on 6/22/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
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
extension DPLoginViewController: FUIAuthDelegate {
    @objc public func logIn(_ viewController:UIViewController) {
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
        viewController.present(authUI.authViewController(), animated: true)
    }

    @objc func logInClick() {
        logIn(self)
    }

    public func authUI(_ authUI: FUIAuth, didSignInWith authDataResult: AuthDataResult?, error: Error?) {
        if error == nil {
            self.completeLogIn(authDataResult?.additionalUserInfo?.isNewUser ?? false)
        }
        self.dismiss(animated: true)
    }

    public func authUI(_ authUI: FUIAuth, didFinish operation: FUIAccountSettingsOperationType, error: Error?) {
        self.dismiss(animated: true)
    }
}
#endif
