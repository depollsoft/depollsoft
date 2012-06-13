//
//  DPPitchedSong.h
//  pitchperfectlib
//
//  Created by David Poll on 5/4/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@class DPKey;

@interface DPPitchedSong : NSObject

- (void)play;
- (void)stop;
- (BOOL)isPlaying;

@property (nonatomic, copy) NSString *id;
@property (nonatomic, strong) DPKey *key;
@property (nonatomic, copy) NSString *name;

@end
