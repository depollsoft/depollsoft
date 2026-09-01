//
//  DPTheme.swift
//  pitchperfect
//
//  The Laboratory Instrument palette: grayscale only, one luminous value.
//

import Foundation
import UIKit

@objc public class DPTheme: NSObject {
    private static func dyn(_ light: UIColor, _ dark: UIColor) -> UIColor {
        UIColor { traits in traits.userInterfaceStyle == .dark ? dark : light }
    }

    private static func rgb(_ value: UInt32) -> UIColor {
        UIColor(
            red: CGFloat((value >> 16) & 0xFF) / 255.0,
            green: CGFloat((value >> 8) & 0xFF) / 255.0,
            blue: CGFloat(value & 0xFF) / 255.0,
            alpha: 1,
        )
    }

    @objc public static let plateGround = dyn(rgb(0xDADBDC), rgb(0x0E0F10))
    @objc public static let plateSurface = dyn(rgb(0xE7E8E9), rgb(0x16181A))
    @objc public static let plateRow = dyn(
        rgb(0xDADBDC).withAlphaComponent(0.95),
        rgb(0x0E0F10).withAlphaComponent(0.95)
    )
    @objc public static let plateInk = dyn(rgb(0x1C1E20), rgb(0xD9DBDD))
    @objc public static let plateInkSecondary = dyn(rgb(0x55585C), rgb(0x898D92))
    @objc public static let plateHairline = dyn(rgb(0xB7B9BC), rgb(0x2C2F33))
    @objc public static let plateLit = dyn(rgb(0x141618), rgb(0xF2EFE6))
    @objc public static let plateOnLit = dyn(rgb(0xF2F3F4), rgb(0x101214))

    private static func staffTile(dark: Bool) -> UIImage {
        let size = CGSize(width: 430, height: 239)
        let markColor = dark ? rgb(0x898D92) : rgb(0x55585C)
        let ground = dark ? rgb(0x0E0F10) : rgb(0xDADBDC)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { context in
            ground.setFill()
            context.fill(CGRect(origin: .zero, size: size))
            guard let artwork = UIImage(named: "panobackground.png") else { return }
            artwork.withTintColor(markColor, renderingMode: .alwaysOriginal)
                .draw(in: CGRect(origin: .zero, size: size), blendMode: .normal, alpha: 0.10)
        }
    }

    /// The etched-staff panel: the app's heritage staff background as engraving.
    @objc public static func staffBackgroundColor() -> UIColor {
        UIColor { traits in
            UIColor(patternImage: staffTile(dark: traits.userInterfaceStyle == .dark))
        }
    }

    private static let themeKey = "depollsoft.pitchperfect.theme"

    @objc public static var storedTheme: Int {
        get { UserDefaults.standard.integer(forKey: themeKey) }
        set {
            UserDefaults.standard.set(newValue, forKey: themeKey)
            applyStoredAppearance()
        }
    }

    @objc public static func applyStoredAppearance() {
        let style: UIUserInterfaceStyle
        switch storedTheme {
        case 1: style = .light
        case 2: style = .dark
        default: style = .unspecified
        }
        for scene in UIApplication.shared.connectedScenes {
            guard let windowScene = scene as? UIWindowScene else { continue }
            for window in windowScene.windows {
                window.overrideUserInterfaceStyle = style
            }
        }
        for window in UIApplication.shared.windows {
            window.overrideUserInterfaceStyle = style
        }
    }
}
