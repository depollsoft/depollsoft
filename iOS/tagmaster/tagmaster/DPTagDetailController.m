//
//  DPTagDetailController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagDetailController.h"
// Metadata refits per pair. Shared DPGridLayout and other products stay unchanged.
@interface TMDetailPair : UIStackView
@property UILabel *caption;
@property UIView *value;
@property NSLayoutConstraint *captionWidth;
@property CGFloat preferredCaptionWidth;
@end
@implementation TMDetailPair
- (void)layoutSubviews {
    CGFloat width = self.bounds.size.width;
    if (width > 0 && !self.hidden) {
        CGFloat caption = ceil([self.caption.text sizeWithAttributes:@{NSFontAttributeName:self.caption.font}].width);
        UILabel *label = [self.value isKindOfClass:UIButton.class] ? ((UIButton *)self.value).titleLabel : (UILabel *)self.value;
        NSString *text = [self.value isKindOfClass:UIButton.class] ? [(UIButton *)self.value currentTitle] : label.text;
        UIFont *font = label.font ?: [UIFont preferredFontForTextStyle:UIFontTextStyleBody compatibleWithTraitCollection:self.traitCollection];
        CGFloat natural = ceil([text sizeWithAttributes:@{NSFontAttributeName:font}].width) + 4;
        CGFloat usableValue = MAX(44, MIN(natural, font.pointSize * 8));
        // Align fitting columns where possible; a long caption cannot take width
        // away from another pair whose short caption still fits beside its value.
        CGFloat column = self.preferredCaptionWidth + 8 + usableValue <= width ? self.preferredCaptionWidth : caption;
        BOOL stacked = column + 8 + usableValue > width;
        self.captionWidth.active = !stacked;
        self.captionWidth.constant = column;
        self.axis = stacked ? UILayoutConstraintAxisVertical : UILayoutConstraintAxisHorizontal;
        self.alignment = stacked ? UIStackViewAlignmentFill : UIStackViewAlignmentTop;
        self.spacing = stacked ? 4 : 8;
        UIStackView *parent = (UIStackView *)self.superview;
        CGFloat separation = stacked ? 16 : 4;
        if ([parent customSpacingAfterView:self] != separation) [parent setCustomSpacing:separation afterView:self];
    }
    [super layoutSubviews];
}
@end

@interface TMDetailBodyLabel : UILabel
@end
@implementation TMDetailBodyLabel
- (CGSize)intrinsicContentSize {
    if (self.bounds.size.width <= 0) return [super intrinsicContentSize];
    return CGSizeMake(UIViewNoIntrinsicMetric, [self sizeThatFits:CGSizeMake(self.bounds.size.width, CGFLOAT_MAX)].height);
}
- (void)setBounds:(CGRect)bounds {
    BOOL changed = self.bounds.size.width != bounds.size.width;
    [super setBounds:bounds];
    if (changed) [self invalidateIntrinsicContentSize];
}
@end
#import "UIView+DPUtils.h"
#import "DPTagPageControllerBase.h"

@interface DPTagDetailController ()

@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UILabel *tagIdHeader;
@property (nonatomic, strong) UILabel *tagIdLabel;
@property (nonatomic, strong) UILabel *lastRefreshedHeader;
@property (nonatomic, strong) UILabel *lastRefreshedLabel;
@property (nonatomic, strong) UILabel *downloadsHeader;
@property (nonatomic, strong) UILabel *downloadsLabel;
@property (nonatomic, strong) UILabel *linkHeader;
@property (nonatomic, strong) UIButton *linkButton;
@property (nonatomic, strong) UILabel *postedByHeader;
@property (nonatomic, strong) UIButton *postedByButton;
@property (nonatomic, strong) UILabel *postedHeader;
@property (nonatomic, strong) UILabel *postedLabel;
@property (nonatomic, strong) UILabel *arrangedByHeader;
@property (nonatomic, strong) UIButton *arrangedByButton;
@property (nonatomic, strong) UILabel *yearArrangedHeader;
@property (nonatomic, strong) UILabel *yearArrangedLabel;
@property (nonatomic, strong) UILabel *sungByHeader;
@property (nonatomic, strong) UIButton *sungByButton;
@property (nonatomic, strong) UILabel *yearSungHeader;
@property (nonatomic, strong) UILabel *yearSungLabel;

