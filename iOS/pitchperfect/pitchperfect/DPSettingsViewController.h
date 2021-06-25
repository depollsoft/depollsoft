//
//  DPSettingsViewController.h
//  pitchperfect
//
//  Created by David Poll on 6/23/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPToolbarViewController.h"

@interface DPSettingsViewController : DPToolbarViewController<UITableViewDataSource, UITableViewDelegate>

+ (DPSettingsViewController *)sharedInstance;

@end
