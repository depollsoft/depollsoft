//
//  DPToolbarViewController.m
//  depolllib
//
//  Created by David Poll on 12/17/17.
//  Copyright © 2017 DepollSoft. All rights reserved.
//

#import "DPToolbarViewController.h"
#import <UIKit/UIKit.h>
#import <UIKit/NSLayoutAnchor.h>

@interface DPToolbarViewController () <UINavigationBarDelegate>

@end

@implementation DPToolbarViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    _topNavigationBar = [[UINavigationBar alloc] init];
    _topNavigationBar.delegate = self;
    _topNavigationBar.translatesAutoresizingMaskIntoConstraints = NO;

    _topNavigationItem = [[UINavigationItem alloc] init];
    [_topNavigationBar setItems:@[_topNavigationItem]];

    [self.view addSubview:_topNavigationBar];
    [_topNavigationBar.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor].active = YES;
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_topNavigationBar]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_topNavigationBar)]];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

- (UIBarPosition)positionForBar:(id<UIBarPositioning>)bar {
    return UIBarPositionTopAttached;
}

@end

