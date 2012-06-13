//
//  DPTrack.h
//  tagmaster
//
//  Created by David Poll on 3/16/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "DPRemoteLocation.h"

@interface DPTrack : NSObject

@property (nonatomic, copy) NSString *title;
@property (nonatomic, copy) DPRemoteLocation *source;

@end
