//
//  DPHomeViewController.swift
//  tagmaster
//
//  Created by David Poll on 6/8/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import SmartlookConsentSDK
import Firebase

public extension DPHomeViewController {
    @objc func viewDidLoadExtension() {
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(onUserDataChanged),
                                               name: .userDataChanged,
                                               object: nil)
        SmartlookConsentSDK.check {
            if SmartlookConsentSDK.consentState(for: .analytics) == .provided {
            }
            if SmartlookConsentSDK.consentState(for: .privacy) == .provided {
                FirebaseApp.app()?.isDataCollectionDefaultEnabled = true
            }
        }
    }
    
    @objc func onUserDataChanged() {
        self.tableView.reloadData()
    }
}
