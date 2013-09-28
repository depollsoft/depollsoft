//
//  DPRemoteLocation.m
//  tagmaster
//
//  Created by David Poll on 3/16/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPRemoteLocation.h"
#import "DPFileCache.h"

@implementation DPRemoteLocation

@synthesize uri;
@synthesize type;

- (NSString *)cacheKey {
    return [NSString stringWithFormat:@"%@.%@", [DPFileCache keyForURL:self.uri], self.type];
}

@end
