//
//  DPSearchViewController.m
//  tagmaster
//
//  Created by David Poll on 9/29/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  The query desk. The field comes first and searching is one return key away;
//  the filters sit under it as labelled groups rather than a cramped two-column
//  grid.
//

#import "DPSearchViewController.h"
#import "DPTagQueryViewController.h"
#import "UIView+DPUtils.h"
#import "DPAppDelegate.h"
#import "tagmaster-Swift.h"

@interface DPSearchViewController () <UISearchBarDelegate>

@property (nonatomic, strong) UISearchBar *searchBar;
@property (nonatomic, strong) UISegmentedControl *sortBy;
@property (nonatomic, strong) UISegmentedControl *sheetMusic;
@property (nonatomic, strong) UISegmentedControl *learningTracks;
@property (nonatomic, strong) UISegmentedControl *parts;
@property (nonatomic, strong) UISegmentedControl *collection;
@property (nonatomic, strong) UIButton *searchButton;

@end

@implementation DPSearchViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.view];

    self.searchBar = [[UISearchBar alloc] init];
    self.searchBar.placeholder = @"Search";
    self.searchBar.barTintColor = [UIColor clearColor];
    self.searchBar.backgroundImage = [[UIImage alloc] init];
    self.searchBar.delegate = self;
    self.searchBar.searchBarStyle = UISearchBarStyleMinimal;
    self.searchBar.returnKeyType = UIReturnKeySearch;
    self.searchBar.searchTextField.accessibilityLabel = @"Search tags by title or lyrics";
    self.searchBar.searchTextField.accessibilityIdentifier = @"tagSearchField";

    self.sortBy = [[UISegmentedControl alloc] initWithItems:@[@"Title", @"Downloads", @"Recent", @"Rating"]];
    self.sortBy.apportionsSegmentWidthsByContent = YES;
    self.sortBy.selectedSegmentIndex = self.sortByValue;
    self.sheetMusic = [[UISegmentedControl alloc] initWithItems:@[@"Not Important", @"Yes", @"No"]];
    self.sheetMusic.apportionsSegmentWidthsByContent = YES;
    self.sheetMusic.selectedSegmentIndex = self.sheetMusicValue;
    self.learningTracks = [[UISegmentedControl alloc] initWithItems:@[@"Not Important", @"Yes", @"No"]];
    self.learningTracks.apportionsSegmentWidthsByContent = YES;
    self.learningTracks.selectedSegmentIndex = self.learningTracksValue;
    self.parts = [[UISegmentedControl alloc] initWithItems:@[@"Any", @"3", @"4", @"5", @"6", @"7", @"8"]];
    self.parts.selectedSegmentIndex = self.partsValue;
    self.collection = [[UISegmentedControl alloc] initWithItems:@[@"Any", @"Classic Tags", @"Easy Tags"]];
    self.collection.apportionsSegmentWidthsByContent = YES;
    self.collection.selectedSegmentIndex = self.collectionValue;

    self.sortBy.accessibilityLabel = @"Sort by";
    self.sheetMusic.accessibilityLabel = @"Sheet music";
    self.learningTracks.accessibilityLabel = @"Learning tracks";
    self.parts.accessibilityLabel = @"Number of parts";
    self.collection.accessibilityLabel = @"Collection";

    UILabel *searchOptionsHeader = [self makeTitleLabel];
    searchOptionsHeader.text = @"Search Options";
    searchOptionsHeader.accessibilityTraits = UIAccessibilityTraitHeader;

    self.searchButton = [UIButton buttonWithType:UIButtonTypeSystem];
    UIButtonConfiguration *config = [UIButtonConfiguration filledButtonConfiguration];
    config.title = @"Search Tags";
    config.baseForegroundColor = [UIColor systemBackgroundColor];
    config.image = [UIImage systemImageNamed:@"magnifyingglass"];
    config.imagePadding = TMTheme.spaceS;
    config.contentInsets = NSDirectionalEdgeInsetsMake(TMTheme.spaceM, TMTheme.spaceL,
                                                       TMTheme.spaceM, TMTheme.spaceL);
    self.searchButton.configuration = config;
    self.searchButton.titleLabel.adjustsFontForContentSizeCategory = YES;
    [self.searchButton.heightAnchor
        constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;
    [self.searchButton addTarget:self action:@selector(search) forControlEvents:UIControlEventTouchUpInside];

    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[
        self.searchBar,
        self.searchButton,
        searchOptionsHeader,
        [self groupWithTitle:@"Sort By" control:self.sortBy],
        [self groupWithTitle:@"Sheet Music" control:self.sheetMusic],
        [self groupWithTitle:@"Learning Tracks" control:self.learningTracks],
        [self groupWithTitle:@"Parts" control:self.parts],
        [self groupWithTitle:@"Collection" control:self.collection]
    ]];
    stack.axis = UILayoutConstraintAxisVertical;
    stack.alignment = UIStackViewAlignmentFill;
    stack.spacing = TMTheme.spaceL;
    [stack setCustomSpacing:TMTheme.spaceS afterView:self.searchBar];
    [stack setCustomSpacing:TMTheme.spaceXL afterView:self.searchButton];

    UIScrollView *scroller = [[UIScrollView alloc] init];
    scroller.keyboardDismissMode = UIScrollViewKeyboardDismissModeInteractive;

    [self setUpRootView:stack withScroller:scroller];

    UIBarButtonItem *searchItem =
        [DPAppDelegate barButtonItemWithSystemName:@"magnifyingglass"
                                             target:self
                                             action:@selector(search)];
    searchItem.accessibilityLabel = @"Search";
    self.navigationItem.rightBarButtonItem = searchItem;

    UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc]
                                   initWithTarget:self
                                   action:@selector(dismissKeyboard)];
    tap.cancelsTouchesInView = NO;

    [self.view addGestureRecognizer:tap];

    self.navigationItem.title = @"Search";
}

