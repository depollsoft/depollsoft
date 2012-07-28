//
//  DPSerializer.h
//  depolllib
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPJsonSerializer : NSObject

+ (Class)getClassForName:(NSString *)name;
+ (id)deserializeDictionary:(NSDictionary *)dictionary;
+ (void)registerAlias:(NSString *)alias forClass:(Class)aliasedClass;
+ (void)registerAlias:(NSString *)alias forObjCType:(NSString *)typeName;
+ (void)registerSerializer:(NSString * (^)(id object))serializer deserializer:(id (^)(NSString *))deserializer forClass:(Class)theClass;
+ (void)clearAliases;
+ (NSDictionary *)serialize:(id)object;

@end
