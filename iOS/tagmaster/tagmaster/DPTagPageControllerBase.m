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
#import "tagmaster-Swift.h"

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

/// Field labels, body copy, and screen titles all ride Dynamic Type: the old
/// frozen 12pt and 24pt sizes ignored the reader's setting entirely.
- (UILabel *)makeHeader:(NSString *)name {
    UILabel *label = [[UILabel alloc] init];
    label.text = name;
    label.font = [TMTheme fieldLabelFont];
    label.adjustsFontForContentSizeCategory = YES;
    label.textColor = [TMTheme secondaryText];
    label.numberOfLines = 0;
    [label setContentCompressionResistancePriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    return label;
}

- (UILabel *)makeBodyLabel {
    UILabel *label = [[DPLabel alloc] init];
    label.font = [TMTheme bodyFont];
    label.adjustsFontForContentSizeCategory = YES;
    label.textColor = [TMTheme primaryText];
    return label;
}

- (UILabel *)makeTitleLabel {
    UILabel *titleLabel = [[UILabel alloc] init];
    titleLabel.font = [TMTheme screenTitleFont];
    titleLabel.adjustsFontForContentSizeCategory = YES;
    titleLabel.textColor = [TMTheme ink];
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
    view = [view padHorizontal:TMTheme.spaceL vertical:TMTheme.spaceS];
    view.translatesAutoresizingMaskIntoConstraints = NO;
    scroller.translatesAutoresizingMaskIntoConstraints = NO;
    NSDictionary *bindings = NSDictionaryOfVariableBindings(view);
    [scroller addSubview:view];
    [scroller addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|-4-[view]-4-|"
                                                                     options:0
                                                                     metrics:nil
                                                                       views:bindings]];
    
    // Content stays within a readable measure and centres itself on wide
    // screens instead of stretching a lyric across a full iPad.
    NSLayoutConstraint *fullWidth = [view.widthAnchor constraintEqualToAnchor:scroller.widthAnchor];
    fullWidth.priority = UILayoutPriorityRequired - 1;
    [NSLayoutConstraint activateConstraints:@[
        [view.centerXAnchor constraintEqualToAnchor:scroller.centerXAnchor],
        [view.widthAnchor constraintLessThanOrEqualToAnchor:scroller.widthAnchor],
        [view.widthAnchor constraintLessThanOrEqualToConstant:700],
        fullWidth
    ]];
    
    id leftGuide = self.view.leftSafeAreaLayoutGuide;
    id rightGuide = self.view.rightSafeAreaLayoutGuide;
    
    [self.view addSubview:scroller];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:[leftGuide][scroller][rightGuide]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(scroller, leftGuide, rightGuide)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[scroller]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(scroller)]];
    [scroller.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor].active = YES;
}

@end
