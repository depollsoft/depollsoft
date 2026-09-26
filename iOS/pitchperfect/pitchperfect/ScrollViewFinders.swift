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

/// Finds the scroll view of the List this sits beside (as the List's background)
/// and hands it over, independent of which of the List's rows exist yet.
struct EnclosedListFinder: UIViewRepresentable {
    let found: (UIScrollView) -> Void

    final class Finder: UIView {
        var found: (UIScrollView) -> Void = { _ in }
        private weak var reported: UIScrollView?

        override func didMoveToWindow() {
            super.didMoveToWindow()
            find()
        }

        override func layoutSubviews() {
            super.layoutSubviews()
            find()
        }

        /// Looks for the List; it may build its scroll view a moment after this
        /// joins the window, so a miss looks again on the next few turns.
        private func find(attempt: Int = 0) {
            guard window != nil else { return }
            var ancestor = superview
            for _ in 0..<6 {
                guard let current = ancestor else { break }
                if let list = Self.firstCollectionView(in: current) {
                    if list !== reported {
                        reported = list
                        found(list)
                    }
                    return
                }
                ancestor = current.superview
            }
            guard attempt < 20 else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.05) { [weak self] in self?.find(attempt: attempt + 1) }
        }

        private static func firstCollectionView(in view: UIView) -> UICollectionView? {
            var queue = [view]
            while !queue.isEmpty {
                let next = queue.removeFirst()
                if let list = next as? UICollectionView { return list }
                queue.append(contentsOf: next.subviews)
            }
            return nil
        }
    }

    func makeUIView(context: Context) -> Finder {
        let finder = Finder()
        finder.isUserInteractionEnabled = false
        finder.isAccessibilityElement = false
        return finder
    }

    func updateUIView(_ finder: Finder, context: Context) { finder.found = found }
}
