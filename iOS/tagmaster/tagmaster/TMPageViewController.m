#import "TMPageViewController.h"

@interface TMPageSwitcher ()
@property UIStackView *stack;
@property UIVisualEffectView *materialView;
@end
@implementation TMPageSwitcher
- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.backgroundColor = UIColor.clearColor;
        self.materialView = [[UIVisualEffectView alloc] initWithEffect:nil];
        self.materialView.userInteractionEnabled = NO;
        self.materialView.translatesAutoresizingMaskIntoConstraints = NO;
        self.materialView.layer.cornerRadius = 16;
        self.materialView.layer.cornerCurve = kCACornerCurveContinuous;
        self.materialView.clipsToBounds = YES;
        [self addSubview:self.materialView];
        [NSLayoutConstraint activateConstraints:@[
            [self.materialView.leadingAnchor constraintEqualToAnchor:self.leadingAnchor],
            [self.materialView.trailingAnchor constraintEqualToAnchor:self.trailingAnchor],
            [self.materialView.topAnchor constraintEqualToAnchor:self.topAnchor],
            [self.materialView.bottomAnchor constraintEqualToAnchor:self.bottomAnchor]
        ]];
        [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(updateMaterial) name:UIAccessibilityReduceTransparencyStatusDidChangeNotification object:nil];
        [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(updateMaterial) name:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
        [self updateMaterial];
        self.accessibilityIdentifier = @"page-switcher";
        self.isAccessibilityElement = NO;
        self.shouldGroupAccessibilityChildren = YES;
        self.accessibilityContainerType = UIAccessibilityContainerTypeSemanticGroup;
        self.stack = [UIStackView new];
        self.stack.distribution = UIStackViewDistributionFillEqually;
        self.stack.translatesAutoresizingMaskIntoConstraints = NO;
        [self addSubview:self.stack];
        [NSLayoutConstraint activateConstraints:@[
            [self.stack.leadingAnchor constraintEqualToAnchor:self.leadingAnchor],
            [self.stack.trailingAnchor constraintEqualToAnchor:self.trailingAnchor],
            [self.stack.topAnchor constraintEqualToAnchor:self.topAnchor],
            [self.stack.bottomAnchor constraintEqualToAnchor:self.bottomAnchor]
        ]];
    }
    return self;
}
- (void)dealloc { [NSNotificationCenter.defaultCenter removeObserver:self]; }
- (BOOL)reduceTransparencyEnabled { return UIAccessibilityIsReduceTransparencyEnabled(); }
- (BOOL)reduceMotionEnabled { return UIAccessibilityIsReduceMotionEnabled(); }
- (UIVisualEffect *)pageMaterialEffect {
    if ([self reduceTransparencyEnabled]) return nil;
    if (@available(iOS 26.0, *)) {
        UIGlassEffect *glass = [UIGlassEffect effectWithStyle:UIGlassEffectStyleRegular];
        glass.interactive = ![self reduceMotionEnabled];
        return glass;
    }
    return [UIBlurEffect effectWithStyle:UIBlurEffectStyleSystemMaterial];
}
- (void)updateMaterial {
    // No backing plate behind the native material, and no animated accessibility changes.
    self.materialView.effect = [self pageMaterialEffect];
    self.materialView.backgroundColor = [self reduceTransparencyEnabled] ? UIColor.secondarySystemBackgroundColor : UIColor.clearColor;
}
- (NSArray<UIButton *> *)buttons { return (NSArray<UIButton *> *)self.stack.arrangedSubviews; }
- (void)setItems:(NSArray<UITabBarItem *> *)items {
    _items = [items copy];
    for (UIView *view in self.stack.arrangedSubviews) {
        [self.stack removeArrangedSubview:view];
        [view removeFromSuperview];
    }
    for (UITabBarItem *item in items) {
        UIButton *button = [UIButton buttonWithType:UIButtonTypeSystem];
        UIButtonConfiguration *configuration = [UIButtonConfiguration plainButtonConfiguration];
        configuration.title = item.title;
        configuration.image = item.image;
        configuration.imagePlacement = NSDirectionalRectEdgeTop;
        configuration.imagePadding = 4;
        configuration.titleAlignment = UIButtonConfigurationTitleAlignmentCenter;
        configuration.titleLineBreakMode = NSLineBreakByCharWrapping;
        configuration.contentInsets = NSDirectionalEdgeInsetsMake(6, 4, 6, 4);
        configuration.preferredSymbolConfigurationForImage = [UIImageSymbolConfiguration configurationWithPointSize:22];
        UIFont *font = [UIFont preferredFontForTextStyle:UIFontTextStyleCaption1 compatibleWithTraitCollection:self.traitCollection];
        configuration.attributedTitle = [[NSAttributedString alloc] initWithString:item.title attributes:@{NSFontAttributeName: font}];
        button.configuration = configuration;
        button.titleLabel.numberOfLines = 0;
        button.titleLabel.lineBreakMode = NSLineBreakByCharWrapping;
        button.titleLabel.textAlignment = NSTextAlignmentCenter;
        button.titleLabel.adjustsFontForContentSizeCategory = YES;
        button.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleCaption1 compatibleWithTraitCollection:self.traitCollection];
        button.isAccessibilityElement = YES;
        button.accessibilityLabel = item.title;
        button.accessibilityIdentifier = [@"page-" stringByAppendingString:item.title];
        [button addTarget:self action:@selector(activate:) forControlEvents:UIControlEventTouchUpInside];
        [self.stack addArrangedSubview:button];
    }
    [self updateButtons];
}
- (void)setSelectedItem:(UITabBarItem *)selectedItem {
    _selectedItem = selectedItem;
    [self updateButtons];
}
- (void)updateButtons {
    [self.items enumerateObjectsUsingBlock:^(UITabBarItem *item, NSUInteger index, BOOL *stop) {
        UIButton *button = self.buttons[index];
        BOOL selected = item == self.selectedItem;
        button.selected = selected;
        button.enabled = item.enabled;
        // Semantic labels remain readable over adaptive material in either theme.
        // A blue outline and tint mark selection without relying on blue caption text.
        UIButtonConfiguration *configuration = button.configuration;
        configuration.baseForegroundColor = UIColor.labelColor;
        configuration.background.backgroundColor = selected ? [UIColor.systemBlueColor colorWithAlphaComponent:0.12] : UIColor.clearColor;
        configuration.background.strokeColor = selected ? UIColor.systemBlueColor : UIColor.clearColor;
        configuration.background.strokeWidth = selected ? 2 : 0;
        configuration.background.cornerRadius = 12;
        configuration.cornerStyle = UIButtonConfigurationCornerStyleFixed;
        button.configuration = configuration;
        button.accessibilityTraits = UIAccessibilityTraitButton | (selected ? UIAccessibilityTraitSelected : 0);
        button.accessibilityValue = [NSString stringWithFormat:@"%lu of %lu", (unsigned long)index + 1, (unsigned long)self.items.count];
    }];
}
- (void)activate:(UIButton *)button {
    NSUInteger index = [self.buttons indexOfObject:button];
    if (index == NSNotFound) return;
    self.selectedItem = self.items[index];
    [self.delegate tabBar:self didSelectItem:self.selectedItem];
}
- (CGFloat)heightForWidth:(CGFloat)width {
    CGFloat height = 56;
    UIFont *font = [UIFont preferredFontForTextStyle:UIFontTextStyleCaption1 compatibleWithTraitCollection:self.traitCollection];
    for (UIButton *button in self.buttons) {
        UIFont *current = [button.configuration.attributedTitle attribute:NSFontAttributeName atIndex:0 effectiveRange:nil];
        if (![current isEqual:font]) {
            UIButtonConfiguration *configuration = button.configuration;
            configuration.attributedTitle = [[NSAttributedString alloc] initWithString:button.accessibilityLabel attributes:@{NSFontAttributeName: font}];
            button.configuration = configuration;
        }
        // Measure the new font directly. UIButton can retain its pre-trait sizeThatFits
        // cache until its own next layout, especially when only text size changes.
        button.titleLabel.font = font;
        NSMutableParagraphStyle *paragraph = [NSMutableParagraphStyle new];
        paragraph.lineBreakMode = NSLineBreakByCharWrapping;
        CGRect text = [button.accessibilityLabel boundingRectWithSize:CGSizeMake(MAX(1, width / MAX(1, self.buttons.count) - 8), CGFLOAT_MAX)
            options:NSStringDrawingUsesLineFragmentOrigin | NSStringDrawingUsesFontLeading
            attributes:@{NSFontAttributeName:font, NSParagraphStyleAttributeName:paragraph} context:nil];
        height = MAX(height, ceil(text.size.height) + 22 + 4 + 12);
    }
    return height;
}
@end

