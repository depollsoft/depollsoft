//
//  DPTagViewController.h
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "TMPageViewController.h"
#import "DPAppDelegate.h"

@interface DPTagViewController : TMPageViewController

@property (nonatomic) int tagId;
/// The list that opened this tag, if any. Drives the previous/next tag steppers.
@property (nonatomic, weak) id<TMTagListSource> source;
/// Step to the neighbouring tag in `source`; no-ops when there is none or the
/// split is collapsed. Also reachable through ⌘↑ / ⌘↓ from either column.
- (void)stepToPreviousTag;
- (void)stepToNextTag;

@end
