//
//  DPTagSummaryController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagSummaryController.h"
#import "DPAppDelegate.h"
#import "TMBarberPoleLoadingView.h"
#import "TMDetailLayout.h"
#import "UIView+DPUtils.h"
#import "DPTextView.h"
#import "DPFileCache.h"
#import "DPPitchPipeButton.h"
#import <QuickLook/QuickLook.h>

// Width-aware wrapping stays local to Tag Master. Shared pitch callbacks own sound.
@interface TMKeyButton : TMWrappingButton
@property (nonatomic, copy) void (^playNote)(void);
@property (nonatomic, copy) void (^cancelTimedNote)(void);
@property (nonatomic, strong) DPNote *note;
@property (nonatomic, strong) NSTimer *noteTimer;
@property (nonatomic) BOOL showingPlayback;
- (void)updatePitchAppearance;
@end
@implementation TMKeyButton
- (BOOL)accessibilityActivate {
    if (!self.enabled || !self.playNote) return NO;
    self.playNote();
    [self updatePitchAppearance];
    return YES;
}
- (void)setNote:(DPNote *)note {
    if (self.cancelTimedNote) self.cancelTimedNote();
    _note = note;
    [self updatePitchAppearance];
}
- (void)didMoveToWindow {
    [super didMoveToWindow];
    [self.noteTimer invalidate];
    self.noteTimer = nil;
    if (self.window) {
        // DPNote play/stop mutate an ivar, so KVO cannot observe playback.
        // Only restyle when the actual bound note changes, while mounted.
        __weak TMKeyButton *weakSelf = self;
        self.noteTimer = [NSTimer timerWithTimeInterval:1.0 / 30 repeats:YES block:^(NSTimer *timer) {
            TMKeyButton *button = weakSelf;
            if ((button.enabled && button.note.isPlaying) != button.showingPlayback) [button updatePitchAppearance];
        }];
        [NSRunLoop.mainRunLoop addTimer:self.noteTimer forMode:NSRunLoopCommonModes];
    } else {
        if (self.cancelTimedNote) self.cancelTimedNote();
        [self.note stop];
    }
    [self updatePitchAppearance];
}
- (void)dealloc { [_noteTimer invalidate]; }
- (void)setHighlighted:(BOOL)highlighted {
    [super setHighlighted:highlighted];
    [self updatePitchAppearance];
}
- (void)setEnabled:(BOOL)enabled {
    [super setEnabled:enabled];
    [self updatePitchAppearance];
}
- (void)updateConfiguration {
    [super updateConfiguration];
    [self updatePitchAppearance];
}
- (void)tintColorDidChange {
    [super tintColorDidChange];
    [self updatePitchAppearance];
}
- (void)traitCollectionDidChange:(UITraitCollection *)previousTraitCollection {
    [super traitCollectionDidChange:previousTraitCollection];
    [self updatePitchAppearance];
}
- (void)updatePitchAppearance {
    self.showingPlayback = self.enabled && self.note.isPlaying;
    BOOL dark = self.traitCollection.userInterfaceStyle == UIUserInterfaceStyleDark;
    UIColor *blue = self.tintColor ?: [DPAppDelegate accentColor];
    // Use UIKit's high-contrast blue beneath white body text in light mode.
    UITraitCollection *contrast = [UITraitCollection traitCollectionWithTraitsFromCollections:@[self.traitCollection,
        [UITraitCollection traitCollectionWithAccessibilityContrast:UIAccessibilityContrastHigh]]];
    UIColor *fill = dark ? blue : [blue resolvedColorWithTraitCollection:contrast];
    UIColor *foreground = self.showingPlayback ? (dark ? UIColor.blackColor : UIColor.whiteColor) : blue;
    UIButtonConfiguration *configuration = self.configuration;
    if (configuration) {
        configuration.baseForegroundColor = foreground;
        UIBackgroundConfiguration *background = [UIBackgroundConfiguration clearConfiguration];
        background.backgroundColor = self.showingPlayback ? fill : UIColor.clearColor;
        background.cornerRadius = 8;
        background.backgroundColorTransformer = ^UIColor *(UIColor *color) { return color; };
        configuration.background = background;
        configuration.imageColorTransformer = ^UIColor *(UIColor *color) { return foreground; };
        // Navigation bars can retint template images independently of the title.
        configuration.image = [[UIImage systemImageNamed:@"key"] imageWithTintColor:foreground renderingMode:UIImageRenderingModeAlwaysOriginal];
        self.configuration = configuration;
    }
    self.backgroundColor = UIColor.clearColor;
    [self setTitleColor:foreground forState:UIControlStateNormal];
    [self setTitleColor:foreground forState:UIControlStateHighlighted];
    self.layer.borderColor = [blue resolvedColorWithTraitCollection:self.traitCollection].CGColor;
    self.layer.borderWidth = 1.5;
    self.layer.cornerRadius = 8;
}
@end

