//
//  DPTagCell.m
//  tagmaster
//
//  Created by David Poll on 8/15/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
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
@property (nonatomic, strong) DPBusyIndicator *busyIndicator;

@end

@implementation DPTagCell

@synthesize tagInstance, title, aka, details, hasLearningTracks, hasSheetMusic, rootView, busyIndicator;

- (id)initWithStyle:(UITableViewCellStyle)style reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
        self.backgroundColor = [UIColor clearColor];
        // Every tag row navigates to the tag, so it carries the standard disclosure chevron.
        self.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
        self.rootView = [[UIView alloc] init];
        self.rootView.translatesAutoresizingMaskIntoConstraints = NO;
        
        self.busyIndicator = [[DPBusyIndicator alloc] init];
        // The shared grid overlay has expanding star dimensions, which prevent self-sizing rows.
        UILabel *loading = [[UILabel alloc] init];
        loading.text = @"Loading tag…";
        loading.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
        loading.adjustsFontForContentSizeCategory = YES;
        loading.textAlignment = NSTextAlignmentCenter;
        loading.backgroundColor = [UIColor systemBackgroundColor];
        self.busyIndicator.overlay = loading;
        self.busyIndicator.child = self.rootView;
        self.busyIndicator.translatesAutoresizingMaskIntoConstraints = NO;
        
        [self.contentView addSubview:self.busyIndicator];
        [self.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[busyIndicator]|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:NSDictionaryOfVariableBindings(busyIndicator)]];
        [self.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[busyIndicator]|"
                                                                                 options:0
                                                                                 metrics:nil
                                                                                   views:NSDictionaryOfVariableBindings(busyIndicator)]];
        
        self.title = [[UILabel alloc] init];
        self.aka = [[UILabel alloc] init];
        self.details = [[UILabel alloc] init];
        self.title.font = [UIFont preferredFontForTextStyle:UIFontTextStyleHeadline];
        self.aka.font = [UIFont preferredFontForTextStyle:UIFontTextStyleFootnote];
        self.details.font = [UIFont preferredFontForTextStyle:UIFontTextStyleFootnote];
        self.aka.textColor = [UIColor labelColor];
        self.details.textColor = [UIColor labelColor];
        UILabel *hasSheetMusicLabel = [[UILabel alloc] init];
        hasSheetMusicLabel.text = @"Sheet Music";
        UILabel *hasLearningTracksLabel = [[UILabel alloc] init];
        hasLearningTracksLabel.text = @"Learning Tracks";
        for (UILabel *label in @[hasSheetMusicLabel, hasLearningTracksLabel]) {
            label.font = [UIFont preferredFontForTextStyle:UIFontTextStyleCaption1];
            label.textColor = [UIColor labelColor];
        }
        for (UILabel *label in @[self.title, self.aka, self.details, hasSheetMusicLabel, hasLearningTracksLabel]) {
            label.adjustsFontForContentSizeCategory = YES;
            label.numberOfLines = 0;
            label.isAccessibilityElement = NO;
        }
        self.hasSheetMusic = [[UIImageView alloc] initWithImage:[DPTagCell offImage]];
        self.hasLearningTracks = [[UIImageView alloc] initWithImage:[DPTagCell offImage]];
        for (UIImageView *image in @[self.hasSheetMusic, self.hasLearningTracks]) {
            image.contentMode = UIViewContentModeScaleAspectFit;
            [image.widthAnchor constraintEqualToConstant:20].active = YES;
            [image.heightAnchor constraintEqualToConstant:20].active = YES;
        }
        UIStackView *sheet = [[UIStackView alloc] initWithArrangedSubviews:@[self.hasSheetMusic, hasSheetMusicLabel]];
        UIStackView *tracks = [[UIStackView alloc] initWithArrangedSubviews:@[self.hasLearningTracks, hasLearningTracksLabel]];
        sheet.spacing = tracks.spacing = 8;
        sheet.alignment = tracks.alignment = UIStackViewAlignmentCenter;
        UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[self.title, self.aka, self.details, sheet, tracks]];
        stack.axis = UILayoutConstraintAxisVertical;
        stack.spacing = 4;
        stack.translatesAutoresizingMaskIntoConstraints = NO;
        [self.rootView addSubview:stack];
        [NSLayoutConstraint activateConstraints:@[
            [stack.leadingAnchor constraintEqualToAnchor:self.rootView.leadingAnchor constant:16],
            [stack.trailingAnchor constraintEqualToAnchor:self.rootView.trailingAnchor constant:-16],
            [stack.topAnchor constraintEqualToAnchor:self.rootView.topAnchor constant:12],
            [stack.bottomAnchor constraintEqualToAnchor:self.rootView.bottomAnchor constant:-12]
        ]];
        self.isAccessibilityElement = YES;
        self.accessibilityTraits = UIAccessibilityTraitButton;
    }
    return self;
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
                if (tagId != self.tagId) return;
                [self.busyIndicator clearBusyCount];
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
                if (tagId == self.tagId) {
                    [self.busyIndicator clearBusyCount];
                    self.tagInstance = nil;
                }
            });
        }
    });
}

