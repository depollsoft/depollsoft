//
//  TMTagPlaceholder.swift
//  tagmaster
//
//  The detail column before a tag is chosen on iPad: the quartet staff at
//  rest over the app's barber-pole background, and a word on what goes here.
//

import SwiftUI

struct TMTagPlaceholder: View {
    var body: some View {
        VStack(spacing: 0) {
            TMQuartetStaff(animating: false)
                .padding(.bottom, 24)
            VStack(spacing: 8) {
                Text("Pick a tag")
                    .font(.title2.weight(.semibold))
                    .foregroundStyle(Color(.label))
                    .accessibilityAddTraits(.isHeader)
                    .frame(maxWidth: 480)
                Text("Choose a tag from the list. Its summary, tracks, sheet music, and videos open here.")
                    .font(.body)
                    .foregroundStyle(Color(.secondaryLabel))
                    .frame(maxWidth: 480)
            }
            .multilineTextAlignment(.center)
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// The placeholder on the UIKit split, over the shared (or its own) watermark.
@objc(TMTagPlaceholderController)
final class TMTagPlaceholderController: UIViewController {
    private let hosting = UIHostingController(rootView: TMTagPlaceholder())

    override func viewDidLoad() {
        super.viewDidLoad()
        DPAppDelegate.setUpBackground(view)
        addChild(hosting)
        hosting.view.backgroundColor = .clear
        hosting.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(hosting.view)
        NSLayoutConstraint.activate([
            hosting.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            hosting.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            hosting.view.topAnchor.constraint(equalTo: view.topAnchor),
            hosting.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        hosting.didMove(toParent: self)
    }
}
