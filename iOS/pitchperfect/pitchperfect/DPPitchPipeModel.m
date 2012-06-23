//
//  DPPitchPipeModel.m
//  pitchperfect
//
//  Created by David Poll on 6/12/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPPitchPipeModel.h"
#import "DPNote.h"
#import "DPAccidental.h"

#define F_TO_F_KEY @"depollsoft.pitchperfect.isFToF"

@interface DPPitchPipeModel ()

@property (nonatomic, strong) NSArray *cToC;
@property (nonatomic, strong) NSArray *fToF;

@end

@implementation DPPitchPipeModel

@synthesize cToC, fToF;

- (BOOL)isFromFToF {
    return [[NSUserDefaults standardUserDefaults] boolForKey:F_TO_F_KEY];
}

- (void)setIsFromFToF:(BOOL)isFromFToF {
    [[NSUserDefaults standardUserDefaults] setBool:isFromFToF forKey:F_TO_F_KEY];
}

- (id)init {
    if (self = [super init]) {        
        NSMutableArray *temp;
        temp = [NSMutableArray arrayWithCapacity:12];
        [temp addObject:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Sharp] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"E" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Sharp] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Sharp] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Sharp] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        self.cToC = [NSArray arrayWithArray:temp];
        
        temp = [NSMutableArray arrayWithCapacity:12];
        [temp addObject:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Sharp] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Sharp] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Sharp] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Natural] octave:4]];
        [temp addObject:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:5]];
        [temp addObject:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:5]];
        [temp addObject:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Natural] octave:5]];
        [temp addObject:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Sharp] octave:5]];
        [temp addObject:[DPNote findNoteWithName:@"E" accidental:[DPAccidental enumWithInt:Natural] octave:5]];
        self.fToF = [NSArray arrayWithArray:temp];
    }
    return self;
}

- (NSArray *)notes {
    if (self.isFromFToF) {
        return self.fToF;
    } else {
        return self.cToC;
    }
}

@end
