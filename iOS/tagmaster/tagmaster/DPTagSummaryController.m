//
//  DPTagSummaryController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  The singing view of a tag: what it is, what key it starts in, where the
//  chart is, and the words — in that order, because that is the order a group
//  needs them in.
//

#import "DPTagSummaryController.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "DPTextView.h"
#import "DPFileCache.h"
#import "DPPitchPipeButton.h"
#import "tagmaster-Swift.h"
#import <QuickLook/QuickLook.h>

@interface DPSheetMusicPreview : NSObject <QLPreviewItem>

@property (nonatomic, strong) NSURL *previewItemURL;
@property (nonatomic, strong) NSString *previewItemTitle;

@end

@implementation DPSheetMusicPreview

@end

@interface DPTagSummaryController () <QLPreviewControllerDataSource>

@property (nonatomic, strong) UILabel *titleLabel;
@property (nonatomic, strong) UILabel *akaLabel;
@property (nonatomic, strong) UILabel *ratingLabel;
@property (nonatomic, strong) UILabel *ratingStars;
@property (nonatomic, strong) UIButton *ratingButton;
@property (nonatomic, strong) UILabel *partsLabel;
@property (nonatomic, strong) UILabel *typeLabel;
@property (nonatomic, strong) DPPitchPipeButton *keyButton;
@property (nonatomic, strong) UILabel *classicTagNumberLabel;
@property (nonatomic, strong) UIButton *sheetMusicButton;
@property (nonatomic, strong) UILabel *lyricsLabel;
@property (nonatomic, strong) UILabel *notesLabel;

@property (nonatomic, strong) UIView *keyRow;
@property (nonatomic, strong) UIStackView *ratingRow;
@property (nonatomic, strong) UIView *partsRow;
@property (nonatomic, strong) UIView *typeRow;
@property (nonatomic, strong) UIView *classicTagRow;
@property (nonatomic, strong) UIView *lyricsBlock;
@property (nonatomic, strong) UIView *notesBlock;

@property (nonatomic, strong) UIStackView *stack;

@end

@implementation DPTagSummaryController

@synthesize titleLabel, akaLabel, ratingLabel, partsLabel, typeLabel, keyButton, classicTagNumberLabel, sheetMusicButton, ratingButton, lyricsLabel, notesLabel;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)refreshView {
    titleLabel.text = self.tag.title ?: @"Tag";

    akaLabel.text = self.tag.alternativeTitle
        ? [NSString stringWithFormat:@"a.k.a. %@", self.tag.alternativeTitle]
        : nil;
    akaLabel.hidden = !self.tag.alternativeTitle;

    if (self.tag.rating > 0) {
        ratingLabel.text = [NSString stringWithFormat:@"%1.2f out of 5", self.tag.rating];
        self.ratingStars.text = [DPTagSummaryController starsForRating:self.tag.rating];
        self.ratingStars.accessibilityLabel =
            [NSString stringWithFormat:@"Rated %1.2f out of 5", self.tag.rating];
    } else {
        ratingLabel.text = @"Not rated yet";
        self.ratingStars.text = @"☆☆☆☆☆";
        self.ratingStars.accessibilityLabel = @"Not rated yet";
    }

    partsLabel.text = self.tag.parts > 0 ? [NSString stringWithFormat:@"%d", self.tag.parts] : @"—";
    self.partsRow.hidden = self.tag.parts == 0;

    typeLabel.text = self.tag.tagType;
    self.typeRow.hidden = self.tag.tagType.length == 0;

    keyButton.note = [self.tag keyNote];
    NSString *keyTitle = self.tag.writtenKey
        ? [NSString stringWithFormat:@"Sound the key of %@", self.tag.writtenKey]
        : @"Sound the key";
    [keyButton.button setTitle:keyTitle forState:UIControlStateNormal];
    keyButton.button.accessibilityLabel = keyTitle;
    keyButton.button.accessibilityHint = @"Plays the starting pitch";
    self.keyRow.hidden = !self.tag.writtenKey;

    classicTagNumberLabel.text = [NSString stringWithFormat:@"%d", self.tag.classicTagNumber];
    self.classicTagRow.hidden = self.tag.classicTagNumber == 0;

    sheetMusicButton.hidden = !self.tag.sheetMusicUri;

    lyricsLabel.text = self.tag.lyrics;
    self.lyricsBlock.hidden = !self.tag.lyrics;

    notesLabel.text = self.tag.notes;
    self.notesBlock.hidden = !self.tag.notes;

    [self.ratingButton setEnabled:YES];
}

