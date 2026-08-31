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
        let control = UIControl(frame: CGRect(x: 0, y: 0, width: 44, height: 44))
        control.addTarget(target, action: selector, for: .touchUpInside)
        control.accessibilityIdentifier = systemName
        control.accessibilityLabel = [
            "checkmark": "Done",
            "xmark": "Close",
            "gearshape": "Settings",
            "plus": "Add",
            "pencil": "Edit",
            "square.and.arrow.up": "Share",
            "magnifyingglass": "Search",
            "arrow.clockwise": "Refresh",
        ][systemName] ?? systemName
        control.accessibilityTraits = .button

        var buttonConfiguration: UIButton.Configuration
        if #available(iOS 26.0, *) {
            buttonConfiguration = .glass()
        } else {
            buttonConfiguration = .gray()
        }
        buttonConfiguration.image = image
        buttonConfiguration.buttonSize = .mini
        buttonConfiguration.contentInsets = NSDirectionalEdgeInsets(
            top: 8,
            leading: 8,
            bottom: 8,
            trailing: 8
        )

        let visualButton = UIButton(configuration: buttonConfiguration)
        visualButton.frame = CGRect(x: 4, y: 4, width: 36, height: 36)
        visualButton.isUserInteractionEnabled = false
        control.addSubview(visualButton)

        let item = UIBarButtonItem(customView: control)
        if #available(iOS 26.0, *) {
            item.hidesSharedBackground = true
        }
        return item
    }

    @objc public static func getSettingsButton(target: Any, selector: Selector) -> UIBarButtonItem {
        barButton(systemName: "gearshape", target: target, selector: selector)
    }
}
