//
//  DPTagViewController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagViewController.h"
#import "TMBarberPoleLoadingView.h"
#import "TMQuartetArtwork.h"
#import "TMQuartetStaffView.h"
#import "DPBarbershop.h"
#import "DPTagSummaryController.h"
#import "DPTagDetailController.h"
#import "DPTagTracksController.h"
#import "DPTagVideoController.h"
#import "DPAppDelegate.h"
#import "tagmaster-Swift.h"
#import <MessageUI/MessageUI.h>

@interface DPTagViewController () <UIActionSheetDelegate, MFMessageComposeViewControllerDelegate, MFMailComposeViewControllerDelegate, UIScrollViewDelegate>

@property (nonatomic, strong) DPTag *tag;

@property (nonatomic, strong) DPTagSummaryController *summaryController;
@property (nonatomic, strong) DPTagDetailController *detailController;
@property (nonatomic, strong) DPTagTracksController *tagTracksController;
@property (nonatomic, strong) DPTagVideoController *tagVideoController;

@property (nonatomic, strong) UIBarButtonItem *actionBarButton;
@property (nonatomic, strong) UIBarButtonItem *shareBarButton;
@property (nonatomic, strong) UIBarButtonItem *refreshBarButton;
@property (nonatomic, strong) UIBarButtonItem *loadingBarButton;
@property (nonatomic, strong) UIBarButtonItem *favoriteBarButton;
@property (nonatomic, strong) UIBarButtonItem *teachableBarButton;
@property (nonatomic, strong) UIBarButtonItem *addToListBarButton;
@property (nonatomic, strong) UIBarButtonItem *previousTagBarButton;
@property (nonatomic, strong) UIBarButtonItem *nextTagBarButton;

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

@property (nonatomic, readonly) BOOL hasPreviousTag;
@property (nonatomic, readonly) BOOL hasNextTag;
- (void)stepToPreviousTag;
- (void)stepToNextTag;

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
    if (tagId == tId) return;
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
    // Let the native controller update child safe areas as the bar returns.
    // Setting UITabBar.hidden directly can leave content underneath the floating bar.
    if (@available(iOS 18.0, *)) {
        [self.pageTabController setTabBarHidden:empty animated:NO];
    } else {
        self.tabBar.hidden = empty;
    }
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
    BOOL busy = self.tagFetchPending; // Row/button work has its own inline indicator.
    TMBarberPoleLoadingView *spinner = (TMBarberPoleLoadingView *)self.loadingBarButton.customView;
    spinner.controllerVisible = self.screenVisible;
    if (busy && !empty) [spinner startAnimating]; else [spinner stopAnimating];
    self.previousTagBarButton.enabled = [self hasPreviousTag];
    self.nextTagBarButton.enabled = [self hasNextTag];
    BOOL expanded = self.splitViewController != nil && !self.splitViewController.isCollapsed;
    BOOL showSteppers = self.source != nil && expanded;
    [self refreshSavedStateButtons];
    NSMutableArray<UIBarButtonItem *> *rightItems = [NSMutableArray array];
    // UIKit renders the first item in this array farthest out. Beside a list the bar reads,
    // left to right, the way Android's detail pane does: previous, next, favorite, teachable,
    // refresh, share. Collapsed (iPhone) keeps share, the tag action sheet, refresh.
    if (!empty) {
        [rightItems addObjectsFromArray:expanded
            ? @[self.shareBarButton, busy ? self.loadingBarButton : self.refreshBarButton,
                self.addToListBarButton, self.teachableBarButton, self.favoriteBarButton]
            : @[self.shareBarButton, self.actionBarButton, busy ? self.loadingBarButton : self.refreshBarButton]];
    }
    if (showSteppers) [rightItems addObjectsFromArray:@[self.nextTagBarButton, self.previousTagBarButton]];
    if (![self.navigationItem.rightBarButtonItems isEqualToArray:rightItems]) {
        self.navigationItem.rightBarButtonItems = rightItems;
    }
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
    ((TMBarberPoleLoadingView *)self.loadingBarButton.customView).controllerVisible = NO;
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

- (void)setSource:(id<TMTagListSource>)source {
    _source = source;
    [self updateLoadingState];
}

- (BOOL)canStepThroughSource {
    return self.source && self.splitViewController && !self.splitViewController.isCollapsed;
}

#pragma mark - Stepping to a neighbouring tag

- (NSInteger)indexOfCurrentTagInSource {
    if (!self.source) return NSNotFound;
    return [[self.source tm_listedTagIds] indexOfObject:@(self.tagId)];
}

