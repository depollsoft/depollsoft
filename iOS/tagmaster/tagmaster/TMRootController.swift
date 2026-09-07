//
//  TMRootController.swift
//  tagmaster
//
//  The singing desk's three destinations. Home is where a group starts, Browse
//  is the catalog by collection, Search is the query desk. Settings stays a
//  utility in Home's navigation bar rather than taking a destination slot.
//
//  On iPhone this is the standard tab bar. On iPad (and any regular-width
//  window, including Split View and Stage Manager) it becomes a persistent
//  sidebar so navigation never disappears behind a tap.
//

import UIKit

@objc(TMRootController)
final class TMRootController: UITabBarController {

    /// The stack deep links and programmatic pushes travel through.
    @objc private(set) var homeNavigationController: UINavigationController!

    @objc static func make() -> TMRootController {
        let root = TMRootController()
        root.build()
        return root
    }

    private func build() {
        let home = DPHomeViewController()
        let homeNav = UINavigationController(rootViewController: home)
        homeNav.tabBarItem = UITabBarItem(
            title: "Home",
            image: UIImage(systemName: "music.mic"),
            selectedImage: UIImage(systemName: "music.mic"))
        homeNav.tabBarItem.accessibilityLabel = "Home"
        homeNavigationController = homeNav

        let browse = DPBrowseViewController()
        let browseNav = UINavigationController(rootViewController: browse)
        browseNav.tabBarItem = UITabBarItem(
            title: "Browse",
            image: UIImage(systemName: "square.stack"),
            selectedImage: UIImage(systemName: "square.stack.fill"))

        let search = DPSearchViewController()
        let searchNav = UINavigationController(rootViewController: search)
        searchNav.tabBarItem = UITabBarItem(
            title: "Search",
            image: UIImage(systemName: "magnifyingglass"),
            selectedImage: UIImage(systemName: "magnifyingglass"))

        for nav in [homeNav, browseNav, searchNav] {
            nav.navigationBar.prefersLargeTitles = true
            nav.navigationBar.tintColor = TMTheme.tint
        }

        viewControllers = [homeNav, browseNav, searchNav]

        // Regular-width windows keep navigation on screen rather than pinned to
        // the bottom edge of a large display.
        if #available(iOS 18.0, *) {
            mode = .tabSidebar
            sidebar.preferredLayout = .tile
            sidebar.isHidden = true
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad();
        view.backgroundColor = TMTheme.canvas
        if #available(iOS 18.0, *) {
            registerForTraitChanges([UITraitHorizontalSizeClass.self]) {
                (root: TMRootController, _) in
                root.updateSidebarLayout()
            }
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        updateSidebarLayout()
    }

    private func updateSidebarLayout() {
        if #available(iOS 18.0, *) {
            // Narrow windows retain native tabs instead of an overlay sidebar.
            let hidden = view.bounds.width < 1000 || traitCollection.horizontalSizeClass != .regular
            if sidebar.isHidden != hidden { sidebar.isHidden = hidden }
        }
    }

    /// Brings Home forward and returns the stack a pushed controller belongs in.
    @objc @discardableResult
    func focusHome() -> UINavigationController {
        selectedIndex = 0
        return homeNavigationController
    }

    /// Brings the Search destination forward — Home's "Find a tag" action.
    @objc func focusSearch() {
        let navigation = viewControllers?[2] as? UINavigationController
        navigation?.popToRootViewController(animated: false)
        selectedIndex = 2
    }
}
