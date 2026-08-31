import Foundation
import SwiftUI
import UIKit
import Firebase

@objc public class DPRootViewBridge: NSObject {
    @objc public static func rootViewController() -> UIViewController {
        let rootView = MainTabView()
        let hostingController = UIHostingController(rootView: rootView)
        hostingController.view.backgroundColor = .systemBackground
        return hostingController
    }
}
