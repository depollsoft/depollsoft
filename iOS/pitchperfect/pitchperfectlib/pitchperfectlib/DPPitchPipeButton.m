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

@synthesize note, button;
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
    downEvent = [button addBlock:^{
        [self.note play];
    } forControlEvents:UIControlEventTouchDown];
    upEvent = [button addBlock:^{
        [self.note stop];
    } forControlEvents:UIControlEventTouchUpInside|UIControlEventTouchUpOutside];
    button.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    [self addSubview:button];
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
