//
//  DPGridLayout.m
//  depolllib
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPGridLayout.h"

@implementation DPGridDimension

+ (DPGridDimension *)dimensionWithSize:(CGFloat)size {
    DPGridDimension *dimension = [[DPGridDimension alloc] init];
    dimension.size = @(size);
    return dimension;
}

+ (DPGridDimension *)dimensionWithStars:(CGFloat)stars {
    DPGridDimension *dimension = [[DPGridDimension alloc] init];
    dimension.stars = @(stars);
    return dimension;
}

+ (DPGridDimension *)dimension {
    return [[DPGridDimension alloc] init];
}

@end

@interface DPLayoutInfo : NSObject

@property (nonatomic) NSUInteger row;
@property (nonatomic) NSUInteger column;
@property (nonatomic) NSUInteger rowSpan;
@property (nonatomic) NSUInteger colSpan;

@end

@implementation DPLayoutInfo

@end

@interface DPGridLayout ()

@property (nonatomic, strong) NSMutableArray *layoutInfo;
@property (nonatomic, strong) NSMutableArray *children;
@property (nonatomic, strong) NSMutableArray *hiddenChildren;
@property (nonatomic, strong) NSMutableArray *hiddenLayoutInfo;
@property (nonatomic, strong) NSMutableArray *rowSpacers;
@property (nonatomic, strong) NSMutableArray *colSpacers;
@property (nonatomic, strong) UIView *starSpacer;

- (void)addSubview:(UIView *)view layoutInfo:(DPLayoutInfo *)layoutInfo;

@end

@implementation DPGridLayout

@synthesize rowDimensions, columnDimensions;

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        self.layoutInfo = [NSMutableArray array];
        self.children = [NSMutableArray array];
        self.hiddenChildren = [NSMutableArray array];
        self.hiddenLayoutInfo = [NSMutableArray array];
        self.starSpacer = [[UIView alloc] init];
        self.starSpacer.translatesAutoresizingMaskIntoConstraints = NO;
    }
    return self;
}

- (void)setRowDimensions:(NSArray *)dimensions {
    rowDimensions = dimensions;
    [self invalidateLayout];
}

- (void)setColumnDimensions:(NSArray *)dimensions {
    columnDimensions = dimensions;
    [self invalidateLayout];
}

