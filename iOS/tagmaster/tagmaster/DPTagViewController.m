//
//  DPTagViewController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagViewController.h"
#import "DPBarbershop.h"
#import "DPTagSummaryController.h"
#import "DPTagDetailController.h"
#import "DPTagTracksController.h"
#import "DPTagVideoController.h"
#import "DPAppDelegate.h"
#import <MessageUI/MessageUI.h>

// Decorative vector notation. No timers, assets, audio or accessibility children.
@interface TMQuartetStaffView : UIView
@property (nonatomic, copy) NSArray<CAShapeLayer *> *notes;
@property (nonatomic) BOOL animationAllowed;
- (void)updateMotion;
@end

@implementation TMQuartetStaffView
- (instancetype)initWithFrame:(CGRect)frame {
    if ((self = [super initWithFrame:frame])) {
        self.backgroundColor = UIColor.clearColor;
        self.accessibilityElementsHidden = YES;
        NSMutableArray *notes = [NSMutableArray array];
        for (NSUInteger i = 0; i < 4; i++) {
            CAShapeLayer *note = [CAShapeLayer layer];
            [self.layer addSublayer:note];
            [notes addObject:note];
        }
        self.notes = notes;
    }
    return self;
}
- (CGSize)intrinsicContentSize { return CGSizeMake(204, 88); }
- (void)drawRect:(CGRect)rect {
    [UIColor.separatorColor setStroke];
    UIBezierPath *staff = [UIBezierPath bezierPath];
    staff.lineWidth = 1;
    for (NSUInteger i = 0; i < 5; i++) {
        CGFloat y = 26 + i * 10;
        [staff moveToPoint:CGPointMake(0, y)];
        [staff addLineToPoint:CGPointMake(self.bounds.size.width, y)];
    }
    [staff stroke];
}
- (void)layoutSubviews {
    [super layoutSubviews];
    const CGFloat heights[] = {46, 36, 41, 56};
    [CATransaction begin];
    [CATransaction setDisableActions:YES];
    for (NSUInteger i = 0; i < self.notes.count; i++) {
        CGFloat x = self.bounds.size.width * (0.2 + i * 0.2);
        CGFloat y = heights[i];
        UIBezierPath *path = [UIBezierPath bezierPathWithOvalInRect:CGRectMake(x - 7, y - 4.5, 14, 9)];
        [path appendPath:[UIBezierPath bezierPathWithRoundedRect:CGRectMake(x + 5, y - 29, 2, 29) cornerRadius:1]];
        self.notes[i].path = path.CGPath;
        self.notes[i].fillColor = [UIColor.systemBlueColor resolvedColorWithTraitCollection:self.traitCollection].CGColor;
    }
    [CATransaction commit];
}
- (void)setAnimationAllowed:(BOOL)allowed {
    _animationAllowed = allowed;
    [self updateMotion];
}
- (void)setHidden:(BOOL)hidden {
    [super setHidden:hidden];
    [self updateMotion];
}
- (void)didMoveToWindow {
    [super didMoveToWindow];
    [self updateMotion];
}
- (BOOL)reduceMotionEnabled { return UIAccessibilityIsReduceMotionEnabled(); }
- (void)updateMotion {
    BOOL animate = self.animationAllowed && self.window && !self.hidden && ![self reduceMotionEnabled];
    for (NSUInteger i = 0; i < self.notes.count; i++) {
        CAShapeLayer *note = self.notes[i];
        if (!animate) {
            [note removeAllAnimations];
        } else if (![note animationForKey:@"gather"]) {
            CAKeyframeAnimation *motion = [CAKeyframeAnimation animationWithKeyPath:@"transform.translation.y"];
            motion.values = @[@0, @(-4), @0, @0];
            motion.keyTimes = @[@0, @0.2, @0.4, @1];
            motion.timingFunctions = @[[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut],
                                      [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut],
                                      [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionLinear]];
            motion.duration = 2.4;
            motion.beginTime = CACurrentMediaTime() + i * 0.16;
            motion.repeatCount = HUGE_VALF;
            [note addAnimation:motion forKey:@"gather"];
        }
    }
}
@end

@interface DPTagViewController () <UIActionSheetDelegate, MFMessageComposeViewControllerDelegate, MFMailComposeViewControllerDelegate>

@property (nonatomic, strong) DPTag *tag;

