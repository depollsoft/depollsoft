//
//  DPToolbarViewController.m
//  depolllib
//
//  Created by David Poll on 12/17/17.
//  Copyright © 2017 DepollSoft. All rights reserved.
//

#import "DPToolbarViewController.h"

@implementation DPToolbarViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.edgesForExtendedLayout = UIRectEdgeNone;
}

- (UINavigationItem *)topNavigationItem {
    return self.navigationItem;
}

@end
