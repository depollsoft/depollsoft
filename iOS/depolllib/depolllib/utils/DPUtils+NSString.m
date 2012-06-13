//
//  DPUtils+NSString.m
//  depolllib
//
//  Created by David Poll on 5/4/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPUtils+NSString.h"

@implementation NSString (DPUtils)

+ (NSString *)stringWithUUID {
    CFUUIDRef uuidObj = CFUUIDCreate(nil);
    NSString *uuidString = (__bridge NSString*)CFUUIDCreateString(nil, uuidObj);
    CFRelease(uuidObj);
    return uuidString;
}

@end
