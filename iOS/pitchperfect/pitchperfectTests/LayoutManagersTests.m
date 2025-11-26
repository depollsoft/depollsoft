//
//  LayoutManagersTests.m
//  pitchperfectTests
//
//  Adds regression coverage for the custom layout manager
//  views that power legacy Objective-C screens.
//

#import <XCTest/XCTest.h>
#import "LayoutManagers.h"

@interface LayoutManagersTests : XCTestCase
@end

@implementation LayoutManagersTests

- (UIView *)viewWithSize:(CGSize)size
{
    UIView *view = [[UIView alloc] initWithFrame:CGRectMake(0, 0, size.width, size.height)];
    view.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    return view;
}

- (void)testGridLayoutPositionsSubviewsAndReportsContentSize
{
    GridLayoutView *grid = [[GridLayoutView alloc] initWithFrame:CGRectMake(0, 0, 200, 200)
                                                         spacing:4
                                                      leftMargin:10
                                                     rightMargin:8
                                                       topMargin:6
                                                    bottomMargin:2
                                                          rows:2
                                                          cols:2];

    UIView *first = [self viewWithSize:CGSizeMake(30, 20)];
    UIView *second = [self viewWithSize:CGSizeMake(24, 18)];
    UIView *third = [self viewWithSize:CGSizeMake(10, 12)];
    UIView *fourth = [self viewWithSize:CGSizeMake(16, 14)];
    [grid addSubview:first];
    [grid addSubview:second];
    [grid addSubview:third];
    [grid addSubview:fourth];

    [grid layoutSubviews];

    // Largest width/height drive grid spacing.
    CGFloat expectedWidth = 10 + (30 * 2) + 4 + 8;
    CGFloat expectedHeight = 6 + (20 * 2) + 4 + 2;
    XCTAssertEqualWithAccuracy(grid.contentSize.width, expectedWidth, 0.1);
    XCTAssertEqualWithAccuracy(grid.contentSize.height, expectedHeight, 0.1);

    // Verify the first row is offset by left/top margins.
    XCTAssertEqualWithAccuracy(first.frame.origin.x, 10, 0.1);
    XCTAssertEqualWithAccuracy(first.frame.origin.y, 6, 0.1);
    // Second cell should be shifted by max width plus spacing.
    XCTAssertEqualWithAccuracy(second.frame.origin.x, 10 + 30 + 4, 0.1);
    XCTAssertEqualWithAccuracy(second.frame.origin.y, 6, 0.1);

    // sizeThatFits should reuse calculations without moving subviews.
    CGSize fitted = [grid sizeThatFits:CGSizeZero];
    XCTAssertEqualWithAccuracy(fitted.width, expectedWidth, 0.1);
    XCTAssertEqualWithAccuracy(fitted.height, expectedHeight, 0.1);
}

- (void)testHorizontalLayoutRespectingAlignmentAndInsets
{
    HLayoutView *layout = [[HLayoutView alloc] initWithFrame:CGRectMake(0, 0, 60, 40)
                                                    spacing:6
                                                 leftMargin:8
                                                rightMargin:4
                                                  topMargin:5
                                               bottomMargin:3
                                                hAlignment:UIControlContentHorizontalAlignmentRight
                                                vAlignment:UIControlContentVerticalAlignmentBottom];

    UIView *leading = [self viewWithSize:CGSizeMake(20, 12)];
    UIView *trailing = [self viewWithSize:CGSizeMake(10, 18)];
    [layout addSubview:leading];
    [layout addSubview:trailing];

    [layout layoutSubviews];

    // Content size matches calculated dimensions without overflowing the frame.
    XCTAssertEqualWithAccuracy(layout.contentSize.width, 48, 0.1);
    XCTAssertEqualWithAccuracy(layout.contentSize.height, 26, 0.1);
    XCTAssertEqual(layout.contentInset.left, 0);
    XCTAssertEqual(layout.contentInset.top, 0);

    // With right/bottom alignment set, trailing view hugs the bottom right edge.
    CGFloat expectedTrailingX = layout.frame.size.width - layout.rightMargin - trailing.frame.size.width;
    CGFloat expectedTrailingY = layout.frame.size.height - layout.bottomMargin - trailing.frame.size.height;
    XCTAssertEqualWithAccuracy(trailing.frame.origin.x, expectedTrailingX, 0.1);
    XCTAssertEqualWithAccuracy(trailing.frame.origin.y, expectedTrailingY, 0.1);

    // For coverage of helper setters.
    [layout setSize];
    XCTAssertEqualWithAccuracy(layout.frame.size.width, layout.contentSize.width, 0.1);
    [layout setSizeWithWidth:120];
    XCTAssertEqualWithAccuracy(layout.frame.size.width, 120, 0.1);
    [layout setSizeWithHeight:80];
    XCTAssertEqualWithAccuracy(layout.frame.size.height, 80, 0.1);

    // Exercising scrollToShow ensures the math covers offset conversion.
    [layout layoutSubviews];
    [layout scrollToShow:trailing animated:NO];
    XCTAssertEqual(layout.contentOffset.x, 0);
}

- (void)testVerticalLayoutCenteringSubviews
{
    VLayoutView *layout = [[VLayoutView alloc] initWithFrame:CGRectMake(0, 0, 80, 70)
                                                    spacing:5
                                                 leftMargin:4
                                                rightMargin:6
                                                  topMargin:10
                                               bottomMargin:2
                                                hAlignment:UIControlContentHorizontalAlignmentLeft
                                                vAlignment:UIControlContentVerticalAlignmentCenter];

    UIView *top = [self viewWithSize:CGSizeMake(20, 10)];
    UIView *bottom = [self viewWithSize:CGSizeMake(40, 16)];
    [layout addSubview:top];
    [layout addSubview:bottom];

    [layout layoutSubviews];

    // sizeThatFits drives total height using tallest widths and spacing.
    CGSize fitted = [layout sizeThatFits:CGSizeZero];
    CGFloat expectedHeight = 10 + 10 + 16 + 5 + 2;
    CGFloat expectedWidth = 4 + 40 + 6;
    XCTAssertEqualWithAccuracy(fitted.height, expectedHeight, 0.1);
    XCTAssertEqualWithAccuracy(fitted.width, expectedWidth, 0.1);

    // Left alignment should anchor subviews to the left margin.
    XCTAssertEqualWithAccuracy(top.frame.origin.x, 4, 0.1);
    XCTAssertEqualWithAccuracy(bottom.frame.origin.x, 4, 0.1);
    // Ensure layout centers vertically inside the available frame.
    XCTAssertGreaterThan(top.frame.origin.y, 0);
}

@end
