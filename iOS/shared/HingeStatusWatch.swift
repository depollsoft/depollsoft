//
//  HingeStatusWatch.swift
//  Pitch Perfect and Tag Master
//
//  An iPhone Duo's fold changes its reserved region, not the window's size, so a
//  view laying itself out around the fold has to be told to look again. Both apps
//  read the hinge's state here.
//

import SwiftUI

extension View {
    /// Keeps `status` at the hinge's state (1 closed, 2 half open, 3 open; 0 before
    /// the first report and on devices without one). A view that reads the fold from
    /// `reservedRegions` reads `status` too, so it is laid out again as the device
    /// folds and opens. It changes with the state, not with every degree the hinge turns.
    func hingeStatus(_ status: Binding<Int>) -> some View {
        modifier(HingeStatusWatch(status: status))
    }
}

private struct HingeStatusWatch: ViewModifier {
    @Binding var status: Int

    func body(content: Content) -> some View {
        if #available(iOS 27.1, *) {
            content.onHingeChange { _, context in
                let now: Int
                switch context.hinge?.status {
                case .closed?: now = 1
                case .partiallyOpen?: now = 2
                case .fullyOpen?: now = 3
                default: now = 0
                }
                if now != status { status = now }
            }
        } else {
            content
        }
    }
}
