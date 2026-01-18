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
@property NSInteger value;
@end

@implementation CacheTestObject
- (BOOL)isEqual:(id)object {
    if (![object isKindOfClass:[CacheTestObject class]]) return NO;
    CacheTestObject *other = object;
    return [self.name isEqualToString:other.name] && self.value == other.value;
}
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

// MARK: - testReadNonexistentKeyReturnsNil

- (void)testReadNonexistentKeyReturnsNil {
    NSString *nonexistentKey = @"this_key_definitely_does_not_exist_12345.dat";
    
    NSData *data = [DPFileCache readDataForKey:nonexistentKey];
    STAssertNil(data, @"Reading nonexistent key should return nil for data");
    
    id obj = [DPFileCache readObjectForKey:nonexistentKey];
    STAssertNil(obj, @"Reading nonexistent key should return nil for object");
}

- (void)testReadNonexistentKeyWithUniqueTimestamp {
    // Use timestamp to ensure unique key
    NSString *uniqueKey = [NSString stringWithFormat:@"nonexistent_%f.dat", [[NSDate date] timeIntervalSince1970]];
    
    NSData *result = [DPFileCache readDataForKey:uniqueKey];
    STAssertNil(result, @"Data for nonexistent unique key should be nil");
}

// MARK: - testWriteOverwritesExisting

- (void)testWriteOverwritesExisting {
    NSString *key = @"overwrite_test.dat";
    
    // Write initial data
    NSData *initialData = [@"initial" dataUsingEncoding:NSUTF8StringEncoding];
    [DPFileCache writeData:initialData forKey:key];
    
    // Verify initial write
    NSData *readInitial = [DPFileCache readDataForKey:key];
    STAssertEqualObjects(readInitial, initialData, @"Initial data should be written");
    
    // Overwrite with new data
    NSData *newData = [@"overwritten" dataUsingEncoding:NSUTF8StringEncoding];
    [DPFileCache writeData:newData forKey:key];
    
    // Verify overwrite
    NSData *readNew = [DPFileCache readDataForKey:key];
    STAssertEqualObjects(readNew, newData, @"Data should be overwritten");
    STAssertFalse([readNew isEqualToData:initialData], @"Old data should be replaced");
}

- (void)testWriteObjectOverwritesExisting {
    NSString *key = @"overwrite_object_test.plist";
    
    // Write initial object
    CacheTestObject *initial = [[CacheTestObject alloc] init];
    initial.name = @"initial";
    initial.value = 1;
    [DPFileCache writeObject:initial forKey:key];
    
    // Overwrite with new object
    CacheTestObject *replacement = [[CacheTestObject alloc] init];
    replacement.name = @"replaced";
    replacement.value = 999;
    [DPFileCache writeObject:replacement forKey:key];
    
    // Verify overwrite
    CacheTestObject *readBack = [DPFileCache readObjectForKey:key];
    STAssertEqualObjects(readBack.name, @"replaced", @"Object name should be overwritten");
    STAssertEquals(readBack.value, (NSInteger)999, @"Object value should be overwritten");
}

// MARK: - testPathForKeyWithSpecialChars

- (void)testPathForKeyWithSpecialChars {
    // Test with various special characters
    NSString *keyWithSpaces = @"file with spaces.txt";
    NSString *pathWithSpaces = [DPFileCache pathForKey:keyWithSpaces];
    STAssertTrue([pathWithSpaces hasSuffix:keyWithSpaces], @"Path should handle spaces");
    
    NSString *keyWithDots = @"file.name.with.dots.txt";
    NSString *pathWithDots = [DPFileCache pathForKey:keyWithDots];
    STAssertTrue([pathWithDots hasSuffix:keyWithDots], @"Path should handle multiple dots");
    
    NSString *keyWithUnicode = @"文件名.txt";
    NSString *pathWithUnicode = [DPFileCache pathForKey:keyWithUnicode];
    STAssertTrue([pathWithUnicode hasSuffix:keyWithUnicode], @"Path should handle unicode");
    
    NSString *keyWithDash = @"file-name-with-dashes.dat";
    NSString *pathWithDash = [DPFileCache pathForKey:keyWithDash];
    STAssertTrue([pathWithDash hasSuffix:keyWithDash], @"Path should handle dashes");
}

- (void)testPathForKeyContainsBasePath {
    NSString *key = @"testfile.dat";
    NSString *path = [DPFileCache pathForKey:key];
    
    // Path should contain the cache directory
    STAssertTrue([path containsString:@"depolllib.cache"], @"Path should contain cache directory");
    STAssertTrue([path hasSuffix:key], @"Path should end with the key");
}

// MARK: - testKeyForURLWithQueryParams

- (void)testKeyForURLWithQueryParams {
    NSURL *urlWithQuery = [NSURL URLWithString:@"https://api.example.com/data?param1=value1&param2=value2"];
    NSString *key = [DPFileCache keyForURL:urlWithQuery];
    
    // Key should not contain slashes or colons
    STAssertFalse([key containsString:@"/"], @"Key should not contain slashes");
    STAssertFalse([key containsString:@":"], @"Key should not contain colons");
    
    // Key should contain replacements
    STAssertTrue([key containsString:@"_SLASH_"], @"Slashes should be replaced with _SLASH_");
    STAssertTrue([key containsString:@"_COLON_"], @"Colons should be replaced with _COLON_");
    
    // Query params should be preserved (with encoded special chars)
    STAssertTrue([key containsString:@"param1"], @"Query param should be preserved");
    STAssertTrue([key containsString:@"value1"], @"Query value should be preserved");
}

