//
//  DPNoteTests.m
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import "DPNote.h"
#import "DPAccidental.h"

@interface DPNoteTests : XCTestCase
@end

@implementation DPNoteTests

- (void)testCommonAndPrunedNotes {
    NSArray *common = [DPNote commonNotes];
    NSArray *pruned = [DPNote prunedNotes];
    STAssertTrue(common.count > pruned.count, @"Pruned should be fewer than common");
}

- (void)testFindNoteAndC4 {
    DPNote *c4 = [DPNote C4];
    STAssertNotNil(c4, @"C4 should be initialized");
    DPNote *found = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    STAssertTrue([found isEqual:c4], @"Find should return C4");
}

- (void)testKeyNumberUpdatesFrequency {
    DPNote *n = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:0];
    n.keyNumber = @(49);
    STAssertTrue(fabs(n.frequency - 440.0) < 0.1, @"A4 frequency approx 440Hz");
}

- (void)testDescriptionFormatsAccidentals {
    DPNote *n = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:0];
    STAssertEqualObjects([n description], @"C", @"Natural no suffix");
    n.accidental = [DPAccidental enumWithInt:Sharp];
    STAssertEqualObjects([n description], @"C#", @"Sharp suffix");
    n.accidental = [DPAccidental enumWithInt:Flat];
    STAssertEqualObjects([n description], @"Cb", @"Flat suffix");
}

#pragma mark - Frequency Calculation Tests

- (void)testNoteFrequencyCalculations {
    // Test A4 = 440 Hz (key number 49)
    // Using the formula: 440 * pow(2, (keyNumber - 49) / 12.0)
    DPNote *a4 = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] keyNumber:49];
    XCTAssertEqualWithAccuracy(a4.frequency, 440.0, 0.001, @"A4 should be 440 Hz");
}

- (void)testA4StandardFrequency {
    DPNote *a4 = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNotNil(a4);
    XCTAssertEqualWithAccuracy(a4.frequency, 440.0, 1.0, @"A4 frequency should be approximately 440 Hz");
}

- (void)testMiddleCFrequency {
    DPNote *c4 = [DPNote C4];
    XCTAssertNotNil(c4);
    // Middle C is approximately 261.63 Hz
    XCTAssertEqualWithAccuracy(c4.frequency, 261.63, 1.0, @"C4 frequency should be approximately 261.63 Hz");
}

- (void)testOctaveDoublesFrequency {
    DPNote *a3 = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:3];
    DPNote *a4 = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    DPNote *a5 = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:5];
    
    XCTAssertNotNil(a3);
    XCTAssertNotNil(a4);
    XCTAssertNotNil(a5);
    
    // Each octave should double the frequency
    XCTAssertEqualWithAccuracy(a4.frequency / a3.frequency, 2.0, 0.01, @"A4 should be double A3 frequency");
    XCTAssertEqualWithAccuracy(a5.frequency / a4.frequency, 2.0, 0.01, @"A5 should be double A4 frequency");
}

- (void)testEnharmonicNotesHaveSameFrequency {
    // C# and Db should have the same frequency
    DPNote *cSharp = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:4];
    DPNote *dFlat = [DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Flat] octave:4];
    
    XCTAssertNotNil(cSharp);
    XCTAssertNotNil(dFlat);
    XCTAssertEqualWithAccuracy(cSharp.frequency, dFlat.frequency, 0.001, @"Enharmonic notes should have same frequency");
}

- (void)testGetNoteFrequencyFormula {
    // Key 49 (A4) = 440 Hz
    // Key 40 (C4) should be lower
    // Using keyNumber to compute frequencies
    DPNote *c4 = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Natural] keyNumber:40];
    DPNote *a4 = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] keyNumber:49];
    
    XCTAssertTrue(c4.frequency < a4.frequency, @"C4 should have lower frequency than A4");
    XCTAssertEqualWithAccuracy(a4.frequency, 440.0, 0.001);
}

- (void)testFrequencyRelativeToA4 {
    // Test half step above A4 (A#4/Bb4) - key number 50
    DPNote *aSharp4 = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Sharp] keyNumber:50];
    double expectedFreq = 440.0 * pow(2, 1.0/12.0);
    XCTAssertEqualWithAccuracy(aSharp4.frequency, expectedFreq, 0.01, @"A#4 should be one half step above A4");
}

