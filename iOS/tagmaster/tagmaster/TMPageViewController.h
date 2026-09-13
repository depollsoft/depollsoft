#import <UIKit/UIKit.h>

@class TMPageSwitcher;
@protocol TMPageSwitcherDelegate <NSObject>
- (void)tabBar:(TMPageSwitcher *)tabBar didSelectItem:(UITabBarItem *)item;
@end

// In-screen page controls, not application navigation. The familiar item/delegate
// contract remains available, but these buttons are the only rendered controls.
@interface TMPageSwitcher : UIView
@property (nonatomic, weak) id<TMPageSwitcherDelegate> delegate;
@property (nonatomic, copy) NSArray<UITabBarItem *> *items;
@property (nonatomic, strong) UITabBarItem *selectedItem;
@property (nonatomic, readonly) NSArray<UIButton *> *buttons;
- (CGFloat)heightForWidth:(CGFloat)width;
@end

@interface TMPageViewController : UIViewController
@property (nonatomic, readonly) TMPageSwitcher *tabBar;
@property (nonatomic, readonly) UIView *rootView;
@property (nonatomic, copy) NSArray<UIViewController *> *viewControllers;
@property (nonatomic) NSUInteger selectedIndex;
@end
