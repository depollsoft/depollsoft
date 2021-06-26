//
//  DPAppDelegate.m
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPAppDelegate.h"

@import FirebaseAuthUI;

#import <Parse/Parse.h>
#import <FBSDKCoreKit/FBSDKCoreKit.h>

#import "DPBarbershop.h"
#import "DPHomeViewController.h"
#import "DPBrowseViewController.h"
#import "DPJsonSerializer.h"
#import "DPTagViewController.h"
#import "tagmaster-Swift.h"

@import Firebase;

@implementation DPAppDelegate

@synthesize window = _window;
@synthesize managedObjectContext = __managedObjectContext;
@synthesize managedObjectModel = __managedObjectModel;
@synthesize persistentStoreCoordinator = __persistentStoreCoordinator;
@synthesize navigationController;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    [Parse initializeWithConfiguration:[ParseClientConfiguration configurationWithBlock:^(id<ParseMutableClientConfiguration>  _Nonnull configuration) {
        configuration.applicationId = @"RhfRllVEF5Qlm0DyVWzx6zi1yjxlmCrnqFtJFwbj";
        configuration.clientKey = @"7xDIp24FCSz218vpiHhcudEb2Bytn8AzIrBfVLM4";
        configuration.server = @"https://tagmaster-api.depollsoft.xyz";
    }]];
    [FIRApp configure];
    [[FBSDKApplicationDelegate sharedInstance] application:application
                             didFinishLaunchingWithOptions:launchOptions];
        
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
    self.window.backgroundColor = [UIColor systemBackgroundColor];
    [self.window makeKeyAndVisible];
    
    UINavigationController *navController = [[UINavigationController alloc] init];
    self.window.rootViewController = navController;
    [navController pushViewController:[[DPHomeViewController alloc] init] animated:YES];
    
    navigationController = navController;
    [self.window makeKeyAndVisible];
    
    [self extraInit];

    return YES;
}

- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options {
    NSString *sourceApplication = options[UIApplicationOpenURLOptionsSourceApplicationKey];
    if ([[FUIAuth defaultAuthUI] handleOpenURL:url sourceApplication:sourceApplication]) {
        return YES;
    }
    if (url.pathComponents.count == 3 && [url.pathComponents[1] isEqualToString:@"tag"]) {
        NSString *tagNumberString = url.pathComponents[2];
        @try {
            int tagId = tagNumberString.intValue;
            DPTagViewController *controller = [[DPTagViewController alloc] init];
            controller.tagId = tagId;
            [self.navigationController pushViewController:controller animated:YES];
        }
        @catch (NSException *exception) {
        }
        return NO;
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

+ (void)setUpBackground:(UIView *)view {
    view.backgroundColor = [UIColor systemBackgroundColor];
    UIImageView *backgroundImage = [[UIImageView alloc] initWithImage:[UIImage imageNamed:@"screenbackground.png"]];
    backgroundImage.userInteractionEnabled = NO;
    backgroundImage.contentMode = UIViewContentModeScaleAspectFit;
    backgroundImage.translatesAutoresizingMaskIntoConstraints = NO;

    if ([view isKindOfClass:[UITableView class]]) {
        UITableView *tableView = (UITableView *)view;
        view = tableView.backgroundView = [[UIView alloc] init];
    }

    [view addSubview:backgroundImage];
    [view sendSubviewToBack:backgroundImage];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[backgroundImage]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(backgroundImage)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-60-[backgroundImage]-44-|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(backgroundImage)]];
}


@end
