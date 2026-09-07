//
//  DPTeachableTagsController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  The teaching list: the tags this singer is ready to teach, in the order they
//  want to teach them.
//

#import "DPTeachableTagsController.h"
#import "DPAppDelegate.h"
#import "DPTagCell.h"
#import "DPBrowseViewController.h"
#import "DPTagViewController.h"
#import "tagmaster-Swift.h"

@interface DPTeachableTagsController ()

@property (nonatomic, strong) TMEmptyStateView *emptyState;

@end

@implementation DPTeachableTagsController


- (id)init {
    return [self initWithStyle:UITableViewStyleInsetGrouped];
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
    self.tableView.estimatedRowHeight = 88;
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.accessibilityIdentifier = @"teachableTags";

    self.emptyState = [[TMEmptyStateView alloc] initWithFrame:CGRectZero];
    [self.emptyState configureWithSymbolName:@"person.2.wave.2"
                                       title:@"No teachable tags yet"
                                     message:@"Mark a tag as teachable from its tag menu and it "
                                              "will wait here for the next time you teach."
                                 actionTitle:nil
                                      action:nil];

    self.navigationItem.title = @"Teachable Tags";
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] init];
    self.navigationItem.backBarButtonItem.title = @"Teachable";

    self.navigationItem.rightBarButtonItem = self.editButtonItem;
    self.editButtonItem.accessibilityLabel = @"Edit teachable tags";
    [self viewDidLoadExtension];
    [self updateEmptyState];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self.tableView reloadData];
    [self updateEmptyState];
}

- (void)updateEmptyState {
    BOOL empty = [DPAppDelegate teachable].count == 0;
    self.tableView.backgroundView = empty ? self.emptyState : nil;
    self.editButtonItem.enabled = !empty;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return [DPAppDelegate teachable].count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    int tagId = [[DPAppDelegate teachable][indexPath.row] intValue];
    DPTagCell *tagCell = [self.tableView dequeueReusableCellWithIdentifier:@"Tag" forIndexPath:indexPath];
    tagCell.tagId = tagId;
    return tagCell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    int tagId = [[DPAppDelegate teachable][indexPath.row] intValue];
    DPTagViewController *tagViewController = [[DPTagViewController alloc] init];
    tagViewController.tagId = tagId;
    [self.navigationController pushViewController:tagViewController animated:YES];
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        [DPAppDelegate removeTeachable:[[DPAppDelegate teachable][indexPath.row] intValue]];
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
    [DPAppDelegate moveTeachableAt:fromIndexPath.row to:toIndexPath.row];
    [TMTheme saved];
}

// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

@end