+ (NSString *)starsForRating:(float)rating {
    NSMutableString *stars = [NSMutableString string];
    for (int index = 1; index <= 5; index++) {
        [stars appendString:rating >= index - 0.25 ? @"★" : @"☆"];
    }
    return stars;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(pitchTouchUp)
        name:UIApplicationWillResignActiveNotification object:nil];

    UIScrollView *scroller = [[UIScrollView alloc] init];

    titleLabel = [self makeTitleLabel];
    titleLabel.accessibilityTraits = UIAccessibilityTraitHeader;

    akaLabel = [[UILabel alloc] init];
    akaLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleSubheadline];
    akaLabel.adjustsFontForContentSizeCategory = YES;
    akaLabel.textColor = [TMTheme secondaryText];
    akaLabel.numberOfLines = 0;

    // The pitch comes first: it is the one control a group reaches for while
    // standing in a circle.
    keyButton = [[DPPitchPipeButton alloc] init];
    keyButton.button.titleLabel.font = [TMTheme fontWithStyle:UIFontTextStyleBody
                                                        weight:UIFontWeightSemibold];
    keyButton.button.titleLabel.adjustsFontForContentSizeCategory = YES;
    UIButtonConfiguration *keyConfig = [UIButtonConfiguration filledButtonConfiguration];
    keyConfig.contentInsets = NSDirectionalEdgeInsetsMake(TMTheme.spaceM, TMTheme.spaceL,
                                                          TMTheme.spaceM, TMTheme.spaceL);
    keyConfig.baseForegroundColor = [UIColor systemBackgroundColor];
    keyConfig.image = [UIImage systemImageNamed:@"tuningfork"];
    keyConfig.imagePadding = TMTheme.spaceS;
    keyButton.button.configuration = keyConfig;
    [keyButton.button.heightAnchor
        constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;
    self.keyRow = keyButton;

    sheetMusicButton = [UIButton buttonWithType:UIButtonTypeSystem];
    UIButtonConfiguration *sheetConfig = [UIButtonConfiguration tintedButtonConfiguration];
    sheetConfig.title = @"Sheet Music";
    sheetConfig.image = [UIImage systemImageNamed:@"doc.text"];
    sheetConfig.imagePadding = TMTheme.spaceS;
    sheetConfig.contentInsets = NSDirectionalEdgeInsetsMake(TMTheme.spaceM, TMTheme.spaceL,
                                                            TMTheme.spaceM, TMTheme.spaceL);
    sheetMusicButton.configuration = sheetConfig;
    sheetMusicButton.titleLabel.adjustsFontForContentSizeCategory = YES;
    sheetMusicButton.accessibilityHint = @"Opens the chart for this tag";
    [sheetMusicButton addTarget:self action:@selector(openSheetMusic) forControlEvents:UIControlEventTouchUpInside];
    [sheetMusicButton.heightAnchor
        constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;

    self.ratingStars = [[UILabel alloc] init];
    self.ratingStars.font = [TMTheme fontWithStyle:UIFontTextStyleTitle3 weight:UIFontWeightRegular];
    self.ratingStars.adjustsFontForContentSizeCategory = YES;
    self.ratingStars.textColor = [TMTheme ink];

    ratingLabel = [[UILabel alloc] init];
    ratingLabel.adjustsFontForContentSizeCategory = YES;
    ratingLabel.textColor = [TMTheme secondaryText];
    ratingLabel.font = [TMTheme metadataFont];
    ratingLabel.numberOfLines = 0;

    ratingButton = [UIButton buttonWithType:UIButtonTypeSystem];
    UIButtonConfiguration *rateConfig = [UIButtonConfiguration borderedButtonConfiguration];
    rateConfig.title = @"Rate";
    rateConfig.contentInsets = NSDirectionalEdgeInsetsMake(TMTheme.spaceS, TMTheme.spaceL,
                                                           TMTheme.spaceS, TMTheme.spaceL);
    ratingButton.configuration = rateConfig;
    ratingButton.accessibilityHint = @"Rate this tag from one to five stars";
    [ratingButton.heightAnchor
        constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;
    [ratingButton addTarget:self action:@selector(rate) forControlEvents:UIControlEventTouchUpInside];
    [ratingButton setContentHuggingPriority:UILayoutPriorityRequired
                                    forAxis:UILayoutConstraintAxisHorizontal];

    UIStackView *ratingText =
        [[UIStackView alloc] initWithArrangedSubviews:@[self.ratingStars, ratingLabel]];
    ratingText.axis = UILayoutConstraintAxisVertical;
    ratingText.spacing = TMTheme.spaceXS;
    UIStackView *ratingGroup = [[UIStackView alloc] initWithArrangedSubviews:@[ratingText, ratingButton]];
    ratingGroup.axis = UILayoutConstraintAxisHorizontal;
    ratingGroup.alignment = UIStackViewAlignmentCenter;
    ratingGroup.spacing = TMTheme.spaceL;
    self.ratingRow = ratingGroup;

    partsLabel = [self makeBodyLabel];
    typeLabel = [self makeBodyLabel];
    typeLabel.numberOfLines = 0;
    classicTagNumberLabel = [self makeBodyLabel];

    self.partsRow = [TMFieldRow fieldRowWithTitle:@"Parts" value:partsLabel];
    self.typeRow = [TMFieldRow fieldRowWithTitle:@"Type" value:typeLabel];
    self.classicTagRow = [TMFieldRow fieldRowWithTitle:@"Classic Tag" value:classicTagNumberLabel];

    lyricsLabel = [self makeBodyLabel];
    lyricsLabel.numberOfLines = 0;
    notesLabel = [self makeBodyLabel];
    notesLabel.numberOfLines = 0;

    self.lyricsBlock = [self makeBlockWithTitle:@"Lyrics" body:lyricsLabel];
    self.notesBlock = [self makeBlockWithTitle:@"Notes" body:notesLabel];

    self.stack = [[UIStackView alloc] initWithArrangedSubviews:@[
        titleLabel, akaLabel, keyButton, sheetMusicButton, ratingGroup,
        self.partsRow, self.typeRow, self.classicTagRow,
        self.lyricsBlock, self.notesBlock
    ]];
    self.stack.axis = UILayoutConstraintAxisVertical;
    self.stack.alignment = UIStackViewAlignmentFill;
    self.stack.spacing = TMTheme.spaceM;
    [self.stack setCustomSpacing:TMTheme.spaceXS afterView:titleLabel];
    [self.stack setCustomSpacing:TMTheme.spaceL afterView:akaLabel];
    [self.stack setCustomSpacing:TMTheme.spaceXL afterView:ratingGroup];
    [self.stack setCustomSpacing:TMTheme.spaceXL afterView:self.classicTagRow];
    [self.stack setCustomSpacing:TMTheme.spaceXL afterView:self.lyricsBlock];

    [self setUpRootView:self.stack withScroller:scroller];

    [self refreshView];
}

- (void)viewWillLayoutSubviews {
    [super viewWillLayoutSubviews];
    // Give the rating its own width at accessibility sizes. Keep Rate in the
    // same scrolling group, below the complete rating rather than beside it.
    BOOL stacked = UIContentSizeCategoryIsAccessibilityCategory(self.traitCollection.preferredContentSizeCategory);
    self.ratingRow.axis = stacked ? UILayoutConstraintAxisVertical : UILayoutConstraintAxisHorizontal;
    self.ratingRow.alignment = stacked ? UIStackViewAlignmentFill : UIStackViewAlignmentCenter;
    // In a filled vertical stack, required hugging on Rate would force the
    // entire Summary to the button's intrinsic width.
    [self.ratingButton setContentHuggingPriority:stacked ? UILayoutPriorityDefaultLow : UILayoutPriorityRequired
                                        forAxis:UILayoutConstraintAxisHorizontal];
    self.ratingStars.font = [UIFont preferredFontForTextStyle:UIFontTextStyleTitle3
                              compatibleWithTraitCollection:self.traitCollection];
    self.ratingLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleFootnote
                              compatibleWithTraitCollection:self.traitCollection];
}

/// A heading with more space above it than below, and its body beneath.
- (UIView *)makeBlockWithTitle:(NSString *)title body:(UILabel *)body {
    UILabel *heading = [[UILabel alloc] init];
    heading.text = title;
    heading.font = [TMTheme groupTitleFont];
    heading.adjustsFontForContentSizeCategory = YES;
    heading.textColor = [TMTheme ink];
    heading.accessibilityTraits = UIAccessibilityTraitHeader;

    UIStackView *block = [[UIStackView alloc] initWithArrangedSubviews:@[heading, body]];
    block.axis = UILayoutConstraintAxisVertical;
    block.spacing = TMTheme.spaceS;
    return block;
}

- (void)rate {
    UIAlertController *alertController = [UIAlertController alertControllerWithTitle:@"Rating"
                                                                             message:@"Rate the tag on a scale of 1-5 stars"
                                                                      preferredStyle:UIAlertControllerStyleActionSheet];
    // iPad presents this as a popover and needs both an anchor view and a rect.
    alertController.popoverPresentationController.sourceView = ratingButton;
    alertController.popoverPresentationController.sourceRect = ratingButton.bounds;
    [alertController addAction:[UIAlertAction actionWithTitle:@"★★★★★" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:5];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"★★★★☆" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:4];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"★★★☆☆" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:3];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"★★☆☆☆" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:2];
    }]];
    [alertController addAction:[UIAlertAction actionWithTitle:@"★☆☆☆☆" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self rateTag:1];
    }]];

    [alertController addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                        style:UIAlertActionStyleCancel
                                                      handler:nil]];

    [self presentViewController:alertController animated:YES completion:nil];
}

