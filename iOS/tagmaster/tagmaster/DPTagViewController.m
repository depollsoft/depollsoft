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
@property (nonatomic, strong) UISegmentedControl *sectionPicker;
@property (nonatomic, strong) TMAdaptiveChoiceView *sectionChoice;
@property (nonatomic, strong) UIView *summaryPane;
@property (nonatomic, strong) UIView *sectionPane;
@property (nonatomic, strong) UIView *paneDivider;
@property (nonatomic, strong) NSArray<NSLayoutConstraint *> *paneConstraints;
@property (nonatomic, strong) DPTagPageControllerBase *visibleSectionController;
@property (nonatomic, readwrite) NSInteger selectedSectionIndex;
@property (nonatomic) BOOL usesSplitLayout;

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
    tag = t;

    self.title = t.title;
    for (DPTagPageControllerBase *page in self.viewControllers) {
        page.tag = t;
    }
    [self refreshActionMenu];
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
    }

    self.sectionPicker = [[UISegmentedControl alloc] init];
    self.sectionPicker.translatesAutoresizingMaskIntoConstraints = NO;
    self.sectionPicker.accessibilityLabel = @"Tag section";
    [self.sectionPicker addTarget:self
                           action:@selector(sectionPickerChanged)
                 forControlEvents:UIControlEventValueChanged];

    self.summaryPane = [[UIView alloc] init];
    self.summaryPane.translatesAutoresizingMaskIntoConstraints = NO;
    self.sectionPane = [[UIView alloc] init];
    self.sectionPane.translatesAutoresizingMaskIntoConstraints = NO;
    self.paneDivider = [[UIView alloc] init];
    self.paneDivider.translatesAutoresizingMaskIntoConstraints = NO;
    self.paneDivider.backgroundColor = [TMTheme separator];

    self.sectionChoice = [[TMAdaptiveChoiceView alloc] initWithSegments:self.sectionPicker];
    self.sectionChoice.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.sectionChoice];
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

