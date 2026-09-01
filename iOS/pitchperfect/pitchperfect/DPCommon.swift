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
        let navigationController = UINavigationController(
            rootViewController: settings
        )
        configureInstrumentChrome(navigationController)
        navigationController.modalTransitionStyle = .coverVertical
        navigationController.modalPresentationStyle = .automatic
        viewController.present(navigationController, animated: true)
    }
    
    @objc public static func configureInstrumentChrome(_ navigationController: UINavigationController) {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = .systemBackground
        appearance.shadowColor = .separator
        appearance.titleTextAttributes = [
            .foregroundColor: UIColor.label,
            .font: UIFont(name: "Oswald-Medium", size: 19)
                ?? UIFont.preferredFont(forTextStyle: .headline),
        ]
        navigationController.navigationBar.standardAppearance = appearance
        navigationController.navigationBar.scrollEdgeAppearance = appearance
        navigationController.navigationBar.compactAppearance = appearance
        navigationController.navigationBar.tintColor = .label
        navigationController.navigationBar.isTranslucent = false
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
        let item = UIBarButtonItem(
            image: image,
            style: .plain,
            target: target,
            action: selector
        )
        item.accessibilityIdentifier = systemName
        item.accessibilityLabel = [
            "checkmark": "Done",
            "xmark": "Close",
            "gearshape": "Settings",
            "plus": "Add",
            "pencil": "Edit",
            "square.and.arrow.up": "Share",
            "magnifyingglass": "Search",
            "arrow.clockwise": "Refresh",
        ][systemName] ?? systemName
        return item
    }

    @objc public static func getSettingsButton(target: Any, selector: Selector) -> UIBarButtonItem {
        barButton(systemName: "gearshape", target: target, selector: selector)
    }
}
