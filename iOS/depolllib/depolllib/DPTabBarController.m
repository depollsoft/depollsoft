//
//  DPTabBarController.m
//  depolllib
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTabBarController.h"

@interface DPTabBarController () <UITabBarDelegate>

@property (nonatomic, strong) UIView *rootView;

@end

@implementation DPTabBarController

@synthesize viewControllers, tabBar, rootView;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        tabBar = [[UITabBar alloc] init];
        tabBar.translatesAutoresizingMaskIntoConstraints = NO;
        tabBar.delegate = self;
        
        self.rootView = [[UIView alloc] init];
        self.rootView.translatesAutoresizingMaskIntoConstraints = NO;
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	
    [self.view addSubview:self.tabBar];
    [self.view addSubview:self.rootView];
    
    NSMutableDictionary *bindings = [NSMutableDictionary dictionaryWithDictionary:NSDictionaryOfVariableBindings(tabBar, rootView)];
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[rootView][tabBar]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:bindings]];
    [rootView.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor].active = YES;
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|->=0-[tabBar]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:bindings]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[rootView]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:bindings]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[tabBar]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:bindings]];
    [self.view layoutIfNeeded];
    [tabBar invalidateIntrinsicContentSize];
}
    
- (void)viewSafeAreaInsetsDidChange {
    [super viewSafeAreaInsetsDidChange];
    [self.view layoutIfNeeded];
    [tabBar invalidateIntrinsicContentSize];
}

- (void)setViewControllers:(NSArray *)vc {
    for (UIView *subview in self.rootView.subviews) {
        [subview removeFromSuperview];
    }
    for (UIViewController *controller in viewControllers) {
        [controller removeFromParentViewController];
    }
    viewControllers = vc;
    NSMutableArray *newItems = [NSMutableArray array];
    for (UIViewController *controller in viewControllers) {
        [self addChildViewController:controller];
        [newItems addObject:controller.tabBarItem];
    }
    tabBar.items = newItems;
    if (newItems.count == 0) {
        return;
    }
    [tabBar setSelectedItem:newItems[0]];
    [self tabBar:tabBar didSelectItem:newItems[0]];
}

- (void)tabBar:(UITabBar *)tabBar didSelectItem:(UITabBarItem *)item {
    for (UIViewController *controller in viewControllers) {
        if (controller.tabBarItem == item) {
            for (UIView *subview in self.rootView.subviews) {
                [subview removeFromSuperview];
            }
            [rootView removeConstraints:rootView.constraints];
            controller.view.translatesAutoresizingMaskIntoConstraints = NO;
            [rootView addSubview:controller.view];
            
            [rootView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[subview]|"
                                                                             options:0
                                                                             metrics:nil
                                                                               views:@{@"subview":controller.view}]];
            [rootView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[subview]|"
                                                                             options:0
                                                                             metrics:nil
                                                                               views:@{@"subview":controller.view}]];
        }
    }
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
