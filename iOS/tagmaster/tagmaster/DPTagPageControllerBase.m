//
//  DPTagPageControllerBase.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagPageControllerBase.h"
#import "DPLabel.h"
#import "UIView+DPUtils.h"

@interface DPTagPageControllerBase ()

@end

// Segments remain the source of truth for the existing index-to-filter mappings.
// Accessibility text sizes use a native menu so every option can remain readable.
@interface TMFilterControl : UIStackView
@property (nonatomic, strong) UISegmentedControl *control;
@property (nonatomic, strong) UIButton *menuButton;
- (instancetype)initWithControl:(UISegmentedControl *)control label:(NSString *)label;
- (void)selectIndex:(NSInteger)index;
@end

@implementation TMFilterControl
- (instancetype)initWithControl:(UISegmentedControl *)control label:(NSString *)label {
    if (self = [super initWithFrame:CGRectZero]) {
        self.axis = UILayoutConstraintAxisVertical;
        self.control = control;
        self.menuButton = [UIButton buttonWithType:UIButtonTypeSystem];
        UIButtonConfiguration *menuConfiguration = [UIButtonConfiguration grayButtonConfiguration];
        menuConfiguration.image = [UIImage systemImageNamed:@"chevron.up.chevron.down"];
        menuConfiguration.imagePlacement = NSDirectionalRectEdgeTrailing;
        menuConfiguration.imagePadding = 8;
        menuConfiguration.preferredSymbolConfigurationForImage = [UIImageSymbolConfiguration configurationWithTextStyle:UIFontTextStyleCaption1];
        self.menuButton.configuration = menuConfiguration;
        self.menuButton.titleLabel.adjustsFontForContentSizeCategory = YES;
        self.menuButton.titleLabel.numberOfLines = 0;
        self.menuButton.accessibilityLabel = label;
        self.menuButton.showsMenuAsPrimaryAction = YES;
        NSLayoutConstraint *minimumHeight = [self.menuButton.heightAnchor constraintGreaterThanOrEqualToConstant:44];
        minimumHeight.priority = UILayoutPriorityRequired - 1;
        minimumHeight.active = YES;
        for (NSLayoutConstraint *constraint in control.constraints) {
            if (constraint.firstAttribute == NSLayoutAttributeHeight) constraint.priority = UILayoutPriorityRequired - 1;
        }
        [self addArrangedSubview:control];
        [self addArrangedSubview:self.menuButton];
        [control addTarget:self action:@selector(updateFilter) forControlEvents:UIControlEventValueChanged];
        [self updateFilter];
    }
    return self;
}
- (void)traitCollectionDidChange:(UITraitCollection *)previousTraitCollection {
    [super traitCollectionDidChange:previousTraitCollection];
    [self updateFilter];
}
- (void)selectIndex:(NSInteger)index {
    self.control.selectedSegmentIndex = index;
    [self.control sendActionsForControlEvents:UIControlEventValueChanged];
}
- (void)updateFilter {
    BOOL large = UIContentSizeCategoryIsAccessibilityCategory(self.traitCollection.preferredContentSizeCategory);
    self.control.hidden = large;
    self.menuButton.hidden = !large;
    self.menuButton.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody compatibleWithTraitCollection:self.traitCollection];
    NSString *selected = [self.control titleForSegmentAtIndex:self.control.selectedSegmentIndex];
    [self.menuButton setTitle:selected forState:UIControlStateNormal];
    self.menuButton.accessibilityValue = selected;
    NSMutableArray *options = [NSMutableArray array];
    for (NSInteger index = 0; index < self.control.numberOfSegments; index++) {
        __weak TMFilterControl *weakSelf = self;
        UIAction *action = [UIAction actionWithTitle:[self.control titleForSegmentAtIndex:index] image:nil identifier:nil handler:^(UIAction *action) {
            [weakSelf selectIndex:index];
        }];
        action.state = self.control.selectedSegmentIndex == index ? UIMenuElementStateOn : UIMenuElementStateOff;
        [options addObject:action];
    }
    self.menuButton.menu = [UIMenu menuWithChildren:options];
}
@end

@implementation TMWrappingButton
- (CGSize)intrinsicContentSize {
    CGSize size = [super intrinsicContentSize];
    if (CGRectGetWidth(self.bounds) > 0) {
        size.height = [self sizeThatFits:CGSizeMake(CGRectGetWidth(self.bounds), CGFLOAT_MAX)].height;
    }
    return size;
}
- (void)setBounds:(CGRect)bounds {
    BOOL widthChanged = CGRectGetWidth(self.bounds) != CGRectGetWidth(bounds);
    [super setBounds:bounds];
    if (widthChanged) [self invalidateIntrinsicContentSize];
}
@end

@implementation TMBusyIndicator

- (void)notify {
    if (self.onBusyCountChanged) self.onBusyCountChanged(self.busyCount);
}

- (void)incrementBusyCount {
    [super incrementBusyCount];
    [self notify];
}

- (void)decrementBusyCount {
    [super decrementBusyCount];
    [self notify];
}

- (void)clearBusyCount {
    [super clearBusyCount];
    [self notify];
}

@end

@implementation UIViewController (TMRecovery)

