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

@interface DPSearchViewController () <UISearchBarDelegate>

@property (nonatomic, strong) UISearchBar *searchBar;
@property (nonatomic, strong) UISegmentedControl *sortBy;
@property (nonatomic, strong) UISegmentedControl *sheetMusic;
@property (nonatomic, strong) UISegmentedControl *learningTracks;
@property (nonatomic, strong) UISegmentedControl *parts;
@property (nonatomic, strong) UISegmentedControl *collection;

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

	DPGridLayout *grid = [[DPGridLayout alloc] init];
    grid.rowDimensions = @[
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension]
                           ];
    grid.columnDimensions = @[
                              [DPGridDimension dimensionWithSize:75],
                              [DPGridDimension dimensionWithSize:8],
                              [DPGridDimension dimensionWithStars:1]
                              ];
    
    self.searchBar = [[UISearchBar alloc] init];
    self.searchBar.placeholder = @"Search";
    self.searchBar.barTintColor = [UIColor clearColor];
    self.searchBar.delegate = self;
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
    
    UILabel *searchOptionsHeader = [self makeTitleLabel];
    searchOptionsHeader.text = @"Search Options";
    
    [grid addSubview:self.searchBar row:0 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:[searchOptionsHeader padLeft:0 top:0 right:0 bottom:8] row:1 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:[self makeHeader:@"Sort By"] row:2 column:0];
    [grid addSubview:[self.sortBy padHorizontal:0 vertical:8] row:2 column:2];
    [grid addSubview:[self makeHeader:@"Sheet Music"] row:3 column:0];
    [grid addSubview:[self.sheetMusic padHorizontal:0 vertical:8] row:3 column:2];
    [grid addSubview:[self makeHeader:@"Tracks"] row:4 column:0];
    [grid addSubview:[self.learningTracks padHorizontal:0 vertical:8] row:4 column:2];
    [grid addSubview:[self makeHeader:@"Parts"] row:5 column:0];
    [grid addSubview:[self.parts padHorizontal:0 vertical:8] row:5 column:2];
    [grid addSubview:[self makeHeader:@"Collection"] row:6 column:0];
    [grid addSubview:[self.collection padHorizontal:0 vertical:8] row:6 column:2];
    
    UIScrollView *scroller = [[UIScrollView alloc] init];
    
    [self setUpRootView:grid withScroller:scroller];
    [DPAppDelegate setUpBackground:self.view];
    //[self.view bringSubviewToFront:scroller];
    
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemSearch
                                                                                           target:self
                                                                                           action:@selector(search)];
    
    UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc]
                                   initWithTarget:self
                                   action:@selector(dismissKeyboard)];
    
    [self.view addGestureRecognizer:tap];
    
    self.navigationItem.title = @"Search";
}

- (void)viewDidDisappear:(BOOL)animated {
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
