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
#import "GADBannerView.h"
#import "KJGridLayoutView.h"
#import "DPUtils+UIControl.h"
#import "DPPitchPipeModel.h"
#import "DPPitchPipeButton.h"
#import "LayoutManagers.h"
#import "DPSettingsViewController.h"
#import "DPSettingsModel.h"
#import "DPAppDelegate.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"
#define LAYOUT_TAG 1337

@interface DPPitchPipeViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) NSMutableArray *noteButtons;
@property (nonatomic, strong) DPPitchPipeModel *model;
@property (nonatomic, strong) UIPopoverController *popover;
@property (nonatomic, strong) UIBarButtonItem *settingsButton;

- (void)stopNotes;

@end

@implementation DPPitchPipeViewController

@synthesize bannerView, model, noteButtons, popover, settingsButton;

- (void)viewDidLoad
{
    [super viewDidLoad];
    //VLayoutView *topLayout = [[VLayoutView alloc] init];
    self.noteButtons = [NSMutableArray arrayWithCapacity:12];
    self.model = [[DPPitchPipeModel alloc] init];
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] initWithAdSize:kGADAdSizeSmartBannerPortrait];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    
    bannerView.rootViewController = self;
    
    UIView *background = [[UIView alloc] initWithFrame:self.view.frame];
    background.backgroundColor = [UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]];
    //[self.view setBackgroundColor:[UIColor blackColor]];
    [self.view addSubview:background];
    
    [bannerView loadRequest:DPAppDelegate.adRequest];
    
    UIToolbar *toolbar = [[UIToolbar alloc] init];
    toolbar.barStyle = UIBarStyleDefault;
    
    [toolbar sizeToFit];
    //[topLayout addSubview:toolbar];
    //[topLayout addSubview:bannerView];
    //[topLayout sizeToFit];
    //[self.view addSubview:topLayout];
    
    //CGRect gridLayoutViewBounds = CGRectInset(CGRectMake(0, topLayout.frame.size.height, self.view.frame.size.width, self.view.frame.size.height - topLayout.frame.size.height - self.tabBarController.tabBar.frame.size.height), 4, 4);
    
    KJGridLayoutView *glv = [[KJGridLayoutView alloc] init];
    
    glv.rowSpacing = 4;
    glv.columnSpacing = 4;
    
    int rowMap[12] = { 0, 0, 0, 0, 1, 2, 3, 3, 3, 3, 2, 1 };
    int colMap[12] = { 0, 1, 2, 3, 3, 3, 3, 2, 1, 0, 0, 0 };
    
    for (int buttonNumber = 0; buttonNumber < 12; buttonNumber++) {
        id button = [[DPPitchPipeButton alloc] initWithFrame:self.view.bounds];
        [noteButtons addObject:button];
        [glv addSubview:button row:rowMap[buttonNumber] column:colMap[buttonNumber]];
    }
    
    UISegmentedControl *typeSwitcher = [[UISegmentedControl alloc] initWithItems:[NSArray arrayWithObjects:@"C to B", @"F to E", nil]];
    typeSwitcher.segmentedControlStyle = UISegmentedControlStyleBar;
    typeSwitcher.tintColor = [UIColor darkGrayColor];
    typeSwitcher.alpha = 0.75;
    [typeSwitcher addBlock:^{
        [self stopNotes];
        self.model.isFromFToF = typeSwitcher.selectedSegmentIndex == 1;
        [self refreshButtons];
    } forControlEvents:UIControlEventValueChanged];
    typeSwitcher.selectedSegmentIndex = self.model.isFromFToF ? 1 : 0;
    [glv addSubview:typeSwitcher row:2 rowSpan:2 column:1 columnSpan:2 options:KJGridLayoutFixedHeight];
    
    UIBarButtonItem *titleItem = [[UIBarButtonItem alloc] initWithTitle:@"Pitch Perfect" style:UIBarButtonItemStylePlain target:nil action:nil];
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    
    
    settingsButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemPageCurl target:self action:@selector(openSettings)];
    toolbar.items = [NSArray arrayWithObjects:flexibleSpace, titleItem, flexibleSpace, settingsButton, nil];
    
    [self.view addSubview:glv];
    [self.view addSubview:toolbar];
    [self.view addSubview:bannerView];
    
    [glv setTranslatesAutoresizingMaskIntoConstraints:NO];
    [toolbar setTranslatesAutoresizingMaskIntoConstraints:NO];
    [bannerView setTranslatesAutoresizingMaskIntoConstraints:NO];
    UITabBar *tabBar = self.tabBarController.tabBar;
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[topLayoutGuide][toolbar][bannerView][glv][bottomLayoutGuide]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(toolbar, bannerView, glv, tabBar)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[toolbar]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(toolbar, bannerView, glv)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[bannerView]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(toolbar, bannerView, glv)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[glv]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(toolbar, bannerView, glv)]];
    
}

- (void)viewDidAppear:(BOOL)animated {
    [self refreshButtons];
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
        [button.button setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
        [button.button setTitleColor:[UIColor blackColor] forState:UIControlStateHighlighted];
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
}

- (void)openSettings {
    DPSettingsViewController *settings = [DPSettingsViewController sharedInstance];
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        if (self.popover.isPopoverVisible) {
            [popover dismissPopoverAnimated:YES];
            return;
        }
        settings.contentSizeForViewInPopover = CGSizeMake(320, 480);
        popover = [[UIPopoverController alloc] initWithContentViewController:settings];
        settings.popoverController = popover;
        [popover presentPopoverFromBarButtonItem:settingsButton permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
        
    } else {
        settings.modalTransitionStyle = UIModalTransitionStylePartialCurl;
        [self presentViewController:settings animated:YES completion:^{
        }];
    }
}

@end
