#import <UIKit/UIKit.h>

// App-local host. UIKit owns the visible bar, selection and page lifecycle.
@interface TMPageViewController : UIViewController
@property (nonatomic, readonly) UITabBarController *pageTabController;
@property (nonatomic, readonly) UITabBar *tabBar;
// The whole native container, so initial loading hides every page and the bar.
@property (nonatomic, readonly) UIView *rootView;
@property (nonatomic, copy) NSArray<UIViewController *> *viewControllers;
@property (nonatomic) NSUInteger selectedIndex;
@end
