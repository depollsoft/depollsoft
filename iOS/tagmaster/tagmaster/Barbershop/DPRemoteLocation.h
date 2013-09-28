//
//  DPRemoteLocation.h
//  tagmaster
//
//  Created by David Poll on 3/16/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPRemoteLocation : NSObject

@property (nonatomic, copy) NSURL *uri;
@property (nonatomic, copy) NSString *type;
- (NSString *)cacheKey;

@end
