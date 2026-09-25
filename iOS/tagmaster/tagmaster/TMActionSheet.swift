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

/// Put in the background of the control the sheet belongs to.
struct TMActionSheet: UIViewControllerRepresentable {
    @Binding var isPresented: Bool
    let actions: [TMSheetAction]
    var title: String?
    var message: String?

    final class Controller: UIViewController {
        weak var sheet: UIAlertController?
    }

    /// Hears a popover dismissed by a tap outside it, which runs no action.
    final class Coordinator: NSObject, UIPopoverPresentationControllerDelegate {
        var dismissed: () -> Void = {}
        func presentationControllerDidDismiss(_ presentationController: UIPresentationController) {
            dismissed()
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIViewController(context: Context) -> Controller {
        let controller = Controller()
        controller.view.isUserInteractionEnabled = false
        controller.view.backgroundColor = .clear
        return controller
    }

    func updateUIViewController(_ controller: Controller, context: Context) {
        if isPresented, controller.sheet == nil {
            let sheet = UIAlertController(title: title, message: message, preferredStyle: .actionSheet)
            for action in actions {
                let style: UIAlertAction.Style = switch action.style {
                case .normal: .default
                case .cancel: .cancel
                case .destructive: .destructive
                }
                sheet.addAction(UIAlertAction(title: action.title, style: style) { _ in
                    isPresented = false
                    action.handler()
                })
            }
            // iPhone shows the classic centred card; iPad needs a popover source.
            if controller.traitCollection.userInterfaceIdiom == .pad {
                context.coordinator.dismissed = { isPresented = false }
                sheet.popoverPresentationController?.delegate = context.coordinator
                sheet.popoverPresentationController?.sourceView = controller.view
                sheet.popoverPresentationController?.sourceRect = controller.view.bounds
            }
            controller.sheet = sheet
            DispatchQueue.main.async {
                guard controller.view.window != nil, controller.presentedViewController == nil else {
                    controller.sheet = nil
                    return
                }
                controller.present(sheet, animated: true)
            }
        } else if !isPresented, let sheet = controller.sheet {
            controller.sheet = nil
            if sheet.presentingViewController != nil { sheet.dismiss(animated: true) }
        }
    }
}
