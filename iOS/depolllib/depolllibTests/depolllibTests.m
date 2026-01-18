//
//  depolllibTests.m
//  depolllibTests
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "depolllibTests.h"
#import "DPJsonSerializer.h"
#define ENUM_IMPLEMENTATION
#import "DPEnum.h"

DEFINE_ENUM(SpecialEnum, Zero = 0, One, Two, Three, Five = 5)
IMPLEMENT_ENUM(SpecialEnum)

@interface TestClassA : NSObject

@property int intProp;
@property double doubleProp;
@property NSString *stringProp;
@property (strong) SpecialEnum *enumProp;
@property (strong) id objectProp;
@property (readonly) int readOnlyIntProp;
@property NSArray *arrayProp;
@property BOOL boolProp;
@property unsigned char ucharProp;

@end

@implementation TestClassA

@synthesize arrayProp, doubleProp, enumProp, intProp, objectProp, stringProp, readOnlyIntProp, boolProp, ucharProp;

- (BOOL)isEqual:(id)object {
    TestClassA *obj = object;
    return self.intProp == obj.intProp && self.doubleProp == obj.doubleProp && (self.stringProp == obj.stringProp || [self.stringProp isEqual:obj.stringProp]) && (self.enumProp == obj.enumProp || [self.enumProp isEqual:obj.enumProp]) && (self.objectProp == obj.objectProp || [self.objectProp isEqual:obj.objectProp]) && (self.arrayProp == obj.arrayProp || [self.arrayProp isEqual:obj.arrayProp]) && self.boolProp == obj.boolProp && self.ucharProp == obj.ucharProp;
}

@end

@interface TestClassB : NSObject

@property int intProp;
@property double doubleProp;
@property NSString *stringProp;
@property (strong) SpecialEnum *enumProp;
@property (strong) id objectProp;
@property (readonly) int readOnlyIntProp;
@property NSArray *arrayProp;
@property BOOL boolProp;
@property unsigned char ucharProp;

@end

@implementation TestClassB

@synthesize arrayProp, doubleProp, enumProp, intProp, objectProp, stringProp, readOnlyIntProp, boolProp, ucharProp;

- (BOOL)isEqual:(id)object {
    TestClassB *obj = object;
    return self.intProp == obj.intProp && self.doubleProp == obj.doubleProp && (self.stringProp == obj.stringProp || [self.stringProp isEqual:obj.stringProp]) && (self.enumProp == obj.enumProp || [self.enumProp isEqual:obj.enumProp]) && (self.objectProp == obj.objectProp || [self.objectProp isEqual:obj.objectProp]) && (self.arrayProp == obj.arrayProp || [self.arrayProp isEqual:obj.arrayProp]) && self.boolProp == obj.boolProp && self.ucharProp == obj.ucharProp;
}

@end

@interface TestSubClassA : TestClassA

@property int subIntProp;

@end

@implementation TestSubClassA

@synthesize subIntProp;

- (BOOL)isEqual:(id)object {
    return [super isEqual:object] && self.subIntProp == [object subIntProp];
}

@end

@implementation depolllibTests


- (void)setUp
{
    [super setUp];
}

- (void)tearDown
{
    [DPJsonSerializer clearAliases];
    [super tearDown];
}

- (void)testEnum {
    NSDictionary *enumDict = [SpecialEnum getEnumValues];
    NSDictionary *sourceDict = [NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithInt:Zero], @"Zero", [NSNumber numberWithInt:One], @"One", [NSNumber numberWithInt:Two], @"Two", [NSNumber numberWithInt:Three], @"Three", [NSNumber numberWithInt:Five], @"Five", nil];
    STAssertEqualObjects(enumDict, sourceDict, @"enumValues should be correct.");
}

- (void)testBool {
    bool b = false;
    if (b) {
        STFail(@"false should not hit the 'true' condition in the if statement");
    }
    b = true;
    if (b) {
        
    } else {
        STFail(@"true should not hit the 'false' condition in the if statement");
    }
    b = NO;
    if (b) {
        STFail(@"NO should not hit the 'true' condition in the if statement");
    }
    b = YES;
    if (b) {
        
    } else {
        STFail(@"YES should not hit the 'false' condition in the if statement");
    }
}

