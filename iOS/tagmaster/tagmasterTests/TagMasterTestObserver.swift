//
//  TagMasterTestObserver.swift
//  tagmasterTests
//
//  The bundle's principal class: runs once before any test.
//

import Foundation
@testable import tagmaster

@objc(TagMasterTestObserver)
final class TagMasterTestObserver: NSObject {
    override init() {
        super.init()
        // No test may start the audio hardware; see TMBalanceAudioPlayer.usesAudioHardware.
        TMBalanceAudioPlayer.usesAudioHardware = false
    }
}
