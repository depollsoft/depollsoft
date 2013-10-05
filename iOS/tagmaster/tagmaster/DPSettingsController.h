//
//  DPSettingsController.h
//  tagmaster
//
//  Created by David Poll on 9/30/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPTagPageControllerBase.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"

@interface DPSettingsController : DPTagPageControllerBase

+ (NSNumber *)minDownloads;
+ (NSNumber *)minRating;
+ (NSNumber *)sheetMusic;
+ (NSNumber *)learningTracks;

@end
