//
//  DPSettingsController.m
//  tagmaster
//
//  Created by David Poll on 9/30/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPSettingsController.h"

@import Firebase;
@import FirebaseUI;
#import "DPAppDelegate.h"
#import "tagmaster-Swift.h"

@interface DPSettingsController () <UIAlertViewDelegate>

@property (nonatomic, strong) DPBusyIndicator *busyIndicator;

@property (nonatomic, strong) UIButton *logInButton;
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
    
    DPGridLayout *grid = [[DPGridLayout alloc] init];
    grid.rowDimensions = @[
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension]
                           ];
    grid.columnDimensions = @[
                              [DPGridDimension dimension],
                              [DPGridDimension dimensionWithSize:8],
                              [DPGridDimension dimensionWithStars:1]
                              ];
    UIScrollView *scroller = [[UIScrollView alloc] init];
    
    self.logInButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    self.clearFavoritesButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [self.clearFavoritesButton setTitle:@"Clear Favorites" forState:UIControlStateNormal];
    [self.clearFavoritesButton addTarget:self action:@selector(clearFavorites) forControlEvents:UIControlEventTouchUpInside];
    self.clearTeachableButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [self.clearTeachableButton setTitle:@"Clear Teachable Tags" forState:UIControlStateNormal];
    [self.clearTeachableButton addTarget:self action:@selector(clearTeachable) forControlEvents:UIControlEventTouchUpInside];
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
    
    UILabel *logInHeader = [self makeTitleLabel];
    logInHeader.text = @"Log In";
    UILabel *logInText = [self makeBodyLabel];
    logInText.text = @"Log in to back up and synchronize your tag lists.";
    logInText.numberOfLines = 0;
    UILabel *favoritesHeader = [self makeTitleLabel];
    favoritesHeader.text = @"Favorites";
    UILabel *teachableHeader = [self makeTitleLabel];
    teachableHeader.text = @"Teachable Tags";
    UILabel *randomTagsHeader = [self makeTitleLabel];
    randomTagsHeader.text = @"Random Tag Filters";
    UILabel *minRatingHeader = [self makeHeader:@"Minimum Rating"];
    UILabel *minDownloadHeader = [self makeHeader:@"Minimum Downloads"];
    UILabel *sheetMusicHeader = [self makeHeader:@"Sheet Music"];
    UILabel *learningTracksHeader = [self makeHeader:@"Learning Tracks"];
    
    [grid addSubview:logInHeader row:0 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:logInText row:1 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:self.logInButton row:2 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:favoritesHeader row:3 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:self.clearFavoritesButton row:4 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:teachableHeader row:5 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:self.clearTeachableButton row:6 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:randomTagsHeader row:7 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:minRatingHeader row:8 column:0];
    [grid addSubview:[self.minRating padHorizontal:0 vertical:8] row:8 column:2];
    [grid addSubview:minDownloadHeader row:9 column:0];
    [grid addSubview:[self.minDownloads padHorizontal:0 vertical:8] row:9 column:2];
    [grid addSubview:sheetMusicHeader row:10 column:0];
    [grid addSubview:[self.sheetMusic padHorizontal:0 vertical:8] row:10 column:2];
    [grid addSubview:learningTracksHeader row:11 column:0];
    [grid addSubview:[self.learningTracks padHorizontal:0 vertical:8] row:11 column:2];
    
    [self.logInButton addTarget:self action:@selector(logInClick) forControlEvents:UIControlEventTouchUpInside];
    
    [self setUpRootView:grid withScroller:scroller];
    
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

- (void)refreshLoginButton {
    if (![FIRAuth auth].currentUser) {
        [self.logInButton setTitle:@"Log In" forState:UIControlStateNormal];
    } else {
        [self.logInButton setTitle:@"Log Out" forState:UIControlStateNormal];
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

- (void)viewDidDisappear:(BOOL)animated {
    [DPSettingsController setMinDownloadsValue:self.minDownloads.selectedSegmentIndex];
    [DPSettingsController setMinRatingValue:self.minRating.selectedSegmentIndex];
    [DPSettingsController setSheetMusicValue:self.sheetMusic.selectedSegmentIndex];
    [DPSettingsController setLearningTracksValue:self.learningTracks.selectedSegmentIndex];
}

- (void)clearFavorites {
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Favorite Tags"
                                                    message:@"Are you sure you want to clear your favorites?"
                                                   delegate:self
                                          cancelButtonTitle:@"No"
                                          otherButtonTitles:@"Yes", nil];
    [alert show];
}

- (void)clearTeachable {
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Teachable Tags"
                                                    message:@"Are you sure you want to clear your teachable tags list?"
                                                   delegate:self
                                          cancelButtonTitle:@"No"
                                          otherButtonTitles:@"Yes", nil];
    [alert show];
}

- (void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex {
    if (buttonIndex == alertView.cancelButtonIndex) {
        return;
    }
    if ([alertView.title isEqualToString:@"Favorite Tags"]) {
        [DPAppDelegate setFavorites:@[]];
    } else {
        [DPAppDelegate setTeachable:@[]];
    }
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
