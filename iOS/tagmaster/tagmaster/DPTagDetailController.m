//
//  DPTagDetailController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  Provenance: who posted, arranged, and sang this tag, and where it lives on
//  BarbershopTags.com. Attribution is part of the product, so it reads as
//  content rather than as small print.
//

#import "DPTagDetailController.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "tagmaster-Swift.h"

@interface DPTagDetailController ()

@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UILabel *tagIdLabel;
@property (nonatomic, strong) UILabel *lastRefreshedLabel;
@property (nonatomic, strong) UILabel *downloadsLabel;
@property (nonatomic, strong) UIButton *linkButton;
@property (nonatomic, strong) UIStackView *postedByValue;
@property (nonatomic, strong) UILabel *postedLabel;
@property (nonatomic, strong) UIStackView *arrangedByValue;
@property (nonatomic, strong) UILabel *yearArrangedLabel;
@property (nonatomic, strong) UIStackView *sungByValue;
@property (nonatomic, strong) UILabel *yearSungLabel;

@property (nonatomic, strong) UIView *tagIdRow;
@property (nonatomic, strong) UIView *lastRefreshedRow;
@property (nonatomic, strong) UIView *downloadsRow;
@property (nonatomic, strong) UIView *linkRow;
@property (nonatomic, strong) UIView *postedByRow;
@property (nonatomic, strong) UIView *postedRow;
@property (nonatomic, strong) UIView *arrangedByRow;
@property (nonatomic, strong) UIView *yearArrangedRow;
@property (nonatomic, strong) UIView *sungByRow;
@property (nonatomic, strong) UIView *yearSungRow;

@property (nonatomic, strong) UIStackView *stack;

@end

@implementation DPTagDetailController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)refreshView {
    // Update UI
    self.titleLabel.text = self.tag.title;

    self.tagIdLabel.text = [NSString stringWithFormat:@"%d", self.tag.tagId];

    NSDateFormatter *lastRefreshedFormatter = [[NSDateFormatter alloc] init];
    lastRefreshedFormatter.dateStyle = NSDateFormatterMediumStyle;
    lastRefreshedFormatter.timeStyle = NSDateFormatterShortStyle;
    self.lastRefreshedLabel.text = self.tag.lastRefreshed
        ? [lastRefreshedFormatter stringFromDate:self.tag.lastRefreshed]
        : @"Not cached yet";

    NSDateFormatter *otherDateFormatter = [[NSDateFormatter alloc] init];
    otherDateFormatter.dateStyle = NSDateFormatterLongStyle;
    otherDateFormatter.timeStyle = NSDateFormatterNoStyle;

    self.downloadsLabel.text = [NSString stringWithFormat:@"%d", self.tag.downloadCount];

    self.linkButton.url = [NSURL URLWithString:[NSString stringWithFormat:@"http://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=%d", self.tag.tagId]];

    [self updateAttribution:self.postedByValue name:self.tag.provider website:self.tag.providerWebsite];
    self.postedByRow.hidden = !self.tag.provider;

    self.postedLabel.text = self.tag.posted ? [otherDateFormatter stringFromDate:self.tag.posted] : @"Unknown";

    [self updateAttribution:self.arrangedByValue name:self.tag.arranger website:self.tag.arrangerWebsite];
    self.arrangedByRow.hidden = !self.tag.arranger;

    self.yearArrangedLabel.text = [NSString stringWithFormat:@"%d", self.tag.yearArranged];
    self.yearArrangedRow.hidden = self.tag.yearArranged == 0;

    [self updateAttribution:self.sungByValue name:self.tag.sungBy website:self.tag.sungByWebsite];
    self.sungByRow.hidden = !self.tag.sungBy;

    self.yearSungLabel.text = [NSString stringWithFormat:@"%d", self.tag.sungYear];
    self.yearSungRow.hidden = self.tag.sungYear == 0;
}

