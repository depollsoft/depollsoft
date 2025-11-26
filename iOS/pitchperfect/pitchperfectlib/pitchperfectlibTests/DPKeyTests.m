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

@end
