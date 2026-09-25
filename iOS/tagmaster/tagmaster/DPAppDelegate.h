//
//  DPAppDelegate.h
//  tagmaster
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>

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

/// Both app and unit-test hosts must preserve URL/date values in the disk cache.
+ (void)configureCacheSerialization;

/// Hands a sign-in callback URL to Google, Facebook or Firebase; NO when none of them owns it.
+ (BOOL)handleAuthURL:(NSURL *)url;

+ (BOOL)containsFavorite:(int)tagId;
+ (void)moveFavoriteAt:(NSUInteger)fromIndex to:(NSUInteger)toIndex;
+ (void)addFavorite:(int)tagId;
+ (void)removeFavorite:(int)tagId;
+ (BOOL)containsTeachable:(int)tagId;
+ (void)moveTeachableAt:(NSUInteger)fromIndex to:(NSUInteger)toIndex;
+ (void)addTeachable:(int)tagId;
+ (void)removeTeachable:(int)tagId;

/// Tag Master's one accent, shared with Android: #007AA3 in light, #5AC8FA in dark. Tints,
/// page tabs, links, the lit row and the quartet notes all draw from it.
+ (UIColor *)accentColor;

@end
