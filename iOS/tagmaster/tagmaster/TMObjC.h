//
//  TMObjC.h
//  tagmaster
//
//  Objective-C conveniences Swift cannot express itself.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface TMObjC : NSObject
/// Runs `block`, returning nil instead of propagating an Objective-C exception.
/// The catalog loaders raise on malformed responses; Swift callers treat that as a failed load.
+ (nullable id)catching:(id _Nullable (NS_NOESCAPE ^)(void))block;
@end

NS_ASSUME_NONNULL_END
