//
//  DPSettingsController.m
//  tagmaster
//
//  Created by David Poll on 9/30/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#if __has_include(<UIKit/UIKit.h>)
#import "DPSettingsController.h"

@import FirebaseAuth;
#import "DPAppDelegate.h"
#import "tagmaster-Swift.h"

typedef NS_ENUM(NSInteger, TMSettingsSection) {
    TMSettingsSectionAccount,
    TMSettingsSectionLists,
    TMSettingsSectionRandomFilters,
    TMSettingsSectionPrivateBuild
};

@interface DPSettingsController () <UITableViewDataSource, UITableViewDelegate>

@property (nonatomic, strong) UITableView *tableView;
@property (nonatomic, strong) UISegmentedControl *minRating;
@property (nonatomic, strong) UISegmentedControl *minDownloads;
@property (nonatomic, strong) UISegmentedControl *sheetMusic;
@property (nonatomic, strong) UISegmentedControl *learningTracks;
@property (nonatomic, strong) NSArray<NSString *> *filterTitles;
@property (nonatomic, strong) NSArray<UIView *> *filterControls;

@end

@implementation DPSettingsController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    [DPAppDelegate setUpBackground:self.view];
    // Inset groups need the grouped page color behind them to read as groups in light mode.
    self.view.backgroundColor = [UIColor systemGroupedBackgroundColor];
    
    self.title = @"Settings";
    
    self.minRating = [[UISegmentedControl alloc] initWithItems:@[@"Any", @"1", @"2", @"3", @"4"]];
    self.minRating.apportionsSegmentWidthsByContent = YES;
    self.minRating.selectedSegmentIndex = [DPSettingsController minRatingValue];
    self.minDownloads = [[UISegmentedControl alloc] initWithItems:@[@"Any", @"50", @"100", @"500", @"1000"]];
    self.minDownloads.apportionsSegmentWidthsByContent = YES;
    self.minDownloads.selectedSegmentIndex = [DPSettingsController minDownloadsValue];
    self.sheetMusic = [[UISegmentedControl alloc] initWithItems:@[@"Not Important", @"Yes", @"No"]];
    self.sheetMusic.apportionsSegmentWidthsByContent = YES;
    self.sheetMusic.selectedSegmentIndex = [DPSettingsController sheetMusicValue];
    self.learningTracks = [[UISegmentedControl alloc] initWithItems:@[@"Not Important", @"Yes", @"No"]];
    self.learningTracks.apportionsSegmentWidthsByContent = YES;
    self.learningTracks.selectedSegmentIndex = [DPSettingsController learningTracksValue];
    
    self.filterTitles = @[@"Minimum Rating", @"Minimum Downloads", @"Sheet Music", @"Learning Tracks"];
    NSArray *controls = @[self.minRating, self.minDownloads, self.sheetMusic, self.learningTracks];
    NSMutableArray *filterControls = [NSMutableArray array];
    for (NSUInteger index = 0; index < controls.count; index++) {
        UISegmentedControl *control = controls[index];
        control.accessibilityLabel = self.filterTitles[index];
        [control.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
        [filterControls addObject:[self makeFilterControl:control label:self.filterTitles[index]]];
    }
    self.filterControls = filterControls;
    
    // Settings-shaped content belongs in an inset-grouped list.
    self.tableView = [[UITableView alloc] initWithFrame:CGRectZero style:UITableViewStyleInsetGrouped];
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
    self.tableView.backgroundColor = [UIColor clearColor];
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.estimatedRowHeight = 60;
    self.tableView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.tableView];
    [NSLayoutConstraint activateConstraints:@[
        [self.tableView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.tableView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.tableView.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [self.tableView.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
    ]];
    
    [self refreshLoginButton];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self.tableView reloadData];
}

- (void)refreshLoginButton {
    if (!self.isViewLoaded) return;
    [self.tableView reloadSections:[NSIndexSet indexSetWithIndex:TMSettingsSectionAccount] withRowAnimation:UITableViewRowAnimationNone];
}

- (BOOL)isSignedIn {
    return [FIRAuth auth].currentUser != nil;
}

