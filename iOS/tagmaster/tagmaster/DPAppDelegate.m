//
//  DPAppDelegate.m
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPAppDelegate.h"
#import "DPBarbershop.h"
#import "DPHomeViewController.h"
#import "DPBrowseViewController.h"
#import "DPJsonSerializer.h"

@implementation DPAppDelegate

@synthesize window = _window;
@synthesize managedObjectContext = __managedObjectContext;
@synthesize managedObjectModel = __managedObjectModel;
@synthesize persistentStoreCoordinator = __persistentStoreCoordinator;
@synthesize navigationController;

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
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
    self.window.backgroundColor = [UIColor whiteColor];
    [self.window makeKeyAndVisible];
    
    UINavigationController *navController = [[UINavigationController alloc] init];
    self.window.rootViewController = navController;
    [navController pushViewController:[[DPHomeViewController alloc] init] animated:YES];
    
    navigationController = navController;
    [self.window makeKeyAndVisible];

    return YES;
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

- (void)applicationDidBecomeActive:(UIApplication *)application
{
    /*
     Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
     */
}

- (void)applicationWillTerminate:(UIApplication *)application
{
    // Saves changes in the application's managed object context before the application terminates.
    [self saveContext];
}

- (void)saveContext
{
    NSError *error = nil;
    NSManagedObjectContext *managedObjectContext = self.managedObjectContext;
    if (managedObjectContext != nil)
    {
        if ([managedObjectContext hasChanges] && ![managedObjectContext save:&error])
        {
            /*
             Replace this implementation with code to handle the error appropriately.
             
             abort() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
             */
            NSLog(@"Unresolved error %@, %@", error, [error userInfo]);
            abort();
        }
    }
}

#pragma mark - Core Data stack

/**
 Returns the managed object context for the application.
 If the context doesn't already exist, it is created and bound to the persistent store coordinator for the application.
 */
- (NSManagedObjectContext *)managedObjectContext
{
    if (__managedObjectContext != nil)
    {
        return __managedObjectContext;
    }
    
    NSPersistentStoreCoordinator *coordinator = [self persistentStoreCoordinator];
    if (coordinator != nil)
    {
        __managedObjectContext = [[NSManagedObjectContext alloc] init];
        [__managedObjectContext setPersistentStoreCoordinator:coordinator];
    }
    return __managedObjectContext;
}

/**
 Returns the managed object model for the application.
 If the model doesn't already exist, it is created from the application's model.
 */
- (NSManagedObjectModel *)managedObjectModel
{
    if (__managedObjectModel != nil)
    {
        return __managedObjectModel;
    }
    NSURL *modelURL = [[NSBundle mainBundle] URLForResource:@"tagmaster" withExtension:@"momd"];
    __managedObjectModel = [[NSManagedObjectModel alloc] initWithContentsOfURL:modelURL];
    return __managedObjectModel;
}

/**
 Returns the persistent store coordinator for the application.
 If the coordinator doesn't already exist, it is created and the application's store added to it.
 */
- (NSPersistentStoreCoordinator *)persistentStoreCoordinator
{
    if (__persistentStoreCoordinator != nil)
    {
        return __persistentStoreCoordinator;
    }
    
    NSURL *storeURL = [[self applicationDocumentsDirectory] URLByAppendingPathComponent:@"tagmaster.sqlite"];
    
    NSError *error = nil;
    __persistentStoreCoordinator = [[NSPersistentStoreCoordinator alloc] initWithManagedObjectModel:[self managedObjectModel]];
    if (![__persistentStoreCoordinator addPersistentStoreWithType:NSSQLiteStoreType configuration:nil URL:storeURL options:nil error:&error])
    {
        /*
         Replace this implementation with code to handle the error appropriately.
         
         abort() causes the application to generate a crash log and terminate. You should not use this function in a shipping application, although it may be useful during development.
         
         Typical reasons for an error here include:
         * The persistent store is not accessible;
         * The schema for the persistent store is incompatible with current managed object model.
         Check the error message to determine what the actual problem was.
         
         
         If the persistent store is not accessible, there is typically something wrong with the file path. Often, a file URL is pointing into the application's resources directory instead of a writeable directory.
         
         If you encounter schema incompatibility errors during development, you can reduce their frequency by:
         * Simply deleting the existing store:
         [[NSFileManager defaultManager] removeItemAtURL:storeURL error:nil]
         
         * Performing automatic lightweight migration by passing the following dictionary as the options parameter:
         [NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithBool:YES], NSMigratePersistentStoresAutomaticallyOption, [NSNumber numberWithBool:YES], NSInferMappingModelAutomaticallyOption, nil];
         
         Lightweight migration will only work for a limited set of schema changes; consult "Core Data Model Versioning and Data Migration Programming Guide" for details.
         
         */
        NSLog(@"Unresolved error %@, %@", error, [error userInfo]);
        abort();
    }
    
    return __persistentStoreCoordinator;
}

#pragma mark - Application's Documents directory

/**
 Returns the URL to the application's Documents directory.
 */
- (NSURL *)applicationDocumentsDirectory {
    return [[[NSFileManager defaultManager] URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask] lastObject];
}

+ (NSArray *)favorites {
    NSArray *array = [[NSUserDefaults standardUserDefaults] arrayForKey:@"favorites"];
    if (array) {
        return array;
    }
    return @[];
}

+ (void)setFavorites:(NSArray *)favorites {
    [[NSUserDefaults standardUserDefaults] setObject:favorites forKey:@"favorites"];
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

+ (NSArray *)teachable {
    NSArray *array = [[NSUserDefaults standardUserDefaults] arrayForKey:@"teachable"];
    if (array) {
        return array;
    }
    return @[];
}

+ (void)setTeachable:(NSArray *)teachable {
    [[NSUserDefaults standardUserDefaults] setObject:teachable forKey:@"teachable"];
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
    view.backgroundColor = [UIColor whiteColor];
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
