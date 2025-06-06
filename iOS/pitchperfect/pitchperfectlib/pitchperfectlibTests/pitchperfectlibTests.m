//
//  pitchperfectlibTests.m
//  pitchperfectlibTests
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "pitchperfectlibTests.h"
#import "DPNote.h"
#import "DPAccidental.h"

@implementation pitchperfectlibTests

- (void)setUp
{
    [super setUp];
    
    // Set-up code here.
}

- (void)tearDown
{
    // Tear-down code here.
    
    [super tearDown];
}

- (void)testDPNoteC4Creation
{
    DPNote *c4 = [DPNote C4];
    XCTAssertNotNil(c4, @"C4 note should be created successfully");
    XCTAssertEqualObjects(c4.friendlyName, @"C", @"C4 should have friendly name 'C'");
    XCTAssertEqual(c4.octave, 4, @"C4 should be in octave 4");
}

- (void)testCommonNotesArray
{
    NSArray *commonNotes = [DPNote commonNotes];
    XCTAssertNotNil(commonNotes, @"Common notes array should not be nil");
    XCTAssertTrue([commonNotes count] > 0, @"Common notes array should contain notes");
}

- (void)testPrunedNotesArray
{
    NSArray *prunedNotes = [DPNote prunedNotes];
    XCTAssertNotNil(prunedNotes, @"Pruned notes array should not be nil");
    XCTAssertTrue([prunedNotes count] > 0, @"Pruned notes array should contain notes");
}

- (void)testDPAccidentalEnum
{
    DPAccidental *flat = [DPAccidental enumWithString:@"Flat"];
    DPAccidental *natural = [DPAccidental enumWithString:@"Natural"];
    DPAccidental *sharp = [DPAccidental enumWithString:@"Sharp"];
    
    XCTAssertNotNil(flat, @"Flat accidental should be created");
    XCTAssertNotNil(natural, @"Natural accidental should be created");
    XCTAssertNotNil(sharp, @"Sharp accidental should be created");
    
    XCTAssertNotEqual([flat get], [natural get], @"Flat and Natural should have different values");
    XCTAssertNotEqual([natural get], [sharp get], @"Natural and Sharp should have different values");
}

@end
