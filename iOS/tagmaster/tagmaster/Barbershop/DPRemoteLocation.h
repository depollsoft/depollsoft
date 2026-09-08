//
//  DPRemoteLocation.h
//  tagmaster
//
//  Created by David Poll on 3/16/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

/// Requests wait at most this long for a stalled server before failing so the
/// caller can offer Retry instead of leaving the user waiting.
extern const NSTimeInterval DPRemoteRequestTimeout;

@interface DPRemoteLocation : NSObject

@property (nonatomic, copy) NSURL *uri;
@property (nonatomic, copy) NSString *type;
@property (nonatomic, copy) NSURL *cachedUri;
@property (nonatomic, readonly) NSString *cacheKey;

/// Synchronous fetch through NSURLSession with a request timeout. Call it off
/// the main thread. Returns nil (and an error when requested) on any failure,
/// including HTTP error statuses.
+ (NSData *)dataWithContentsOfURL:(NSURL *)url error:(NSError **)error;

@end
