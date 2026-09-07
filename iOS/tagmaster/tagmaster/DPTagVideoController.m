//
//  DPTagVideoController.m
//  tagmaster
//
//  Created by David Poll on 10/5/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  Teaching video first, then what other quartets have posted. Thumbnails are
//  16:9 with a play badge so a row reads as a video, not as a grey box.
//

#import "DPTagVideoController.h"
#import "UIView+DPUtils.h"
#import "DPFileCache.h"
#import "tagmaster-Swift.h"

@interface DPTagVideoController () <UITableViewDelegate, UITableViewDataSource>

@property (nonatomic, strong) UITableView *tableView;
@property (nonatomic, strong) TMEmptyStateView *emptyState;

@end

@implementation DPTagVideoController

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
    self.tableView = [[UITableView alloc] initWithFrame:CGRectZero
                                                  style:UITableViewStylePlain];
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
    self.tableView.translatesAutoresizingMaskIntoConstraints = NO;
    self.tableView.backgroundColor = [UIColor clearColor];
    self.tableView.cellLayoutMarginsFollowReadableWidth = YES;
    self.tableView.estimatedRowHeight = 88;
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.accessibilityIdentifier = @"tagVideos";

    self.emptyState = [[TMEmptyStateView alloc] initWithFrame:CGRectZero];
    self.emptyState.translatesAutoresizingMaskIntoConstraints = NO;
    [self.emptyState configureWithSymbolName:@"play.rectangle"
                                       title:@"No videos yet"
                                     message:@"Nobody has posted a teaching video or a performance "
                                              "of this tag on BarbershopTags.com."
                                 actionTitle:nil
                                      action:nil];

    [self.view addSubview:self.tableView];
    [self.view addSubview:self.emptyState];

    [NSLayoutConstraint activateConstraints:@[
        [self.tableView.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor],
        [self.tableView.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.tableView.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.tableView.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],

        [self.emptyState.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor],
        [self.emptyState.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.emptyState.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.emptyState.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
    ]];

    [self refreshView];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)refreshView {
    [self.tableView reloadData];
    BOOL hasAny = self.tag.teachingVideo != nil || self.tag.videos.count > 0;
    self.emptyState.hidden = hasAny;
    self.tableView.hidden = !hasAny;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    if (self.tag.teachingVideo && section == 0) {
        return @"Teaching Video";
    }
    return @"User Submissions";
}

