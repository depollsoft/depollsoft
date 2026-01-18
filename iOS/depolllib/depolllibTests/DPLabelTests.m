//
//  DPLabelTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import <UIKit/UIKit.h>
#import "DPLabel.h"

@interface DPLabelTests : XCTestCase
@end

@implementation DPLabelTests

- (void)testIntrinsicContentSizeMultiLineWidthIsNoIntrinsicMetric {
    DPLabel *label = [[DPLabel alloc] initWithFrame:CGRectZero];
    label.text = @"Hello\nWorld";
    label.numberOfLines = 2;
    CGSize size = [label intrinsicContentSize];
    STAssertEquals(size.width, (CGFloat)UIViewNoIntrinsicMetric, @"Width should be UIViewNoIntrinsicMetric when multi-line");
}

@end
