//
//  DPTagQueryViewController.m
//  tagmaster
//
//  Created by David Poll on 8/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPTagQueryViewController.h"
#import "DPTagCell.h"
#import "DPUtils+Subscripts.h"
#import "DPTagViewController.h"
#import "DPAppDelegate.h"
#import "DPTagPageControllerBase.h"

#import "TMBarberPoleLoadingView.h"

@interface DPTagQueryViewController () <UITableViewDataSource, UITableViewDelegate>

@property (atomic, retain) DPTagQueryResult *mostRecentResult;
@property (nonatomic, retain) TMBarberPoleLoadingView *activity;
@property (nonatomic, retain) UILabel *statusLabel;
@property (nonatomic, retain) UIButton *retryButton;
@property (nonatomic) BOOL failed;
@property (nonatomic, retain) UITableView *tagTable;
@property (nonatomic, retain) UIRefreshControl *refreshControl;

- (NSUInteger)indexForTagId:(int)tagId;
- (void)tm_syncSelectionForSplit;

@end

@implementation DPTagQueryViewController

@synthesize tagTable;

- (id)init {
    if (self = [super init]) {
        [self commonInit];
    }
    return self;
}

- (id)initWithCoder:(NSCoder *)aDecoder {
    if (self = [super initWithCoder:aDecoder]) {
        [self commonInit];
    }
    return self;
}

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        [self commonInit];
    }
    return self;
}

- (void)commonInit {
    self.mostRecentResult = [[DPTagQueryResult alloc] init];
    self.mostRecentResult.start = 0;
    self.mostRecentResult.count = 0;
    self.isLoading = NO;
    self.hasMoreResults = YES;
    self.resultSetSize = 20;
    self.maxResults = 1000;
    self.tags = [NSMutableArray array];
    
    //self.query = @"lover come back";
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(tm_splitSelectionChanged:) name:TMTagSelectionDidChangeNotification object:nil];
    
    if (![self.tabBarController.parentViewController isKindOfClass:[TMPageViewController class]]) {
        [DPAppDelegate setUpBackground:self.view];
    }
    
	// Do any additional setup after loading the view.
    
    self.tagTable = [[UITableView alloc] init];
    self.tagTable.translatesAutoresizingMaskIntoConstraints = NO;
    self.tagTable.delegate = self;
    self.tagTable.dataSource = self;
    self.tagTable.backgroundColor = [UIColor clearColor];
    self.tagTable.rowHeight = UITableViewAutomaticDimension;
    self.tagTable.estimatedRowHeight = 100;
    [self.tagTable registerClass:[DPTagCell class] forCellReuseIdentifier:@"Tag"];
    
    self.activity = [[TMBarberPoleLoadingView alloc] initWithFrame:CGRectMake(0, 0, self.view.bounds.size.width, 68)];
    
    self.statusLabel = [[UILabel alloc] init];
    self.statusLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    self.statusLabel.adjustsFontForContentSizeCategory = YES;
    self.statusLabel.numberOfLines = 0;
    self.statusLabel.textAlignment = NSTextAlignmentCenter;
    self.statusLabel.textColor = [UIColor secondaryLabelColor];
    self.retryButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [self.retryButton setTitle:@"Retry" forState:UIControlStateNormal];
    [self.retryButton.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
    [self.retryButton addTarget:self action:@selector(refresh) forControlEvents:UIControlEventTouchUpInside];
    
    self.tagTable.tableFooterView = self.activity;
    
    self.refreshControl = [[UIRefreshControl alloc] init];
    [self.refreshControl addTarget:self action:@selector(refresh) forControlEvents:UIControlEventValueChanged];
    [self.tagTable addSubview:self.refreshControl];
    
    [self.view addSubview:self.tagTable];
    
    NSMutableDictionary *bindings = [NSMutableDictionary dictionaryWithDictionary:NSDictionaryOfVariableBindings(tagTable)];
    
    if (![self.tabBarController.parentViewController isKindOfClass:[TMPageViewController class]]) {
        [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[tagTable]|"
                                                                          options:0
                                                                          metrics:nil
                                                                            views:bindings]];
        [tagTable.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor].active = YES;
    } else {
        [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[tagTable]|"
                                                                          options:0
                                                                          metrics:nil
                                                                            views:bindings]];
    }
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[tagTable]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:bindings]];
    
    [self fetchResults];
    [self refreshViews]; // A request may have started before the view mounted.
    
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
    self.navigationItem.title = !self.query || self.query.length == 0 ? @"Search Results" : self.query;
}

