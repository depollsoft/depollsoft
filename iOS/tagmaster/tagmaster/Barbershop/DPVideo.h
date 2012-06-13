//
//  DPVideo.h
//  tagmaster
//
//  Created by David Poll on 3/16/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPVideo : NSObject

@property (nonatomic) int videoId;
@property (nonatomic, copy) NSString *description;
@property (nonatomic, copy) NSString *sungKey;
@property (nonatomic) BOOL isMultitrack;
@property (nonatomic, copy) NSString *youTubeCode;
@property (nonatomic, copy) NSString *sungBy;
@property (nonatomic, copy) NSURL *sungWebsite;
@property (nonatomic, copy) NSDate *posted;

@end