// The bar item owns the fitting size; the child keeps the existing press,
// release, cancel, accessibility and blue playback appearance unchanged.
@interface TMSheetKeyView : UIView
@property (nonatomic, strong) TMKeyButton *button;
- (instancetype)initWithButton:(TMKeyButton *)button;
@end
@implementation TMSheetKeyView
- (instancetype)initWithButton:(TMKeyButton *)button {
    CGSize size = button.intrinsicContentSize;
    if ((self = [super initWithFrame:CGRectMake(0, 0, MAX(44, size.width), MAX(44, size.height))])) {
        self.button = button;
        [self.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
        [self.widthAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
        button.translatesAutoresizingMaskIntoConstraints = NO;
        [self addSubview:button];
        [NSLayoutConstraint activateConstraints:@[
            [button.leadingAnchor constraintEqualToAnchor:self.leadingAnchor],
            [button.trailingAnchor constraintEqualToAnchor:self.trailingAnchor],
            [button.topAnchor constraintEqualToAnchor:self.topAnchor],
            [button.bottomAnchor constraintEqualToAnchor:self.bottomAnchor]
        ]];
    }
    return self;
}
- (CGSize)intrinsicContentSize {
    CGSize size = self.button.intrinsicContentSize;
    return CGSizeMake(MAX(44, size.width), MAX(44, size.height));
}
- (CGSize)sizeThatFits:(CGSize)size { return self.intrinsicContentSize; }
@end

@interface TMKeyPitchButton : DPPitchPipeButton
@end
@implementation TMKeyPitchButton
- (void)setButton:(UIButton *)button {
    [super setButton:button];
    // Remove the shared widget's 2pt face inset only in this app-local adapter.
    // Its existing target/actions continue to own press and release playback.
    for (NSLayoutConstraint *constraint in button.superview.constraints) {
        if (constraint.firstItem == button || constraint.secondItem == button) constraint.constant = 0;
    }
}
- (void)setNote:(DPNote *)note {
    [super setNote:note];
    if ([self.button isKindOfClass:TMKeyButton.class]) ((TMKeyButton *)self.button).note = note;
}
- (void)updateConstraints {
    [super updateConstraints];
    [self.button setBackgroundImage:nil forState:UIControlStateNormal];
    [self.button setBackgroundImage:nil forState:UIControlStateHighlighted];
    if ([self.button isKindOfClass:TMKeyButton.class]) [(TMKeyButton *)self.button updatePitchAppearance];
}
@end

// Multiline rows measure at their actual column width. UIKit's cached intrinsic
// height can otherwise survive the first containment/layout transaction.
@interface TMSummaryBodyLabel : UILabel
@end
@implementation TMSummaryBodyLabel
- (CGSize)intrinsicContentSize {
    if (self.bounds.size.width <= 0) return CGSizeMake(UIViewNoIntrinsicMetric, [super intrinsicContentSize].height);
    return CGSizeMake(UIViewNoIntrinsicMetric, [self sizeThatFits:CGSizeMake(self.preferredMaxLayoutWidth > 0 ? self.preferredMaxLayoutWidth : self.bounds.size.width, CGFLOAT_MAX)].height);
}
- (void)setBounds:(CGRect)bounds {
    BOOL changed = self.bounds.size.width != bounds.size.width;
    [super setBounds:bounds];
    if (changed) [self invalidateIntrinsicContentSize];
}
@end

@interface DPSheetMusicPreview : NSObject <QLPreviewItem>

@property (nonatomic, strong) NSURL *previewItemURL;
@property (nonatomic, strong) NSString *previewItemTitle;

@end

@implementation DPSheetMusicPreview

@end

/// Hosts the QuickLook preview inside the visible detail column. Beside a list on iPad the
/// secondary column extends beneath the floating list column and marks that strip as unsafe
/// area; QuickLook centres its page across its full bounds regardless, so the page ended up
/// under the list. Pinning the preview to the safe area keeps the sheet in the detail view,
/// and a full-screen toggle hides the list for reading, restoring the split on the way back.
@interface TMSheetMusicViewController : UIViewController
@property (nonatomic, strong, readonly) QLPreviewController *previewer;
@property (nonatomic, strong, readonly) NSURL *fileURL;
@property (nonatomic, strong) UIBarButtonItem *fullScreenItem;
@property (nonatomic, strong) UIBarButtonItem *shareItem;
@property (nonatomic, strong) UIBarButtonItem *keyItem;
@property (nonatomic) BOOL fullScreen;
@property (nonatomic) BOOL changedDisplayMode;
@property (nonatomic) UISplitViewControllerDisplayMode displayModeToRestore;
- (instancetype)initWithPreviewer:(QLPreviewController *)previewer title:(NSString *)title fileURL:(NSURL *)fileURL keyItem:(UIBarButtonItem *)keyItem;
- (void)toggleFullScreen;
@end

@implementation TMSheetMusicViewController
- (instancetype)initWithPreviewer:(QLPreviewController *)previewer title:(NSString *)title fileURL:(NSURL *)fileURL keyItem:(UIBarButtonItem *)keyItem {
    if ((self = [super initWithNibName:nil bundle:nil])) {
        _previewer = previewer;
        _fileURL = fileURL;
        _keyItem = keyItem;
        self.title = title;
        self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
        [self addChildViewController:previewer];
        self.shareItem = [DPAppDelegate barButtonItemWithSystemName:@"square.and.arrow.up" target:self action:@selector(share)];
        self.fullScreenItem = [[UIBarButtonItem alloc] initWithImage:nil style:UIBarButtonItemStylePlain target:self action:@selector(toggleFullScreen)];
        [self refreshFullScreenItem];
    }
    return self;
}
- (void)viewDidLoad {
    [super viewDidLoad];
    self.view.backgroundColor = UIColor.systemBackgroundColor;
    UIView *preview = self.previewer.view;
    preview.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:preview];
    [NSLayoutConstraint activateConstraints:@[
        [preview.leadingAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.leadingAnchor],
        [preview.trailingAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.trailingAnchor],
        [preview.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [preview.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
    ]];
    [self.previewer didMoveToParentViewController:self];
}
- (BOOL)besideAList {
    UISplitViewController *split = self.splitViewController;
    return split != nil && !split.isCollapsed;
}
- (void)refreshFullScreenItem {
    UIImageSymbolConfiguration *configuration =
        [UIImageSymbolConfiguration configurationWithPointSize:17 weight:UIImageSymbolWeightRegular scale:UIImageSymbolScaleMedium];
    self.fullScreenItem.image = [UIImage systemImageNamed:self.fullScreen ? @"arrow.down.right.and.arrow.up.left" : @"arrow.up.left.and.arrow.down.right"
                                        withConfiguration:configuration];
    self.fullScreenItem.accessibilityLabel = self.fullScreen ? @"Show list" : @"Full screen";
    // Leftmost first in the bar: key note, full screen (beside a list only), share.
    NSMutableArray *items = [NSMutableArray arrayWithObject:self.shareItem];
    if ([self besideAList]) [items addObject:self.fullScreenItem];
    if (self.keyItem) [items addObject:self.keyItem];
    self.navigationItem.rightBarButtonItems = items;
}
- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self refreshFullScreenItem];
}
- (void)toggleFullScreen {
    UISplitViewController *split = self.splitViewController;
    if (!split) return;
    if (!self.fullScreen) {
        if (!self.changedDisplayMode) {
            self.displayModeToRestore = split.preferredDisplayMode;
            self.changedDisplayMode = YES;
        }
        split.preferredDisplayMode = UISplitViewControllerDisplayModeSecondaryOnly;
        self.fullScreen = YES;
    } else {
        split.preferredDisplayMode = self.displayModeToRestore;
        self.changedDisplayMode = NO;
        self.fullScreen = NO;
    }
    [self refreshFullScreenItem];
}
- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    // Leaving the sheet (back, or the tag changing beside it) gives the list back.
    if (self.changedDisplayMode && (self.isMovingFromParentViewController || self.isBeingDismissed)) {
        self.splitViewController.preferredDisplayMode = self.displayModeToRestore;
        self.changedDisplayMode = NO;
        self.fullScreen = NO;
    }
}
- (void)share {
    if (!self.fileURL) return;
    UIActivityViewController *activity = [[UIActivityViewController alloc] initWithActivityItems:@[self.fileURL] applicationActivities:nil];
    activity.popoverPresentationController.barButtonItem = self.shareItem;
    [self presentViewController:activity animated:YES completion:nil];
}
- (void)done {
    [self dismissViewControllerAnimated:YES completion:nil];
}
@end

