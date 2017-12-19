//
//  DPSongEditorViewController.h
//  pitchperfect
//
//  Created by David Poll on 6/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "DPPitchedSong.h"
#import "DPToolbarViewController.h"

@interface DPSongEditorViewController : DPToolbarViewController<UITextFieldDelegate, UIPickerViewDelegate, UIPickerViewDataSource>

@property (nonatomic, strong) DPPitchedSong *song;
@property (nonatomic, copy) void(^completionCallback)(BOOL cancelled);

@end
