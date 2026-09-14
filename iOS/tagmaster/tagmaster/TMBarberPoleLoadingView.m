#import "TMBarberPoleLoadingView.h"
#import "TMLogoArtwork.h"

@interface TMBarberPoleLoadingView ()
@property (nonatomic, strong) CALayer *logoLayer;
@property (nonatomic, strong) CALayer *cylinder;
@property (nonatomic, strong) CAShapeLayer *stripes;
@property (nonatomic, strong) CALayer *axisLayer;
@property (nonatomic, strong) CAShapeLayer *frameLayer;
@property (nonatomic) BOOL requested;
@property (nonatomic) BOOL appActive;
@property (nonatomic, readwrite, getter=isCompact) BOOL compact;
- (void)updateAnimation;
@end

// Ancestor hiding and scrolling need not trigger layout on an accessory view.
// One weak registry checks those boundaries, never one retained timer per host.
// Core Animation owns stripe motion; this monitor creates no paths or frames.
@interface TMLoaderVisibilityMonitor : NSObject
@property (nonatomic, strong) NSHashTable<TMBarberPoleLoadingView *> *views;
@property (nonatomic, strong) NSTimer *timer;
+ (instancetype)shared;
- (void)updateView:(TMBarberPoleLoadingView *)view;
@end
@implementation TMLoaderVisibilityMonitor
+ (instancetype)shared {
    static TMLoaderVisibilityMonitor *monitor;
    static dispatch_once_t once;
    dispatch_once(&once, ^{ monitor = [self new]; monitor.views = [NSHashTable weakObjectsHashTable]; });
    return monitor;
}
- (void)updateView:(TMBarberPoleLoadingView *)view {
    if (view.requested && view.window && view.appActive) [self.views addObject:view];
    else [self.views removeObject:view];
    if (self.views.count && !self.timer) {
        self.timer = [NSTimer timerWithTimeInterval:0.1 target:self selector:@selector(checkVisibility:) userInfo:nil repeats:YES];
        [NSRunLoop.mainRunLoop addTimer:self.timer forMode:NSRunLoopCommonModes];
    } else if (!self.views.count) {
        [self.timer invalidate]; self.timer = nil;
    }
}
- (void)checkVisibility:(NSTimer *)timer {
    for (TMBarberPoleLoadingView *view in self.views.allObjects) [view updateAnimation];
    if (!self.views.count) { [self.timer invalidate]; self.timer = nil; }
}
@end

