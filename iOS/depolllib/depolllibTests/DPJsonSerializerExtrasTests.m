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

@end
