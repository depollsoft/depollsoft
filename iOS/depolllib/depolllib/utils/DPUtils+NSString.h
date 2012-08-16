//
//  DPUtils+NSString.h
//  depolllib
//
//  Created by David Poll on 5/4/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface NSString (DPUtils)

+ (NSString *)stringWithUUID;
- (NSString *)stringByURLEncoding;

@end