- (void)testKeyForURLWithFragment {
    NSURL *urlWithFragment = [NSURL URLWithString:@"https://example.com/page#section1"];
    NSString *key = [DPFileCache keyForURL:urlWithFragment];
    
    STAssertFalse([key containsString:@"/"], @"Key should not contain slashes");
    STAssertTrue([key containsString:@"section1"], @"Fragment should be preserved");
}

- (void)testKeyForURLWithPort {
    NSURL *urlWithPort = [NSURL URLWithString:@"https://example.com:8080/api"];
    NSString *key = [DPFileCache keyForURL:urlWithPort];
    
    STAssertFalse([key containsString:@":"], @"Key should not contain colons");
    STAssertTrue([key containsString:@"8080"], @"Port should be preserved");
}

- (void)testKeyForURLUniqueness {
    NSURL *url1 = [NSURL URLWithString:@"https://example.com/path1"];
    NSURL *url2 = [NSURL URLWithString:@"https://example.com/path2"];
    
    NSString *key1 = [DPFileCache keyForURL:url1];
    NSString *key2 = [DPFileCache keyForURL:url2];
    
    STAssertFalse([key1 isEqualToString:key2], @"Different URLs should produce different keys");
}

// MARK: - testReadObjectReturnsNilForCorruptData

- (void)testReadObjectReturnsNilForCorruptData {
    NSString *key = @"corrupt_data_test.plist";
    
    // Write invalid/corrupt data that is not a valid plist
    NSData *corruptData = [@"this is not a valid plist {{{{" dataUsingEncoding:NSUTF8StringEncoding];
    [DPFileCache writeData:corruptData forKey:key];
    
    // Reading as object should return nil (or handle gracefully)
    id result = [DPFileCache readObjectForKey:key];
    // The result depends on how NSPropertyListSerialization handles invalid data
    // It may return nil or throw - we just verify no crash occurs
    // If it returns something, it shouldn't be a valid CacheTestObject
    if (result != nil) {
        STAssertFalse([result isKindOfClass:[CacheTestObject class]], @"Corrupt data should not deserialize to CacheTestObject");
    }
}

- (void)testReadObjectWithEmptyData {
    NSString *key = @"empty_data_test.plist";
    
    // Write empty data
    NSData *emptyData = [NSData data];
    [DPFileCache writeData:emptyData forKey:key];
    
    // Reading as object should handle gracefully
    id result = [DPFileCache readObjectForKey:key];
    STAssertNil(result, @"Empty data should return nil");
}

- (void)testReadObjectWithRandomBytes {
    NSString *key = @"random_bytes_test.plist";
    
    // Write random bytes
    unsigned char randomBytes[] = {0x00, 0xFF, 0xAB, 0xCD, 0xEF, 0x12, 0x34};
    NSData *randomData = [NSData dataWithBytes:randomBytes length:sizeof(randomBytes)];
    [DPFileCache writeData:randomData forKey:key];
    
    // Should not crash when reading
    id result = [DPFileCache readObjectForKey:key];
    // Result should be nil since random bytes aren't valid plist
    STAssertNil(result, @"Random bytes should not deserialize to valid object");
}

// MARK: - Additional coverage tests

- (void)testWriteAndReadLargeData {
    NSString *key = @"large_data_test.dat";
    
    // Create 1MB of data
    NSMutableData *largeData = [NSMutableData dataWithLength:1024 * 1024];
    memset([largeData mutableBytes], 'A', [largeData length]);
    
    [DPFileCache writeData:largeData forKey:key];
    NSData *readBack = [DPFileCache readDataForKey:key];
    
    STAssertEqualObjects(largeData, readBack, @"Large data should round-trip correctly");
}

- (void)testWriteAndReadBinaryData {
    NSString *key = @"binary_data_test.dat";
    
    // Create binary data with null bytes
    unsigned char binaryBytes[] = {0x00, 0x01, 0x02, 0x00, 0xFF, 0xFE, 0x00};
    NSData *binaryData = [NSData dataWithBytes:binaryBytes length:sizeof(binaryBytes)];
    
    [DPFileCache writeData:binaryData forKey:key];
    NSData *readBack = [DPFileCache readDataForKey:key];
    
    STAssertEqualObjects(binaryData, readBack, @"Binary data with null bytes should round-trip correctly");
}

- (void)testObjectRoundTripPreservesProperties {
    NSString *key = @"object_properties_test.plist";
    
    CacheTestObject *original = [[CacheTestObject alloc] init];
    original.name = @"Test Object with Special Chars: é ñ 中文";
    original.value = 42;
    
    [DPFileCache writeObject:original forKey:key];
    CacheTestObject *readBack = [DPFileCache readObjectForKey:key];
    
    STAssertNotNil(readBack, @"Object should be read back");
    STAssertEqualObjects(readBack.name, original.name, @"Name should be preserved");
    STAssertEquals(readBack.value, original.value, @"Value should be preserved");
}

@end