@interface DPTagSummaryController () <QLPreviewControllerDataSource, UIActionSheetDelegate>

@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UILabel *akaLabel;
@property (nonatomic, strong) UILabel *versionLabel;
@property (nonatomic, strong) UILabel *savedStatusLabel;
@property (nonatomic, strong) UILabel *tagIdHeader;
@property (nonatomic, strong) UILabel *tagIdLabel;
@property (nonatomic, strong) UIProgressView *ratingBar;
@property (nonatomic, strong) UILabel *ratingLabel;
@property (nonatomic, strong) UIButton *ratingButton;
@property (nonatomic, strong) UILabel *partsLabel;
@property (nonatomic, strong) UILabel *typeLabel;
@property (nonatomic, strong) DPPitchPipeButton *keyButton;
@property (nonatomic) NSUInteger keyActivationGeneration;
@property (nonatomic, strong) UILabel *classicTagNumberLabel;
@property (nonatomic, strong) UIButton *sheetMusicButton;
@property (nonatomic, strong) UIStackView *sheetMusicAction;
@property (nonatomic, strong) TMBarberPoleLoadingView *sheetMusicLoading;
@property (nonatomic, strong) TMBarberPoleLoadingView *ratingLoading;
@property (nonatomic, strong) UILabel *lyricsLabel;
@property (nonatomic, strong) UILabel *notesLabel;
@property (nonatomic, strong) UILabel *ratingHeader;
@property (nonatomic, strong) UILabel *partsHeader;
@property (nonatomic, strong) UILabel *typeHeader;
@property (nonatomic, strong) UILabel *keyHeader;
@property (nonatomic, strong) UILabel *notesHeader;
@property (nonatomic, strong) UILabel *lyricsHeader;
@property (nonatomic, strong) UILabel *classicTagNumberHeader;