- (BOOL)hasPreviousTag {
    NSInteger index = [self indexOfCurrentTagInSource];
    return index != NSNotFound && index > 0;
}

- (BOOL)hasNextTag {
    NSInteger index = [self indexOfCurrentTagInSource];
    if (index == NSNotFound) return NO;
    return (NSUInteger)(index + 1) < [self.source tm_listedTagIds].count;
}

- (void)stepBy:(NSInteger)delta {
    id<TMTagListSource> source = self.source;
    if (![self canStepThroughSource]) return;
    NSArray<NSNumber *> *ids = [source tm_listedTagIds] ?: @[];
    NSUInteger index = [ids indexOfObject:@(self.tagId)];
    if (index == NSNotFound) return;
    NSInteger newIndex = (NSInteger)index + delta;
    if (newIndex < 0 || (NSUInteger)newIndex >= ids.count) return;
    int newTagId = ids[(NSUInteger)newIndex].intValue;
    [DPAppDelegate showTagWithId:newTagId from:self];
    [self updateLoadingState];
}

- (void)stepToPreviousTag {
    [self stepBy:-1];
}

- (void)stepToNextTag {
    [self stepBy:1];
}

- (void)sourceListMayHaveChanged:(NSNotification *)notification {
    if (!self.source) return;
    if ([notification.name isEqualToString:TMTagListDidChangeNotification] && notification.object != self.source) return;
    [self updateLoadingState];
}

- (BOOL)canPerformAction:(SEL)action withSender:(id)sender {
    if (action == @selector(stepToPreviousTag)) return [self canStepThroughSource] && self.hasPreviousTag;
    if (action == @selector(stepToNextTag)) return [self canStepThroughSource] && self.hasNextTag;
    return [super canPerformAction:action withSender:sender];
}

- (NSArray<UIKeyCommand *> *)keyCommands {
    if (![self canStepThroughSource]) return super.keyCommands;
    UIKeyCommand *previous = [UIKeyCommand keyCommandWithInput:UIKeyInputUpArrow
                                                  modifierFlags:UIKeyModifierCommand
                                                         action:@selector(stepToPreviousTag)];
    previous.discoverabilityTitle = @"Previous Tag";
    UIKeyCommand *next = [UIKeyCommand keyCommandWithInput:UIKeyInputDownArrow
                                              modifierFlags:UIKeyModifierCommand
                                                     action:@selector(stepToNextTag)];
    next.discoverabilityTitle = @"Next Tag";
    return [(@[previous, next]) arrayByAddingObjectsFromArray:super.keyCommands ?: @[]];
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
    self.favoriteBarButton = [[UIBarButtonItem alloc] initWithImage:nil style:UIBarButtonItemStylePlain
                                                             target:self action:@selector(toggleFavorite)];
    self.teachableBarButton = [[UIBarButtonItem alloc] initWithImage:nil style:UIBarButtonItemStylePlain
                                                              target:self action:@selector(toggleTeachable)];
    self.addToListBarButton = [DPAppDelegate barButtonItemWithSystemName:@"text.badge.plus"
                                                                   target:self
                                                                   action:@selector(showListPicker)];
    [self refreshSavedStateButtons];
    self.previousTagBarButton = [DPAppDelegate barButtonItemWithSystemName:@"chevron.up"
                                                                     target:self
                                                                     action:@selector(stepToPreviousTag)];
    self.nextTagBarButton = [DPAppDelegate barButtonItemWithSystemName:@"chevron.down"
                                                                 target:self
                                                                 action:@selector(stepToNextTag)];
    TMBarberPoleLoadingView *spinner = [[TMBarberPoleLoadingView alloc] initWithOperationName:@"Refreshing tag"];
    spinner.darkSurface = YES;
    self.loadingBarButton = [[UIBarButtonItem alloc] initWithCustomView:spinner];
    if (@available(iOS 26.0, *)) {
        // Keep light metal on charcoal, not inside the adjacent actions' pale glass fill.
        self.loadingBarButton.sharesBackground = NO;
        self.loadingBarButton.hidesSharedBackground = YES;
    }
    self.loadingBarButton.accessibilityLabel = @"Refreshing tag";
    self.loadingBarButton.accessibilityTraits = UIAccessibilityTraitStaticText;
    [self installInitialLoadingView];
    for (NSString *name in @[UIAccessibilityReduceMotionStatusDidChangeNotification,
                             UIApplicationWillResignActiveNotification, UIApplicationDidBecomeActiveNotification]) {
        [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(loadingEnvironmentChanged:) name:name object:nil];
    }
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(loadingEnvironmentChanged:) name:TMTagSelectionDidChangeNotification object:nil];
    // A source's list can change underneath this screen (a query page finishes
    // loading, favorites/teachable are edited): recheck stepper enablement.
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(sourceListMayHaveChanged:) name:TMTagListDidChangeNotification object:nil];
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(sourceListMayHaveChanged:) name:@"tagmaster.userDataChanged" object:nil];
    [self setTag:self.tag];
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    [self.quartetStaff updateMotion];
}

