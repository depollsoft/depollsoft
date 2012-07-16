//
//  DPSettingsViewController.h
//  pitchperfect
//
//  Created by David Poll on 6/23/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface DPSettingsViewController : UIViewController<UITableViewDataSource, UITableViewDelegate>

+ (DPSettingsViewController *)sharedInstance;

@property (nonatomic, weak) UIPopoverController *popoverController;

@end
