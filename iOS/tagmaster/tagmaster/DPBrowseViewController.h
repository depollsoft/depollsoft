//
//  DPBrowseViewController.h
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>

@class DPTagQueryViewController;

/// Browse is a destination in its own right, so its four collections are a
/// segmented control over one list rather than a second tab bar nested inside
/// the app's navigation.
@interface DPBrowseViewController : UIViewController

/// Titles of the browse collections, in display order.
+ (NSArray<NSString *> *)collectionTitles;

/// Switches the visible collection. Exposed for tests and state restoration.
- (void)selectCollectionAtIndex:(NSInteger)index;

/// The collection currently on screen.
@property (nonatomic, readonly) NSInteger selectedCollectionIndex;

/// The list controller backing a collection, created on first use.
- (DPTagQueryViewController *)queryControllerForIndex:(NSInteger)index;

@end
