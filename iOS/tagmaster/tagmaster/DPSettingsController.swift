//
//  DPSettingsController.swift
//  tagmaster
//
//  Created by David Poll on 6/15/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import Firebase
import FirebaseUI

extension DPSettingsController: FUIAuthDelegate {
    @objc func logInClick() {
        if Auth.auth().currentUser == nil {
            let authUI = FUIAuth.defaultAuthUI()
            let providers: [FUIAuthProvider] = [
                FUIEmailAuth(),
                FUIGoogleAuth(),
                FUIFacebookAuth(),
                FUIOAuth.appleAuthProvider()
            ]
            authUI?.providers = providers
            authUI?.delegate = self
            
            self.present(authUI!.authViewController(), animated: true)
        } else {
            try! Auth.auth().signOut()
            self.refreshLoginButton()
        }
    }
    
    public func authUI(_ authUI: FUIAuth, didSignInWith authDataResult: AuthDataResult?, error: Error?) {
        self.refreshLoginButton()
    }
    
    public func authUI(_ authUI: FUIAuth, didFinish operation: FUIAccountSettingsOperationType, error: Error?) {
        self.refreshLoginButton()
    }
}
