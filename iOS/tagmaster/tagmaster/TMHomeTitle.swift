//
//  TMHomeTitle.swift
//  tagmaster
//
//  Home's title in the Wickhop handwriting face: large at the top of the app,
//  and the same face inline once the bar collapses. The face belongs to Home
//  only, so the bar goes back to the system title when Home leaves.
//

import UIKit

@MainActor
final class TMHomeTitle {
    /// The inline title. UIKit clips Wickhop in the standard title label, so Home
    /// supplies a full-height label of its own and shows it only once collapsed.
    let inlineLabel = UILabel()
    private var barObservation: NSKeyValueObservation?

    static func handwritingFont(_ style: UIFont.TextStyle, size: CGFloat, maximum: CGFloat) -> UIFont {
        let base = UIFont(name: "wickhop handwriting", size: size) ?? .preferredFont(forTextStyle: style)
        return UIFontMetrics(forTextStyle: style).scaledFont(for: base, maximumPointSize: maximum)
    }

    init() {
        inlineLabel.textAlignment = .center
        inlineLabel.accessibilityTraits = .header
    }

    func attach(to controller: UIViewController) {
        guard let bar = controller.navigationController?.navigationBar else { return }
        bar.prefersLargeTitles = true
        let largeFont = TMHomeTitle.handwritingFont(.largeTitle, size: 34, maximum: 44)
        let inlineFont = TMHomeTitle.handwritingFont(.headline, size: 22, maximum: 26)
        // Wickhop's descender extends below UIKit's title box. Lift the glyphs, not the bar.
        let appearance = bar.standardAppearance.copy()
        appearance.largeTitleTextAttributes = [
            .font: largeFont,
            .baselineOffset: 8 * largeFont.pointSize / 34,
            .foregroundColor: UIColor.white,
        ]
        appearance.titleTextAttributes = [
            .font: inlineFont,
            .baselineOffset: 6 * inlineFont.pointSize / 22,
            .foregroundColor: UIColor.white,
        ]
        TMHomeTitle.apply(appearance, to: bar)
        inlineLabel.attributedText = NSAttributedString(string: "Tag Master", attributes: appearance.titleTextAttributes)
        inlineLabel.sizeToFit()
        inlineLabel.frame = CGRect(x: 0, y: 0, width: inlineLabel.bounds.width, height: 44)
        updateVisibility(bar)
        barObservation = bar.observe(\.bounds, options: [.new]) { [weak self] bar, _ in
            MainActor.assumeIsolated { self?.updateVisibility(bar) }
        }
    }

    func detach(from controller: UIViewController) {
        barObservation = nil
        guard let bar = controller.navigationController?.navigationBar else { return }
        let appearance = bar.standardAppearance.copy()
        appearance.titleTextAttributes = [.foregroundColor: UIColor.white]
        appearance.largeTitleTextAttributes = [.foregroundColor: UIColor.white]
        TMHomeTitle.apply(appearance, to: bar)
    }

    /// UIKit does not fade a custom title view with its large title; show ours
    /// only once the bar has collapsed to its standard height.
    private func updateVisibility(_ bar: UINavigationBar) {
        let hidden = bar.bounds.height > 64
        if inlineLabel.isHidden != hidden { inlineLabel.isHidden = hidden }
    }

    private static func apply(_ appearance: UINavigationBarAppearance, to bar: UINavigationBar) {
        bar.standardAppearance = appearance
        bar.scrollEdgeAppearance = appearance
        bar.compactAppearance = appearance
        bar.compactScrollEdgeAppearance = appearance
    }
}
