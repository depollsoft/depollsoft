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

/// Starts the note. If the audio unit can't start (no output, a failed
/// session), the synthesizer stays stopped: isPlaying is NO and it doesn't
/// count as running.
- (void)start;
/// Stops the note. A wave fades out over 20 ms (882 samples at 44.1 kHz, on
/// a raised cosine) first, so it doesn't click; the pitch pipe stops at once,
/// as it always has.
- (void)stop;

/// Whether the note is sounding: started, not yet stopped, and its audio unit
/// really running.
@property (readonly) BOOL isPlaying;

/// How many synthesizers have their audio unit running (including a wave
/// still ramping out), so other players know whether the app is sounding.
+ (NSInteger)runningCount;

/// Called on the main queue whenever the last running synthesizer stops, so
/// whoever kept the audio session active for it can give it up.
+ (void)setOnLastStopped:(void (^)(void))block;

/// Whether notes start the real audio output (YES unless changed). The app's
/// hosted tests turn it off: starting the output unit on CI's audio-less
/// simulators has deadlocked. Without it a note still starts, counts as running
/// and stops as usual; it just isn't heard.
@property (class) BOOL usesAudioHardware;

@end
