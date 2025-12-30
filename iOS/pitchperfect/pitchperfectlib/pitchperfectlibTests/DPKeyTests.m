//
//  DPKeyTests.m
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import "DPKey.h"
#import "DPKeyType.h"
#import "DPAccidental.h"
#import "DPNote.h"

@interface DPKeyTests : XCTestCase
@end

@implementation DPKeyTests

- (void)testMajorMinorKeysCount {
    STAssertEquals((int)[[DPKey majorKeys] count], 13, @"13 major keys");
    STAssertEquals((int)[[DPKey minorKeys] count], 13, @"13 minor keys");
}

- (void)testFriendlyNameCasing {
    DPKey *maj = [[DPKey alloc] init];
    maj.keyType = [DPKeyType enumWithInt:Major];
    maj.note = [DPNote findNoteWithName:@"c" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    STAssertEqualObjects([maj friendlyName], maj.note.friendlyName.uppercaseString, @"Major uppercase");

    DPKey *min = [[DPKey alloc] init];
    min.keyType = [DPKeyType enumWithInt:Minor];
    min.note = [DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    STAssertEqualObjects([min friendlyName], min.note.friendlyName.lowercaseString, @"Minor lowercase");
}

- (void)testEqualityAndAccidentalHelpers {
    DPKey *a = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:4]
                                     keyType:[DPKeyType enumWithInt:Major]
                              numAccidentals:1];
    DPKey *b = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:4]
                                     keyType:[DPKeyType enumWithInt:Major]
                              numAccidentals:1];
    STAssertTrue([a isEqual:b], @"Keys with same note/type equal");
    STAssertTrue([[a accidental] isEqual:[DPAccidental enumWithInt:Sharp]], @"Accidental passthrough matches");

    STAssertFalse([a isEqual:@42], @"Different class not equal");
}

#pragma mark - All Accidentals Tests

- (void)testKeyWithAllAccidentals {
    // Test key with flat accidental
    DPKey *flatKey = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Flat] octave:4]
                                         keyType:[DPKeyType enumWithInt:Major]
                                  numAccidentals:-2];
    XCTAssertNotNil(flatKey);
    XCTAssertEqualObjects([flatKey accidental], [DPAccidental enumWithInt:Flat]);
    
    // Test key with natural accidental
    DPKey *naturalKey = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                            keyType:[DPKeyType enumWithInt:Major]
                                     numAccidentals:0];
    XCTAssertNotNil(naturalKey);
    XCTAssertEqualObjects([naturalKey accidental], [DPAccidental enumWithInt:Natural]);
    
    // Test key with sharp accidental
    DPKey *sharpKey = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Sharp] octave:4]
                                          keyType:[DPKeyType enumWithInt:Major]
                                   numAccidentals:6];
    XCTAssertNotNil(sharpKey);
    XCTAssertEqualObjects([sharpKey accidental], [DPAccidental enumWithInt:Sharp]);
}

#pragma mark - Key Equality Tests

- (void)testKeyEquality {
    DPKey *key1 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:1];
    DPKey *key2 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:1];
    
    // Same note and type should be equal
    XCTAssertTrue([key1 isEqual:key2], @"Keys with same note and type should be equal");
}

- (void)testKeyInequalityDifferentNote {
    DPKey *key1 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:1];
    DPKey *key2 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"D" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:2];
    
    // Different notes should not be equal
    XCTAssertFalse([key1 isEqual:key2], @"Keys with different notes should not be equal");
}

- (void)testKeyInequalityDifferentType {
    DPKey *majorKey = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                          keyType:[DPKeyType enumWithInt:Major]
                                   numAccidentals:3];
    DPKey *minorKey = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                          keyType:[DPKeyType enumWithInt:Minor]
                                   numAccidentals:0];
    
    // Different types should not be equal even with same note
    XCTAssertFalse([majorKey isEqual:minorKey], @"Major and minor keys with same note should not be equal");
}

- (void)testKeyEqualityIgnoresNumAccidentals {
    // Num accidentals is not part of equality check per the implementation
    DPKey *key1 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:0];
    DPKey *key2 = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                      keyType:[DPKeyType enumWithInt:Major]
                               numAccidentals:99];
    
    // Should still be equal since equality is based on note and type only
    XCTAssertTrue([key1 isEqual:key2], @"Keys should be equal regardless of numAccidentals");
}