- (void)refresh {
    if (self.isLoading) return;
    self.hasMoreResults = YES;
    self.failed = NO;
    self.statusText = nil;
    self.mostRecentResult = [[DPTagQueryResult alloc] init];
    self.mostRecentResult.start = 0;
    self.mostRecentResult.count = 0;
    self.tags = [NSMutableArray array];
    [self fetchResults];
}

- (void)refreshViews {
    if (self.isLoading) {
        self.tagTable.tableFooterView = self.activity;
        [self.activity startAnimating];
    } else {
        [self.activity stopAnimating];
        self.tagTable.tableFooterView = nil;
    }
    
    if (self.statusText) {
        self.statusLabel.text = self.statusText;
        // Centered symbol, message and recovery, the way system empty states read.
        UIImageView *symbol = [[UIImageView alloc] initWithImage:[UIImage systemImageNamed:self.failed ? @"wifi.exclamationmark" : @"magnifyingglass"
                                                                            withConfiguration:[UIImageSymbolConfiguration configurationWithPointSize:44 weight:UIImageSymbolWeightLight]]];
        symbol.tintColor = [UIColor secondaryLabelColor];
        symbol.contentMode = UIViewContentModeScaleAspectFit;
        symbol.isAccessibilityElement = NO;
        UIStackView *header = [[UIStackView alloc] initWithArrangedSubviews:@[symbol, self.statusLabel, self.retryButton]];
        header.axis = UILayoutConstraintAxisVertical;
        header.alignment = UIStackViewAlignmentCenter;
        header.spacing = 12;
        header.layoutMarginsRelativeArrangement = YES;
        header.directionalLayoutMargins = NSDirectionalEdgeInsetsMake(48, 32, 24, 32);
        self.retryButton.hidden = !self.failed;
        self.tagTable.tableHeaderView = header;
        [self sizeStatusHeader];
    } else {
        self.tagTable.tableHeaderView = nil;
    }
    [self sizeStatusHeader];
    [self.activity sizeToFit];
    if ([self.refreshControl isRefreshing]) {
        [self.refreshControl endRefreshing];
    }

    [self.tagTable reloadData];
    [self tm_syncSelectionForSplit];
    [NSNotificationCenter.defaultCenter postNotificationName:TMTagListDidChangeNotification object:self];
}

- (void)sizeStatusHeader {
    UIView *header = self.tagTable.tableHeaderView;
    if (!header) return;
    CGFloat width = self.tagTable.bounds.size.width;
    CGFloat height = [header systemLayoutSizeFittingSize:CGSizeMake(width, 0) withHorizontalFittingPriority:UILayoutPriorityRequired verticalFittingPriority:UILayoutPriorityFittingSizeLevel].height;
    if (header.frame.size.height != height || header.frame.size.width != width) {
        header.frame = CGRectMake(0, 0, width, height);
        self.tagTable.tableHeaderView = header;
    }
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    self.activity.controllerVisible = YES;
    [self tm_syncSelectionForSplit];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    self.activity.controllerVisible = NO;
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    [self tm_syncSelectionForSplit];
    [self sizeStatusHeader];
    if (self.tagTable.tableFooterView == self.activity) {
        CGRect frame = CGRectMake(0, 0, self.tagTable.bounds.size.width, 68);
        if (!CGSizeEqualToSize(self.activity.frame.size, frame.size)) {
            self.activity.frame = frame;
            self.tagTable.tableFooterView = self.activity;
        }
    }
}

