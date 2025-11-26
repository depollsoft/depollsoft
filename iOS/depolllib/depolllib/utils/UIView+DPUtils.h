//
//  UIView+DPUtils.h
//  depolllib
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#pragma once

#import <TargetConditionals.h>

// Only declare this category when UIKit is available.
#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UIView.h>

@interface UIView (DPUtils)

- (UIView *)pad:(CGFloat)amount;
- (UIView *)padHorizontal:(CGFloat)horizontal vertical:(CGFloat)vertical;
- (UIView *)padLeft:(CGFloat)left top:(CGFloat)top right:(CGFloat)right bottom:(CGFloat)bottom;
- (UIView *)centeredVertically;
- (UIView *)centeredHorizontally;
- (UIView *)centered;
- (UIView *)alignTop;
- (UIView *)alignLeft;
- (UIView *)alignBottom;
- (UIView *)alignRight;
- (UIView *)fixHeight:(CGFloat)height;
- (UIView *)fixWidth:(CGFloat)width;

@property (nonatomic, readonly) UILayoutGuide *safeAreaLayoutGuideCompat;
@property (nonatomic, readonly) UILayoutGuide *topSafeAreaLayoutGuide;
@property (nonatomic, readonly) UILayoutGuide *bottomSafeAreaLayoutGuide;
@property (nonatomic, readonly) UILayoutGuide *leftSafeAreaLayoutGuide;
@property (nonatomic, readonly) UILayoutGuide *rightSafeAreaLayoutGuide;

@end

#else
// Non-UIKit platforms: intentionally leave this header empty to avoid build errors
// when included by shared sources. The implementations are only available on UIKit.
#endif
