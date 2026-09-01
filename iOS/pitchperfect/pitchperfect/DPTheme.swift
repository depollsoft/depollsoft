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
    @objc public static let plateInk = dyn(rgb(0x1C1E20), rgb(0xD9DBDD))
    @objc public static let plateInkSecondary = dyn(rgb(0x55585C), rgb(0x898D92))
    @objc public static let plateHairline = dyn(rgb(0xB7B9BC), rgb(0x2C2F33))
    @objc public static let plateLit = dyn(rgb(0x141618), rgb(0xF2EFE6))
    @objc public static let plateOnLit = dyn(rgb(0xF2F3F4), rgb(0x101214))

    private static func staffTile(dark: Bool) -> UIImage {
        let size = CGSize(width: 540, height: 600)
        let lineColor = dark ? rgb(0x1F2225) : rgb(0xC7C9CB)
        let markColor = dark ? rgb(0x898D92) : rgb(0x55585C)
        let ground = dark ? rgb(0x0E0F10) : rgb(0xDADBDC)
        let renderer = UIGraphicsImageRenderer(size: size)
        return renderer.image { context in
            ground.setFill()
            context.fill(CGRect(origin: .zero, size: size))

            lineColor.setStroke()
            let path = UIBezierPath()
            path.lineWidth = 0.7
            for groupTop in [70.0, 370.0] {
                for line in 0..<5 {
                    let lineY = groupTop + CGFloat(line) * 10.0
                    path.move(to: CGPoint(x: 0, y: lineY))
                    path.addLine(to: CGPoint(x: size.width, y: lineY))
                }
            }
            path.stroke()

            guard let artwork = UIImage(named: "panobackground.png"),
                  let image = artwork.cgImage else { return }
            let trebleRect = CGRect(x: 0, y: 0, width: image.width / 3, height: image.height / 2)
            let bassRect = CGRect(x: 0, y: image.height / 2, width: image.width / 3, height: image.height / 2)
            if let crop = image.cropping(to: trebleRect) {
                UIImage(cgImage: crop)
                    .withTintColor(markColor, renderingMode: .alwaysOriginal)
                    .draw(in: CGRect(x: 18, y: 25, width: 220, height: 128), blendMode: .normal, alpha: 0.11)
            }
            if let crop = image.cropping(to: bassRect) {
                UIImage(cgImage: crop)
                    .withTintColor(markColor, renderingMode: .alwaysOriginal)
                    .draw(in: CGRect(x: 302, y: 355, width: 220, height: 128), blendMode: .normal, alpha: 0.11)
            }
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
