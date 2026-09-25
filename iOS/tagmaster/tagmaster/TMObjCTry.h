//
//  TMObjCTry.h
//  tagmaster
//
//  The catalog layer (DPTag loading, rating, downloads) reports failure by
//  raising Objective-C exceptions, which Swift cannot catch. Swift callers run
//  that work through this shim instead.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// Runs `block`, returning NO (and the exception's reason) if it raised.
FOUNDATION_EXPORT BOOL TMTry(NS_NOESCAPE void (^block)(void), NSString * _Nullable * _Nullable reason);

NS_ASSUME_NONNULL_END
