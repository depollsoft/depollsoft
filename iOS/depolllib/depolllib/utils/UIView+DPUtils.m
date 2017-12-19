//
//  UIView+DPUtils.m
//  depolllib
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "UIView+DPUtils.h"
#import <objc/runtime.h>

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

static char safeAreaLayoutGuideCompatKey;
- (UILayoutGuide *)safeAreaLayoutGuideCompat {
    if (@available(iOS 11.0, *)) {
        return self.safeAreaLayoutGuide;
    } else {
        UILayoutGuide *lg = objc_getAssociatedObject(self, &safeAreaLayoutGuideCompatKey);
        if (lg) {
            return lg;
        }
        lg = [[UILayoutGuide alloc] init];
        objc_setAssociatedObject(self, &safeAreaLayoutGuideCompatKey, lg, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
        
        [self addLayoutGuide:lg];
        [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[lg]|"
                                                                     options:0
                                                                     metrics:nil
                                                                       views:NSDictionaryOfVariableBindings(lg)]];
        [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[lg]|"
                                                                     options:0
                                                                     metrics:nil
                                                                       views:NSDictionaryOfVariableBindings(lg)]];
        return lg;
    }
}

static char topSafeAreaLayoutGuideKey;
- (UILayoutGuide *)topSafeAreaLayoutGuide {
    UILayoutGuide *lg = objc_getAssociatedObject(self, &topSafeAreaLayoutGuideKey);
    if (lg) {
        return lg;
    }
    lg = [[UILayoutGuide alloc] init];
    lg.identifier = @"Top Guide";
    [self addLayoutGuide:lg];
    id safeAreaLayoutGuide = self.safeAreaLayoutGuideCompat;
    
    [self addConstraint:[NSLayoutConstraint constraintWithItem:lg
                                                     attribute:NSLayoutAttributeTop
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self
                                                     attribute:NSLayoutAttributeTop
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraint:[NSLayoutConstraint constraintWithItem:lg
                                                     attribute:NSLayoutAttributeBottom
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:safeAreaLayoutGuide
                                                     attribute:NSLayoutAttributeTop
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[lg]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(lg)]];
    return lg;
}

static char bottomSafeAreaLayoutGuideKey;
- (UILayoutGuide *)bottomSafeAreaLayoutGuide {
    UILayoutGuide *lg = objc_getAssociatedObject(self, &bottomSafeAreaLayoutGuideKey);
    if (lg) {
        return lg;
    }
    lg = [[UILayoutGuide alloc] init];
    lg.identifier = @"Bottom Guide";
    [self addLayoutGuide:lg];
    id safeAreaLayoutGuide = self.safeAreaLayoutGuideCompat;
    
    [self addConstraint:[NSLayoutConstraint constraintWithItem:lg
                                                     attribute:NSLayoutAttributeBottom
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self
                                                     attribute:NSLayoutAttributeBottom
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraint:[NSLayoutConstraint constraintWithItem:lg
                                                     attribute:NSLayoutAttributeTop
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:safeAreaLayoutGuide
                                                     attribute:NSLayoutAttributeBottom
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[lg]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(lg)]];
    return lg;
}

static char leftSafeAreaLayoutGuideKey;
- (UILayoutGuide *)leftSafeAreaLayoutGuide {
    UILayoutGuide *lg = objc_getAssociatedObject(self, &leftSafeAreaLayoutGuideKey);
    if (lg) {
        return lg;
    }
    lg = [[UILayoutGuide alloc] init];
    lg.identifier = @"Left Guide";
    [self addLayoutGuide:lg];
    id safeAreaLayoutGuide = self.safeAreaLayoutGuideCompat;
    
    [self addConstraint:[NSLayoutConstraint constraintWithItem:lg
                                                     attribute:NSLayoutAttributeLeft
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self
                                                     attribute:NSLayoutAttributeLeft
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraint:[NSLayoutConstraint constraintWithItem:lg
                                                     attribute:NSLayoutAttributeRight
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:safeAreaLayoutGuide
                                                     attribute:NSLayoutAttributeLeft
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[lg]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(lg)]];
    return lg;
}

static char rightSafeAreaLayoutGuideKey;
- (UILayoutGuide *)rightSafeAreaLayoutGuide {
    UILayoutGuide *lg = objc_getAssociatedObject(self, &rightSafeAreaLayoutGuideKey);
    if (lg) {
        return lg;
    }
    lg = [[UILayoutGuide alloc] init];
    lg.identifier = @"Right Guide";
    [self addLayoutGuide:lg];
    id safeAreaLayoutGuide = self.safeAreaLayoutGuideCompat;
    
    [self addConstraint:[NSLayoutConstraint constraintWithItem:lg
                                                     attribute:NSLayoutAttributeRight
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self
                                                     attribute:NSLayoutAttributeRight
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraint:[NSLayoutConstraint constraintWithItem:lg
                                                     attribute:NSLayoutAttributeLeft
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:safeAreaLayoutGuide
                                                     attribute:NSLayoutAttributeRight
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[lg]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(lg)]];
    return lg;
}

@end
