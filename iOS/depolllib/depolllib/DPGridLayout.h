//
//  DPGridLayout.h
//  depolllib
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#pragma once

#import <TargetConditionals.h>

#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UIView.h>

@interface DPGridDimension : NSObject

@property (nonatomic, strong) NSNumber *stars;
@property (nonatomic, strong) NSNumber *size;
@property (nonatomic, strong) NSNumber *minSize;
@property (nonatomic, strong) NSNumber *maxSize;

+ (DPGridDimension *)dimensionWithStars:(CGFloat)stars;
+ (DPGridDimension *)dimensionWithSize:(CGFloat)size;
+ (DPGridDimension *)dimension;

@end

@interface DPGridLayout : UIView

@property (nonatomic, copy) NSArray *rowDimensions;
@property (nonatomic, copy) NSArray *columnDimensions;

- (void)addSubview:(UIView *)subView row:(NSUInteger)row column:(NSUInteger)col rowSpan:(NSUInteger)rowSpan colSpan:(NSUInteger)colSpan;
- (void)addSubview:(UIView *)subView row:(NSUInteger)row column:(NSUInteger)col;

- (void)setView:(UIView *)view hidden:(BOOL)isHidden;

- (void)invalidateLayout;

@end

#else
// Non-UIKit platforms: intentionally left empty to avoid build errors when included by shared code.
#endif
