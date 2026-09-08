//
//  DPBrowseViewController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPBrowseViewController.h"
#import "DPTagQueryViewController.h"
#import "DPAppDelegate.h"

@interface DPBrowseViewController ()

@end

@implementation DPBrowseViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        self.title = @"Browse";
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
    [DPAppDelegate setUpBackground:self.view];
    // Keep native page tabs usable when UIKit reports only the safe-area height.
    [self.tabBar.heightAnchor constraintGreaterThanOrEqualToConstant:83].active = YES;
    
	NSMutableArray *controllers = [NSMutableArray array];
    
    DPTagQueryViewController *latest = [[DPTagQueryViewController alloc] init];
    latest.sortBy = DPTagSortPosted;
    latest.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Latest" image:[UIImage systemImageNamed:@"clock"] tag:0];
    [controllers addObject:latest];
    
    DPTagQueryViewController *rating = [[DPTagQueryViewController alloc] init];
    rating.sortBy = DPTagSortRating;
    rating.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Rating" image:[UIImage systemImageNamed:@"star"] tag:1];
    [controllers addObject:rating];
    
    DPTagQueryViewController *downloads = [[DPTagQueryViewController alloc] init];
    downloads.sortBy = DPTagSortDownloaded;
    downloads.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Downloads" image:[UIImage systemImageNamed:@"arrow.down.circle"] tag:2];
    [controllers addObject:downloads];
    
    DPTagQueryViewController *classic = [[DPTagQueryViewController alloc] init];
    classic.sortBy = DPTagSortClassic;
    classic.collection = DPTagCollectionClassicTags;
    classic.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Classic" image:[UIImage systemImageNamed:@"book"] tag:3];
    [controllers addObject:classic];
    
    self.viewControllers = controllers;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
