#import <UIKit/UIKit.h>

// Decorative cached notation. The enclosing native labels own loading announcements.
// Shared by DPTagViewController's loading state and the iPad split placeholder.
@interface TMQuartetStaffView : UIView
@property (nonatomic, copy) NSArray<CAShapeLayer *> *notes;
@property CALayer *artwork;
@property CAShapeLayer *staff;
@property NSArray<CAShapeLayer *> *notation;
@property (nonatomic) BOOL animationAllowed;
- (void)updateMotion;
@end
