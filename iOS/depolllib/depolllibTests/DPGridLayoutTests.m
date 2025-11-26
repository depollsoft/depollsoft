//
//  DPGridLayoutTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import <UIKit/UIKit.h>
#import "DPGridLayout.h"

@interface DPGridLayoutTests : XCTestCase
@end

@implementation DPGridLayoutTests

- (void)testAddHideShowSubview {
    DPGridLayout *grid = [[DPGridLayout alloc] initWithFrame:CGRectMake(0, 0, 200, 200)];
    grid.columnDimensions = @[ [DPGridDimension dimensionWithStars:1],
                               [DPGridDimension dimensionWithSize:20],
                               [DPGridDimension dimensionWithStars:2] ];
    grid.rowDimensions = @[ [DPGridDimension dimensionWithStars:1],
                            [DPGridDimension dimensionWithSize:30],
                            [DPGridDimension dimensionWithStars:1] ];

    UIView *child = [[UIView alloc] init];
    [grid addSubview:child row:1 column:1 rowSpan:1 colSpan:1];
    STAssertEquals(child.superview, grid, @"Child should be added to grid");

    // Hide the child and verify it is removed from the hierarchy
    [grid setView:child hidden:YES];
    STAssertNil(child.superview, @"Hidden child should be removed from superview");

    // Show it again and verify it is re-added
    [grid setView:child hidden:NO];
    STAssertEquals(child.superview, grid, @"Child should be re-added to grid after showing");
}

- (void)testInvalidateLayoutKeepsChildren {
    DPGridLayout *grid = [[DPGridLayout alloc] initWithFrame:CGRectMake(0, 0, 100, 100)];
    grid.columnDimensions = @[ [DPGridDimension dimensionWithStars:1] ];
    grid.rowDimensions = @[ [DPGridDimension dimensionWithStars:1] ];

    UIView *child = [[UIView alloc] init];
    [grid addSubview:child row:0 column:0];
    STAssertEquals(child.superview, grid, @"Child should be added");

    // Trigger re-layout
    [grid invalidateLayout];
    STAssertEquals(child.superview, grid, @"Child should still be attached after invalidateLayout");
}

@end
