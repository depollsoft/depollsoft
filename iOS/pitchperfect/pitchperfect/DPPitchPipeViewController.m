//
//  DPFirstViewController.m
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <CoreText/CoreText.h>
#import "DPAppDelegate.h"
#import "DPPitchPipeViewController.h"
#import "DPNote.h"
#import "DPAccidental.h"
#import <GoogleMobileAds/GoogleMobileAds.h>
#import "DPUtils+UIControl.h"
#import "DPPitchPipeModel.h"
#import "DPPitchPipeButton.h"
#import "LayoutManagers.h"
#import "DPSettingsViewController.h"
#import "DPSettingsModel.h"
#import "DPAppDelegate.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "UIToolbar+DPUtils.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"
#define LAYOUT_TAG 1337

@interface DPPitchPipeViewController () <UIPopoverControllerDelegate>

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) NSMutableArray *noteButtons;
@property (nonatomic, strong) DPPitchPipeModel *model;
@property (nonatomic, strong) UIBarButtonItem *settingsButton;

- (void)stopNotes;

@end

@implementation DPPitchPipeViewController

@synthesize bannerView, model, noteButtons, settingsButton;

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UIToolbar *toolbar = self.toolbar;
    
    //VLayoutView *topLayout = [[VLayoutView alloc] init];
    self.noteButtons = [NSMutableArray arrayWithCapacity:12];
    self.model = [[DPPitchPipeModel alloc] init];
    // Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] init];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    [self resetBannerViewSize];
    
    bannerView.rootViewController = self;
    
    UIView *background = [[UIView alloc] init];
    background.backgroundColor = [[UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]] colorWithAlphaComponent:0.5];
    [self.view setBackgroundColor:[UIColor systemBackgroundColor]];
    background.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:background];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(background)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[toolbar][background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(toolbar, background)]];
    
    [bannerView loadRequest:DPAppDelegate.adRequest];
    
    toolbar.barStyle = UIBarStyleDefault;
    
    [toolbar sizeToFit];
    
    DPGridLayout *buttonLayout = [[DPGridLayout alloc] init];
    buttonLayout.rowDimensions = @[
                                   [DPGridDimension dimensionWithStars:1],
                                   [DPGridDimension dimensionWithStars:1],
                                   [DPGridDimension dimensionWithStars:1],
                                   [DPGridDimension dimensionWithStars:1]
                                   ];
    buttonLayout.columnDimensions = @[
                                      [DPGridDimension dimensionWithStars:1],
                                      [DPGridDimension dimensionWithStars:1],
                                      [DPGridDimension dimensionWithStars:1],
                                      [DPGridDimension dimensionWithStars:1]
                                      ];
    
    int rowMap[12] = { 0, 0, 0, 0, 1, 2, 3, 3, 3, 3, 2, 1 };
    int colMap[12] = { 0, 1, 2, 3, 3, 3, 3, 2, 1, 0, 0, 0 };
    
    for (int buttonNumber = 0; buttonNumber < 12; buttonNumber++) {
        id button = [[DPPitchPipeButton alloc] initWithFrame:self.view.bounds];
        [noteButtons addObject:button];
        [buttonLayout addSubview:button row:rowMap[buttonNumber] column:colMap[buttonNumber]];
    }
    
    UISegmentedControl *typeSwitcher = [[UISegmentedControl alloc] initWithItems:[NSArray arrayWithObjects:@"C to B", @"F to E", nil]];
    typeSwitcher.tintColor = [UIColor systemGrayColor];
    typeSwitcher.alpha = 0.75;
    __weak UISegmentedControl *weakTypeSwitcher = typeSwitcher;
    [typeSwitcher addBlock:^{
        [self stopNotes];
        self.model.isFromFToF = weakTypeSwitcher.selectedSegmentIndex == 1;
        [self refreshButtons];
    } forControlEvents:UIControlEventValueChanged];
    typeSwitcher.selectedSegmentIndex = self.model.isFromFToF ? 1 : 0;
    [buttonLayout addSubview:[typeSwitcher centered] row:1 column:1 rowSpan:2 colSpan:2];
    
    [toolbar addTitle:@"Pitch Perfect"];
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    settingsButton = [DPCommon getSettingsButtonWithTarget:self selector:@selector(openSettings)];
    toolbar.items = [NSArray arrayWithObjects:flexibleSpace, settingsButton, nil];
    
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimensionWithStars:1]
                                 ];
    rootLayout.columnDimensions = @[
                                    [DPGridDimension dimensionWithStars:1]
                                    ];
    
    [rootLayout addSubview:bannerView  row:1 column:0];
    [rootLayout addSubview:buttonLayout row:2 column:0];
    
    [self.view addSubview:rootLayout];
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    id bottomLayoutGuide = self.bottomLayoutGuide;
    
    self.edgesForExtendedLayout = UIRectEdgeNone;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[toolbar][rootLayout][bottomLayoutGuide]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(toolbar, rootLayout, bottomLayoutGuide)]];
    
    id leftLayoutGuide = self.view.leftSafeAreaLayoutGuide;
    id rightLayoutGuide = self.view.rightSafeAreaLayoutGuide;
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:[leftLayoutGuide][rootLayout][rightLayoutGuide]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(leftLayoutGuide, rightLayoutGuide, rootLayout)]];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(refreshButtons)
                                                 name:DPSettingsModel.settingsChangedNotificationName
                                               object:[DPSettingsModel sharedInstance]];
}