#pragma mark - String Representation Tests

- (void)testNoteStringRepresentations {
    // Natural note
    DPNote *cNatural = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:261.63];
    XCTAssertEqualObjects([cNatural description], @"C");
    
    // Sharp note
    DPNote *cSharp = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Sharp] frequency:277.18];
    XCTAssertEqualObjects([cSharp description], @"C#");
    
    // Flat note
    DPNote *bFlat = [[DPNote alloc] initWithFriendlyName:@"B" octave:4 accidental:[DPAccidental enumWithInt:Flat] frequency:466.16];
    XCTAssertEqualObjects([bFlat description], @"Bb");
}

- (void)testAllNoteNamesWithNatural {
    NSArray *noteNames = @[@"C", @"D", @"E", @"F", @"G", @"A", @"B"];
    DPAccidental *natural = [DPAccidental enumWithInt:Natural];
    
    for (NSString *name in noteNames) {
        DPNote *note = [[DPNote alloc] initWithFriendlyName:name octave:4 accidental:natural frequency:0];
        XCTAssertEqualObjects([note description], name, @"Natural note should have no suffix");
    }
}

- (void)testAllNoteNamesWithSharp {
    NSArray *noteNames = @[@"C", @"D", @"F", @"G", @"A"];  // Standard sharps
    DPAccidental *sharp = [DPAccidental enumWithInt:Sharp];
    
    for (NSString *name in noteNames) {
        DPNote *note = [[DPNote alloc] initWithFriendlyName:name octave:4 accidental:sharp frequency:0];
        NSString *expected = [name stringByAppendingString:@"#"];
        XCTAssertEqualObjects([note description], expected, @"Sharp note should have # suffix");
    }
}

- (void)testAllNoteNamesWithFlat {
    NSArray *noteNames = @[@"D", @"E", @"G", @"A", @"B"];  // Standard flats
    DPAccidental *flat = [DPAccidental enumWithInt:Flat];
    
    for (NSString *name in noteNames) {
        DPNote *note = [[DPNote alloc] initWithFriendlyName:name octave:4 accidental:flat frequency:0];
        NSString *expected = [name stringByAppendingString:@"b"];
        XCTAssertEqualObjects([note description], expected, @"Flat note should have b suffix");
    }
}

#pragma mark - Equality Tests

- (void)testNoteEqualitySameNote {
    DPNote *note1 = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:440];
    DPNote *note2 = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:440];
    
    XCTAssertTrue([note1 isEqual:note2], @"Notes with same properties should be equal");
}

- (void)testNoteEqualityDifferentOctave {
    DPNote *note1 = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:440];
    DPNote *note2 = [[DPNote alloc] initWithFriendlyName:@"A" octave:5 accidental:[DPAccidental enumWithInt:Natural] frequency:880];
    
    XCTAssertFalse([note1 isEqual:note2], @"Notes with different octaves should not be equal");
}

- (void)testNoteEqualityDifferentAccidental {
    DPNote *note1 = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:261];
    DPNote *note2 = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Sharp] frequency:277];
    
    XCTAssertFalse([note1 isEqual:note2], @"Notes with different accidentals should not be equal");
}

- (void)testNoteEqualityDifferentName {
    DPNote *note1 = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:261];
    DPNote *note2 = [[DPNote alloc] initWithFriendlyName:@"D" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:293];
    
    XCTAssertFalse([note1 isEqual:note2], @"Notes with different names should not be equal");
}

- (void)testNoteNotEqualToNonNote {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:261];
    
    XCTAssertFalse([note isEqual:@"C"], @"Note should not be equal to string");
    XCTAssertFalse([note isEqual:@42], @"Note should not be equal to number");
    XCTAssertFalse([note isEqual:nil], @"Note should not be equal to nil");
}

#pragma mark - Play/Stop Tests

- (void)testPlayAndStop {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNotNil(note);
    
    XCTAssertFalse(note.isPlaying, @"Note should not be playing initially");
    
    [note play];
    XCTAssertTrue(note.isPlaying, @"Note should be playing after play");
    
    [note stop];
    XCTAssertFalse(note.isPlaying, @"Note should not be playing after stop");
}

