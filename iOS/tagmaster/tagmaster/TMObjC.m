//
//  TMObjC.m
//  tagmaster
//

#import "TMObjC.h"

@implementation TMObjC
+ (id)catching:(id (NS_NOESCAPE ^)(void))block {
    @try {
        return block();
    } @catch (NSException *exception) {
        return nil;
    }
}
@end