@property (nonatomic, strong) UIStackView *grid;
@property TMDetailPair *classicPair;
@property TMDetailMetadata *facts;
@property UIStackView *keySection;
@property UIStackView *lyricsSection;
@property UIStackView *notesSection;

@end

@implementation DPTagSummaryController

@synthesize titleLabel, akaLabel, ratingBar, ratingLabel, partsLabel, typeLabel, keyButton, classicTagNumberLabel, sheetMusicButton, ratingButton, lyricsLabel, notesLabel, ratingHeader, partsHeader, typeHeader, keyHeader, notesHeader, lyricsHeader, classicTagNumberHeader, grid;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

/// "Favorite" and "Teachable tag" under the title, as on Android, whenever the tag is on a saved list.
- (void)refreshSavedStatus {
    if (!self.savedStatusLabel) return;
    NSMutableArray<NSString *> *marks = [NSMutableArray array];
    if (self.tag && [DPAppDelegate containsFavorite:self.tag.tagId]) [marks addObject:@"Favorite"];
    if (self.tag && [DPAppDelegate containsTeachable:self.tag.tagId]) [marks addObject:@"Teachable tag"];
    self.savedStatusLabel.text = [marks componentsJoinedByString:@"   "];
    self.savedStatusLabel.hidden = marks.count == 0;
}

- (void)savedListsChanged:(NSNotification *)notification {
    [self refreshSavedStatus];
}

