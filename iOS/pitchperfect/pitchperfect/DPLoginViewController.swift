//
//  DPLoginViewController.swift
//  pitchperfect
//
//  Created by David Poll on 6/22/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

import Foundation
import Firebase
import FirebaseAuthUI
import FirebaseOAuthUI
import FirebaseGoogleAuthUI
import FirebaseEmailAuthUI
import FirebaseFacebookAuthUI

extension DPLoginViewController: FUIAuthDelegate {
    @objc func logInClick() {
        if Auth.auth().currentUser == nil {
            let authUI = FUIAuth.defaultAuthUI()!
            let providers: [FUIAuthProvider] = [
                FUIEmailAuth(authAuthUI: authUI,
                             signInMethod: EmailPasswordAuthSignInMethod,
                             forceSameDevice: false,
                             allowNewEmailAccounts: true,
                             requireDisplayName: false,
                             actionCodeSetting: ActionCodeSettings()),
                FUIGoogleAuth(authUI: authUI),
                FUIFacebookAuth(authUI: authUI),
                FUIOAuth.appleAuthProvider()
            ]
            authUI.providers = providers
            authUI.delegate = self
            
            self.present(authUI.authViewController(), animated: true)
        } else {
            try! Auth.auth().signOut()
        }
    }
    
    public func authUI(_ authUI: FUIAuth, didSignInWith authDataResult: AuthDataResult?, error: Error?) {
        if error == nil {
            self.completeLogIn(authDataResult?.additionalUserInfo?.isNewUser ?? false)
        }
        self.dismiss(animated: true)
    }
    
    public func authUI(_ authUI: FUIAuth, didFinish operation: FUIAccountSettingsOperationType, error: Error?) {
    }
}
