//
//  DPTagCell.m
//  tagmaster
//
//  Created by David Poll on 8/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//
//  One tag as a singer scans it: what it's called, what it's known as, and
//  whether there is sheet music and a learning track to work from.
//

#import "DPTagCell.h"
#import "DPBusyIndicator.h"
#import "DPAppDelegate.h"
#import "tagmaster-Swift.h"

@interface DPTagCell ()

@property (nonatomic, strong) UILabel *title;
@property (nonatomic, strong) UILabel *aka;
@property (nonatomic, strong) UILabel *details;
@property (nonatomic, strong) UIImageView *hasSheetMusic;
@property (nonatomic, strong) UIImageView *hasLearningTracks;
@property (nonatomic, strong) UILabel *sheetMusicLabel;
@property (nonatomic, strong) UILabel *learningTracksLabel;
@property (nonatomic, strong) DPBusyIndicator *busyIndicator;

@end

@implementation DPTagCell

@synthesize tagInstance, title, aka, details, hasLearningTracks, hasSheetMusic, rootView, busyIndicator;

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        self.backgroundColor = [UIColor clearColor];
        self.accessoryType = UITableViewCellAccessoryDisclosureIndicator;

        self.title = [[UILabel alloc] init];
        self.title.font = [TMTheme fontWithStyle:UIFontTextStyleHeadline weight:UIFontWeightSemibold];
        self.title.adjustsFontForContentSizeCategory = YES;
        self.title.textColor = [TMTheme primaryText];
        self.title.numberOfLines = 0;

        self.aka = [[UILabel alloc] init];
        self.aka.font = [UIFont preferredFontForTextStyle:UIFontTextStyleSubheadline];
        self.aka.adjustsFontForContentSizeCategory = YES;
        self.aka.textColor = [TMTheme secondaryText];
        self.aka.numberOfLines = 0;

        self.details = [[UILabel alloc] init];
        self.details.font = [TMTheme metadataFont];
        self.details.adjustsFontForContentSizeCategory = YES;
        self.details.textColor = [TMTheme secondaryText];
        self.details.numberOfLines = 0;

        self.sheetMusicLabel = [DPTagCell makeMarkerLabel:@"Sheet music"];
        self.learningTracksLabel = [DPTagCell makeMarkerLabel:@"Learning tracks"];
        self.hasSheetMusic = [DPTagCell makeMarkerImageView];
        self.hasLearningTracks = [DPTagCell makeMarkerImageView];

        UIStackView *sheetMusicGroup =
            [DPTagCell makeMarkerGroup:self.hasSheetMusic label:self.sheetMusicLabel];
        UIStackView *tracksGroup =
            [DPTagCell makeMarkerGroup:self.hasLearningTracks label:self.learningTracksLabel];

        UIStackView *markers =
            [[UIStackView alloc] initWithArrangedSubviews:@[sheetMusicGroup, tracksGroup]];
        markers.axis = UILayoutConstraintAxisHorizontal;
        markers.spacing = TMTheme.spaceL;
        markers.alignment = UIStackViewAlignmentFirstBaseline;

        self.rootView = [[UIStackView alloc] initWithArrangedSubviews:@[
            self.title, self.aka, self.details, markers
        ]];
        UIStackView *stack = (UIStackView *)self.rootView;
        stack.axis = UILayoutConstraintAxisVertical;
        stack.spacing = TMTheme.spaceXS;
        [stack setCustomSpacing:TMTheme.spaceS afterView:self.details];
        stack.translatesAutoresizingMaskIntoConstraints = NO;

        self.busyIndicator = [[DPBusyIndicator alloc] init];
        self.busyIndicator.child = self.rootView;
        self.busyIndicator.translatesAutoresizingMaskIntoConstraints = NO;

        [self.contentView addSubview:self.busyIndicator];
        [NSLayoutConstraint activateConstraints:@[
            [self.busyIndicator.leadingAnchor
                constraintEqualToAnchor:self.contentView.layoutMarginsGuide.leadingAnchor],
            [self.busyIndicator.trailingAnchor
                constraintEqualToAnchor:self.contentView.layoutMarginsGuide.trailingAnchor],
            [self.busyIndicator.topAnchor constraintEqualToAnchor:self.contentView.topAnchor
                                                         constant:TMTheme.spaceM],
            [self.busyIndicator.bottomAnchor constraintEqualToAnchor:self.contentView.bottomAnchor
                                                            constant:-TMTheme.spaceM],
            [self.contentView.heightAnchor
                constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget]
        ]];
    }
    return self;
}

+ (UILabel *)makeMarkerLabel:(NSString *)text {
    UILabel *label = [[UILabel alloc] init];
    label.text = text;
    label.font = [TMTheme metadataFont];
    label.adjustsFontForContentSizeCategory = YES;
    label.numberOfLines = 0;
    return label;
}

+ (UIImageView *)makeMarkerImageView {
    UIImageView *view = [[UIImageView alloc] init];
    view.contentMode = UIViewContentModeScaleAspectFit;
    view.preferredSymbolConfiguration =
        [UIImageSymbolConfiguration configurationWithTextStyle:UIFontTextStyleFootnote];
    [view setContentHuggingPriority:UILayoutPriorityRequired
                            forAxis:UILayoutConstraintAxisHorizontal];
    return view;
}

