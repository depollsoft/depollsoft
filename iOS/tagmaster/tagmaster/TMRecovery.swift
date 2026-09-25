//
//  TMRecovery.swift
//  tagmaster
//
//  The one recovery alert every screen shows when a request fails: what went
//  wrong and, when trying again makes sense, Retry.
//

import SwiftUI

/// A recoverable failure: what went wrong and, when it makes sense, how to try again.
struct TMRecovery: Identifiable {
    static let title = "Couldn't complete request"

    let id = UUID()
    let message: String
    let retry: (() -> Void)?
}

extension View {
    /// "Couldn't complete request", with Retry when the failure can be retried.
    func tmRecoveryAlert(_ recovery: Binding<TMRecovery?>) -> some View {
        alert(TMRecovery.title,
              isPresented: Binding(get: { recovery.wrappedValue != nil },
                                   set: { if !$0 { recovery.wrappedValue = nil } }),
              presenting: recovery.wrappedValue) { shown in
            if let retry = shown.retry {
                Button("Retry") {
                    recovery.wrappedValue = nil
                    retry()
                }
            }
            Button("Cancel", role: .cancel) { recovery.wrappedValue = nil }
        } message: { shown in
            Text(shown.message)
        }
    }
}
