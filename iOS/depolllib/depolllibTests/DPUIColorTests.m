//
//  DPUIColorTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import <UIKit/UIKit.h>
#import "DPUtils+UIColor.h"

@interface DPUIColorTests : XCTestCase
@end

@implementation DPUIColorTests

- (void)testInvertAndAlpha {
    UIColor *red = [UIColor colorWithRed:1 green:0 blue:0 alpha:1];
    UIColor *inverted = [red invert];

    const CGFloat *invComponents = CGColorGetComponents(inverted.CGColor);
    STAssertEqualsWithAccuracy(invComponents[0], (CGFloat)0.0, 0.0001, @"Red should invert to 0");
    STAssertEqualsWithAccuracy(invComponents[1], (CGFloat)1.0, 0.0001, @"Green should invert to 1");
    STAssertEqualsWithAccuracy(invComponents[2], (CGFloat)1.0, 0.0001, @"Blue should invert to 1");
    STAssertEqualsWithAccuracy(invComponents[3], (CGFloat)1.0, 0.0001, @"Alpha should be unchanged");

    UIColor *withAlpha = [red withAlpha:0.4];
    const CGFloat *alphaComponents = CGColorGetComponents(withAlpha.CGColor);
    STAssertEqualsWithAccuracy(alphaComponents[3], (CGFloat)0.4, 0.0001, @"Alpha should be updated");
}

@end
