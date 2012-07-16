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
    VLayoutView *topLayout = [[VLayoutView alloc] initWithFrame:self.view.bounds spacing:4];
    topLayout.vAlignment = UIControlContentVerticalAlignmentTop;
    
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] initWithAdSize:kGADAdSizeSmartBannerPortrait];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    
    bannerView.rootViewController = self;
    
    UIToolbar *toolbar = [[UIToolbar alloc] init];
    toolbar.barStyle = UIBarStyleBlackTranslucent;
    
    [toolbar sizeToFit];
    toolbar.frame = CGRectMake(0, 0, self.view.frame.size.width, toolbar.frame.size.height);
    
    UIView *background = [[UIView alloc] initWithFrame:self.view.frame];
    background.backgroundColor = [UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]];
    self.view.backgroundColor = [UIColor colorWithRed:200.0/255 green:200.0/255 blue:200.0/255 alpha:1];
    [self.view addSubview:background];
    
    [topLayout addSubview:toolbar];

    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
        [topLayout addSubview:bannerView];
        
        GADRequest *request = [GADRequest request];
        request.testing = [DPAppDelegate testAds];
        [bannerView loadRequest:request];
    }
    
    [topLayout sizeToFit];
    
    [self.view addSubview:topLayout];
    
    tableView = [[UITableView alloc] initWithFrame:CGRectInfinite style:UITableViewStyleGrouped];
    tableView.dataSource = self;
    tableView.delegate = self;
    tableView.allowsSelection = NO;
    tableView.frame = CGRectMake(0, topLayout.frame.size.height, self.view.frame.size.width, self.view.frame.size.height - topLayout.frame.size.height - self.tabBarController.tabBar.frame.size.height);
    tableView.backgroundColor = [UIColor clearColor];
    tableView.backgroundView = nil;
    [self.view addSubview:tableView];
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    UIBarButtonItem *doneItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone target:self action:@selector(complete)];
    
    toolbar.items = [NSArray arrayWithObjects:flexibleSpace, doneItem, nil];
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
}

- (void)viewDidAppear:(BOOL)animated {
    [tableView reloadData];
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
                    [switchView setOn:[DPSettingsModel sharedInstance].toggleNotes];
                    cell.accessoryView = switchView;
                    [switchView addBlock:^{
                        [DPSettingsModel sharedInstance].toggleNotes = switchView.isOn;
                    } forControlEvents:UIControlEventValueChanged];
                    break;
                }
                case 1:
                {
                    cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"Cell"];
                    cell.textLabel.text = @"Wake Lock";
                    cell.detailTextLabel.text = @"Prevent device from sleeping";
                    UISwitch *switchView = [[UISwitch alloc] initWithFrame:CGRectZero];
                    [switchView setOn:[DPSettingsModel sharedInstance].wakeLock];
                    cell.accessoryView = switchView;
                    [switchView addBlock:^{
                        [DPSettingsModel sharedInstance].wakeLock = switchView.isOn;
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
    [self dismissModalViewControllerAnimated:YES];
    [popoverController dismissPopoverAnimated:YES];
}

@end