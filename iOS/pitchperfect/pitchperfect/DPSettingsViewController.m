//
//  DPSettingsViewController.m
//  pitchperfect
//
//  Created by David Poll on 6/23/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#if __has_include(<UIKit/UIKit.h>)
#import "DPSettingsViewController.h"
#import "pitchperfect-Swift.h"
#import "GoogleMobileAdsStub.h"
#import "DPNote.h"
#import "DPKey.h"
#import "DPAccidental.h"
#import "LayoutManagers.h"
#import "DPUtils+UIControl.h"
#import "DPUtils+UIColor.h"
#import "DPSettingsModel.h"
#import "DPSongsModel.h"
#import "DPAppDelegate.h"
#import "DPAppDelegate+Ads.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "DPLoginViewController.h"
#import "UIToolbar+DPUtils.h"
@import FirebaseAuth;
@import FirebaseFunctions;
@import UIKit;

@interface DPSettingsViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) UITableView *tableView;

@end

@implementation DPSettingsViewController

@synthesize bannerView, tableView;

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
    
    UINavigationItem *navigationItem = self.topNavigationItem;
    
    if ([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        self.view.frame = CGRectMake(0, 0, 320, 480);
    }
    
    // Do any additional setup after loading the view.
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimensionWithStars:1],
                                 [DPGridDimension dimension]
                                 ];
    
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
    [self.view setBackgroundColor:[UIColor systemBackgroundColor]];
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
    
    self.view.backgroundColor = [UIColor systemBackgroundColor];
    
    if ([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPhone) {
        [rootLayout addSubview:bannerView row:1 column:0];
        
        // Loaded after layout in resetBannerViewSize so the creative uses the full screen width.
    }
    
    tableView = [[UITableView alloc] initWithFrame:CGRectInfinite style:UITableViewStyleGrouped];
    tableView.dataSource = self;
    tableView.delegate = self;
    tableView.allowsSelection = NO;
    tableView.backgroundColor = [UIColor clearColor];
    tableView.backgroundView = nil;
    [rootLayout addSubview:tableView row:0 column:0];
    
    navigationItem.title = @"Settings";
    navigationItem.rightBarButtonItem =
        [DPCommon barButtonWithSystemName:@"checkmark"
                                   target:self
                                 selector:@selector(complete)];
    
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    [self.view addSubview:rootLayout];
        
    self.edgesForExtendedLayout = UIRectEdgeNone;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[rootLayout]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
    [rootLayout.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor].active = YES;
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[rootLayout]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleSettingsUpdate)
                                                 name:DPSettingsModel.settingsChangedNotificationName
                                               object:DPSettingsModel.sharedInstance];
}

- (void)handleSettingsUpdate {
    [tableView reloadData];
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
    [self resetBannerViewSize];
    [tableView reloadData];
    [super viewDidAppear:animated];
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    return UIInterfaceOrientationMaskPortrait;
}

- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation {
    return UIInterfaceOrientationPortrait;
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
                    cell.detailTextLabel.text = @"Notes play until pressed again";
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
                case 2:
                {
                    cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"ThemeCell"];
                    cell.textLabel.text = @"Theme";
                    UISegmentedControl *themeControl = [[UISegmentedControl alloc] initWithItems:@[@"Default", @"Light", @"Dark"]];
                    themeControl.selectedSegmentIndex = DPTheme.storedTheme;
                    __weak UISegmentedControl *weakThemeControl = themeControl;
                    [themeControl addBlock:^{
                        DPTheme.storedTheme = weakThemeControl.selectedSegmentIndex;
                    } forControlEvents:UIControlEventValueChanged];
                    cell.accessoryView = themeControl;
                    break;
                }
                default:
                    break;
            }
            break;
        }
        case 1:
        {
            switch(indexPath.row) {
                case 0:
                {
                    cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"Cell"];
                    if ([FIRAuth auth].currentUser) {
                        cell.textLabel.text = @"Log out";
                    } else {
                        cell.textLabel.text = @"Log in";
                    }
                    UIActivityIndicatorView *activityIndicator = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleMedium];
                    activityIndicator.hidesWhenStopped = YES;
                    if (@available(iOS 10.0, *)) {
                        cell.accessoryView = activityIndicator;
                    }
                    [cell addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(loginButtonPress:)]];
                    break;
                }
                case 1:
                {
                    cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"Cell"];
                    cell.textLabel.text = @"Delete Account";
                    UIActivityIndicatorView *activityIndicator = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleMedium];
                    activityIndicator.hidesWhenStopped = YES;
                    if (@available(iOS 10.0, *)) {
                        cell.accessoryView = activityIndicator;
                    }
                    [cell addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(deleteAccountButtonPress:)]];
                    break;
                }
                default:
                    break;
            }
            break;
        }
        case 2:
        {
            if (indexPath.row == 0) {
                cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:@"BuildCell"];
                cell.textLabel.text = @"Private Build";
                cell.detailTextLabel.text = [NSString stringWithFormat:@"Build %@ · PR #%@", [self privateBuildNumber], [self privatePRNumber]];
            } else {
                cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:@"CopyLogsCell"];
                cell.textLabel.text = @"Copy Logs";
                [cell addGestureRecognizer:[[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(copyLogs)]];
            }
            break;
        }
        default:
            break;
    }
    if (!cell) {
        cell = [[UITableViewCell alloc] init];
    }
    [DPTheme styleListCell:cell];
    return cell;
}

