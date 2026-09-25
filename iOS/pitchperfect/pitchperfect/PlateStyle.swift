//
//  PlateStyle.swift
//  pitchperfect
//
//  The Laboratory Instrument palette and faces for SwiftUI, the etched-staff
//  background every screen sits on, and the bar chrome the tabs share. Colours
//  and fonts are the very UIColor/UIFont values DPTheme defines, bridged, so
//  SwiftUI text measures and renders exactly as the UIKit screens did.
//

import SwiftUI
import UIKit

enum Plate {
    static let ground = Color(DPTheme.plateGround)
    static let surface = Color(DPTheme.plateSurface)
    static let ink = Color(DPTheme.plateInk)
    static let inkSecondary = Color(DPTheme.plateInkSecondary)
    static let hairline = Color(DPTheme.plateHairline)
    static let lit = Color(DPTheme.plateLit)
    static let onLit = Color(DPTheme.plateOnLit)

    /// The engraved display face (Oswald).
    static func display(_ size: CGFloat) -> Font { Font(DPTheme.condensedFont(size: size) as CTFont) }
    /// The condensed text face for list rows.
    static func text(_ size: CGFloat) -> Font { Font(DPTheme.listTitleFont(size: size) as CTFont) }
    /// The measurement face.
    static func mono(_ size: CGFloat) -> Font { Font(DPTheme.monospacedFont(size: size) as CTFont) }
    static func music(_ size: CGFloat) -> Font { Font.custom("MusiQwik", fixedSize: size) }
    static func noteHedz(_ size: CGFloat) -> Font { Font.custom("NoteHedz", fixedSize: size) }

    /// NoteHedz's sharp and flat glyphs.
    static let sharpGlyph = "\u{00EC}"
    static let flatGlyph = "\u{00ED}"

    static func glyph(for accidental: Int) -> String? {
        switch accidental {
        case Int(Sharp.rawValue): return sharpGlyph
        case Int(Flat.rawValue): return flatGlyph
        default: return nil
        }
    }
}

/// The etched-staff tile, tiled from the view's own origin (shifted down by
/// `originY`) exactly as a UIKit pattern colour tiles from its layer's origin.
struct StaffBackground: View {
    var originY: CGFloat = 0
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let tile = DPTheme.staffTileImage(dark: colorScheme == .dark)
        let height = tile.size.height
        let phase = originY.truncatingRemainder(dividingBy: height)
        GeometryReader { proxy in
            Image(uiImage: tile)
                .resizable(resizingMode: .tile)
                .frame(width: proxy.size.width, height: proxy.size.height + height)
                .offset(y: phase > 0 ? phase - height : phase)
        }
        .clipped()
        .accessibilityHidden(true)
    }
}

extension View {
    /// The full-bleed staff surface a screen sits on, running under the bars.
    /// UIKit drew it as a non-scrolling scroll view's pattern colour, which
    /// starts where the scroll view's content does: below the top bars.
    func staffScreenBackground() -> some View {
        background {
            GeometryReader { content in
                let contentTop = content.frame(in: .global).minY
                GeometryReader { full in
                    StaffBackground(originY: contentTop - full.frame(in: .global).minY)
                }
                .ignoresSafeArea()
            }
        }
    }
}

// MARK: - Bar chrome

/// Sets the hosting navigation bar's title face: Oswald for the instrument's own
/// screens, the system face for the sheets that never carried it.
private struct NavigationTitleFace: UIViewControllerRepresentable {
    let oswald: Bool

    final class Controller: UIViewController {
        var oswald = true
        override func didMove(toParent parent: UIViewController?) {
            super.didMove(toParent: parent)
            apply()
        }
        override func viewWillAppear(_ animated: Bool) {
            super.viewWillAppear(animated)
            apply()
        }
        func apply() {
            guard let bar = navigationController?.navigationBar else { return }
            bar.tintColor = .label
            guard oswald else { return }
            bar.titleTextAttributes = [
                .foregroundColor: UIColor.label,
                .font: UIFont(name: "Oswald-Medium", size: 19) ?? UIFont.preferredFont(forTextStyle: .headline),
            ]
        }
    }

    func makeUIViewController(context: Context) -> Controller {
        let controller = Controller()
        controller.oswald = oswald
        controller.view.isHidden = true
        return controller
    }

    func updateUIViewController(_ controller: Controller, context: Context) {
        controller.oswald = oswald
        controller.apply()
    }
}

extension View {
    /// The instrument chrome: an Oswald title and label-tinted bar buttons.
    func instrumentChrome(oswaldTitle: Bool = true) -> some View {
        background(NavigationTitleFace(oswald: oswaldTitle).frame(width: 0, height: 0))
            .tint(Color(uiColor: .label))
    }
}

/// A bar button drawn from an SF Symbol at the shared bar configuration, named
/// and identified the way the UIKit items were (see DPCommon).
struct BarSymbolButton: View {
    let systemName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            BarSymbol(systemName: systemName)
        }
        .accessibilityLabel(DPCommon.accessibilityLabel(forSymbol: systemName))
        .accessibilityIdentifier(systemName)
    }
}

/// The same bar symbol, opening a menu.
struct BarSymbolMenu<Content: View>: View {
    let systemName: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        Menu(content: content) {
            BarSymbol(systemName: systemName)
        }
        .accessibilityLabel(DPCommon.accessibilityLabel(forSymbol: systemName))
        .accessibilityIdentifier(systemName)
    }
}

/// An SF Symbol at the UIKit bar-button configuration: 17 pt, regular, medium
/// scale. (SwiftUI's own toolbar symbols come out a size larger.)
struct BarSymbol: View {
    let systemName: String

    private static let configuration = UIImage.SymbolConfiguration(pointSize: 17, weight: .regular, scale: .medium)

    /// The same symbol as a UIKit bar item, named and identified the same way.
    static func item(systemName: String, target: Any, action: Selector) -> UIBarButtonItem {
        let item = UIBarButtonItem(image: UIImage(systemName: systemName, withConfiguration: configuration),
                                   style: .plain, target: target, action: action)
        item.accessibilityIdentifier = systemName
        item.accessibilityLabel = DPCommon.accessibilityLabel(forSymbol: systemName)
        return item
    }

    var body: some View {
        Image(uiImage: UIImage(systemName: systemName, withConfiguration: Self.configuration) ?? UIImage())
            .renderingMode(.template)
    }
}
