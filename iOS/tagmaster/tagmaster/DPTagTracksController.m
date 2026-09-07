//
//  DPTagTracksController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  Learning tracks, one row per voice part. A singer taps their part and it
//  plays; the row says which part it is, not just "track 3".
//

#import "DPTagTracksController.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "tagmaster-Swift.h"
#import <MediaPlayer/MediaPlayer.h>

@interface DPTagTracksController () <UITableViewDataSource>

@property (nonatomic, strong) TMEmptyStateView *emptyState;
@property (nonatomic, strong) UILabel *recordingNotesHeader;
@property (nonatomic, strong) UILabel *recordingNotesLabel;
@property (nonatomic, strong) UIView *recordingNotes;
@property (nonatomic, strong) UITableView *partsTable;

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

    self.emptyState = [[TMEmptyStateView alloc] initWithFrame:CGRectZero];
    self.emptyState.translatesAutoresizingMaskIntoConstraints = NO;
    [self.emptyState configureWithSymbolName:@"waveform"
                                       title:@"No learning tracks"
                                     message:@"Nobody has posted part recordings for this tag yet. "
                                              "The sheet music and videos may still help."
                                 actionTitle:nil
                                      action:nil];

    self.recordingNotesHeader = [self makeHeader:@"Recording Notes"];
    self.recordingNotesLabel = [self makeBodyLabel];
    self.recordingNotesLabel.numberOfLines = 0;
    UIStackView *notes = [[UIStackView alloc] initWithArrangedSubviews:@[
        self.recordingNotesHeader, self.recordingNotesLabel
    ]];
    notes.axis = UILayoutConstraintAxisVertical;
    notes.spacing = TMTheme.spaceXS;
    notes.translatesAutoresizingMaskIntoConstraints = NO;
    notes.layoutMargins = UIEdgeInsetsMake(TMTheme.spaceS, TMTheme.spaceL,
                                           TMTheme.spaceS, TMTheme.spaceL);
    notes.layoutMarginsRelativeArrangement = YES;
    self.recordingNotes = notes;

    self.partsTable = [[UITableView alloc] initWithFrame:CGRectZero
                                                   style:UITableViewStyleInsetGrouped];
    self.partsTable.delegate = self;
    self.partsTable.dataSource = self;
    self.partsTable.backgroundColor = [UIColor clearColor];
    self.partsTable.translatesAutoresizingMaskIntoConstraints = NO;
    self.partsTable.cellLayoutMarginsFollowReadableWidth = YES;
    self.partsTable.estimatedRowHeight = TMTheme.minimumTarget;
    self.partsTable.rowHeight = UITableViewAutomaticDimension;
    self.partsTable.accessibilityIdentifier = @"learningTracks";

    [self.view addSubview:self.recordingNotes];
    [self.view addSubview:self.partsTable];
    [self.view addSubview:self.emptyState];

    UILayoutGuide *safe = self.view.safeAreaLayoutGuide;
    [NSLayoutConstraint activateConstraints:@[
        [self.recordingNotes.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [self.recordingNotes.leadingAnchor constraintEqualToAnchor:self.view.readableContentGuide.leadingAnchor],
        [self.recordingNotes.trailingAnchor constraintEqualToAnchor:self.view.readableContentGuide.trailingAnchor],

        [self.partsTable.topAnchor constraintEqualToAnchor:self.recordingNotes.bottomAnchor],
        [self.partsTable.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.partsTable.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.partsTable.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],

        [self.emptyState.topAnchor constraintEqualToAnchor:safe.topAnchor],
        [self.emptyState.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.emptyState.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.emptyState.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
    ]];

    [self refreshView];
}

- (void)refreshView {
    BOOL hasTracks = self.tag.tracks.count > 0;

    self.recordingNotesLabel.text = self.tag.recordingMethod;
    self.recordingNotes.hidden = !self.tag.recordingMethod || !hasTracks;

    [self.partsTable reloadData];
    self.partsTable.hidden = !hasTracks;
    self.emptyState.hidden = hasTracks;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault
                                                   reuseIdentifier:nil];
    NSString *title = [self.tag.tracks[indexPath.row] title] ?: @"Track";

    UIListContentConfiguration *content = [UIListContentConfiguration cellConfiguration];
    content.text = title;
    content.textProperties.font = [TMTheme fontWithStyle:UIFontTextStyleBody weight:UIFontWeightSemibold];
    content.textProperties.color = [TMTheme primaryText];
    content.image = [UIImage systemImageNamed:@"play.circle.fill"];
    content.imageProperties.tintColor = [TMTheme tint];
    content.imageProperties.preferredSymbolConfiguration =
        [UIImageSymbolConfiguration configurationWithTextStyle:UIFontTextStyleTitle3];
    content.imageToTextPadding = TMTheme.spaceM;
    content.directionalLayoutMargins = NSDirectionalEdgeInsetsMake(TMTheme.spaceM, 0,
                                                                   TMTheme.spaceM, 0);
    cell.contentConfiguration = content;
    cell.backgroundColor = [TMTheme surface];
    cell.accessibilityLabel = [NSString stringWithFormat:@"Play %@", title];
    cell.accessibilityTraits = UIAccessibilityTraitButton;
    return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return self.tag.tracks.count;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    return @"Learning Tracks";
}

@end
