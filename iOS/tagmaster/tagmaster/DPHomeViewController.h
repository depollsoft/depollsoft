//
//  DPHomeViewController.h
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPAppDelegate.h"

/// Home's three groups: the app's destinations, the user's lists, then favorites.
typedef NS_ENUM(NSInteger, TMHomeSection) {
    TMHomeNavigationSection = 0,
    TMHomeListsSection = 1,
    TMHomeFavoritesSection = 2
};

@interface DPHomeViewController : UITableViewController <TMTagListSource>

/// Set while Home applies a list change of its own with a row animation, so the
/// user-data-changed hook does not reload the table underneath that animation
/// (or underneath an in-progress drag).
@property (nonatomic) BOOL tm_applyingLocalListChange;

/// Re-selects the favorites row for the tag currently open in an expanded
/// split, or deselects when collapsed. Exposed for the Swift user-data-changed
/// hook, which also reloads the table.
- (void)tm_syncSelectionForSplit;

/// Edit has work to do when there are favorites or custom lists; called after
/// every change Home makes to either.
- (void)updateEditButton;

/// The ordered keys of the user's custom lists, one row each in the Lists group.
- (NSArray<NSString *> *)tm_customListKeys;

/// The Rename… / Delete… menu a custom list row offers on a long press.
- (UIMenu *)tm_menuForListKey:(NSString *)key;

@end
