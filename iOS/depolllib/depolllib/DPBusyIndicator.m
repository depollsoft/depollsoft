//
//  DPBusyIndicator.m
//  depolllib
//
//  Created by David Poll on 10/5/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPBusyIndicator.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"

@implementation DPBusyIndicator

- (id)init {
    if (self = [super init]) {
        [self commonInit];
    }
    return self;
}

- (id)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame]) {
        [self commonInit];
    }
    return self;
}

- (void)commonInit {
    DPGridLayout *overlay = [[DPGridLayout alloc] init];
    overlay.rowDimensions = @[
                              [DPGridDimension dimensionWithStars:1],
                              [DPGridDimension dimension],
                              [DPGridDimension dimension],
                              [DPGridDimension dimensionWithStars:1]
                              ];
    overlay.columnDimensions = @[
                                 [DPGridDimension dimensionWithStars:1],
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimensionWithStars:1]
                                 ];
    overlay.backgroundColor = [UIColor colorWithWhite:0.2 alpha:0.8];
    UIActivityIndicatorView *progressView = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
    [progressView startAnimating];
    progressView.translatesAutoresizingMaskIntoConstraints = NO;
    UILabel *label = [[UILabel alloc] init];
    [label setContentCompressionResistancePriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    label.translatesAutoresizingMaskIntoConstraints = NO;
    label.text = @"Loading...";
    label.textColor = [UIColor lightTextColor];
    [overlay addSubview:[progressView pad:8] row:1 column:1];
    [overlay addSubview:label row:2 column:1];
    self.overlay = overlay;
    [self updateVisibility];
}

- (void)setOverlay:(UIView *)overlay {
    [_overlay removeFromSuperview];
    _overlay = overlay;
    _overlay.translatesAutoresizingMaskIntoConstraints = NO;
    [self addSubview:_overlay];
    [self bringSubviewToFront:_overlay];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_overlay]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(_overlay)]];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[_overlay]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(_overlay)]];
    [self updateVisibility];
}

- (void)setChild:(UIView *)child {
    [_child removeFromSuperview];
    _child = child;
    _child.translatesAutoresizingMaskIntoConstraints = NO;
    [self addSubview:_child];
    [self sendSubviewToBack:_child];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_child]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(_child)]];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[_child]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(_child)]];
}

- (void)updateVisibility {
    [_overlay setHidden:_busyCount == 0];
    [_overlay setUserInteractionEnabled:_busyCount > 0];
    [self setUserInteractionEnabled:_busyCount > 0];
}

- (void)incrementBusyCount {
    _busyCount++;
    [self updateVisibility];
}

- (void)decrementBusyCount {
    if (_busyCount == 0) {
        return;
    }
    _busyCount--;
    [self updateVisibility];
}

- (void)clearBusyCount {
    _busyCount = 0;
    [self updateVisibility];
}

/*
// Only override drawRect: if you perform custom drawing.
// An empty implementation adversely affects performance during animation.
- (void)drawRect:(CGRect)rect
{
    // Drawing code
}
*/

@end
