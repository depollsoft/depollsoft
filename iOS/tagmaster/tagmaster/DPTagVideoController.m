//
//  DPTagVideoController.m
//  tagmaster
//
//  Created by David Poll on 10/5/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagVideoController.h"
#import "UIView+DPUtils.h"
#import "DPFileCache.h"
#import <SafariServices/SafariServices.h>

@interface DPTagVideoController () <UITableViewDelegate, UITableViewDataSource>

@property (nonatomic, strong) UITableView *tableView;

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
	self.tableView = [[UITableView alloc] initWithFrame:CGRectNull style:UITableViewStyleGrouped];
    self.tableView.dataSource = self;
    self.tableView.delegate = self;
    self.tableView.translatesAutoresizingMaskIntoConstraints = NO;
    self.tableView.backgroundColor = [UIColor clearColor];
    
    [self.view addSubview:self.tableView];
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_tableView]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_tableView)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[_tableView]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(_tableView)]];
    
    [self refreshView];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)refreshView {
    [self.tableView reloadData];
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
        return @"Videos open on YouTube inside Tag Master.";
    }
    return @"Sorry, this tag does not have any videos associated with it.";
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
    UIImageView *thumb = [[UIImageView alloc] init];
    thumb.backgroundColor = [UIColor secondarySystemFillColor];
    thumb.contentMode = UIViewContentModeScaleAspectFill;
    thumb.clipsToBounds = YES;
    thumb.layer.cornerRadius = 6;
    thumb.layer.cornerCurve = kCACornerCurveContinuous;
    thumb.isAccessibilityElement = NO;
    [thumb.widthAnchor constraintEqualToConstant:80].active = YES;
    [thumb.heightAnchor constraintEqualToConstant:60].active = YES;
    UIStackView *metadata = [[UIStackView alloc] init];
    metadata.axis = UILayoutConstraintAxisVertical;
    metadata.spacing = 4;
    NSMutableArray<NSString *> *lines = [NSMutableArray array];
    NSURL *thumbnail = nil;
    NSString *spoken = nil;
    if (self.tag.teachingVideo && indexPath.section == 0) {
        [lines addObject:[NSString stringWithFormat:@"Teacher: %@", self.tag.teacher ?: @"Unknown"]];
        thumbnail = [NSURL URLWithString:[NSString stringWithFormat:@"https://img.youtube.com/vi/%@/2.jpg", self.tag.teachingVideo]];
        spoken = [NSString stringWithFormat:@"Teaching video by %@", self.tag.teacher ?: @"an unknown teacher"];
    } else {
        DPVideo *video = self.tag.videos[indexPath.row];
        thumbnail = [NSURL URLWithString:[NSString stringWithFormat:@"https://img.youtube.com/vi/%@/2.jpg", video.youTubeCode]];
        if (video.sungBy) [lines addObject:[NSString stringWithFormat:@"Sung By: %@", video.sungBy]];
        if (video.sungKey) [lines addObject:[NSString stringWithFormat:@"Key: %@", video.sungKey]];
        NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
        formatter.dateStyle = NSDateFormatterLongStyle;
        NSString *posted = video.posted ? [formatter stringFromDate:video.posted] : @"Unknown";
        [lines addObject:[NSString stringWithFormat:@"Posted: %@", posted]];
        [lines addObject:[NSString stringWithFormat:@"Multitrack: %@", video.isMultitrack ? @"Yes" : @"No"]];
        spoken = [NSString stringWithFormat:@"Video sung by %@%@. Posted %@. %@.",
                  video.sungBy ?: @"an unknown group",
                  video.sungKey ? [NSString stringWithFormat:@" in %@", video.sungKey] : @"",
                  posted,
                  video.isMultitrack ? @"Multitrack" : @"Single track"];
    }
    // Who sang it is the row's title; key, date and multitrack are supporting metadata.
    [lines enumerateObjectsUsingBlock:^(NSString *line, NSUInteger index, BOOL *stop) {
        UILabel *label = [self makeBodyLabel];
        label.text = line;
        label.font = [UIFont preferredFontForTextStyle:index == 0 ? UIFontTextStyleHeadline : UIFontTextStyleSubheadline];
        label.textColor = index == 0 ? [UIColor labelColor] : [UIColor secondaryLabelColor];
        [metadata addArrangedSubview:label];
    }];
    UIStackView *grid = [[UIStackView alloc] initWithArrangedSubviews:@[thumb, metadata]];
    grid.alignment = UIStackViewAlignmentTop;
    grid.spacing = 12;
    grid.layoutMarginsRelativeArrangement = YES;
    grid.directionalLayoutMargins = NSDirectionalEdgeInsetsMake(12, 16, 12, 16);
    grid.translatesAutoresizingMaskIntoConstraints = NO;

    NSString *thumbnailKey = [DPFileCache keyForURL:thumbnail];
    if ([[NSFileManager defaultManager] fileExistsAtPath:[DPFileCache pathForKey:thumbnailKey]]) {
        thumb.image = [UIImage imageWithData:[DPFileCache readDataForKey:thumbnailKey]];
    } else {
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
            NSData *data = [DPRemoteLocation dataWithContentsOfURL:thumbnail error:nil];
            if (data.length > 0) [DPFileCache writeData:data forKey:thumbnailKey];
            dispatch_async(dispatch_get_main_queue(), ^{
                thumb.image = [UIImage imageWithData:data];
            });
        });
    }
    
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:nil];
    cell.backgroundColor = [UIColor clearColor];
    cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
    cell.isAccessibilityElement = YES;
    cell.accessibilityLabel = spoken;
    cell.accessibilityHint = @"Opens the video";
    cell.accessibilityTraits = UIAccessibilityTraitButton;
    [cell.contentView addSubview:grid];
    [cell.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[grid]|"
                                                                             options:0
                                                                             metrics:nil
                                                                               views:NSDictionaryOfVariableBindings(grid)]];
    [cell.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[grid]|"
                                                                             options:0
                                                                             metrics:nil
                                                                               views:NSDictionaryOfVariableBindings(grid)]];

    return cell;
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewAutomaticDimension;
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
    // Stay in the app; the in-app browser still hands off to the YouTube app when installed.
    SFSafariViewController *browser = [[SFSafariViewController alloc] initWithURL:youTubeURL];
    browser.preferredControlTintColor = self.view.tintColor;
    [self presentViewController:browser animated:YES completion:nil];
}

@end