- (void)invalidateLayout {
    for (UIView *subView in self.subviews) {
        [subView removeFromSuperview];
    }
    [self removeConstraints:self.constraints];
    
    [self addSubview:self.starSpacer];
    
    NSLayoutConstraint *starWidthConstraint = [NSLayoutConstraint constraintWithItem:self.starSpacer
                                                                           attribute:NSLayoutAttributeWidth
                                                                           relatedBy:NSLayoutRelationGreaterThanOrEqual
                                                                              toItem:nil
                                                                           attribute:NSLayoutAttributeNotAnAttribute
                                                                          multiplier:1
                                                                            constant:100000];
    starWidthConstraint.priority = 1;
    [self addConstraint:starWidthConstraint];
    NSLayoutConstraint *starHeightConstraint = [NSLayoutConstraint constraintWithItem:self.starSpacer
                                                                            attribute:NSLayoutAttributeHeight
                                                                            relatedBy:NSLayoutRelationGreaterThanOrEqual
                                                                               toItem:nil
                                                                            attribute:NSLayoutAttributeNotAnAttribute
                                                                           multiplier:1
                                                                             constant:100000];
    starHeightConstraint.priority = 1;
    [self addConstraint:starHeightConstraint];
    
    // Ensure at least one row/column
    if (self.rowDimensions.count == 0) {
        rowDimensions = @[[[DPGridDimension alloc] init]];
    }
    if (self.columnDimensions.count == 0) {
        columnDimensions = @[[[DPGridDimension alloc] init]];
    }
    
    self.rowSpacers = [NSMutableArray array];
    self.colSpacers = [NSMutableArray array];
    
    // Add column constraints
    for (DPGridDimension *dimension in self.columnDimensions) {
        UIView *spacer = [[UIView alloc] init];
        spacer.translatesAutoresizingMaskIntoConstraints = NO;
        [self addSubview:spacer];
        
        if (self.colSpacers.count > 0) {
            // Trail the previous spacer
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeLeft
                                                             relatedBy:NSLayoutRelationEqual
                                                                toItem:self.colSpacers.lastObject
                                                             attribute:NSLayoutAttributeRight
                                                            multiplier:1
                                                              constant:0]];
        }
        
        if (dimension.stars) {
            // Add star constraints
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeWidth
                                                             relatedBy:NSLayoutRelationEqual
                                                                toItem:self.starSpacer
                                                             attribute:NSLayoutAttributeWidth
                                                            multiplier:dimension.stars.doubleValue
                                                              constant:0]];
        }
        
        if (dimension.size) {
            // Add an explicit sizing
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeWidth
                                                             relatedBy:NSLayoutRelationEqual
                                                                toItem:nil
                                                             attribute:NSLayoutAttributeNotAnAttribute
                                                            multiplier:1
                                                              constant:dimension.size.doubleValue]];
        }
        
        if (dimension.minSize) {
            // Add a minimum sizing
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeWidth
                                                             relatedBy:NSLayoutRelationGreaterThanOrEqual
                                                                toItem:nil
                                                             attribute:NSLayoutAttributeNotAnAttribute
                                                            multiplier:1
                                                              constant:dimension.minSize.doubleValue]];
        }
        
        if (dimension.maxSize) {
            // Add a minimum sizing
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeWidth
                                                             relatedBy:NSLayoutRelationLessThanOrEqual
                                                                toItem:nil
                                                             attribute:NSLayoutAttributeNotAnAttribute
                                                            multiplier:1
                                                              constant:dimension.maxSize.doubleValue]];
        }
        
        [self.colSpacers addObject:spacer];
    }
    
    // Fill the view's width
    [self addConstraint:[NSLayoutConstraint constraintWithItem:self.colSpacers.firstObject
                                                     attribute:NSLayoutAttributeLeft
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self
                                                     attribute:NSLayoutAttributeLeft
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraint:[NSLayoutConstraint constraintWithItem:self.colSpacers.lastObject
                                                     attribute:NSLayoutAttributeRight
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self
                                                     attribute:NSLayoutAttributeRight
                                                    multiplier:1
                                                      constant:0]];
    
    // Add row constraints
    for (DPGridDimension *dimension in self.rowDimensions) {
        UIView *spacer = [[UIView alloc] init];
        spacer.translatesAutoresizingMaskIntoConstraints = NO;
        [self addSubview:spacer];
        
        if (self.rowSpacers.count > 0) {
            // Trail the previous spacer
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeTop
                                                             relatedBy:NSLayoutRelationEqual
                                                                toItem:self.rowSpacers.lastObject
                                                             attribute:NSLayoutAttributeBottom
                                                            multiplier:1
                                                              constant:0]];
        }
        
        if (dimension.stars) {
            // Add star constraints
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeHeight
                                                             relatedBy:NSLayoutRelationEqual
                                                                toItem:self.starSpacer
                                                             attribute:NSLayoutAttributeHeight
                                                            multiplier:dimension.stars.doubleValue
                                                              constant:0]];
        }
        
        if (dimension.size) {
            // Add an explicit sizing
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeHeight
                                                             relatedBy:NSLayoutRelationEqual
                                                                toItem:nil
                                                             attribute:NSLayoutAttributeNotAnAttribute
                                                            multiplier:1
                                                              constant:dimension.size.doubleValue]];
        }
        
        if (dimension.minSize) {
            // Add a minimum sizing
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeHeight
                                                             relatedBy:NSLayoutRelationGreaterThanOrEqual
                                                                toItem:nil
                                                             attribute:NSLayoutAttributeNotAnAttribute
                                                            multiplier:1
                                                              constant:dimension.minSize.doubleValue]];
        }
        
        if (dimension.maxSize) {
            // Add a minimum sizing
            [self addConstraint:[NSLayoutConstraint constraintWithItem:spacer
                                                             attribute:NSLayoutAttributeHeight
                                                             relatedBy:NSLayoutRelationLessThanOrEqual
                                                                toItem:nil
                                                             attribute:NSLayoutAttributeNotAnAttribute
                                                            multiplier:1
                                                              constant:dimension.maxSize.doubleValue]];
        }
        
        [self.rowSpacers addObject:spacer];
    }
    
    // Fill the view's height
    [self addConstraint:[NSLayoutConstraint constraintWithItem:self.rowSpacers.firstObject
                                                     attribute:NSLayoutAttributeTop
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self
                                                     attribute:NSLayoutAttributeTop
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraint:[NSLayoutConstraint constraintWithItem:self.rowSpacers.lastObject
                                                     attribute:NSLayoutAttributeBottom
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self
                                                     attribute:NSLayoutAttributeBottom
                                                    multiplier:1
                                                      constant:0]];
    
    NSMutableArray *oldLayoutInfos = self.layoutInfo;
    NSMutableArray *oldChildren = self.children;
    self.layoutInfo = [NSMutableArray array];
    self.children = [NSMutableArray array];
    for (int i = 0; i < oldLayoutInfos.count; i++) {
        [self addSubview:oldChildren[i] layoutInfo:oldLayoutInfos[i]];
    }
}

