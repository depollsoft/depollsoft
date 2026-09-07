//
//  DPFavoritesViewController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  Favorites, in the singer's saved order.
//

#import "DPFavoritesViewController.h"
#import "DPAppDelegate.h"
#import "DPTagCell.h"
#import "DPBrowseViewController.h"
#import "DPTagViewController.h"
#import "tagmaster-Swift.h"

@interface DPFavoritesViewController ()

@property (nonatomic, strong) TMEmptyStateView *emptyState;

@end

@implementation DPFavoritesViewController


- (id)init {
    return [self initWithStyle:UITableViewStylePlain];
}

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.tableView];

    [self.tableView registerClass:[DPTagCell class] forCellReuseIdentifier:@"Tag"];
    self.tableView.cellLayoutMarginsFollowReadableWidth = YES;
    self.tableView.estimatedRowHeight = 64;
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.accessibilityIdentifier = @"favorites";

    self.emptyState = [[TMEmptyStateView alloc] initWithFrame:CGRectZero];
    [self.emptyState configureWithSymbolName:@"star"
                                       title:@"No favorites yet"
                                     message:@"Save favorites from any tag’s list menu."
                                 actionTitle:nil
                                      action:nil];

    self.title = @"Favorites";
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] init];
    self.navigationItem.backBarButtonItem.title = @"Favorites";

    self.navigationItem.rightBarButtonItem = self.editButtonItem;
    self.editButtonItem.accessibilityIdentifier = @"favorites.edit";
    self.editButtonItem.accessibilityLabel = @"Edit favorites";
    // Keep the page background installed in every list state.
    UIView *background = self.tableView.backgroundView;
    self.emptyState.translatesAutoresizingMaskIntoConstraints = NO;
    [background addSubview:self.emptyState];
    [NSLayoutConstraint activateConstraints:@[
        [self.emptyState.leadingAnchor constraintEqualToAnchor:background.leadingAnchor],
        [self.emptyState.trailingAnchor constraintEqualToAnchor:background.trailingAnchor],
        [self.emptyState.topAnchor constraintEqualToAnchor:background.safeAreaLayoutGuide.topAnchor],
        [self.emptyState.bottomAnchor constraintEqualToAnchor:background.safeAreaLayoutGuide.bottomAnchor]
    ]];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onUserDataChanged)
        name:@"tagmaster.userDataChanged" object:nil];
    [self updateEmptyState];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self updateEmptyState];
}

- (void)setEditing:(BOOL)editing animated:(BOOL)animated {
    [super setEditing:editing animated:animated];
    self.editButtonItem.accessibilityLabel = editing ? @"Done editing favorites" : @"Edit favorites";
}

- (void)updateEmptyState {
    BOOL empty = [DPAppDelegate favorites].count == 0;
    self.emptyState.hidden = !empty;
    self.editButtonItem.enabled = !empty;
}

- (void)onUserDataChanged {
    if (![NSThread isMainThread]) {
        [self performSelectorOnMainThread:@selector(onUserDataChanged) withObject:nil waitUntilDone:NO];
        return;
    }
    CGPoint offset = self.tableView.contentOffset;
    [self.tableView reloadData];
    [self.tableView layoutIfNeeded];
    [self.tableView setContentOffset:offset animated:NO];
    [self updateEmptyState];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return [DPAppDelegate favorites].count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    int tagId = [[DPAppDelegate favorites][indexPath.row] intValue];
    DPTagCell *tagCell = [self.tableView dequeueReusableCellWithIdentifier:@"Tag" forIndexPath:indexPath];
    tagCell.tagId = tagId;
    return tagCell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    int tagId = [[DPAppDelegate favorites][indexPath.row] intValue];
    DPTagViewController *tagViewController = [[DPTagViewController alloc] init];
    tagViewController.tagId = tagId;
    [self.navigationController pushViewController:tagViewController animated:YES];
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        [DPAppDelegate removeFavorite:[[DPAppDelegate favorites][indexPath.row] intValue]];
        [TMTheme saved];
        [self.tableView reloadData];
        [self updateEmptyState];
    }
}

- (NSString *)tableView:(UITableView *)tableView titleForDeleteConfirmationButtonForRowAtIndexPath:(NSIndexPath *)indexPath {
    return @"Remove";
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewAutomaticDimension;
}

// Override to support rearranging the table view.
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
    [DPAppDelegate moveFavoriteAt:fromIndexPath.row to:toIndexPath.row];
    [TMTheme saved];
}

// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

@end
