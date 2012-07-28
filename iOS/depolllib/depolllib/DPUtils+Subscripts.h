//
//  NSObject+DPUtils_Subscripts.h
//  depolllib
//
//  Created by David Poll on 7/26/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

#if __IPHONE_OS_VERSION_MAX_ALLOWED < 60000

@interface NSDictionary (DPUtilsSubscripts)

- (id)objectForKeyedSubscript:(id)key;

@end

@interface NSMutableDictionary (DPUtilsSubscripts)

- (void)setObject:(id)obj forKeyedSubscript:(id<NSCopying>)key;

@end

@interface NSArray (DPUtilsSubscripts)

- (id)objectAtIndexedSubscript:(NSUInteger)idx;

@end

@interface NSMutableArray (DPUtilsSubscripts)

- (void)setObject:(id)obj atIndexedSubscript:(NSUInteger)idx;

@end

#endif