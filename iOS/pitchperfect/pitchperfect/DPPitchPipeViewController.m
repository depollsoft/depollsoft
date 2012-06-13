//
//  DPFirstViewController.m
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <CoreText/CoreText.h>
#import "DPPitchPipeViewController.h"
#import "DPNote.h"
#import "DPAccidental.h"
#import "GADBannerView.h"
#import "KJGridLayoutView.h"
#import "DPUtils+UIControl.h"
#import "DPPitchPipeModel.h"
#import "DPPitchPipeButton.h"
#import "HLayoutView.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"
#define LAYOUT_TAG 1337

@interface DPPitchPipeViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) NSMutableArray *toRetain;
@property (nonatomic, strong) NSMutableArray *noteButtons;
@property (nonatomic, strong) DPPitchPipeModel *model;

@end

@implementation DPPitchPipeViewController

@synthesize bannerView, toRetain, model, noteButtons;

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.toRetain = [NSMutableArray array];
    self.noteButtons = [NSMutableArray arrayWithCapacity:12];
    self.model = [[DPPitchPipeModel alloc] init];
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] initWithAdSize:kGADAdSizeSmartBannerPortrait];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    
    bannerView.rootViewController = self;
    [self.view addSubview:bannerView];
    [self.view setBackgroundColor:[UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]]];
    
    [bannerView loadRequest:[GADRequest request]];
    
    CGRect gridLayoutViewBounds = CGRectInset(self.view.bounds, 4, bannerView.frame.size.height + bannerView.frame.origin.y + 4);
    
    KJGridLayoutView *glv = [[KJGridLayoutView alloc] initWithFrame:gridLayoutViewBounds];
    
    glv.rowSpacing = 4;
    glv.columnSpacing = 4;
    
    int rowMap[12] = { 0, 0, 0, 0, 1, 2, 3, 3, 3, 3, 2, 1 };
    int colMap[12] = { 0, 1, 2, 3, 3, 3, 3, 2, 1, 0, 0, 0 };
    
    for (int buttonNumber = 0; buttonNumber < 12; buttonNumber++) {
        id button = [[DPPitchPipeButton alloc] initWithFrame:self.view.bounds];
        [noteButtons addObject:button];
        [glv addSubview:button row:rowMap[buttonNumber] column:colMap[buttonNumber]];
    }
    
    UISegmentedControl *typeSwitcher = [[UISegmentedControl alloc] initWithItems:[NSArray arrayWithObjects:@"C to C", @"F to F", nil]];
    typeSwitcher.segmentedControlStyle = UISegmentedControlStyleBordered;
    typeSwitcher.alpha = 0.75;
    [toRetain addObject:[typeSwitcher addBlock:^{
        self.model.isFromFToF = typeSwitcher.selectedSegmentIndex == 1;
        [self refreshButtons];
    } forControlEvents:UIControlEventValueChanged]];
    typeSwitcher.selectedSegmentIndex = self.model.isFromFToF ? 1 : 0;
    [glv addSubview:typeSwitcher row:2 rowSpan:2 column:1 columnSpan:2 options:KJGridLayoutFixedHeight];
    [self.view addSubview:glv];
    dispatch_async(dispatch_get_main_queue(), ^{
        [self refreshButtons];
    });
}

- (void)refreshButtons {
    for (int x = 0; x < noteButtons.count; x++) {
        DPPitchPipeButton *button = [noteButtons objectAtIndex:x];
        [button.button setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
        [button.button setTitleColor:[UIColor blackColor] forState:UIControlStateHighlighted];
        button.alpha = 0.75;
        button.note = [model.notes objectAtIndex:x];
        for (UIView *subview in button.button.subviews) {
            if ([subview isKindOfClass:[HLayoutView class]]) {
                [subview removeFromSuperview];
            }
        }
        
        switch(button.note.accidental.get == Natural) {
            case Natural:
                [button.button setTitle:button.note.friendlyName forState:UIControlStateNormal];
                break;
            default:
            {
                [button.button setTitle:@"" forState:UIControlStateNormal];
                UILabel *sharpLabel = [[UILabel alloc] initWithFrame:CGRectInset(button.button.frame, 4, 4)];
                sharpLabel.text = SHARP_STRING;
                sharpLabel.font = [UIFont fontWithName:@"NoteHedz" size:40];
                sharpLabel.backgroundColor = [UIColor clearColor];
                sharpLabel.userInteractionEnabled = NO;
                [sharpLabel sizeToFit];
                sharpLabel.textAlignment = UITextAlignmentCenter;
                UILabel *slashLabel = [[UILabel alloc] initWithFrame:button.frame];
                slashLabel.text = @"/";
                slashLabel.textColor = [UIColor blackColor];
                slashLabel.backgroundColor = [UIColor clearColor];
                slashLabel.userInteractionEnabled = NO;
                [slashLabel sizeToFit];
                UILabel *flatLabel = [[UILabel alloc] init];
                flatLabel.text = FLAT_STRING;
                flatLabel.font = [UIFont fontWithName:@"NoteHedz" size:40];
                flatLabel.backgroundColor = [UIColor clearColor];
                flatLabel.userInteractionEnabled = NO;
                [flatLabel sizeToFit];
                HLayoutView *layout = [[HLayoutView alloc] initWithFrame:button.frame spacing:4];
                layout.userInteractionEnabled = NO;
                [layout addSubview:sharpLabel];
                [layout addSubview:slashLabel];
                [layout addSubview:flatLabel];
                [layout sizeToFit];
                layout.center = CGPointMake(button.button.frame.size.width / 2, button.button.frame.size.height / 2);
                
                [button.button addSubview:layout];
                [button.button bringSubviewToFront:layout];
                break;
            }
        }
    }
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
    bannerView = nil;
    toRetain = nil;
}

@end
