//
//  DPTagQueryViewController.h
//  tagmaster
//
//  Created by David Poll on 8/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPBarbershop.h"
#import "DPAppDelegate.h"

@interface DPTagQueryViewController : UIViewController <TMTagListSource>

@property (nonatomic) int maxResults;
@property (nonatomic, copy) NSString *statusText;
@property (nonatomic) enum DPTagCollection collection;
@property (nonatomic) BOOL hasMoreResults;
@property (atomic) BOOL isLoading;
@property (nonatomic, copy) NSString *query;
@property (nonatomic) int resultSetSize;
@property (nonatomic, retain) NSNumber *parts;
@property (nonatomic, retain) NSNumber *hasLearningTracks;
@property (nonatomic, retain) NSNumber *hasSheetMusic;
@property (nonatomic) enum DPTagSortOptions sortBy;
@property (nonatomic) NSArray *tags;
@property (nonatomic, retain) NSNumber *minRating;
@property (nonatomic, retain) NSNumber *minDownloads;

@end
