import UIKit

/// One familiar Home → catalog → tag stack, also used by deep links.
@objc(TMRootController)
final class TMRootController: UINavigationController {
    @objc var homeNavigationController: UINavigationController! { self }

    @objc static func make() -> TMRootController {
        let root = TMRootController(rootViewController: DPHomeViewController())
        root.navigationBar.prefersLargeTitles = false
        root.navigationBar.tintColor = TMTheme.tint
        return root
    }

    @objc @discardableResult
    func focusHome() -> UINavigationController { self }

    @objc func focusSearch() {
        pushViewController(DPSearchViewController(), animated: true)
    }
}
