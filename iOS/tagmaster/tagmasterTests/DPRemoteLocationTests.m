//
//  DPRemoteLocationTests.m
//  tagmasterTests
//
//  Covers DPRemoteLocation cacheKey behavior.
//

#import <XCTest/XCTest.h>
#import "DPRemoteLocation.h"

@interface DPRemoteLocationTests : XCTestCase
@end

@implementation DPRemoteLocationTests

- (void)testCacheKeyIncludesSanitizedURLAndType {
    DPRemoteLocation *loc = [[DPRemoteLocation alloc] init];
    loc.uri = [NSURL URLWithString:@"https://example.com/a/b:c"];
    loc.type = @"mp3";
    NSString *key = [loc cacheKey];
    XCTAssertNotNil(key);
    XCTAssertTrue([key containsString:@"_SLASH_"]);
    XCTAssertTrue([key containsString:@"_COLON_"]);
    XCTAssertTrue([key hasSuffix:@".mp3"], @"Type should be appended after a dot");
}

@end