@implementation TMBarberPoleLoadingView
- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.isAccessibilityElement = YES;
        self.accessibilityLabel = @"Loading tags";
        self.accessibilityIdentifier = @"query.loading.barberpole";
        self.appActive = UIApplication.sharedApplication.applicationState == UIApplicationStateActive;
        self.logoLayer = [CALayer layer];
        self.logoLayer.bounds = CGRectMake(0, 0, TMLogoWidth, TMLogoHeight);
        [self.layer addSublayer:self.logoLayer];
        self.frameLayer = [CAShapeLayer layer];
        self.frameLayer.frame = self.logoLayer.bounds;
        self.frameLayer.path = TMLoaderMetalPath();
        self.frameLayer.fillRule = kCAFillRuleEvenOdd;
        [self.logoLayer addSublayer:self.frameLayer];

        self.cylinder = [CALayer layer];
        self.cylinder.frame = self.logoLayer.bounds;
        self.cylinder.backgroundColor = TMLoaderColor(@"white");
        CAShapeLayer *shaftMask = [CAShapeLayer layer];
        shaftMask.frame = self.cylinder.bounds;
        shaftMask.path = TMLoaderShaftPath();
        self.cylinder.mask = shaftMask;
        [self.logoLayer addSublayer:self.cylinder];
        // Rotate a parent around the canonical origin. Translation of its child is
        // therefore in axial space, never screen y. Shared paths remain immutable.
        self.axisLayer = [CALayer layer];
        self.axisLayer.anchorPoint = CGPointZero;
        self.axisLayer.position = CGPointZero;
        self.axisLayer.bounds = self.logoLayer.bounds;
        self.axisLayer.affineTransform = CGAffineTransformMakeRotation(TMLoaderAxisAngle * M_PI / 180);
        [self.cylinder addSublayer:self.axisLayer];
        self.stripes = [CAShapeLayer layer];
        self.stripes.frame = self.logoLayer.bounds;
        [self.axisLayer addSublayer:self.stripes];
        for (NSInteger index = TMLoaderRepeatMin; index <= TMLoaderRepeatMax; index++) {
            CAShapeLayer *stripe = [CAShapeLayer layer];
            stripe.frame = self.stripes.bounds;
            stripe.path = TMLoaderStripePath();
            stripe.affineTransform = CGAffineTransformMakeTranslation(0, index * TMLoaderStripeStep);
            stripe.fillColor = TMLoaderColor(index % 2 == 0 ? @"red" : @"blue");
            [self.stripes addSublayer:stripe];
        }
        NSNotificationCenter *center = NSNotificationCenter.defaultCenter;
        [center addObserver:self selector:@selector(resignActive) name:UIApplicationWillResignActiveNotification object:nil];
        [center addObserver:self selector:@selector(becomeActive) name:UIApplicationDidBecomeActiveNotification object:nil];
        [center addObserver:self selector:@selector(updateAnimation) name:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
    }
    return self;
}
- (void)dealloc { [NSNotificationCenter.defaultCenter removeObserver:self]; }
- (instancetype)initWithOperationName:(NSString *)operationName {
    if ((self = [self initWithFrame:CGRectMake(0, 0, TMLoaderCompactWidth, TMLoaderCompactHeight)])) {
        self.compact = YES;
        self.controllerVisible = YES;
        self.accessibilityIdentifier = nil;
        self.accessibilityLabel = operationName;
        self.hidden = YES;
        [self setContentHuggingPriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
        [self setContentCompressionResistancePriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    }
    return self;
}
- (CGSize)intrinsicContentSize {
    return self.compact ? CGSizeMake(TMLoaderCompactWidth, TMLoaderCompactHeight) : CGSizeMake(TMLoaderArtworkWidth, 68);
}
- (CGSize)sizeThatFits:(CGSize)size {
    return self.compact ? self.intrinsicContentSize : CGSizeMake(MAX(TMLoaderArtworkWidth, self.bounds.size.width), 68);
}
- (void)setDarkSurface:(BOOL)darkSurface { _darkSurface = darkSurface; [self setNeedsLayout]; }
- (BOOL)reduceMotionEnabled { return UIAccessibilityIsReduceMotionEnabled(); }
- (BOOL)isAnimating { return self.requested; }
- (void)startAnimating { self.requested = YES; self.hidden = NO; [self updateAnimation]; }
- (void)stopAnimating { self.requested = NO; self.hidden = YES; [self updateAnimation]; }
- (void)setControllerVisible:(BOOL)visible { _controllerVisible = visible; [self updateAnimation]; }
- (void)setHidden:(BOOL)hidden { [super setHidden:hidden]; [self updateAnimation]; }
- (void)didMoveToWindow { [super didMoveToWindow]; [self updateAnimation]; }
- (void)resignActive { self.appActive = NO; [self updateAnimation]; }
- (void)becomeActive { self.appActive = YES; [self updateAnimation]; }
- (void)updateAnimation {
    [[TMLoaderVisibilityMonitor shared] updateView:self];
    BOOL visible = self.window && self.controllerVisible && self.appActive && self.requested;
    for (UIView *view = self; view; view = view.superview) {
        visible &= !view.hidden && view.alpha > 0;
        if (view.clipsToBounds) visible &= CGRectIntersectsRect([self convertRect:self.bounds toView:view], view.bounds);
    }
    if (!visible || [self reduceMotionEnabled]) {
        [self.stripes removeAnimationForKey:@"rotationStripes"];
    } else if (![self.stripes animationForKey:@"rotationStripes"]) {
        CABasicAnimation *motion = [CABasicAnimation animationWithKeyPath:@"transform.translation.y"];
        motion.fromValue = @0; motion.toValue = @(TMLoaderStripeStep * TMLoaderPhaseMultiplier);
        motion.duration = TMLoaderDurationSeconds;
        motion.repeatCount = HUGE_VALF;
        motion.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionLinear];
        [self.stripes addAnimation:motion forKey:@"rotationStripes"];
    }
}
- (void)layoutSubviews {
    [super layoutSubviews];
    [CATransaction begin]; [CATransaction setDisableActions:YES];
    CGFloat artworkHeight = self.compact ? TMLoaderCompactHeight : TMLoaderArtworkHeight;
    CGFloat artworkWidth = self.compact ? TMLoaderCompactWidth : TMLoaderArtworkWidth;
    CGFloat scale = MIN(MIN(artworkHeight, self.bounds.size.height) / TMLogoHeight, MIN(artworkWidth, self.bounds.size.width) / TMLogoWidth);
    self.logoLayer.position = CGPointMake(CGRectGetMidX(self.bounds), CGRectGetMidY(self.bounds));
    self.logoLayer.affineTransform = CGAffineTransformMakeScale(scale, scale);
    self.frameLayer.fillColor = TMLoaderColor((self.darkSurface || self.traitCollection.userInterfaceStyle == UIUserInterfaceStyleDark) ? @"metalDark" : @"metalLight");
    [CATransaction commit];
    [self updateAnimation];
}
- (void)traitCollectionDidChange:(UITraitCollection *)previousTraitCollection {
    [super traitCollectionDidChange:previousTraitCollection];
    [self setNeedsLayout];
}
@end
