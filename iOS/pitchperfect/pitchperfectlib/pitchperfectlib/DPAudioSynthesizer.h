//
//  DPAudioSynthesizer.h
//  pitchperfectlib
//
//  Created by David Poll on 6/1/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPAudioSynthesizer : NSObject

- (id)initWithFrequency:(double)newFrequency
             sampleRate:(int)newSampleRate;

- (void)start;
- (void)stop;

@end
