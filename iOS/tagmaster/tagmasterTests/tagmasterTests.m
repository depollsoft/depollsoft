//
//  tagmasterTests.m
//  tagmasterTests
//
//  Created by David Poll on 3/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "tagmasterTests.h"
#import "DPConstants.h"

@implementation tagmasterTests

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

- (void)testDPTagCollectionEnumValues
{
    // Test that enum values are defined and different
    XCTAssertNotEqual(DPTagCollectionNone, DPTagCollectionClassicTags, @"DPTagCollectionNone should not equal DPTagCollectionClassicTags");
    XCTAssertNotEqual(DPTagCollectionClassicTags, DPTagCollectionEasyTags, @"DPTagCollectionClassicTags should not equal DPTagCollectionEasyTags");
    XCTAssertNotEqual(DPTagCollectionNone, DPTagCollectionEasyTags, @"DPTagCollectionNone should not equal DPTagCollectionEasyTags");
}

- (void)testDPTagSortOptionsEnumValues
{
    // Test that enum values are defined and different
    XCTAssertNotEqual(DPTagSortNone, DPTagSortTitle, @"DPTagSortNone should not equal DPTagSortTitle");
    XCTAssertNotEqual(DPTagSortTitle, DPTagSortPosted, @"DPTagSortTitle should not equal DPTagSortPosted");
    XCTAssertNotEqual(DPTagSortPosted, DPTagSortRating, @"DPTagSortPosted should not equal DPTagSortRating");
    XCTAssertNotEqual(DPTagSortRating, DPTagSortDownloaded, @"DPTagSortRating should not equal DPTagSortDownloaded");
    XCTAssertNotEqual(DPTagSortDownloaded, DPTagSortClassic, @"DPTagSortDownloaded should not equal DPTagSortClassic");
}

- (void)testEnumInitialValues
{
    // Test that the first enum values start at expected values (typically 0)
    XCTAssertEqual(DPTagCollectionNone, 0, @"DPTagCollectionNone should equal 0");
    XCTAssertEqual(DPTagSortNone, 0, @"DPTagSortNone should equal 0");
}

@end
