//
//  DPAppDelegate.m
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPAppDelegate.h"
#import <AVFoundation/AVFoundation.h>
#import <Parse/Parse.h>
#import "DPSettingsModel.h"
#import "DPSongsModel.h"
#import "DPJsonSerializer.h"
#import "DPJsonPrimitive.h"
#import "DPKey.h"
#import "DPKeyType.h"
#import "DPAccidental.h"
#import "DPNote.h"
#import "DPPitchedSong.h"
#import "FlurryAnalytics.h"

#define PRODUCTION
//#define TEST_ADS

@implementation DPAppDelegate

@synthesize window = _window;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryPlayback error:nil];
        
#ifdef PRODUCTION
    [Parse setApplicationId:@"cXYwcCUUP2f78OBfMlXu7dk03f2JRMQYXpCnv7H9" clientKey:@"Y9ZIP3kLs1Jbh9Mpr2s8tRw9tjdGt6GuseuRHNdE"];
    [PFFacebookUtils initializeWithApplicationId:@"263872380333771"];
    [FlurryAnalytics startSession:@"JMG2ZWM6HXCZTC33YHKF"];
#else
    [Parse setApplicationId:@"fIRF0tfJBkE2XbiJf4diG2LsRphoqPe4q4GazAKu" clientKey:@"Edcy5i5CKUTLwJe7m56MeIT1LrjBb9ZP1by89Rd4"];
    [PFFacebookUtils initializeWithApplicationId:@"292538514135026"];
#endif
    
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
    
    // Initialize settings
    [DPSettingsModel sharedInstance];
    
    // Override point for customization after application launch.
    return YES;
}

+ (void)startupRefreshFromParse {
    if ([PFUser currentUser]) {
        @try {
            [[PFUser currentUser] fetchInBackgroundWithBlock:^(PFObject *object, NSError *error) {
                if (!error) {
                    [[DPSettingsModel sharedInstance] restoreUser];
                    [[DPSongsModel sharedInstance] refreshFromParse];
                }
            }];
        }
        @catch (NSException *exception) {
        }
    }
}

+ (BOOL)testAds {
#ifdef TEST_ADS
    return YES;
#else
    return NO;
#endif
}

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
    [DPAppDelegate startupRefreshFromParse];
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Called when the application is about to terminate. Save data if appropriate. See also applicationDidEnterBackground:.
}

- (BOOL)application:(UIApplication *)application handleOpenURL:(NSURL *)url {
    return [PFFacebookUtils handleOpenURL:url];
}

- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url
  sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
    return [PFFacebookUtils handleOpenURL:url]; 
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

+ (GADRequest *)adRequest {
    GADRequest *request = [GADRequest request];
    request.testing = [DPAppDelegate testAds];
    request.keywords = [NSMutableArray arrayWithObjects:@"music", @"musician", @"singer", @"a cappella", @"notes", @"harmony", @"sheet music", @"songs", @"instrument", @"pitch pipe", @"barbershop", nil];
    return request;
}

@end
