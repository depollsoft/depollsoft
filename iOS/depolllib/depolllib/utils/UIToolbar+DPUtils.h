//
//  UIToolbar+DPUtils.h
//  depolllib
//
//  Created by David Poll on 3/23/15.
//

#pragma once

#import <TargetConditionals.h>

#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UIToolbar.h>
@class UILabel;

@interface UIToolbar (DPUtils)
- (UILabel *)addTitle:(NSString *)title;
@end

#else
// Non-UIKit platforms: no UIToolbar.
#endif
