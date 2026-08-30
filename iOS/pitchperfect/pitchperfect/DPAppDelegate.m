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
// Facebook SDK removed during SDK migration
// #import <FBSDKCoreKit/FBSDKCoreKit.h>
#import "pitchperfect-Swift.h"

@import FirebaseAuth;
@import FirebaseCore;

#define PRODUCTION
//#define TEST_ADS

@implementation DPAppDelegate

@synthesize window = _window;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    if (NSClassFromString(@"XCTestCase") != nil) {
        return YES;
    }

    [FIRApp configure];
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
    
    dispatch_async(dispatch_get_main_queue(), ^{
        if (![[NSUserDefaults standardUserDefaults] boolForKey:@"depollsoft.pitchperfect.LoginShown"] && ![FIRAuth auth].currentUser) {
            [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"depollsoft.pitchperfect.LoginShown"];
            DPLoginViewController *loginViewController = [[DPLoginViewController alloc] init];
            [self.window.rootViewController presentViewController:loginViewController
                                                         animated:YES
                                                       completion:^{
                                                       }];
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
  // Let Firebase Auth handle email link / OAuth redirect URLs
  if ([[FIRAuth auth] canHandleURL:url]) {
    return YES;
  }
  return NO;
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