@property (nonatomic, strong) DPTagSummaryController *summaryController;
@property (nonatomic, strong) DPTagDetailController *detailController;
@property (nonatomic, strong) DPTagTracksController *tagTracksController;
@property (nonatomic, strong) DPTagVideoController *tagVideoController;

@property (nonatomic, strong) UIBarButtonItem *actionBarButton;
@property (nonatomic, strong) UIBarButtonItem *shareBarButton;
@property (nonatomic, strong) UIBarButtonItem *refreshBarButton;
@property (nonatomic, strong) UIBarButtonItem *loadingBarButton;

@property (nonatomic, strong) TMBusyIndicator *busyIndicator;
@property (nonatomic) BOOL tagFetchPending;
@property (nonatomic) BOOL loadFailed;
@property (nonatomic) BOOL lastFetchWasRefresh;
@property (nonatomic) NSUInteger requestGeneration;
@property (nonatomic) NSUInteger announcedGeneration;
@property (nonatomic) BOOL screenVisible;
@property (nonatomic) BOOL applicationActive;
@property (nonatomic, strong) UIView *initialLoadingView;
@property (nonatomic, strong) TMQuartetStaffView *quartetStaff;
@property (nonatomic, strong) UILabel *loadingHeading;
@property (nonatomic, strong) UILabel *loadingStatus;
@property (nonatomic, strong) UIButton *retryButton;

@end

@implementation DPTagViewController

@synthesize tagId, tag;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        [self commonInit];
    }
    return self;
}

- (id)init {
    if (self = [super init]) {
        [self commonInit];
    }
    return self;
}

- (void)commonInit {
    // Counts in-flight work; progress is shown inline so navigation stays usable.
    self.busyIndicator = [[TMBusyIndicator alloc] init];
    __weak DPTagViewController *weakSelf = self;
    self.busyIndicator.onBusyCountChanged = ^(NSUInteger busyCount) {
        [weakSelf updateLoadingState];
    };
}

- (void)setTagId:(int)tId {
    if (tagId == tId && self.tagFetchPending) return;
    if (tagId != tId) {
        // Old work may finish, but it no longer owns this screen.
        self.requestGeneration++;
        self.tagFetchPending = NO;
        if (self.tag) self.tag = nil;
    }
    tagId = tId;
    [self loadTag:NO];
}

// Keep the synchronous catalog/cache contract off the main thread. Completion is on main.
- (void)fetchTagId:(int)identifier refresh:(BOOL)refresh completion:(void (^)(DPTag *))completion {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        DPTag *loaded = nil;
        @try { loaded = [DPTag loadTagById:identifier refresh:refresh]; }
        @catch (NSException *exception) { /* Present a useful, non-diagnostic recovery state. */ }
        dispatch_async(dispatch_get_main_queue(), ^{ completion(loaded); });
    });
}

- (void)loadTag:(BOOL)refresh {
    if (self.tagFetchPending) return;
    const int identifier = self.tagId;
    const NSUInteger generation = ++self.requestGeneration;
    self.tagFetchPending = YES;
    self.loadFailed = NO;
    self.lastFetchWasRefresh = refresh;
    TMBusyIndicator *busy = self.busyIndicator;
    [busy incrementBusyCount];
    [self updateLoadingState];
    __weak DPTagViewController *weakSelf = self;
    [self fetchTagId:identifier refresh:refresh completion:^(DPTag *loaded) {
        DPTagViewController *controller = weakSelf;
        BOOL current = controller && controller.requestGeneration == generation && controller.tagId == identifier;
        if (current) {
            controller.tagFetchPending = NO;
            controller.loadFailed = loaded == nil;
            if (loaded) controller.tag = loaded;
        }
        // Balance even when the controller has gone away or another request owns the screen.
        [busy decrementBusyCount];
        if (!current) return;
        [controller updateLoadingState];
        if (!loaded && controller.tag && controller.screenVisible) {
            [controller tm_showError:@"The tag couldn't be refreshed. Check your connection and try again. Your saved tags are unchanged." retry:^{
                DPTagViewController *retryController = weakSelf;
                if (retryController.requestGeneration == generation && retryController.tagId == identifier) {
                    [retryController loadTag:refresh];
                }
            }];
        }
    }];
}

- (void)retryInitialTag {
    [self loadTag:self.lastFetchWasRefresh];
}

