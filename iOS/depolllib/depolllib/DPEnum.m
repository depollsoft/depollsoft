//
//  DPEnum.m
//  depolllib
//
//  Created by David Poll on 4/29/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPEnum.h"

@implementation DPEnum

@synthesize value = _value;

- (id)init {
    return [self initWithValue:[NSNumber numberWithInt:0]];
}

- (id)initWithInt:(int)value {
    return [self initWithValue:[NSNumber numberWithInt:value]];
}

- (id)initWithString:(NSString *)string {
    return [self initWithValue:[[self.class getEnumValues] objectForKey:string]];
}

- (id)initWithValue:(NSNumber *)value {
    if (self = [super init]) {
        _value = value;
    }
    return self;
}

- (int)get {
    return [self.value intValue];
}

+ (id)enumWithInt:(int)value {
    return [[self alloc] initWithInt:value];
}

+ (id)enumWithString:(NSString *)string {
    return [[self alloc] initWithString:string];
}

+ (id)enumWithValue:(NSNumber *)value {
    return [[self alloc] initWithValue:value];
}

- (NSString *)name {
    return [[self.class getEnumValues] allKeysForObject:self.value].lastObject;
}

+ (NSDictionary *)getEnumValues {
    return [NSDictionary dictionary];
}

- (BOOL)isEqual:(id)object {
    DPEnum *obj = object;
    return [self.value isEqual:obj.value];
}

@end
