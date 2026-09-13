//
//  DPTagSummaryController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagSummaryController.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "DPTextView.h"
#import "DPFileCache.h"
#import "DPPitchPipeButton.h"
#import <QuickLook/QuickLook.h>

// Width-aware wrapping stays local to Tag Master. Shared pitch callbacks own sound.
@interface TMKeyButton : TMWrappingButton
@property (nonatomic, copy) void (^playNote)(void);
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
    UIColor *blue = self.tintColor ?: UIColor.systemBlueColor;
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

@interface TMKeyPitchButton : DPPitchPipeButton
@end
@implementation TMKeyPitchButton
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
    return CGSizeMake(UIViewNoIntrinsicMetric, [self sizeThatFits:CGSizeMake(self.bounds.size.width, CGFLOAT_MAX)].height);
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

@interface DPTagSummaryController () <QLPreviewControllerDataSource, UIActionSheetDelegate>

@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UILabel *akaLabel;
@property (nonatomic, strong) UIProgressView *ratingBar;
@property (nonatomic, strong) UILabel *ratingLabel;
@property (nonatomic, strong) UIButton *ratingButton;
@property (nonatomic, strong) UILabel *partsLabel;
@property (nonatomic, strong) UILabel *typeLabel;
@property (nonatomic, strong) DPPitchPipeButton *keyButton;
@property (nonatomic, strong) UILabel *classicTagNumberLabel;
@property (nonatomic, strong) UIButton *sheetMusicButton;
@property (nonatomic, strong) UILabel *lyricsLabel;
@property (nonatomic, strong) UILabel *notesLabel;
@property (nonatomic, strong) UILabel *ratingHeader;
@property (nonatomic, strong) UILabel *partsHeader;
@property (nonatomic, strong) UILabel *typeHeader;
@property (nonatomic, strong) UILabel *keyHeader;
@property (nonatomic, strong) UILabel *notesHeader;
@property (nonatomic, strong) UILabel *lyricsHeader;
@property (nonatomic, strong) UILabel *classicTagNumberHeader;

@property (nonatomic, strong) DPGridLayout *grid;

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

