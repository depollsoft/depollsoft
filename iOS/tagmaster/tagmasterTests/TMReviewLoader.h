#import <AVFoundation/AVFoundation.h>

@class TMTrackLoader;

// The Swift fixture is looked up by its explicit Objective-C name because the
// generated test header imports tagmaster as a Clang module, which the app is not.
@protocol TMReviewLoaderControl <NSObject>
@property (nonatomic, copy) void (^completion)(AVAudioPCMBuffer *, NSError *);
@property (nonatomic, readonly) NSInteger cancels;
@property (nonatomic) BOOL backgroundDelivery;
- (void)succeedWithBuffer:(AVAudioPCMBuffer *)buffer;
- (void)fail;
@end

typedef TMTrackLoader<TMReviewLoaderControl> TMControlledTrackLoader;

static inline TMControlledTrackLoader *TMCreateReviewLoader(NSURL *url, NSString *cacheKey) {
    Class loaderClass = NSClassFromString(@"TMReviewLoader");
    NSCAssert(loaderClass != Nil, @"The Swift loader fixture must be linked into tagmasterTests");
    return [[loaderClass alloc] initWithUrl:url cacheKey:cacheKey];
}
