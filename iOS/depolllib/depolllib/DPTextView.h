//
//  DPTextView.h
//  depolllib
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#pragma once

#import <TargetConditionals.h>

// iOS, tvOS, visionOS, and Mac Catalyst use UIKit.
#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UITextView.h>
@interface DPTextView : UITextView
@end

// macOS uses AppKit.
#elif TARGET_OS_OSX
#import <AppKit/NSTextView.h>
@interface DPTextView : NSTextView
@end

#else
#error "Unsupported platform: Neither UIKit nor AppKit is available for DPTextView."
#endif
