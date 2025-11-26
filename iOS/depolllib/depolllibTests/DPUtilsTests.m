//
//  DPUtilsTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "DPUtils+NSString.h"
#import "DPUtils+UIControl.h"
#import "UIToolbar+DPUtils.h"

@interface DPUtilsTests : SenTestCase
@end

@implementation DPUtilsTests

- (void)testUUIDAndURLEncoding {
    NSString *uuid1 = [NSString stringWithUUID];
    NSString *uuid2 = [NSString stringWithUUID];
    STAssertTrue(uuid1.length > 0 && uuid2.length > 0, @"UUIDs should be non-empty");
    STAssertFalse([uuid1 isEqualToString:uuid2], @"Two UUIDs should differ");

    NSString *raw = @"hello world!";
    NSString *encoded = [raw stringByURLEncoding];
    STAssertEqualObjects(encoded, @"hello+world%21", @"Should URL-encode spaces and punctuation");
}

- (void)testUIControlBlockTarget {
    UIButton *button = [UIButton buttonWithType:UIButtonTypeSystem];
    __block BOOL tapped = NO;
    [button addBlock:^{ tapped = YES; } forControlEvents:UIControlEventTouchUpInside];
    [button sendActionsForControlEvents:UIControlEventTouchUpInside];
    STAssertTrue(tapped, @"Block should be invoked on control event");
}

- (void)testToolbarAddTitle {
    UIToolbar *tb = [[UIToolbar alloc] initWithFrame:CGRectMake(0, 0, 200, 44)];
    UILabel *label = [tb addTitle:@"My Title"];
    STAssertNotNil(label, @"Label should be created");
    STAssertEqualObjects(label.text, @"My Title", @"Label text should match");
    STAssertTrue([[tb subviews] containsObject:label], @"Label should be added as subview");
}

@end

