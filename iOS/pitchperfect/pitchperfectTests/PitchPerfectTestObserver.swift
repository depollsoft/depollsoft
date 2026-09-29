//
//  PitchPerfectTestObserver.swift
//  pitchperfectTests
//
//  The bundle's principal class: runs once before any test.
//

import Foundation
import SwiftUI
import XCTest
@testable import pitchperfect

@objc(PitchPerfectTestObserver)
final class PitchPerfectTestObserver: NSObject, XCTestObservation {
    override init() {
        super.init()
        XCTestObservationCenter.shared.addTestObserver(self)
    }

    func testBundleWillStart(_ testBundle: Bundle) {
        // No test may start the audio hardware: wiring AVAudioEngine's output
        // has deadlocked hosted runs. Instruments get no engine here.
        MIDINotePlayer.usesAudioHardware = false
        // The first Canvas and alert pay their one-off rendering cost here, not in a test; see TestRendering.
        MainActor.assumeIsolated {
            TestRendering.warmUp([AnyView(PitchInstrumentView(model: PitchPipeModel()).frame(width: 360, height: 360))])
        }
    }
}
