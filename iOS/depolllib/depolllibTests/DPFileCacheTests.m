//
//  DPFileCacheTests.m
//  depolllibTests
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import "DPFileCache.h"
#import "DPJsonSerializer.h"

@interface CacheTestObject : NSObject
@property NSString *name;
@end

@implementation CacheTestObject
@end

@interface DPFileCacheTests : XCTestCase
@end

@implementation DPFileCacheTests

- (void)testWriteReadDataAndObject {
    NSString *key = @"unit_test_key.dat";
    NSData *data = [@"hello" dataUsingEncoding:NSUTF8StringEncoding];
    [DPFileCache writeData:data forKey:key];
    NSData *readBack = [DPFileCache readDataForKey:key];
    STAssertEqualObjects(data, readBack, @"Raw data round-trip");

    CacheTestObject *obj = [[CacheTestObject alloc] init];
    obj.name = @"cached";
    [DPFileCache writeObject:obj forKey:@"obj_key.plist"];
    id back = [DPFileCache readObjectForKey:@"obj_key.plist"];
    STAssertTrue([back isKindOfClass:[CacheTestObject class]], @"Should deserialize back to object");
}

- (void)testPathAndKeyForURL {
    NSString *path = [DPFileCache pathForKey:@"a/b:c.txt"];
    STAssertTrue([path hasSuffix:@"a/b:c.txt"], @"Path should append key");
    NSURL *url = [NSURL URLWithString:@"https://example.com/a/b:c"]; 
    NSString *key = [DPFileCache keyForURL:url];
    STAssertFalse([key containsString:@"/"], @"Key should replace slashes");
    STAssertFalse([key containsString:@":"], @"Key should replace colons");
}

@end

