//
//  DPTagTracksController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagTracksController.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import <MediaPlayer/MediaPlayer.h>

@interface DPTagTracksController () <UITableViewDataSource, UITableViewDelegate>

@property (nonatomic, strong) UILabel *apology;
@property (nonatomic, strong) UILabel *recordingNotesHeader;
@property (nonatomic, strong) UILabel *recordingNotesLabel;
@property (nonatomic, strong) UITableView *partsTable;
@property (nonatomic, strong) UIStackView *header;

@end

@implementation DPTagTracksController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	
    self.apology = [self makeBodyLabel];
    self.apology.text = @"Sorry, no tracks could be found for this tag.";
    self.apology.numberOfLines = 0;
    self.recordingNotesHeader = [self makeHeader:@"Recording Notes"];
    self.recordingNotesHeader.textColor = [UIColor secondaryLabelColor];
    self.recordingNotesLabel = [self makeBodyLabel];
    self.recordingNotesLabel.numberOfLines = 0;
    
    self.partsTable = [[UITableView alloc] initWithFrame:CGRectNull style:UITableViewStyleGrouped];
    self.partsTable.delegate = self;
    self.partsTable.dataSource = self;
    self.partsTable.backgroundColor = [UIColor clearColor];
    
    self.partsTable.translatesAutoresizingMaskIntoConstraints = NO;
    self.partsTable.rowHeight = UITableViewAutomaticDimension;
    self.partsTable.estimatedRowHeight = 60;
    self.header = [[UIStackView alloc] initWithArrangedSubviews:@[self.apology, self.recordingNotesHeader, self.recordingNotesLabel]];
    self.header.axis = UILayoutConstraintAxisVertical;
    self.header.spacing = 8;
    self.header.layoutMarginsRelativeArrangement = YES;
    self.header.directionalLayoutMargins = NSDirectionalEdgeInsetsMake(16, 16, 16, 16);
    self.partsTable.tableHeaderView = self.header;
    [self.view addSubview:self.partsTable];
    [NSLayoutConstraint activateConstraints:@[
        [self.partsTable.leadingAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.leadingAnchor],
        [self.partsTable.trailingAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.trailingAnchor],
        [self.partsTable.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor],
        [self.partsTable.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor]
    ]];
    [self refreshView];
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    CGFloat width = self.partsTable.bounds.size.width;
    CGFloat height = [self.header systemLayoutSizeFittingSize:CGSizeMake(width, 0) withHorizontalFittingPriority:UILayoutPriorityRequired verticalFittingPriority:UILayoutPriorityFittingSizeLevel].height;
    if (self.header.frame.size.height != height || self.header.frame.size.width != width) {
        self.header.frame = CGRectMake(0, 0, width, height);
        self.partsTable.tableHeaderView = self.header;
    }
}

- (void)refreshView {
    self.apology.hidden = self.tag.tracks.count > 0;
    self.recordingNotesLabel.text = self.tag.recordingMethod;
    self.recordingNotesLabel.hidden = !self.tag.recordingMethod;
    self.recordingNotesHeader.hidden = !self.tag.recordingMethod;
    [self.partsTable reloadData];
    if (self.isViewLoaded) [self.view setNeedsLayout];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    UITableViewCell *cell = [[UITableViewCell alloc] init];
    cell.textLabel.text = [self.tag.tracks[indexPath.row] title];
    cell.textLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    cell.textLabel.adjustsFontForContentSizeCategory = YES;
    cell.textLabel.numberOfLines = 0;
    cell.backgroundColor = [UIColor clearColor];
    // Tapping a part plays it; say so before the tap.
    cell.imageView.image = [UIImage systemImageNamed:@"play.circle"
                                   withConfiguration:[UIImageSymbolConfiguration configurationWithTextStyle:UIFontTextStyleTitle2]];
    cell.imageView.tintColor = self.view.tintColor;
    cell.accessibilityHint = @"Plays the learning track";
    return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.tag.tracks.count;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    return @"Tracks";
}

@end
