#import <UIKit/UIKit.h>
#import <CoreGraphics/CoreGraphics.h>

#if __has_include(<GoogleMobileAds/GoogleMobileAds.h>)
#import <GoogleMobileAds/GoogleMobileAds.h>
#else
@interface GADRequest : NSObject
+ (instancetype)request;
@end

@interface GADBannerView : UIView
@property(nonatomic, copy) NSString *adUnitID;
@property(nonatomic, weak) UIViewController *rootViewController;
@property(nonatomic) CGSize adSize;
- (void)loadRequest:(GADRequest *)request;
@end

@interface GADMobileAds : NSObject
+ (instancetype)sharedInstance;
- (void)disableSDKCrashReporting;
@end

static inline CGSize GADLandscapeAnchoredAdaptiveBannerAdSizeWithWidth(CGFloat width) {
    return CGSizeMake(width, 50);
}

static inline CGSize GADPortraitAnchoredAdaptiveBannerAdSizeWithWidth(CGFloat width) {
    return CGSizeMake(width, 50);
}
#endif
