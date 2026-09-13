#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN
/// The same cached vector renderer serves the query footer and fixed compact slots.
@interface TMBarberPoleLoadingView : UIView
@property (nonatomic) BOOL controllerVisible;
/// Use on charcoal navigation chrome even when the content has a light appearance.
@property (nonatomic) BOOL darkSurface;
@property (nonatomic, readonly, getter=isCompact) BOOL compact;
- (instancetype)initWithOperationName:(NSString *)operationName NS_SWIFT_NAME(init(operationName:));
- (void)startAnimating;
- (void)stopAnimating;
- (BOOL)isAnimating;
/// Query-footer scrolling can request an immediate visibility check.
- (void)updateAnimation;
@end
NS_ASSUME_NONNULL_END