- (void)refreshView {
    titleLabel.text = self.tag.title;
    
    akaLabel.text = [NSString stringWithFormat:@"a.k.a. %@", self.tag.alternativeTitle];
    [grid setView:akaLabel hidden:!self.tag.alternativeTitle];
    
    ratingLabel.text = [NSString stringWithFormat:@"%1.2f", self.tag.rating];
    ratingLabel.accessibilityValue = ratingLabel.text;
    ratingBar.progress = self.tag.rating / 5;
    
    partsLabel.text = [NSString stringWithFormat:@"%d", self.tag.parts];
    
    typeLabel.text = self.tag.tagType;
    
    keyButton.note = [self.tag keyNote];
    keyButton.button.accessibilityLabel = [NSString stringWithFormat:@"Play key note %@", self.tag.keyNote];
    keyButton.button.accessibilityHint = @"Plays for one and a half seconds";
    [keyButton.button setTitle:self.tag.writtenKey forState:UIControlStateNormal];
    [grid setView:keyButton hidden:!self.tag.writtenKey];
    [grid setView:keyHeader hidden:!self.tag.writtenKey];
    
    classicTagNumberLabel.text = [NSString stringWithFormat:@"%d", self.tag.classicTagNumber];
    [grid setView:classicTagNumberLabel hidden:self.tag.classicTagNumber == 0];
    [grid setView:classicTagNumberHeader hidden:self.tag.classicTagNumber == 0];
    
    [grid setView:sheetMusicButton hidden:!self.tag.sheetMusicUri];
    
    lyricsLabel.text = self.tag.lyrics;
    [grid setView:lyricsLabel hidden:!self.tag.lyrics];
    [grid setView:lyricsHeader hidden:!self.tag.lyrics];
    
    notesLabel.text = self.tag.notes;
    [grid setView:notesLabel hidden:!self.tag.notes];
    [grid setView:notesHeader hidden:!self.tag.notes];
    
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
    ratingLabel = [self makeBodyLabel];
    ratingButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [ratingButton setTitle:@"Rate" forState:UIControlStateNormal];
    partsLabel = [self makeBodyLabel];
    typeLabel = [self makeBodyLabel];
    keyButton = [[TMKeyPitchButton alloc] init];
    TMKeyButton *accessibleKey = [TMKeyButton buttonWithType:UIButtonTypeCustom];
    __weak DPTagSummaryController *weakSelf = self;
    accessibleKey.playNote = ^{ [weakSelf playKeyNote]; };
    keyButton.button = accessibleKey;
    accessibleKey.accessibilityIdentifier = @"summary.key";
    [accessibleKey addTarget:self action:@selector(pitchTouchUp) forControlEvents:UIControlEventTouchCancel];
    keyButton.button.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    keyButton.button.titleLabel.adjustsFontForContentSizeCategory = YES;
    UIButtonConfiguration *config = [UIButtonConfiguration plainButtonConfiguration];
    config.contentInsets = NSDirectionalEdgeInsetsMake(4, 8, 4, 8);
    config.image = [UIImage systemImageNamed:@"key"];
    config.imagePadding = 8;
    keyButton.button.configuration = config;
    classicTagNumberLabel = [self makeBodyLabel];
    sheetMusicButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [sheetMusicButton setTitle:@"Sheet Music" forState:UIControlStateNormal];
    [sheetMusicButton addTarget:self action:@selector(openSheetMusic) forControlEvents:UIControlEventTouchUpInside];
    // Sheet music is the primary action; key is outlined and rating stays plain.
    UIButtonConfiguration *sheetConfiguration = [UIButtonConfiguration filledButtonConfiguration];
    sheetConfiguration.baseForegroundColor = [UIColor whiteColor];
    sheetConfiguration.cornerStyle = UIButtonConfigurationCornerStyleMedium;
    sheetConfiguration.image = [UIImage systemImageNamed:@"doc.richtext"];
    sheetConfiguration.imagePadding = 8;
    sheetConfiguration.titleLineBreakMode = NSLineBreakByWordWrapping;
    sheetConfiguration.contentInsets = NSDirectionalEdgeInsetsMake(4, 16, 4, 16);
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
    
    ratingHeader = [self makeHeader:@"Rating"];
    partsHeader = [self makeHeader:@"Parts"];
    typeHeader = [self makeHeader:@"Type"];
    keyHeader = [self makeHeader:@"Key"];
    notesHeader = [self makeHeader:@"Notes"];
    lyricsHeader = [self makeHeader:@"Lyrics"];
    classicTagNumberHeader = [self makeHeader:@"Classic Tag"];
    
    grid = [[DPGridLayout alloc] init];
    grid.columnDimensions = @[
                              [DPGridDimension dimension],
                              [DPGridDimension dimensionWithSize:8],
                              [DPGridDimension dimensionWithStars:1],
                              ];
    grid.rowDimensions = @[
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension],
                           [DPGridDimension dimension]
                           ];
    
    // Add titles
    [grid addSubview:titleLabel row:0 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:[akaLabel padLeft:0 top:0 right:0 bottom:8]
                 row:1
              column:0
             rowSpan:1
             colSpan:3];
    
    // Add headers
    [grid addSubview:[ratingHeader centeredVertically] row:2 column:0];
    [grid addSubview:partsHeader row:3 column:0];
    [grid addSubview:typeHeader row:4 column:0];
    [grid addSubview:keyHeader row:5 column:0];
    [grid addSubview:classicTagNumberHeader row:6 column:0];
    [grid addSubview:sheetMusicButton row:7 column:0 rowSpan:1 colSpan:3];
    [grid addSubview:[lyricsHeader alignTop] row:8 column:0];
    [grid addSubview:[notesHeader alignTop] row:9 column:0];
    
    // Add content
    [grid addSubview:partsLabel row:3 column:2];
    [grid addSubview:typeLabel row:4 column:2];
    [grid addSubview:keyButton row:5 column:2];
    [grid addSubview:classicTagNumberLabel row:6 column:2];
    [grid addSubview:[lyricsLabel padLeft:0 top:0 right:0 bottom:8] row:8 column:2];
    [grid addSubview:notesLabel row:9 column:2];
    
    // Build rating UI
    UIStackView *ratingValue = [[UIStackView alloc] initWithArrangedSubviews:@[ratingLabel, ratingBar]];
    ratingValue.axis = UILayoutConstraintAxisVertical;
    ratingValue.spacing = 4;
    ratingLabel.textAlignment = NSTextAlignmentCenter;
    ratingLabel.accessibilityLabel = @"Rating out of 5";
    UIStackView *ratingGrid = [[UIStackView alloc] initWithArrangedSubviews:@[ratingValue, ratingButton]];
    ratingGrid.axis = UILayoutConstraintAxisHorizontal;
    ratingGrid.alignment = UIStackViewAlignmentCenter;
    ratingGrid.spacing = 8;
    [ratingButton setContentCompressionResistancePriority:UILayoutPriorityRequired forAxis:UILayoutConstraintAxisHorizontal];
    [grid addSubview:[ratingGrid padHorizontal:0 vertical:4] row:2 column:2];
    
    [ratingButton addTarget:self action:@selector(rate) forControlEvents:UIControlEventTouchUpInside];
    
    [self setUpRootView:grid withScroller:scroller];
    [sheetMusicButton.heightAnchor constraintEqualToAnchor:keyButton.button.heightAnchor].active = YES;
    [ratingButton.heightAnchor constraintEqualToAnchor:keyButton.button.heightAnchor].active = YES;
    
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

