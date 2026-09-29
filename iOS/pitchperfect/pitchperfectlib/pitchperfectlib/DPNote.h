//
//  DPNote.h
//  pitchperfectlib
//
//  Created by David Poll on 5/4/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@class DPAccidental;

@interface DPNote : NSObject

+ (DPNote *)findNoteWithName:(NSString *)name accidental:(DPAccidental *)accidental octave:(int)octave;
+ (DPNote *)C4;
+ (NSArray *)commonNotes;
+ (NSArray *)prunedNotes;

/// The A4 notes sound at, in Hz (440 unless changed). A note already sounding
/// keeps its pitch until it is played again.
@property (class, nonatomic) double referencePitch;

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
@property (nonatomic, copy) NSString *friendlyName;
@property (nonatomic) BOOL isPlaying;
@property (nonatomic, strong) NSNumber *keyNumber;
@property (nonatomic) int octave;

@end
