#include <TargetConditionals.h>
#if TARGET_OS_IPHONE

#import "DPBannerAdView.h"
#import "DPAppDelegate+Ads.h"
#import "GoogleMobileAdsStub.h"

@interface DPBannerAdView ()
@property (nonatomic, strong) GADBannerView *bannerView;
@end

@implementation DPBannerAdView

- (instancetype)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        _bannerView = [[GADBannerView alloc] init];
        _bannerView.adUnitID = @"a14fd7eba4542f0";
        _bannerView.translatesAutoresizingMaskIntoConstraints = NO;
        [self addSubview:_bannerView];
        [NSLayoutConstraint activateConstraints:@[
            [_bannerView.centerXAnchor constraintEqualToAnchor:self.centerXAnchor],
            [_bannerView.centerYAnchor constraintEqualToAnchor:self.centerYAnchor]
        ]];
        [_bannerView loadRequest:[DPAppDelegate adRequest]];
    }
    return self;
}

- (CGSize)intrinsicContentSize {
    return CGSizeMake(UIViewNoIntrinsicMetric, 50);
}

- (void)didMoveToWindow {
    [super didMoveToWindow];
    [self updateBanner];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    [self updateBanner];
}

- (void)updateBanner {
    CGFloat width = CGRectGetWidth(self.bounds);
    if (width <= 0) {
        return;
    }
    UIInterfaceOrientation orientation = self.window.windowScene.interfaceOrientation;
    if (UIInterfaceOrientationIsLandscape(orientation)) {
        self.bannerView.adSize = GADLandscapeAnchoredAdaptiveBannerAdSizeWithWidth(width);
    } else {
        self.bannerView.adSize = GADPortraitAnchoredAdaptiveBannerAdSizeWithWidth(width);
    }
    UIViewController *controller = self.window.rootViewController;
    while (controller.presentedViewController) {
        controller = controller.presentedViewController;
    }
    self.bannerView.rootViewController = controller;
}

@end

#endif
