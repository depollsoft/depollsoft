//
//  DPJsonPrimitive.h
//  depolllib
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface DPJsonPrimitive : NSObject

@property (nonatomic, retain) id value;
@property (nonatomic, retain) NSString *type;

- (id)trueValue;

@end
