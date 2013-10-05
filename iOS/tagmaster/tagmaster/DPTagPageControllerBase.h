//
//  DPTagPageControllerBase.h
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPBarbershop.h"
#import "UIButton+Hyperlink.h"
#import "DPGridLayout.h"
#import "DPBusyIndicator.h"

@interface DPTagPageControllerBase : UIViewController

@property (nonatomic, strong) DPBusyIndicator *busyIndicator;
@property (nonatomic, strong) DPTag *tag;
- (UILabel *)makeHeader:(NSString *)name;
- (UILabel *)makeBodyLabel;
- (UILabel *)makeTitleLabel;
- (void)refreshView;
- (void)setUpGrid:(DPGridLayout *)grid withScroller:(UIScrollView *)scroller;

@end
