//
//  DPSettingsController.m
//  tagmaster
//
//  Created by David Poll on 9/30/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  A utility screen: the account that backs up lists, the two list-clearing
//  actions, and the filters that shape Random Tag.
//

#if __has_include(<UIKit/UIKit.h>)
#import "DPSettingsController.h"

@import FirebaseAuth;
#import "DPAppDelegate.h"
#import "tagmaster-Swift.h"

@interface DPSettingsController ()

@property (nonatomic, strong) DPBusyIndicator *busyIndicator;

@property (nonatomic, strong) UIButton *logInButton;
@property (nonatomic, strong) UILabel *accountStatusLabel;
@property (nonatomic, strong) UIButton *clearFavoritesButton;
@property (nonatomic, strong) UIButton *clearTeachableButton;
@property (nonatomic, strong) UISegmentedControl *minRating;
@property (nonatomic, strong) UISegmentedControl *minDownloads;
@property (nonatomic, strong) UISegmentedControl *sheetMusic;
@property (nonatomic, strong) UISegmentedControl *learningTracks;

@end

@implementation DPSettingsController

@synthesize busyIndicator;

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

    self.busyIndicator = [[DPBusyIndicator alloc] init];

    self.title = @"Settings";
    self.navigationItem.title = @"Settings";

    self.logInButton = [self makeActionButton:@"Log In" destructive:NO];
    [self.logInButton addTarget:self action:@selector(logInClick) forControlEvents:UIControlEventTouchUpInside];

    self.accountStatusLabel = [self makeBodyLabel];
    self.accountStatusLabel.numberOfLines = 0;
    self.accountStatusLabel.textColor = [TMTheme secondaryText];
    self.accountStatusLabel.font = [TMTheme metadataFont];

    self.clearFavoritesButton = [self makeActionButton:@"Clear Favorites" destructive:YES];
    [self.clearFavoritesButton addTarget:self action:@selector(clearFavorites) forControlEvents:UIControlEventTouchUpInside];
    self.clearTeachableButton = [self makeActionButton:@"Clear Teachable Tags" destructive:YES];
    [self.clearTeachableButton addTarget:self action:@selector(clearTeachable) forControlEvents:UIControlEventTouchUpInside];

    self.minRating = [[UISegmentedControl alloc] initWithItems:@[@"Any", @"1", @"2", @"3", @"4"]];
    self.minRating.apportionsSegmentWidthsByContent = YES;
    self.minRating.selectedSegmentIndex = [DPSettingsController minRatingValue];
    self.minRating.accessibilityLabel = @"Minimum rating";
    self.minDownloads = [[UISegmentedControl alloc] initWithItems:@[@"Any", @"50", @"100", @"500", @"1000"]];
    self.minDownloads.apportionsSegmentWidthsByContent = YES;
    self.minDownloads.selectedSegmentIndex = [DPSettingsController minDownloadsValue];
    self.minDownloads.accessibilityLabel = @"Minimum downloads";
    self.sheetMusic = [[UISegmentedControl alloc] initWithItems:@[@"Not Important", @"Yes", @"No"]];
    self.sheetMusic.apportionsSegmentWidthsByContent = YES;
    self.sheetMusic.selectedSegmentIndex = [DPSettingsController sheetMusicValue];
    self.sheetMusic.accessibilityLabel = @"Sheet music";
    self.learningTracks = [[UISegmentedControl alloc] initWithItems:@[@"Not Important", @"Yes", @"No"]];
    self.learningTracks.apportionsSegmentWidthsByContent = YES;
    self.learningTracks.selectedSegmentIndex = [DPSettingsController learningTracksValue];
    self.learningTracks.accessibilityLabel = @"Learning tracks";

    UILabel *logInText = [self makeBodyLabel];
    logInText.text = @"Log in to back up and synchronize your tag lists.";
    logInText.numberOfLines = 0;

    UILabel *listsText = [self makeBodyLabel];
    listsText.text = @"Clearing a list removes it on this device and, if you are logged in, "
                      "everywhere else it is synchronized.";
    listsText.numberOfLines = 0;
    listsText.font = [TMTheme metadataFont];
    listsText.textColor = [TMTheme secondaryText];

    UILabel *randomText = [self makeBodyLabel];
    randomText.text = @"Random Tag only picks tags that clear these thresholds.";
    randomText.numberOfLines = 0;
    randomText.font = [TMTheme metadataFont];
    randomText.textColor = [TMTheme secondaryText];

    NSMutableArray<UIView *> *sections = [NSMutableArray arrayWithArray:@[
        [self sectionWithTitle:@"Account" views:@[logInText, self.logInButton, self.accountStatusLabel]],
        [self sectionWithTitle:@"Your Lists"
                         views:@[listsText, self.clearFavoritesButton, self.clearTeachableButton]],
        [self sectionWithTitle:@"Random Tag Filters"
                         views:@[randomText,
                                 [self groupWithTitle:@"Minimum Rating" control:self.minRating],
                                 [self groupWithTitle:@"Minimum Downloads" control:self.minDownloads],
                                 [self groupWithTitle:@"Sheet Music" control:self.sheetMusic],
                                 [self groupWithTitle:@"Learning Tracks" control:self.learningTracks]]]
    ]];

    if ([self isPrivateBuild]) {
        UILabel *metadata = [self makeBodyLabel];
        metadata.text = [NSString stringWithFormat:@"Build %@ · PR #%@",
                         [self privateBuildNumber], [self privatePRNumber]];
        metadata.numberOfLines = 0;
        UIButton *copyLogs = [self makeActionButton:@"Copy Logs" destructive:NO];
        [copyLogs addTarget:self action:@selector(copyLogs) forControlEvents:UIControlEventTouchUpInside];
        [sections addObject:[self sectionWithTitle:@"Private Build" views:@[metadata, copyLogs]]];
    }

    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:sections];
    stack.axis = UILayoutConstraintAxisVertical;
    stack.alignment = UIStackViewAlignmentFill;
    stack.spacing = TMTheme.spaceXL;

    UIScrollView *scroller = [[UIScrollView alloc] init];
    [self setUpRootView:stack withScroller:scroller];

    self.busyIndicator.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.busyIndicator];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[busyIndicator]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(busyIndicator)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[busyIndicator]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(busyIndicator)]];

    [self refreshLoginButton];
}

