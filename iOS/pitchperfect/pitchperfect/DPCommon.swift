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
        // Leave the Liquid Glass background untouched; only the Oswald title
        // and label tint ride on top via the legacy attributes.
        navigationController.navigationBar.titleTextAttributes = [
            .foregroundColor: UIColor.label,
            .font: UIFont(name: "Oswald-Medium", size: 19)
                ?? UIFont.preferredFont(forTextStyle: .headline),
        ]
        navigationController.navigationBar.tintColor = .label
    }

    private static let barButtonLabels = [
        "checkmark": "Done",
        "xmark": "Close",
        "gearshape": "Settings",
        "plus": "Add",
        "pencil": "Edit",
        "square.and.arrow.up": "Share",
        "magnifyingglass": "Search",
        "arrow.clockwise": "Refresh",
        "ellipsis.circle": "More",
        "list.bullet": "Set lists",
    ]

    private static func barButtonImage(_ systemName: String) -> UIImage? {
        UIImage(
            systemName: systemName,
            withConfiguration: UIImage.SymbolConfiguration(
                pointSize: 17,
                weight: .regular,
                scale: .medium
            )
        )
    }

    private static func label(_ item: UIBarButtonItem, systemName: String) -> UIBarButtonItem {
        item.accessibilityIdentifier = systemName
        item.accessibilityLabel = barButtonLabels[systemName] ?? systemName
        return item
    }

    @objc public static func barButton(
        systemName: String,
        target: Any,
        selector: Selector
    ) -> UIBarButtonItem {
        label(
            UIBarButtonItem(
                image: barButtonImage(systemName),
                style: .plain,
                target: target,
                action: selector
            ),
            systemName: systemName
        )
    }

    /// The same bar button, but presenting a menu instead of firing an action.
    @objc public static func menuBarButton(
        systemName: String,
        menu: UIMenu
    ) -> UIBarButtonItem {
        label(
            UIBarButtonItem(image: barButtonImage(systemName), menu: menu),
            systemName: systemName
        )
    }

    @objc public static func getSettingsButton(target: Any, selector: Selector) -> UIBarButtonItem {
        barButton(systemName: "gearshape", target: target, selector: selector)
    }
}
