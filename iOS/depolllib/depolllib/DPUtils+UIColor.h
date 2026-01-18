//
//  DPUtils+UIColor.h
//  depolllib
//
//  Created by David Poll on 6/21/12.
//

#pragma once

#import <TargetConditionals.h>
#import <Foundation/Foundation.h>

#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UIColor.h>

@interface UIColor (DPUtils)
- (UIColor *)invert;
- (UIColor *)withAlpha:(CGFloat)alpha;
@end

#else
// Non-UIKit platforms: no UIColor; leave empty.
#endif
