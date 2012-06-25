//
//  DPPitchPipeButton.h
//  pitchperfectlib
//
//  Created by David Poll on 6/12/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPNote.h"

@interface DPPitchPipeButton : UIView

@property (nonatomic, strong) DPNote *note;
@property (nonatomic, strong) UIButton *button;
@property (nonatomic) BOOL toggle;

@end
