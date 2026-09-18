#import "TMTestAppDelegate.h"
#import "DPAppDelegate.h"

@implementation TMTestAppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    [DPAppDelegate configureCacheSerialization];
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    self.window.rootViewController = [UIViewController new];
    self.window.hidden = YES;
    return YES;
}

@end
