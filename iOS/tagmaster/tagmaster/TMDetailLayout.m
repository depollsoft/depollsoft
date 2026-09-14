#import "TMDetailLayout.h"

@interface TMDetailPair ()
@property NSLayoutConstraint *fittedHeight;
@end
@implementation TMDetailPair
+ (instancetype)caption:(UILabel *)caption value:(UIView *)value {
    TMDetailPair *pair = [self new];
    pair.caption = caption; pair.value = value; pair.rowHeight = 44;
    [pair addSubview:caption]; [pair addSubview:value];
    caption.translatesAutoresizingMaskIntoConstraints = YES;
    value.translatesAutoresizingMaskIntoConstraints = YES;
    pair.fittedHeight = [pair.heightAnchor constraintEqualToConstant:0];
    pair.fittedHeight.priority = UILayoutPriorityRequired - 2;
    pair.fittedHeight.active = YES;
    pair.accessibilityElements = @[caption, value];
    return pair;
}
- (void)fitWidth:(CGFloat)width captionWidth:(CGFloat)captionWidth stacked:(BOOL)stacked {
    if (width <= 0) return;
    self.axis = stacked ? UILayoutConstraintAxisVertical : UILayoutConstraintAxisHorizontal;
    self.preferredCaptionWidth = captionWidth;
    CGFloat captionSize = stacked ? width : captionWidth;
    CGFloat valueX = stacked ? 0 : captionWidth + 8;
    CGFloat valueWidth = MAX(1, width - valueX);
    if ([self.value isKindOfClass:UIButton.class]) {
        UIButton *button = (UIButton *)self.value;
        UIButtonConfiguration *configuration = button.configuration;
        UIFont *font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody compatibleWithTraitCollection:self.traitCollection];
        // UIKit's configured title can retain its fitting-height content box even
        // inside a taller manual frame. Put the row rhythm into its real content.
        CGFloat inset = MAX(4, (self.rowHeight - ceil(font.lineHeight)) / 2);
        NSDirectionalEdgeInsets insets = NSDirectionalEdgeInsetsMake(inset, 0, inset, 0);
        if (!NSDirectionalEdgeInsetsEqualToDirectionalEdgeInsets(configuration.contentInsets, insets)) {
            configuration.contentInsets = insets;
            button.configuration = configuration;
        }
        button.contentVerticalAlignment = UIControlContentVerticalAlignmentCenter;
    }
    CGSize captionFit = [self.caption sizeThatFits:CGSizeMake(captionSize, CGFLOAT_MAX)];
    CGSize valueFit = [self.value isKindOfClass:UIStackView.class]
        ? [self.value systemLayoutSizeFittingSize:CGSizeMake(valueWidth, UILayoutFittingCompressedSize.height) withHorizontalFittingPriority:UILayoutPriorityRequired verticalFittingPriority:UILayoutPriorityFittingSizeLevel]
        : [self.value sizeThatFits:CGSizeMake(valueWidth, CGFLOAT_MAX)];
    if ([self.value isKindOfClass:UIButton.class]) valueFit.height = MAX(44, valueFit.height);
    self.value.frame = CGRectMake(valueX, 0, valueWidth, ceil(valueFit.height));
    [self.value setNeedsLayout]; [self.value layoutIfNeeded];
    UILabel *label = [self.value isKindOfClass:UIButton.class] ? ((UIButton *)self.value).titleLabel : (UILabel *)self.value.viewForFirstBaselineLayout;
    CGFloat captionY = 0, valueY = stacked ? ceil(captionFit.height) + 4 : 0;
    if (!stacked && [label isKindOfClass:UILabel.class]) {
        CGFloat baseline = [label convertPoint:CGPointMake(0, label.font.ascender) toView:self.value].y;
        captionY = MAX(0, baseline - self.caption.font.ascender);
        valueY = MAX(0, self.caption.font.ascender - baseline);
    }
    CGFloat natural = MAX(captionY + ceil(captionFit.height), valueY + ceil(valueFit.height));
    BOOL multiline = stacked || ([label isKindOfClass:UILabel.class] && label.bounds.size.height > label.font.lineHeight * 1.5);
    CGFloat fitted = ceil(MAX(self.rowHeight, natural + (multiline && ![self.value isKindOfClass:UIButton.class] ? 8 : 0)));
    CGFloat offset = (fitted - natural) / 2;
    captionY += offset; valueY += offset;
    self.caption.frame = CGRectMake(0, captionY, captionSize, ceil(captionFit.height));
    self.value.frame = CGRectMake(valueX, valueY, valueWidth, ceil(valueFit.height));
    self.fittedHeight.constant = fitted;
}
- (void)layoutSubviews {
    [super layoutSubviews];
    [self fitWidth:self.bounds.size.width captionWidth:self.preferredCaptionWidth stacked:self.axis == UILayoutConstraintAxisVertical];
}
@end