- (void)refreshView {
    titleLabel.text = self.tag.title;
    
    akaLabel.text = [NSString stringWithFormat:@"a.k.a. %@", self.tag.alternativeTitle];
    akaLabel.hidden = self.tag.alternativeTitle.length == 0;
    self.versionLabel.text = [NSString stringWithFormat:@"Version: %@", self.tag.version ?: @""];
    self.versionLabel.hidden = self.tag.version.length == 0;
    [self refreshSavedStatus];
    self.tagIdLabel.text = [NSString stringWithFormat:@"%d", self.tag.tagId];

    ratingLabel.text = [NSString stringWithFormat:@"%1.2f", self.tag.rating];
    ratingLabel.accessibilityValue = ratingLabel.text;
    ratingBar.progress = self.tag.rating / 5;
    
    partsLabel.text = [NSString stringWithFormat:@"%d", self.tag.parts];
    
    typeLabel.text = self.tag.tagType;
    
    keyButton.note = [self.tag keyNote];
    keyButton.button.accessibilityLabel = [NSString stringWithFormat:@"Play key note %@", self.tag.keyNote];
    keyButton.button.accessibilityHint = @"Plays for one and a half seconds";
    [keyButton.button setTitle:self.tag.writtenKey forState:UIControlStateNormal];
    self.keySection.hidden = self.tag.writtenKey.length == 0;
    
    classicTagNumberLabel.text = [NSString stringWithFormat:@"%d", self.tag.classicTagNumber];
    self.classicPair.hidden = self.tag.classicTagNumber == 0;
    
    self.sheetMusicAction.hidden = !self.tag.sheetMusicUri;
    
    lyricsLabel.text = self.tag.lyrics;
    self.lyricsSection.hidden = self.tag.lyrics.length == 0;
    notesLabel.text = self.tag.notes;
    self.notesSection.hidden = self.tag.notes.length == 0;
    [self.facts reloadValues];
    [self.grid setNeedsLayout];
    [self.view setNeedsLayout];

    [self.ratingButton setEnabled:self.tag != nil];
    [self.ratingButton setTitle:@"Rate" forState:UIControlStateNormal];
    self.ratingButton.accessibilityLabel = @"Rate tag";
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UIScrollView *scroller = [[UIScrollView alloc] init];
    
    titleLabel = [self makeTitleLabel];
    akaLabel = [[UILabel alloc] init];
    akaLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleTitle3];
    akaLabel.textColor = [UIColor secondaryLabelColor];
    akaLabel.adjustsFontForContentSizeCategory = YES;
    akaLabel.numberOfLines = 0;
    ratingBar = [[UIProgressView alloc] initWithProgressViewStyle:UIProgressViewStyleBar];
    ratingBar.trackTintColor = [UIColor tertiarySystemFillColor];
    ratingBar.isAccessibilityElement = NO;
    ratingButton = [[UIButton alloc] init];
    [ratingButton setTitle:@"Rate" forState:UIControlStateNormal];
    // The rating unit needs the number's lexical width, unlike full-width prose.
    ratingLabel = [UILabel new];
    ratingLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    ratingLabel.adjustsFontForContentSizeCategory = YES;
    [ratingLabel setContentCompressionResistancePriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    ratingButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [ratingButton setTitle:@"Rate" forState:UIControlStateNormal];
    partsLabel = [self makeBodyLabel];
    typeLabel = [self makeBodyLabel];
    keyButton = [[TMKeyPitchButton alloc] init];
    TMKeyButton *accessibleKey = [TMKeyButton buttonWithType:UIButtonTypeCustom];
    __weak DPTagSummaryController *weakSelf = self;
    accessibleKey.playNote = ^{ [weakSelf playKeyNote]; };
    accessibleKey.cancelTimedNote = ^{ [weakSelf cancelTimedKeyNote]; };
    keyButton.button = accessibleKey;
    accessibleKey.accessibilityIdentifier = @"summary.key";
    [accessibleKey addTarget:self action:@selector(pitchTouchUp) forControlEvents:UIControlEventTouchCancel];
    // Invalidate timed accessibility cleanup without replacing the shared sound targets.
    [accessibleKey addTarget:self action:@selector(cancelTimedKeyNote) forControlEvents:UIControlEventTouchDown | UIControlEventTouchUpInside | UIControlEventTouchUpOutside];
    keyButton.button.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    keyButton.button.titleLabel.adjustsFontForContentSizeCategory = YES;
    UIButtonConfiguration *config = [UIButtonConfiguration plainButtonConfiguration];
    config.contentInsets = NSDirectionalEdgeInsetsMake(4, 8, 4, 8);
    config.image = [UIImage systemImageNamed:@"key"];
    config.imagePadding = 8;
    keyButton.button.configuration = config;
    classicTagNumberLabel = [self makeBodyLabel];
    sheetMusicButton = [TMWrappingButton buttonWithType:UIButtonTypeRoundedRect];
    [sheetMusicButton setTitle:@"Sheet Music" forState:UIControlStateNormal];
    [sheetMusicButton addTarget:self action:@selector(openSheetMusic) forControlEvents:UIControlEventTouchUpInside];
    // Sheet music is the primary action; key is outlined and rating stays plain.
    UIButtonConfiguration *sheetConfiguration = [UIButtonConfiguration filledButtonConfiguration];
    sheetConfiguration.baseForegroundColor = [UIColor whiteColor];
    sheetConfiguration.cornerStyle = UIButtonConfigurationCornerStyleMedium;
    sheetConfiguration.image = [UIImage systemImageNamed:@"doc.richtext"];
    sheetConfiguration.imagePadding = 8;
    sheetConfiguration.titleLineBreakMode = NSLineBreakByWordWrapping;
    sheetConfiguration.contentInsets = NSDirectionalEdgeInsetsMake(8, 44, 8, 44);
    sheetMusicButton.configuration = sheetConfiguration;
    [sheetMusicButton setTitle:@"Sheet Music" forState:UIControlStateNormal];
    UIButtonConfiguration *rateConfiguration = [UIButtonConfiguration plainButtonConfiguration];
    rateConfiguration.image = [UIImage systemImageNamed:@"star"];
    rateConfiguration.imagePadding = 8;
    rateConfiguration.contentInsets = NSDirectionalEdgeInsetsMake(4, 8, 4, 8);
    ratingButton.configuration = rateConfiguration;
    for (UIButton *button in @[ratingButton, sheetMusicButton, keyButton.button]) {
        button.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
        button.titleLabel.adjustsFontForContentSizeCategory = YES;
        [button.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
        [button.widthAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
    }
    ratingButton.accessibilityLabel = @"Rate tag";
    lyricsLabel = [self makeBodyLabel];
    lyricsLabel.numberOfLines = 0;
    notesLabel = [self makeBodyLabel];
    notesLabel.numberOfLines = 0;
    
    self.versionLabel = [self makeBodyLabel];
    self.versionLabel.textColor = [UIColor secondaryLabelColor];
    self.savedStatusLabel = [UILabel new];
    self.savedStatusLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleSubheadline];
    self.savedStatusLabel.adjustsFontForContentSizeCategory = YES;
    self.savedStatusLabel.numberOfLines = 0;
    self.savedStatusLabel.textColor = [UIColor secondaryLabelColor];
    self.tagIdHeader = [self makeHeader:@"Tag ID"];
    self.tagIdLabel = [self makeBodyLabel];
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(savedListsChanged:) name:@"tagmaster.userDataChanged" object:nil];
    ratingHeader = [self makeHeader:@"Rating"];
    partsHeader = [self makeHeader:@"Parts"];
    typeHeader = [self makeHeader:@"Type"];
    keyHeader = [self makeHeader:@"Key"];
    notesHeader = [self makeHeader:@"Notes"];
    lyricsHeader = [self makeHeader:@"Lyrics"];
    classicTagNumberHeader = [self makeHeader:@"Classic Tag"];
    
    // Build rating UI
    UIStackView *ratingValue = [[UIStackView alloc] initWithArrangedSubviews:@[ratingLabel, ratingBar]];
    ratingValue.axis = UILayoutConstraintAxisVertical;
    ratingValue.spacing = 4;
    ratingLabel.textAlignment = NSTextAlignmentNatural;
    ratingLabel.accessibilityLabel = @"Rating out of 5";
    self.ratingLoading = [[TMBarberPoleLoadingView alloc] initWithOperationName:@"Sending rating…"];
    UIStackView *ratingAction = [self actionRowForButton:ratingButton loader:self.ratingLoading];
    TMRatingUnit *ratingGrid = [[TMRatingUnit alloc] initWithArrangedSubviews:@[ratingValue, ratingAction, [UIView new]]];
    ratingGrid.baselineLabel = ratingLabel;
    [ratingValue setContentHuggingPriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    [ratingAction setContentHuggingPriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    ratingGrid.axis = UILayoutConstraintAxisHorizontal;
    ratingGrid.alignment = UIStackViewAlignmentCenter;
    ratingGrid.spacing = 8;
    [ratingButton setContentCompressionResistancePriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    self.classicPair = [TMDetailPair caption:classicTagNumberHeader value:classicTagNumberLabel];
    TMDetailMetadata *facts = [[TMDetailMetadata alloc] initWithArrangedSubviews:@[
        [TMDetailPair caption:self.tagIdHeader value:self.tagIdLabel],
        [TMDetailPair caption:partsHeader value:partsLabel],
        [TMDetailPair caption:typeHeader value:typeLabel], self.classicPair,
        [TMDetailPair caption:ratingHeader value:ratingGrid]]];
    facts.compactFacts = YES;
    self.facts = facts;
    self.sheetMusicLoading = [[TMBarberPoleLoadingView alloc] initWithOperationName:@"Opening sheet music…"];
    self.sheetMusicAction = [[UIStackView alloc] initWithArrangedSubviews:@[sheetMusicButton]];
    [self.sheetMusicAction addSubview:self.sheetMusicLoading];
    self.sheetMusicLoading.translatesAutoresizingMaskIntoConstraints = NO;
    CGSize poleSize = self.sheetMusicLoading.intrinsicContentSize;
    [NSLayoutConstraint activateConstraints:@[
        [self.sheetMusicLoading.trailingAnchor constraintEqualToAnchor:self.sheetMusicAction.trailingAnchor constant:-12],
        [self.sheetMusicLoading.centerYAnchor constraintEqualToAnchor:self.sheetMusicAction.centerYAnchor],
        [self.sheetMusicLoading.widthAnchor constraintEqualToConstant:poleSize.width],
        [self.sheetMusicLoading.heightAnchor constraintEqualToConstant:poleSize.height]
    ]];
    self.keySection = [[UIStackView alloc] initWithArrangedSubviews:@[keyHeader, keyButton]];
    self.keySection.axis = UILayoutConstraintAxisVertical;
    self.keySection.spacing = 4;
    TMDetailSections *performance = [[TMDetailSections alloc] initWithArrangedSubviews:@[facts, self.keySection, self.sheetMusicAction]];
    self.lyricsSection = [[UIStackView alloc] initWithArrangedSubviews:@[lyricsHeader, lyricsLabel]];
    self.lyricsSection.axis = UILayoutConstraintAxisVertical;
    self.lyricsSection.spacing = 4;
    self.notesSection = [[UIStackView alloc] initWithArrangedSubviews:@[notesHeader, notesLabel]];
    self.notesSection.axis = UILayoutConstraintAxisVertical;
    self.notesSection.spacing = 4;
    TMDetailSections *prose = [[TMDetailSections alloc] initWithArrangedSubviews:@[self.lyricsSection, self.notesSection]];
    TMSummaryColumns *columns = [[TMSummaryColumns alloc] initWithArrangedSubviews:@[performance, prose]];
    UIStackView *identity = [[UIStackView alloc] initWithArrangedSubviews:@[titleLabel, akaLabel, self.versionLabel, self.savedStatusLabel]];
    identity.axis = UILayoutConstraintAxisVertical;
    identity.spacing = 4;
    grid = [[UIStackView alloc] initWithArrangedSubviews:@[identity, columns]];
    grid.axis = UILayoutConstraintAxisVertical;
    grid.spacing = 8;
    
    [ratingButton addTarget:self action:@selector(rate) forControlEvents:UIControlEventTouchUpInside];
    
    [self setUpRootView:grid withScroller:scroller];
    [sheetMusicButton.heightAnchor constraintEqualToAnchor:keyButton.button.heightAnchor].active = YES;
    
    [self refreshView];
}

- (UILabel *)makeBodyLabel {
    UILabel *label = [TMSummaryBodyLabel new];
    label.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    label.adjustsFontForContentSizeCategory = YES;
    label.numberOfLines = 0;
    return label;
}

- (void)rate {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Rating"
                                                                             message:@"Rate the tag on a scale of 1-5 stars"
                                                                      preferredStyle:UIAlertControllerStyleActionSheet];
    alertController.popoverPresentationController.sourceView = ratingButton;
    alertController.popoverPresentationController.sourceRect = ratingButton.bounds;
    [alertController addAction:[UIAlertAction actionWithTitle:@"5 stars" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:5];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"4 stars" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:4];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"3 stars" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:3];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"2 stars" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:2];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"1 star" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:1];
    }]];
    
    [alertController addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                        style:UIAlertActionStyleCancel
                                                      handler:nil]];
    
    [self presentViewController:alertController animated:YES completion:nil];
}