- (void)tm_showError:(NSString *)message retry:(void (^)(void))retry {
    UIAlertController *alert = [UIAlertController alertControllerWithTitle:@"Couldn't complete request" message:message preferredStyle:UIAlertControllerStyleAlert];
    if (retry) {
        [alert addAction:[UIAlertAction actionWithTitle:@"Retry" style:UIAlertActionStyleDefault handler:^(UIAlertAction *action) { retry(); }]];
    }
    [alert addAction:[UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleCancel handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

@end

@implementation DPTagPageControllerBase

@synthesize tag;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Only Home shows a large title; every pushed page stays inline.
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (UIView *)makeFilterControl:(UISegmentedControl *)control label:(NSString *)label {
    return [[TMFilterControl alloc] initWithControl:control label:label];
}

- (UITableViewCell *)makeFormCellWithHeader:(NSString *)header control:(UIView *)control {
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:nil];
    cell.selectionStyle = UITableViewCellSelectionStyleNone;
    UILabel *label = [self makeHeader:header];
    label.textColor = [UIColor secondaryLabelColor];
    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[label, control]];
    stack.axis = UILayoutConstraintAxisVertical;
    stack.spacing = 8;
    stack.translatesAutoresizingMaskIntoConstraints = NO;
    [cell.contentView addSubview:stack];
    UILayoutGuide *margins = cell.contentView.layoutMarginsGuide;
    [NSLayoutConstraint activateConstraints:@[
        [stack.leadingAnchor constraintEqualToAnchor:margins.leadingAnchor],
        [stack.trailingAnchor constraintEqualToAnchor:margins.trailingAnchor],
        [stack.topAnchor constraintEqualToAnchor:cell.contentView.topAnchor constant:12],
        [stack.bottomAnchor constraintEqualToAnchor:cell.contentView.bottomAnchor constant:-12]
    ]];
    return cell;
}

- (UILabel *)makeHeader:(NSString *)name {
    UILabel *label = [[UILabel alloc] init];
    label.text = name;
    label.font = [UIFont preferredFontForTextStyle:UIFontTextStyleSubheadline];
    label.adjustsFontForContentSizeCategory = YES;
    label.numberOfLines = 0;
    label.textColor = [UIColor labelColor];
    [label setContentCompressionResistancePriority:UILayoutPriorityDefaultHigh forAxis:UILayoutConstraintAxisHorizontal];
    return label;
}

- (UILabel *)makeBodyLabel {
    UILabel *label = [[DPLabel alloc] init];
    label.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    label.adjustsFontForContentSizeCategory = YES;
    label.numberOfLines = 0;
    return label;
}

- (UILabel *)makeTitleLabel {
    UILabel *titleLabel = [[UILabel alloc] init];
    titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleTitle1];
    titleLabel.adjustsFontForContentSizeCategory = YES;
    titleLabel.numberOfLines = 0;
    return titleLabel;
}

- (void)setTag:(DPTag *)t {
    tag = t;
    [self refreshView];
}

- (void)refreshView {
    // Override in child classes.
}

- (void)setUpRootView:(UIView *)view withScroller:(UIScrollView *)scroller {
    // The scroller spans the safe area so its indicator sits at the screen edge;
    // the content inside follows the readable width, which keeps lines short on
    // iPad and in landscape and still uses the full width on a phone.
    UIView *container = [[UIView alloc] init];
    container.translatesAutoresizingMaskIntoConstraints = NO;
    // A bare view only carries 8pt margins; page content sits on the standard 16pt inset.
    container.directionalLayoutMargins = NSDirectionalEdgeInsetsMake(0, 16, 0, 16);
    view.translatesAutoresizingMaskIntoConstraints = NO;
    scroller.translatesAutoresizingMaskIntoConstraints = NO;
    [container addSubview:view];
    UILayoutGuide *readable = container.readableContentGuide;
    [NSLayoutConstraint activateConstraints:@[
        [view.leadingAnchor constraintEqualToAnchor:readable.leadingAnchor],
        [view.trailingAnchor constraintEqualToAnchor:readable.trailingAnchor],
        [view.topAnchor constraintEqualToAnchor:container.topAnchor constant:4],
        [view.bottomAnchor constraintEqualToAnchor:container.bottomAnchor constant:-4]
    ]];

    NSDictionary *bindings = NSDictionaryOfVariableBindings(container);
    [scroller addSubview:container];
    [scroller addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[container]|"
                                                                     options:0
                                                                     metrics:nil
                                                                       views:bindings]];
    [scroller addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[container]|"
                                                                     options:0
                                                                     metrics:nil
                                                                       views:bindings]];
    [container.widthAnchor constraintEqualToAnchor:scroller.frameLayoutGuide.widthAnchor].active = YES;
    
    id leftGuide = self.view.leftSafeAreaLayoutGuide;
    id rightGuide = self.view.rightSafeAreaLayoutGuide;
    
    [self.view addSubview:scroller];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:[leftGuide][scroller][rightGuide]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(scroller, leftGuide, rightGuide)]];
    [scroller.bottomAnchor constraintEqualToAnchor:self.view.keyboardLayoutGuide.topAnchor].active = YES;
    [scroller.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor].active = YES;
}

@end
