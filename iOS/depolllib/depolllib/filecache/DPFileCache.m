//
//  DPFileCache.m
//  depolllib
//
//  Created by David Poll on 7/26/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPFileCache.h"
#import "DPJsonSerializer.h"
#import "DPUtils+Subscripts.h"

#define CACHE_DIR @"depolllib.cache"

@implementation DPFileCache

static NSString *basePath = nil;

+ (void)initialize {
    if (basePath) {
        return;
    }
    NSArray *cachePaths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES);
    basePath = cachePaths[0];
    basePath = [basePath stringByAppendingPathComponent:CACHE_DIR];
    [[NSFileManager defaultManager] createDirectoryAtPath:basePath withIntermediateDirectories:YES attributes:nil error:nil];
}

+ (void)writeData:(NSData *)data forKey:(NSString *)key {
    [[NSFileManager defaultManager] createFileAtPath:[basePath stringByAppendingPathComponent:key] contents:data attributes:nil];
}

+ (void)writeObject:(id)object forKey:(NSString *)key {
    NSDictionary *dataDict = [DPJsonSerializer serialize:object];
    NSData *dataPlist = [NSPropertyListSerialization dataWithPropertyList:dataDict format:NSPropertyListBinaryFormat_v1_0 options:0 error:nil];
    [self writeData:dataPlist forKey:key];
}

+ (NSData *)readDataForKey:(NSString *)key {
    return [[NSFileManager defaultManager] contentsAtPath:[basePath stringByAppendingPathComponent:key]];
}

+ (id)readObjectForKey:(NSString *)key {
    NSData *data = [self readDataForKey:key];
    if (!data) {
        return nil;
    }
    NSDictionary *dataDict = [NSPropertyListSerialization propertyListWithData:data options:NSPropertyListImmutable format:NULL error:nil];
    return [DPJsonSerializer deserializeDictionary:dataDict];
}

+ (NSString *)pathForKey:(NSString *)key {
    return [basePath stringByAppendingPathComponent:key];
}

+ (NSString *)keyForURL:(NSURL *)url {
    NSString *str = [NSString stringWithFormat:@"%@", url];
    str = [str stringByReplacingOccurrencesOfString:@"/" withString:@"_SLASH_"];
    str = [str stringByReplacingOccurrencesOfString:@":" withString:@"_COLON_"];
    return str;
}

@end
