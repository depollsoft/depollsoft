//
//  DPLabel.h
//  depolllib
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#pragma once

#import <TargetConditionals.h>

// iOS, tvOS, visionOS, and Mac Catalyst use UIKit.
#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UILabel.h>
@interface DPLabel : UILabel
@end

// macOS uses AppKit.
#elif TARGET_OS_OSX
#import <AppKit/NSTextField.h>
@interface DPLabel : NSTextField
@end

#else
#error "Unsupported platform: Neither UIKit nor AppKit is available for DPLabel."
#endif
