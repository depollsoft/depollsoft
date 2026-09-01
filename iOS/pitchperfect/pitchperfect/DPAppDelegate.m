//
//  DPAppDelegate.m
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPAppDelegate.h"
#import <AVFoundation/AVFoundation.h>
#import "DPSettingsModel.h"
#import "DPSongsModel.h"
#import "DPJsonSerializer.h"
#import "DPJsonPrimitive.h"
#import "DPKey.h"
#import "DPKeyType.h"
#import "DPAccidental.h"
#import "DPNote.h"
#import "DPPitchedSong.h"
#import "DPLoginViewController.h"
#import "DPAppDelegate+Ads.h"
#import "GoogleMobileAdsStub.h"
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
#import "pitchperfect-Swift.h"

@import FirebaseAuth;
@import FirebaseCore;

#define PRODUCTION
//#define TEST_ADS

@implementation DPAppDelegate

@synthesize window = _window;

- (void)configureRootNavigationControllers {
    UITabBarController *tabBarController =
        (UITabBarController *)self.window.rootViewController;
    if (![tabBarController isKindOfClass:[UITabBarController class]]) {
        return;
    }

    NSMutableArray<UIViewController *> *controllers = [NSMutableArray array];
    for (UIViewController *controller in tabBarController.viewControllers) {
        if ([controller isKindOfClass:[UINavigationController class]]) {
            [controllers addObject:controller];
            continue;
        }
        UINavigationController *navigationController =
            [[UINavigationController alloc] initWithRootViewController:controller];
        navigationController.tabBarItem = controller.tabBarItem;
        [controllers addObject:navigationController];
    }
    tabBarController.viewControllers = controllers;

    // Pitch Perfect owns a stable grayscale instrument world. The opaque
    // appearance and template images avoid Liquid Glass icon morphing/flicker
    // when switching tabs in light mode.
    UITabBarAppearance *appearance = [[UITabBarAppearance alloc] init];
    [appearance configureWithOpaqueBackground];
    appearance.backgroundColor = UIColor.systemBackgroundColor;
    appearance.shadowColor = UIColor.separatorColor;
    NSArray<UITabBarItemAppearance *> *itemAppearances = @[
        appearance.stackedLayoutAppearance,
        appearance.inlineLayoutAppearance,
        appearance.compactInlineLayoutAppearance
    ];
    for (UITabBarItemAppearance *itemAppearance in itemAppearances) {
        itemAppearance.normal.iconColor = UIColor.secondaryLabelColor;
        itemAppearance.normal.titleTextAttributes = @{NSForegroundColorAttributeName: UIColor.secondaryLabelColor};
        itemAppearance.selected.iconColor = UIColor.labelColor;
        itemAppearance.selected.titleTextAttributes = @{NSForegroundColorAttributeName: UIColor.labelColor};
    }
    tabBarController.tabBar.standardAppearance = appearance;
    tabBarController.tabBar.scrollEdgeAppearance = appearance;
    tabBarController.tabBar.translucent = NO;

    UINavigationBarAppearance *navigationAppearance = [[UINavigationBarAppearance alloc] init];
    [navigationAppearance configureWithOpaqueBackground];
    navigationAppearance.backgroundColor = UIColor.systemBackgroundColor;
    navigationAppearance.shadowColor = UIColor.separatorColor;
    UIFont *titleFont = [UIFont fontWithName:@"Oswald-Medium" size:19] ?: [UIFont preferredFontForTextStyle:UIFontTextStyleHeadline];
    navigationAppearance.titleTextAttributes = @{
        NSForegroundColorAttributeName: UIColor.labelColor,
        NSFontAttributeName: titleFont
    };

    for (UINavigationController *navigationController in controllers) {
        UITabBarItem *item = navigationController.tabBarItem;
        UIImage *templateImage = [item.image imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
        item.image = templateImage;
        item.selectedImage = templateImage;

        navigationController.navigationBar.standardAppearance = navigationAppearance;
        navigationController.navigationBar.scrollEdgeAppearance = navigationAppearance;
        navigationController.navigationBar.compactAppearance = navigationAppearance;
        navigationController.navigationBar.tintColor = UIColor.labelColor;
        navigationController.navigationBar.translucent = NO;
    }
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    [self configureRootNavigationControllers];

    if (NSClassFromString(@"XCTestCase") != nil) {
        return YES;
    }

    [DPAppLog start];
    [FIRApp configure];
#if HAS_FBSDK
    [[FBSDKApplicationDelegate sharedInstance] application:application
                             didFinishLaunchingWithOptions:launchOptions];
#endif
    [[GADMobileAds sharedInstance] startWithCompletionHandler:nil];
    [application registerForRemoteNotifications];

    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryPlayback error:nil];
    
    [DPJsonSerializer registerAlias:@"List" forClass:NSClassFromString(@"__NSArrayM")];
    [DPJsonSerializer registerAlias:@"Key" forClass:[DPKey class]];
    [DPJsonSerializer registerAlias:@"KeyType" forClass:[DPKeyType class]];
    [DPJsonSerializer registerAlias:@"Accidental" forClass:[DPAccidental class]];
    [DPJsonSerializer registerAlias:@"Note" forClass:[DPNote class]];
    [DPJsonSerializer registerAlias:@"PitchedSong" forClass:[DPPitchedSong class]];
    [DPJsonSerializer registerAlias:@"String" forClass:[NSString class]];
    [DPJsonSerializer registerAlias:@"Primitive" forClass:[DPJsonPrimitive class]];
    [DPJsonSerializer registerAlias:@"Integer" forObjCType:[NSString stringWithUTF8String:@encode(int)]];
    [DPJsonSerializer registerAlias:@"Boolean" forObjCType:[NSString stringWithUTF8String:@encode(BOOL)]];
    [DPJsonSerializer registerAlias:@"Double" forObjCType:[NSString stringWithUTF8String:@encode(double)]];
    
    [DPTheme applyStoredAppearance];

    dispatch_async(dispatch_get_main_queue(), ^{
        // First launch belongs to the first pitch: the login prompt waits for the next session.
        BOOL firstLaunchEver = ![[NSUserDefaults standardUserDefaults] boolForKey:@"depollsoft.pitchperfect.FirstLaunchSeen"];
        if (firstLaunchEver) {
            [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"depollsoft.pitchperfect.FirstLaunchSeen"];
            return;
        }
        if (![[NSUserDefaults standardUserDefaults] boolForKey:@"depollsoft.pitchperfect.LoginShown"] && ![FIRAuth auth].currentUser) {
            [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"depollsoft.pitchperfect.LoginShown"];
            DPLoginViewController *loginViewController = [[DPLoginViewController alloc] init];
            UINavigationController *navigationController =
                [[UINavigationController alloc] initWithRootViewController:loginViewController];
            [self.window.rootViewController presentViewController:navigationController
                                                         animated:YES
                                                       completion:nil];
        }
    });
    
    // Facebook SDK initialization removed
    
    [self extraInit];
    
    // Override point for customization after application launch.
    return YES;
}

