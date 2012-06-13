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

static DPNote *C4 = nil;
static NSArray *commonNotes = nil;
static NSArray *prunedNotes = nil;

@interface DPNote ()

@property (nonatomic, readonly) NSObject *synchronizer;
@property (nonatomic, strong) DPAudioSynthesizer *synth;

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
    
    int originalCount = [temp count];
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
        if (!self.synth) {
            self.synth = [[DPAudioSynthesizer alloc] initWithFrequency:self.frequency sampleRate:44100];
        }
        [synth start];
        self->isPlaying = YES;
    }
}

- (void)stop {
    @synchronized(self.synchronizer) {
        [synth stop];
        self->isPlaying = NO;
    }
}

@end
