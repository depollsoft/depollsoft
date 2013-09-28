//
//  DPTabBarController.h
//  depolllib
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface DPTabBarController : UIViewController

@property (nonatomic, readonly, strong) UITabBar *tabBar;
@property (nonatomic, copy) NSArray *viewControllers;

@end
