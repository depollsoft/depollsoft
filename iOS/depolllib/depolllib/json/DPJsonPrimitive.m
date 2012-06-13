//
//  DPJsonPrimitive.m
//  depolllib
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPJsonPrimitive.h"
#import "DPJsonSerializer.h"

@implementation DPJsonPrimitive

@synthesize type, value;

- (id)trueValue {
    return value;
}

@end
