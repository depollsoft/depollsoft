//
//  DPSerializer.m
//  depolllib
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPJsonSerializer.h"
#import "DPJsonPrimitive.h"
#import "DPEnum.h"
#import <objc/runtime.h>

static Class primitiveClass;

static NSMutableDictionary *typeSerializers;
static NSMutableDictionary *typeDeserializers;
static NSMutableDictionary *typeAliases; // Class -> String
static NSMutableDictionary *revTypeAliases; // String -> Class
static NSMutableDictionary *boxers;
static NSMutableDictionary *unboxers;
static NSNumber *kTrue;
static NSNumber *kFalse;


@interface DPJsonSerializer ()

+ (id)deserializeDictionary:(NSDictionary *)dictionary withType:(Class)type;
+ (id)deserializeArray:(NSArray *)array withType:(Class)type;
+ (NSString *)getNameForClass:(Class)type;
+ (NSString *)getNameForObjCType:(NSString *)typeName;
+ (NSArray *)getProperties:(Class)type;
+ (NSDictionary *)serializeObject:(id)object;
+ (NSDictionary *)serializeArray:(NSArray *)array;

@end

@interface NSString (PropertyManipulation)

- (NSString *)propertyCapitalize;

@end

@implementation DPJsonSerializer

+ (void)initialize {
    if (self == [DPJsonSerializer class]) {
        kTrue = [NSNumber numberWithBool:YES];
        kFalse = [NSNumber numberWithBool:NO];
        primitiveClass = [DPJsonPrimitive class];
        typeAliases = [[NSMutableDictionary alloc] init];
        revTypeAliases = [[NSMutableDictionary alloc] init];
        boxers = [[NSMutableDictionary alloc] init];
        typeSerializers = [[NSMutableDictionary alloc] init];
        typeDeserializers = [[NSMutableDictionary alloc] init];
        
        [boxers setValue:[^(char *value) {
            return [NSNumber numberWithChar:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(char)]];
        [boxers setValue:[^(int *value) {
            return [NSNumber numberWithInt:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(int)]];
        [boxers setValue:[^(short *value) {
            return [NSNumber numberWithShort:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(short)]];
        [boxers setValue:[^(long *value) {
            return [NSNumber numberWithLong:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(long)]];
        [boxers setValue:[^(long long *value) {
            return [NSNumber numberWithLongLong:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(long long)]];
        [boxers setValue:[^(unsigned char *value) {
            if (*value == 0 || *value == 1) {
                return [NSNumber numberWithBool:*value];
            }
            return [NSNumber numberWithUnsignedChar:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned char)]];
        [boxers setValue:[^(unsigned int *value) {
            return [NSNumber numberWithUnsignedInt:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned int)]];
        [boxers setValue:[^(unsigned short *value) {
            return [NSNumber numberWithUnsignedShort:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned short)]];
        [boxers setValue:[^(unsigned long *value) {
            return [NSNumber numberWithUnsignedLong:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned long)]];
        [boxers setValue:[^(unsigned long long *value) {
            return [NSNumber numberWithUnsignedLongLong:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned long long)]];
        [boxers setValue:[^(float *value) {
            return [NSNumber numberWithFloat:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(float)]];
        [boxers setValue:[^(double *value) {
            return [NSNumber numberWithDouble:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(double)]];
        [boxers setValue:[^(BOOL *value) {
            return [NSNumber numberWithBool:*value];
        } copy] forKey:[NSString stringWithUTF8String:@encode(BOOL)]];
        
        unboxers = [[NSMutableDictionary alloc] init];
        
        [unboxers setValue:[^(NSNumber *value, char *retValue) {
            *retValue = [value charValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(char)]];
        [unboxers setValue:[^(NSNumber *value, int *retValue) {
            *retValue = [value intValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(int)]];
        [unboxers setValue:[^(NSNumber *value, short *retValue) {
            *retValue = [value shortValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(short)]];
        [unboxers setValue:[^(NSNumber *value, long *retValue) {
            *retValue = [value longValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(long)]];
        [unboxers setValue:[^(NSNumber *value, long long *retValue) {
            *retValue = [value longLongValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(long long)]];
        [unboxers setValue:[^(NSNumber *value, unsigned char *retValue) {
            if (value == kTrue || value == kFalse) {
                *retValue = [value boolValue];
                return;
            }
            *retValue = [value unsignedCharValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned char)]];
        [unboxers setValue:[^(NSNumber *value, unsigned int *retValue) {
            *retValue = [value unsignedIntValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned int)]];
        [unboxers setValue:[^(NSNumber *value, unsigned short *retValue) {
            *retValue = [value unsignedShortValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned short)]];
        [unboxers setValue:[^(NSNumber *value, unsigned long *retValue) {
            *retValue = [value unsignedLongValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned long)]];
        [unboxers setValue:[^(NSNumber *value, unsigned long long *retValue) {
            *retValue = [value unsignedLongLongValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(unsigned long long)]];
        [unboxers setValue:[^(NSNumber *value, double *retValue) {
            *retValue = [value doubleValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(double)]];
        [unboxers setValue:[^(NSNumber *value, float *retValue) {
            *retValue = [value floatValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(float)]];
        [unboxers setValue:[^(NSNumber *value, BOOL *retValue) {
            *retValue = [value boolValue];
        } copy] forKey:[NSString stringWithUTF8String:@encode(BOOL)]];
    }
}

+ (id)deserializeDictionary:(NSDictionary *)dictionary {
    id result = nil;
    NSString *typeName = [dictionary objectForKey:@"*type"];
    Class type = [DPJsonSerializer getClassForName:typeName];
    NSArray *items = [dictionary objectForKey:@"*items"];
    if (items) {
        result = [DPJsonSerializer deserializeArray:items withType:type];
    } else {
        result = [DPJsonSerializer deserializeDictionary:dictionary withType:type];
        if ([result isKindOfClass:[DPJsonPrimitive class]]) {
            result = [result trueValue];
        }
    }
    return result;
}

+ (id)deserializeArray:(NSArray *)array withType:(Class)type {
    NSMutableArray *toBuild = [NSMutableArray arrayWithCapacity:[array count]];
    for (id obj in array) {
        id item = obj;
        if ([item isKindOfClass:[NSDictionary class]]) {
            item = [DPJsonSerializer deserializeDictionary:item];
        }
        [toBuild addObject:item];
    }
    id result = [[type alloc] initWithArray:toBuild];
    return result;
}

+ (id)deserializeDictionary:(NSDictionary *)dictionary withType:(Class)type {
    id result = nil;
    if ([type isSubclassOfClass:[DPEnum class]]) {
        result = [[type alloc] initWithString:[dictionary objectForKey:@"*name"]];
    } else if ([typeDeserializers objectForKey:type]) {
        id (^deserializer)(NSString *) = [typeDeserializers objectForKey:type];
        result = deserializer([dictionary objectForKey:@"*serialized"]);
    } else {
        result = [[type alloc] init];
    }
    for (NSString *key in dictionary) {
        if ([key hasPrefix:@"*"]) {
            continue;
        }
        id cur = [dictionary objectForKey:key];
        if (cur == [NSNull null]) {
            cur = nil;
        } else if ([cur isKindOfClass:[NSDictionary class]]) {
            cur = [DPJsonSerializer deserializeDictionary:cur];
        }
        SEL selector = NSSelectorFromString([NSString stringWithFormat:@"set%@:", [key propertyCapitalize]]);
        // A key the class has no property for (saved by another version, say) is
        // skipped rather than crashing the whole read.
        if (![result respondsToSelector:selector]) {
            continue;
        }
        NSMethodSignature *meth = [result methodSignatureForSelector:selector];
        NSInvocation *inv = [NSInvocation invocationWithMethodSignature:meth];
        inv.target = result;
        inv.selector = selector;
        NSString *propertyType = [NSString stringWithUTF8String:[meth getArgumentTypeAtIndex:2]];
        void (^unboxer)(id, Byte*) = [unboxers objectForKey:propertyType];
        if (unboxer) {
            Byte buffer[sizeof(long long)];
            unboxer(cur, buffer);
            [inv setArgument:buffer atIndex:2];
        } else {
            [inv setArgument:&cur atIndex:2];
        }
        [inv invoke];
    }
    return result;
}

+ (Class)getClassForName:(NSString *)name {
    Class result = [revTypeAliases objectForKey:name];
    if (result) {
        return result;
    }
    return NSClassFromString(name);
}

+ (NSString *)getNameForObjCType:(NSString *)typeName {
    NSString *result = [typeAliases objectForKey:typeName];
    if (result) {
        return result;
    }
    return typeName;
}

+ (NSString *)getNameForClass:(Class)type {
    NSString *result = [typeAliases objectForKey:type];
    if (result) {
        return result;
    }
    return NSStringFromClass(type);
}

+ (NSArray *)getProperties:(Class)type {
    unsigned int propertyCount = 0;
    objc_property_t *properties = class_copyPropertyList(type, &propertyCount);
    NSMutableArray *result = [NSMutableArray arrayWithCapacity:propertyCount];
    for (int i = 0; i < propertyCount; i++) {
        NSString *name = [NSString stringWithUTF8String:property_getName(properties[i])];
        if ([type instancesRespondToSelector:NSSelectorFromString([NSString stringWithFormat:@"set%@:", [name propertyCapitalize]])]) {
            [result addObject:name];
        }
    }
    return result;
}

+ (void)registerAlias:(NSString *)alias forClass:(Class)aliasedClass {
    id class = aliasedClass;
    [typeAliases setObject:alias forKey:class];
    [revTypeAliases setObject:aliasedClass forKey:alias];
}

+ (void)registerAlias:(NSString *)alias forObjCType:(NSString *)typeName {
    [typeAliases setObject:alias forKey:typeName];
    [revTypeAliases setObject:typeName forKey:alias];
}

+ (void)registerSerializer:(NSString *(^)(id))serializer deserializer:(id (^)(NSString *))deserializer forClass:(Class)theClass {
    id class = theClass;
    [typeSerializers setObject:[serializer copy] forKey:class];
    [typeDeserializers setObject:[deserializer copy] forKey:class];
}

+ (void)clearAliases {
    [typeAliases removeAllObjects];
    [revTypeAliases removeAllObjects];
}

+ (NSDictionary *)serialize:(id)object {
    if (object == nil || object == [NSNull null]) {
        DPJsonPrimitive *prim = [[DPJsonPrimitive alloc] init];
        prim.value = nil;
        return [DPJsonSerializer serializeObject:prim];
    }
    if ([object isKindOfClass:[NSValue class]]) {
        NSValue *value = object;
        DPJsonPrimitive *prim = [[DPJsonPrimitive alloc] init];
        prim.type = [DPJsonSerializer getNameForObjCType:[NSString stringWithUTF8String:value.objCType]];
        prim.value = value;
        return [DPJsonSerializer serializeObject:prim];
    }
    if ([object isKindOfClass:[NSArray class]]) {
        return [DPJsonSerializer serializeArray:object];
    }
    return [DPJsonSerializer serializeObject:object];
}

+ (NSDictionary *)serializeArray:(NSArray *)array {
    NSMutableDictionary *result = [NSMutableDictionary dictionary];
    [result setObject:[DPJsonSerializer getNameForClass:[array class]] forKey:@"*type"];
    NSMutableArray *data = [NSMutableArray arrayWithCapacity:[array count]];
    for (id o in array) {
        if ([o isKindOfClass:[NSString class]]) {
            [data addObject:[NSString stringWithString:o]];
        } else {
            [data addObject:[DPJsonSerializer serialize:o]];
        }
    }
    [result setObject:data forKey:@"*items"];
    return result;
}

+ (NSDictionary *)serializeObject:(id)object {
    Class type = [object class];
    NSArray *properties = [DPJsonSerializer getProperties:type];
    NSMutableDictionary *result = [NSMutableDictionary dictionaryWithCapacity:properties.count + 1];
    [result setObject:[DPJsonSerializer getNameForClass:type] forKey:@"*type"];
    if ([object isKindOfClass:[DPEnum class]]) {
        [result setObject:[object name] forKey:@"*name"];
    } else if ([typeSerializers objectForKey:type]) {
        NSString * (^serializer)(id) = [typeSerializers objectForKey:type];
        [result setObject:serializer(object) forKey:@"*serialized"];
    }
    for (NSString *property in properties) {
        NSString *propertyKey = [property propertyCapitalize];
        id propValue = nil;
        SEL propertySelector = NSSelectorFromString(property);
        NSMethodSignature *meth = [object methodSignatureForSelector:propertySelector];
        if (![[NSString stringWithUTF8String:[meth methodReturnType]] isEqualToString:@"@"]) {
            NSInvocation *inv = [NSInvocation invocationWithMethodSignature:meth];
            inv.selector = propertySelector;
            [inv invokeWithTarget:object];
            void *returnValue = malloc([meth methodReturnLength]);
            [inv getReturnValue:returnValue];
            id (^constructor)(void *value);
            constructor = [boxers objectForKey:[NSString stringWithUTF8String:[meth methodReturnType]]];
            if (constructor) {
                propValue = constructor(returnValue);
                free(returnValue);
            } else {
                propValue = [NSValue valueWithBytes:returnValue objCType:[meth methodReturnType]];
            }
        } else {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            propValue = [object performSelector:propertySelector];
#pragma clang diagnostic pop
        }
        if (!propValue) {
            // Do nothing for a null property value
        } else if ([propValue isKindOfClass:[NSValue class]] && [object isKindOfClass:[DPJsonPrimitive class]]) {
            [result setObject:propValue forKey:propertyKey];
        } else if ([propValue isKindOfClass:[NSString class]]) {
            [result setObject:[NSString stringWithString:propValue] forKey:propertyKey];
        } else {
            [result setObject:[DPJsonSerializer serialize:propValue] forKey:propertyKey];
        }
    }
    return result;
}

@end

@implementation NSString (PropertyManipulation)

- (NSString *)propertyCapitalize {
    return [self stringByReplacingCharactersInRange:NSMakeRange(0, 1) withString:[[self substringToIndex:1] uppercaseString]];
}

@end
