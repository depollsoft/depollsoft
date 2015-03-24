//
//  UIToolbar+DPUtils.m
//  depolllib
//
//  Created by David Poll on 3/23/15.
//  Copyright (c) 2015 DepollSoft. All rights reserved.
//

#import "UIToolbar+DPUtils.h"

@implementation UIToolbar (DPUtils)

- (UILabel *)addTitle:(NSString *)title {
    [self sizeToFit];
    UILabel *labelTitle = [[UILabel alloc] init];
    labelTitle.font = [UIFont fontWithName:@"Helvectica-Bold" size:18];
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
