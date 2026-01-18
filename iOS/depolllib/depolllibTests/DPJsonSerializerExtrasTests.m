//
//  DPJsonSerializerExtrasTests.m
//  depolllibTests
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import "DPJsonSerializer.h"

@interface NumericHolderA : NSObject
@property char c;
@property short s;
@property long l;
@property long long ll;
@property unsigned char uc;
@property unsigned short us;
@property unsigned long ul;
@property unsigned long long ull;
@property float f;
@property double d;
@property BOOL b;
@property NSDate *date;
@end

@implementation NumericHolderA
- (BOOL)isEqual:(id)object {
    NumericHolderA *o = object;
    return self.c == o.c && self.s == o.s && self.l == o.l && self.ll == o.ll &&
           self.uc == o.uc && self.us == o.us && self.ul == o.ul && self.ull == o.ull &&
           fabs(self.f - o.f) < 0.0001 && fabs(self.d - o.d) < 0.0000001 && self.b == o.b &&
           ((self.date == o.date) || [self.date isEqualToDate:o.date]);
}
@end

@interface DPJsonSerializerExtrasTests : XCTestCase
@end

@implementation DPJsonSerializerExtrasTests

- (void)setUp {
    [super setUp];
    [DPJsonSerializer clearAliases];
}

- (void)tearDown {
    [DPJsonSerializer clearAliases];
    [super tearDown];
}

- (void)testRoundTripAllNumericTypes {
    NumericHolderA *a = [[NumericHolderA alloc] init];
    a.c = 'A';
    a.s = -1234;
    a.l = 123456;
    a.ll = -9876543210LL;
    a.uc = 1; // exercise special boolean mapping path for unsigned char
    a.us = 55555;
    a.ul = 123456789UL;
    a.ull = 9876543210ULL;
    a.f = 1.5f;
    a.d = 2.5;
    a.b = YES;

    NSDictionary *serialized = [DPJsonSerializer serialize:a];
    NumericHolderA *b = [DPJsonSerializer deserializeDictionary:serialized];
    STAssertEqualObjects(a, b, @"Numeric round-trip should match for all fields");
}

- (void)testClassAliasAndResolution {
    [DPJsonSerializer registerAlias:@"NumericAlias" forClass:[NumericHolderA class]];
    NumericHolderA *a = [[NumericHolderA alloc] init];
    NSDictionary *serialized = [DPJsonSerializer serialize:a];
    STAssertEqualObjects([serialized objectForKey:@"*type"], @"NumericAlias", @"Class alias should be used in *type");
    id back = [DPJsonSerializer deserializeDictionary:serialized];
    STAssertTrue([back isKindOfClass:[NumericHolderA class]], @"Alias should resolve back to the class");
}

- (void)testObjCTypeAliasForNSValue {
    int val = 123;
    [DPJsonSerializer registerAlias:@"int_alias" forObjCType:[NSString stringWithUTF8String:@encode(int)]];
    NSDictionary *prim = [DPJsonSerializer serialize:[NSValue value:&val withObjCType:@encode(int)]];
    STAssertEqualObjects([prim objectForKey:@"*type"], @"DPJsonPrimitive", @"Primitive wrapper *type");
    STAssertEqualObjects([prim objectForKey:@"Type"], @"int_alias", @"ObjC type alias should be used");
}

- (void)testCustomSerializerForNSDate {
    NSDateFormatter *fmt = [[NSDateFormatter alloc] init];
    fmt.locale = [NSLocale localeWithLocaleIdentifier:@"en_US_POSIX"];
    fmt.dateFormat = @"yyyy-MM-dd'T'HH:mm:ssZ";
    Class dateClass = [[NSDate date] class];
    [DPJsonSerializer registerSerializer:^NSString * (NSDate *date) {
        return [fmt stringFromDate:date];
    } deserializer:^id(NSString *string) {
        return [fmt dateFromString:string];
    } forClass:dateClass];

    NumericHolderA *a = [[NumericHolderA alloc] init];
    a.date = [NSDate dateWithTimeIntervalSince1970:123456.0];
    NSDictionary *serialized = [DPJsonSerializer serialize:a];
    NSDictionary *dateField = [serialized objectForKey:@"Date"];
    STAssertNotNil(dateField, @"Date field should be present");
    STAssertNotNil([dateField objectForKey:@"*serialized"], @"Custom serializer should write *serialized");
}

// MARK: - testSerializeNestedArrays

- (void)testSerializeNestedArrays {
    // Create an object with a nested array
    NSArray *innerArray = @[@"a", @"b", @"c"];
    NSArray *outerArray = @[innerArray, @"standalone"];
    
    NSDictionary *serialized = [DPJsonSerializer serialize:outerArray];
    STAssertNotNil(serialized, @"Serialized nested arrays should not be nil");
    STAssertEqualObjects([serialized objectForKey:@"*type"], @"__NSArrayI", @"Type should be array");
    
    NSArray *items = [serialized objectForKey:@"*items"];
    STAssertNotNil(items, @"Items should be present");
    STAssertEquals([items count], (NSUInteger)2, @"Should have 2 items");
}

