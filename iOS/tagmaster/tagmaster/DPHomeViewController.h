//
//  DPHomeViewController.h
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPAppDelegate.h"

@interface DPHomeViewController : UITableViewController <TMTagListSource>

/// Re-selects the favorites row for the tag currently open in an expanded
/// split, or deselects when collapsed. Exposed for the Swift user-data-changed
/// hook, which also reloads the table.
- (void)tm_syncSelectionForSplit;

@end
