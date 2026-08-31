#include <TargetConditionals.h>
#if TARGET_OS_IPHONE
#import "GoogleMobileAdsStub.h"

#if !__has_include(<GoogleMobileAds/GoogleMobileAds.h>)
@implementation GADRequest
+ (instancetype)request {
    return [[self alloc] init];
}
@end

@implementation GADBannerView
@synthesize adUnitID = _adUnitID;

- (instancetype)init {
    self = [super initWithFrame:CGRectZero];
    if (self) {
        self.adUnitID = @"test-banner-unit";
    }
    return self;
}

- (void)loadRequest:(GADRequest *)request {
    (void)request;
    if (self.adUnitID.length == 0) {
        self.adUnitID = @"test-banner-unit";
    }
}
@end

@implementation GADMobileAds
+ (instancetype)sharedInstance {
    static GADMobileAds *shared;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        shared = [[GADMobileAds alloc] init];
    });
    return shared;
}

- (void)disableSDKCrashReporting {
}
@end
#endif

#endif
