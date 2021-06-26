//
//  DPToolbarViewController.m
//  depolllib
//
//  Created by David Poll on 12/17/17.
//  Copyright © 2017 DepollSoft. All rights reserved.
//

#import "DPToolbarViewController.h"

@interface DPToolbarViewController () <UIToolbarDelegate>

@end

@implementation DPToolbarViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    
    _toolbar = [[UIToolbar alloc] init];
    _toolbar.delegate = self;
    _toolbar.translatesAutoresizingMaskIntoConstraints = NO;
    
    [self.view addSubview:_toolbar];
    [_toolbar.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor].active = YES;
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_toolbar]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_toolbar)]];
    
    [_toolbar sizeToFit];
    [_toolbar layoutIfNeeded];
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
