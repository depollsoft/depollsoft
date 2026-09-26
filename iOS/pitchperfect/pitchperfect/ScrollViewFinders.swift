//
//  ScrollViewFinders.swift
//  pitchperfect
//
//  SwiftUI does not hand out the UIKit scroll views behind a List or ScrollView,
//  which the screens need to put a list back exactly where UIKit's table was
//  left and to give it UIKit's margins. These find them.
//

import SwiftUI
import UIKit

/// Finds the scroll view of `kind` this sits in (a List row's collection view, a
/// ScrollView's scroll view) and hands it to `found`: when it joins a window, and
/// again on every update, so what `found` applies follows the values it captured.
struct EnclosingScrollView: UIViewRepresentable {
    let kind: UIScrollView.Type
    let found: (UIScrollView) -> Void

    init(_ kind: UIScrollView.Type = UICollectionView.self, found: @escaping (UIScrollView) -> Void) {
        self.kind = kind
        self.found = found
    }

    final class Reporter: UIView {
        var kind: UIScrollView.Type = UICollectionView.self
        var found: (UIScrollView) -> Void = { _ in }
        private(set) weak var list: UIScrollView?

        override func didMoveToWindow() {
            super.didMoveToWindow()
            guard window != nil else { return }
            var view = superview
            while let current = view, !current.isKind(of: kind) { view = current.superview }
            list = view as? UIScrollView
            if let list { found(list) }
        }
    }

    func makeUIView(context: Context) -> Reporter {
        let reporter = Reporter()
        reporter.kind = kind
        reporter.isUserInteractionEnabled = false
        reporter.isAccessibilityElement = false
        return reporter
    }

    func updateUIView(_ reporter: Reporter, context: Context) {
        reporter.found = found
        if let list = reporter.list, list.window != nil { found(list) }
    }
}

/// Sets the layout margins of the List cell this sits in. A SwiftUI List gives
/// its cells margins of its own (16 or 20 pt, whatever the list's), and those
/// place the edit-mode delete and reorder controls; UIKit's table put them at
/// its margins. The reorder control sits 1.5 pt inside the cell's trailing margin.
struct ListCellMargins: UIViewRepresentable {
    let leading: CGFloat?
    let trailing: CGFloat

    /// A UITableView's: the delete control at `margin`, the reorder control ending `margin` from the edge.
    static func table(_ margin: CGFloat) -> ListCellMargins {
        ListCellMargins(leading: margin, trailing: margin + 1.5)
    }

    final class Setter: UIView {
        var leading: CGFloat?
        var trailing: CGFloat = 0

        override func didMoveToWindow() {
            super.didMoveToWindow()
            apply()
        }

        override func layoutSubviews() {
            super.layoutSubviews()
            apply()
        }

        func apply() {
            guard window != nil else { return }
            var view = superview
            while let current = view, !(current is UICollectionViewCell) { view = current.superview }
            guard let cell = view as? UICollectionViewCell else { return }
            var margins = cell.directionalLayoutMargins
            if let leading { margins.leading = leading }
            margins.trailing = trailing
            guard cell.preservesSuperviewLayoutMargins || cell.directionalLayoutMargins != margins else { return }
            cell.preservesSuperviewLayoutMargins = false
            cell.directionalLayoutMargins = margins
        }
    }

    func makeUIView(context: Context) -> Setter {
        let setter = Setter()
        setter.isUserInteractionEnabled = false
        setter.isAccessibilityElement = false
        return setter
    }

    func updateUIView(_ setter: Setter, context: Context) {
        setter.leading = leading
        setter.trailing = trailing
        setter.apply()
    }
}
