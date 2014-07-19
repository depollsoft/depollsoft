//
//  DPFirstViewController.m
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPSplashScreenViewController.h"
#import "DPNote.h"
#import "DPAccidental.h"
#import "GADBannerView.h"
#import "KJGridLayoutView.h"
#import "DPUtils+UIControl.h"
#import "DPPitchPipeModel.h"
#import "DPPitchPipeButton.h"
#import "LayoutManagers.h"
#import "DPSettingsViewController.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"
#define LAYOUT_TAG 1337

@interface DPSplashScreenViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) NSMutableArray *noteButtons;
@property (nonatomic, strong) DPPitchPipeModel *model;

@end

@implementation DPSplashScreenViewController

@synthesize bannerView, model, noteButtons;

- (void)viewDidLoad
{
    [super viewDidLoad];
    VLayoutView *topLayout = [[VLayoutView alloc] init];
    self.noteButtons = [NSMutableArray arrayWithCapacity:12];
    self.model = [[DPPitchPipeModel alloc] init];
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] initWithAdSize:kGADAdSizeSmartBannerPortrait];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    
    bannerView.rootViewController = self;
    
    UIView *background = [[UIView alloc] initWithFrame:self.view.frame];
    background.backgroundColor = [UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]];
    [self.view setBackgroundColor:[UIColor blackColor]];
    [self.view addSubview:background];
    
    //[bannerView loadRequest:[GADRequest request]];
    
    UIToolbar *toolbar = [[UIToolbar alloc] init];
    toolbar.barStyle = UIBarStyleBlackTranslucent;
    
    [toolbar sizeToFit];
    [topLayout addSubview:toolbar];
    [topLayout addSubview:bannerView];
    [topLayout sizeToFit];
    [self.view addSubview:topLayout];
    
    
    CGRect gridLayoutViewBounds = CGRectInset(CGRectMake(0, topLayout.frame.size.height, self.view.frame.size.width, self.view.frame.size.height - topLayout.frame.size.height - self.tabBarController.tabBar.frame.size.height), 4, 4);
    
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
    
    UISegmentedControl *typeSwitcher = [[UISegmentedControl alloc] initWithItems:[NSArray arrayWithObjects:@"C to B", @"F to E", nil]];
    __weak UISegmentedControl *weakTypeSwitcher = typeSwitcher;
    typeSwitcher.tintColor = [UIColor darkGrayColor];
    typeSwitcher.alpha = 0.75;
    [typeSwitcher addBlock:^{
        self.model.isFromFToF = weakTypeSwitcher.selectedSegmentIndex == 1;
        [self refreshButtons];
    } forControlEvents:UIControlEventValueChanged];
    [glv addSubview:typeSwitcher row:2 rowSpan:2 column:1 columnSpan:2 options:KJGridLayoutFixedHeight];
    
    UIBarButtonItem *titleItem = [[UIBarButtonItem alloc] initWithTitle:@"Pitch Perfect" style:UIBarButtonItemStylePlain target:nil action:nil];
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    
    
    UIBarButtonItem *settingsButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemPageCurl target:self action:@selector(openSettings)];
    toolbar.items = [NSArray arrayWithObjects:flexibleSpace, titleItem, flexibleSpace, settingsButton, nil];
    
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
    settings.modalTransitionStyle = UIModalTransitionStylePartialCurl;
    [self presentViewController:settings animated:YES completion:^{
    }];
}

@end
