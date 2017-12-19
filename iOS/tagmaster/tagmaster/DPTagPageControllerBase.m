//
//  DPTagPageControllerBase.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagPageControllerBase.h"
#import "DPLabel.h"
#import "UIView+DPUtils.h"

@interface DPTagPageControllerBase ()

@end

@implementation DPTagPageControllerBase

@synthesize tag;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (UILabel *)makeHeader:(NSString *)name {
    UILabel *label = [[UILabel alloc] init];
    label.text = name;
    label.font = [UIFont boldSystemFontOfSize:12];
    [label setContentCompressionResistancePriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    return label;
}

- (UILabel *)makeBodyLabel {
    UILabel *label = [[DPLabel alloc] init];
    label.font = [UIFont systemFontOfSize:12];
    return label;
}

- (UILabel *)makeTitleLabel {
    UILabel *titleLabel = [[UILabel alloc] init];
    titleLabel.font = [UIFont boldSystemFontOfSize:24];
    titleLabel.numberOfLines = 0;
    return titleLabel;
}

- (void)setTag:(DPTag *)t {
    tag = t;
    [self refreshView];
}

- (void)refreshView {
    // Override in child classes.
}

- (void)setUpRootView:(UIView *)view withScroller:(UIScrollView *)scroller {
    view = [view padHorizontal:8 vertical:0];
    id topLayoutGuide = self.topLayoutGuide;
    view.translatesAutoresizingMaskIntoConstraints = NO;
    scroller.translatesAutoresizingMaskIntoConstraints = NO;
    NSDictionary *bindings = NSDictionaryOfVariableBindings(view);
    [scroller addSubview:view];
    [scroller addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-4-[view]-4-|"
                                                                     options:0
                                                                     metrics:nil
                                                                       views:bindings]];
    
    [scroller addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[view]|"
                                                                     options:0
                                                                     metrics:nil
                                                                       views:bindings]];
    [scroller addConstraint:[NSLayoutConstraint constraintWithItem:view
                                                         attribute:NSLayoutAttributeWidth
                                                         relatedBy:NSLayoutRelationEqual
                                                            toItem:scroller
                                                         attribute:NSLayoutAttributeWidth
                                                        multiplier:1
                                                          constant:0]];
    
    id leftGuide = self.view.leftSafeAreaLayoutGuide;
    id rightGuide = self.view.rightSafeAreaLayoutGuide;
    
    [self.view addSubview:scroller];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:[leftGuide][scroller][rightGuide]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(scroller, leftGuide, rightGuide)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[topLayoutGuide][scroller]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(scroller, topLayoutGuide)]];
}

@end