- (void)installInitialLoadingView {
    UIView *overlay = [UIView new];
    overlay.translatesAutoresizingMaskIntoConstraints = NO;
    overlay.backgroundColor = UIColor.systemBackgroundColor;
    overlay.accessibilityIdentifier = @"tag.initialLoading";
    [self.view addSubview:overlay];
    self.initialLoadingView = overlay;
    UIScrollView *scroll = [UIScrollView new];
    scroll.delegate = self;
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
        [self.quartetStaff.widthAnchor constraintEqualToConstant:TMQuartetWidth],
        [self.loadingHeading.widthAnchor constraintLessThanOrEqualToAnchor:stack.widthAnchor],
        [self.loadingStatus.widthAnchor constraintLessThanOrEqualToAnchor:stack.widthAnchor],
        [self.retryButton.heightAnchor constraintGreaterThanOrEqualToConstant:44],
        [self.retryButton.widthAnchor constraintGreaterThanOrEqualToConstant:80]
    ]];
}

#pragma mark - Favorite and teachable beside a list

/// Beside a list the bar has room for the two saved-list toggles directly, as on Android;
/// the symbol fills to show membership and the label names the action VoiceOver will take.
- (void)refreshSavedStateButtons {
    if (!self.favoriteBarButton || !self.teachableBarButton) return;
    UIImageSymbolConfiguration *configuration =
        [UIImageSymbolConfiguration configurationWithPointSize:17 weight:UIImageSymbolWeightRegular scale:UIImageSymbolScaleMedium];
    BOOL favorite = [DPAppDelegate containsFavorite:self.tagId];
    BOOL teachable = [DPAppDelegate containsTeachable:self.tagId];
    self.favoriteBarButton.image = [UIImage systemImageNamed:favorite ? @"heart.fill" : @"heart" withConfiguration:configuration];
    self.favoriteBarButton.accessibilityLabel = favorite ? @"Remove Favorite" : @"Add Favorite";
    self.teachableBarButton.image = [UIImage systemImageNamed:teachable ? @"person.2.fill" : @"person.2" withConfiguration:configuration];
    self.teachableBarButton.accessibilityLabel = teachable ? @"Unmark as Teachable" : @"Mark as Teachable";
    self.favoriteBarButton.enabled = self.tag != nil;
    self.teachableBarButton.enabled = self.tag != nil;
    self.addToListBarButton.enabled = self.tag != nil;
}

/// Every list this tag could join, from the bar button beside the heart and people toggles.
- (void)showListPicker {
    [self showListPickerFrom:self.addToListBarButton];
}

- (void)showListPickerFrom:(UIBarButtonItem *)item {
    if (!self.tag) return;
    [TMListPickerController presentForTagId:self.tagId from:self barButtonItem:item sourceView:nil];
}

- (void)toggleFavorite {
    if (!self.tag) return;
    if ([DPAppDelegate containsFavorite:self.tagId]) [DPAppDelegate removeFavorite:self.tagId];
    else [DPAppDelegate addFavorite:self.tagId];
    [self updateLoadingState];
}

- (void)toggleTeachable {
    if (!self.tag) return;
    if ([DPAppDelegate containsTeachable:self.tagId]) [DPAppDelegate removeTeachable:self.tagId];
    else [DPAppDelegate addTeachable:self.tagId];
    [self updateLoadingState];
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
    
    [actions addAction:[UIAlertAction actionWithTitle:@"Add to List…"
                                                style:UIAlertActionStyleDefault
                                              handler:^(UIAlertAction * _Nonnull action) {
        [self showListPickerFrom:self.actionBarButton];
    }]];

    [actions addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                style:UIAlertActionStyleCancel
                                              handler:nil]];
    
    [self presentViewController:actions animated:YES completion:nil];
}

- (UIViewController *)makeShareControllerWithItems:(NSArray *)items {
    return [[UIActivityViewController alloc] initWithActivityItems:items applicationActivities:nil];
}

- (void)sendTag {
    if (!self.tag) return;
    NSString *string = [NSString stringWithFormat:@"%@ - Tag Master for iOS", self.tag.title];
    NSURL *url = self.tag.tagUri;
    UIViewController *activityController = [self makeShareControllerWithItems:@[string, url]];
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