// Reserve a neutral accessory slot, outside the disabled/filled button. Hiding
// the pole never changes button width, title/icon configuration or hit area.
- (UIStackView *)actionRowForButton:(UIButton *)button loader:(TMBarberPoleLoadingView *)loader {
    UIView *slot = [UIView new];
    [slot addSubview:loader];
    loader.translatesAutoresizingMaskIntoConstraints = NO;
    CGSize size = loader.intrinsicContentSize;
    [NSLayoutConstraint activateConstraints:@[
        [slot.widthAnchor constraintEqualToConstant:size.width],
        [slot.heightAnchor constraintEqualToConstant:size.height],
        [loader.centerXAnchor constraintEqualToAnchor:slot.centerXAnchor],
        [loader.centerYAnchor constraintEqualToAnchor:slot.centerYAnchor],
        [loader.widthAnchor constraintEqualToConstant:size.width],
        [loader.heightAnchor constraintEqualToConstant:size.height]
    ]];
    UIStackView *row = [[UIStackView alloc] initWithArrangedSubviews:@[button, slot]];
    row.axis = UILayoutConstraintAxisHorizontal;
    row.alignment = UIStackViewAlignmentCenter;
    row.spacing = 8;
    return row;
}

- (void)setButton:(UIButton *)button busy:(BOOL)busy {
    TMBarberPoleLoadingView *loader = button == self.ratingButton ? self.ratingLoading : self.sheetMusicLoading;
    if (busy) [loader startAnimating]; else [loader stopAnimating];
    button.enabled = !busy;
}