- (void)testKeyNotEqualToNil {
    DPKey *key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                     keyType:[DPKeyType enumWithInt:Major]
                              numAccidentals:0];
    
    XCTAssertFalse([key isEqual:nil], @"Key should not be equal to nil");
}

#pragma mark - Major Keys Array Tests

- (void)testMajorKeysContainExpectedKeys {
    NSArray *majorKeys = [DPKey majorKeys];
    
    // Check for C Major (key of no accidentals)
    BOOL foundCMajor = NO;
    for (DPKey *key in majorKeys) {
        if ([key.note.friendlyName isEqualToString:@"C"] &&
            key.note.accidental.get == Natural &&
            key.keyType.get == Major) {
            foundCMajor = YES;
            XCTAssertEqual(key.numAccidentals, 0, @"C Major should have 0 accidentals");
            break;
        }
    }
    XCTAssertTrue(foundCMajor, @"Major keys should contain C Major");
}

- (void)testMajorKeysContainFlatKeys {
    NSArray *majorKeys = [DPKey majorKeys];
    
    int flatKeyCount = 0;
    for (DPKey *key in majorKeys) {
        if (key.numAccidentals < 0) {
            flatKeyCount++;
        }
    }
    XCTAssertEqual(flatKeyCount, 6, @"Should have 6 flat major keys");
}

- (void)testMajorKeysContainSharpKeys {
    NSArray *majorKeys = [DPKey majorKeys];
    
    int sharpKeyCount = 0;
    for (DPKey *key in majorKeys) {
        if (key.numAccidentals > 0) {
            sharpKeyCount++;
        }
    }
    XCTAssertEqual(sharpKeyCount, 6, @"Should have 6 sharp major keys");
}

#pragma mark - Minor Keys Array Tests

- (void)testMinorKeysContainExpectedKeys {
    NSArray *minorKeys = [DPKey minorKeys];
    
    // Check for A minor (key of no accidentals)
    BOOL foundAMinor = NO;
    for (DPKey *key in minorKeys) {
        if ([key.note.friendlyName isEqualToString:@"A"] &&
            key.note.accidental.get == Natural &&
            key.keyType.get == Minor) {
            foundAMinor = YES;
            XCTAssertEqual(key.numAccidentals, 0, @"A minor should have 0 accidentals");
            break;
        }
    }
    XCTAssertTrue(foundAMinor, @"Minor keys should contain A minor");
}

#pragma mark - Default Initialization Tests

- (void)testDefaultInit {
    DPKey *key = [[DPKey alloc] init];
    
    XCTAssertNotNil(key);
    XCTAssertNotNil(key.note);
    XCTAssertEqual(key.numAccidentals, 0);
    XCTAssertEqual(key.keyType.get, Major);
}

#pragma mark - Friendly Name Edge Cases

- (void)testFriendlyNameWithSharpNote {
    DPKey *sharpKey = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"F" accidental:[DPAccidental enumWithInt:Sharp] octave:4]
                                          keyType:[DPKeyType enumWithInt:Major]
                                   numAccidentals:6];
    
    NSString *friendlyName = [sharpKey friendlyName];
    XCTAssertNotNil(friendlyName);
    // Major keys return uppercase
    XCTAssertEqualObjects(friendlyName, sharpKey.note.friendlyName.uppercaseString);
}

- (void)testFriendlyNameWithFlatNote {
    DPKey *flatKey = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Flat] octave:4]
                                         keyType:[DPKeyType enumWithInt:Minor]
                                  numAccidentals:-5];
    
    NSString *friendlyName = [flatKey friendlyName];
    XCTAssertNotNil(friendlyName);
    // Minor keys return lowercase
    XCTAssertEqualObjects(friendlyName, flatKey.note.friendlyName.lowercaseString);
}

#pragma mark - Caching Tests

- (void)testMajorKeysCachingReturnsIdenticalArray {
    NSArray *firstCall = [DPKey majorKeys];
    NSArray *secondCall = [DPKey majorKeys];
    
    // Should return cached instance
    XCTAssertEqual(firstCall, secondCall, @"Major keys should return cached array");
}

- (void)testMinorKeysCachingReturnsIdenticalArray {
    NSArray *firstCall = [DPKey minorKeys];
    NSArray *secondCall = [DPKey minorKeys];
    
    // Should return cached instance
    XCTAssertEqual(firstCall, secondCall, @"Minor keys should return cached array");
}

@end
