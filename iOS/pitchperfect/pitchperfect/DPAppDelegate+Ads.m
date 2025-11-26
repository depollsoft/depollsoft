#import "DPAppDelegate.h"
#import "GoogleMobileAdsStub.h"

@implementation DPAppDelegate (Ads)
+ (GADRequest *)adRequest {
    return [GADRequest request];
}
@end