/// Shows progress on the control that started the request and blocks only that control.
- (void)setButton:(UIButton *)button busy:(BOOL)busy {
    UIButtonConfiguration *configuration = button.configuration;
    if (configuration) {
        configuration.showsActivityIndicator = busy;
        button.configuration = configuration;
    } else if (button == self.ratingButton) {
        [button setTitle:busy ? @"Sending…" : @"Rate" forState:UIControlStateNormal];
    }
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
                if (self.tag.keyNote) {
                    TMKeyButton *toucher = [TMKeyButton buttonWithType:UIButtonTypeCustom];
                    __weak DPTagSummaryController *weakSelf = self;
                    toucher.playNote = ^{ [weakSelf playKeyNote]; };
                    toucher.note = self.tag.keyNote;
                    toucher.tintColor = UIColor.systemBlueColor;
                    toucher.accessibilityIdentifier = @"sheet.key";
                    UIButtonConfiguration *keyConfiguration = [UIButtonConfiguration plainButtonConfiguration];
                    keyConfiguration.image = [UIImage systemImageNamed:@"key"];
                    keyConfiguration.imagePadding = 8;
                    keyConfiguration.contentInsets = NSDirectionalEdgeInsetsMake(4, 8, 4, 8);
                    toucher.configuration = keyConfiguration;
                    toucher.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
                    toucher.titleLabel.adjustsFontForContentSizeCategory = YES;
                    toucher.accessibilityLabel = [NSString stringWithFormat:@"Play key note %@", self.tag.keyNote];
                    toucher.accessibilityHint = @"Plays for one and a half seconds";
                    [toucher.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
                    [toucher.widthAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
                    [toucher setTitle:[NSString stringWithFormat:@"Key: %@", self.tag.keyNote] forState:UIControlStateNormal];
                    [toucher addTarget:self action:@selector(pitchTouchDown) forControlEvents:UIControlEventTouchDown];
                    [toucher addTarget:self action:@selector(pitchTouchUp) forControlEvents:UIControlEventTouchUpInside | UIControlEventTouchUpOutside | UIControlEventTouchCancel];
                    previewer.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithCustomView:toucher];
                }
                // Pushed previews keep the navigation bar (Back and the key note) on screen
                // from the first frame; a modal preview opened with its chrome hidden.
                previewer.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
                if (self.navigationController) {
                    [self.navigationController pushViewController:previewer animated:YES];
                } else {
                    [self presentViewController:previewer animated:YES completion:NULL];
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

- (void)pitchTouchDown {
    [self.tag.keyNote play];
}

- (void)pitchTouchUp {
    [self.tag.keyNote stop];
}

- (void)playKeyNote {
    DPNote *note = self.tag.keyNote;
    [note play];
    dispatch_time_t delayTime = dispatch_time(DISPATCH_TIME_NOW, 1.5 * NSEC_PER_SEC);
    dispatch_after(delayTime, dispatch_get_main_queue(), ^{
        [note stop];
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
