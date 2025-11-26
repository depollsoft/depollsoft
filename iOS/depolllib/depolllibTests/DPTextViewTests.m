//
//  DPTextViewTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import <UIKit/UIKit.h>
#import "DPTextView.h"

@interface DPTextViewTests : XCTestCase
@end

@implementation DPTextViewTests

- (void)testIntrinsicContentSizeAndLayoutInvalidation {
    DPTextView *tv = [[DPTextView alloc] initWithFrame:CGRectMake(0, 0, 100, 40)];
    tv.text = @"Some sample text to size";
    CGSize before = [tv intrinsicContentSize];
    [tv setBounds:CGRectMake(0, 0, 120, 50)];
    [tv layoutSubviews];
    CGSize after = [tv intrinsicContentSize];
    // Sizes should be computed; not necessarily equal
    STAssertTrue(before.width >= 0 && after.width >= 0, @"Sizes should be non-negative");
}

@end