#pragma mark - Table view

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return [self isPrivateBuild] ? 4 : 3;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    switch ((TMSettingsSection)section) {
        case TMSettingsSectionAccount: return 1;
        case TMSettingsSectionLists: return 2;
        case TMSettingsSectionRandomFilters: return self.filterTitles.count;
        case TMSettingsSectionPrivateBuild: return 2;
    }
    return 0;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    switch ((TMSettingsSection)section) {
        case TMSettingsSectionAccount: return @"Account";
        case TMSettingsSectionLists: return @"Saved Tags";
        case TMSettingsSectionRandomFilters: return @"Random Tag Filters";
        case TMSettingsSectionPrivateBuild: return @"Private Build";
    }
    return nil;
}

- (NSString *)tableView:(UITableView *)tableView titleForFooterInSection:(NSInteger)section {
    switch ((TMSettingsSection)section) {
        case TMSettingsSectionAccount: return @"Log in to back up and synchronize your tag lists.";
        case TMSettingsSectionLists: return @"Clearing a list removes every tag from it on this device and, when logged in, on your other devices.";
        case TMSettingsSectionRandomFilters: return @"Random Tag only picks tags that match these filters.";
        case TMSettingsSectionPrivateBuild: return nil;
    }
    return nil;
}

- (UITableViewCell *)actionCellWithTitle:(NSString *)title detail:(NSString *)detail destructive:(BOOL)destructive enabled:(BOOL)enabled {
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:nil];
    cell.textLabel.text = title;
    cell.textLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    cell.textLabel.adjustsFontForContentSizeCategory = YES;
    cell.textLabel.numberOfLines = 0;
    cell.detailTextLabel.text = detail;
    cell.detailTextLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    cell.detailTextLabel.adjustsFontForContentSizeCategory = YES;
    UIColor *color = destructive ? [UIColor systemRedColor] : self.view.tintColor;
    cell.textLabel.textColor = enabled ? color : [UIColor tertiaryLabelColor];
    cell.userInteractionEnabled = enabled;
    cell.accessibilityTraits = UIAccessibilityTraitButton | (enabled ? 0 : UIAccessibilityTraitNotEnabled);
    return cell;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    switch ((TMSettingsSection)indexPath.section) {
        case TMSettingsSectionAccount: {
            UITableViewCell *cell = [self actionCellWithTitle:[self isSignedIn] ? @"Log Out" : @"Log In" detail:nil destructive:NO enabled:YES];
            cell.accessibilityHint = [self isSignedIn] ? @"Signs out of Tag Master on this device" : @"Opens the sign-in options";
            return cell;
        }
        case TMSettingsSectionLists: {
            NSUInteger count = indexPath.row == 0 ? [DPAppDelegate favorites].count : [DPAppDelegate teachable].count;
            NSString *title = indexPath.row == 0 ? @"Clear Favorites" : @"Clear Teachable Tags";
            NSString *detail = [NSString stringWithFormat:@"%lu %@", (unsigned long)count, count == 1 ? @"tag" : @"tags"];
            return [self actionCellWithTitle:title detail:detail destructive:YES enabled:count > 0];
        }
        case TMSettingsSectionRandomFilters:
            return [self makeFormCellWithHeader:self.filterTitles[indexPath.row] control:self.filterControls[indexPath.row]];
        case TMSettingsSectionPrivateBuild: {
            if (indexPath.row == 0) {
                UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:nil];
                cell.textLabel.text = [NSString stringWithFormat:@"Build %@ · PR #%@", [self privateBuildNumber], [self privatePRNumber]];
                cell.textLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
                cell.textLabel.adjustsFontForContentSizeCategory = YES;
                cell.textLabel.numberOfLines = 0;
                cell.selectionStyle = UITableViewCellSelectionStyleNone;
                return cell;
            }
            return [self actionCellWithTitle:@"Copy Logs" detail:nil destructive:NO enabled:YES];
        }
    }
    return [[UITableViewCell alloc] init];
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    switch ((TMSettingsSection)indexPath.section) {
        case TMSettingsSectionAccount:
            [self logInClick];
            break;
        case TMSettingsSectionLists:
            if (indexPath.row == 0) [self clearFavorites]; else [self clearTeachable];
            break;
        case TMSettingsSectionRandomFilters:
            break;
        case TMSettingsSectionPrivateBuild:
            if (indexPath.row == 1) [self copyLogs];
            break;
    }
}

+ (NSInteger)minDownloadsValue {
    [[NSUserDefaults standardUserDefaults] registerDefaults:@{@"random.minDownloads": @2}];
    return [[NSUserDefaults standardUserDefaults] integerForKey:@"random.minDownloads"];
}

