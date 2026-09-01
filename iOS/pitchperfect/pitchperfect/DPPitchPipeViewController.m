//
//  DPFirstViewController.m
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import <CoreText/CoreText.h>
#import "DPAppDelegate.h"
#import "DPAppDelegate+Ads.h"
#import "DPPitchPipeViewController.h"
#import "DPNote.h"
#import "DPAccidental.h"
#import "GoogleMobileAdsStub.h"
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
#import "pitchperfect-Swift.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"
#define LAYOUT_TAG 1337

@interface DPPitchPipeViewController () <UIPopoverControllerDelegate>

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) NSMutableArray *noteButtons;
@property (nonatomic, strong) DPPitchPipeModel *model;
@property (nonatomic, strong) UIBarButtonItem *settingsButton;
@property (nonatomic, strong) DPPitchInstrumentView *instrumentView;

- (void)stopNotes;

@end

@implementation DPPitchPipeViewController

@synthesize bannerView, model, noteButtons, settingsButton;

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UINavigationItem *navigationItem = self.topNavigationItem;
    
    //VLayoutView *topLayout = [[VLayoutView alloc] init];
    self.noteButtons = [NSMutableArray arrayWithCapacity:12];
    self.model = [[DPPitchPipeModel alloc] init];
    // Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] init];
    bannerView.adUnitID = [DPAppDelegate bannerAdUnitID];
    bannerView.adSize = GADPortraitAnchoredAdaptiveBannerAdSizeWithWidth(
        self.view.frame.size.width
    );
    [self resetBannerViewSize];
    
    bannerView.rootViewController = self;
    bannerView.delegate = (id<GADBannerViewDelegate>)UIApplication.sharedApplication.delegate;
    
    UIView *background = [[UIView alloc] init];
    background.backgroundColor = [DPTheme staffBackgroundColor];
    [self.view setBackgroundColor:DPTheme.plateGround];
    background.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:background];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(background)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(background)]];
    
    // Loaded after layout in resetBannerViewSize so the creative uses the full screen width.
    
    DPPitchInstrumentView *instrument = [[DPPitchInstrumentView alloc] initWithFrame:self.view.bounds];
    self.instrumentView = instrument;
    __weak DPPitchPipeViewController *weakSelf = self;
    instrument.onRangeChange = ^(BOOL high) {
        weakSelf.model.isFromFToF = high;
        [weakSelf refreshButtons];
    };
    
    navigationItem.title = @"Pitch Perfect";
    settingsButton = [DPCommon getSettingsButtonWithTarget:self
                                                  selector:@selector(openSettings)];
    navigationItem.rightBarButtonItem = settingsButton;
    
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimensionWithStars:1],
                                 [DPGridDimension dimension]
                                 ];
    rootLayout.columnDimensions = @[
                                    [DPGridDimension dimensionWithStars:1]
                                    ];
    
    // The instrument owns the page; the ad slot docks at the case edge below it.
    [rootLayout addSubview:instrument row:0 column:0];
    [rootLayout addSubview:bannerView row:1 column:0];
    
    [self.view addSubview:rootLayout];
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
        
    self.edgesForExtendedLayout = UIRectEdgeNone;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[rootLayout]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
    [rootLayout.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor].active = YES;
    [rootLayout.leftAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.leftAnchor].active = YES;
    [rootLayout.rightAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.rightAnchor].active = YES;

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(refreshButtons)
                                                 name:DPSettingsModel.settingsChangedNotificationName
                                               object:[DPSettingsModel sharedInstance]];
}

- (void)resetBannerViewSize {
    [DPAppDelegate resizeAndReloadBannerView:self.bannerView forViewController:self];
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
    [self.instrumentView stopAll];
}

- (void)viewDidDisappear:(BOOL)animated {
    [self stopNotes];
    [super viewDidDisappear:animated];
}

- (void)refreshButtons {
    NSMutableArray<NSNumber *> *naturals = [NSMutableArray arrayWithCapacity:model.notes.count];
    for (DPNote *note in model.notes) {
        [naturals addObject:@(note.accidental.get == Natural)];
    }
    self.instrumentView.toggleMode = [DPSettingsModel sharedInstance].toggleNotes;
    self.instrumentView.isHighRange = self.model.isFromFToF;
    [self.instrumentView setNotes:model.notes naturals:naturals];
}

- (void)openSettings {
    [DPCommon openSettings:self barButtonItem:settingsButton];
}

@end
