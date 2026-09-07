//
//  DPTagViewController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagViewController.h"
#import "DPBarbershop.h"
#import "DPTagPageControllerBase.h"
#import "DPTagSummaryController.h"
#import "DPTagDetailController.h"
#import "DPTagTracksController.h"
#import "DPTagVideoController.h"
#import "DPAppDelegate.h"
#import "tagmaster-Swift.h"
#import <MessageUI/MessageUI.h>

@interface DPTagViewController () <MFMessageComposeViewControllerDelegate, MFMailComposeViewControllerDelegate>

@property (nonatomic, strong) DPTag *tag;

@property (nonatomic, strong) DPTagSummaryController *summaryController;
@property (nonatomic, strong) DPTagDetailController *detailController;
@property (nonatomic, strong) DPTagTracksController *tagTracksController;
@property (nonatomic, strong) DPTagVideoController *tagVideoController;

@property (nonatomic, strong) UIBarButtonItem *actionBarButton;
@property (nonatomic, strong) UIBarButtonItem *shareBarButton;
@property (nonatomic, strong) UIView *summaryPane;
@property (nonatomic, strong) UIView *sectionPane;
@property (nonatomic, strong) UIView *paneDivider;
@property (nonatomic, strong) NSArray<NSLayoutConstraint *> *paneConstraints;
@property (nonatomic, strong) DPTagPageControllerBase *visibleSectionController;
@property (nonatomic, readwrite) NSInteger selectedSectionIndex;
@property (nonatomic) BOOL usesSplitLayout;
@property (nonatomic) BOOL applyingLayout;

@property (nonatomic, strong) DPBusyIndicator *busyIndicator;

@end

@implementation DPTagViewController

@synthesize tagId, tag;

+ (NSArray<NSString *> *)sectionTitles {
    return @[@"Summary", @"Details", @"Tracks", @"Videos"];
}

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
    self.busyIndicator = [[DPBusyIndicator alloc] init];
    self.busyIndicator.translatesAutoresizingMaskIntoConstraints = NO;
    _selectedSectionIndex = 0;
}

- (void)setTagId:(int)tId {
    tagId = tId;
    [self loadTag:NO];
}

- (void)loadTag:(BOOL)refresh {
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            DPTag *t = [DPTag loadTagById:self->tagId refresh:refresh];
            dispatch_async(dispatch_get_main_queue(), ^{
                if (!t) {
                    [self reportLoadFailure];
                    [self.busyIndicator decrementBusyCount];
                    return;
                }
                self.tag = t;
                [self.busyIndicator decrementBusyCount];
            });
        }
        @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                if (!self.tag) {
                    [self reportLoadFailure];
                }
                [self.busyIndicator decrementBusyCount];
            });
        }
    });
}

