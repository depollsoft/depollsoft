//
//  DPToolbarViewController.h
//  depolllib
//
//  Created by David Poll on 12/17/17.
//  Copyright © 2017 DepollSoft. All rights reserved.
//

#pragma once

#import <TargetConditionals.h>

#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UIViewController.h>
#import <UIKit/UIToolbar.h>

@interface DPToolbarViewController : UIViewController
@property (nonatomic, readonly) UIToolbar *toolbar;
@end

#elif TARGET_OS_OSX
#import <AppKit/NSViewController.h>
#import <AppKit/NSToolbar.h>

@interface DPToolbarViewController : NSViewController
@property (nonatomic, readonly) NSToolbar *toolbar;
@end

#else
// Unsupported platform: intentionally left undefined to avoid build errors.
#endif