- (void)deleteAccountButtonPress:(UIGestureRecognizer *)recognizer {
    UITableViewCell *cell = (UITableViewCell *)recognizer.view;
    UIActivityIndicatorView *activity = (UIActivityIndicatorView*)cell.accessoryView;
    [activity startAnimating];
    UIAlertController *alertController =
        [UIAlertController alertControllerWithTitle:[NSString stringWithFormat:@"Delete Account %@", DPSettingsModel.sharedInstance.userString]
                                            message:@"Are you sure you want to delete your account and all associated data?  This cannot be undone.  Locally-saved songs and preferences will not be deleted."
                                     preferredStyle:UIAlertControllerStyleAlert];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                        style:UIAlertActionStyleCancel
                                                      handler:^(UIAlertAction * _Nonnull action) {
        [activity stopAnimating];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Yes"
                                                        style:UIAlertActionStyleDestructive
                                                      handler:^(UIAlertAction * _Nonnull action) {
        [[DPSongsModel sharedInstance] detachFromFirestore];
        [[DPSettingsModel sharedInstance] detachFromFirestore];
        FIRHTTPSCallable *callable = [[FIRFunctions functions] HTTPSCallableWithName:@"deleteUser"];
        [callable callWithCompletion:^(FIRHTTPSCallableResult * _Nullable result, NSError * _Nullable error) {
            [activity stopAnimating];
            if (!error) {
                [[FIRAuth auth] signOut:nil];
                [self->tableView reloadData];
            } else {
                UIAlertController *errorAlert =
                    [UIAlertController alertControllerWithTitle:@"Couldn't Delete Account"
                                                        message:[NSString stringWithFormat:@"Something went wrong and your account was not deleted. Please try again. (%@)", error.localizedDescription]
                                                 preferredStyle:UIAlertControllerStyleAlert];
                [errorAlert addAction:[UIAlertAction actionWithTitle:@"OK"
                                                               style:UIAlertActionStyleDefault
                                                             handler:nil]];
                [self presentViewController:errorAlert animated:YES completion:nil];
            }
        }];
    }]];
    [self presentViewController:alertController
                       animated:YES
                     completion:^{
    }];
}

- (void)loginButtonPress:(UIGestureRecognizer *)recognizer {
    if ([FIRAuth auth].currentUser) {
        [[FIRAuth auth] signOut:nil];
        [tableView reloadData];
    } else {
        UITableViewCell *cell = (UITableViewCell *)recognizer.view;
        UIActivityIndicatorView *activity = (UIActivityIndicatorView*)cell.accessoryView;
        [activity startAnimating];
        __block DPLoginViewController *loginViewController = [[DPLoginViewController alloc] init];
        loginViewController.loginCompletion = ^{
            [activity stopAnimating];
            [self->tableView reloadData];
            loginViewController.loginCompletion = nil;
            loginViewController = nil;
        };
        [loginViewController logIn:self];
    }
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    switch (section) {
        case 0:
            return 3;
        case 1:
            return [FIRAuth auth].currentUser ? 2 : 1;
        case 2:
            return [self isPrivateBuild] ? 2 : 0;
        default:
            break;
    }
    return 0;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return [self isPrivateBuild] ? 3 : 2;
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
        return @"Log in to back up and synchronize your song list and settings.";
    }
    return nil;
}

- (BOOL)isPrivateBuild {
    return [self privateBuildNumber].length > 0;
}

- (NSString *)privateBuildNumber {
    return [[NSBundle mainBundle] objectForInfoDictionaryKey:@"PrivateBuildNumber"] ?: @"";
}

- (NSString *)privatePRNumber {
    return [[NSBundle mainBundle] objectForInfoDictionaryKey:@"PrivatePRNumber"] ?: @"?";
}

- (void)copyLogs {
    NSString *metadata = [NSString stringWithFormat:@"Build %@ · PR #%@", [self privateBuildNumber], [self privatePRNumber]];
    [DPAppLog log:@"Settings: copied app logs"];
    [UIPasteboard generalPasteboard].string = [NSString stringWithFormat:@"%@\n\n%@", metadata, [DPAppLog contents]];
}

- (void)complete {
    [self dismissViewControllerAnimated:YES completion:nil];
}

@end
#endif
