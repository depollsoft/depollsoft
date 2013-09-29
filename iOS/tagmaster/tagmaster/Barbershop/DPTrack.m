//
//  DPTrack.m
//  tagmaster
//
//  Created by David Poll on 3/16/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPTrack.h"

@implementation DPTrack

@synthesize title;
@synthesize source;

+ (DPTrack *)trackWithTitle:(NSString *)title source:(DPRemoteLocation *)source {
    DPTrack *track = [[DPTrack alloc] init];
    track.title = title;
    track.source = source;
    return track;
}

@end