@interface TMPageViewController () <TMPageSwitcherDelegate>
@property (nonatomic, strong) TMPageSwitcher *tabBar;
@property (nonatomic, strong) UIView *rootView;
@property UIViewController *visiblePage;
@property NSLayoutConstraint *switcherHeight;
@property BOOL appeared;
@end
@implementation TMPageViewController
- (void)viewDidLoad {
    [super viewDidLoad];
    self.rootView = [UIView new];
    self.tabBar = [TMPageSwitcher new];
    self.tabBar.delegate = self;
    self.rootView.translatesAutoresizingMaskIntoConstraints = NO;
    self.tabBar.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.rootView];
    [self.view addSubview:self.tabBar];
    UIView *bottom = [UIView new];
    bottom.backgroundColor = UIColor.clearColor;
    bottom.userInteractionEnabled = NO;
    bottom.translatesAutoresizingMaskIntoConstraints = NO;
    bottom.isAccessibilityElement = NO;
    [self.view insertSubview:bottom belowSubview:self.tabBar];
    UILayoutGuide *safe = self.view.safeAreaLayoutGuide;
    self.switcherHeight = [self.tabBar.heightAnchor constraintEqualToConstant:56];
    [NSLayoutConstraint activateConstraints:@[
        [self.rootView.topAnchor constraintEqualToAnchor:safe.topAnchor],
        [self.rootView.leadingAnchor constraintEqualToAnchor:safe.leadingAnchor],
        [self.rootView.trailingAnchor constraintEqualToAnchor:safe.trailingAnchor],
        [self.rootView.bottomAnchor constraintEqualToAnchor:self.tabBar.topAnchor],
        [self.tabBar.leadingAnchor constraintEqualToAnchor:safe.leadingAnchor],
        [self.tabBar.trailingAnchor constraintEqualToAnchor:safe.trailingAnchor],
        [self.tabBar.bottomAnchor constraintEqualToAnchor:safe.bottomAnchor],
        self.switcherHeight,
        [bottom.topAnchor constraintEqualToAnchor:self.tabBar.bottomAnchor],
        [bottom.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
        [bottom.leadingAnchor constraintEqualToAnchor:safe.leadingAnchor],
        [bottom.trailingAnchor constraintEqualToAnchor:safe.trailingAnchor]
    ]];
    [self.view layoutIfNeeded];
    [self rebuildItems];
}
- (void)traitCollectionDidChange:(UITraitCollection *)previousTraitCollection {
    [super traitCollectionDidChange:previousTraitCollection];
    [self.view setNeedsLayout];
}
- (void)viewWillLayoutSubviews {
    [super viewWillLayoutSubviews];
    CGFloat height = [self.tabBar heightForWidth:self.view.safeAreaLayoutGuide.layoutFrame.size.width];
    if (self.switcherHeight.constant != height) self.switcherHeight.constant = height;
}
- (void)setViewControllers:(NSArray<UIViewController *> *)controllers {
    UIViewController *chosen = self.visiblePage;
    _viewControllers = [controllers copy];
    NSUInteger index = [_viewControllers indexOfObject:chosen];
    _selectedIndex = index == NSNotFound ? MIN(_selectedIndex, MAX(1, _viewControllers.count) - 1) : index;
    if (self.isViewLoaded) [self rebuildItems];
}
- (void)rebuildItems {
    NSMutableArray *items = [NSMutableArray array];
    for (UIViewController *controller in self.viewControllers) [items addObject:controller.tabBarItem];
    self.tabBar.items = items;
    [self showSelectedPage];
}
- (void)setSelectedIndex:(NSUInteger)selectedIndex {
    if (self.viewControllers.count && selectedIndex >= self.viewControllers.count) return;
    _selectedIndex = selectedIndex;
    if (self.isViewLoaded) [self showSelectedPage];
}
- (void)tabBar:(TMPageSwitcher *)tabBar didSelectItem:(UITabBarItem *)item {
    NSUInteger index = [tabBar.items indexOfObject:item];
    if (index != NSNotFound) self.selectedIndex = index;
}
- (void)showSelectedPage {
    UIViewController *next = self.viewControllers.count ? self.viewControllers[self.selectedIndex] : nil;
    self.tabBar.selectedItem = next.tabBarItem;
    if (next == self.visiblePage) return;
    UIViewController *previous = self.visiblePage;
    [previous willMoveToParentViewController:nil];
    if (self.appeared) [previous beginAppearanceTransition:NO animated:NO];
    [previous.viewIfLoaded removeFromSuperview];
    if (self.appeared) [previous endAppearanceTransition];
    [previous removeFromParentViewController];
    self.visiblePage = next;
    if (!next) return;
    [self addChildViewController:next];
    next.view.frame = self.rootView.bounds;
    next.view.translatesAutoresizingMaskIntoConstraints = NO;
    [self.rootView addSubview:next.view];
    [NSLayoutConstraint activateConstraints:@[
        [next.view.topAnchor constraintEqualToAnchor:self.rootView.topAnchor],
        [next.view.bottomAnchor constraintEqualToAnchor:self.rootView.bottomAnchor],
        [next.view.leadingAnchor constraintEqualToAnchor:self.rootView.leadingAnchor],
        [next.view.trailingAnchor constraintEqualToAnchor:self.rootView.trailingAnchor]
    ]];
    [next didMoveToParentViewController:self];
    if (self.appeared) {
        [next beginAppearanceTransition:YES animated:NO];
        [next endAppearanceTransition];
    }
}
- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    self.appeared = YES;
}
- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    self.appeared = NO;
}
@end