- (void)updateLoadingState {
    if (!self.isViewLoaded || !self.initialLoadingView) return;
    BOOL empty = self.tag == nil;
    BOOL initialPending = empty && self.tagFetchPending;
    self.rootView.hidden = empty;
    self.rootView.accessibilityElementsHidden = empty;
    self.tabBar.hidden = empty;
    self.tabBar.accessibilityElementsHidden = empty;
    self.initialLoadingView.hidden = !empty;
    self.quartetStaff.hidden = !initialPending;
    self.loadingHeading.text = self.loadFailed ? @"Tag unavailable" : @"Gathering the quartet…";
    self.loadingStatus.text = self.loadFailed
        ? [NSString stringWithFormat:@"Couldn't load tag %d. Check your connection and tag ID, then try again.", self.tagId]
        : (initialPending ? [NSString stringWithFormat:@"Loading tag %d…", self.tagId] : @"Choose a tag to get started.");
    self.loadingStatus.accessibilityLabel = initialPending
        ? [NSString stringWithFormat:@"%@ %@", self.loadingHeading.text, self.loadingStatus.text]
        : self.loadingStatus.text;
    self.retryButton.hidden = !self.loadFailed;
    BOOL busy = self.busyIndicator.busyCount > 0;
    UIActivityIndicatorView *spinner = (UIActivityIndicatorView *)self.loadingBarButton.customView;
    if (busy && !empty) [spinner startAnimating]; else [spinner stopAnimating];
    self.navigationItem.rightBarButtonItems = empty ? @[] : @[self.shareBarButton, self.actionBarButton,
        busy ? self.loadingBarButton : self.refreshBarButton];
    self.quartetStaff.animationAllowed = initialPending && self.screenVisible && self.applicationActive;
    if (initialPending && self.screenVisible && self.applicationActive && self.announcedGeneration != self.requestGeneration) {
        self.announcedGeneration = self.requestGeneration;
        UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, self.loadingStatus);
    }
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    self.screenVisible = YES;
    self.applicationActive = UIApplication.sharedApplication.applicationState == UIApplicationStateActive;
    [self updateLoadingState];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    self.screenVisible = NO;
    self.quartetStaff.animationAllowed = NO;
}

- (void)loadingEnvironmentChanged:(NSNotification *)notification {
    if ([notification.name isEqualToString:UIApplicationWillResignActiveNotification]) self.applicationActive = NO;
    if ([notification.name isEqualToString:UIApplicationDidBecomeActiveNotification]) self.applicationActive = YES;
    [self updateLoadingState];
}

- (void)dealloc {
    [NSNotificationCenter.defaultCenter removeObserver:self];
}

- (void)setTag:(DPTag *)t {
    tag = t;
    
    self.title = t.title ?: @"Tag";
    self.shareBarButton.enabled = t != nil;
    self.actionBarButton.enabled = t != nil;
    for (DPTagPageControllerBase *page in self.viewControllers) {
        page.tag = t;
    }
    [self updateLoadingState];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
    [DPAppDelegate setUpBackground:self.view];
    NSMutableArray *controllers = [NSMutableArray array];
    
    self.summaryController = [[DPTagSummaryController alloc] init];
    self.summaryController.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Summary" image:[UIImage systemImageNamed:@"doc.text"] tag:0];
    self.summaryController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.summaryController];
    
    self.detailController = [[DPTagDetailController alloc] init];
    self.detailController.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Details" image:[UIImage systemImageNamed:@"info.circle"] tag:1];
    self.detailController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.detailController];
    
    self.tagTracksController = [[DPTagTracksController alloc] init];
    self.tagTracksController.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Tracks" image:[UIImage systemImageNamed:@"waveform"] tag:2];
    self.tagTracksController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.tagTracksController];
    
    self.tagVideoController = [[DPTagVideoController alloc] init];
    self.tagVideoController.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Videos" image:[UIImage systemImageNamed:@"play.rectangle"] tag:3];
    self.tagVideoController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.tagVideoController];
    
    self.viewControllers = controllers;
    
    self.shareBarButton = [DPAppDelegate barButtonItemWithSystemName:@"square.and.arrow.up"
                                                              target:self
                                                              action:@selector(sendTag)];
    self.actionBarButton = [DPAppDelegate barButtonItemWithSystemName:@"tag"
                                                               target:self
                                                               action:@selector(showActions)];
    self.refreshBarButton = [DPAppDelegate barButtonItemWithSystemName:@"arrow.clockwise"
                                                                target:self
                                                                action:@selector(refreshTag)];
    UIActivityIndicatorView *spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleMedium];
    spinner.hidesWhenStopped = NO;
    self.loadingBarButton = [[UIBarButtonItem alloc] initWithCustomView:spinner];
    self.loadingBarButton.accessibilityLabel = @"Loading";
    self.loadingBarButton.accessibilityTraits = UIAccessibilityTraitStaticText;
    [self installInitialLoadingView];
    for (NSString *name in @[UIAccessibilityReduceMotionStatusDidChangeNotification,
                             UIApplicationWillResignActiveNotification, UIApplicationDidBecomeActiveNotification]) {
        [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(loadingEnvironmentChanged:) name:name object:nil];
    }
    [self setTag:self.tag];
}

