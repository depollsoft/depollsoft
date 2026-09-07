//
//  DPTagViewController.h
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPBarbershop.h"

@class DPTagPageControllerBase;

/// One tag, with its four sections in the order the Android app uses:
/// Summary, Details, Tracks, Videos. A pushed detail screen gets a segmented
/// control rather than a second tab bar; in regular width the summary stays on
/// screen beside the section being read.
@interface DPTagViewController : UIViewController

@property (nonatomic) int tagId;

/// The section controllers, in display order.
@property (nonatomic, copy) NSArray<DPTagPageControllerBase *> *viewControllers;

/// Section titles, in display order.
+ (NSArray<NSString *> *)sectionTitles;

/// The section on screen (index into `sectionTitles`).
@property (nonatomic, readonly) NSInteger selectedSectionIndex;

- (void)selectSectionAtIndex:(NSInteger)index;

@end