+ (NSDateFormatter *)dateFormatter {
    static NSDateFormatter *formatter;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        formatter = [[NSDateFormatter alloc] init];
        formatter.dateFormat = @"MM/dd/yy";
        formatter.locale = [NSLocale localeWithLocaleIdentifier:@"en_US"];
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
    self.aka.text = self.tagInstance.alternativeTitle ? [@"a.k.a. " stringByAppendingString:self.tagInstance.alternativeTitle] : nil;
    NSString *detailsString = nil;
    if (tagInstance) {
        detailsString = [@"Posted: " stringByAppendingString:[formatter stringFromDate:tagInstance.posted] ?: @"Unknown"];
        if (tagInstance.rating != 0) {
            detailsString = [[NSString stringWithFormat:@"Rating: %1.2f ", tagInstance.rating] stringByAppendingString:detailsString];
        }
        if (tagInstance.downloadCount != 0) {
            detailsString = [detailsString stringByAppendingFormat:@" DLs: %d", tagInstance.downloadCount];
        }
        detailsString = [[NSString stringWithFormat:@"ID: %d ", tagInstance.tagId] stringByAppendingString:detailsString];
    }
    self.aka.hidden = self.aka.text.length == 0;
    self.details.text = detailsString ?: @"Open tag to load details";
    NSDateFormatter *spokenDate = [[NSDateFormatter alloc] init];
    spokenDate.dateStyle = NSDateFormatterMediumStyle;
    self.accessibilityLabel = tagInstance ? [NSString stringWithFormat:@"%@. %@. Tag ID %d. Rating %.2f out of 5. Posted %@. %d downloads. Sheet music %@. Learning tracks %@.", self.title.text, self.aka.text ?: @"", tagInstance.tagId, tagInstance.rating, tagInstance.posted ? [spokenDate stringFromDate:tagInstance.posted] : @"unknown", tagInstance.downloadCount, tagInstance.sheetMusicUri ? @"available" : @"unavailable", tagInstance.tracks.count ? @"available" : @"unavailable"] : [NSString stringWithFormat:@"Tag %d. Open to load details.", self.tagId];
    self.hasSheetMusic.tintColor = self.tagInstance.sheetMusicUri ? [UIColor systemGreenColor] : [UIColor secondaryLabelColor];
    self.hasLearningTracks.tintColor = self.tagInstance.tracks.count ? [UIColor systemGreenColor] : [UIColor secondaryLabelColor];
    self.hasLearningTracks.image = self.tagInstance.tracks.count > 0 ? [DPTagCell onImage] : [DPTagCell offImage];
    self.hasSheetMusic.image = self.tagInstance.sheetMusicUri ? [DPTagCell onImage] : [DPTagCell offImage];
}

- (CGFloat)calculatedHeight {
    if (tagInstance.alternativeTitle) {
        return [DPTagCell withAkaHeight];
    } else {
        return [DPTagCell withoutAkaHeight];
    }
}

+ (CGFloat)withAkaHeight {
    static CGFloat height;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        DPTag *tag = [[DPTag alloc] init];
        tag.title = @"A";
        tag.alternativeTitle = @"A";
        tag.posted = [NSDate date];
        DPTagCell *cell = [[DPTagCell alloc] init];
        cell.tagInstance = tag;
        height = [cell.rootView systemLayoutSizeFittingSize:CGSizeZero].height;
    });
    return height;
}

+ (CGFloat)withoutAkaHeight {
    static CGFloat height;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        DPTag *tag = [[DPTag alloc] init];
        tag.title = @"A";
        tag.posted = [NSDate date];
        DPTagCell *cell = [[DPTagCell alloc] init];
        cell.tagInstance = tag;
        height = [cell.rootView systemLayoutSizeFittingSize:CGSizeZero].height;
    });
    return height;
}

+ (CGFloat)tagHeight:(DPTag *)tag {
    if (tag.alternativeTitle) {
        return [self withAkaHeight];
    } else {
        return [self withoutAkaHeight];
    }
}

@end
