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
- (void)stop;

@end
