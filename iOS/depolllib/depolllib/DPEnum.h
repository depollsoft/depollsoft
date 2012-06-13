//
//  DPEnum.h
//  depolllib
//
//  Created by David Poll on 4/29/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>


#ifdef ENUM_IMPLEMENTATION
#define DEFINE_ENUM(enumName, enumValues...) \
  enum k##enumName { \
    enumValues \
  }; \
  NSString *VALUES_##enumName = @#enumValues; \
  @interface enumName : DPEnum \
  @end 
#undef ENUM_IMPLEMENTATION
#else
#define DEFINE_ENUM(enumName, enumValues...) \
enum k##enumName { \
enumValues \
}; \
@interface enumName : DPEnum \
@end 
#endif

#define IMPLEMENT_ENUM(enumName) \
  static NSDictionary *enumMappings_##enumName = nil; \
  @implementation enumName \
  + (NSDictionary *)getEnumValues { \
    if (!enumMappings_##enumName) { \
      NSMutableDictionary *temp = [NSMutableDictionary dictionary]; \
      NSArray *parts = [VALUES_##enumName componentsSeparatedByString:@","]; \
      int index = 0; \
      for (NSString *part in parts) { \
        NSArray *splitOnEquals = [part componentsSeparatedByString:@"="]; \
        NSString *realPart = [splitOnEquals objectAtIndex:0]; \
        if ([splitOnEquals count] > 1) { \
          index = [[splitOnEquals objectAtIndex:1] intValue]; \
        } \
        realPart = [realPart stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]]; \
        [temp setObject:[NSNumber numberWithInt:index] forKey:realPart]; \
        index++; \
      } \
      enumMappings_##enumName = [NSDictionary dictionaryWithDictionary:temp]; \
    } \
    return enumMappings_##enumName; \
  } \
  @end

@interface DPEnum : NSObject

- (id)initWithString:(NSString *)string;
- (id)initWithValue:(NSNumber *)value;
- (id)initWithInt:(int)value;
- (NSString *)name;
- (int)get;
+ (NSDictionary *)getEnumValues;
+ (id)enumWithInt:(int)value;
+ (id)enumWithString:(NSString *)string;
+ (id)enumWithValue:(NSNumber *)value;

@property (readonly) NSNumber *value;

@end
