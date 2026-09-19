//
//  DPTagSummaryController.h
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPBarbershop.h"
#import "DPTagPageControllerBase.h"

@interface DPTagSummaryController : DPTagPageControllerBase

// How long an accessibility-activated (untimed-touch) key note keeps sounding
// before it stops itself. Ships as 1.5 seconds; exposed only so tests can
// simulate the elapsed deadline without spending it in real wall clock. The
// getter's default is the shipping value, so app behaviour is unchanged.
@property (class, nonatomic) NSTimeInterval timedKeyNoteDuration;

@end