@implementation TMDetailMetadata
- (void)reloadValues {
    // Manually measured children do not propagate UILabel intrinsic-size changes.
    for (UIView *pair in self.arrangedSubviews) [pair setNeedsLayout];
    [self setNeedsLayout];
}
- (instancetype)initWithArrangedSubviews:(NSArray<UIView *> *)views {
    if ((self = [super initWithArrangedSubviews:views])) self.axis = UILayoutConstraintAxisVertical;
    return self;
}
- (void)layoutSubviews {
    CGFloat captionWidth = 0, valueWidth = 96;
    NSMutableArray<TMDetailPair *> *visible = [NSMutableArray array];
    for (UIView *view in self.arrangedSubviews) {
        if (view.hidden || ![view isKindOfClass:TMDetailPair.class]) continue;
        TMDetailPair *pair = (TMDetailPair *)view;
        [visible addObject:pair];
        captionWidth = MAX(captionWidth, ceil([pair.caption.text sizeWithAttributes:@{NSFontAttributeName:pair.caption.font}].width));
        UILabel *label = [pair.value isKindOfClass:UIButton.class] ? ((UIButton *)pair.value).titleLabel : ([pair.value isKindOfClass:UILabel.class] ? (UILabel *)pair.value : nil);
        if (label) {
            NSString *text = [pair.value isKindOfClass:UIButton.class] ? [(UIButton *)pair.value currentTitle] : label.text;
            CGFloat natural = [text sizeWithAttributes:@{NSFontAttributeName:label.font}].width;
            valueWidth = MAX(valueWidth, MIN(natural, label.font.pointSize * 8));
        } else {
            valueWidth = MAX(valueWidth, [pair.value systemLayoutSizeFittingSize:UILayoutFittingCompressedSize].width);
        }
    }
    BOOL stacked = captionWidth + 8 + valueWidth > self.bounds.size.width;
    for (NSUInteger index = 0; index < visible.count; index++) {
        TMDetailPair *pair = visible[index];
        pair.rowHeight = self.compactFacts && ![pair.value isKindOfClass:TMRatingUnit.class] ? 28 : 44;
        [pair fitWidth:self.bounds.size.width captionWidth:captionWidth stacked:stacked];
        CGFloat gap = index + 1 < visible.count && self.compactFacts && [visible[index + 1].value isKindOfClass:TMRatingUnit.class] ? 8 : (stacked ? 4 : 0);
        if ([self customSpacingAfterView:pair] != gap) [self setCustomSpacing:gap afterView:pair];
    }
    [super layoutSubviews];
}
@end

@implementation TMDetailSections
- (instancetype)initWithArrangedSubviews:(NSArray<UIView *> *)views {
    if ((self = [super initWithArrangedSubviews:views])) {
        self.axis = UILayoutConstraintAxisVertical;
        self.spacing = 16;
    }
    return self;
}
@end

@implementation TMSummaryColumns
- (void)layoutSubviews {
    UIStackView *prose = (UIStackView *)self.arrangedSubviews.lastObject;
    BOOL hasProse = NO;
    for (UIView *view in prose.arrangedSubviews) if (!view.hidden) hasProse = YES;
    prose.hidden = !hasProse;
    CGFloat scale = [UIFont preferredFontForTextStyle:UIFontTextStyleBody compatibleWithTraitCollection:self.traitCollection].pointSize / 17;
    BOOL wide = hasProse && self.bounds.size.width >= 560 * scale + 16;
    self.axis = wide ? UILayoutConstraintAxisHorizontal : UILayoutConstraintAxisVertical;
    self.alignment = wide ? UIStackViewAlignmentTop : UIStackViewAlignmentFill;
    self.distribution = wide ? UIStackViewDistributionFillEqually : UIStackViewDistributionFill;
    self.spacing = 16;
    [super layoutSubviews];
}
@end

@implementation TMRatingUnit
- (UIView *)viewForFirstBaselineLayout { return self.baselineLabel; }
- (CGSize)systemLayoutSizeFittingSize:(CGSize)targetSize withHorizontalFittingPriority:(UILayoutPriority)horizontalPriority verticalFittingPriority:(UILayoutPriority)verticalPriority {
    if (targetSize.width > 0 && self.arrangedSubviews.count == 3) {
        UIView *value = self.arrangedSubviews[0];
        UIView *action = self.arrangedSubviews[1];
        UIView *spacer = self.arrangedSubviews[2];
        CGFloat valueWidth = MAX([value systemLayoutSizeFittingSize:UILayoutFittingCompressedSize].width,
                                 ceil([self.baselineLabel.text sizeWithAttributes:@{NSFontAttributeName:self.baselineLabel.font}].width));
        CGFloat actionWidth = [action systemLayoutSizeFittingSize:UILayoutFittingCompressedSize].width;
        BOOL stacked = valueWidth + actionWidth + self.spacing * 2 > targetSize.width;
        // Keep the full number and native Rate target. At AX sizes the whole
        // action moves below the value rather than compressing either title.
        self.axis = stacked ? UILayoutConstraintAxisVertical : UILayoutConstraintAxisHorizontal;
        self.alignment = stacked ? UIStackViewAlignmentLeading : UIStackViewAlignmentCenter;
        spacer.hidden = stacked;
    }
    return [super systemLayoutSizeFittingSize:targetSize withHorizontalFittingPriority:horizontalPriority verticalFittingPriority:verticalPriority];
}
@end
