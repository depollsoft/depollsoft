//
//  DPHomeViewController.swift
//  tagmaster
//
//  Created by David Poll on 6/8/19.
//  Copyright © 2019 DepollSoft. All rights reserved.
//

import Foundation
import UIKit

extension DPHomeViewController: UITextFieldDelegate {
    @objc func viewDidLoadExtension() {
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(onUserDataChanged),
                                               name: .userDataChanged,
                                               object: nil)
    }
    
    @objc func onUserDataChanged() {
        self.tableView.reloadData()
    }
    
    public func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        if string.rangeOfCharacter(from: NSCharacterSet.decimalDigits) != nil || string.count == 0 {
            return true
        }
        return false
    }
    
    @objc func openTag() {
        let alert = UIAlertController(title: "Open Tag", message: "Enter Tag ID", preferredStyle: .alert)
        alert.addTextField {
            $0.keyboardType = .decimalPad
            $0.delegate = self
            $0.returnKeyType = .go
            $0.enablesReturnKeyAutomatically = true
        }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "Open", style: .default, handler: { action in
            if let tagId = Int32(alert.textFields![0].text ?? "") {
                self.openTag(tagId: tagId)
            }
        }))
        self.present(alert, animated: true)
    }
    
    private func openTag(tagId: Int32) {
        DPAppDelegate.showTag(withId: tagId, from: self)
    }
    
    public func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        if Int32(textField.text ?? "") != nil {
            return true
        }
        return false
    }
}
