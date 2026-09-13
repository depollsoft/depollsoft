#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN
FOUNDATION_EXPORT const CGFloat TMLogoWidth;
FOUNDATION_EXPORT const CGFloat TMLogoHeight;

// Borrowed, immutable, process-lifetime paths cached with dispatch_once.
// Never release or mutate these. Use CGPathCreateMutableCopy for editable copies.
FOUNDATION_EXPORT CGPathRef TMLogoFullPath(void) CF_RETURNS_NOT_RETAINED;
FOUNDATION_EXPORT CGPathRef TMLogoSilhouettePath(void) CF_RETURNS_NOT_RETAINED;
FOUNDATION_EXPORT CGPathRef TMLogoHighlightsPath(void) CF_RETURNS_NOT_RETAINED;
NS_ASSUME_NONNULL_END
