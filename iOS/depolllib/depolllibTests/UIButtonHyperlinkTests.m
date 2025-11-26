//
//  UIButtonHyperlinkTests.m
//  depolllibTests
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import <UIKit/UIKit.h>
#import "UIButton+Hyperlink.h"

@interface UIButtonHyperlinkTests : XCTestCase
@end

@implementation UIButtonHyperlinkTests

- (void)testSetAndGetURLProperty {
    UIButton *button = [UIButton buttonWithType:UIButtonTypeSystem];
    NSURL *u = [NSURL URLWithString:@"https://depollsoft.xyz/"];
    button.url = u;
    STAssertEqualObjects(button.url, u, @"URL property should round-trip via associated object");
}

@end