// MARK: - testDeserializeNullValues

- (void)testDeserializeNullValues {
    // Create a serialized dictionary with null values
    NSDictionary *dict = @{
        @"*type": @"NumericHolderA",
        @"Name": [NSNull null]
    };
    
    id result = [DPJsonSerializer deserializeDictionary:dict];
    STAssertNotNil(result, @"Deserialized object should not be nil");
    STAssertTrue([result isKindOfClass:[NumericHolderA class]], @"Should deserialize to NumericHolderA");
}

// MARK: - testSerializeWithNilProperties

@end

// Test object with nil-able properties
@interface NilPropertyHolder : NSObject
@property (nonatomic, strong) NSString *name;
@property (nonatomic, strong) NSArray *items;
@property (nonatomic, strong) NSDictionary *data;
@end

@implementation NilPropertyHolder
@end

@interface DPJsonSerializerNilTests : XCTestCase
@end

@implementation DPJsonSerializerNilTests

- (void)setUp {
    [super setUp];
    [DPJsonSerializer clearAliases];
}

- (void)tearDown {
    [DPJsonSerializer clearAliases];
    [super tearDown];
}

- (void)testSerializeWithNilProperties {
    NilPropertyHolder *holder = [[NilPropertyHolder alloc] init];
    holder.name = nil;
    holder.items = nil;
    holder.data = nil;
    
    NSDictionary *serialized = [DPJsonSerializer serialize:holder];
    STAssertNotNil(serialized, @"Serialization should succeed with nil properties");
    STAssertEqualObjects([serialized objectForKey:@"*type"], @"NilPropertyHolder", @"Type should be set");
    
    // Nil properties should not be in the output
    STAssertNil([serialized objectForKey:@"Name"], @"Nil name should not be serialized");
    STAssertNil([serialized objectForKey:@"Items"], @"Nil items should not be serialized");
}

- (void)testSerializeWithMixedNilAndNonNilProperties {
    NilPropertyHolder *holder = [[NilPropertyHolder alloc] init];
    holder.name = @"test";
    holder.items = nil;
    holder.data = @{@"key": @"value"};
    
    NSDictionary *serialized = [DPJsonSerializer serialize:holder];
    STAssertNotNil(serialized, @"Serialization should succeed");
    STAssertEqualObjects([serialized objectForKey:@"Name"], @"test", @"Non-nil name should be serialized");
    STAssertNil([serialized objectForKey:@"Items"], @"Nil items should not be serialized");
    STAssertNotNil([serialized objectForKey:@"Data"], @"Non-nil data should be serialized");
}

// MARK: - testDeserializeUnknownClass

- (void)testDeserializeUnknownClass {
    // Test with a class that exists
    NSDictionary *dict = @{
        @"*type": @"NilPropertyHolder",
        @"Name": @"testValue"
    };
    
    id result = [DPJsonSerializer deserializeDictionary:dict];
    STAssertNotNil(result, @"Should deserialize known class");
    STAssertTrue([result isKindOfClass:[NilPropertyHolder class]], @"Should be correct type");
    STAssertEqualObjects([result name], @"testValue", @"Property should be set");
}

- (void)testGetClassForNameWithAlias {
    [DPJsonSerializer registerAlias:@"TestAlias" forClass:[NilPropertyHolder class]];
    
    Class result = [DPJsonSerializer getClassForName:@"TestAlias"];
    STAssertTrue(result == [NilPropertyHolder class], @"Should resolve alias to class");
    
    Class direct = [DPJsonSerializer getClassForName:@"NilPropertyHolder"];
    STAssertTrue(direct == [NilPropertyHolder class], @"Should resolve direct class name");
}

// MARK: - testSerializeNSURL

- (void)testSerializeNSURL {
    // Register a custom serializer for NSURL
    Class urlClass = [NSURL class];
    [DPJsonSerializer registerSerializer:^NSString *(NSURL *url) {
        return [url absoluteString];
    } deserializer:^id(NSString *string) {
        return [NSURL URLWithString:string];
    } forClass:urlClass];
    
    NSURL *testURL = [NSURL URLWithString:@"https://example.com/path?query=value"];
    NSDictionary *serialized = [DPJsonSerializer serialize:testURL];
    
    STAssertNotNil(serialized, @"URL serialization should not be nil");
    STAssertNotNil([serialized objectForKey:@"*serialized"], @"Should have serialized string");
    STAssertEqualObjects([serialized objectForKey:@"*serialized"], @"https://example.com/path?query=value", @"URL should be serialized correctly");
}