- (void)fetchResults {
    if (self.isLoading) {
        return;
    }
    if (!self.hasMoreResults) {
        return;
    }
    self.isLoading = YES;
    [self refreshViews];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        DPTagQueryResult *result = nil;
        @try {
            result = [DPTag query:self.query
                 numberOfResults:self.resultSetSize
                           start:self.mostRecentResult.start + self.mostRecentResult.count
                           parts:self.parts
                  learningTracks:self.hasLearningTracks
                      sheetMusic:self.hasSheetMusic
                      collection:self.collection
                          sortBy:self.sortBy];
        } @catch (NSException *exception) {
            // The main-thread completion presents the same recovery state as a nil result.
        }
        dispatch_async(dispatch_get_main_queue(), ^{
            // Steppers read this list on main. Publish a whole page there too.
            if (result) {
                self.mostRecentResult = result;
                self.tags = [self.tags arrayByAddingObjectsFromArray:result.tags];
                self.hasMoreResults = result.start + result.count < MIN(result.available, self.maxResults);
                self.statusText = result.available == 0 ? @"No tags could be found that matched your query." : nil;
            } else {
                self.statusText = @"Tags couldn't be loaded. Check your connection and try again.";
                self.failed = YES;
                self.hasMoreResults = NO;
            }
            self.isLoading = NO;
            [self refreshViews];
        });
    });
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.tags.count;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    BOOL expanded = self.splitViewController && !self.splitViewController.isCollapsed;
    if (!expanded) {
        [tableView deselectRowAtIndexPath:indexPath animated:YES];
    }
    [DPAppDelegate showTagWithId:[self.tags[indexPath.row] tagId] from:self];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    DPTagCell *cell = [tableView dequeueReusableCellWithIdentifier:@"Tag" forIndexPath:indexPath];
    cell.tagInstance = self.tags[indexPath.row];
    BOOL expanded = self.splitViewController && !self.splitViewController.isCollapsed;
    cell.accessoryType = expanded ? UITableViewCellAccessoryNone : UITableViewCellAccessoryDisclosureIndicator;
    return cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewAutomaticDimension;
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    [self.activity updateAnimation];
    CGFloat currentOffset = scrollView.contentOffset.y;
    CGFloat threshold = scrollView.contentSize.height * 7 / 8 - scrollView.frame.size.height;
    if (currentOffset >= threshold) {
        [self fetchResults];
    }
}

#pragma mark - TMTagListSource

- (NSArray<NSNumber *> *)tm_listedTagIds {
    return [self.tags valueForKeyPath:@"tagId"];
}

- (NSUInteger)indexForTagId:(int)tagId {
    return [self.tags indexOfObjectPassingTest:^BOOL(DPTag *candidate, NSUInteger idx, BOOL *stop) {
        return candidate.tagId == tagId;
    }];
}

- (void)tm_didStepToTagId:(int)tagId {
    NSUInteger index = [self indexForTagId:tagId];
    if (index != NSNotFound) {
        NSIndexPath *path = [NSIndexPath indexPathForRow:index inSection:0];
        [self.tagTable selectRowAtIndexPath:path animated:!UIAccessibilityIsReduceMotionEnabled() scrollPosition:UITableViewScrollPositionNone];
        [self.tagTable scrollToRowAtIndexPath:path atScrollPosition:UITableViewScrollPositionNone animated:!UIAccessibilityIsReduceMotionEnabled()];
    }
    DPTag *last = self.tags.lastObject;
    if (last && last.tagId == tagId && self.hasMoreResults) {
        [self fetchResults];
    }
}

- (void)tm_splitSelectionChanged:(NSNotification *)notification {
    [self tm_syncSelectionForSplit];
}

- (void)tm_syncSelectionForSplit {
    if (!self.isViewLoaded) return;
    BOOL expanded = self.splitViewController && !self.splitViewController.isCollapsed;
    for (UITableViewCell *cell in self.tagTable.visibleCells) {
        if ([cell isKindOfClass:DPTagCell.class]) {
            UITableViewCellAccessoryType accessory = expanded ? UITableViewCellAccessoryNone : UITableViewCellAccessoryDisclosureIndicator;
            if (cell.accessoryType != accessory) cell.accessoryType = accessory;
        }
    }
    NSNumber *current = expanded ? [DPAppDelegate currentSplitTagIdFor:self] : nil;
    NSUInteger index = current ? [[self tm_listedTagIds] indexOfObject:current] : NSNotFound;
    NSIndexPath *path = index == NSNotFound ? nil : [NSIndexPath indexPathForRow:index inSection:0];
    NSIndexPath *selected = self.tagTable.indexPathForSelectedRow;
    if (selected && ![selected isEqual:path]) [self.tagTable deselectRowAtIndexPath:selected animated:NO];
    if (path && ![selected isEqual:path]) [self.tagTable selectRowAtIndexPath:path animated:NO scrollPosition:UITableViewScrollPositionNone];
}

@end
