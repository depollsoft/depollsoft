#import <Foundation/Foundation.h>

@interface NSString (UUIDFallback)
@end

@implementation NSString (UUIDFallback)

+ (NSString *)stringWithUUID
{
    return [[NSUUID UUID] UUIDString];
}

@end
