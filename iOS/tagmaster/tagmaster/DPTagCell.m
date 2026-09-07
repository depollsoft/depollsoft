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
@property (nonatomic, strong) DPBusyIndicator *busyIndicator;

@end

@implementation DPTagCell

@synthesize tagInstance, title, aka, details, rootView, busyIndicator;

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

        self.rootView = [[UIStackView alloc] initWithArrangedSubviews:@[
            self.title, self.aka, self.details
        ]];
        UIStackView *stack = (UIStackView *)self.rootView;
        stack.axis = UILayoutConstraintAxisVertical;
        stack.spacing = TMTheme.spaceXS;
        stack.translatesAutoresizingMaskIntoConstraints = NO;

        self.busyIndicator = [[DPBusyIndicator alloc] init];
        // The legacy grid overlay requests enormous star-row heights even when
        // hidden. A native spinner must not dictate the self-sizing row height.
        UIActivityIndicatorView *spinner = [[UIActivityIndicatorView alloc]
            initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleMedium];
        spinner.color = [TMTheme tint];
        [spinner startAnimating];
        UIView *loadingOverlay = [[UIView alloc] init];
        spinner.translatesAutoresizingMaskIntoConstraints = NO;
        [loadingOverlay addSubview:spinner];
        [NSLayoutConstraint activateConstraints:@[
            [spinner.centerXAnchor constraintEqualToAnchor:loadingOverlay.centerXAnchor],
            [spinner.centerYAnchor constraintEqualToAnchor:loadingOverlay.centerYAnchor]
        ]];
        self.busyIndicator.overlay = loadingOverlay;
        self.busyIndicator.child = self.rootView;
        self.busyIndicator.translatesAutoresizingMaskIntoConstraints = NO;

        [self.contentView addSubview:self.busyIndicator];
        [NSLayoutConstraint activateConstraints:@[
            [self.busyIndicator.leadingAnchor
                constraintEqualToAnchor:self.contentView.layoutMarginsGuide.leadingAnchor],
            [self.busyIndicator.trailingAnchor
                constraintEqualToAnchor:self.contentView.layoutMarginsGuide.trailingAnchor],
            [self.busyIndicator.topAnchor constraintEqualToAnchor:self.contentView.topAnchor
                                                         constant:TMTheme.spaceS],
            [self.busyIndicator.bottomAnchor constraintEqualToAnchor:self.contentView.bottomAnchor
                                                            constant:-TMTheme.spaceS],
            [self.contentView.heightAnchor
                constraintGreaterThanOrEqualToConstant:60]
        ]];
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
    int requestedId = self.tagId;
    if (!refresh) {
        DPTag *cached = [DPTag loadFromCache:requestedId];
        if (cached) {
            self.tagInstance = cached;
            return;
        }
    }
    self.tagInstance = nil;
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        DPTag *loaded = nil;
        @try { loaded = [DPTag loadTagById:requestedId refresh:refresh]; }
        @catch (NSException *exception) { /* The row offers recovery through its tag page. */ }
        dispatch_async(dispatch_get_main_queue(), ^{
            if (self.tagId != requestedId) { return; }
            [self.busyIndicator clearBusyCount];
            if (loaded) {
                self.tagInstance = loaded;
            } else {
                self.title.text = [NSString stringWithFormat:@"Tag %d", requestedId];
                self.details.text = @"Couldn't load tag. Open to retry.";
                self.accessibilityLabel = [NSString stringWithFormat:@"%@. %@", self.title.text, self.details.text];
            }
            UITableView *table = (UITableView *)self.superview;
            while (table && ![table isKindOfClass:[UITableView class]]) {
                table = (UITableView *)table.superview;
            }
            // Recalculate without dequeuing a failed row and starting another request.
            if ([table indexPathForCell:self]) {
                [table beginUpdates];
                [table endUpdates];
            }
        });
    });
}

- (void)setTagInstance:(DPTag *)newTag {
    tagInstance = newTag;
    if (newTag) { _tagId = newTag.tagId; }
    self.title.text = newTag.title ?: [NSString stringWithFormat:@"Tag %d", self.tagId];
    NSString *alternate = [newTag.alternativeTitle stringByTrimmingCharactersInSet:NSCharacterSet.whitespaceAndNewlineCharacterSet];
    NSString *primary = [newTag.title stringByTrimmingCharactersInSet:NSCharacterSet.whitespaceAndNewlineCharacterSet];
    BOOL showAlternate = alternate.length > 0 && [alternate caseInsensitiveCompare:primary ?: @""] != NSOrderedSame;
    self.aka.text = showAlternate ? [@"a.k.a. " stringByAppendingString:alternate] : nil;
    self.aka.hidden = !showAlternate;

    BOOL sheets = newTag.sheetMusicUri != nil;
    BOOL tracks = newTag.tracks.count > 0;
    NSMutableArray<NSString *> *support = [NSMutableArray arrayWithObject:
        [NSString stringWithFormat:@"ID %d", self.tagId]];
    if (newTag) {
        if (sheets) { [support addObject:@"Sheet music"]; }
        if (tracks) { [support addObject:@"Learning tracks"]; }
        if (!sheets && !tracks) { [support addObject:@"No materials"]; }
    } else {
        [support addObject:@"Loading…"];
    }
    self.details.text = [support componentsJoinedByString:@" · "];
    self.accessibilityIdentifier = [NSString stringWithFormat:@"tag.%d", self.tagId];
    self.isAccessibilityElement = YES;
    self.accessibilityLabel = [[@[
        self.title.text ?: @"", self.aka.text ?: @"",
        [NSString stringWithFormat:@"ID %d", self.tagId],
        newTag ? (sheets ? @"Has sheet music" : @"No sheet music") : @"Loading",
        newTag ? (tracks ? @"Has learning tracks" : @"No learning tracks") : @""
    ] filteredArrayUsingPredicate:[NSPredicate predicateWithFormat:@"length > 0"]] componentsJoinedByString:@". "];
    self.accessibilityTraits = UIAccessibilityTraitButton;
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
