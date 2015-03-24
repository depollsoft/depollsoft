//
//  HFAccessToken+Internal.h
//  Hoomi
//
//  Created by David Poll on 12/4/14.
//  Copyright (c) 2014 Hoomi. All rights reserved.
//

#import "HFAccessToken.h"

@interface HFAccessToken (Internal)

+ (instancetype)tokenWithJSON:(NSDictionary *)json;
- (NSDictionary *)JSON;

@end
