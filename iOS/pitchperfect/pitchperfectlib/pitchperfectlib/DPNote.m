//
//  DPNote.m
//  pitchperfectlib
//
//  Created by David Poll on 5/4/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPNote.h"
#import "DPAccidental.h"
#import "DPAudioSynthesizer.h"
#import "DPNoteSound.h"
#import <AVKit/AVKit.h>

static DPNote *C4 = nil;
static NSArray *commonNotes = nil;
static NSArray *prunedNotes = nil;
static const double standardA4 = 440;
static double sReferencePitch = standardA4;
static NSString *sSound = nil;
static id<DPNoteInstrumentPlayer> sInstrumentPlayer = nil;

NSNotificationName const DPNotePlayingDidChangeNotification = @"DPNotePlayingDidChangeNotification";

@interface DPNote ()

@property (nonatomic, readonly) NSObject *synchronizer;
@property (nonatomic, readonly) DPAudioSynthesizer *synth;
/// The frequency `synth` was made for.
@property (nonatomic) double synthFrequency;
/// The sound `synth` was made for.
@property (nonatomic, copy) NSString *synthSound;
/// The instrument player's handle on this note while it sounds in an instrument.
@property (nonatomic, strong) id instrumentToken;
/// The player that gave out `instrumentToken`.
@property (nonatomic, strong) id<DPNoteInstrumentPlayer> tokenPlayer;

@end

@implementation DPNote

@synthesize accidental, friendlyName, octave, alternate, frequency, isPlaying, keyNumber, synchronizer, synth;

+ (DPNote *)findNoteWithName:(NSString *)name accidental:(DPAccidental *)accidental octave:(int)octave {
    for (DPNote *n in [DPNote commonNotes]) {
        if ([n.accidental isEqual:accidental] && n.octave == octave && [n.friendlyName isEqual:name]) {
            return n;
        }
    }
    return nil;
}

+ (DPNote *)C4 {
    if (!C4) {
        [DPNote commonNotes];
    }
    return C4;
}

+ (NSArray *)commonNotes {
    if (commonNotes) {
        return commonNotes;
    }
    NSMutableArray *temp = [NSMutableArray array];
    
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"C" octave:0 accidental:[DPAccidental enumWithInt:Natural] keyNumber:-8]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"C" octave:0 accidental:[DPAccidental enumWithInt:Sharp] keyNumber:-7]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"D" octave:0 accidental:[DPAccidental enumWithInt:Flat] keyNumber:-7]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"D" octave:0 accidental:[DPAccidental enumWithInt:Natural] keyNumber:-6]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"D" octave:0 accidental:[DPAccidental enumWithInt:Sharp] keyNumber:-5]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"E" octave:0 accidental:[DPAccidental enumWithInt:Flat] keyNumber:-5]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"E" octave:0 accidental:[DPAccidental enumWithInt:Natural] keyNumber:-4]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"F" octave:0 accidental:[DPAccidental enumWithInt:Natural] keyNumber:-3]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"F" octave:0 accidental:[DPAccidental enumWithInt:Sharp] keyNumber:-2]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"G" octave:0 accidental:[DPAccidental enumWithInt:Flat] keyNumber:-2]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"G" octave:0 accidental:[DPAccidental enumWithInt:Natural] keyNumber:-1]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"G" octave:0 accidental:[DPAccidental enumWithInt:Sharp] keyNumber:0]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"A" octave:0 accidental:[DPAccidental enumWithInt:Flat] keyNumber:0]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"A" octave:0 accidental:[DPAccidental enumWithInt:Natural] keyNumber:1]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"A" octave:0 accidental:[DPAccidental enumWithInt:Sharp] keyNumber:2]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"B" octave:0 accidental:[DPAccidental enumWithInt:Flat] keyNumber:2]];
    [temp addObject:[[DPNote alloc] initWithFriendlyName:@"B" octave:0 accidental:[DPAccidental enumWithInt:Natural] keyNumber:3]];
    
    NSUInteger originalCount = [temp count];
    for (int octave = 1; octave < 8; octave++) {
        for (int i = 0; i < originalCount; i++) {
            DPNote *cur = [temp objectAtIndex:i];
            DPNote *derived = [[DPNote alloc] initWithFriendlyName:cur.friendlyName octave:octave accidental:cur.accidental frequency:cur.frequency * pow(2, octave)];
            [temp addObject:derived];
            if (octave == 4 && [derived.friendlyName isEqual:@"C"] && derived.accidental.get == Natural) {
                C4 = derived;
            }
        }
    }
    
    commonNotes = [NSArray arrayWithArray:temp];
    return commonNotes;
}

+ (double)getNoteFrequency:(int)number {
    return 440 * pow(2, (number - 49) / 12.0);
}

+ (double)referencePitch {
    return sReferencePitch;
}

+ (void)setReferencePitch:(double)value {
    sReferencePitch = value;
}

+ (NSString *)sound {
    @synchronized([DPNote class]) {
        return sSound ?: DPNoteSoundPitchPipe;
    }
}

+ (void)setSound:(NSString *)sound {
    @synchronized([DPNote class]) {
        sSound = [[DPNoteSound validated:sound] copy];
    }
}

