#include <TargetConditionals.h>
#if TARGET_OS_IPHONE
#import "DPAppDelegate+Ads.h"
#import "GoogleMobileAdsStub.h"

@implementation DPAppDelegate (Ads)
+ (GADRequest *)adRequest {
    return [GADRequest request];
}
@end

#endif
