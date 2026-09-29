//
//  DPNote.h
//  pitchperfectlib
//
//  Created by David Poll on 5/4/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@class DPAccidental;
@class DPNote;

/// Plays notes in a MIDI instrument; the app provides one (see DPNote.instrumentPlayer).
@protocol DPNoteInstrumentPlayer <NSObject>
/// Starts `note` in the instrument `sound` at the current tuning. Returns a
/// token for stopNote:, or nil when it can't play (the note then plays in the
/// pitch pipe voice).
- (nullable id)startNote:(nonnull DPNote *)note sound:(nonnull NSString *)sound;
- (void)stopNote:(nonnull id)token;
@end

@interface DPNote : NSObject

+ (DPNote *)findNoteWithName:(NSString *)name accidental:(DPAccidental *)accidental octave:(int)octave;
+ (DPNote *)C4;
+ (NSArray *)commonNotes;
+ (NSArray *)prunedNotes;

/// The A4 notes sound at, in Hz (440 unless changed). A note already sounding
/// keeps its pitch until it is played again.
@property (class, nonatomic) double referencePitch;

/// The sound notes play in: a DPNoteSound id, the pitch pipe unless changed.
/// An id this version doesn't know reads as the pitch pipe. A note already
/// sounding keeps its sound until it is played again.
@property (class, nonatomic, copy, null_resettable) NSString *sound;

/// Plays notes whose sound is an instrument. Without one, instruments play
/// in the pitch pipe voice.
@property (class, nonatomic, strong, nullable) id<DPNoteInstrumentPlayer> instrumentPlayer;

- (id)initWithFriendlyName:(NSString *)friendlyName octave:(int)octave accidental:(DPAccidental *)accidental frequency:(double)frequency;
- (id)initWithFriendlyName:(NSString *)friendlyName octave:(int)octave accidental:(DPAccidental *)accidental keyNumber:(int)keyNumber;
- (void)play;
- (void)stop;

@property (nonatomic, strong) DPAccidental *accidental;
@property (nonatomic, strong) DPNote *alternate;
/// The note's frequency at A4 = 440 Hz, as stored with a song.
@property (nonatomic) double frequency;
/// The frequency the note sounds at, tuned to `referencePitch`.
@property (nonatomic, readonly) double tunedFrequency;
/// The note's MIDI key, from its A440 frequency (keyNumber is only set for octave 0).
@property (nonatomic, readonly) int midiKey;
@property (nonatomic, copy) NSString *friendlyName;
@property (nonatomic) BOOL isPlaying;
@property (nonatomic, strong) NSNumber *keyNumber;
@property (nonatomic) int octave;

@end
