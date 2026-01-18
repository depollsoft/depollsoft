//
//  DPEnumTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
// Define a sample enum type for testing
#define ENUM_IMPLEMENTATION
#import "DPEnum.h"
DEFINE_ENUM(SampleEnum, Alpha = 0, Beta, Gamma = 5, Delta)
IMPLEMENT_ENUM(SampleEnum)

@interface DPEnumTests : XCTestCase
@end

@implementation DPEnumTests

- (void)testInitAndName {
    SampleEnum *a = [SampleEnum enumWithInt:0];
    STAssertEquals([a get], 0, @"Value should be 0");
    STAssertEqualObjects([a name], @"Alpha", @"Name for 0 should be Alpha");

    SampleEnum *g = [SampleEnum enumWithInt:5];
    STAssertEqualObjects([g name], @"Gamma", @"Name for 5 should be Gamma");
}

- (void)testEnumWithStringAndEquality {
    SampleEnum *betaA = [SampleEnum enumWithString:@"Beta"];
    SampleEnum *betaB = [SampleEnum enumWithInt:1];
    STAssertTrue([betaA isEqual:betaB], @"Enums with same value should be equal");
}

@end