/// Compact width: one pane, four segments. Regular width: the summary keeps its
/// place on the left while details, tracks, or videos sit beside it — the tag
/// stays readable while a teacher digs into the material.
- (void)applyLayoutForTraits {
    BOOL split = self.traitCollection.horizontalSizeClass == UIUserInterfaceSizeClassRegular
        && self.view.bounds.size.width >= 760
        && !UIContentSizeCategoryIsAccessibilityCategory(self.traitCollection.preferredContentSizeCategory);
    self.usesSplitLayout = split;

    NSArray<NSString *> *titles = [DPTagViewController sectionTitles];
    [self.sectionPicker removeAllSegments];
    NSArray<NSString *> *pickerTitles = split
        ? [titles subarrayWithRange:NSMakeRange(1, titles.count - 1)]
        : titles;
    [pickerTitles enumerateObjectsUsingBlock:^(NSString *title, NSUInteger idx, BOOL *stop) {
        [self.sectionPicker insertSegmentWithTitle:title atIndex:idx animated:NO];
    }];

    NSInteger desired = self.selectedSectionIndex;
    if (split && desired < 1) {
        desired = 1;
    }
    self.sectionPicker.selectedSegmentIndex = split ? desired - 1 : desired;

    if (self.paneConstraints) {
        [NSLayoutConstraint deactivateConstraints:self.paneConstraints];
    }

    UILayoutGuide *safe = self.view.safeAreaLayoutGuide;
    UILayoutGuide *readable = self.view.readableContentGuide;
    NSMutableArray<NSLayoutConstraint *> *constraints = [NSMutableArray array];

    [constraints addObjectsFromArray:@[
        [self.sectionChoice.topAnchor constraintEqualToAnchor:safe.topAnchor constant:TMTheme.spaceS],
        [self.sectionChoice.heightAnchor
            constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget]
    ]];

    self.summaryPane.hidden = NO;
    self.paneDivider.hidden = !split;

    if (split) {
        [constraints addObjectsFromArray:@[
            [self.summaryPane.topAnchor constraintEqualToAnchor:safe.topAnchor],
            [self.summaryPane.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
            [self.summaryPane.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
            [self.summaryPane.widthAnchor constraintEqualToAnchor:self.view.widthAnchor
                                                       multiplier:0.42],

            [self.paneDivider.leadingAnchor constraintEqualToAnchor:self.summaryPane.trailingAnchor],
            [self.paneDivider.widthAnchor constraintEqualToConstant:1.0 / UIScreen.mainScreen.scale],
            [self.paneDivider.topAnchor constraintEqualToAnchor:safe.topAnchor],
            [self.paneDivider.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],

            [self.sectionChoice.leadingAnchor constraintEqualToAnchor:self.paneDivider.trailingAnchor
                                                            constant:TMTheme.spaceL],
            [self.sectionChoice.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor
                                                              constant:-TMTheme.spaceL],

            [self.sectionPane.topAnchor constraintEqualToAnchor:self.sectionChoice.bottomAnchor
                                                       constant:TMTheme.spaceS],
            [self.sectionPane.leadingAnchor constraintEqualToAnchor:self.paneDivider.trailingAnchor],
            [self.sectionPane.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
            [self.sectionPane.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
        ]];
    } else {
        [constraints addObjectsFromArray:@[
            [self.sectionChoice.leadingAnchor constraintEqualToAnchor:readable.leadingAnchor],
            [self.sectionChoice.trailingAnchor constraintEqualToAnchor:readable.trailingAnchor],

            [self.sectionPane.topAnchor constraintEqualToAnchor:self.sectionChoice.bottomAnchor
                                                       constant:TMTheme.spaceS],
            [self.sectionPane.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
            [self.sectionPane.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
            [self.sectionPane.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],

            [self.summaryPane.topAnchor constraintEqualToAnchor:self.sectionPane.topAnchor],
            [self.summaryPane.leadingAnchor constraintEqualToAnchor:self.sectionPane.leadingAnchor],
            [self.summaryPane.trailingAnchor constraintEqualToAnchor:self.sectionPane.trailingAnchor],
            [self.summaryPane.bottomAnchor constraintEqualToAnchor:self.sectionPane.bottomAnchor]
        ]];
    }

    self.paneConstraints = constraints;
    [NSLayoutConstraint activateConstraints:constraints];

    // Reseat the children for the new arrangement.
    for (DPTagPageControllerBase *page in self.viewControllers) {
        [self detachChild:page];
    }
    self.visibleSectionController = nil;
    if (split) {
        [self attachChild:self.summaryController toPane:self.summaryPane];
        self.summaryPane.hidden = NO;
    }
    _selectedSectionIndex = -1;
    [self selectSectionAtIndex:desired];
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

- (void)sectionPickerChanged {
    [TMTheme selected];
    NSInteger index = self.sectionPicker.selectedSegmentIndex;
    [self selectSectionAtIndex:self.usesSplitLayout ? index + 1 : index];
}

- (void)selectSectionAtIndex:(NSInteger)index {
    NSArray<DPTagPageControllerBase *> *pages = self.viewControllers;
    if (index < 0 || index >= (NSInteger)pages.count) {
        return;
    }
    if (self.usesSplitLayout && index == 0) {
        index = 1;
    }
    if (index == self.selectedSectionIndex && self.visibleSectionController) {
        return;
    }
    _selectedSectionIndex = index;
    self.sectionPicker.selectedSegmentIndex = self.usesSplitLayout ? index - 1 : index;

    DPTagPageControllerBase *next = pages[index];
    if (next == self.visibleSectionController) {
        return;
    }
    if (self.visibleSectionController) {
        [self detachChild:self.visibleSectionController];
    }
    [self attachChild:next toPane:self.sectionPane];
    self.visibleSectionController = next;
    self.summaryPane.hidden = !self.usesSplitLayout;
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
