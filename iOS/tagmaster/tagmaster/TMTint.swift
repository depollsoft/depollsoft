//
//  TMTint.swift
//  tagmaster
//
//  Tag Master's accent, as UIKit resolves it where a view sits. While a sheet,
//  popover or alert is up UIKit dims every tint behind it to gray; SwiftUI
//  colours do not follow. A probe view reads the live UIKit tint (dimmed or not)
//  and hands it to the SwiftUI views below as `\.tmAccent`, so accented text,
//  outlines and fills dim exactly as the UIKit screens' did.
//

import SwiftUI

private struct TMAccentKey: EnvironmentKey {
    static let defaultValue = Color(DPAppDelegate.accentColor() ?? .tintColor)
}

extension EnvironmentValues {
    /// The accent where this view sits: gray while something is presented over it.
    var tmAccent: Color {
        get { self[TMAccentKey.self] }
        set { self[TMAccentKey.self] = newValue }
    }
}

extension View {
    /// Resolves `\.tmAccent` and `\.tmTintDimmed` from the UIKit tint at this point.
    /// Callers apply them where they want a tint; the navigation bar keeps its own
    /// white. Every route installs it once (TMShell), so any screen can read them.
    func tmFollowsUIKitTint() -> some View {
        modifier(TMTintFollower())
    }
}

private struct TMTintFollower: ViewModifier {
    @State private var tint: UIColor?
    @State private var dimmed = false

    func body(content: Content) -> some View {
        let accent = Color(tint ?? DPAppDelegate.accentColor() ?? .tintColor)
        content
            .environment(\.tmAccent, accent)
            .environment(\.tmTintDimmed, dimmed)
            .background(TMTintProbe { color, isDimmed in
                if color != tint { tint = color }
                if isDimmed != dimmed { dimmed = isDimmed }
            }.accessibilityHidden(true))
    }
}

/// An invisible view that reports its tint, and whether UIKit is dimming it,
/// whenever UIKit changes either.
private struct TMTintProbe: UIViewRepresentable {
    let changed: (UIColor, Bool) -> Void

    final class ProbeView: UIView {
        var changed: ((UIColor, Bool) -> Void)?

        override func tintColorDidChange() {
            super.tintColorDidChange()
            report()
        }

        override func didMoveToWindow() {
            super.didMoveToWindow()
            report()
        }

        func report() {
            guard window != nil, let changed else { return }
            let color = tintColor ?? .tintColor
            let dimmed = tintAdjustmentMode == .dimmed
            DispatchQueue.main.async { changed(color, dimmed) }
        }
    }

    func makeUIView(context: Context) -> ProbeView {
        let view = ProbeView()
        view.isUserInteractionEnabled = false
        view.isAccessibilityElement = false
        view.changed = changed
        return view
    }

    func updateUIView(_ view: ProbeView, context: Context) {
        view.changed = changed
    }
}
