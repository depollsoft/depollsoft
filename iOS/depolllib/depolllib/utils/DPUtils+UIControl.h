//
//  DPUtils+UIView.h
//  depolllib
//
//  Created by David Poll on 6/12/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#pragma once

#import <TargetConditionals.h>
#import <Foundation/Foundation.h>

#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UIControl.h>

@interface UIControl (DPUtils)

- (id)addBlock:(void(^)(void))block forControlEvents:(UIControlEvents)controlEvents;

@end

#else
// Non-UIKit platforms: leave empty to avoid build errors when shared sources include this header.
#endif
