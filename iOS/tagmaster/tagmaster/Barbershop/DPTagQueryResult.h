//
//  DPTagQueryResult.h
//  tagmaster
//
//  Created by David Poll on 3/17/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPTagQueryResult : NSObject

@property (nonatomic, strong) NSArray *tags;
@property (nonatomic) int start;
@property (nonatomic) int count;
@property (nonatomic) int available;

@end
