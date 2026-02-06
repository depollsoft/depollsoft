//
//  ListMenuHelper.swift
//  tagmaster
//
//  Copyright © 2025 DepollSoft. All rights reserved.
//

import UIKit

@objc class ListMenuHelper: NSObject {
    @objc static func menu(forTagId tagId: Int32) -> UIMenu {
        let allLists = CustomListsModel.allLists()
        var actions: [UIAction] = []

        for meta in allLists {
            let model = ListModel.get(meta.key)
            let isInList = model.contains(Int(tagId))
            let action = UIAction(
                title: meta.name,
                image: isInList ? UIImage(systemName: "checkmark") : nil
            ) { _ in
                if isInList {
                    model.remove(Int(tagId))
                } else {
                    model.add(Int(tagId))
                }
            }
            actions.append(action)
        }

        return UIMenu(title: NSLocalizedString("Add to List", comment: ""), children: actions)
    }
}