- (void)rateTag:(NSInteger)rating {
    [self.busyIndicator incrementBusyCount];
    [self setButton:self.ratingButton busy:YES];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            [self.tag rate:rating];
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self setButton:self.ratingButton busy:NO];
                [self.ratingButton setEnabled:NO];
                [self.ratingButton setTitle:@"Rated" forState:UIControlStateNormal];
                self.ratingButton.accessibilityLabel = @"Rating submitted";
            });
        }
        @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self setButton:self.ratingButton busy:NO];
                [self tm_showError:@"Your rating couldn't be sent. Check your connection and try again." retry:^{ [self rateTag:rating]; }];
            });
        }
    });
}

- (void)openSheetMusic {
    if (self.busyIndicator.busyCount > 0 && !self.sheetMusicButton.enabled) return;
    [self.busyIndicator incrementBusyCount];
    [self setButton:self.sheetMusicButton busy:YES];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            NSString *key = self.tag.sheetMusicUri.cacheKey;
            if (![[NSFileManager defaultManager] fileExistsAtPath:[DPFileCache pathForKey:key]]) {
                NSData *data = [DPRemoteLocation dataWithContentsOfURL:self.tag.sheetMusicUri.uri error:nil];
                if (data.length == 0) {
                    [NSException raise:@"SheetMusicUnavailable" format:@"No sheet music data"];
                }
                [DPFileCache writeData:data forKey:key];
                if (![[NSFileManager defaultManager] fileExistsAtPath:[DPFileCache pathForKey:key]]) {
                    [NSException raise:@"SheetMusicUnavailable" format:@"Unable to save sheet music"];
                }
            }
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self setButton:self.sheetMusicButton busy:NO];
                QLPreviewController *previewer = [[QLPreviewController alloc] init];
                previewer.dataSource = self;
                UIBarButtonItem *keyItem = nil;
                if (self.tag.keyNote) {
                    TMKeyButton *toucher = [TMKeyButton buttonWithType:UIButtonTypeCustom];
                    __weak DPTagSummaryController *weakSelf = self;
                    toucher.playNote = ^{ [weakSelf playKeyNote]; };
                    toucher.cancelTimedNote = ^{ [weakSelf cancelTimedKeyNote]; };
                    toucher.note = self.tag.keyNote;
                    toucher.tintColor = [DPAppDelegate accentColor];
                    toucher.accessibilityIdentifier = @"sheet.key";
                    UIButtonConfiguration *keyConfiguration = [UIButtonConfiguration plainButtonConfiguration];
                    keyConfiguration.image = [UIImage systemImageNamed:@"key"];
                    keyConfiguration.imagePadding = 8;
                    keyConfiguration.contentInsets = NSDirectionalEdgeInsetsMake(4, 8, 4, 8);
                    // The key glyph already says "key"; the title is the written key alone, as on
                    // the Summary and on Android, and it stays on one line however tight the bar.
                    keyConfiguration.titleLineBreakMode = NSLineBreakByTruncatingTail;
                    toucher.configuration = keyConfiguration;
                    toucher.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
                    toucher.titleLabel.adjustsFontForContentSizeCategory = YES;
                    toucher.titleLabel.numberOfLines = 1;
                    [toucher setContentCompressionResistancePriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
                    toucher.accessibilityLabel = [NSString stringWithFormat:@"Play key note %@", self.tag.keyNote];
                    toucher.accessibilityHint = @"Plays for one and a half seconds";
                    [toucher setTitle:self.tag.writtenKey forState:UIControlStateNormal];
                    [toucher addTarget:self action:@selector(pitchTouchDown) forControlEvents:UIControlEventTouchDown];
                    [toucher addTarget:self action:@selector(pitchTouchUp) forControlEvents:UIControlEventTouchUpInside | UIControlEventTouchUpOutside | UIControlEventTouchCancel];
                    keyItem = [[UIBarButtonItem alloc] initWithCustomView:[[TMSheetKeyView alloc] initWithButton:toucher]];
                    // This control draws its own outlined/pressed background.
                    // Shared Glass fitting caps custom content at 36pt even with
                    // a 44pt intrinsic size; opting out leaves the bar unchanged.
                    if (@available(iOS 26.0, *)) keyItem.hidesSharedBackground = YES;
                }
                // The host keeps the navigation bar (Back, the key note, full screen, share)
                // on screen from the first frame and the sheet inside the detail column.
                TMSheetMusicViewController *sheet =
                    [[TMSheetMusicViewController alloc] initWithPreviewer:previewer
                                                                    title:self.tag.title
                                                                  fileURL:[NSURL fileURLWithPath:[DPFileCache pathForKey:key]]
                                                                  keyItem:keyItem];
                if (self.navigationController) {
                    [self.navigationController pushViewController:sheet animated:YES];
                } else {
                    sheet.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone
                                                                                                           target:sheet
                                                                                                           action:@selector(done)];
                    [self presentViewController:[[UINavigationController alloc] initWithRootViewController:sheet] animated:YES completion:NULL];
                }
            });
        } @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self setButton:self.sheetMusicButton busy:NO];
                [self tm_showError:@"Sheet music couldn't be opened. Check your connection and try again." retry:^{ [self openSheetMusic]; }];
            });
        }
    });
}

