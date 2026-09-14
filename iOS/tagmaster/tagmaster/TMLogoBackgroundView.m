#import "TMLogoBackgroundView.h"
#import "TMLogoArtwork.h"

@interface TMLogoBackgroundView ()
@property (nonatomic, strong) CAShapeLayer *artworkLayer;
@property (nonatomic) CGRect lastArtworkBounds;
@end

@implementation TMLogoBackgroundView
- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.userInteractionEnabled = NO;
        self.isAccessibilityElement = NO;
        self.accessibilityElementsHidden = YES;
        self.accessibilityIdentifier = @"background.logo.vector";
        self.lastArtworkBounds = CGRectNull;
        self.artworkLayer = [CAShapeLayer layer];
        self.artworkLayer.anchorPoint = CGPointZero;
        self.artworkLayer.position = CGPointZero;
        self.artworkLayer.bounds = CGRectMake(0, 0, TMLogoWidth, TMLogoHeight);
        self.artworkLayer.path = TMLogoFullPath();
        self.artworkLayer.fillRule = kCAFillRuleNonZero;
        self.artworkLayer.fillColor = [UIColor colorWithRed:128.0/255 green:128.0/255 blue:128.0/255 alpha:76.0/255].CGColor;
        [self.layer addSublayer:self.artworkLayer];
    }
    return self;
}
- (void)layoutSubviews {
    [super layoutSubviews];
    if (CGRectEqualToRect(self.bounds, self.lastArtworkBounds)) return;
    self.lastArtworkBounds = self.bounds;
    // Preserve the old 480x800 canvas and its (16,9)-(467,783) artwork margins.
    // Uniform fit, not independent x/y scaling. Canvas center is intentionally
    // slightly offset to match the original PNG's nonempty bounding box.
    CGFloat canvasScale = MIN(self.bounds.size.width / 480, self.bounds.size.height / 800);
    CGFloat scale = canvasScale * MIN(451 / TMLogoWidth, 774 / TMLogoHeight);
    CGFloat x = CGRectGetMidX(self.bounds) + 1.5 * canvasScale - TMLogoWidth * scale / 2;
    CGFloat y = CGRectGetMidY(self.bounds) - 4 * canvasScale - TMLogoHeight * scale / 2;
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    self.artworkLayer.affineTransform = CGAffineTransformMake(scale, 0, 0, scale, x, y);
    [CATransaction commit];
}
@end
