//
//  DPUtils+UIView.m
//  depolllib
//
//  Created by David Poll on 6/12/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPUtils+UIControl.h"
#import <objc/runtime.h>

@interface Delegator : NSObject

@property (nonatomic, copy) void(^block)();

- (void)invoke;

@end

@implementation Delegator

@synthesize block;

- (void)invoke {
    if (block) {
        block();
    }
}

@end

@implementation UIControl (DPUtils)

static char BLOCKS_LIST_KEY;

- (NSMutableArray *)blocks {
    id result = objc_getAssociatedObject(self, &BLOCKS_LIST_KEY);
    if (!result) {
        result = [NSMutableArray array];
        objc_setAssociatedObject(self, &BLOCKS_LIST_KEY, result, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    }
    return result;
}

- (id)addBlock:(void(^)())block forControlEvents:(UIControlEvents)controlEvents {
    Delegator *d = [[Delegator alloc] init];
    d.block = block;
    [self addTarget:d action:@selector(invoke) forControlEvents:controlEvents];
    [[self blocks] addObject:d];
    return d;
}

@end