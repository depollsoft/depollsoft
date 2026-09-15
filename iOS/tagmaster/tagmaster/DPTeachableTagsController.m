//
//  DPTeachableTagsController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTeachableTagsController.h"
#import "DPAppDelegate.h"
#import "DPTagCell.h"
#import "DPBrowseViewController.h"
#import "DPTagViewController.h"
#import "tagmaster-Swift.h"

@interface DPTeachableTagsController ()
@property (nonatomic, strong) UIView *emptyHeader;
@end

@implementation DPTeachableTagsController


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
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(tm_splitSelectionChanged:) name:TMTagSelectionDidChangeNotification object:nil];
    [DPAppDelegate setUpBackground:self.view];

    [self.tableView registerClass:[DPTagCell class] forCellReuseIdentifier:@"Tag"];
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.estimatedRowHeight = 100;
    
    self.navigationItem.title = @"Teachable Tags";
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] init];
    self.navigationItem.backBarButtonItem.title = @"Teachable";
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    self.navigationItem.rightBarButtonItem = self.editButtonItem;
    // The row for the tag open beside this list stays selected instead of
    // clearing when the screen reappears in an expanded split.
    self.clearsSelectionOnViewWillAppear = !(self.splitViewController && !self.splitViewController.isCollapsed);
    [self viewDidLoadExtension];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self tm_syncSelectionForSplit];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self.tableView reloadData];
    [self tm_syncSelectionForSplit];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (UIView *)emptyHeader {
    if (!_emptyHeader) {
        // A table header participates in native scrolling and accessibility. The
        // unavailable overlay can put its action below the viewport at AX sizes.
        UILabel *heading = [UILabel new];
        heading.text = @"No teachable tags yet";
        UIFont *titleFont = [UIFont preferredFontForTextStyle:UIFontTextStyleTitle2];
        heading.font = [UIFont fontWithDescriptor:[titleFont.fontDescriptor fontDescriptorWithSymbolicTraits:UIFontDescriptorTraitBold] size:0];
        heading.accessibilityTraits |= UIAccessibilityTraitHeader;
        UILabel *guidance = [UILabel new];
        guidance.text = @"Open a tag, choose Favorite and Teachable options, then Mark as Teachable. Your teaching list will appear here.";
        guidance.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
        guidance.textColor = UIColor.secondaryLabelColor;
        for (UILabel *label in @[heading, guidance]) {
            label.numberOfLines = 0;
            label.adjustsFontForContentSizeCategory = YES;
            label.textAlignment = NSTextAlignmentCenter;
        }
        UIButton *browse = [UIButton buttonWithType:UIButtonTypeSystem];
        [browse setTitle:@"Browse Tags" forState:UIControlStateNormal];
        browse.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
        browse.titleLabel.adjustsFontForContentSizeCategory = YES;
        browse.titleLabel.numberOfLines = 0;
        browse.titleLabel.textAlignment = NSTextAlignmentCenter;
        browse.accessibilityIdentifier = @"teachable.browse";
        [browse.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
        [browse addTarget:self action:@selector(browseTags) forControlEvents:UIControlEventTouchUpInside];
        UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[heading, guidance, browse]];
        stack.axis = UILayoutConstraintAxisVertical;
        stack.spacing = 16;
        stack.translatesAutoresizingMaskIntoConstraints = NO;
        _emptyHeader = [UIView new];
        [_emptyHeader addSubview:stack];
        [NSLayoutConstraint activateConstraints:@[
            [stack.leadingAnchor constraintEqualToAnchor:_emptyHeader.leadingAnchor constant:32],
            [stack.trailingAnchor constraintEqualToAnchor:_emptyHeader.trailingAnchor constant:-32],
            [stack.topAnchor constraintEqualToAnchor:_emptyHeader.topAnchor constant:32],
            [stack.bottomAnchor constraintEqualToAnchor:_emptyHeader.bottomAnchor constant:-32]
        ]];
    }
    return _emptyHeader;
}

