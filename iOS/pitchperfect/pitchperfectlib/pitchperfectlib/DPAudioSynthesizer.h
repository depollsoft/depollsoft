//
//  DPAudioSynthesizer.h
//  pitchperfectlib
//
//  Created by David Poll on 6/1/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPAudioSynthesizer : NSObject

/// The pitch pipe voice.
- (id)initWithFrequency:(double)newFrequency
             sampleRate:(int)newSampleRate;

/// `sound` (a DPNoteSound id) at `newFrequency` Hz. A wave plays that wave;
/// anything else plays the pitch pipe voice.
- (id)initWithFrequency:(double)newFrequency
             sampleRate:(int)newSampleRate
                  sound:(NSString *)sound;

- (void)start;
/// Stops the note. A wave ramps out over 5 ms first, so it doesn't click;
/// the pitch pipe stops at once, as it always has.
- (void)stop;

/// How many synthesizers have their audio unit running (including a wave
/// still ramping out), so other players know whether the app is sounding.
+ (NSInteger)runningCount;

@end