- (void)installInitialLoadingView {
    UIView *overlay = [UIView new];
    overlay.translatesAutoresizingMaskIntoConstraints = NO;
    overlay.backgroundColor = UIColor.systemBackgroundColor;
    overlay.accessibilityIdentifier = @"tag.initialLoading";
    [self.view addSubview:overlay];
    self.initialLoadingView = overlay;
    UIScrollView *scroll = [UIScrollView new];
    scroll.translatesAutoresizingMaskIntoConstraints = NO;
    [overlay addSubview:scroll];
    UIView *content = [UIView new];
    content.translatesAutoresizingMaskIntoConstraints = NO;
    [scroll addSubview:content];
    self.quartetStaff = [TMQuartetStaffView new];
    self.loadingHeading = [UILabel new];
    self.loadingHeading.font = [UIFont preferredFontForTextStyle:UIFontTextStyleTitle3];
    self.loadingHeading.textColor = UIColor.labelColor;
    self.loadingHeading.isAccessibilityElement = NO;
    self.loadingStatus = [UILabel new];
    self.loadingStatus.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    // SecondaryLabel's translucent gray falls below 4.5:1 on white at body size.
    self.loadingStatus.textColor = [UIColor colorWithDynamicProvider:^UIColor *(UITraitCollection *traits) {
        return traits.userInterfaceStyle == UIUserInterfaceStyleDark ? UIColor.secondaryLabelColor : UIColor.darkGrayColor;
    }];
    self.loadingStatus.accessibilityIdentifier = @"tag.loadingStatus";
    for (UILabel *label in @[self.loadingHeading, self.loadingStatus]) {
        label.adjustsFontForContentSizeCategory = YES;
        label.numberOfLines = 0;
        label.textAlignment = NSTextAlignmentCenter;
    }
    self.retryButton = [UIButton buttonWithType:UIButtonTypeSystem];
    [self.retryButton setTitle:@"Retry" forState:UIControlStateNormal];
    self.retryButton.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    self.retryButton.titleLabel.adjustsFontForContentSizeCategory = YES;
    [self.retryButton addTarget:self action:@selector(retryInitialTag) forControlEvents:UIControlEventTouchUpInside];
    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[self.quartetStaff, self.loadingHeading, self.loadingStatus, self.retryButton]];
    stack.axis = UILayoutConstraintAxisVertical;
    stack.alignment = UIStackViewAlignmentCenter;
    stack.spacing = 8;
    [stack setCustomSpacing:20 afterView:self.quartetStaff];
    stack.translatesAutoresizingMaskIntoConstraints = NO;
    [content addSubview:stack];
    NSLayoutConstraint *height = [content.heightAnchor constraintEqualToAnchor:scroll.frameLayoutGuide.heightAnchor];
    height.priority = UILayoutPriorityDefaultLow;
    [NSLayoutConstraint activateConstraints:@[
        [overlay.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [overlay.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [overlay.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [overlay.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
        [scroll.leadingAnchor constraintEqualToAnchor:overlay.safeAreaLayoutGuide.leadingAnchor],
        [scroll.trailingAnchor constraintEqualToAnchor:overlay.safeAreaLayoutGuide.trailingAnchor],
        [scroll.topAnchor constraintEqualToAnchor:overlay.safeAreaLayoutGuide.topAnchor],
        [scroll.bottomAnchor constraintEqualToAnchor:overlay.safeAreaLayoutGuide.bottomAnchor],
        [content.leadingAnchor constraintEqualToAnchor:scroll.contentLayoutGuide.leadingAnchor],
        [content.trailingAnchor constraintEqualToAnchor:scroll.contentLayoutGuide.trailingAnchor],
        [content.topAnchor constraintEqualToAnchor:scroll.contentLayoutGuide.topAnchor],
        [content.bottomAnchor constraintEqualToAnchor:scroll.contentLayoutGuide.bottomAnchor],
        [content.widthAnchor constraintEqualToAnchor:scroll.frameLayoutGuide.widthAnchor], height,
        [stack.centerXAnchor constraintEqualToAnchor:content.centerXAnchor],
        [stack.centerYAnchor constraintEqualToAnchor:content.centerYAnchor],
        [stack.topAnchor constraintGreaterThanOrEqualToAnchor:content.topAnchor constant:24],
        [stack.bottomAnchor constraintLessThanOrEqualToAnchor:content.bottomAnchor constant:-24],
        [stack.widthAnchor constraintEqualToAnchor:content.widthAnchor constant:-48],
        [self.quartetStaff.widthAnchor constraintEqualToConstant:204],
        [self.loadingHeading.widthAnchor constraintLessThanOrEqualToAnchor:stack.widthAnchor],
        [self.loadingStatus.widthAnchor constraintLessThanOrEqualToAnchor:stack.widthAnchor],
        [self.retryButton.heightAnchor constraintGreaterThanOrEqualToConstant:44],
        [self.retryButton.widthAnchor constraintGreaterThanOrEqualToConstant:80]
    ]];
}

- (void)showActions {
    UIAlertController *actions = [UIAlertController alertControllerWithTitle:nil
                                                                     message:nil
                                                              preferredStyle:UIAlertControllerStyleActionSheet];
    actions.popoverPresentationController.barButtonItem = self.actionBarButton;
    
    if (![DPAppDelegate containsFavorite:self.tagId]) {
        [actions addAction:[UIAlertAction actionWithTitle:@"Add Favorite"
                                                    style:UIAlertActionStyleDefault
                                                  handler:^(UIAlertAction * _Nonnull action) {
            [DPAppDelegate addFavorite:self.tagId];
        }]];
    } else {
        [actions addAction:[UIAlertAction actionWithTitle:@"Remove Favorite"
                                                    style:UIAlertActionStyleDefault
                                                  handler:^(UIAlertAction * _Nonnull action) {
            [DPAppDelegate removeFavorite:self.tagId];
        }]];
    }
    if (![DPAppDelegate containsTeachable:self.tagId]) {
        [actions addAction:[UIAlertAction actionWithTitle:@"Mark as Teachable"
                                                    style:UIAlertActionStyleDefault
                                                  handler:^(UIAlertAction * _Nonnull action) {
            [DPAppDelegate addTeachable:self.tagId];
        }]];
    } else {
        [actions addAction:[UIAlertAction actionWithTitle:@"Unmark as Teachable"
                                                    style:UIAlertActionStyleDefault
                                                  handler:^(UIAlertAction * _Nonnull action) {
            [DPAppDelegate removeTeachable:self.tagId];
        }]];
    }
    
    [actions addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                style:UIAlertActionStyleCancel
                                              handler:nil]];
    
    [self presentViewController:actions animated:YES completion:nil];
}

- (void)sendTag {
    if (!self.tag) return;
    NSString *string = [NSString stringWithFormat:@"%@ - Tag Master for iOS", self.tag.title];
    NSURL *url = self.tag.tagUri;
    UIActivityViewController *activityController = [[UIActivityViewController alloc] initWithActivityItems:@[string, url] applicationActivities:nil];
    activityController.popoverPresentationController.barButtonItem = self.shareBarButton;
    [self presentViewController:activityController animated:YES completion:nil];
}

- (void)refreshTag {
    if (self.busyIndicator.busyCount > 0) return;
    [self loadTag:YES];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)messageComposeViewController:(MFMessageComposeViewController *)controller didFinishWithResult:(MessageComposeResult)result {
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)mailComposeController:(MFMailComposeViewController *)controller didFinishWithResult:(MFMailComposeResult)result error:(NSError *)error {
    [self dismissViewControllerAnimated:YES completion:nil];
}

@end
