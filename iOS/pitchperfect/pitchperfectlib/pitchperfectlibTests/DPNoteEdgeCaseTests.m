//
//  DPNoteEdgeCaseTests.m
//  pitchperfectlibTests
//
//  Additional edge case and coverage tests for DPNote
//

#import <XCTest/XCTest.h>
#import "DPNote.h"
#import "DPAccidental.h"

@interface DPNoteEdgeCaseTests : XCTestCase
@end

@implementation DPNoteEdgeCaseTests

#pragma mark - Common Notes Population Tests

- (void)testCommonNotesPopulatedOnFirstAccess {
    // Access common notes and verify it's populated
    NSArray *notes = [DPNote commonNotes];
    XCTAssertNotNil(notes);
    XCTAssertGreaterThan(notes.count, 0);
}

- (void)testCommonNotesContainAllNaturalNotes {
    NSArray *notes = [DPNote commonNotes];
    NSArray *naturalNames = @[@"C", @"D", @"E", @"F", @"G", @"A", @"B"];
    
    for (NSString *name in naturalNames) {
        BOOL found = NO;
        for (DPNote *note in notes) {
            if ([note.friendlyName isEqualToString:name] && note.accidental.get == Natural) {
                found = YES;
                break;
            }
        }
        XCTAssertTrue(found, @"Should find natural %@", name);
    }
}

- (void)testCommonNotesContainSharpsAndFlats {
    NSArray *notes = [DPNote commonNotes];
    
    BOOL foundSharp = NO;
    BOOL foundFlat = NO;
    
    for (DPNote *note in notes) {
        if (note.accidental.get == Sharp) foundSharp = YES;
        if (note.accidental.get == Flat) foundFlat = YES;
        if (foundSharp && foundFlat) break;
    }
    
    XCTAssertTrue(foundSharp, @"Should contain sharp notes");
    XCTAssertTrue(foundFlat, @"Should contain flat notes");
}

#pragma mark - C4 Reference Note Tests

- (void)testC4ReturnsValidNote {
    DPNote *c4 = [DPNote C4];
    XCTAssertNotNil(c4);
    XCTAssertEqualObjects(c4.friendlyName, @"C");
    XCTAssertEqual(c4.octave, 4);
    XCTAssertEqual(c4.accidental.get, Natural);
}

- (void)testC4IsCached {
    DPNote *first = [DPNote C4];
    DPNote *second = [DPNote C4];
    XCTAssertEqual(first, second, @"C4 should return same cached instance");
}

- (void)testC4FrequencyIsCorrect {
    DPNote *c4 = [DPNote C4];
    // Middle C is approximately 261.63 Hz
    XCTAssertEqualWithAccuracy(c4.frequency, 261.63, 2.0, @"C4 frequency should be around 261.63 Hz");
}

#pragma mark - Pruned Notes Tests

- (void)testPrunedNotesRemovesDuplicates {
    NSArray *common = [DPNote commonNotes];
    NSArray *pruned = [DPNote prunedNotes];
    
    XCTAssertLessThan(pruned.count, common.count, @"Pruned should have fewer notes than common");
}

- (void)testPrunedNotesLinksAlternates {
    NSArray *pruned = [DPNote prunedNotes];
    
    int alternateCount = 0;
    for (DPNote *note in pruned) {
        if (note.alternate != nil) {
            alternateCount++;
            // Verify alternate has same frequency
            XCTAssertEqual(note.frequency, note.alternate.frequency);
        }
    }
    
    XCTAssertGreaterThan(alternateCount, 0, @"Should have some notes with alternates");
}

- (void)testPrunedNotesEachHasUniqueFrequency {
    NSArray *pruned = [DPNote prunedNotes];
    NSMutableSet *frequencies = [NSMutableSet set];
    
    for (DPNote *note in pruned) {
        NSNumber *freq = @(note.frequency);
        XCTAssertFalse([frequencies containsObject:freq], @"Pruned notes should have unique frequencies");
        [frequencies addObject:freq];
    }
}

#pragma mark - Key Number and Frequency Tests

- (void)testKeyNumberCalculatesFrequencyCorrectly {
    // A4 (key number 49) = 440 Hz
    DPNote *a4 = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] keyNumber:49];
    XCTAssertEqualWithAccuracy(a4.frequency, 440.0, 0.001);
}

- (void)testKeyNumberUpdatesFrequencyWhenChanged {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"X" octave:0 accidental:[DPAccidental enumWithInt:Natural] frequency:100.0];
    
    // Set key number to 49 (A4)
    note.keyNumber = @49;
    XCTAssertEqualWithAccuracy(note.frequency, 440.0, 0.001);
    
    // Set key number to 40 (C4)
    note.keyNumber = @40;
    double expectedC4 = 440.0 * pow(2, (40 - 49) / 12.0);
    XCTAssertEqualWithAccuracy(note.frequency, expectedC4, 0.01);
}

- (void)testKeyNumberNilDoesNotChangeFrequency {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"X" octave:0 accidental:[DPAccidental enumWithInt:Natural] frequency:100.0];
    
    note.keyNumber = nil;
    // Frequency should remain unchanged
    XCTAssertEqual(note.frequency, 100.0);
}

- (void)testKeyNumberForLowNotes {
    // Key 1 = A0 ≈ 27.5 Hz
    DPNote *a0 = [[DPNote alloc] initWithFriendlyName:@"A" octave:0 accidental:[DPAccidental enumWithInt:Natural] keyNumber:1];
    double expected = 440.0 * pow(2, (1 - 49) / 12.0);
    XCTAssertEqualWithAccuracy(a0.frequency, expected, 0.01);
}