/// A tag that will not load used to drop the singer back a screen with no
/// explanation. Now it says what happened and offers the retry.
- (void)reportLoadFailure {
    if (!self.viewLoaded || !self.view.window) {
        [self.navigationController popViewControllerAnimated:YES];
        return;
    }
    UIAlertController *alert =
        [UIAlertController alertControllerWithTitle:@"Tag unavailable"
                                            message:@"Tag Master could not load this tag from "
                                                     "BarbershopTags.com. Check your connection and try again."
                                     preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"Try Again"
                                              style:UIAlertActionStyleDefault
                                            handler:^(UIAlertAction * _Nonnull action) {
        [self loadTag:YES];
    }]];
    [alert addAction:[UIAlertAction actionWithTitle:@"Back"
                                              style:UIAlertActionStyleCancel
                                            handler:^(UIAlertAction * _Nonnull action) {
        [self.navigationController popViewControllerAnimated:YES];
    }]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)setTag:(DPTag *)t {
    BOOL firstLoad = tag == nil;
    tag = t;

    self.title = t.title;
    for (DPTagPageControllerBase *page in self.viewControllers) {
        page.tag = t;
    }
    [self refreshActionMenu];
    if (firstLoad && t && self.usesSplitLayout) { [self selectSectionAtIndex:t.tracks.count > 0 ? 2 : 1]; }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.view];

    self.summaryController = [[DPTagSummaryController alloc] init];
    self.detailController = [[DPTagDetailController alloc] init];
    self.tagTracksController = [[DPTagTracksController alloc] init];
    self.tagVideoController = [[DPTagVideoController alloc] init];

    self.viewControllers = @[
        self.summaryController, self.detailController,
        self.tagTracksController, self.tagVideoController
    ];
    for (DPTagPageControllerBase *page in self.viewControllers) {
        page.busyIndicator = self.busyIndicator;
        page.workspace = self;
    }

    self.summaryPane = [[UIView alloc] init];
    self.summaryPane.translatesAutoresizingMaskIntoConstraints = NO;
    self.sectionPane = [[UIView alloc] init];
    self.sectionPane.translatesAutoresizingMaskIntoConstraints = NO;
    self.paneDivider = [[UIView alloc] init];
    self.paneDivider.translatesAutoresizingMaskIntoConstraints = NO;
    self.paneDivider.backgroundColor = [TMTheme separator];

    [self.view addSubview:self.summaryPane];
    [self.view addSubview:self.paneDivider];
    [self.view addSubview:self.sectionPane];

    self.shareBarButton = [DPAppDelegate barButtonItemWithSystemName:@"square.and.arrow.up"
                                                              target:self
                                                              action:@selector(sendTag)];
    self.shareBarButton.accessibilityLabel = @"Share tag";

    self.actionBarButton = [[UIBarButtonItem alloc]
        initWithImage:[UIImage systemImageNamed:@"text.badge.plus"]
                 menu:[UIMenu menuWithTitle:@"" children:@[]]];
    self.actionBarButton.accessibilityLabel = @"Add to a list";

    UIBarButtonItem *refresh = [DPAppDelegate barButtonItemWithSystemName:@"arrow.clockwise"
                                                                   target:self
                                                                   action:@selector(refreshTag)];
    refresh.accessibilityLabel = @"Refresh tag";

    self.navigationItem.rightBarButtonItems = @[self.shareBarButton, self.actionBarButton, refresh];
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;

    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(refreshActionMenu)
        name:@"tagmaster.userDataChanged" object:nil];
    [self applyLayoutForTraits];
    [self refreshActionMenu];
    [self setTag:self.tag];
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    BOOL split = self.traitCollection.horizontalSizeClass == UIUserInterfaceSizeClassRegular
        && self.view.bounds.size.width >= 760
        && !UIContentSizeCategoryIsAccessibilityCategory(self.traitCollection.preferredContentSizeCategory);
    if (split != self.usesSplitLayout) { [self applyLayoutForTraits]; }
}

- (void)traitCollectionDidChange:(UITraitCollection *)previousTraitCollection {
    [super traitCollectionDidChange:previousTraitCollection];
    if (previousTraitCollection.horizontalSizeClass != self.traitCollection.horizontalSizeClass) {
        [self applyLayoutForTraits];
    }
}

#pragma mark - Layout

