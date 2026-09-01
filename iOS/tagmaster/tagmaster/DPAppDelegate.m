//
//  DPAppDelegate.m
//  tagmaster
//

#include <TargetConditionals.h>
#if TARGET_OS_IPHONE

#import "DPAppDelegate.h"
#import "DPBarbershop.h"
#import "DPJsonSerializer.h"
#import "tagmaster-Swift.h"
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

@import FirebaseAuth;
@import FirebaseCore;

@implementation DPAppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    if (NSClassFromString(@"XCTestCase") != nil) {
        return YES;
    }

    [DPAppLog start];
    [FIRApp configure];
#if HAS_FBSDK
    [[FBSDKApplicationDelegate sharedInstance] application:application
                             didFinishLaunchingWithOptions:launchOptions];
#endif
    [application registerForRemoteNotifications];

    [DPJsonSerializer registerSerializer:^NSString *(NSURL *url) {
        return url.absoluteString;
    } deserializer:^NSURL *(NSString *input) {
        return [NSURL URLWithString:input];
    } forClass:[NSURL class]];

    NSNumberFormatter *numberFormatter = [[NSNumberFormatter alloc] init];
    [DPJsonSerializer registerSerializer:^NSString *(NSDate *date) {
        return [numberFormatter stringFromNumber:@(date.timeIntervalSince1970)];
    } deserializer:^NSDate *(NSString *input) {
        return [NSDate dateWithTimeIntervalSince1970:[numberFormatter numberFromString:input].doubleValue];
    } forClass:[NSDate class]];

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
    if ([[FIRAuth auth] canHandleURL:url]) {
        return YES;
    }
    if (url.pathComponents.count != 3 || ![url.pathComponents[1] isEqualToString:@"tag"]) {
        return NO;
    }

    NSScanner *scanner = [NSScanner scannerWithString:url.pathComponents[2]];
    int tagId = 0;
    if (![scanner scanInt:&tagId] || !scanner.isAtEnd || tagId <= 0) {
        return NO;
    }
    [TagMasterDeepLinkRouter openTagWithId:tagId];
    return YES;
}

@end

#endif
