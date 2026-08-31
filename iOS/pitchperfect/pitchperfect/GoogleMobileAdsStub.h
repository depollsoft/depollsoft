#import <UIKit/UIKit.h>
#import <CoreGraphics/CoreGraphics.h>

#if __has_include(<GoogleMobileAds/GoogleMobileAds.h>)
#import <GoogleMobileAds/GoogleMobileAds.h>
#else
@class GADBannerView;

@protocol GADBannerViewDelegate <NSObject>
@optional
- (void)bannerViewDidReceiveAd:(GADBannerView *)bannerView;
- (void)bannerView:(GADBannerView *)bannerView didFailToReceiveAdWithError:(NSError *)error;
@end

@interface GADRequest : NSObject
+ (instancetype)request;
@end

@interface GADBannerView : UIView
@property(nonatomic, copy) NSString *adUnitID;
@property(nonatomic, weak) UIViewController *rootViewController;
@property(nonatomic, weak) id<GADBannerViewDelegate> delegate;
@property(nonatomic) CGSize adSize;
- (void)loadRequest:(GADRequest *)request;
@end

@interface GADMobileAds : NSObject
+ (instancetype)sharedInstance;
- (void)disableSDKCrashReporting;
- (void)startWithCompletionHandler:(void (^ _Nullable)(id _Nullable status))completionHandler;
@end

static inline CGSize GADLandscapeAnchoredAdaptiveBannerAdSizeWithWidth(CGFloat width) {
    return CGSizeMake(width, 50);
}

static inline CGSize GADPortraitAnchoredAdaptiveBannerAdSizeWithWidth(CGFloat width) {
    return CGSizeMake(width, 50);
}
#endif
