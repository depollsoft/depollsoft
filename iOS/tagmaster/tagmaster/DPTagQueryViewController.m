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
#import "tagmaster-Swift.h"

@interface DPTagQueryViewController () <UITableViewDataSource, UITableViewDelegate>

@property (atomic, retain) DPTagQueryResult *mostRecentResult;
@property (nonatomic, retain) UIActivityIndicatorView *activity;
@property (nonatomic, retain) UIView *loadingFooter;
@property (nonatomic, retain) TMEmptyStateView *stateView;
@property (nonatomic, retain) UITableView *tagTable;
@property (nonatomic, retain) UIRefreshControl *refreshControl;
/// YES when the last fetch failed rather than simply returning nothing.
@property (nonatomic) BOOL lastFetchFailed;

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
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    if (!self.embedded) { [DPAppDelegate setUpBackground:self.view]; }
    else { self.view.backgroundColor = UIColor.clearColor; }

    self.tagTable = [[UITableView alloc] initWithFrame:CGRectZero style:UITableViewStylePlain];
    self.tagTable.translatesAutoresizingMaskIntoConstraints = NO;
    self.tagTable.delegate = self;
    self.tagTable.dataSource = self;
    self.tagTable.backgroundColor = [UIColor clearColor];
    self.tagTable.cellLayoutMarginsFollowReadableWidth = YES;
    self.tagTable.estimatedRowHeight = 88;
    self.tagTable.rowHeight = UITableViewAutomaticDimension;
    self.tagTable.accessibilityIdentifier = @"tagResults";
    [self.tagTable registerClass:[DPTagCell class] forCellReuseIdentifier:@"Tag"];

    self.activity = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleMedium];
    self.activity.hidesWhenStopped = NO;
    self.loadingFooter = [self makeLoadingFooter];

    self.stateView = [[TMEmptyStateView alloc] initWithFrame:CGRectZero];
    self.stateView.translatesAutoresizingMaskIntoConstraints = NO;
    self.stateView.hidden = YES;

    self.refreshControl = [[UIRefreshControl alloc] init];
    [self.refreshControl addTarget:self action:@selector(refresh) forControlEvents:UIControlEventValueChanged];
    self.tagTable.refreshControl = self.refreshControl;

    [self.view addSubview:self.tagTable];
    [self.view addSubview:self.stateView];

    // Embedded in Browse the container already sits below the collection
    // picker; standing alone the list starts at the safe area.
    NSLayoutYAxisAnchor *topAnchor =
        self.embedded ? self.view.topAnchor : self.view.safeAreaLayoutGuide.topAnchor;
    [NSLayoutConstraint activateConstraints:@[
        [self.tagTable.topAnchor constraintEqualToAnchor:topAnchor],
        [self.tagTable.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.tagTable.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.tagTable.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],

        [self.stateView.topAnchor constraintEqualToAnchor:self.tagTable.topAnchor],
        [self.stateView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.stateView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.stateView.bottomAnchor constraintEqualToAnchor:self.tagTable.bottomAnchor]
    ]];

    [self fetchResults];

    self.navigationItem.title = !self.query || self.query.length == 0 ? @"Search Results" : self.query;
}

- (UIView *)makeLoadingFooter {
    UILabel *label = [[UILabel alloc] init];
    label.text = @"Loading more tags…";
    label.font = [TMTheme metadataFont];
    label.adjustsFontForContentSizeCategory = YES;
    label.textColor = [TMTheme secondaryText];

    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[self.activity, label]];
    stack.axis = UILayoutConstraintAxisHorizontal;
    stack.alignment = UIStackViewAlignmentCenter;
    stack.spacing = TMTheme.spaceS;
    stack.layoutMargins = UIEdgeInsetsMake(TMTheme.spaceL, TMTheme.spaceL,
                                           TMTheme.spaceL, TMTheme.spaceL);
    stack.layoutMarginsRelativeArrangement = YES;

    UIView *footer = [[UIView alloc] init];
    [footer addSubview:stack];
    stack.translatesAutoresizingMaskIntoConstraints = NO;
    [NSLayoutConstraint activateConstraints:@[
        [stack.centerXAnchor constraintEqualToAnchor:footer.centerXAnchor],
        [stack.topAnchor constraintEqualToAnchor:footer.topAnchor],
        [stack.bottomAnchor constraintEqualToAnchor:footer.bottomAnchor]
    ]];
    CGSize size = [stack systemLayoutSizeFittingSize:UILayoutFittingCompressedSize];
    footer.frame = CGRectMake(0, 0, size.width, size.height);
    return footer;
}

- (void)refresh {
    self.mostRecentResult = [[DPTagQueryResult alloc] init];
    self.mostRecentResult.start = 0;
    self.mostRecentResult.count = 0;
    self.tags = [NSMutableArray array];
    self.hasMoreResults = YES;
    self.lastFetchFailed = NO;
    [self fetchResults];
}

