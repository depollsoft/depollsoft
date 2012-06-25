//
//  DPPitchPipeButton.m
//  pitchperfectlib
//
//  Created by David Poll on 6/12/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPPitchPipeButton.h"
#import "DPUtils+UIControl.h"

@interface DPPitchPipeButton ()

@property (nonatomic, strong) id downEvent;
@property (nonatomic, strong) id upEvent;

@end

@implementation DPPitchPipeButton

@synthesize note, button, toggle;
@synthesize downEvent, upEvent;

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        self.button = [UIButton buttonWithType:UIButtonTypeRoundedRect];
        self.autoresizesSubviews = YES;
    }
    return self;
}

- (void)setButton:(UIButton *)newButton {
    [button removeTarget:upEvent action:@selector(invoke) forControlEvents:UIControlEventAllEvents];
    [button removeTarget:downEvent action:@selector(invoke) forControlEvents:UIControlEventAllEvents];
    [button removeFromSuperview];
    button = newButton;
    button.frame = self.frame;
    __weak DPPitchPipeButton *me = self;
    downEvent = [button addBlock:^{
        if (self.toggle) {
            if (self.note.isPlaying) {
                [self.note stop];
            } else {
                [self.note play];
            }
        } else {
            [self.note play];
        }
    } forControlEvents:UIControlEventTouchDown];
    upEvent = [button addBlock:^{
        if (!self.toggle) {
            [self.note stop];
        } else {
            [me performSelector:@selector(doHighlight) withObject:[NSNumber numberWithBool:NO] afterDelay:0];
        }
    } forControlEvents:UIControlEventTouchUpInside|UIControlEventTouchUpOutside];
    button.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [self addSubview:button];
}

- (void)doHighlight {
    button.highlighted = note.isPlaying;
}

- (void)setNote:(DPNote *)newNote {
    [self->note stop];
    self->note = newNote;
}

/*
 // Only override drawRect: if you perform custom drawing.
 // An empty implementation adversely affects performance during animation.
 - (void)drawRect:(CGRect)rect
 {
 // Drawing code
 }
 */

@end
