//
//  DPKey.m
//  pitchperfectlib
//
//  Created by David Poll on 5/4/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPKey.h"
#import "DPAccidental.h"
#import "DPNote.h"
#import "DPKeyType.h"

static NSArray *majorKeyValues;
static NSArray *minorKeyValues;

@implementation DPKey

@synthesize keyType, note, numAccidentals;

+ (NSArray *)majorKeys {
    if (!majorKeyValues) {
        NSMutableArray *values = [NSMutableArray arrayWithCapacity:13];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Flat] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:-6]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Flat] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:-5]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Flat] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:-4]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"E" accidental:[DPAccidental enumWithInt:Flat] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:-3]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Flat] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:-2]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:-1]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:0]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:1]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:2]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:3]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"E" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:4]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:5]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Sharp] octave:4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:6]];
        majorKeyValues = [NSArray arrayWithArray:values];
    }
    return majorKeyValues;
}

+ (NSArray *)minorKeys {
    if (!minorKeyValues) {
        NSMutableArray *values = [NSMutableArray arrayWithCapacity:13];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"E" accidental:[DPAccidental enumWithInt:Flat] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:-6]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Flat] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:-5]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:-4]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:-3]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:-2]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:-1]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:0]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"E" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:1]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Natural] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:2]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Sharp] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:3]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:4]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Sharp] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:5]];
        [values addObject:[[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Sharp] octave:4] keyType:[DPKeyType enumWithInt:Minor] numAccidentals:6]];
        minorKeyValues = [NSArray arrayWithArray:values];
    }
    return minorKeyValues;
}

- (id)init {
    return [self initWithNote:[DPNote C4] keyType:[DPKeyType enumWithInt:Major] numAccidentals:0];
}

- (id)initWithNote:(DPNote *)newNote keyType:(DPKeyType *)type numAccidentals:(int)newNumAccidentals {
    if (self = [super init]) {
        self.note = newNote;
        self.keyType = type;
        self.numAccidentals = newNumAccidentals;
    }
    return self;
}

- (BOOL)isEqual:(id)object {
    if (![object isKindOfClass:[DPKey class]]) {
        return NO;
    }
    DPKey *key = object;
    return [key.note isEqual:self.note] && [key.keyType isEqual:self.keyType];
}

- (DPAccidental *)accidental {
    return self.note.accidental;
}

- (NSString *)friendlyName {
    switch ([self.keyType get]) {
        case Major:
            return self.note.friendlyName.uppercaseString;
            break;
        case Minor:
            return self.note.friendlyName.lowercaseString;
            break;
    }
    return @"";
}

@end
