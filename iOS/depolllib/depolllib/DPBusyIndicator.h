//
//  DPBusyIndicator.h
//  depolllib
//
//  Created by David Poll on 10/5/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface DPBusyIndicator : UIView

@property (nonatomic, strong) UIView *child;
@property (nonatomic, readonly) NSUInteger busyCount;
@property (nonatomic, strong) UIView *overlay;

- (void)incrementBusyCount;
- (void)decrementBusyCount;

@end
