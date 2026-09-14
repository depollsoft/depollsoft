//
//  DPAppDelegate.h
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPBusyIndicator.h"

/// Adopted by any on-screen list of tags (a query results list, Home's favorites
/// section, the teachable list) so the open detail can step to a neighbouring tag
/// without leaving the list, on both the primary chevrons and ⌘↑/⌘↓.
@protocol TMTagListSource <NSObject>
/// The tag ids in the order they are presented, top to bottom.
- (NSArray<NSNumber *> *)tm_listedTagIds;
/// Called after a step lands on tagId, so the source can select and scroll the
/// matching row into view (and, for a paged query list whose last loaded tag
/// was just stepped onto while more results remain, fetch the next page).
- (void)tm_didStepToTagId:(int)tagId;
@end

/// Posted whenever a tag list source's `tm_listedTagIds` may have changed, so a
/// showing detail can recompute whether a neighbouring tag now exists.
FOUNDATION_EXPORT NSNotificationName const TMTagListDidChangeNotification;
/// The open tag or the split presentation changed; lists reconcile their selection.
FOUNDATION_EXPORT NSNotificationName const TMTagSelectionDidChangeNotification;

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
/// The tag id currently shown by the secondary column's detail controller for
/// sender's split, or nil when there is no split or no tag showing yet.
+ (NSNumber *)currentSplitTagIdFor:(UIViewController *)sender;
/// Tag Master's one accent, shared with Android: #007AA3 in light, #5AC8FA in dark. The window
/// tint, page tabs, links, the lit row and the quartet notes all draw from it.
+ (UIColor *)accentColor;

@end
