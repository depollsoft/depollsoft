//
//  DPTeachableTagsController.h
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPAppDelegate.h"

@interface DPTeachableTagsController : UITableViewController <TMTagListSource>

/// Re-selects the row for the tag currently open in an expanded split, or
/// deselects when collapsed. Exposed for the Swift user-data-changed hook.
- (void)tm_syncSelectionForSplit;

@end