/// Summary is the workspace. Material is pushed on phones and adjacent on wide windows.
- (void)applyLayoutForTraits {
    if (self.applyingLayout) { return; }
    self.applyingLayout = YES;
    BOOL split = self.traitCollection.horizontalSizeClass == UIUserInterfaceSizeClassRegular
        && self.view.bounds.size.width >= 760
        && !UIContentSizeCategoryIsAccessibilityCategory(self.traitCollection.preferredContentSizeCategory);
    BOOL wasSplit = self.usesSplitLayout;
    self.usesSplitLayout = split;
    NSInteger desired = self.selectedSectionIndex;
    if (split && desired == 0) { desired = self.tag.tracks.count > 0 ? 2 : 1; }
    if (self.paneConstraints) { [NSLayoutConstraint deactivateConstraints:self.paneConstraints]; }
    UILayoutGuide *safe = self.view.safeAreaLayoutGuide;
    self.summaryPane.hidden = NO;
    self.sectionPane.hidden = !split;
    self.paneDivider.hidden = !split;
    NSMutableArray *constraints = [NSMutableArray arrayWithArray:@[
        [self.summaryPane.topAnchor constraintEqualToAnchor:safe.topAnchor],
        [self.summaryPane.leadingAnchor constraintEqualToAnchor:safe.leadingAnchor],
        [self.summaryPane.bottomAnchor constraintEqualToAnchor:safe.bottomAnchor]
    ]];
    if (split) {
        [constraints addObjectsFromArray:@[
            [self.summaryPane.widthAnchor constraintEqualToAnchor:safe.widthAnchor multiplier:0.46],
            [self.paneDivider.leadingAnchor constraintEqualToAnchor:self.summaryPane.trailingAnchor],
            [self.paneDivider.widthAnchor constraintEqualToConstant:1.0 / UIScreen.mainScreen.scale],
            [self.paneDivider.topAnchor constraintEqualToAnchor:safe.topAnchor],
            [self.paneDivider.bottomAnchor constraintEqualToAnchor:safe.bottomAnchor],
            [self.sectionPane.leadingAnchor constraintEqualToAnchor:self.paneDivider.trailingAnchor],
            [self.sectionPane.trailingAnchor constraintEqualToAnchor:safe.trailingAnchor],
            [self.sectionPane.topAnchor constraintEqualToAnchor:safe.topAnchor],
            [self.sectionPane.bottomAnchor constraintEqualToAnchor:safe.bottomAnchor]
        ]];
    } else {
        [constraints addObject:[self.summaryPane.trailingAnchor constraintEqualToAnchor:safe.trailingAnchor]];
    }
    self.paneConstraints = constraints;
    [NSLayoutConstraint activateConstraints:constraints];
    if (split && [self.viewControllers containsObject:self.navigationController.topViewController]) {
        [self.navigationController popToViewController:self animated:NO];
        // UINavigationController finishes removing the popped parent after its
        // appearance callbacks. Do not attach that page to a pane mid-removal.
        dispatch_async(dispatch_get_main_queue(), ^{ [self applyLayoutForTraits]; });
        self.applyingLayout = NO;
        return;
    }
    for (DPTagPageControllerBase *page in self.viewControllers) { [self detachChild:page]; }
    [self attachChild:self.summaryController toPane:self.summaryPane];
    self.visibleSectionController = nil;
    if (split) { [self selectSectionAtIndex:desired]; }
    else if (wasSplit && desired > 0) { [self selectSectionAtIndex:desired]; }
    self.applyingLayout = NO;
}

- (void)detachChild:(UIViewController *)child {
    if (child.parentViewController != self) {
        return;
    }
    [child willMoveToParentViewController:nil];
    [child.view removeFromSuperview];
    [child removeFromParentViewController];
}

- (void)attachChild:(UIViewController *)child toPane:(UIView *)pane {
    [TMTheme removeBackgroundFrom:child.view];
    [self addChildViewController:child];
    child.view.translatesAutoresizingMaskIntoConstraints = NO;
    [pane addSubview:child.view];
    [NSLayoutConstraint activateConstraints:@[
        [child.view.topAnchor constraintEqualToAnchor:pane.topAnchor],
        [child.view.leadingAnchor constraintEqualToAnchor:pane.leadingAnchor],
        [child.view.trailingAnchor constraintEqualToAnchor:pane.trailingAnchor],
        [child.view.bottomAnchor constraintEqualToAnchor:pane.bottomAnchor]
    ]];
    [child didMoveToParentViewController:self];
}

- (void)selectSectionAtIndex:(NSInteger)index {
    if (index < 0 || index >= (NSInteger)self.viewControllers.count) { return; }
    _selectedSectionIndex = index;
    DPTagPageControllerBase *next = self.viewControllers[index];
    if (index == 0) {
        [self.navigationController popToViewController:self animated:NO];
        return;
    }
    next.title = index == 2 ? @"Learning Tracks" : [DPTagViewController sectionTitles][index];
    if (self.usesSplitLayout) {
        if (next == self.visibleSectionController) { return; }
        [self detachChild:self.visibleSectionController];
        [self attachChild:next toPane:self.sectionPane];
        self.visibleSectionController = next;
    } else {
        [self detachChild:next];
        UINavigationController *navigation = self.navigationController;
        if (navigation.topViewController == next) { return; }
        if (navigation.topViewController != self) { [navigation popToViewController:self animated:NO]; }
        next.view.translatesAutoresizingMaskIntoConstraints = YES;
        [navigation pushViewController:next animated:YES];
    }
}

#pragma mark - Lists, sharing, refresh

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self refreshActionMenu];

    UIView *navView = self.navigationController.view;
    [navView addSubview:self.busyIndicator];
    [navView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_busyIndicator]|"
                                                                    options:0
                                                                    metrics:nil
                                                                      views:NSDictionaryOfVariableBindings(_busyIndicator)]];
    [navView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[_busyIndicator]|"
                                                                    options:0
                                                                    metrics:nil
                                                                      views:NSDictionaryOfVariableBindings(_busyIndicator)]];
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [self.busyIndicator removeFromSuperview];
}