- (void)refreshViews {
    BOOL showingResults = self.tags.count > 0;

    if (self.isLoading && showingResults) {
        self.tagTable.tableFooterView = self.loadingFooter;
        [self.activity startAnimating];
    } else if (self.lastFetchFailed && showingResults) {
        [self.activity stopAnimating];
        UIButton *retry = [UIButton buttonWithType:UIButtonTypeSystem];
        [retry setTitle:@"Couldn't load more tags. Try Again" forState:UIControlStateNormal];
        retry.titleLabel.font = [TMTheme bodyFont];
        retry.titleLabel.adjustsFontForContentSizeCategory = YES;
        retry.titleLabel.numberOfLines = 0;
        retry.titleLabel.textAlignment = NSTextAlignmentCenter;
        [retry addTarget:self action:@selector(retryPage) forControlEvents:UIControlEventTouchUpInside];
        CGSize size = [retry sizeThatFits:CGSizeMake(self.tagTable.bounds.size.width - 32, CGFLOAT_MAX)];
        retry.frame = CGRectMake(0, 0, self.tagTable.bounds.size.width, MAX(64, size.height + 24));
        self.tagTable.tableFooterView = retry;
    } else {
        [self.activity stopAnimating];
        self.tagTable.tableFooterView = [[UIView alloc] initWithFrame:CGRectZero];
    }

    [self updateStateView];

    if ([self.refreshControl isRefreshing]) {
        [self.refreshControl endRefreshing];
    }

    [self.tagTable reloadData];
}

/// Empty, error, and first-load states share one surface; each names what
/// happened and, where the singer can act, offers the way forward.
- (void)updateStateView {
    __weak typeof(self) weakSelf = self;
    if (self.tags.count > 0) {
        self.stateView.hidden = YES;
        self.tagTable.hidden = NO;
        return;
    }

    self.tagTable.hidden = NO;
    if (self.isLoading) {
        [self.stateView configureWithSymbolName:@"music.note.list"
                                          title:@"Finding tags…"
                                        message:@"Fetching from BarbershopTags.com."
                                    actionTitle:nil
                                         action:nil];
    } else if (self.lastFetchFailed) {
        [self.stateView configureWithSymbolName:@"wifi.exclamationmark"
                                          title:@"Couldn't reach BarbershopTags.com"
                                        message:self.statusText ?: @"The request did not complete."
                                    actionTitle:@"Try Again"
                                         action:^{
            [weakSelf refresh];
        }];
    } else {
        [self.stateView configureWithSymbolName:@"magnifyingglass"
                                          title:@"No tags matched"
                                        message:@"Try fewer filters, or a shorter search."
                                    actionTitle:@"Try Again"
                                         action:^{
            [weakSelf refresh];
        }];
    }
    self.stateView.hidden = NO;
}

- (void)retryPage {
    self.lastFetchFailed = NO;
    self.hasMoreResults = YES;
    [self fetchResults];
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
        @try {
            DPTagQueryResult *result = [DPTag query:self.query
                                    numberOfResults:self.resultSetSize
                                              start:self.mostRecentResult.start + self.mostRecentResult.count
                                              parts:self.parts
                                     learningTracks:self.hasLearningTracks
                                         sheetMusic:self.hasSheetMusic
                                         collection:self.collection
                                             sortBy:self.sortBy];
            if (!result) {
                [NSException raise:NSInternalInconsistencyException format:@"Result should be non-nil"];
            };
            self.statusText = nil;
            self.lastFetchFailed = NO;
            self.mostRecentResult = result;
            [(NSMutableArray *)self.tags addObjectsFromArray:result.tags];
            self.hasMoreResults = result.start + result.count < MIN(result.available, self.maxResults);
            if (result.available == 0) {
                self.statusText = @"No tags could be found that matched your query.";
            }
        }
        @catch (NSException *exception) {
            self.lastFetchFailed = YES;
            self.hasMoreResults = NO;
            self.statusText = [NSString stringWithFormat:@"%@", exception.reason ?: @"The request failed."];
        }
        @finally {
            self.isLoading = NO;
            [self performSelectorOnMainThread:@selector(refreshViews) withObject:nil waitUntilDone:NO];
        }
    });
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.tags.count;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    DPTagViewController *controller = [[DPTagViewController alloc] init];
    controller.tagId = [self.tags[indexPath.row] tagId];
    [self.navigationController pushViewController:controller animated:YES];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    DPTagCell *cell = [tableView dequeueReusableCellWithIdentifier:@"Tag" forIndexPath:indexPath];
    cell.tagInstance = self.tags[indexPath.row];
    return cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewAutomaticDimension;
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    CGFloat currentOffset = scrollView.contentOffset.y;
    CGFloat threshold = scrollView.contentSize.height * 7 / 8 - scrollView.frame.size.height;
    if (currentOffset >= threshold) {
        [self fetchResults];
    }
}

@end
