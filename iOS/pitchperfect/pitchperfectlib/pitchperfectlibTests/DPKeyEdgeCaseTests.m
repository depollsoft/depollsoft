//
//  DPKeyEdgeCaseTests.m
//  pitchperfectlibTests
//
//  Additional edge case and coverage tests for DPKey
//

#import <XCTest/XCTest.h>
#import "DPKey.h"
#import "DPKeyType.h"
#import "DPAccidental.h"
#import "DPNote.h"

@interface DPKeyEdgeCaseTests : XCTestCase
@end

@implementation DPKeyEdgeCaseTests

#pragma mark - Major Keys Complete Coverage

- (void)testMajorKeysHasExactly13Keys {
    NSArray *majorKeys = [DPKey majorKeys];
    XCTAssertEqual(majorKeys.count, 13, @"Should have exactly 13 major keys");
}

- (void)testAllMajorKeysHaveMajorType {
    NSArray *majorKeys = [DPKey majorKeys];
    for (DPKey *key in majorKeys) {
        XCTAssertEqual(key.keyType.get, Major, @"All keys in majorKeys should be Major type");
    }
}

- (void)testMajorKeysAccidentalRange {
    NSArray *majorKeys = [DPKey majorKeys];
    
    int minAccidentals = INT_MAX;
    int maxAccidentals = INT_MIN;
    
    for (DPKey *key in majorKeys) {
        if (key.numAccidentals < minAccidentals) minAccidentals = key.numAccidentals;
        if (key.numAccidentals > maxAccidentals) maxAccidentals = key.numAccidentals;
    }
    
    XCTAssertEqual(minAccidentals, -6, @"Minimum accidentals should be -6 (Gb Major)");
    XCTAssertEqual(maxAccidentals, 6, @"Maximum accidentals should be 6 (F# Major)");
}

- (void)testMajorKeysCMajorHasZeroAccidentals {
    NSArray *majorKeys = [DPKey majorKeys];
    
    BOOL foundCMajor = NO;
    for (DPKey *key in majorKeys) {
        if ([key.note.friendlyName isEqualToString:@"C"] && 
            key.note.accidental.get == Natural) {
            foundCMajor = YES;
            XCTAssertEqual(key.numAccidentals, 0, @"C Major should have 0 accidentals");
            break;
        }
    }
    XCTAssertTrue(foundCMajor, @"Should find C Major");
}

- (void)testMajorKeysContainGMajor {
    NSArray *majorKeys = [DPKey majorKeys];
    
    BOOL found = NO;
    for (DPKey *key in majorKeys) {
        if ([key.note.friendlyName isEqualToString:@"G"] && 
            key.note.accidental.get == Natural) {
            found = YES;
            XCTAssertEqual(key.numAccidentals, 1, @"G Major should have 1 sharp");
            break;
        }
    }
    XCTAssertTrue(found, @"Should find G Major");
}

- (void)testMajorKeysContainFMajor {
    NSArray *majorKeys = [DPKey majorKeys];
    
    BOOL found = NO;
    for (DPKey *key in majorKeys) {
        if ([key.note.friendlyName isEqualToString:@"F"] && 
            key.note.accidental.get == Natural) {
            found = YES;
            XCTAssertEqual(key.numAccidentals, -1, @"F Major should have 1 flat");
            break;
        }
    }
    XCTAssertTrue(found, @"Should find F Major");
}

#pragma mark - Minor Keys Complete Coverage

- (void)testMinorKeysHasExactly13Keys {
    NSArray *minorKeys = [DPKey minorKeys];
    XCTAssertEqual(minorKeys.count, 13, @"Should have exactly 13 minor keys");
}

- (void)testAllMinorKeysHaveMinorType {
    NSArray *minorKeys = [DPKey minorKeys];
    for (DPKey *key in minorKeys) {
        XCTAssertEqual(key.keyType.get, Minor, @"All keys in minorKeys should be Minor type");
    }
}

- (void)testMinorKeysAccidentalRange {
    NSArray *minorKeys = [DPKey minorKeys];
    
    int minAccidentals = INT_MAX;
    int maxAccidentals = INT_MIN;
    
    for (DPKey *key in minorKeys) {
        if (key.numAccidentals < minAccidentals) minAccidentals = key.numAccidentals;
        if (key.numAccidentals > maxAccidentals) maxAccidentals = key.numAccidentals;
    }
    
    XCTAssertEqual(minAccidentals, -6, @"Minimum accidentals should be -6 (Eb minor)");
    XCTAssertEqual(maxAccidentals, 6, @"Maximum accidentals should be 6 (D# minor)");
}

- (void)testMinorKeysAMinorHasZeroAccidentals {
    NSArray *minorKeys = [DPKey minorKeys];
    
    BOOL foundAMinor = NO;
    for (DPKey *key in minorKeys) {
        if ([key.note.friendlyName isEqualToString:@"A"] && 
            key.note.accidental.get == Natural) {
            foundAMinor = YES;
            XCTAssertEqual(key.numAccidentals, 0, @"A minor should have 0 accidentals");
            break;
        }
    }
    XCTAssertTrue(foundAMinor, @"Should find A minor");
}

