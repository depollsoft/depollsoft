//
//  DPFirstViewController.h
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPAudioSynthesizer.h"
#import "DPToolbarViewController.h"

@interface DPPitchPipeViewController : DPToolbarViewController

- (void)playWidgetNoteNamed:(NSString *)name
                 accidental:(NSString *)accidental
                     octave:(NSInteger)octave
                  highRange:(BOOL)highRange;

@end
