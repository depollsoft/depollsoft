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

@interface DPTagQueryViewController () <UITableViewDataSource, UITableViewDelegate>

@property (atomic, retain) DPTagQueryResult *mostRecentResult;
@property (nonatomic, retain) UIActivityIndicatorView *activity;
@property (nonatomic, retain) UILabel *statusLabel;
@property (nonatomic, retain) UITableView *tagTable;

@end

@implementation DPTagQueryViewController

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
    
    self.query = @"lover come back";
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view.
    
    self.tagTable = [[UITableView alloc] initWithFrame:self.view.frame];
    self.tagTable.delegate = self;
    self.tagTable.dataSource = self;
    
    self.activity = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
    self.activity.hidesWhenStopped = YES;
    
    self.statusLabel = [[UILabel alloc] init];
    
    self.tagTable.tableFooterView = self.activity;
    
    [self.view addSubview:self.tagTable];
    [self fetchResults];
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}

- (void)refreshViews {
    if (self.isLoading) {
        [self.activity startAnimating];
    } else {
        [self.activity stopAnimating];
    }
    
    if (self.statusText) {
        self.statusLabel.text = self.statusText;
        self.tagTable.tableHeaderView = self.statusLabel;
    } else {
        self.tagTable.tableHeaderView = self.statusLabel;
    }
    [self.statusLabel sizeToFit];
    [self.activity sizeToFit];
    
    [self.tagTable reloadData];
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
            self.mostRecentResult = result;
            [(NSMutableArray *)self.tags addObjectsFromArray:result.tags];
            self.hasMoreResults = result.start + result.count < MIN(result.available, self.maxResults);
            if (result.available == 0) {
                self.statusText = @"No tags could be found that matched your query.";
            }
        }
        @catch (NSException *exception) {
            self.statusText = [NSString stringWithFormat:@"An error has occurred: %@", exception.reason];
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

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    DPTagCell *cell = [[DPTagCell alloc] init];
    cell.tag = self.tags[indexPath.row];
    return cell;
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    CGFloat currentOffset = scrollView.contentOffset.y;
    CGFloat threshold = scrollView.contentSize.height * 7 / 8 - scrollView.frame.size.height;
    if (currentOffset >= threshold) {
        [self fetchResults];
    }
}

@end
