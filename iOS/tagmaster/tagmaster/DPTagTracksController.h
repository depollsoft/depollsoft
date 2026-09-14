//
//  DPTagTracksController.h
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPTagPageControllerBase.h"

@class TMTrackPlaybackSession;

@interface DPTagTracksController : DPTagPageControllerBase

// Owned here so Objective-C lifecycle and Swift playback share one cancellation path.
@property (nonatomic, strong) TMTrackPlaybackSession *playbackSession;
@property (nonatomic) BOOL playbackHasLeft;

@end
