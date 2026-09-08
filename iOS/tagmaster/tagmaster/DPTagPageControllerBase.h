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

@interface UIViewController (TMRecovery)
- (void)tm_showError:(NSString *)message retry:(void (^)(void))retry;
@end

/// Counts in-flight work without covering any view. Hosts observe the count and
/// show inline progress (a bar-button spinner, a busy row, a busy button) so the
/// navigation bar and the rest of the screen stay usable while a request runs.
/// A configured button whose height follows its wrapped title at the actual width.
/// A plain configured button measures its title against a zero width and collapses.
@interface TMWrappingButton : UIButton
@end

@interface TMBusyIndicator : DPBusyIndicator
@property (nonatomic, copy) void (^onBusyCountChanged)(NSUInteger busyCount);
@end

@interface DPTagPageControllerBase : UIViewController

@property (nonatomic, strong) DPBusyIndicator *busyIndicator;
@property (nonatomic, strong) DPTag *tag;
- (UILabel *)makeHeader:(NSString *)name;
- (UIView *)makeFilterControl:(UISegmentedControl *)control label:(NSString *)label;
- (UILabel *)makeBodyLabel;
- (UILabel *)makeTitleLabel;
/// An inset-grouped form row: a subheadline label above a full-width control.
- (UITableViewCell *)makeFormCellWithHeader:(NSString *)header control:(UIView *)control;
- (void)refreshView;
- (void)setUpRootView:(UIView *)view withScroller:(UIScrollView *)scroller;

@end