- (void)browseTags {
    [self.navigationController pushViewController:[[DPBrowseViewController alloc] init] animated:YES];
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    [self tm_syncSelectionForSplit];
    UIView *header = self.tableView.tableHeaderView;
    CGFloat width = self.tableView.bounds.size.width;
    if (!header || width <= 0) return;
    CGSize fit = [header systemLayoutSizeFittingSize:CGSizeMake(width, 0)
        withHorizontalFittingPriority:UILayoutPriorityRequired verticalFittingPriority:UILayoutPriorityFittingSizeLevel];
    if (fabs(header.bounds.size.width - width) > .5 || fabs(header.bounds.size.height - ceil(fit.height)) > .5) {
        header.frame = CGRectMake(0, 0, width, ceil(fit.height));
        self.tableView.tableHeaderView = header;
    }
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    NSInteger count = [DPAppDelegate teachable].count;
    UIView *header = count == 0 ? self.emptyHeader : nil;
    if (tableView.tableHeaderView != header) {
        tableView.tableHeaderView = header;
        [self.view setNeedsLayout];
    }
    self.editButtonItem.enabled = count > 0;
    return count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    int tagId = [[DPAppDelegate teachable][indexPath.row] intValue];
    DPTagCell *tagCell = [self.tableView dequeueReusableCellWithIdentifier:@"Tag" forIndexPath:indexPath];
    tagCell.tagId = tagId;
    BOOL expanded = self.splitViewController && !self.splitViewController.isCollapsed;
    tagCell.accessoryType = expanded ? UITableViewCellAccessoryNone : UITableViewCellAccessoryDisclosureIndicator;
    return tagCell;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    BOOL expanded = self.splitViewController && !self.splitViewController.isCollapsed;
    if (!expanded) [tableView deselectRowAtIndexPath:indexPath animated:YES];
    int tagId = [[DPAppDelegate teachable][indexPath.row] intValue];
    [DPAppDelegate showTagWithId:tagId from:self];
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        [DPAppDelegate removeTeachable:[[DPAppDelegate teachable][indexPath.row] intValue]];
        [self.tableView reloadData];
    }
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewAutomaticDimension;
}

// Override to support rearranging the table view.
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
    [DPAppDelegate moveTeachableAt:fromIndexPath.row to:toIndexPath.row];
}

// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

#pragma mark - TMTagListSource

- (NSArray<NSNumber *> *)tm_listedTagIds {
    return [DPAppDelegate teachable];
}

- (void)tm_didStepToTagId:(int)tagId {
    NSArray<NSNumber *> *list = [DPAppDelegate teachable];
    NSUInteger index = [list indexOfObject:@(tagId)];
    if (index == NSNotFound) return;
    NSIndexPath *path = [NSIndexPath indexPathForRow:index inSection:0];
    [self.tableView selectRowAtIndexPath:path animated:!UIAccessibilityIsReduceMotionEnabled() scrollPosition:UITableViewScrollPositionNone];
    [self.tableView scrollToRowAtIndexPath:path atScrollPosition:UITableViewScrollPositionNone animated:!UIAccessibilityIsReduceMotionEnabled()];
}

- (void)tm_splitSelectionChanged:(NSNotification *)notification {
    [self tm_syncSelectionForSplit];
}

- (void)tm_syncSelectionForSplit {
    if (!self.isViewLoaded) return;
    BOOL expanded = self.splitViewController && !self.splitViewController.isCollapsed;
    self.clearsSelectionOnViewWillAppear = !expanded;
    for (UITableViewCell *cell in self.tableView.visibleCells) {
        if ([cell isKindOfClass:DPTagCell.class]) {
            UITableViewCellAccessoryType accessory = expanded ? UITableViewCellAccessoryNone : UITableViewCellAccessoryDisclosureIndicator;
            if (cell.accessoryType != accessory) cell.accessoryType = accessory;
        }
    }
    NSNumber *current = expanded ? [DPAppDelegate currentSplitTagIdFor:self] : nil;
    NSUInteger index = current ? [[DPAppDelegate teachable] indexOfObject:current] : NSNotFound;
    NSIndexPath *path = index == NSNotFound ? nil : [NSIndexPath indexPathForRow:index inSection:0];
    NSIndexPath *selected = self.tableView.indexPathForSelectedRow;
    if (selected && ![selected isEqual:path]) [self.tableView deselectRowAtIndexPath:selected animated:NO];
    if (path && ![selected isEqual:path]) [self.tableView selectRowAtIndexPath:path animated:NO scrollPosition:UITableViewScrollPositionNone];
}

@end
