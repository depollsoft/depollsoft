#import "TMQuartetStaffView.h"
#import "TMQuartetArtwork.h"

@implementation TMQuartetStaffView
- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.backgroundColor = UIColor.clearColor;
        self.accessibilityElementsHidden = YES;
        self.artwork = [CALayer layer];
        self.artwork.anchorPoint = CGPointZero;
        [self.layer addSublayer:self.artwork];
        self.staff = [CAShapeLayer layer];
        self.staff.path = TMQuartetStaffPath();
        self.staff.fillColor = nil;
        self.staff.lineWidth = TMQuartetStaffWidth;
        CAShapeLayer *staffMask = [CAShapeLayer layer];
        staffMask.path = TMQuartetStaffMaskPath();
        staffMask.fillRule = kCAFillRuleEvenOdd;
        self.staff.mask = staffMask;
        [self.artwork addSublayer:self.staff];
        NSMutableArray *notation = [NSMutableArray array];
        CGPathRef paths[] = {TMQuartetStemPath(), TMQuartetLedgerPath(), TMQuartetFlatPath(), TMQuartetLabelPath()};
        for (NSUInteger i = 0; i < 4; i++) {
            CAShapeLayer *symbol = [CAShapeLayer layer];
            symbol.path = paths[i];
            CAShapeLayer *mask = [CAShapeLayer layer];
            mask.path = TMQuartetStaffMaskPath();
            mask.fillRule = kCAFillRuleEvenOdd;
            symbol.mask = mask;
            [self.artwork addSublayer:symbol];
            [notation addObject:symbol];
        }
        self.notation = notation;
        NSMutableArray *notes = [NSMutableArray array];
        for (NSUInteger i = 0; i < 4; i++) {
            CAShapeLayer *note = [CAShapeLayer layer];
            note.path = TMQuartetNotePath();
            note.position = CGPointMake(TMQuartetX[i], TMQuartetY[i]);
            [self.artwork addSublayer:note];
            [notes addObject:note];
        }
        self.notes = notes;
    }
    return self;
}
- (CGSize)intrinsicContentSize { return CGSizeMake(TMQuartetWidth, TMQuartetHeight); }
- (void)layoutSubviews {
    [super layoutSubviews];
    CGFloat scale = MIN(self.bounds.size.width / TMQuartetWidth, self.bounds.size.height / TMQuartetHeight);
    BOOL dark = self.traitCollection.userInterfaceStyle == UIUserInterfaceStyleDark;
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    self.artwork.position = CGPointMake((self.bounds.size.width - TMQuartetWidth * scale) / 2,
                                       (self.bounds.size.height - TMQuartetHeight * scale) / 2);
    self.artwork.transform = CATransform3DMakeScale(scale, scale, 1);
    self.staff.strokeColor = TMQuartetColor(dark, YES);
    for (CAShapeLayer *note in self.notes) note.fillColor = TMQuartetColor(dark, NO);
    for (CAShapeLayer *symbol in self.notation) symbol.fillColor = TMQuartetColor(dark, NO);
    [CATransaction commit];
    [self updateMotion];
}
- (void)traitCollectionDidChange:(UITraitCollection *)previousTraitCollection {
    [super traitCollectionDidChange:previousTraitCollection];
    [self setNeedsLayout];
}
- (void)setAnimationAllowed:(BOOL)allowed {
    _animationAllowed = allowed;
    [self updateMotion];
}
- (void)setHidden:(BOOL)hidden {
    [super setHidden:hidden];
    [self updateMotion];
}
- (void)didMoveToWindow {
    [super didMoveToWindow];
    [self updateMotion];
}
- (BOOL)reduceMotionEnabled { return UIAccessibilityIsReduceMotionEnabled(); }
- (void)updateMotion {
    BOOL animate = self.animationAllowed && self.window && !self.hidden && ![self reduceMotionEnabled];
    CGRect visible = [self convertRect:self.bounds toView:self.window];
    for (UIView *ancestor = self; ancestor && animate; ancestor = ancestor.superview) {
        if (ancestor.hidden || ancestor.alpha <= 0.01) animate = NO;
        if (ancestor.clipsToBounds) visible = CGRectIntersection(visible, [ancestor convertRect:ancestor.bounds toView:self.window]);
    }
    if (CGRectIsEmpty(visible) || !CGRectIntersectsRect(visible, self.window.bounds)) animate = NO;
    CFTimeInterval start = [self.artwork convertTime:CACurrentMediaTime() fromLayer:nil];
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    for (NSUInteger i = 0; i < self.notes.count; i++) {
        CAShapeLayer *note = self.notes[i];
        note.opacity = TMQuartetStill[i];
        if (!animate) {
            [note removeAllAnimations];
        } else if (![note animationForKey:@"gather"]) {
            CAKeyframeAnimation *motion = [CAKeyframeAnimation animationWithKeyPath:@"opacity"];
            motion.values = TMQuartetSamples(i);
            motion.calculationMode = kCAAnimationLinear;
            motion.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionLinear];
            motion.duration = TMQuartetPeriod;
            motion.beginTime = start;
            motion.repeatCount = HUGE_VALF;
            [note addAnimation:motion forKey:@"gather"];
        }
    }
    [CATransaction commit];
}
@end