+ (void)setMinDownloadsValue:(NSInteger)value {
    [[NSUserDefaults standardUserDefaults] setInteger:value forKey:@"random.minDownloads"];
}

+ (NSNumber *)minDownloads {
    switch (DPSettingsController.minDownloadsValue) {
        case 0:
            return nil;
        case 1:
            return @50;
        case 2:
            return @100;
        case 3:
            return @500;
        case 4:
            return @1000;
    }
    return nil;
}

+ (NSNumber *)minRating {
    switch (DPSettingsController.minRatingValue) {
        case 0:
            return nil;
        case 1:
            return @1.0;
        case 2:
            return @2.0;
        case 3:
            return @3.0;
        case 4:
            return @4.0;
    }
    return nil;
}

+ (NSNumber *)learningTracks {
    switch (DPSettingsController.learningTracksValue) {
        case 0:
            return nil;
        case 1:
            return @YES;
        case 2:
            return @NO;
    }
    return nil;
}

+ (NSNumber *)sheetMusic {
    switch (DPSettingsController.sheetMusicValue) {
        case 0:
            return nil;
        case 1:
            return @YES;
        case 2:
            return @NO;
    }
    return nil;
}

+ (NSInteger)minRatingValue {
    [[NSUserDefaults standardUserDefaults] registerDefaults:@{@"random.minRating": @2}];
    return [[NSUserDefaults standardUserDefaults] integerForKey:@"random.minRating"];
}

+ (void)setMinRatingValue:(NSInteger)value {
    [[NSUserDefaults standardUserDefaults] setInteger:value forKey:@"random.minRating"];
}

+ (NSInteger)sheetMusicValue {
    [[NSUserDefaults standardUserDefaults] registerDefaults:@{@"random.sheetMusic": @1}];
    return [[NSUserDefaults standardUserDefaults] integerForKey:@"random.sheetMusic"];
}

+ (void)setSheetMusicValue:(NSInteger)value {
    [[NSUserDefaults standardUserDefaults] setInteger:value forKey:@"random.sheetMusic"];
}

+ (NSInteger)learningTracksValue {
    return [[NSUserDefaults standardUserDefaults] integerForKey:@"random.learningTracks"];
}

+ (void)setLearningTracksValue:(NSInteger)value {
    [[NSUserDefaults standardUserDefaults] setInteger:value forKey:@"random.learningTracks"];
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

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [DPSettingsController setMinDownloadsValue:self.minDownloads.selectedSegmentIndex];
    [DPSettingsController setMinRatingValue:self.minRating.selectedSegmentIndex];
    [DPSettingsController setSheetMusicValue:self.sheetMusic.selectedSegmentIndex];
    [DPSettingsController setLearningTracksValue:self.learningTracks.selectedSegmentIndex];
}

- (void)clearFavorites {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Clear Favorites?"
                                                                             message:@"Every favorite will be removed from your list. You can add tags again from any tag's Favorite and Teachable options."
                                                                      preferredStyle:UIAlertControllerStyleAlert];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Clear Favorites"
                                                        style:UIAlertActionStyleDestructive
                                                      handler:^(UIAlertAction * _Nonnull action) {
        [DPAppDelegate setFavorites:@[]];
        [self.tableView reloadSections:[NSIndexSet indexSetWithIndex:TMSettingsSectionLists] withRowAnimation:UITableViewRowAnimationNone];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                        style:UIAlertActionStyleCancel
                                                      handler:nil]];
    [self presentViewController:alertController animated:YES completion:nil];
}

- (void)clearTeachable {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Clear Teachable Tags?"
                                                                             message:@"Every teachable tag will be removed from your list. You can mark tags as teachable again from any tag's Favorite and Teachable options."
                                                                      preferredStyle:UIAlertControllerStyleAlert];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Clear Teachable Tags"
                                                        style:UIAlertActionStyleDestructive
                                                      handler:^(UIAlertAction * _Nonnull action) {
        [DPAppDelegate setTeachable:@[]];
        [self.tableView reloadSections:[NSIndexSet indexSetWithIndex:TMSettingsSectionLists] withRowAnimation:UITableViewRowAnimationNone];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                        style:UIAlertActionStyleCancel
                                                      handler:nil]];
    [self presentViewController:alertController animated:YES completion:nil];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
#endif
