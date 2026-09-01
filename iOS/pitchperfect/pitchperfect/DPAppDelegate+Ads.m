#import "DPAppDelegate+Ads.h"
#import "GoogleMobileAdsStub.h"

@implementation DPAppDelegate (Ads)
+ (GADRequest *)adRequest {
    return [GADRequest request];
}

- (void)bannerViewDidReceiveAd:(GADBannerView *)bannerView {
    NSLog(@"Pitch Perfect banner ad loaded: %@", bannerView.adUnitID);
}

- (void)bannerView:(GADBannerView *)bannerView
    didFailToReceiveAdWithError:(NSError *)error {
    NSLog(@"Pitch Perfect banner ad failed (%@): %@",
          bannerView.adUnitID,
          error.localizedDescription);
}

+ (void)resizeAndReloadBannerView:(GADBannerView *)bannerView
                forViewController:(UIViewController *)viewController {
    if (!bannerView || !bannerView.superview || !viewController.view.window) {
        return;
    }

    CGFloat width = CGRectGetWidth(viewController.view.bounds);
    if (width <= 0) {
        return;
    }

    UIInterfaceOrientation orientation = viewController.view.window.windowScene.interfaceOrientation;
    if (UIInterfaceOrientationIsLandscape(orientation)) {
        bannerView.adSize = GADLandscapeAnchoredAdaptiveBannerAdSizeWithWidth(width);
    } else {
        bannerView.adSize = GADPortraitAnchoredAdaptiveBannerAdSizeWithWidth(width);
    }
    bannerView.backgroundColor = UIColor.systemBackgroundColor;
    bannerView.clipsToBounds = YES;
    [bannerView loadRequest:[self adRequest]];
}

+ (NSString *)bannerAdUnitID {
#if DEBUG
    return @"ca-app-pub-3940256099942544/2934735716";
#else
    if ([NSBundle.mainBundle.bundleIdentifier hasSuffix:@".private"]) {
        return @"ca-app-pub-3940256099942544/2934735716";
    }
    return @"a14fd7eba4542f0";
#endif
}
@end