- (void)addSubview:(UIView *)view layoutInfo:(DPLayoutInfo *)layoutInfo {
    view.translatesAutoresizingMaskIntoConstraints = NO;
    [self addSubview:view];
    [self.children addObject:view];
    [self.layoutInfo addObject:layoutInfo];
    unsigned long topIndex = MAX(layoutInfo.row, 0);
    unsigned long bottomIndex = MIN(layoutInfo.row + layoutInfo.rowSpan - 1, self.rowDimensions.count);
    unsigned long leftIndex = MAX(layoutInfo.column, 0);
    unsigned long rightIndex = MIN(layoutInfo.column + layoutInfo.colSpan - 1, self.columnDimensions.count);
    
    [self addConstraint:[NSLayoutConstraint constraintWithItem:view
                                                     attribute:NSLayoutAttributeLeft
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self.colSpacers[leftIndex]
                                                     attribute:NSLayoutAttributeLeft
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraint:[NSLayoutConstraint constraintWithItem:view
                                                     attribute:NSLayoutAttributeRight
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self.colSpacers[rightIndex]
                                                     attribute:NSLayoutAttributeRight
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraint:[NSLayoutConstraint constraintWithItem:view
                                                     attribute:NSLayoutAttributeTop
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self.rowSpacers[topIndex]
                                                     attribute:NSLayoutAttributeTop
                                                    multiplier:1
                                                      constant:0]];
    [self addConstraint:[NSLayoutConstraint constraintWithItem:view
                                                     attribute:NSLayoutAttributeBottom
                                                     relatedBy:NSLayoutRelationEqual
                                                        toItem:self.rowSpacers[bottomIndex]
                                                     attribute:NSLayoutAttributeBottom
                                                    multiplier:1
                                                      constant:0]];
}

- (void)setView:(UIView *)view hidden:(BOOL)isHidden {
    NSUInteger childrenIndex = NSNotFound;
    NSUInteger hiddenChildrenIndex = NSNotFound;
    while (view &&
           !((childrenIndex = [self.children indexOfObjectIdenticalTo:view]) != NSNotFound ||
             (hiddenChildrenIndex = [self.hiddenChildren indexOfObjectIdenticalTo:view]) != NSNotFound)) {
        view = view.superview;
    }
    if (isHidden) {
        NSUInteger index = childrenIndex;
        if (index != NSNotFound) {
            [view removeFromSuperview];
            [self.hiddenLayoutInfo addObject:self.layoutInfo[index]];
            [self.hiddenChildren addObject:view];
            [self.children removeObjectAtIndex:index];
            [self.layoutInfo removeObjectAtIndex:index];
        }
    } else {
        NSUInteger index = hiddenChildrenIndex;
        if (index != NSNotFound) {
            [view removeFromSuperview];
            [self addSubview:view layoutInfo:self.hiddenLayoutInfo[index]];
            [self.hiddenChildren removeObjectAtIndex:index];
            [self.hiddenLayoutInfo removeObjectAtIndex:index];
        }
    }
}

- (void)addSubview:(UIView *)subView row:(NSUInteger)row column:(NSUInteger)col {
    [self addSubview:subView row:row column:col rowSpan:1 colSpan:1];
    
}

- (void)addSubview:(UIView *)subView row:(NSUInteger)row column:(NSUInteger)col rowSpan:(NSUInteger)rowSpan colSpan:(NSUInteger)colSpan {
    DPLayoutInfo *info = [[DPLayoutInfo alloc] init];
    info.row = row;
    info.column = col;
    info.rowSpan = rowSpan;
    info.colSpan = colSpan;
    [self addSubview:subView layoutInfo:info];
}

@end