- (void)testSimpleSerialization {
    TestClassA *a = [[TestClassA alloc] init];
    NSDictionary *serialized = [DPJsonSerializer serialize:a];
    STAssertEquals([[[serialized objectForKey:@"IntProp"] objectForKey:@"Value"] intValue], 0, @"intProp should be zero");
    STAssertEqualObjects([[serialized objectForKey:@"IntProp"] objectForKey:@"*type"], @"DPJsonPrimitive", @"intProp should be a primitive");
    STAssertEqualObjects([[serialized objectForKey:@"IntProp"] objectForKey:@"Type"], [NSString stringWithUTF8String:@encode(int)], @"intProp's primitive type should be int");
    
    STAssertEquals([[[serialized objectForKey:@"DoubleProp"] objectForKey:@"Value"] doubleValue], 0.0, @"doubleProp should be zero");
    STAssertEqualObjects([[serialized objectForKey:@"DoubleProp"] objectForKey:@"*type"], @"DPJsonPrimitive", @"doubleProp should be a primitive");
    STAssertEqualObjects([[serialized objectForKey:@"DoubleProp"] objectForKey:@"Type"], [NSString stringWithUTF8String:@encode(double)], @"doubleProp's primitive type should be int");
    
    STAssertNil([serialized objectForKey:@"StringProp"], @"stringProp should be omitted when nil");
    
    STAssertNil([serialized objectForKey:@"ReadOnlyInt"], @"readOnlyInt should not have been serialized.");
}

- (void)testSimpleSerializationWithData {
    TestClassA *a = [[TestClassA alloc] init];
    a.intProp = 5;
    a.doubleProp = 1.234;
    a.arrayProp = [NSArray arrayWithObjects:[NSNumber numberWithInt:1], [NSNumber numberWithInt:2], [NSNumber numberWithInt:3], nil];
    NSDictionary *serialized = [DPJsonSerializer serialize:a];
    STAssertEquals([[[serialized objectForKey:@"IntProp"] objectForKey:@"Value"] intValue], 5, @"intProp should be 5");
    STAssertEqualObjects([[serialized objectForKey:@"IntProp"] objectForKey:@"*type"], @"DPJsonPrimitive", @"intProp should be a primitive");
    STAssertEqualObjects([[serialized objectForKey:@"IntProp"] objectForKey:@"Type"], [NSString stringWithUTF8String:@encode(int)], @"intProp's primitive type should be int");
    
    STAssertEquals([[[serialized objectForKey:@"DoubleProp"] objectForKey:@"Value"] doubleValue], 1.234, @"doubleProp should have the right value");
    STAssertEqualObjects([[serialized objectForKey:@"DoubleProp"] objectForKey:@"*type"], @"DPJsonPrimitive", @"doubleProp should be a primitive");
    STAssertEqualObjects([[serialized objectForKey:@"DoubleProp"] objectForKey:@"Type"], [NSString stringWithUTF8String:@encode(double)], @"doubleProp's primitive type should be int");
    
    STAssertNil([serialized objectForKey:@"StringProp"], @"stringProp should be omitted when nil");
    
    STAssertNil([serialized objectForKey:@"ReadOnlyInt"], @"readOnlyInt should not have been serialized.");
    
    STAssertTrue([[[serialized objectForKey:@"ArrayProp"] objectForKey:@"*items"] isKindOfClass:[NSArray class]], @"arrayProp's items should be an array.");
    STAssertEquals([[[serialized objectForKey:@"ArrayProp"] objectForKey:@"*items"] count], 3u, @"arrayProp should have 3 elements");
}

- (void)testRoundTrip {
    TestClassA *a = [[TestClassA alloc] init];
    a.intProp = 5;
    a.doubleProp = 1.234;
    a.arrayProp = [NSArray arrayWithObjects:[NSNumber numberWithInt:1], [NSNumber numberWithInt:2], [NSNumber numberWithInt:3], nil];
    TestClassB *b = [[TestClassB alloc] init];
    a.objectProp = b;
    b.boolProp = YES;
    TestSubClassA *subA = [[TestSubClassA alloc] init];
    b.objectProp = subA;
    b.arrayProp = [NSArray arrayWithObject:[[TestClassA alloc] init]];
    b.enumProp = [[SpecialEnum alloc] initWithInt:Zero];
    subA.subIntProp = 17;
    NSDictionary *serialized = [DPJsonSerializer serialize:a];
    TestClassA *aAgain = [DPJsonSerializer deserializeDictionary:serialized];
    STAssertEqualObjects(aAgain, a, @"Round trip should succeed.");
}


@end
