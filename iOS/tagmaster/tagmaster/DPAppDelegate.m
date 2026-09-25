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
#import "DPJsonSerializer.h"
#import "tagmaster-Swift.h"

NSNotificationName const TMTagListDidChangeNotification = @"TMTagListDidChangeNotification";
NSNotificationName const TMTagSelectionDidChangeNotification = @"TMTagSelectionDidChangeNotification";

@implementation DPAppDelegate

+ (void)configureCacheSerialization
{
    // Unit tests write the same disk cache later read by UI test launches.
    // Register its value types before the test-only startup returns.
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
}

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    [DPAppDelegate configureCacheSerialization];

    [DPAppLog start];
    [FIRApp configure];
    [TelemetryConsent configure];
#if HAS_FBSDK
    [[FBSDKApplicationDelegate sharedInstance] application:application
                             didFinishLaunchingWithOptions:launchOptions];
#endif
    [application registerForRemoteNotifications];
    [self extraInit];
    return YES;
}

+ (BOOL)handleAuthURL:(NSURL *)url {
#if HAS_GOOGLE_SIGN_IN
    if ([[GIDSignIn sharedInstance] handleURL:url]) {
        return YES;
    }
#endif
#if HAS_FBSDK
    if ([[FBSDKApplicationDelegate sharedInstance] application:UIApplication.sharedApplication
                                                     openURL:url
                                                     options:@{}]) {
        return YES;
    }
#endif
    return [[FIRAuth auth] canHandleURL:url];
}

+ (UIColor *)accentColor {
    static UIColor *accent;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        accent = [UIColor colorWithDynamicProvider:^UIColor *(UITraitCollection *traits) {
            return traits.userInterfaceStyle == UIUserInterfaceStyleDark
                ? [UIColor colorWithRed:0x5A / 255.0 green:0xC8 / 255.0 blue:0xFA / 255.0 alpha:1]
                : [UIColor colorWithRed:0x00 / 255.0 green:0x7A / 255.0 blue:0xA3 / 255.0 alpha:1];
        }];
    });
    return accent;
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

@end
