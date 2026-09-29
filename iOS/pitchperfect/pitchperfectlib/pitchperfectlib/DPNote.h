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

/// Posted, with the note as the object, when a note stops or changes voice on
/// its own rather than through play or stop (see instrumentNoteEnded:failed:).
extern NSNotificationName const DPNotePlayingDidChangeNotification;

/// Plays notes in a MIDI instrument; the app provides one (see DPNote.instrumentPlayer).
@protocol DPNoteInstrumentPlayer <NSObject>
/// Starts `note` in the instrument `sound` at the current tuning. Returns a
/// token for stopNote:, or nil when it can't play (the note then plays in the
/// pitch pipe voice).
- (id)startNote:(DPNote *)note sound:(NSString *)sound NS_SWIFT_NAME(startNote(_:sound:));
- (void)stopNote:(id)token NS_SWIFT_NAME(stopNote(_:));
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
@property (class, nonatomic, copy) NSString *sound;

/// Plays notes whose sound is an instrument. Without one, instruments play
/// in the pitch pipe voice.
@property (class, nonatomic, strong) id<DPNoteInstrumentPlayer> instrumentPlayer;

- (id)initWithFriendlyName:(NSString *)friendlyName octave:(int)octave accidental:(DPAccidental *)accidental frequency:(double)frequency;
- (id)initWithFriendlyName:(NSString *)friendlyName octave:(int)octave accidental:(DPAccidental *)accidental keyNumber:(int)keyNumber;
- (void)play;
- (void)stop;

/// The instrument player calls this (on the main queue) when the note it
/// gave `token` for ends without being stopped: `failed` if it could never
/// play (the note then plays in the pitch pipe voice instead), otherwise
/// because another note took its sampler (the note stops). Posts
/// DPNotePlayingDidChangeNotification if the note's state changed.
- (void)instrumentNoteEnded:(id)token failed:(BOOL)failed;

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
