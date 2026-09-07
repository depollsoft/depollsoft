//
//  DPAppDelegate.m
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPAppDelegate.h"

@import FirebaseAuth;
@import FirebaseCore;

#if __has_include(<FBSDKCoreKit/FBSDKCoreKit.h>)
#import <FBSDKCoreKit/FBSDKCoreKit.h>
#define HAS_FBSDK 1
#else
#define HAS_FBSDK 0
#endif

#if __has_include(<GoogleSignIn/GoogleSignIn.h>)
#import <GoogleSignIn/GoogleSignIn.h>
#define HAS_GOOGLE_SIGN_IN 1
#else
#define HAS_GOOGLE_SIGN_IN 0
#endif

#import "DPBarbershop.h"
#import "DPHomeViewController.h"
#import "DPBrowseViewController.h"
#import "DPJsonSerializer.h"
#import "DPTagViewController.h"
#import "tagmaster-Swift.h"

@implementation DPAppDelegate

@synthesize window = _window;
@synthesize managedObjectContext = __managedObjectContext;
@synthesize managedObjectModel = __managedObjectModel;
@synthesize persistentStoreCoordinator = __persistentStoreCoordinator;
@synthesize navigationController;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    if (NSClassFromString(@"XCTestCase") != nil) {
        self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
        self.window.rootViewController = [UIViewController new];
        self.window.hidden = YES;
        return YES;
    }

    [DPAppLog start];
    [FIRApp configure];
#if HAS_FBSDK
    [[FBSDKApplicationDelegate sharedInstance] application:application
                             didFinishLaunchingWithOptions:launchOptions];
#endif
    [application registerForRemoteNotifications];
        
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    [DPJsonSerializer registerSerializer:^NSString *(NSURL *url) {
        return [url absoluteString];
    } deserializer:^NSURL *(NSString *input) {
        return [NSURL URLWithString:input];
    } forClass:[NSURL class]];
    
    NSNumberFormatter *numberFormatter = [[NSNumberFormatter alloc] init];
    [DPJsonSerializer registerSerializer:^NSString *(NSDate *date) {
        return [numberFormatter stringFromNumber:@([date timeIntervalSince1970])];
    } deserializer:^NSDate *(NSString *input) {
        return [NSDate dateWithTimeIntervalSince1970:[numberFormatter numberFromString:input].doubleValue];
    } forClass:[[NSDate date] class]];
    
    // Override point for customization after application launch.
    self.window.backgroundColor = [UIColor systemGroupedBackgroundColor];
    [TMTheme applyTo:self.window];

    TMRootController *root = [TMRootController make];
    self.window.rootViewController = root;
    self.rootController = root;
    navigationController = root.homeNavigationController;
    [self.window makeKeyAndVisible];
    
    [self extraInit];

    return YES;
}

- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {
#if HAS_GOOGLE_SIGN_IN
    if ([[GIDSignIn sharedInstance] handleURL:url]) {
        return YES;
    }
#endif
#if HAS_FBSDK
    if ([[FBSDKApplicationDelegate sharedInstance] application:app
                                                     openURL:url
                                                     options:options]) {
        return YES;
    }
#endif
    if ([[FIRAuth auth] canHandleURL:url]) {
        return YES;
    }
    if (url.pathComponents.count == 3 && [url.pathComponents[1] isEqualToString:@"tag"]) {
        NSString *tagNumberString = url.pathComponents[2];
        @try {
            int tagId = tagNumberString.intValue;
            DPTagViewController *controller = [[DPTagViewController alloc] init];
            controller.tagId = tagId;
            // Deep links always land in the Home stack, and bring it forward so
            // the pushed tag is the screen the singer is looking at.
            TMRootController *root = (TMRootController *)self.rootController;
            UINavigationController *stack = root ? [root focusHome] : self.navigationController;
            UIViewController *presenter = root ?: stack;
            void (^openTag)(void) = ^{ [stack pushViewController:controller animated:YES]; };
            if (presenter.presentedViewController) {
                [presenter dismissViewControllerAnimated:NO completion:openTag];
            } else {
                openTag();
            }
        }
        @catch (NSException *exception) {
        }
        return YES;
    }
    return NO;
}

- (void)applicationWillResignActive:(UIApplication *)application
{
    /*
     Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
     Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
     */
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    /*
     Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
     If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
     */
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    /*
     Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
     */
}

- (void)applicationWillTerminate:(UIApplication *)application
{

}

+ (BOOL)containsFavorite:(int)tagId {
    return [self.favorites containsObject:@(tagId)];
}

+ (void)moveFavoriteAt:(NSUInteger)fromIndex to:(NSUInteger)toIndex {
    NSMutableArray *favorites = [NSMutableArray arrayWithArray:self.favorites];
    id toMove = self.favorites[fromIndex];
    [favorites removeObjectAtIndex:fromIndex];
    [favorites insertObject:toMove atIndex:toIndex];
    [self setFavorites:favorites];
}

+ (void)addFavorite:(int)tagId {
    if ([self.favorites containsObject:@(tagId)]) {
        return;
    }
    NSMutableArray *favorites = [NSMutableArray arrayWithArray:self.favorites];
    [favorites addObject:@(tagId)];
    [self setFavorites:favorites];
}

+ (void)removeFavorite:(int)tagId {
    NSMutableArray *favorites = [NSMutableArray arrayWithArray:self.favorites];
    [favorites removeObject:@(tagId)];
    [self setFavorites:favorites];
}

+ (BOOL)containsTeachable:(int)tagId {
    return [self.teachable containsObject:@(tagId)];
}

+ (void)moveTeachableAt:(NSUInteger)fromIndex to:(NSUInteger)toIndex {
    NSMutableArray *teachable = [NSMutableArray arrayWithArray:self.teachable];
    id toMove = teachable[fromIndex];
    [teachable removeObjectAtIndex:fromIndex];
    [teachable insertObject:toMove atIndex:toIndex];
    [self setTeachable:teachable];
}

+ (void)addTeachable:(int)tagId {
    if ([self.teachable containsObject:@(tagId)]) {
        return;
    }
    NSMutableArray *teachable = [NSMutableArray arrayWithArray:self.teachable];
    [teachable addObject:@(tagId)];
    [self setTeachable:teachable];
}

+ (void)removeTeachable:(int)tagId {
    NSMutableArray *teachable = [NSMutableArray arrayWithArray:self.teachable];
    [teachable removeObject:@(tagId)];
    [self setTeachable:teachable];
}

+ (UIBarButtonItem *)barButtonItemWithSystemName:(NSString *)systemName
                                          target:(id)target
                                          action:(SEL)action {
    UIImageSymbolConfiguration *configuration =
        [UIImageSymbolConfiguration configurationWithPointSize:17
                                                        weight:UIImageSymbolWeightRegular
                                                         scale:UIImageSymbolScaleMedium];
    UIImage *image = [UIImage systemImageNamed:systemName
                             withConfiguration:configuration];
    return [[UIBarButtonItem alloc] initWithImage:image
                                            style:UIBarButtonItemStylePlain
                                           target:target
                                           action:action];
}

+ (void)setUpBackground:(UIView *)view {
    [TMTheme installBackgroundIn:view];
}

@end
