//
//  DPCommon.swift
//  pitchperfect
//
//  Created by David Poll on 6/25/21.
//  Copyright © 2021 DepollSoft. All rights reserved.
//

import Foundation

@objc public class DPCommon: NSObject {
    @objc public static func openSettings(_ viewController: UIViewController, barButtonItem: UIBarButtonItem) {
        let settings = DPSettingsViewController.sharedInstance()!
        settings.modalTransitionStyle = .coverVertical
        settings.modalPresentationStyle = .automatic
        viewController.present(settings, animated: true)
    }
    
    @objc public static func getSettingsButton(target: Any, selector: Selector) -> UIBarButtonItem {
        let settingsButton = UIBarButtonItem(image: UIImage(systemName: "gearshape"), style: .plain, target: target, action: selector)
        return settingsButton
    }
}
