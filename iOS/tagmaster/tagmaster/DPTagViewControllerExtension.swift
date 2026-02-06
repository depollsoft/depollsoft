import UIKit

extension DPTagViewController {
    @objc func showListPicker() {
        let allLists = CustomListsModel.allLists()
        let tagId = Int(self.tagId)

        let picker = UIAlertController(title: "Add to List", message: nil, preferredStyle: .actionSheet)
        picker.popoverPresentationController?.sourceView = self.view

        for meta in allLists {
            let model = ListModel.get(meta.key)
            let isInList = model.contains(tagId)
            let prefix = isInList ? "✓ " : "   "
            let title = prefix + meta.name
            picker.addAction(UIAlertAction(title: title, style: .default) { _ in
                if isInList {
                    model.remove(tagId)
                } else {
                    model.add(tagId)
                }
            })
        }

        picker.addAction(UIAlertAction(title: "Create New List...", style: .default) { _ in
            self.showCreateListAndAdd()
        })

        picker.addAction(UIAlertAction(title: "Cancel", style: .cancel))

        self.present(picker, animated: true)
    }

    @objc func showCreateListAndAdd() {
        let tagId = Int(self.tagId)
        let alert = UIAlertController(title: "Create New List", message: nil, preferredStyle: .alert)
        alert.addTextField { $0.placeholder = "List name" }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        alert.addAction(UIAlertAction(title: "Create", style: .default) { _ in
            let name = alert.textFields?[0].text?.trimmingCharacters(in: .whitespaces) ?? ""
            if !name.isEmpty {
                let key = CustomListsModel.createList(name)
                ListModel.get(key).add(tagId)
            }
        })
        self.present(alert, animated: true)
    }
}