- (void)testDeserializeNSURL {
    Class urlClass = [NSURL class];
    [DPJsonSerializer registerSerializer:^NSString *(NSURL *url) {
        return [url absoluteString];
    } deserializer:^id(NSString *string) {
        return [NSURL URLWithString:string];
    } forClass:urlClass];
    
    NSDictionary *dict = @{
        @"*type": @"NSURL",
        @"*serialized": @"https://test.com/api"
    };
    
    id result = [DPJsonSerializer deserializeDictionary:dict];
    STAssertTrue([result isKindOfClass:[NSURL class]], @"Should deserialize to NSURL");
    STAssertEqualObjects([result absoluteString], @"https://test.com/api", @"URL should match");
}

// MARK: - testSerializeNSDate

- (void)testSerializeNSDate {
    NSDateFormatter *fmt = [[NSDateFormatter alloc] init];
    fmt.locale = [NSLocale localeWithLocaleIdentifier:@"en_US_POSIX"];
    fmt.dateFormat = @"yyyy-MM-dd'T'HH:mm:ssZ";
    fmt.timeZone = [NSTimeZone timeZoneForSecondsFromGMT:0];
    
    Class dateClass = [[NSDate date] class];
    [DPJsonSerializer registerSerializer:^NSString *(NSDate *date) {
        return [fmt stringFromDate:date];
    } deserializer:^id(NSString *string) {
        return [fmt dateFromString:string];
    } forClass:dateClass];
    
    NSDate *testDate = [NSDate dateWithTimeIntervalSince1970:1609459200]; // 2021-01-01 00:00:00 UTC
    NSDictionary *serialized = [DPJsonSerializer serialize:testDate];
    
    STAssertNotNil(serialized, @"Date serialization should not be nil");
    STAssertNotNil([serialized objectForKey:@"*serialized"], @"Should have serialized string");
    
    NSString *serializedString = [serialized objectForKey:@"*serialized"];
    STAssertTrue([serializedString containsString:@"2021-01-01"], @"Date string should contain correct date");
}

- (void)testDeserializeNSDate {
    NSDateFormatter *fmt = [[NSDateFormatter alloc] init];
    fmt.locale = [NSLocale localeWithLocaleIdentifier:@"en_US_POSIX"];
    fmt.dateFormat = @"yyyy-MM-dd'T'HH:mm:ssZ";
    fmt.timeZone = [NSTimeZone timeZoneForSecondsFromGMT:0];
    
    Class dateClass = [[NSDate date] class];
    [DPJsonSerializer registerSerializer:^NSString *(NSDate *date) {
        return [fmt stringFromDate:date];
    } deserializer:^id(NSString *string) {
        return [fmt dateFromString:string];
    } forClass:dateClass];
    
    NSDictionary *dict = @{
        @"*type": @"__NSDate",
        @"*serialized": @"2021-06-15T10:30:00+0000"
    };
    
    id result = [DPJsonSerializer deserializeDictionary:dict];
    STAssertTrue([result isKindOfClass:[NSDate class]], @"Should deserialize to NSDate");
}

// MARK: - Additional edge cases

- (void)testSerializeNilReturnsWrappedPrimitive {
    NSDictionary *serialized = [DPJsonSerializer serialize:nil];
    STAssertNotNil(serialized, @"Serializing nil should return a wrapped primitive");
    STAssertEqualObjects([serialized objectForKey:@"*type"], @"DPJsonPrimitive", @"Should wrap in DPJsonPrimitive");
}

- (void)testSerializeNSNull {
    NSDictionary *serialized = [DPJsonSerializer serialize:[NSNull null]];
    STAssertNotNil(serialized, @"Serializing NSNull should succeed");
    STAssertEqualObjects([serialized objectForKey:@"*type"], @"DPJsonPrimitive", @"Should wrap in DPJsonPrimitive");
}

- (void)testSerializeNSNumber {
    NSNumber *num = @42;
    NSDictionary *serialized = [DPJsonSerializer serialize:num];
    STAssertNotNil(serialized, @"Number serialization should succeed");
    STAssertEqualObjects([serialized objectForKey:@"*type"], @"DPJsonPrimitive", @"Should wrap in DPJsonPrimitive");
}

- (void)testSerializeEmptyArray {
    NSArray *empty = @[];
    NSDictionary *serialized = [DPJsonSerializer serialize:empty];
    STAssertNotNil(serialized, @"Empty array serialization should succeed");
    
    NSArray *items = [serialized objectForKey:@"*items"];
    STAssertNotNil(items, @"Items should be present");
    STAssertEquals([items count], (NSUInteger)0, @"Items should be empty");
}

- (void)testSerializeArrayOfStrings {
    NSArray *strings = @[@"hello", @"world"];
    NSDictionary *serialized = [DPJsonSerializer serialize:strings];
    STAssertNotNil(serialized, @"String array serialization should succeed");
    
    NSArray *items = [serialized objectForKey:@"*items"];
    STAssertEquals([items count], (NSUInteger)2, @"Should have 2 items");
    STAssertEqualObjects(items[0], @"hello", @"First item should be 'hello'");
    STAssertEqualObjects(items[1], @"world", @"Second item should be 'world'");
}

@end