/// A caption above its control, with the space above the caption larger than
/// the space below it so the pairing reads as one group.
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

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [self saveSettings];
}

- (NSInteger)sortByValue {
    return [[NSUserDefaults standardUserDefaults] integerForKey:@"search.sortBy"];
}

- (void)setSortByValue:(NSInteger)value {
    [[NSUserDefaults standardUserDefaults] setInteger:value forKey:@"search.sortBy"];
}

- (NSInteger)sheetMusicValue {
    return [[NSUserDefaults standardUserDefaults] integerForKey:@"search.sheetMusic"];
}

- (void)setSheetMusicValue:(NSInteger)value {
    [[NSUserDefaults standardUserDefaults] setInteger:value forKey:@"search.sheetMusic"];
}

- (NSInteger)learningTracksValue {
    return [[NSUserDefaults standardUserDefaults] integerForKey:@"search.learningTracks"];
}

- (void)setLearningTracksValue:(NSInteger)value {
    [[NSUserDefaults standardUserDefaults] setInteger:value forKey:@"search.learningTracks"];
}

- (NSInteger)partsValue {
    return [[NSUserDefaults standardUserDefaults] integerForKey:@"search.parts"];
}

- (void)setPartsValue:(NSInteger)value {
    [[NSUserDefaults standardUserDefaults] setInteger:value forKey:@"search.parts"];
}

- (NSInteger)collectionValue {
    return [[NSUserDefaults standardUserDefaults] integerForKey:@"search.collection"];
}

- (void)setCollectionValue:(NSInteger)value {
    [[NSUserDefaults standardUserDefaults] setInteger:value forKey:@"search.collection"];
}

- (void)dismissKeyboard {
    [self.searchBar resignFirstResponder];
}

- (void)searchBarSearchButtonClicked:(UISearchBar *)searchBar {
    [self search];
    [self.searchBar resignFirstResponder];
}

- (void)searchBarTextDidEndEditing:(UISearchBar *)searchBar {
    [self.searchBar resignFirstResponder];
}

- (void)saveSettings {
    self.sortByValue = self.sortBy.selectedSegmentIndex;
    self.sheetMusicValue = self.sheetMusic.selectedSegmentIndex;
    self.learningTracksValue = self.learningTracks.selectedSegmentIndex;
    self.partsValue = self.parts.selectedSegmentIndex;
    self.collectionValue = self.collection.selectedSegmentIndex;
}

- (void)search {
    DPTagQueryViewController *queryController = [[DPTagQueryViewController alloc] init];
    queryController.query = self.searchBar.text;

    [self saveSettings];

    switch (self.sortByValue) {
        case 0:
            queryController.sortBy = DPTagSortTitle;
            break;
        case 1:
            queryController.sortBy = DPTagSortDownloaded;
            break;
        case 2:
            queryController.sortBy = DPTagSortPosted;
            break;
        case 3:
            queryController.sortBy = DPTagSortRating;
            break;
    }

    switch (self.sheetMusicValue) {
        case 1:
            queryController.hasSheetMusic = @YES;
            break;
        case 2:
            queryController.hasSheetMusic = @NO;
            break;
    }

    switch (self.learningTracksValue) {
        case 1:
            queryController.hasLearningTracks = @YES;
            break;
        case 2:
            queryController.hasLearningTracks = @NO;
            break;
    }

    if (self.partsValue > 0) {
        queryController.parts = @(self.partsValue + 2);
    }

    switch (self.collectionValue) {
        case 0:
            queryController.collection = DPTagCollectionNone;
            break;
        case 1:
            queryController.collection = DPTagCollectionClassicTags;
            break;
        case 2:
            queryController.collection = DPTagCollectionEasyTags;
            break;
    }

    [self.navigationController pushViewController:queryController animated:YES];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
