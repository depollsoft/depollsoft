//
//  DPUtils+UIView.h
//  depolllib
//
//  Created by David Poll on 6/12/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@interface UIControl (DPUtils)

- (id)addBlock:(void(^)())block forControlEvents:(UIControlEvents)controlEvents;

@end