// Ads removed; no test Ads toggle

- (void)applicationWillResignActive:(UIApplication *)application
{
    // Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
    // Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
}

- (void)applicationDidEnterBackground:(UIApplication *)application
{
    // Use this method to release shared resources, save user data, invalidate timers, and store enough application state information to restore your application to its current state in case it is terminated later.
    // If your application supports background execution, this method is called instead of applicationWillTerminate: when the user quits.
}

- (void)applicationWillEnterForeground:(UIApplication *)application
{
    // Called as part of the transition from the background to the inactive state; here you can undo many of the changes made on entering the background.
}

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    // Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary *)options {
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
    return [[FIRAuth auth] canHandleURL:url];
}

+ (void)noteTouchStarted:(DPNote *)note forCell:(UITableViewCell *)cell {
    if ([DPSettingsModel sharedInstance].toggleNotes) {
        if (note.isPlaying) {
            [note stop];
            [cell setHighlighted:NO animated:YES];
        } else {
            [note play];
            [cell setHighlighted:YES animated:YES];
        }
    } else {
        [note play];
        [cell setHighlighted:YES animated:YES];
    }
}

+ (void)noteTouchEnded:(DPNote *)note forCell:(UITableViewCell *)cell {
    if (![DPSettingsModel sharedInstance].toggleNotes) {
        [note stop];
        [cell setHighlighted:NO animated:YES];
    }
}

// Ads removed; no ad requests

@end
