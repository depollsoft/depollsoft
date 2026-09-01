//
//  DPAppDelegate.m
//  pitchperfect
//

#include <TargetConditionals.h>
#if TARGET_OS_IPHONE

#import "DPAppDelegate.h"
#import <AVFoundation/AVFoundation.h>
#import "DPJsonSerializer.h"
#import "DPJsonPrimitive.h"
#import "DPKey.h"
#import "DPKeyType.h"
#import "DPAccidental.h"
#import "DPNote.h"
#import "DPPitchedSong.h"
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

@implementation DPAppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
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

    [[AVAudioSession sharedInstance] setCategory:AVAudioSessionCategoryPlayback error:nil];

    [DPAppDelegate setIdleTimerDisabled:DPSettingsModel.sharedInstance.wakeLock];
    [self extraInit];
    return YES;
}

- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey, id> *)options {
#if HAS_GOOGLE_SIGN_IN
    if ([[GIDSignIn sharedInstance] handleURL:url]) {
        return YES;
    }
#endif
#if HAS_FBSDK
    if ([[FBSDKApplicationDelegate sharedInstance] application:application
                                                     openURL:url
                                                     options:options]) {
        return YES;
    }
#endif
    return [[FIRAuth auth] canHandleURL:url];
}

+ (void)setIdleTimerDisabled:(BOOL)disabled {
    dispatch_async(dispatch_get_main_queue(), ^{
        UIApplication.sharedApplication.idleTimerDisabled = disabled;
    });
}

@end

#endif
