//
//  DPUtilsTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <SenTestingKit/SenTestingKit.h>
#import <UIKit/UIKit.h>
#import "DPUtils+NSString.h"

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

@end

