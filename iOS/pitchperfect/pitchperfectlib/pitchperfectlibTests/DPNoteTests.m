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

@end

