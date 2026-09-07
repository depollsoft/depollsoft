//
//  DPTeachableTagsController.swift
//  tagmaster
//
//  Created by David Poll on 6/8/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation

public extension DPTeachableTagsController {
    @objc func viewDidLoadExtension() {
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(onUserDataChanged),
                                               name: .userDataChanged,
                                               object: nil)
    }
    
    @objc func onUserDataChanged() {
        if !Thread.isMainThread {
            DispatchQueue.main.async { [weak self] in self?.onUserDataChanged() }
            return
        }
        let offset = self.tableView.contentOffset
        self.tableView.reloadData()
        self.tableView.layoutIfNeeded()
        self.tableView.setContentOffset(offset, animated: false)
        self.updateEmptyState()
    }
}
