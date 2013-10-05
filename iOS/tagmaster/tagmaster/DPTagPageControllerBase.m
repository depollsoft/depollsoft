//
//  DPTagPageControllerBase.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagPageControllerBase.h"
#import "DPLabel.h"

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

- (void)setUpGrid:(DPGridLayout *)grid withScroller:(UIScrollView *)scroller {
    id topLayoutGuide = self.topLayoutGuide;
    grid.translatesAutoresizingMaskIntoConstraints = NO;
    scroller.translatesAutoresizingMaskIntoConstraints = NO;
    NSDictionary *bindings = NSDictionaryOfVariableBindings(grid);
    [scroller addSubview:grid];
    [scroller addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-4-[grid]-4-|"
                                                                     options:0
                                                                     metrics:nil
                                                                       views:bindings]];
    
    [scroller addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[grid]|"
                                                                     options:0
                                                                     metrics:nil
                                                                       views:bindings]];
    [scroller addConstraint:[NSLayoutConstraint constraintWithItem:grid
                                                         attribute:NSLayoutAttributeWidth
                                                         relatedBy:NSLayoutRelationEqual
                                                            toItem:scroller
                                                         attribute:NSLayoutAttributeWidth
                                                        multiplier:1
                                                          constant:0]];
    
    [self.view addSubview:scroller];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|-4-[scroller]-4-|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(scroller)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[topLayoutGuide][scroller]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(scroller, topLayoutGuide)]];
}

@end