- (UIButton *)makeActionButton:(NSString *)title destructive:(BOOL)destructive {
    UIButton *button = [UIButton buttonWithType:UIButtonTypeSystem];
    UIButtonConfiguration *config = destructive
        ? [UIButtonConfiguration grayButtonConfiguration]
        : [UIButtonConfiguration tintedButtonConfiguration];
    config.title = title;
    config.contentInsets = NSDirectionalEdgeInsetsMake(TMTheme.spaceM, TMTheme.spaceL,
                                                       TMTheme.spaceM, TMTheme.spaceL);
    if (destructive) {
        config.baseForegroundColor = [UIColor systemRedColor];
    }
    button.configuration = config;
    button.titleLabel.adjustsFontForContentSizeCategory = YES;
    [button.heightAnchor constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;
    return button;
}

/// A heading with its content beneath, more space above than below.
- (UIView *)sectionWithTitle:(NSString *)title views:(NSArray<UIView *> *)views {
    UILabel *heading = [[UILabel alloc] init];
    heading.text = title;
    heading.font = [TMTheme groupTitleFont];
    heading.adjustsFontForContentSizeCategory = YES;
    heading.textColor = [TMTheme ink];
    heading.numberOfLines = 0;
    heading.accessibilityTraits = UIAccessibilityTraitHeader;

    NSMutableArray<UIView *> *children = [NSMutableArray arrayWithObject:heading];
    [children addObjectsFromArray:views];

    UIStackView *section = [[UIStackView alloc] initWithArrangedSubviews:children];
    section.axis = UILayoutConstraintAxisVertical;
    section.alignment = UIStackViewAlignmentFill;
    section.spacing = TMTheme.spaceM;
    [section setCustomSpacing:TMTheme.spaceS afterView:heading];
    return section;
}

- (UIView *)groupWithTitle:(NSString *)title control:(UIControl *)control {
    UILabel *caption = [self makeHeader:title];
    UIView *choice = [control isKindOfClass:[UISegmentedControl class]]
        ? [[TMAdaptiveChoiceView alloc] initWithSegments:(UISegmentedControl *)control] : control;
    UIStackView *group = [[UIStackView alloc] initWithArrangedSubviews:@[caption, choice]];
    group.axis = UILayoutConstraintAxisVertical;
    group.spacing = TMTheme.spaceS;
    group.alignment = UIStackViewAlignmentFill;
    [choice.heightAnchor constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;
    return group;
}

- (void)refreshLoginButton {
    FIRUser *user = [FIRAuth auth].currentUser;
    UIButtonConfiguration *config = self.logInButton.configuration;
    if (!user) {
        config.title = @"Log In";
        self.logInButton.configuration = config;
        self.accountStatusLabel.text = @"Not logged in. Your lists stay on this device.";
    } else {
        config.title = @"Log Out";
        self.logInButton.configuration = config;
        NSString *identity = user.email.length > 0 ? user.email : (user.displayName ?: @"your account");
        self.accountStatusLabel.text =
            [NSString stringWithFormat:@"Logged in as %@. Lists back up to your account.", identity];
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
    [TMTheme saved];
}

- (void)viewDidDisappear:(BOOL)animated {
    [DPSettingsController setMinDownloadsValue:self.minDownloads.selectedSegmentIndex];
    [DPSettingsController setMinRatingValue:self.minRating.selectedSegmentIndex];
    [DPSettingsController setSheetMusicValue:self.sheetMusic.selectedSegmentIndex];
    [DPSettingsController setLearningTracksValue:self.learningTracks.selectedSegmentIndex];
}

- (void)clearFavorites {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Clear favorites?"
                                                                             message:@"This removes every tag from your favorites list."
                                                                      preferredStyle:UIAlertControllerStyleAlert];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Clear Favorites"
                                                        style:UIAlertActionStyleDestructive
                                                      handler:^(UIAlertAction * _Nonnull action) {
        [DPAppDelegate setFavorites:@[]];
        [TMTheme saved];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                        style:UIAlertActionStyleCancel
                                                      handler:nil]];
    [self presentViewController:alertController animated:YES completion:nil];
}

- (void)clearTeachable {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Clear teachable tags?"
                                                                             message:@"This removes every tag from your teachable list."
                                                                      preferredStyle:UIAlertControllerStyleAlert];
    [alertController addAction:[UIAlertAction actionWithTitle:@"Clear Teachable Tags"
                                                        style:UIAlertActionStyleDestructive
                                                      handler:^(UIAlertAction * _Nonnull action) {
        [DPAppDelegate setTeachable:@[]];
        [TMTheme saved];
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
