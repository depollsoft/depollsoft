//
//  DPFileCache.h
//  depolllib
//
//  Created by David Poll on 7/26/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPFileCache : NSObject

+ (void)writeData:(NSData *)data forKey:(NSString *)key;
+ (NSData *)readDataForKey:(NSString *)key;
+ (void)writeObject:(id)object forKey:(NSString *)key;
+ (id)readObjectForKey:(NSString *)key;

@end