- (void)testPlayIsIdempotent {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    
    [note play];
    XCTAssertTrue(note.isPlaying);
    
    // Calling play again should not cause issues
    [note play];
    XCTAssertTrue(note.isPlaying, @"Note should still be playing after second play call");
    
    [note stop];
}

- (void)testStopIsIdempotent {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    
    [note play];
    [note stop];
    XCTAssertFalse(note.isPlaying);
    
    // Calling stop again should not cause issues
    [note stop];
    XCTAssertFalse(note.isPlaying, @"Note should still not be playing after second stop call");
}

- (void)testStopWithoutPlay {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    
    // Stopping without playing should not crash
    [note stop];
    XCTAssertFalse(note.isPlaying);
}

#pragma mark - Find Note Tests

- (void)testFindNoteReturnsNilForInvalidNote {
    // Try to find a note that doesn't exist in the common notes
    DPNote *notFound = [DPNote findNoteWithName:@"X" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNil(notFound, @"Invalid note name should return nil");
}

- (void)testFindNoteWithAllAccidentals {
    // Find natural
    DPNote *natural = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNotNil(natural);
    
    // Find sharp
    DPNote *sharp = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:4];
    XCTAssertNotNil(sharp);
    
    // Find flat
    DPNote *flat = [DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Flat] octave:4];
    XCTAssertNotNil(flat);
}

- (void)testFindNoteAcrossOctaves {
    for (int octave = 0; octave < 8; octave++) {
        DPNote *note = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:octave];
        XCTAssertNotNil(note, @"Should find C natural in octave %d", octave);
        XCTAssertEqual(note.octave, octave, @"Octave should match");
    }
}

#pragma mark - Alternate Notes Tests

- (void)testPrunedNotesHaveAlternates {
    NSArray *pruned = [DPNote prunedNotes];
    
    int notesWithAlternates = 0;
    for (DPNote *note in pruned) {
        if (note.alternate != nil) {
            notesWithAlternates++;
            // Alternate should have same frequency
            XCTAssertEqual(note.frequency, note.alternate.frequency, @"Note and alternate should have same frequency");
        }
    }
    
    // There should be some notes with alternates (enharmonic equivalents)
    XCTAssertTrue(notesWithAlternates > 0, @"Some pruned notes should have alternates");
}

#pragma mark - Initialization Tests

- (void)testInitWithFriendlyNameAndFrequency {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:440.0];
    
    XCTAssertEqualObjects(note.friendlyName, @"A");
    XCTAssertEqual(note.octave, 4);
    XCTAssertEqualObjects(note.accidental, [DPAccidental enumWithInt:Natural]);
    XCTAssertEqual(note.frequency, 440.0);
}

- (void)testInitWithFriendlyNameAndKeyNumber {
    DPNote *note = [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] keyNumber:49];
    
    XCTAssertEqualObjects(note.friendlyName, @"A");
    XCTAssertEqual(note.octave, 4);
    XCTAssertEqualObjects(note.accidental, [DPAccidental enumWithInt:Natural]);
    XCTAssertEqualWithAccuracy(note.frequency, 440.0, 0.001, @"Frequency should be calculated from key number");
}

#pragma mark - Common Notes Tests

- (void)testCommonNotesNotEmpty {
    NSArray *common = [DPNote commonNotes];
    XCTAssertTrue(common.count > 0, @"Common notes should not be empty");
}

- (void)testCommonNotesCached {
    NSArray *first = [DPNote commonNotes];
    NSArray *second = [DPNote commonNotes];
    
    XCTAssertEqual(first, second, @"Common notes should return cached array");
}

- (void)testPrunedNotesCached {
    NSArray *first = [DPNote prunedNotes];
    NSArray *second = [DPNote prunedNotes];
    
    XCTAssertEqual(first, second, @"Pruned notes should return cached array");
}

- (void)testCommonNotesContainAllOctaves {
    NSArray *common = [DPNote commonNotes];
    
    NSMutableSet *octaves = [NSMutableSet set];
    for (DPNote *note in common) {
        [octaves addObject:@(note.octave)];
    }
    
    // Should have octaves 0-7
    XCTAssertTrue(octaves.count >= 8, @"Common notes should contain multiple octaves");
}

@end

