//
//  DPSettingsViewController.m
//  pitchperfect
//
//  Created by David Poll on 6/23/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPSettingsViewController.h"
#import "GADBannerView.h"
#import "DPNote.h"
#import "DPKey.h"
#import "DPAccidental.h"
#import "LayoutManagers.h"
#import "DPUtils+UIControl.h"
#import "DPUtils+UIColor.h"
#import "DPSettingsModel.h"
#import "DPSongsModel.h"
#import "DPAppDelegate.h"
#import <Parse/Parse.h>
#import <ParseFacebookUtils/PFFacebookUtils.h>
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"

@interface DPSettingsViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) UITableView *tableView;

@end

@implementation DPSettingsViewController

@synthesize bannerView, tableView, popoverController;

+ (DPSettingsViewController *)sharedInstance {
    static DPSettingsViewController *settings;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        settings = [[DPSettingsViewController alloc] init];
    });
    return settings;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
        
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        self.view.frame = CGRectMake(0, 0, 320, 480);
    }
    
	// Do any additional setup after loading the view.
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimensionWithStars:1]
                                 ];
    
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] initWithAdSize:kGADAdSizeSmartBannerPortrait];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    
    bannerView.rootViewController = self;
    
    UIToolbar *toolbar = [[UIToolbar alloc] init];
    
    [toolbar sizeToFit];
    toolbar.frame = CGRectMake(0, 0, self.view.frame.size.width, toolbar.frame.size.height);
    
    
    UIView *background = [[UIView alloc] init];
    background.backgroundColor = [[UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]] colorWithAlphaComponent:0.5];
    //[self.view setBackgroundColor:[UIColor blackColor]];
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
    
    self.view.backgroundColor = [UIColor whiteColor];
    
    [rootLayout addSubview:toolbar row:0 column:0];

    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
        [rootLayout addSubview:bannerView row:1 column:0];
        
        [bannerView loadRequest:DPAppDelegate.adRequest];
    }
    
    tableView = [[UITableView alloc] initWithFrame:CGRectInfinite style:UITableViewStyleGrouped];
    tableView.dataSource = self;
    tableView.delegate = self;
    tableView.allowsSelection = NO;
    tableView.backgroundColor = [UIColor clearColor];
    tableView.backgroundView = nil;
    [rootLayout addSubview:tableView row:2 column:0];
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    UIBarButtonItem *doneItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone target:self action:@selector(complete)];
    
    toolbar.items = [NSArray arrayWithObjects:flexibleSpace, doneItem, nil];
    
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    [self.view addSubview:rootLayout];
    
    id topLayoutGuide = self.topLayoutGuide;
    id bottomLayoutGuide = self.bottomLayoutGuide;
    
    self.edgesForExtendedLayout = UIRectEdgeNone;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[topLayoutGuide][rootLayout][bottomLayoutGuide]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(topLayoutGuide, rootLayout, bottomLayoutGuide)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[rootLayout]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
}

- (void)resetBannerViewSize {
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        return;
    }
    switch ([UIApplication sharedApplication].statusBarOrientation) {
        case UIInterfaceOrientationLandscapeLeft:
        case UIInterfaceOrientationLandscapeRight:
            self.bannerView.adSize = kGADAdSizeSmartBannerLandscape;
            break;
        case UIInterfaceOrientationPortrait:
        case UIInterfaceOrientationPortraitUpsideDown:
            self.bannerView.adSize = kGADAdSizeSmartBannerPortrait;
            break;
        default:
            break;
    }
}

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation {
    [self resetBannerViewSize];
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
}

- (void)viewDidAppear:(BOOL)animated {
    [self resetBannerViewSize];
    [tableView reloadData];
    [super viewDidAppear:animated];
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    UITableViewCell *cell = nil;
    switch (indexPath.section) {
        case 0:
        {
            switch (indexPath.row) {
                case 0:
                {
                    cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"Cell"];
                    cell.textLabel.text = @"Toggle Notes";
                    cell.detailTextLabel.text = @"Play until pressed again";
                    UISwitch *switchView = [[UISwitch alloc] initWithFrame:CGRectZero];
                    __weak UISwitch *weakSwitchView = switchView;
                    [switchView setOn:[DPSettingsModel sharedInstance].toggleNotes];
                    cell.accessoryView = switchView;
                    [switchView addBlock:^{
                        [DPSettingsModel sharedInstance].toggleNotes = weakSwitchView.isOn;
                    } forControlEvents:UIControlEventValueChanged];
                    break;
                }
                case 1:
                {
                    cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"Cell"];
                    cell.textLabel.text = @"Wake Lock";
                    cell.detailTextLabel.text = @"Prevent device from sleeping";
                    UISwitch *switchView = [[UISwitch alloc] initWithFrame:CGRectZero];
                    __weak UISwitch *weakSwitchView = switchView;
                    [switchView setOn:[DPSettingsModel sharedInstance].wakeLock];
                    cell.accessoryView = switchView;
                    [switchView addBlock:^{
                        [DPSettingsModel sharedInstance].wakeLock = weakSwitchView.isOn;
                    } forControlEvents:UIControlEventValueChanged];
                    break;
                }
                default:
                    break;
            }
            break;
        }
        case 1:
        {
            cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"Cell"];
            if ([PFUser currentUser]) {
                cell.textLabel.text = @"Log out";
            } else {
                cell.textLabel.text = @"Log in with Facebook";
            }
            UIActivityIndicatorView *activityIndicator = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleGray];
            activityIndicator.hidesWhenStopped = YES;
            cell.accessoryView = activityIndicator;
            [cell addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(loginButtonPress:)]];
            break;
        }            
        default:
            break;
    }
    if (!cell) {
        cell = [[UITableViewCell alloc] init];
    }
    return cell;
}

- (void)loginButtonPress:(UIGestureRecognizer *)recognizer {
    if ([PFUser currentUser]) {
        [PFUser logOut];
        [tableView reloadData];
    } else {
        UITableViewCell *cell = (UITableViewCell *)recognizer.view;
        UIActivityIndicatorView *activity = (UIActivityIndicatorView*)cell.accessoryView;
        [activity startAnimating];
        [PFFacebookUtils logInWithPermissions:nil block:^(PFUser *user, NSError *error) {
            [activity stopAnimating];
            if (user) {
                if (!user.isNew) {
                    [[DPSettingsModel sharedInstance] restoreUser];
                    [[DPSongsModel sharedInstance] refreshFromParse];
                } else {
                    [[DPSettingsModel sharedInstance] refreshUser];
                    [[DPSongsModel sharedInstance] saveAllToParse:YES];
                }
            }
            [tableView reloadData];
        }];
    }
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    switch (section) {
        case 0:
            return 2;
        case 1:
            return 1;
        default:
            break;
    }
    return 0;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 2;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    switch (section) {
        case 0:
            return @"Settings";
        case 1:
            return nil;
        default:
            break;
    }
    return nil;
}

- (NSString *)tableView:(UITableView *)tableView titleForFooterInSection:(NSInteger)section {
    if (section == 1) {
        return @"Log in using Facebook to back up and synchronize your song list and settings.";
    }
    return nil;
}

- (void)complete {
    [self dismissViewControllerAnimated:YES completion:^{
        
    }];
    [popoverController dismissPopoverAnimated:YES];
}

@end