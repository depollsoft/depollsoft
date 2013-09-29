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

+ (DPTrack *)trackWithTitle:(NSString *)title source:(DPRemoteLocation *)source;

@property (nonatomic, copy) NSString *title;
@property (nonatomic, strong) DPRemoteLocation *source;

@end
