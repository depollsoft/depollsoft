//
//  DPRemoteLocation.m
//  tagmaster
//
//  Created by David Poll on 3/16/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPRemoteLocation.h"
#import "DPFileCache.h"

const NSTimeInterval DPRemoteRequestTimeout = 15;

@implementation DPRemoteLocation

@synthesize uri;
@synthesize type;

- (NSString *)cacheKey {
    return [NSString stringWithFormat:@"%@.%@", [DPFileCache keyForURL:self.uri], self.type];
}

+ (NSURLSession *)session {
    static NSURLSession *session;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        NSURLSessionConfiguration *configuration = [NSURLSessionConfiguration defaultSessionConfiguration];
        configuration.timeoutIntervalForRequest = DPRemoteRequestTimeout;
        configuration.timeoutIntervalForResource = DPRemoteRequestTimeout * 4;
        configuration.waitsForConnectivity = NO;
        session = [NSURLSession sessionWithConfiguration:configuration];
    });
    return session;
}

+ (NSData *)dataWithContentsOfURL:(NSURL *)url error:(NSError **)error {
    if (!url) {
        if (error) *error = [NSError errorWithDomain:NSURLErrorDomain code:NSURLErrorBadURL userInfo:nil];
        return nil;
    }
    dispatch_semaphore_t done = dispatch_semaphore_create(0);
    __block NSData *result = nil;
    __block NSError *failure = nil;
    NSURLSessionDataTask *task = [[self session] dataTaskWithURL:url completionHandler:^(NSData *data, NSURLResponse *response, NSError *taskError) {
        NSInteger status = [response isKindOfClass:[NSHTTPURLResponse class]] ? ((NSHTTPURLResponse *)response).statusCode : 200;
        if (taskError) {
            failure = taskError;
        } else if (status >= 400) {
            failure = [NSError errorWithDomain:NSURLErrorDomain code:NSURLErrorBadServerResponse userInfo:@{NSURLErrorFailingURLErrorKey: url}];
        } else {
            result = data;
        }
        dispatch_semaphore_signal(done);
    }];
    [task resume];
    dispatch_semaphore_wait(done, DISPATCH_TIME_FOREVER);
    if (error) *error = failure;
    return failure ? nil : result;
}

@end
