//
//  TMActionSheet.swift
//  tagmaster
//
//  The system action sheet, presented from SwiftUI.
//
//  On iOS 26 SwiftUI's confirmationDialog always draws as a menu-like bubble
//  anchored to its source, without a Cancel row. Tag Master's tag actions were
//  the classic sheet: a centred card with Cancel on iPhone, a popover from the
//  button on iPad. This presents that same UIAlertController from wherever the
//  modifier sits, so the sheet looks and behaves exactly as it did.
//

import SwiftUI

struct TMSheetAction: Identifiable {
    enum Style { case normal, cancel, destructive }

    let id = UUID()
    let title: String
    var style: Style = .normal
    let handler: () -> Void
}

/// A plain view that presents a controller from the window it sits in. Presenters
/// live in toolbars, where a view *controller* representable would be adopted by the
/// navigation controller as a pushed screen; a view stays a view. SwiftUI also builds
/// toolbar items twice (once off screen to measure them), so only the copy in a window
/// presents, and never while something of the same kind is already up.
final class TMPresentingAnchor: UIView {
    var wanted = false
    var make: (TMPresentingAnchor) -> UIViewController = { _ in UIViewController() }
    /// Something like this is already presented (by another copy of this control).
    var alreadyUp: (UIViewController) -> Bool = { _ in false }
    /// Wanting it while one is already up takes that one over, so this copy can close
    /// it: for a presentation only one control can want at a time, whose first copy
    /// may have gone with its shell.
    var adoptsPresented = false
    private(set) weak var presented: UIViewController?

    override init(frame: CGRect) {
        super.init(frame: frame)
        isUserInteractionEnabled = false
        backgroundColor = .clear
        isAccessibilityElement = false
    }

    required init?(coder: NSCoder) { fatalError("TMPresentingAnchor is created in code") }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        sync()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        sync()
    }

    func sync() {
        // A bar item is first placed at a provisional size; anchor once it has its own.
        if wanted, presented == nil, bounds.height >= 30, let root = window?.rootViewController {
            let top = TMPresentingAnchor.top(root)
            guard !alreadyUp(top) else {
                if adoptsPresented { presented = top }
                return
            }
            let controller = make(self)
            presented = controller
            top.present(controller, animated: UIView.areAnimationsEnabled)
        } else if !wanted, let controller = presented {
            presented = nil
            if controller.presentingViewController != nil { controller.dismiss(animated: true) }
        }
    }

    func forget() { presented = nil }

    /// The controller currently on top, which is the one that may present.
    static func top(_ controller: UIViewController) -> UIViewController {
        var top = controller
        while let next = top.presentedViewController, !next.isBeingDismissed { top = next }
        return top
    }
}

/// Put in the background of the control the sheet belongs to.
struct TMActionSheet: UIViewRepresentable {
    @Binding var isPresented: Bool
    let actions: [TMSheetAction]
    var title: String?
    var message: String?
    /// Hang the sheet from the control on every device. UIKit anchors any action
    /// sheet given a source view, so on iOS 26 an anchored iPhone sheet is a
    /// bubble at the control (no Cancel row), as the rating sheet always was.
    var anchoredEverywhere = false

    /// Hears a popover dismissed by a tap outside it, which runs no action. The sheet
    /// holds it, so it still clears the request after the control that opened it has
    /// gone (an iPhone Duo opening, closing or turning swaps the whole shell).
    final class Closer: NSObject, UIPopoverPresentationControllerDelegate {
        let dismissed: () -> Void
        init(dismissed: @escaping () -> Void) { self.dismissed = dismissed }
        func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
            dismissed()
        }
    }

    private static var closerKey: UInt8 = 0

    func makeUIView(context: Context) -> TMPresentingAnchor { TMPresentingAnchor() }

    func updateUIView(_ anchor: TMPresentingAnchor, context: Context) {
        let binding = $isPresented
        let actions = actions, title = title, message = message, anchoredEverywhere = anchoredEverywhere
        anchor.alreadyUp = { $0 is UIAlertController }
        // A sheet already up for this control (opened from its copy in the shell the
        // window had before) is this control's to close now.
        anchor.adoptsPresented = true
        anchor.make = { anchor in
            let sheet = UIAlertController(title: title, message: message, preferredStyle: .actionSheet)
            for action in actions {
                let style: UIAlertAction.Style = switch action.style {
                case .normal: .default
                case .cancel: .cancel
                case .destructive: .destructive
                }
                sheet.addAction(UIAlertAction(title: action.title, style: style) { _ in
                    binding.wrappedValue = false
                    action.handler()
                })
            }
            // iPhone shows the classic centred card; iPad, and an open iPhone Duo, need a popover source.
            if anchoredEverywhere || anchor.traitCollection.tmFloatsPopovers {
                let closer = Closer { [weak anchor] in
                    anchor?.forget()
                    binding.wrappedValue = false
                }
                objc_setAssociatedObject(sheet, &TMActionSheet.closerKey, closer, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
                sheet.popoverPresentationController?.delegate = closer
                sheet.popoverPresentationController?.sourceView = anchor
                sheet.popoverPresentationController?.sourceRect = anchor.bounds
            }
            return sheet
        }
        anchor.wanted = isPresented
        // Let the bar finish placing this copy before anchoring to it.
        DispatchQueue.main.async { anchor.sync() }
    }
}

/// The share sheet as UIKit presented it: a UIActivityViewController with the
/// two items the tag has always shared (its title line and its link), anchored to
/// the Share button. ShareLink carries only one item plus an optional message,
/// which many activities drop.
struct TMShareSheet: UIViewRepresentable {
    @Binding var isPresented: Bool
    let items: [Any]

    final class Coordinator: NSObject, UIPopoverPresentationControllerDelegate {
        var dismissed: () -> Void = {}
        func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
            dismissed()
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> TMPresentingAnchor { TMPresentingAnchor() }

    func updateUIView(_ anchor: TMPresentingAnchor, context: Context) {
        let binding = $isPresented
        let coordinator = context.coordinator
        coordinator.dismissed = { [weak anchor] in
            anchor?.forget()
            binding.wrappedValue = false
        }
        let items = items
        anchor.alreadyUp = { $0 is UIActivityViewController }
        anchor.make = { anchor in
            let controller = UIActivityViewController(activityItems: items, applicationActivities: nil)
            controller.completionWithItemsHandler = { _, _, _, _ in
                anchor.forget()
                binding.wrappedValue = false
            }
            controller.popoverPresentationController?.delegate = coordinator
            controller.popoverPresentationController?.sourceView = anchor
            controller.popoverPresentationController?.sourceRect = anchor.bounds
            return controller
        }
        anchor.wanted = isPresented
        DispatchQueue.main.async { anchor.sync() }
    }
}
