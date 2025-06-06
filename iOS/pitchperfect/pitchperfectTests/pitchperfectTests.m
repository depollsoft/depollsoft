//
//  pitchperfectTests.m
//  pitchperfectTests
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "pitchperfectTests.h"
#import "DPPitchPipeModel.h"

@implementation pitchperfectTests

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

- (void)testDPPitchPipeModelCreation
{
    DPPitchPipeModel *model = [[DPPitchPipeModel alloc] init];
    XCTAssertNotNil(model, @"DPPitchPipeModel should be created successfully");
}

- (void)testDPPitchPipeModelNotesArray
{
    DPPitchPipeModel *model = [[DPPitchPipeModel alloc] init];
    NSArray *notes = model.notes;
    XCTAssertNotNil(notes, @"Notes array should not be nil");
    // The notes array might be empty initially, but it should be a valid array
    XCTAssertTrue([notes isKindOfClass:[NSArray class]], @"Notes should be an NSArray");
}

- (void)testDPPitchPipeModelFToFProperty
{
    DPPitchPipeModel *model = [[DPPitchPipeModel alloc] init];
    
    // Test default value (should be settable)
    model.isFromFToF = YES;
    XCTAssertTrue(model.isFromFToF, @"isFromFToF should be settable to YES");
    
    model.isFromFToF = NO;
    XCTAssertFalse(model.isFromFToF, @"isFromFToF should be settable to NO");
}

@end