// Shipping deadline for a timed (accessibility-activated) key note. Only the
// tests reassign it, and they restore it; the app never calls the setter, so the
// delay below is the same 1.5 seconds it has always been.
static NSTimeInterval TMTimedKeyNoteDuration = 1.5;

+ (NSTimeInterval)timedKeyNoteDuration { return TMTimedKeyNoteDuration; }
+ (void)setTimedKeyNoteDuration:(NSTimeInterval)duration { TMTimedKeyNoteDuration = duration; }

- (void)cancelTimedKeyNote {
    // UIKit input, note binding and the delayed callback all run on the main queue.
    self.keyActivationGeneration++;
}

- (void)pitchTouchDown {
    [self cancelTimedKeyNote];
    [self.tag.keyNote play];
}

- (void)pitchTouchUp {
    [self cancelTimedKeyNote];
    [self.tag.keyNote stop];
}

- (void)playKeyNote {
    [self cancelTimedKeyNote];
    NSUInteger generation = self.keyActivationGeneration;
    DPNote *note = self.tag.keyNote;
    [note play];
    __weak DPTagSummaryController *weakSelf = self;
    dispatch_time_t delayTime = dispatch_time(DISPATCH_TIME_NOW, DPTagSummaryController.timedKeyNoteDuration * NSEC_PER_SEC);
    dispatch_after(delayTime, dispatch_get_main_queue(), ^{
        DPTagSummaryController *owner = weakSelf;
        // A cancelled/replaced activation no longer owns this shared note's cleanup.
        if (owner && owner.keyActivationGeneration == generation) [note stop];
    });
}

- (id<QLPreviewItem>)previewController:(QLPreviewController *)controller previewItemAtIndex:(NSInteger)index {
    NSURL *url = [NSURL fileURLWithPath:[DPFileCache pathForKey:self.tag.sheetMusicUri.cacheKey]];
    DPSheetMusicPreview *preview = [[DPSheetMusicPreview alloc] init];
    preview.previewItemURL = url;
    preview.previewItemTitle = self.tag.title;
    return preview;
}

- (NSInteger)numberOfPreviewItemsInPreviewController:(QLPreviewController *)controller {
    return 1;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