+ (id<DPNoteInstrumentPlayer>)instrumentPlayer {
    @synchronized([DPNote class]) {
        return sInstrumentPlayer;
    }
}

+ (void)setInstrumentPlayer:(id<DPNoteInstrumentPlayer>)player {
    @synchronized([DPNote class]) {
        sInstrumentPlayer = player;
    }
}

- (int)midiKey {
    return [DPNoteSound keyForA440Frequency:self.frequency];
}

- (double)tunedFrequency {
    return self.frequency * sReferencePitch / standardA4;
}

+ (NSArray *)prunedNotes {
    if (prunedNotes) {
        return prunedNotes;
    }
    NSMutableArray *temp = [NSMutableArray arrayWithArray:[DPNote commonNotes]];
    for (int i = 1; i < [temp count]; i++) {
        DPNote *cur = [temp objectAtIndex:i];
        DPNote *prev = [temp objectAtIndex:i - 1];
        if (prev.frequency == cur.frequency) {
            prev.alternate = cur;
            [temp removeObjectAtIndex:i];
            i--;
        }
    }
    prunedNotes = [NSArray arrayWithArray:temp];
    return prunedNotes;
}

- (id)init {
    if (self = [super init]) {
        synchronizer = [[NSObject alloc] init];
    }
    return self;
}

- (id)initWithFriendlyName:(NSString *)newFriendlyName octave:(int)newOctave accidental:(DPAccidental *)newAccidental frequency:(double)newFrequency {
    if (self = [self init]) {
        self.friendlyName = newFriendlyName;
        self.octave = newOctave;
        self.accidental = newAccidental;
        self.frequency = newFrequency;
    }
    return self;
}

- (id)initWithFriendlyName:(NSString *)newFriendlyName octave:(int)newOctave accidental:(DPAccidental *)newAccidental keyNumber:(int)newKeyNumber {
    if (self = [self init]) {
        self.friendlyName = newFriendlyName;
        self.octave = newOctave;
        self.accidental = newAccidental;
        self.keyNumber = [NSNumber numberWithInt:newKeyNumber];
    }
    return self;
}

- (BOOL)isEqual:(id)object {
    if (![object isKindOfClass:[DPNote class]]) {
        return NO;
    }
    DPNote *n = object;
    return [n.friendlyName isEqual:self.friendlyName] && [n.accidental isEqual:self.accidental] && n.octave == self.octave;
}

- (void)setKeyNumber:(NSNumber *)newKeyNumber {
    keyNumber = newKeyNumber;
    if (keyNumber) {
        self.frequency = [DPNote getNoteFrequency:[keyNumber intValue]];
    }
}

- (NSString *)description {
    switch (self.accidental.get) {
        case Natural:
            return self.friendlyName;
        case Sharp:
            return [self.friendlyName stringByAppendingString:@"#"];
        case Flat:
            return [self.friendlyName stringByAppendingString:@"b"];
    }
    return self.friendlyName;
}

- (void)play {
    @synchronized(self.synchronizer) {
        if(self.isPlaying) {
            return;
        }
        NSString *sound = DPNote.sound;
        id<DPNoteInstrumentPlayer> player = DPNote.instrumentPlayer;
        if ([DPNoteSound isInstrument:sound] && player) {
            id token = [player startNote:self sound:sound];
            if (token) {
                self.instrumentToken = token;
                self.tokenPlayer = player;
                self->isPlaying = YES;
                return;
            }
        }
        double tuned = self.tunedFrequency;
        if (!synth || self.synthFrequency != tuned || ![self.synthSound isEqualToString:sound]) {
            // Made again after the tuning or the sound changes.
            synth = [[DPAudioSynthesizer alloc] initWithFrequency:tuned sampleRate:44100 sound:sound];
            self.synthFrequency = tuned;
            self.synthSound = sound;
        }
        [synth start];
        // An output that won't start leaves the note silent, so it isn't lit.
        self->isPlaying = synth.isPlaying;
    }
}

- (void)instrumentNoteEnded:(id)token failed:(BOOL)failed {
    @synchronized(self.synchronizer) {
        if (!token || token != self.instrumentToken) {
            return;
        }
        self.instrumentToken = nil;
        self.tokenPlayer = nil;
        if (failed && self.isPlaying) {
            // Couldn't play the instrument: sound the note in the pitch pipe
            // voice instead, as when the player refuses it up front.
            double tuned = self.tunedFrequency;
            synth = [[DPAudioSynthesizer alloc] initWithFrequency:tuned sampleRate:44100 sound:DPNoteSoundPitchPipe];
            self.synthFrequency = tuned;
            self.synthSound = DPNoteSoundPitchPipe;
            [synth start];
            self->isPlaying = synth.isPlaying;
        } else {
            self->isPlaying = NO;
        }
    }
    [[NSNotificationCenter defaultCenter] postNotificationName:DPNotePlayingDidChangeNotification object:self];
}

- (void)stop {
    @synchronized(self.synchronizer) {
        if (self.instrumentToken) {
            [self.tokenPlayer stopNote:self.instrumentToken];
            self.instrumentToken = nil;
            self.tokenPlayer = nil;
        }
        [synth stop];
        self->isPlaying = NO;
    }
}

@end
