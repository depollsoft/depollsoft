//
//  DPAppDelegate+Ads.h
//  pitchperfect
//
//  Ads category for DPAppDelegate
//

#import "DPAppDelegate.h"
#import "GoogleMobileAdsStub.h"

@interface DPAppDelegate (Ads) <GADBannerViewDelegate>

+ (GADRequest *)adRequest;
+ (NSString *)bannerAdUnitID;

@end
