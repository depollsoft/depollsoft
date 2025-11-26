//
//  UIToolbar+DPUtils.m
//  depolllib
//
//  Created by David Poll on 3/23/15.
//  Copyright (c) 2015 DepollSoft. All rights reserved.
//

#import <TargetConditionals.h>

#import "UIToolbar+DPUtils.h"

#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UILabel.h>
#import <UIKit/UIFont.h>
#import <UIKit/UIColor.h>
#import <UIKit/UIView.h>
#import <UIKit/UIToolbar.h>
#endif

#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
@implementation UIToolbar (DPUtils)

- (UILabel *)addTitle:(NSString *)title {
    [self sizeToFit];
    UILabel *labelTitle = [[UILabel alloc] init];
    labelTitle.font = [UIFont boldSystemFontOfSize:18];
    labelTitle.backgroundColor = [UIColor clearColor];
    labelTitle.textAlignment = NSTextAlignmentCenter;
    labelTitle.userInteractionEnabled = NO;
    labelTitle.text = title;
    [labelTitle sizeToFit];
    CGRect labelTitleFrame = labelTitle.frame;
    labelTitleFrame.size.width = self.bounds.size.width;
    labelTitleFrame.origin.y = (self.bounds.size.height - labelTitleFrame.size.height) / 2;
    labelTitle.frame = labelTitleFrame;
    labelTitle.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    // Just add UILabel like UIToolBar's subview
    [self addSubview:labelTitle];
    return labelTitle;
}

@end
#endif
