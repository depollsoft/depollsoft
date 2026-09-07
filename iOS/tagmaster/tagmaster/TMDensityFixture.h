#import <Foundation/Foundation.h>
#if DEBUG
@class DPAppDelegate, DPTag;
/// Local synthetic data for density tests only. No Firebase or disk tag-cache writes.
@interface TMDensityFixture : NSObject
+ (BOOL)launchIfRequested:(DPAppDelegate *)app;
+ (void)activateWithCount:(NSInteger)count;
+ (void)restore;
+ (NSArray<DPTag *> *)tags;
@end
#endif