/// Favorites and teachable live in a menu on the bar button, which anchors
/// itself correctly on iPad without a popover source of our own.
- (void)refreshActionMenu {
    __weak typeof(self) weakSelf = self;
    BOOL isFavorite = [DPAppDelegate containsFavorite:self.tagId];
    BOOL isTeachable = [DPAppDelegate containsTeachable:self.tagId];

    UIAction *favorite = [UIAction actionWithTitle:isFavorite ? @"Remove Favorite" : @"Add Favorite"
                                             image:[UIImage systemImageNamed:isFavorite ? @"heart.slash" : @"heart"]
                                        identifier:nil
                                           handler:^(__kindof UIAction * _Nonnull action) {
        if (isFavorite) { [DPAppDelegate removeFavorite:weakSelf.tagId]; }
        else { [DPAppDelegate addFavorite:weakSelf.tagId]; }
        [TMTheme saved];
        [weakSelf refreshActionMenu];
    }];
    favorite.state = isFavorite ? UIMenuElementStateOn : UIMenuElementStateOff;

    UIAction *teachable = [UIAction actionWithTitle:isTeachable ? @"Unmark as Teachable" : @"Mark as Teachable"
                                              image:[UIImage systemImageNamed:@"person.2.wave.2"]
                                         identifier:nil
                                            handler:^(__kindof UIAction * _Nonnull action) {
        if (isTeachable) { [DPAppDelegate removeTeachable:weakSelf.tagId]; }
        else { [DPAppDelegate addTeachable:weakSelf.tagId]; }
        [TMTheme saved];
        [weakSelf refreshActionMenu];
    }];
    teachable.state = isTeachable ? UIMenuElementStateOn : UIMenuElementStateOff;

    self.actionBarButton.menu = [UIMenu menuWithTitle:@"" children:@[favorite, teachable]];
}

- (void)toggleFavorite {
    if ([DPAppDelegate containsFavorite:self.tagId]) {
        [DPAppDelegate removeFavorite:self.tagId];
    } else {
        [DPAppDelegate addFavorite:self.tagId];
    }
    [TMTheme saved];
    [self refreshActionMenu];
}

- (void)toggleTeachable {
    if ([DPAppDelegate containsTeachable:self.tagId]) {
        [DPAppDelegate removeTeachable:self.tagId];
    } else {
        [DPAppDelegate addTeachable:self.tagId];
    }
    [TMTheme saved];
    [self refreshActionMenu];
}

/// Kept for callers and tests that reach for the older action sheet entry
/// point; it presents the same two list actions, anchored to the bar button.
- (void)showActions {
    UIAlertController *actions = [UIAlertController alertControllerWithTitle:nil
                                                                     message:nil
                                                              preferredStyle:UIAlertControllerStyleActionSheet];
    actions.popoverPresentationController.barButtonItem = self.actionBarButton;

    BOOL isFavorite = [DPAppDelegate containsFavorite:self.tagId];
    BOOL isTeachable = [DPAppDelegate containsTeachable:self.tagId];
    [actions addAction:[UIAlertAction actionWithTitle:isFavorite ? @"Remove Favorite" : @"Add Favorite"
                                                style:UIAlertActionStyleDefault
                                              handler:^(UIAlertAction * _Nonnull action) {
        [self toggleFavorite];
    }]];
    [actions addAction:[UIAlertAction actionWithTitle:isTeachable ? @"Unmark as Teachable" : @"Mark as Teachable"
                                                style:UIAlertActionStyleDefault
                                              handler:^(UIAlertAction * _Nonnull action) {
        [self toggleTeachable];
    }]];
    [actions addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                style:UIAlertActionStyleCancel
                                              handler:nil]];

    [self presentViewController:actions animated:YES completion:nil];
}

- (void)sendTag {
    NSString *string = [NSString stringWithFormat:@"%@ - Tag Master for iOS", self.tag.title];
    NSURL *url = self.tag.tagUri;
    NSArray *items = url ? @[string, url] : @[string];
    UIActivityViewController *activityController =
        [[UIActivityViewController alloc] initWithActivityItems:items applicationActivities:nil];
    // On iPad a share sheet without an anchor is a crash; anchor it to the
    // button it came from.
    activityController.popoverPresentationController.barButtonItem = self.shareBarButton;
    [self presentViewController:activityController animated:YES completion:nil];
}

- (void)refreshTag {
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
