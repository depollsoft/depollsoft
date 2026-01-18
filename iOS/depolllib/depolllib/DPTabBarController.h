#if __has_include(<UIKit/UIKit.h>)
#import <UIKit/UIKit.h>

@interface DPTabBarController : UIViewController

@property (nonatomic, readonly, strong) UITabBar *tabBar;
@property (nonatomic, copy) NSArray *viewControllers;

@end

#else
// UIKit not available on this platform; provide a minimal placeholder to satisfy references.
#import <Foundation/Foundation.h>

@interface DPTabBarController : NSObject
@end

#endif
