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
    
    @objc public static func barButton(
        systemName: String,
        target: Any,
        selector: Selector
    ) -> UIBarButtonItem {
        let configuration = UIImage.SymbolConfiguration(
            pointSize: 17,
            weight: .regular,
            scale: .medium
        )
        let image = UIImage(
            systemName: systemName,
            withConfiguration: configuration
        )
        return UIBarButtonItem(
            image: image,
            style: .plain,
            target: target,
            action: selector
        )
    }

    @objc public static func getSettingsButton(target: Any, selector: Selector) -> UIBarButtonItem {
        barButton(systemName: "gearshape", target: target, selector: selector)
    }
}
