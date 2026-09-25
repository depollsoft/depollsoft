//
//  TagDetailViewController.swift
//  tagmaster
//
//  Puts the SwiftUI tag detail on the app's UIKit navigation stacks and split
//  view. Everything the detail shows and does lives in TagDetailScreen and
//  TagDetailModel; this controller only answers the questions the UIKit shell
//  asks (which tag, which list, the ⌘↑/⌘↓ commands, whether it sits beside a
//  list) and carries out the navigation the detail asks for.
//
//  It keeps the Objective-C name DPTagViewController so the shell's routing
//  (+[DPAppDelegate showTagWithId:from:]) is unchanged.
//

import SwiftUI
import UIKit

@objc(DPTagViewController)
final class TagDetailViewController: UIViewController {
    let model: TagDetailModel
    private let hosting: UIHostingController<TagDetailScreen>
    private var selectionObserver: NSObjectProtocol?

    init(model: TagDetailModel) {
        self.model = model
        hosting = UIHostingController(rootView: TagDetailScreen(model: model))
        super.init(nibName: nil, bundle: nil)
        hosting.navigationItem.largeTitleDisplayMode = .never
        model.navigator = TMDetailNavigator(
            showTag: { [weak self] id in
                guard let self else { return }
                DPAppDelegate.showTag(withId: id, from: self)
            },
            showList: { [weak self] key in
                guard let self else { return }
                DPAppDelegate.showList(withKey: key, from: self)
            },
            showSheetMusic: { [weak self] document in
                guard let self else { return }
                let reader = TMSheetMusicViewController(document: document, summary: model.summary)
                if let navigation = self.navigationController {
                    navigation.pushViewController(reader, animated: true)
                } else {
                    self.present(UINavigationController(rootViewController: reader), animated: true)
                }
            })
    }

    @objc convenience init() {
        self.init(model: TagDetailModel())
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) { fatalError("TagDetailViewController is created in code") }

    isolated deinit {
        if let selectionObserver { NotificationCenter.default.removeObserver(selectionObserver) }
    }

    // MARK: The shell's questions

    @objc var tagId: Int32 {
        get { model.tagId }
        set { model.show(tagId: newValue) }
    }

    @objc weak var source: TMTagListSource? {
        get { model.source }
        set { model.source = newValue }
    }

    /// The hosted screen owns the bar: its title and toolbar live on this item.
    override var navigationItem: UINavigationItem { hosting.navigationItem }

    override var undoManager: UndoManager? { model.undoManager }
    override var canBecomeFirstResponder: Bool { true }

    // MARK: Lifecycle

    override func viewDidLoad() {
        super.viewDidLoad()
        DPAppDelegate.setUpBackground(view)
        addChild(hosting)
        hosting.view.backgroundColor = .clear
        hosting.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(hosting.view)
        NSLayoutConstraint.activate([
            hosting.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            hosting.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            hosting.view.topAnchor.constraint(equalTo: view.topAnchor),
            hosting.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        hosting.didMove(toParent: self)
        selectionObserver = NotificationCenter.default.addObserver(
            forName: .TMTagSelectionDidChange, object: nil, queue: .main
        ) { [weak self] _ in
            MainActor.assumeIsolated { self?.updateExpanded() }
        }
        updateExpanded()
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        updateExpanded()
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        model.screenVisible = true
        model.applicationActive = UIApplication.shared.applicationState == .active
        // Shake-to-undo asks the first responder for its undo manager.
        becomeFirstResponder()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        model.screenVisible = false
    }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        let horizontal = view.safeAreaInsets.left > 0 || view.safeAreaInsets.right > 0
        if model.hasHorizontalSafeArea != horizontal { model.hasHorizontalSafeArea = horizontal }
    }

    private func updateExpanded() {
        let expanded = splitViewController.map { !$0.isCollapsed } ?? false
        if model.expanded != expanded { model.expanded = expanded }
    }

    // MARK: Keyboard stepping

    @objc func stepToPreviousTag() { model.stepToPreviousTag() }
    @objc func stepToNextTag() { model.stepToNextTag() }

    override func canPerformAction(_ action: Selector, withSender sender: Any?) -> Bool {
        if action == #selector(stepToPreviousTag) { return model.canStep && model.hasPreviousTag }
        if action == #selector(stepToNextTag) { return model.canStep && model.hasNextTag }
        return super.canPerformAction(action, withSender: sender)
    }

    override var keyCommands: [UIKeyCommand]? {
        guard model.canStep else { return super.keyCommands }
        let previous = UIKeyCommand(input: UIKeyCommand.inputUpArrow, modifierFlags: .command,
                                    action: #selector(stepToPreviousTag))
        previous.discoverabilityTitle = "Previous Tag"
        let next = UIKeyCommand(input: UIKeyCommand.inputDownArrow, modifierFlags: .command,
                                action: #selector(stepToNextTag))
        next.discoverabilityTitle = "Next Tag"
        return [previous, next] + (super.keyCommands ?? [])
    }
}
