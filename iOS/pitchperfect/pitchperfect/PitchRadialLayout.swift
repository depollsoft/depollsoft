//
//  PitchRadialLayout.swift
//  pitchperfect
//
//  Places subviews around the instrument ring. Shared by the home-screen
//  widget and the song editor's key dial.
//

import SwiftUI

enum RadialOrigin {
    /// Two cells straddle twelve o'clock, so an octave's root and its repeat
    /// sit side by side at the top, as on the pitch pipe face.
    case straddlingTop
    /// The middle cell sits exactly at twelve o'clock; with an odd count the
    /// two ends meet at six o'clock, as the enharmonic keys do.
    case middleAtTop
}

struct RadialLayout: Layout {
    let radius: CGFloat
    let cellDiameter: CGFloat
    var origin: RadialOrigin = .straddlingTop

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let side = (radius + cellDiameter / 2) * 2
        return CGSize(width: side, height: side)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard !subviews.isEmpty else { return }
        let step = 2 * CGFloat.pi / CGFloat(subviews.count)
        let start: CGFloat
        switch origin {
        case .straddlingTop:
            start = -CGFloat.pi / 2 + step / 2
        case .middleAtTop:
            start = -CGFloat.pi / 2 - CGFloat((subviews.count - 1) / 2) * step
        }
        let center = CGPoint(x: bounds.midX, y: bounds.midY)
        let cell = ProposedViewSize(width: cellDiameter, height: cellDiameter)
        for (index, subview) in subviews.enumerated() {
            let angle = start + CGFloat(index) * step
            let point = CGPoint(
                x: center.x + cos(angle) * radius,
                y: center.y + sin(angle) * radius
            )
            subview.place(at: point, anchor: .center, proposal: cell)
        }
    }
}
