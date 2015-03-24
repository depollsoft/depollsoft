//
//  UIView+DPUtils.m
//  depolllib
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "UIView+DPUtils.h"

@implementation UIView (DPUtils)

- (UIView *)pad:(CGFloat)amount {
    return [self padLeft:amount top:amount right:amount bottom:amount];
}

- (UIView *)padHorizontal:(CGFloat)horizontal vertical:(CGFloat)vertical {
    return [self padLeft:horizontal top:vertical right:horizontal bottom:vertical];
}

- (UIView *)padLeft:(CGFloat)left top:(CGFloat)top right:(CGFloat)right bottom:(CGFloat)bottom {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    [view addConstraint:[NSLayoutConstraint constraintWithItem:self
                                                     attribute:NSLayoutAttributeLeft
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:view
                                                     attribute:NSLayoutAttributeLeft
                                                    multiplier:1
                                                      constant:left]];
    [view addConstraint:[NSLayoutConstraint constraintWithItem:self
                                                     attribute:NSLayoutAttributeRight
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:view
                                                     attribute:NSLayoutAttributeRight
                                                    multiplier:1
                                                      constant:-right]];
    [view addConstraint:[NSLayoutConstraint constraintWithItem:self
                                                     attribute:NSLayoutAttributeTop
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:view
                                                     attribute:NSLayoutAttributeTop
                                                    multiplier:1
                                                      constant:top]];
    [view addConstraint:[NSLayoutConstraint constraintWithItem:self
                                                     attribute:NSLayoutAttributeBottom
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:view
                                                     attribute:NSLayoutAttributeBottom
                                                    multiplier:1
                                                      constant:-bottom]];
    
    return view;
}

- (UIView *)centered {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    [view addConstraint:[NSLayoutConstraint constraintWithItem:self
                                                     attribute:NSLayoutAttributeCenterX
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:view
                                                     attribute:NSLayoutAttributeCenterX
                                                    multiplier:1
                                                      constant:0]];
    [view addConstraint:[NSLayoutConstraint constraintWithItem:self
                                                     attribute:NSLayoutAttributeCenterY
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:view
                                                     attribute:NSLayoutAttributeCenterY
                                                    multiplier:1
                                                      constant:0]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|->=0-[self]->=0-|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|->=0-[self]->=0-|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    
    return view;
}

- (UIView *)centeredVertically {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    [view addConstraint:[NSLayoutConstraint constraintWithItem:self
                                                     attribute:NSLayoutAttributeCenterY
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:view
                                                     attribute:NSLayoutAttributeCenterY
                                                    multiplier:1
                                                      constant:0]];
    
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|->=0-[self]->=0-|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    
    return view;
}

- (UIView *)centeredHorizontally {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    [view addConstraint:[NSLayoutConstraint constraintWithItem:self
                                                     attribute:NSLayoutAttributeCenterX
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:view
                                                     attribute:NSLayoutAttributeCenterX
                                                    multiplier:1
                                                      constant:0]];
    
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|->=0-[self]->=0-|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    
    return view;
}

- (UIView *)alignTop {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[self]->=0-|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    
    return view;
}

- (UIView *)alignBottom {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|->=0-[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    
    return view;
}

- (UIView *)alignLeft {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[self]->=0-|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    
    return view;
}

- (UIView *)alignRight {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|->=0-[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    
    return view;
}

- (UIView *)fixHeight:(CGFloat)height {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[self(==height)]|"
                                                                 options:0
                                                                 metrics:@{@"height": @(height)}
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    
    return view;
}

- (UIView *)fixWidth:(CGFloat)width {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    UIView *view = [[UIView alloc] init];
    [view addSubview:self];
    
    
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[self(==width)]|"
                                                                 options:0
                                                                 metrics:@{@"width": @(width)}
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    [view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[self]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(self)]];
    
    return view;
}

@end