- (void)resetBannerViewSize {
    switch ([UIApplication sharedApplication].windows.firstObject.windowScene.interfaceOrientation) {
        case UIInterfaceOrientationLandscapeLeft:
        case UIInterfaceOrientationLandscapeRight:
            self.bannerView.adSize = GADLandscapeAnchoredAdaptiveBannerAdSizeWithWidth(self.view.frame.size.width);
            break;
        case UIInterfaceOrientationPortrait:
        case UIInterfaceOrientationPortraitUpsideDown:
            self.bannerView.adSize = GADPortraitAnchoredAdaptiveBannerAdSizeWithWidth(self.view.frame.size.width);
            break;
        default:
            break;
    }
}

- (void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator {
    [coordinator notifyWhenInteractionChangesUsingBlock:^(id<UIViewControllerTransitionCoordinatorContext>  _Nonnull context) {
        [self resetBannerViewSize];
    }];
    [super viewWillTransitionToSize:size withTransitionCoordinator:coordinator];
}

- (void)viewDidAppear:(BOOL)animated {
    [self refreshButtons];
    [self resetBannerViewSize];
    [super viewDidAppear:animated];
}

- (void)stopNotes {
    for (int x = 0; x < noteButtons.count; x++) {
        DPPitchPipeButton *button = [noteButtons objectAtIndex:x];
        [button.note stop];
        button.button.highlighted = NO;
    }
}

- (void)viewDidDisappear:(BOOL)animated {
    [self stopNotes];
    [super viewDidDisappear:animated];
}

- (void)refreshButtons {
    for (int x = 0; x < noteButtons.count; x++) {
        DPPitchPipeButton *button = [noteButtons objectAtIndex:x];
        button.alpha = 0.75;
        button.toggle = [DPSettingsModel sharedInstance].toggleNotes;
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
                sharpLabel.textColor = button.button.currentTitleColor;
                [sharpLabel sizeToFit];
                UILabel *slashLabel = [[UILabel alloc] initWithFrame:button.frame];
                slashLabel.text = @"/";
                slashLabel.backgroundColor = [UIColor clearColor];
                slashLabel.userInteractionEnabled = NO;
                slashLabel.textColor = button.button.currentTitleColor;
                [slashLabel sizeToFit];
                UILabel *flatLabel = [[UILabel alloc] init];
                flatLabel.text = FLAT_STRING;
                flatLabel.font = [UIFont fontWithName:@"NoteHedz" size:40];
                flatLabel.backgroundColor = [UIColor clearColor];
                flatLabel.userInteractionEnabled = NO;
                flatLabel.textColor = button.button.currentTitleColor;
                [flatLabel sizeToFit];
                HLayoutView *layout = [[HLayoutView alloc] initWithFrame:button.frame spacing:4];
                layout.userInteractionEnabled = NO;
                [layout addSubview:sharpLabel];
                [layout addSubview:slashLabel];
                [layout addSubview:flatLabel];
                [layout sizeToFit];
                layout.translatesAutoresizingMaskIntoConstraints = NO;
                
                [button.button addSubview:layout];
                [button.button bringSubviewToFront:layout];
                
                [button addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[layout]|"
                                                                               options:0
                                                                               metrics:nil
                                                                                 views:NSDictionaryOfVariableBindings(layout)]];
                [button addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[layout]|"
                                                                               options:0
                                                                               metrics:nil
                                                                                 views:NSDictionaryOfVariableBindings(layout)]];
                break;
            }
        }
    }
}

- (void)openSettings {
    [DPCommon openSettings:self barButtonItem:settingsButton];
}

@end
