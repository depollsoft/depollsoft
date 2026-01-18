//
//  DPBusyIndicatorTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import <UIKit/UIKit.h>
#import "DPBusyIndicator.h"

@interface DPBusyIndicatorTests : XCTestCase
@end

@implementation DPBusyIndicatorTests

- (void)testOverlayVisibilityAndBusyCount {
    DPBusyIndicator *indicator = [[DPBusyIndicator alloc] initWithFrame:CGRectMake(0, 0, 100, 100)];

    STAssertNotNil(indicator.overlay, @"Overlay should be created in commonInit");
    STAssertTrue(indicator.overlay.hidden, @"Overlay should be hidden when not busy");
    STAssertEquals(indicator.busyCount, (NSUInteger)0, @"Initial busyCount should be 0");

    [indicator incrementBusyCount];
    STAssertEquals(indicator.busyCount, (NSUInteger)1, @"Busy count should increment");
    STAssertFalse(indicator.overlay.hidden, @"Overlay should show while busy");
    STAssertTrue(indicator.isUserInteractionEnabled, @"User interaction should be enabled while busy");

    [indicator decrementBusyCount];
    STAssertEquals(indicator.busyCount, (NSUInteger)0, @"Busy count should decrement");
    STAssertTrue(indicator.overlay.hidden, @"Overlay should be hidden when no longer busy");

    [indicator incrementBusyCount];
    [indicator clearBusyCount];
    STAssertEquals(indicator.busyCount, (NSUInteger)0, @"clearBusyCount resets busyCount");
    STAssertTrue(indicator.overlay.hidden, @"Overlay hidden after clearBusyCount");
}

- (void)testSetChildReplacesSubview {
    DPBusyIndicator *indicator = [[DPBusyIndicator alloc] initWithFrame:CGRectMake(0, 0, 50, 50)];
    UIView *child1 = [[UIView alloc] init];
    UIView *child2 = [[UIView alloc] init];

    indicator.child = child1;
    STAssertEquals(child1.superview, indicator, @"Child1 should be added");
    indicator.child = child2;
    STAssertNil(child1.superview, @"Child1 should be removed when replacing child");
    STAssertEquals(child2.superview, indicator, @"Child2 should be added");
}

@end
