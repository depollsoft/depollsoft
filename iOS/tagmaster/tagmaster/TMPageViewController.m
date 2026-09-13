#import "TMPageViewController.h"

@interface TMPageSwitcher ()
@property UIStackView *stack;
@end
@implementation TMPageSwitcher
- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.backgroundColor = [UIColor colorWithWhite:55.0 / 255.0 alpha:1];
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
        // Keep blue selection on charcoal, with a non-color selected trait as well.
        // Match the bright-blue indicator on the Android charcoal bar. System blue
        // alone is only 3.26:1 here, insufficient for the small tab captions.
        UIColor *color = selected ? [UIColor colorWithRed:90.0 / 255.0 green:200.0 / 255.0 blue:250.0 / 255.0 alpha:1] : UIColor.whiteColor;
        UIButtonConfiguration *configuration = button.configuration;
        configuration.baseForegroundColor = color;
        configuration.background.backgroundColor = selected ? [color colorWithAlphaComponent:0.14] : UIColor.clearColor;
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
        height = MAX(height, [button sizeThatFits:CGSizeMake(width / MAX(1, self.buttons.count), CGFLOAT_MAX)].height);
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
    bottom.backgroundColor = self.tabBar.backgroundColor;
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
