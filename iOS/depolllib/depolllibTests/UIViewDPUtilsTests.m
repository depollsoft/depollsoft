//
//  UIViewDPUtilsTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import <UIKit/UIKit.h>
#import "UIView+DPUtils.h"
#import "DPUtils+UIControl.h"
#import "DPUtils+NSString.h"
#import "UIToolbar+DPUtils.h"

@interface UIViewDPUtilsTests : XCTestCase
@end

@implementation UIViewDPUtilsTests

- (void)testPaddingHelpersWrapView {
    UIView *inner = [[UIView alloc] initWithFrame:CGRectZero];

    UIView *paddedAll = [inner pad:8];
    STAssertNotNil(paddedAll, @"pad should return a container view");
    STAssertTrue(paddedAll != inner, @"Returned container should not be the same instance");
    STAssertTrue([[paddedAll subviews] containsObject:inner], @"Container should contain the inner view");

    UIView *paddedHV = [inner padHorizontal:4 vertical:6];
    STAssertNotNil(paddedHV, @"padHorizontal:vertical: should return a container view");
    STAssertTrue([[paddedHV subviews] containsObject:inner], @"Container should contain the inner view");
}

- (void)testCenteringHelpersWrapView {
    UIView *inner = [[UIView alloc] initWithFrame:CGRectZero];

    UIView *centered = [inner centered];
    STAssertNotNil(centered, @"centered should return a container view");
    STAssertTrue([[centered subviews] containsObject:inner], @"Container should contain the inner view");

    UIView *centeredV = [inner centeredVertically];
    STAssertNotNil(centeredV, @"centeredVertically should return a container view");

    UIView *centeredH = [inner centeredHorizontally];
    STAssertNotNil(centeredH, @"centeredHorizontally should return a container view");
}

- (void)testAlignAndFixHelpers {
    UIView *inner = [[UIView alloc] initWithFrame:CGRectZero];

    STAssertNotNil([inner alignTop], @"alignTop should return container");
    STAssertNotNil([inner alignBottom], @"alignBottom should return container");
    STAssertNotNil([inner alignLeft], @"alignLeft should return container");
    STAssertNotNil([inner alignRight], @"alignRight should return container");
    STAssertNotNil([inner fixHeight:10], @"fixHeight should return container");
    STAssertNotNil([inner fixWidth:20], @"fixWidth should return container");
}

- (void)testSafeAreaGuideHelpersReturnLayoutGuides {
    UIView *container = [[UIView alloc] initWithFrame:CGRectMake(0, 0, 100, 100)];
    UILayoutGuide *top = [container topSafeAreaLayoutGuide];
    UILayoutGuide *left = [container leftSafeAreaLayoutGuide];
    UILayoutGuide *right = [container rightSafeAreaLayoutGuide];
    STAssertNotNil(top, @"Top safe area guide should be created");
    STAssertNotNil(left, @"Left safe area guide should be created");
    STAssertNotNil(right, @"Right safe area guide should be created");
}

@end

// Additional integrated tests to exercise uncovered categories without
// requiring Xcode project file changes.

@interface DPUIControlBlocksIntegratedTests : XCTestCase @end
@implementation DPUIControlBlocksIntegratedTests
- (void)testAddBlockInvokesOnEvent {
    UIButton *button = [UIButton buttonWithType:UIButtonTypeSystem];
    __block BOOL invoked = NO;
    id token = [button addBlock:^{ invoked = YES; } forControlEvents:UIControlEventTouchUpInside];
    STAssertNotNil(token, @"Should return a token for the added block");
    // Hostless tests cannot dispatch UIControl actions via UIApplication.
    // Invoke the returned delegator directly instead.
    if ([token respondsToSelector:@selector(invoke)]) {
        #pragma clang diagnostic push
        #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
        [token performSelector:@selector(invoke)];
        #pragma clang diagnostic pop
    }
    STAssertTrue(invoked, @"Block should be invoked when calling delegator");
}
@end

@interface DPNSStringUtilsIntegratedTests : XCTestCase @end
@implementation DPNSStringUtilsIntegratedTests
- (void)testStringByURLEncoding {
    NSString *input = @"A b.c-_.~!@#";
    NSString *encoded = [input stringByURLEncoding];
    STAssertTrue([encoded containsString:@"A+"], @"Space should be encoded as '+'");
    STAssertTrue([encoded containsString:@"b.c-_.~"], @"Unreserved chars should remain");
    STAssertTrue([encoded containsString:@"%21"], @"'!' should be percent-encoded");
    STAssertTrue([encoded containsString:@"%40"], @"'@' should be percent-encoded");
}
- (void)testStringWithUUID {
    NSString *uuid = [NSString stringWithUUID];
    STAssertNotNil(uuid, @"UUID should not be nil");
    STAssertTrue(uuid.length > 0, @"UUID should be non-empty");
    STAssertTrue([uuid containsString:@"-"], @"UUID should contain hyphens");
}
@end

@interface UIToolbarDPUtilsIntegratedTests : XCTestCase @end
@implementation UIToolbarDPUtilsIntegratedTests
- (void)testAddTitleAddsLabelToToolbar {
    UIToolbar *toolbar = [[UIToolbar alloc] initWithFrame:CGRectMake(0, 0, 320, 44)];
    UILabel *label = [toolbar addTitle:@"Hello"];
    STAssertNotNil(label, @"Label should be returned");
    STAssertEquals(label.superview, toolbar, @"Label should be added to the toolbar");
    STAssertEqualObjects(label.text, @"Hello", @"Label text should match input");
}
@end
