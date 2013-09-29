//
//  DPTagCell.h
//  tagmaster
//
//  Created by David Poll on 8/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPBarbershop.h"

@interface DPTagCell : UITableViewCell

@property (nonatomic) int tagId;
@property (nonatomic, retain) DPTag *tag;
@property (nonatomic, strong) UIView *rootView;
- (CGFloat)calculatedHeight;
+ (CGFloat)withoutAkaHeight;
+ (CGFloat)withAkaHeight;
+ (CGFloat)tagHeight:(DPTag *)tag;

@end
