#import <UIKit/UIKit.h>

// App-local composition shared only by Tag Master's Summary and Details.
@interface TMDetailPair : UIView
@property UILabel *caption;
@property UIView *value;
@property CGFloat preferredCaptionWidth;
@property NSInteger section;
@property CGFloat rowHeight;
@property UILayoutConstraintAxis axis;
- (void)fitWidth:(CGFloat)width captionWidth:(CGFloat)captionWidth stacked:(BOOL)stacked;
+ (instancetype)caption:(UILabel *)caption value:(UIView *)value;
@end

@interface TMDetailMetadata : UIStackView
@property BOOL compactFacts;
- (void)reloadValues;
@end

@interface TMDetailSections : UIStackView
@end

@interface TMSummaryColumns : UIStackView
@end

@interface TMRatingUnit : UIStackView
@property UILabel *baselineLabel;
@end
