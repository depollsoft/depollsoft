//
//  UIView+DPUtils.h
//  depolllib
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>

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