// Only websites are controls. Replace the value on refresh so a removed URL
// also removes its action and accessibility traits from the hierarchy.
- (void)updateAttribution:(UIStackView *)value name:(NSString *)name website:(NSURL *)website {
    if (!value) return;
    for (UIView *child in value.arrangedSubviews) {
        [value removeArrangedSubview:child];
        [child removeFromSuperview];
    }
    if (website) {
        UIButton *button = [self makeLinkButton];
        [button setTitle:name forState:UIControlStateNormal];
        button.url = website;
        [value addArrangedSubview:button];
    } else {
        // Use UILabel directly. DPLabel removes intrinsic width for multiline
        // text, which collapses a leading-aligned value at accessibility sizes.
        UILabel *label = [[UILabel alloc] init];
        label.font = [TMTheme bodyFont];
        label.adjustsFontForContentSizeCategory = YES;
        label.textColor = [TMTheme primaryText];
        label.text = name;
        label.numberOfLines = 0;
        label.isAccessibilityElement = YES;
        label.accessibilityTraits = UIAccessibilityTraitStaticText;
        [value addArrangedSubview:label];
    }
}

- (UIButton *)makeLinkButton {
    UIButton *button = [UIButton buttonWithType:UIButtonTypeSystem];
    button.titleLabel.font = [TMTheme bodyFont];
    button.titleLabel.adjustsFontForContentSizeCategory = YES;
    button.titleLabel.numberOfLines = 0;
    button.contentHorizontalAlignment = UIControlContentHorizontalAlignmentLeading;
    [button.heightAnchor constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;
    return button;
}

- (void)viewDidLoad
{
    [super viewDidLoad];

    self.titleLabel = [self makeTitleLabel];
    self.titleLabel.accessibilityTraits = UIAccessibilityTraitHeader;
    self.tagIdLabel = [self makeBodyLabel];
    self.lastRefreshedLabel = [self makeBodyLabel];
    self.lastRefreshedLabel.numberOfLines = 0;
    self.downloadsLabel = [self makeBodyLabel];
    self.linkButton = [self makeLinkButton];
    [self.linkButton setTitle:@"BarbershopTags.com" forState:UIControlStateNormal];
    self.postedByValue = [[UIStackView alloc] init];
    self.postedByValue.axis = UILayoutConstraintAxisVertical;
    self.postedLabel = [self makeBodyLabel];
    self.postedLabel.numberOfLines = 0;
    self.arrangedByValue = [[UIStackView alloc] init];
    self.arrangedByValue.axis = UILayoutConstraintAxisVertical;
    self.yearArrangedLabel = [self makeBodyLabel];
    self.sungByValue = [[UIStackView alloc] init];
    self.sungByValue.axis = UILayoutConstraintAxisVertical;
    self.yearSungLabel = [self makeBodyLabel];

    self.tagIdRow = [TMFieldRow fieldRowWithTitle:@"Tag ID" value:self.tagIdLabel];
    self.lastRefreshedRow = [TMFieldRow fieldRowWithTitle:@"Last Refreshed" value:self.lastRefreshedLabel];
    self.downloadsRow = [TMFieldRow fieldRowWithTitle:@"Downloads" value:self.downloadsLabel];
    self.linkRow = [TMFieldRow fieldRowWithTitle:@"Link" value:self.linkButton];
    self.postedByRow = [TMFieldRow fieldRowWithTitle:@"Posted By" value:self.postedByValue];
    self.postedRow = [TMFieldRow fieldRowWithTitle:@"Posted" value:self.postedLabel];
    self.arrangedByRow = [TMFieldRow fieldRowWithTitle:@"Arranged By" value:self.arrangedByValue];
    self.yearArrangedRow = [TMFieldRow fieldRowWithTitle:@"Year Arranged" value:self.yearArrangedLabel];
    self.sungByRow = [TMFieldRow fieldRowWithTitle:@"Sung By" value:self.sungByValue];
    self.yearSungRow = [TMFieldRow fieldRowWithTitle:@"Year Sung" value:self.yearSungLabel];

    self.stack = [[UIStackView alloc] initWithArrangedSubviews:@[
        self.titleLabel,
        self.postedByRow, self.postedRow,
        self.arrangedByRow, self.yearArrangedRow,
        self.sungByRow, self.yearSungRow,
        self.downloadsRow, self.tagIdRow, self.lastRefreshedRow,
        self.linkRow
    ]];
    self.stack.axis = UILayoutConstraintAxisVertical;
    self.stack.alignment = UIStackViewAlignmentFill;
    self.stack.spacing = TMTheme.spaceM;
    [self.stack setCustomSpacing:TMTheme.spaceL afterView:self.titleLabel];
    [self.stack setCustomSpacing:TMTheme.spaceL afterView:self.yearSungRow];

    UIScrollView *scroller = [[UIScrollView alloc] init];

    [self setUpRootView:self.stack withScroller:scroller];

    [self refreshView];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
