//
//  DPPitchPipeButton.h
//  pitchperfectlib
//
//  Created by David Poll on 6/12/12.
//

#pragma once

#import <TargetConditionals.h>

#if TARGET_OS_IOS || TARGET_OS_TV || TARGET_OS_VISION || TARGET_OS_MACCATALYST
#import <UIKit/UIView.h>
#import <UIKit/UIButton.h>
@class DPNote;

@interface DPPitchPipeButton : UIView

@property (nonatomic, strong) DPNote *note;
@property (nonatomic, strong) UIButton *button;
@property (nonatomic) BOOL toggle;

@end

#else
// Non-UIKit platforms: intentionally left empty to avoid build errors when included by shared code.
#endif
