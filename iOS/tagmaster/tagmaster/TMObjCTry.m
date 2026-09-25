//
//  TMObjCTry.m
//  tagmaster
//

#import "TMObjCTry.h"

BOOL TMTry(NS_NOESCAPE void (^block)(void), NSString **reason) {
    @try {
        block();
        return YES;
    } @catch (NSException *exception) {
        if (reason) *reason = exception.reason ?: exception.name;
        return NO;
    }
}