@property (nonatomic, strong) UIStackView *metadataStack;
@property (nonatomic, copy) NSArray<TMDetailPair *> *metadataPairs;

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
    self.lastRefreshedLabel.text = [lastRefreshedFormatter stringFromDate:self.tag.lastRefreshed];
    
    NSDateFormatter *otherDateFormatter = [[NSDateFormatter alloc] init];
    otherDateFormatter.dateStyle = NSDateFormatterFullStyle;
    
    self.downloadsLabel.text = [NSString stringWithFormat:@"%d", self.tag.downloadCount];
    
    self.linkButton.url = [NSURL URLWithString:[NSString stringWithFormat:@"https://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id=%d", self.tag.tagId]];
    
    [self.postedByButton setTitle:self.tag.provider forState:UIControlStateNormal];
    self.postedByButton.url = self.tag.providerWebsite;
    [self setButton:self.postedByButton linked:!!self.tag.providerWebsite];
    [self setMetadataView:self.postedByHeader hidden:!self.tag.provider];
    [self setMetadataView:self.postedByButton hidden:!self.tag.provider];
    
    self.postedLabel.text = [otherDateFormatter stringFromDate:self.tag.posted];
    
    [self.arrangedByButton setTitle:self.tag.arranger forState:UIControlStateNormal];
    self.arrangedByButton.url = self.tag.arrangerWebsite;
    [self setButton:self.arrangedByButton linked:!!self.tag.arrangerWebsite];
    [self setMetadataView:self.arrangedByHeader hidden:!self.tag.arranger];
    [self setMetadataView:self.arrangedByButton hidden:!self.tag.arranger];
    
    self.yearArrangedLabel.text = [NSString stringWithFormat:@"%d", self.tag.yearArranged];
    [self setMetadataView:self.yearArrangedHeader hidden:self.tag.yearArranged == 0];
    [self setMetadataView:self.yearArrangedLabel hidden:self.tag.yearArranged == 0];
    
    [self.sungByButton setTitle:self.tag.sungBy forState:UIControlStateNormal];
    self.sungByButton.url = self.tag.sungByWebsite;
    [self setButton:self.sungByButton linked:!!self.tag.sungByWebsite];
    [self setMetadataView:self.sungByHeader hidden:!self.tag.sungBy];
    [self setMetadataView:self.sungByButton hidden:!self.tag.sungBy];
    
    self.yearSungLabel.text = [NSString stringWithFormat:@"%d", self.tag.sungYear];
    [self setMetadataView:self.yearSungHeader hidden:self.tag.sungYear == 0];
    [self setMetadataView:self.yearSungLabel hidden:self.tag.sungYear == 0];
}