+ (UIStackView *)makeMarkerGroup:(UIImageView *)image label:(UILabel *)label {
    UIStackView *group = [[UIStackView alloc] initWithArrangedSubviews:@[image, label]];
    group.axis = UILayoutConstraintAxisHorizontal;
    group.spacing = TMTheme.spaceXS;
    group.alignment = UIStackViewAlignmentCenter;
    group.isAccessibilityElement = NO;
    return group;
}

- (void)setSelected:(BOOL)selected animated:(BOOL)animated
{
    [super setSelected:selected animated:animated];

    // Configure the view for the selected state
}

- (void)setTagId:(int)tId {
    _tagId = tId;
    [self.busyIndicator clearBusyCount];
    [self loadTag:NO];
}

- (void)loadTag:(BOOL)refresh {
    int tagId = self.tagId;
    if (!refresh) {
        DPTag *t = [DPTag loadFromCache:tagId];
        if (t) {
            self.tagInstance = t;
            return;
        }
    }
    self.tagInstance = nil;
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            DPTag *t = [DPTag loadTagById:tagId refresh:refresh];
            dispatch_async(dispatch_get_main_queue(), ^{
                if (t.tagId == self.tagId) {
                    self.tagInstance = t;
                    UITableView *tableView = (UITableView *)self.superview;
                    while (tableView && ![tableView isKindOfClass:[UITableView class]]) {
                        tableView = (UITableView *)tableView.superview;
                    }
                    NSIndexPath *indexPath = [tableView indexPathForCell:self];
                    if (indexPath) {
                        [tableView reloadRowsAtIndexPaths:@[indexPath]
                                         withRowAnimation:UITableViewRowAnimationAutomatic];
                    }
                    if (self.busyIndicator.busyCount > 0) {
                        [self.busyIndicator decrementBusyCount];
                    }
                }
            });
        }
        @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
            });
        }
    });
}

+ (NSDateFormatter *)dateFormatter {
    static NSDateFormatter *formatter;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        formatter = [[NSDateFormatter alloc] init];
        formatter.dateStyle = NSDateFormatterMediumStyle;
        formatter.timeStyle = NSDateFormatterNoStyle;
    });
    return formatter;
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

- (void)setTagInstance:(DPTag *)newTag {
    NSDateFormatter *formatter = [DPTagCell dateFormatter];
    tagInstance = newTag;
    self.title.text = self.tagInstance.title ?: @"Tag";

    self.aka.text = self.tagInstance.alternativeTitle
        ? [@"a.k.a. " stringByAppendingString:self.tagInstance.alternativeTitle]
        : nil;
    self.aka.hidden = self.tagInstance.alternativeTitle == nil;

    NSMutableArray<NSString *> *facts = [NSMutableArray array];
    if (tagInstance) {
        [facts addObject:[NSString stringWithFormat:@"ID %d", tagInstance.tagId]];
        if (tagInstance.rating != 0) {
            [facts addObject:[NSString stringWithFormat:@"★ %1.2f", tagInstance.rating]];
        }
        if (tagInstance.downloadCount != 0) {
            [facts addObject:[NSString stringWithFormat:@"%d downloads", tagInstance.downloadCount]];
        }
        NSString *posted = tagInstance.posted ? [formatter stringFromDate:tagInstance.posted] : nil;
        [facts addObject:posted ? [@"Posted " stringByAppendingString:posted] : @"Posted date unknown"];
    }
    self.details.text = facts.count > 0 ? [facts componentsJoinedByString:@" · "] : @"Loading…";

    BOOL tracks = self.tagInstance.tracks.count > 0;
    BOOL sheets = self.tagInstance.sheetMusicUri != nil;
    [self applyMarker:self.hasLearningTracks label:self.learningTracksLabel present:tracks];
    [self applyMarker:self.hasSheetMusic label:self.sheetMusicLabel present:sheets];

    self.isAccessibilityElement = YES;
    self.accessibilityLabel = [@[
        self.title.text ?: @"",
        self.aka.text ?: @"",
        self.details.text ?: @"",
        sheets ? @"Has sheet music" : @"No sheet music",
        tracks ? @"Has learning tracks" : @"No learning tracks"
    ] componentsJoinedByString:@". "];
    self.accessibilityTraits = UIAccessibilityTraitButton;
}

/// Presence is carried by the symbol shape and the wording, not by colour
/// alone.
- (void)applyMarker:(UIImageView *)view label:(UILabel *)label present:(BOOL)present {
    view.image = present ? [DPTagCell onImage] : [DPTagCell offImage];
    view.tintColor = present ? [TMTheme affirmative] : [TMTheme secondaryText];
    label.textColor = present ? [TMTheme primaryText] : [TMTheme secondaryText];
}

- (CGFloat)calculatedHeight {
    return UITableViewAutomaticDimension;
}

+ (CGFloat)withAkaHeight {
    return UITableViewAutomaticDimension;
}

+ (CGFloat)withoutAkaHeight {
    return UITableViewAutomaticDimension;
}

/// Rows size themselves from their content now, so every caller gets the
/// automatic dimension regardless of the tag passed in.
+ (CGFloat)tagHeight:(DPTag *)tag {
    return UITableViewAutomaticDimension;
}

@end
