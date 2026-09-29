//
//  TagMasterTestObserver.swift
//  tagmasterTests
//
//  The bundle's principal class: runs once before any test.
//

import Foundation
import SwiftUI
import XCTest
@testable import tagmaster

@objc(TagMasterTestObserver)
final class TagMasterTestObserver: NSObject, XCTestObservation {
    override init() {
        super.init()
        // No test may start the audio hardware; see TMBalanceAudioPlayer.usesAudioHardware.
        TMBalanceAudioPlayer.usesAudioHardware = false
        // Nor may an alert's text field summon the software keyboard; see TestKeyboard.
        MainActor.assumeIsolated { TestKeyboard.keepAlertFieldsFromRaisingTheKeyboard() }
        XCTestObservationCenter.shared.addTestObserver(self)
    }

    func testBundleWillStart(_ testBundle: Bundle) {
        // The first Canvas and alert pay their one-off rendering cost here, not in a test; see TestRendering.
        MainActor.assumeIsolated {
            TestRendering.warmUp([AnyView(TMBarberPole(compact: true)), AnyView(TMBarberPole(compact: false)),
                                  AnyView(TMBarberPole(compact: true, darkSurface: true))])
        }
    }
}