- (NSString *)tableView:(UITableView *)tableView titleForFooterInSection:(NSInteger)section {
    if (self.tag.teachingVideo && section == 0) {
        return  nil;
    }
    if (self.tag.videos.count > 0) {
        return nil;
    }
    return @"No quartet recordings have been posted for this tag yet.";
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    if (self.tag.teachingVideo) {
        return 2;
    }
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    if (self.tag.teachingVideo && section == 0) {
        return 1;
    }
    return self.tag.videos.count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault
                                                   reuseIdentifier:nil];
    cell.backgroundColor = UIColor.clearColor;
    cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;

    UIImageView *thumb = [[UIImageView alloc] init];
    thumb.translatesAutoresizingMaskIntoConstraints = NO;
    thumb.backgroundColor = [UIColor secondarySystemFillColor];
    thumb.contentMode = UIViewContentModeScaleAspectFill;
    thumb.clipsToBounds = YES;
    thumb.layer.cornerRadius = 6;
    thumb.layer.cornerCurve = kCACornerCurveContinuous;
    thumb.isAccessibilityElement = NO;
    [NSLayoutConstraint activateConstraints:@[
        [thumb.widthAnchor constraintEqualToConstant:96],
        [thumb.heightAnchor constraintEqualToAnchor:thumb.widthAnchor multiplier:9.0 / 16.0]
    ]];

    UIImageView *playBadge = [[UIImageView alloc]
        initWithImage:[UIImage systemImageNamed:@"play.circle.fill"]];
    playBadge.translatesAutoresizingMaskIntoConstraints = NO;
    playBadge.tintColor = [UIColor whiteColor];
    playBadge.preferredSymbolConfiguration =
        [UIImageSymbolConfiguration configurationWithPointSize:24];
    [thumb addSubview:playBadge];
    [NSLayoutConstraint activateConstraints:@[
        [playBadge.centerXAnchor constraintEqualToAnchor:thumb.centerXAnchor],
        [playBadge.centerYAnchor constraintEqualToAnchor:thumb.centerYAnchor]
    ]];

    UIStackView *facts = [[UIStackView alloc] init];
    facts.axis = UILayoutConstraintAxisVertical;
    facts.spacing = TMTheme.spaceXS;

    NSURL *thumbnail = nil;
    NSMutableArray<NSString *> *spoken = [NSMutableArray array];

    if (self.tag.teachingVideo && indexPath.section == 0) {
        UILabel *heading = [[UILabel alloc] init];
        heading.text = @"Teaching video";
        heading.font = [TMTheme fontWithStyle:UIFontTextStyleBody weight:UIFontWeightSemibold];
        heading.adjustsFontForContentSizeCategory = YES;
        heading.textColor = [TMTheme primaryText];
        heading.numberOfLines = 0;
        [facts addArrangedSubview:heading];
        [spoken addObject:@"Teaching video"];

        if (self.tag.teacher.length > 0) {
            [facts addArrangedSubview:[self makeFactLabel:
                [NSString stringWithFormat:@"Taught by %@", self.tag.teacher]]];
            [spoken addObject:[NSString stringWithFormat:@"taught by %@", self.tag.teacher]];
        }
        thumbnail = [NSURL URLWithString:[NSString stringWithFormat:@"https://img.youtube.com/vi/%@/2.jpg", self.tag.teachingVideo]];
    } else {
        DPVideo *video = self.tag.videos[indexPath.row];
        thumbnail = [NSURL URLWithString:[NSString stringWithFormat:@"https://img.youtube.com/vi/%@/2.jpg", video.youTubeCode]];

        UILabel *heading = [[UILabel alloc] init];
        heading.text = video.sungBy.length > 0 ? video.sungBy : @"Performance";
        heading.font = [TMTheme fontWithStyle:UIFontTextStyleBody weight:UIFontWeightSemibold];
        heading.adjustsFontForContentSizeCategory = YES;
        heading.textColor = [TMTheme primaryText];
        heading.numberOfLines = 0;
        [facts addArrangedSubview:heading];
        [spoken addObject:heading.text];

        NSMutableArray<NSString *> *details = [NSMutableArray array];
        if (video.sungKey.length > 0) {
            [details addObject:[NSString stringWithFormat:@"Key of %@", video.sungKey]];
        }
        if (video.posted) {
            NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
            formatter.dateStyle = NSDateFormatterMediumStyle;
            formatter.timeStyle = NSDateFormatterNoStyle;
            [details addObject:[formatter stringFromDate:video.posted]];
        }
        if (video.isMultitrack) {
            [details addObject:@"Multitrack"];
        }
        if (details.count > 0) {
            NSString *line = [details componentsJoinedByString:@" · "];
            [facts addArrangedSubview:[self makeFactLabel:line]];
            [spoken addObject:line];
        }
    }

    UIStackView *row = [[UIStackView alloc] initWithArrangedSubviews:@[thumb, facts]];
    row.axis = UILayoutConstraintAxisHorizontal;
    row.alignment = UIStackViewAlignmentCenter;
    row.spacing = TMTheme.spaceM;
    row.translatesAutoresizingMaskIntoConstraints = NO;

    [cell.contentView addSubview:row];
    [NSLayoutConstraint activateConstraints:@[
        [row.leadingAnchor constraintEqualToAnchor:cell.contentView.layoutMarginsGuide.leadingAnchor],
        [row.trailingAnchor constraintEqualToAnchor:cell.contentView.layoutMarginsGuide.trailingAnchor],
        [row.topAnchor constraintEqualToAnchor:cell.contentView.topAnchor constant:TMTheme.spaceM],
        [row.bottomAnchor constraintEqualToAnchor:cell.contentView.bottomAnchor
                                         constant:-TMTheme.spaceM]
    ]];

    cell.isAccessibilityElement = YES;
    cell.accessibilityLabel = [spoken componentsJoinedByString:@", "];
    cell.accessibilityHint = @"Opens the video on YouTube";
    cell.accessibilityTraits = UIAccessibilityTraitButton;

    [self loadThumbnail:thumbnail into:thumb];

    return cell;
}

- (UILabel *)makeFactLabel:(NSString *)text {
    UILabel *label = [[UILabel alloc] init];
    label.text = text;
    label.font = [TMTheme metadataFont];
    label.adjustsFontForContentSizeCategory = YES;
    label.textColor = [TMTheme secondaryText];
    label.numberOfLines = 0;
    return label;
}

- (void)loadThumbnail:(NSURL *)thumbnail into:(UIImageView *)thumb {
    if (!thumbnail) {
        return;
    }
    NSString *thumbnailKey = [DPFileCache keyForURL:thumbnail];
    if ([[NSFileManager defaultManager] fileExistsAtPath:[DPFileCache pathForKey:thumbnailKey]]) {
        thumb.image = [UIImage imageWithData:[DPFileCache readDataForKey:thumbnailKey]];
        return;
    }
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        NSData *data = [NSData dataWithContentsOfURL:thumbnail];
        if (!data) {
            return;
        }
        [DPFileCache writeData:data forKey:thumbnailKey];
        dispatch_async(dispatch_get_main_queue(), ^{
            thumb.image = [UIImage imageWithData:data];
        });
    });
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    NSString *youTubeCode = nil;
    if (self.tag.teachingVideo && indexPath.section == 0) {
        youTubeCode = self.tag.teachingVideo;
    } else {
        DPVideo *video = self.tag.videos[indexPath.row];
        youTubeCode = video.youTubeCode;
    }
    NSURL *youTubeURL = [NSURL URLWithString:[NSString stringWithFormat:@"https://www.youtube.com/watch?v=%@", youTubeCode]];
    [[UIApplication sharedApplication] openURL:youTubeURL options:@{} completionHandler:nil];
}

+ (UIImage *)onImage {
    static UIImage *image;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        image = [UIImage systemImageNamed:@"checkmark.circle.fill"];
    });
    return image;
}

+ (UIImage *)offImage {
    static UIImage *image;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        image = [UIImage systemImageNamed:@"circle"];
    });
    return image;
}

@end
