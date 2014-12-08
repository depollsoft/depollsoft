//
//  DPPitchPipeButton.m
//  pitchperfectlib
//
//  Created by David Poll on 6/12/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPPitchPipeButton.h"
#import "DPUtils+UIControl.h"
#import "UIView+DPUtils.h"

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
        self.button = [UIButton buttonWithType:UIButtonTypeCustom];
        self.button.layer.masksToBounds = YES;
        [self.button setBackgroundImage:[DPPitchPipeButton imageWithColor:[UIColor colorWithWhite:0.9 alpha:1]]
                               forState:UIControlStateNormal];
        [self.button setBackgroundImage:[DPPitchPipeButton imageWithColor:[UIColor colorWithWhite:1 alpha:1]]
                               forState:UIControlStateHighlighted];
        [button.layer setBorderColor:[[UIColor colorWithWhite:0.5 alpha:1] CGColor]];
        [button.layer setBorderWidth:2];
        [button.layer setCornerRadius:4];
        self.autoresizesSubviews = YES;
    }
    return self;
}

+ (UIImage *)imageWithColor:(UIColor *)color {
    CGRect rect = CGRectMake(0.0f, 0.0f, 1.0f, 1.0f);
    UIGraphicsBeginImageContext(rect.size);
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGContextSetFillColorWithColor(context, [color CGColor]);
    CGContextFillRect(context, rect);
    
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return image;
}

- (void)setButton:(UIButton *)newButton {
    [button removeTarget:upEvent action:@selector(invoke) forControlEvents:UIControlEventAllEvents];
    [button removeTarget:downEvent action:@selector(invoke) forControlEvents:UIControlEventAllEvents];
    [button removeFromSuperview];
    button = newButton;
    button.translatesAutoresizingMaskIntoConstraints = NO;
    __weak DPPitchPipeButton *me = self;
    downEvent = [button addBlock:^{
        if (me.toggle) {
            if (me.note.isPlaying) {
                [me.note stop];
            } else {
                [me.note play];
            }
        } else {
            [me.note play];
        }
    } forControlEvents:UIControlEventTouchDown];
    upEvent = [button addBlock:^{
        if (!me.toggle) {
            [me.note stop];
        } else {
            [me performSelector:@selector(doHighlight) withObject:[NSNumber numberWithBool:NO] afterDelay:0];
        }
    } forControlEvents:UIControlEventTouchUpInside|UIControlEventTouchUpOutside];
    UIView *padded = [button pad:2];
    padded.translatesAutoresizingMaskIntoConstraints = NO;
    [self addSubview:padded];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[padded]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(padded)]];
    [self addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[padded]|"
                                                                 options:0
                                                                 metrics:nil
                                                                   views:NSDictionaryOfVariableBindings(padded)]];
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
