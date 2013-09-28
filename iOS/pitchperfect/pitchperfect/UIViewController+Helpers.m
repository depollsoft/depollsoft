//
//  UIViewController+Helpers.m
//  pitchperfect
//
//  Created by David Poll on 9/26/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "UIViewController+Helpers.h"

@implementation UIViewController (Helpers)

-(NSDictionary *)addGuides:(NSDictionary *)dict {
    NSMutableDictionary *result = [NSMutableDictionary dictionaryWithDictionary:dict];
    result[@"topLayoutGuide"] = self.topLayoutGuide;
    result[@"bottomLayoutGuide"] = self.bottomLayoutGuide;
    return result;
}

@end
