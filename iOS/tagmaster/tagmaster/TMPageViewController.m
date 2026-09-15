#import "TMPageViewController.h"
#import "DPAppDelegate.h"

@interface TMPageViewController ()
@property (nonatomic, strong) UITabBarController *pageTabController;
@property BOOL appeared;
@end
@implementation TMPageViewController
- (UITabBarController *)pageTabController {
    if (!_pageTabController) {
        _pageTabController = [UITabBarController new];
        if (@available(iOS 18.0, *)) {
            _pageTabController.mode = UITabBarControllerModeTabBar;
            // Only the tab container is compact. The host/split view and each
            // content page retain their actual column's horizontal size class.
            _pageTabController.traitOverrides.horizontalSizeClass = UIUserInterfaceSizeClassCompact;
        }
        if (@available(iOS 26.0, *)) {
            _pageTabController.tabBarMinimizeBehavior = UITabBarMinimizeBehaviorNever;
        }
    }
    return _pageTabController;
}
- (UITabBar *)tabBar { return self.pageTabController.tabBar; }
- (UIView *)rootView { return self.pageTabController.view; }
- (NSArray<UIViewController *> *)viewControllers { return self.pageTabController.viewControllers; }
- (NSUInteger)selectedIndex { return self.pageTabController.selectedIndex; }
- (void)setSelectedIndex:(NSUInteger)index {
    if (index < self.viewControllers.count) self.pageTabController.selectedIndex = index;
}
- (void)setViewControllers:(NSArray<UIViewController *> *)controllers {
    UIViewController *selected = self.pageTabController.selectedViewController;
    for (UIViewController *page in controllers) {
        page.tabBarItem.accessibilityIdentifier = [@"page-" stringByAppendingString:page.tabBarItem.title ?: @""];
        if (@available(iOS 18.0, *)) {
            page.traitOverrides.horizontalSizeClass = self.traitCollection.horizontalSizeClass;
        }
    }
    self.pageTabController.viewControllers = controllers;
    self.pageTabController.customizableViewControllers = @[];
    if ([controllers containsObject:selected]) self.pageTabController.selectedViewController = selected;
}
- (void)viewDidLoad {
    [super viewDidLoad];
    UITabBarController *tabs = self.pageTabController;
    [self addChildViewController:tabs];
    UIView *content = tabs.view;
    content.backgroundColor = UIColor.clearColor;
    content.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:content];
    [NSLayoutConstraint activateConstraints:@[
        // Split navigation views can extend underneath the neighboring column.
        // Their horizontal safe area, not their bounds, owns this page bar.
        [content.leadingAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.leadingAnchor],
        [content.trailingAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.trailingAnchor],
        [content.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [content.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
    ]];
    [tabs didMoveToParentViewController:self];
    tabs.tabBar.accessibilityIdentifier = @"page-tab-bar";
    tabs.tabBar.tintColor = [DPAppDelegate accentColor];
    // No appearance/background override: UIKit supplies Liquid Glass on 26
    // and its native tab bar material on 17–25, including accessibility policy.
}
- (void)viewWillLayoutSubviews {
    [super viewWillLayoutSubviews];
    if (@available(iOS 18.0, *)) {
        for (UIViewController *page in self.viewControllers) {
            page.traitOverrides.horizontalSizeClass = self.traitCollection.horizontalSizeClass;
        }
    }
}
- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    self.appeared = YES;
}
- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    self.appeared = NO;
}
@end