- (void)testMinorKeysContainEMinor {
    NSArray *minorKeys = [DPKey minorKeys];
    
    BOOL found = NO;
    for (DPKey *key in minorKeys) {
        if ([key.note.friendlyName isEqualToString:@"E"] && 
            key.note.accidental.get == Natural) {
            found = YES;
            XCTAssertEqual(key.numAccidentals, 1, @"E minor should have 1 sharp");
            break;
        }
    }
    XCTAssertTrue(found, @"Should find E minor");
}

- (void)testMinorKeysContainDMinor {
    NSArray *minorKeys = [DPKey minorKeys];
    
    BOOL found = NO;
    for (DPKey *key in minorKeys) {
        if ([key.note.friendlyName isEqualToString:@"D"] && 
            key.note.accidental.get == Natural) {
            found = YES;
            XCTAssertEqual(key.numAccidentals, -1, @"D minor should have 1 flat");
            break;
        }
    }
    XCTAssertTrue(found, @"Should find D minor");
}

#pragma mark - Friendly Name Tests

- (void)testMajorKeyFriendlyNameIsUppercase {
    DPKey *key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                     keyType:[DPKeyType enumWithInt:Major]
                              numAccidentals:0];
    XCTAssertEqualObjects([key friendlyName], @"C");
}

- (void)testMinorKeyFriendlyNameIsLowercase {
    DPKey *key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                     keyType:[DPKeyType enumWithInt:Minor]
                              numAccidentals:0];
    XCTAssertEqualObjects([key friendlyName], @"a");
}

- (void)testMajorKeyWithSharpNoteFriendlyName {
    DPKey *key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Sharp] octave:4]
                                     keyType:[DPKeyType enumWithInt:Major]
                              numAccidentals:6];
    XCTAssertEqualObjects([key friendlyName], @"F");
}

- (void)testMinorKeyWithSharpNoteFriendlyName {
    DPKey *key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Sharp] octave:4]
                                     keyType:[DPKeyType enumWithInt:Minor]
                              numAccidentals:3];
    XCTAssertEqualObjects([key friendlyName], @"f");
}

- (void)testMajorKeyWithFlatNoteFriendlyName {
    DPKey *key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Flat] octave:4]
                                     keyType:[DPKeyType enumWithInt:Major]
                              numAccidentals:-2];
    XCTAssertEqualObjects([key friendlyName], @"B");
}

- (void)testMinorKeyWithFlatNoteFriendlyName {
    DPKey *key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Flat] octave:4]
                                     keyType:[DPKeyType enumWithInt:Minor]
                              numAccidentals:-5];
    XCTAssertEqualObjects([key friendlyName], @"b");
}

#pragma mark - Accidental Property Tests

- (void)testAccidentalPropertyReturnsNoteAccidental {
    DPNote *note = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:4];
    DPKey *key = [[DPKey alloc] initWithNote:note
                                     keyType:[DPKeyType enumWithInt:Major]
                              numAccidentals:7];
    
    XCTAssertEqualObjects([key accidental], [DPAccidental enumWithInt:Sharp]);
}

- (void)testAccidentalPropertyForNaturalNote {
    DPNote *note = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    DPKey *key = [[DPKey alloc] initWithNote:note
                                     keyType:[DPKeyType enumWithInt:Major]
                              numAccidentals:0];
    
    XCTAssertEqualObjects([key accidental], [DPAccidental enumWithInt:Natural]);
}

- (void)testAccidentalPropertyForFlatNote {
    DPNote *note = [DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Flat] octave:4];
    DPKey *key = [[DPKey alloc] initWithNote:note
                                     keyType:[DPKeyType enumWithInt:Major]
                              numAccidentals:-5];
    
    XCTAssertEqualObjects([key accidental], [DPAccidental enumWithInt:Flat]);
}

#pragma mark - Equality Edge Cases

- (void)testKeyEqualityBasedOnNoteAndType {
    DPKey *key1 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:0];
    DPKey *key2 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:0];
    
    XCTAssertTrue([key1 isEqual:key2]);
}

- (void)testKeyInequalityDifferentType {
    DPKey *major = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                       keyType:[DPKeyType enumWithInt:Major]
                                numAccidentals:3];
    DPKey *minor = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                       keyType:[DPKeyType enumWithInt:Minor]
                                numAccidentals:0];
    
    XCTAssertFalse([major isEqual:minor]);
}

- (void)testKeyInequalityDifferentNote {
    DPKey *c = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                   keyType:[DPKeyType enumWithInt:Major]
                            numAccidentals:0];
    DPKey *d = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                   keyType:[DPKeyType enumWithInt:Major]
                            numAccidentals:2];
    
    XCTAssertFalse([c isEqual:d]);
}