/// A name without a website is plain information, not a disabled control.
- (void)setButton:(UIButton *)button linked:(BOOL)linked {
    UIButtonConfiguration *configuration = button.configuration;
    configuration.baseForegroundColor = linked ? nil : [UIColor labelColor];
    button.configuration = configuration;
    button.enabled = YES;
    button.userInteractionEnabled = linked;
    button.accessibilityTraits = linked ? UIAccessibilityTraitLink : UIAccessibilityTraitStaticText;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    self.titleLabel = [self makeTitleLabel];
    self.tagIdHeader = [self makeHeader:@"Tag ID"];
    self.tagIdLabel = [self makeBodyLabel];
    self.lastRefreshedHeader = [self makeHeader:@"Last Refreshed"];
    self.lastRefreshedLabel = [self makeBodyLabel];
    self.downloadsHeader = [self makeHeader:@"Downloads"];
    self.downloadsLabel = [self makeBodyLabel];
    self.linkHeader = [self makeHeader:@"Link"];
    self.linkButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    [self.linkButton setTitle:@"BarbershopTags.com" forState:UIControlStateNormal];
    self.postedByHeader = [self makeHeader:@"Posted By"];
    self.postedByButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    self.postedHeader = [self makeHeader:@"Posted"];
    self.postedLabel = [self makeBodyLabel];
    self.arrangedByHeader = [self makeHeader:@"Arranged By"];
    self.arrangedByButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    self.yearArrangedHeader = [self makeHeader:@"Year Arranged"];
    self.yearArrangedLabel = [self makeBodyLabel];
    self.sungByHeader = [self makeHeader:@"Sung By"];
    self.sungByButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    self.yearSungHeader = [self makeHeader:@"Year Sung"];
    self.yearSungLabel = [self makeBodyLabel];
    
    for (UIButton *button in @[self.linkButton, self.postedByButton, self.arrangedByButton, self.sungByButton]) {
        // Let the configuration own wrapping; a multi-line titleLabel inside a configured
        // button collapses to its minimum width and wraps one character per line.
        UIButtonConfiguration *configuration = [UIButtonConfiguration plainButtonConfiguration];
        configuration.titleLineBreakMode = NSLineBreakByWordWrapping;
        // A hair of leading inset keeps the first glyph of a wrapped title from clipping.
        configuration.contentInsets = NSDirectionalEdgeInsetsMake(0, 2, 0, 2);
        configuration.titleTextAttributesTransformer = ^NSDictionary<NSAttributedStringKey, id> *(NSDictionary<NSAttributedStringKey, id> *attributes) {
            NSMutableDictionary *updated = [attributes mutableCopy];
            updated[NSFontAttributeName] = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
            return updated;
        };
        button.configuration = configuration;
        button.titleLabel.adjustsFontForContentSizeCategory = YES;
        button.contentHorizontalAlignment = UIControlContentHorizontalAlignmentLeading;
        NSLayoutConstraint *minimumHeight = [button.heightAnchor constraintGreaterThanOrEqualToConstant:44];
        minimumHeight.priority = UILayoutPriorityRequired - 1;
        minimumHeight.active = YES;
    }
    NSArray *captions = @[self.tagIdHeader, self.lastRefreshedHeader, self.downloadsHeader, self.linkHeader,
                          self.postedByHeader, self.postedHeader, self.arrangedByHeader, self.yearArrangedHeader,
                          self.sungByHeader, self.yearSungHeader];
    NSArray *values = @[self.tagIdLabel, self.lastRefreshedLabel, self.downloadsLabel, self.linkButton,
                        self.postedByButton, self.postedLabel, self.arrangedByButton, self.yearArrangedLabel,
                        self.sungByButton, self.yearSungLabel];
    self.metadataStack = [[UIStackView alloc] initWithArrangedSubviews:@[self.titleLabel]];
    self.metadataStack.axis = UILayoutConstraintAxisVertical;
    self.metadataStack.spacing = 4;
    [self.metadataStack setCustomSpacing:8 afterView:self.titleLabel];
    NSMutableArray *pairs = [NSMutableArray array];
    for (NSUInteger index = 0; index < captions.count; index++) {
        TMDetailPair *pair = [[TMDetailPair alloc] initWithArrangedSubviews:@[captions[index], values[index]]];
        pair.caption = captions[index];
        pair.value = values[index];
        pair.captionWidth = [pair.caption.widthAnchor constraintEqualToConstant:0];
        pair.captionWidth.priority = UILayoutPriorityRequired - 1;
        pair.accessibilityElements = @[pair.caption, pair.value];
        [pairs addObject:pair];
        [self.metadataStack addArrangedSubview:pair];
    }
    self.metadataPairs = pairs;
    [self setUpRootView:self.metadataStack withScroller:[UIScrollView new]];

    [self refreshView];
}

- (UILabel *)makeBodyLabel {
    UILabel *label = [TMDetailBodyLabel new];
    label.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    label.adjustsFontForContentSizeCategory = YES;
    label.numberOfLines = 0;
    return label;
}

- (void)setMetadataView:(UIView *)view hidden:(BOOL)hidden {
    for (TMDetailPair *pair in self.metadataPairs) {
        if (pair.caption == view || pair.value == view) pair.hidden = hidden;
    }
    if (self.isViewLoaded) [self.view setNeedsLayout];
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    CGFloat widestCaption = 0;
    for (TMDetailPair *pair in self.metadataPairs) {
        if (!pair.hidden) widestCaption = MAX(widestCaption, ceil([pair.caption.text sizeWithAttributes:@{NSFontAttributeName:pair.caption.font}].width));
    }
    for (TMDetailPair *pair in self.metadataPairs) {
        if (pair.preferredCaptionWidth != widestCaption) {
            pair.preferredCaptionWidth = widestCaption;
            [pair setNeedsLayout];
        }
    }
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