- (void)testKeyNumberForHighNotes {
    // Key 88 = C8 ≈ 4186 Hz
    DPNote *c8 = [[DPNote alloc] initWithFriendlyName:@"C" octave:8 accidental:[DPAccidental enumWithInt:Natural] keyNumber:88];
    double expected = 440.0 * pow(2, (88 - 49) / 12.0);
    XCTAssertEqualWithAccuracy(c8.frequency, expected, 0.1);
}

#pragma mark - Equality Edge Cases

- (void)testNoteNotEqualToNil {
    DPNote *note = [DPNote C4];
    XCTAssertFalse([note isEqual:nil]);
}

- (void)testNoteNotEqualToString {
    DPNote *note = [DPNote C4];
    XCTAssertFalse([note isEqual:@"C4"]);
}

- (void)testNoteNotEqualToNumber {
    DPNote *note = [DPNote C4];
    XCTAssertFalse([note isEqual:@261]);
}

- (void)testNoteNotEqualToArray {
    DPNote *note = [DPNote C4];
    NSArray *array = @[@"C", @4];
    XCTAssertFalse([note isEqual:array]);
}

- (void)testNoteNotEqualToDictionary {
    DPNote *note = [DPNote C4];
    NSDictionary *dict = @{@"name": @"C", @"octave": @4};
    XCTAssertFalse([note isEqual:dict]);
}

- (void)testNotesWithSameNameButDifferentAccidentalNotEqual {
    DPNote *cNatural = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    DPNote *cSharp = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:4];
    
    XCTAssertFalse([cNatural isEqual:cSharp]);
}

- (void)testNotesWithSameAccidentalButDifferentNameNotEqual {
    DPNote *c = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    DPNote *d = [DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    
    XCTAssertFalse([c isEqual:d]);
}

#pragma mark - Description Tests for All Accidentals

- (void)testDescriptionForNatural {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:261.63];
    XCTAssertEqualObjects([note description], @"C");
}

- (void)testDescriptionForSharp {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Sharp] frequency:277.18];
    XCTAssertEqualObjects([note description], @"C#");
}

- (void)testDescriptionForFlat {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"D" octave:4 accidental:[DPAccidental enumWithInt:Flat] frequency:277.18];
    XCTAssertEqualObjects([note description], @"Db");
}

- (void)testDescriptionForAllNaturalNotes {
    NSArray *names = @[@"C", @"D", @"E", @"F", @"G", @"A", @"B"];
    for (NSString *name in names) {
        DPNote *note = [[DPNote alloc] initWithFriendlyName:name octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:0];
        XCTAssertEqualObjects([note description], name);
    }
}

#pragma mark - Find Note Edge Cases

- (void)testFindNoteWithInvalidName {
    DPNote *note = [DPNote findNoteWithName:@"X" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNil(note);
}

- (void)testFindNoteWithLowercaseName {
    // Lowercase should not match (case sensitive)
    DPNote *note = [DPNote findNoteWithName:@"c" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNil(note, @"Lowercase note name should not be found");
}

- (void)testFindNoteWithEmptyName {
    DPNote *note = [DPNote findNoteWithName:@"" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNil(note);
}

- (void)testFindNoteInAllOctaves {
    for (int octave = 0; octave < 8; octave++) {
        DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:octave];
        XCTAssertNotNil(note, @"Should find A natural in octave %d", octave);
        if (note) {
            XCTAssertEqual(note.octave, octave);
        }
    }
}

#pragma mark - Frequency Range Tests

- (void)testOctave0NotesHaveLowFrequencies {
    NSArray *common = [DPNote commonNotes];
    for (DPNote *note in common) {
        if (note.octave == 0) {
            // Octave 0 should be very low frequencies (< 50 Hz typically)
            XCTAssertLessThan(note.frequency, 100, @"Octave 0 notes should have low frequencies");
        }
    }
}

- (void)testOctave7NotesHaveHighFrequencies {
    NSArray *common = [DPNote commonNotes];
    for (DPNote *note in common) {
        if (note.octave == 7) {
            // Octave 7 should be very high frequencies (> 2000 Hz)
            XCTAssertGreaterThan(note.frequency, 2000, @"Octave 7 notes should have high frequencies");
        }
    }
}

#pragma mark - Play/Stop State Tests

- (void)testInitialPlayingState {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertFalse(note.isPlaying, @"Note should not be playing initially");
}

- (void)testPlayStartsPlaying {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    [note play];
    XCTAssertTrue(note.isPlaying);
    [note stop];
}

- (void)testStopStopsPlaying {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    [note play];
    [note stop];
    XCTAssertFalse(note.isPlaying);
}

- (void)testMultiplePlayCallsAreIdempotent {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    [note play];
    [note play];
    [note play];
    XCTAssertTrue(note.isPlaying);
    [note stop];
}

- (void)testMultipleStopCallsAreIdempotent {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    [note play];
    [note stop];
    [note stop];
    [note stop];
    XCTAssertFalse(note.isPlaying);
}

#pragma mark - Initialization Edge Cases

- (void)testInitWithZeroFrequency {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"Test" octave:0 accidental:[DPAccidental enumWithInt:Natural] frequency:0.0];
    XCTAssertNotNil(note);
    XCTAssertEqual(note.frequency, 0.0);
}

- (void)testInitWithNegativeOctave {
    // Edge case: negative octave (though musically invalid)
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"A" octave:-1 accidental:[DPAccidental enumWithInt:Natural] frequency:13.75];
    XCTAssertNotNil(note);
    XCTAssertEqual(note.octave, -1);
}

- (void)testInitWithHighOctave {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"A" octave:10 accidental:[DPAccidental enumWithInt:Natural] frequency:14080.0];
    XCTAssertNotNil(note);
    XCTAssertEqual(note.octave, 10);
}

@end
