//
//  DPBrowseViewController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPBrowseViewController.h"
#import "DPTagQueryViewController.h"

@interface DPBrowseViewController ()

@end

@implementation DPBrowseViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        self.title = @"Tag Master";
        self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"Browse"
                                                                                 style:UIBarButtonItemStyleBordered
                                                                                target:nil
                                                                                action:nil];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
	NSMutableArray *controllers = [NSMutableArray array];
    
    DPTagQueryViewController *latest = [[DPTagQueryViewController alloc] init];
    latest.sortBy = DPTagSortPosted;
    latest.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Latest" image:nil selectedImage:nil];
    [controllers addObject:latest];
    
    DPTagQueryViewController *rating = [[DPTagQueryViewController alloc] init];
    rating.sortBy = DPTagSortRating;
    rating.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Rating" image:nil selectedImage:nil];
    [controllers addObject:rating];
    
    DPTagQueryViewController *downloads = [[DPTagQueryViewController alloc] init];
    downloads.sortBy = DPTagSortDownloaded;
    downloads.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Downloads" image:nil selectedImage:nil];
    [controllers addObject:downloads];
    
    DPTagQueryViewController *classic = [[DPTagQueryViewController alloc] init];
    classic.sortBy = DPTagSortClassic;
    classic.collection = DPTagCollectionClassicTags;
    classic.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Classic" image:nil selectedImage:nil];
    [controllers addObject:classic];
    
    self.viewControllers = controllers;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