- (void)testKeyNotEqualToNil {
    DPKey *key = [[DPKey alloc] init];
    XCTAssertFalse([key isEqual:nil]);
}

- (void)testKeyNotEqualToString {
    DPKey *key = [[DPKey alloc] init];
    XCTAssertFalse([key isEqual:@"C Major"]);
}

- (void)testKeyNotEqualToNumber {
    DPKey *key = [[DPKey alloc] init];
    XCTAssertFalse([key isEqual:@0]);
}

- (void)testKeyNotEqualToNote {
    DPKey *key = [[DPKey alloc] init];
    DPNote *note = [DPNote C4];
    XCTAssertFalse([key isEqual:note]);
}

- (void)testKeyEqualityIgnoresNumAccidentals {
    // Per implementation, equality is based on note and type only
    DPKey *key1 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:0];
    DPKey *key2 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:99];
    
    XCTAssertTrue([key1 isEqual:key2], @"NumAccidentals should not affect equality");
}

#pragma mark - Default Initialization Tests

- (void)testDefaultInitSetsC4 {
    DPKey *key = [[DPKey alloc] init];
    XCTAssertNotNil(key.note);
    // Default init uses C4
    XCTAssertEqualObjects(key.note.friendlyName, @"C");
}

- (void)testDefaultInitSetsMajorType {
    DPKey *key = [[DPKey alloc] init];
    XCTAssertEqual(key.keyType.get, Major);
}

- (void)testDefaultInitSetsZeroAccidentals {
    DPKey *key = [[DPKey alloc] init];
    XCTAssertEqual(key.numAccidentals, 0);
}

#pragma mark - Caching Tests

- (void)testMajorKeysReturnsSameInstance {
    NSArray *first = [DPKey majorKeys];
    NSArray *second = [DPKey majorKeys];
    XCTAssertEqual(first, second, @"Should return cached instance");
}

- (void)testMinorKeysReturnsSameInstance {
    NSArray *first = [DPKey minorKeys];
    NSArray *second = [DPKey minorKeys];
    XCTAssertEqual(first, second, @"Should return cached instance");
}

#pragma mark - Property Tests

- (void)testNotePropertyCanBeSet {
    DPKey *key = [[DPKey alloc] init];
    DPNote *newNote = [DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    key.note = newNote;
    XCTAssertEqualObjects(key.note, newNote);
}

- (void)testKeyTypePropertyCanBeSet {
    DPKey *key = [[DPKey alloc] init];
    key.keyType = [DPKeyType enumWithInt:Minor];
    XCTAssertEqual(key.keyType.get, Minor);
}

- (void)testNumAccidentalsPropertyCanBeSet {
    DPKey *key = [[DPKey alloc] init];
    key.numAccidentals = 5;
    XCTAssertEqual(key.numAccidentals, 5);
}

- (void)testNumAccidentalsCanBeNegative {
    DPKey *key = [[DPKey alloc] init];
    key.numAccidentals = -3;
    XCTAssertEqual(key.numAccidentals, -3);
}

#pragma mark - All Keys Iteration Tests

- (void)testAllMajorKeysHaveValidNotes {
    NSArray *majorKeys = [DPKey majorKeys];
    for (DPKey *key in majorKeys) {
        XCTAssertNotNil(key.note, @"Each major key should have a note");
        XCTAssertNotNil(key.note.friendlyName, @"Each note should have a friendly name");
    }
}

- (void)testAllMinorKeysHaveValidNotes {
    NSArray *minorKeys = [DPKey minorKeys];
    for (DPKey *key in minorKeys) {
        XCTAssertNotNil(key.note, @"Each minor key should have a note");
        XCTAssertNotNil(key.note.friendlyName, @"Each note should have a friendly name");
    }
}

- (void)testMajorKeysHaveUniqueNotes {
    NSArray *majorKeys = [DPKey majorKeys];
    NSMutableSet *noteDescriptions = [NSMutableSet set];
    
    for (DPKey *key in majorKeys) {
        NSString *noteDesc = [NSString stringWithFormat:@"%@_%d", key.note.friendlyName, key.note.accidental.get];
        [noteDescriptions addObject:noteDesc];
    }
    
    XCTAssertEqual(noteDescriptions.count, majorKeys.count, @"All major keys should have unique root notes");
}

- (void)testMinorKeysHaveUniqueNotes {
    NSArray *minorKeys = [DPKey minorKeys];
    NSMutableSet *noteDescriptions = [NSMutableSet set];
    
    for (DPKey *key in minorKeys) {
        NSString *noteDesc = [NSString stringWithFormat:@"%@_%d", key.note.friendlyName, key.note.accidental.get];
        [noteDescriptions addObject:noteDesc];
    }
    
    XCTAssertEqual(noteDescriptions.count, minorKeys.count, @"All minor keys should have unique root notes");
}

@end
