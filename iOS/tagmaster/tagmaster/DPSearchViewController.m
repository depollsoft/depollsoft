//
//  DPSearchViewController.m
//  tagmaster
//
//  Created by David Poll on 9/29/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPSearchViewController.h"
#import "DPTagQueryViewController.h"
#import "UIView+DPUtils.h"
#import "DPAppDelegate.h"

@interface DPSearchViewController () <UISearchBarDelegate, UITableViewDataSource, UITableViewDelegate>

@property (nonatomic, strong) UISearchController *searchController;
@property (nonatomic, strong) UISearchBar *searchBar;
@property (nonatomic, strong) UITableView *tableView;
@property (nonatomic, strong) UISegmentedControl *sortBy;
@property (nonatomic, strong) UISegmentedControl *sheetMusic;
@property (nonatomic, strong) UISegmentedControl *learningTracks;
@property (nonatomic, strong) UISegmentedControl *parts;
@property (nonatomic, strong) UISegmentedControl *collection;
@property (nonatomic, strong) NSArray<NSString *> *filterTitles;
@property (nonatomic, strong) NSArray<UIView *> *filterControls;

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

    // The search field lives in the navigation bar, where iOS users expect it.
    self.searchController = [[UISearchController alloc] initWithSearchResultsController:nil];
    self.searchController.obscuresBackgroundDuringPresentation = NO;
    self.searchController.hidesNavigationBarDuringPresentation = NO;
    self.searchController.automaticallyShowsCancelButton = YES;
    self.searchBar = self.searchController.searchBar;
    self.searchBar.placeholder = @"Search";
    self.searchBar.delegate = self;
    self.searchBar.returnKeyType = UIReturnKeySearch;
    self.searchBar.enablesReturnKeyAutomatically = NO;
    self.navigationItem.searchController = self.searchController;
    self.navigationItem.hidesSearchBarWhenScrolling = NO;
    self.navigationItem.preferredSearchBarPlacement = UINavigationItemSearchBarPlacementStacked;
    self.definesPresentationContext = YES;

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
    
    self.filterTitles = @[@"Sort By", @"Sheet Music", @"Learning Tracks", @"Parts", @"Collection"];
    NSArray *controls = @[self.sortBy, self.sheetMusic, self.learningTracks, self.parts, self.collection];
    NSMutableArray *filterControls = [NSMutableArray array];
    for (NSUInteger index = 0; index < controls.count; index++) {
        UISegmentedControl *control = controls[index];
        control.accessibilityLabel = self.filterTitles[index];
        [control.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
        [filterControls addObject:[self makeFilterControl:control label:self.filterTitles[index]]];
    }
    self.filterControls = filterControls;
    
    self.tableView = [[UITableView alloc] initWithFrame:CGRectZero style:UITableViewStyleInsetGrouped];
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
    self.tableView.backgroundColor = [UIColor clearColor];
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.estimatedRowHeight = 80;
    self.tableView.keyboardDismissMode = UIScrollViewKeyboardDismissModeOnDrag;
    self.tableView.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.tableView];
    [NSLayoutConstraint activateConstraints:@[
        [self.tableView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.tableView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.tableView.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [self.tableView.bottomAnchor constraintEqualToAnchor:self.view.keyboardLayoutGuide.topAnchor]
    ]];
    [DPAppDelegate setUpBackground:self.view];
    // Inset groups need the grouped page color behind them to read as groups in light mode.
    self.view.backgroundColor = [UIColor systemGroupedBackgroundColor];
    
    // Runs the search with the current text and filters; also the way to search by filters alone.
    self.navigationItem.rightBarButtonItem =
        [DPAppDelegate barButtonItemWithSystemName:@"magnifyingglass"
                                             target:self
                                             action:@selector(search)];
    
    self.navigationItem.title = @"Search";
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [self saveSettings];
}

#pragma mark - Table view

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.filterTitles.count;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    return @"Search Options";
}

- (NSString *)tableView:(UITableView *)tableView titleForFooterInSection:(NSInteger)section {
    return @"Searches match titles and lyrics. Leave the field empty to list every tag that matches the options.";
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    return [self makeFormCellWithHeader:self.filterTitles[indexPath.row] control:self.filterControls[indexPath.row]];
}

#pragma mark - Settings

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
    
    // Keep the search controller inactive so the pushed results are not covered,
    // and keep the typed query for when the user comes back.
    NSString *query = self.searchBar.text;
    self.searchController.active = NO;
    self.searchBar.text = query;
    [self.navigationController pushViewController:queryController animated:YES];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