- (void)rateTag:(NSInteger)rating {
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            [self.tag rate:rating];
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self.ratingButton setEnabled:NO];
                UIButtonConfiguration *rated = self.ratingButton.configuration;
                rated.title = @"Rated";
                self.ratingButton.configuration = rated;
                [TMTheme saved];
            });
        }
        @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self reportProblem:@"Your rating could not be sent."];
            });
        }
    });
}

- (void)reportProblem:(NSString *)message {
    UIAlertController *alert =
        [UIAlertController alertControllerWithTitle:@"Tag Master"
                                            message:[message stringByAppendingString:
                                                     @" Check your connection and try again."]
                                     preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleDefault handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (void)openSheetMusic {
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            NSString *key = self.tag.sheetMusicUri.cacheKey;
            if (![[NSFileManager defaultManager] fileExistsAtPath:[DPFileCache pathForKey:key]]) {
                [DPFileCache writeData:[NSData dataWithContentsOfURL:self.tag.sheetMusicUri.uri] forKey:key];
            }
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                QLPreviewController *previewer = [[QLPreviewController alloc] init];
                previewer.dataSource = self;
                if (self.tag.keyNote) {
                    // Keep the pitch within reach while the chart is open.
                    UIButton *toucher = [UIButton buttonWithType:UIButtonTypeSystem];
                    [toucher setTitle:[NSString stringWithFormat:@"Key: %@", self.tag.keyNote]
                             forState:UIControlStateNormal];
                    toucher.titleLabel.font = [TMTheme fontWithStyle:UIFontTextStyleBody
                                                               weight:UIFontWeightSemibold];
                    [toucher.heightAnchor constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;
                    [toucher.widthAnchor constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;
                    toucher.accessibilityLabel =
                        [NSString stringWithFormat:@"Sound the key of %@", self.tag.keyNote];
                    [toucher addTarget:self action:@selector(pitchTouchDown) forControlEvents:UIControlEventTouchDown];
                    [toucher addTarget:self action:@selector(pitchTouchUp) forControlEvents:UIControlEventTouchUpInside | UIControlEventTouchUpOutside | UIControlEventTouchCancel];
                    previewer.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithCustomView:toucher];
                }
                [self presentViewController:previewer animated:YES completion:NULL];
            });
        } @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self reportProblem:@"That chart could not be downloaded."];
            });
        }
    });
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [self pitchTouchUp];
}

- (void)pitchTouchDown {
    [self.tag.keyNote play];
}

- (void)pitchTouchUp {
    [self.tag.keyNote stop];
}

- (void)playKeyNote {
    [self.tag.keyNote play];
    dispatch_time_t delayTime = dispatch_time(DISPATCH_TIME_NOW, 1.5 * NSEC_PER_SEC);
    dispatch_after(delayTime, dispatch_get_main_queue(), ^{
        [self.tag.keyNote stop];
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
