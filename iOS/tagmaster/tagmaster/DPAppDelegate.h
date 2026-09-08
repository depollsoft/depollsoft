//
//  DPAppDelegate.h
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPBusyIndicator.h"

@interface DPAppDelegate : UIResponder <UIApplicationDelegate>

@property (strong, nonatomic) UIWindow *window;
@property (strong, nonatomic) DPBusyIndicator *busyIndicator;

@property (readonly, strong, nonatomic) NSManagedObjectContext *managedObjectContext;
@property (readonly, strong, nonatomic) NSManagedObjectModel *managedObjectModel;
@property (readonly, strong, nonatomic) NSPersistentStoreCoordinator *persistentStoreCoordinator;

@property (readonly, strong, nonatomic) UINavigationController *navigationController;

+ (BOOL)containsFavorite:(int)tagId;
+ (void)moveFavoriteAt:(NSUInteger)fromIndex to:(NSUInteger)toIndex;
+ (void)addFavorite:(int)tagId;
+ (void)removeFavorite:(int)tagId;
+ (BOOL)containsTeachable:(int)tagId;
+ (void)moveTeachableAt:(NSUInteger)fromIndex to:(NSUInteger)toIndex;
+ (void)addTeachable:(int)tagId;
+ (void)removeTeachable:(int)tagId;

+ (void)setUpBackground:(UIView *)view;
/// Opens a tag from any screen: the secondary column on a regular-width iPad,
/// a push on the current navigation stack everywhere else.
+ (void)showTagWithId:(int)tagId from:(UIViewController *)sender;
+ (UIBarButtonItem *)barButtonItemWithSystemName:(NSString *)systemName
                                          target:(id)target
                                          action:(SEL)action;

@end
