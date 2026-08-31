//
//  DPPitchedSong.m
//  pitchperfectlib
//
//  Created by David Poll on 5/4/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPPitchedSong.h"
#import "../../../depolllib/depolllib/utils/DPUtils+NSString.h"
#import "DPKey.h"
#import "DPNote.h"

@interface DPPitchedSong ()

@property (readonly) BOOL isPlaying;

@end

@implementation DPPitchedSong

@synthesize id, key, name, isPlaying;

- (id)init {
    if (self = [super init]) {
        self.id = [NSString stringWithUUID];
    }
    return self;
}

- (BOOL)isEqual:(id)object {
    if (![object isKindOfClass:[DPPitchedSong class]]) {
        return NO;
    }
    DPPitchedSong *song = object;
    return [song.id isEqual:self.id];
}

- (void)play {
    isPlaying = YES;
    [self.key.note play];
}

- (void)stop {
    isPlaying = NO;
    [self.key.note stop];
}

@end
