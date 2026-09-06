//
//  DPSecondViewController.m
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPNotesViewController.h"
#import "GoogleMobileAdsStub.h"
#import "LayoutManagers.h"
#import "DPNote.h"
#import "DPUtils+UIColor.h"
#import "DPUtils+UIControl.h"
#import "DPAccidental.h"
#import "DPSettingsViewController.h"
#import "DPAppDelegate.h"
#import "DPAppDelegate+Ads.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "UIToolbar+DPUtils.h"
#import "pitchperfect-Swift.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"

@interface DPNoteCell : UITableViewCell

@property (nonatomic, strong) DPNote *note;

@end

@implementation DPNoteCell

@synthesize note;

// One label per note: the name in the condensed face, the accidental in
// NoteHedz, and the octave as a small subscript, as on the Android row.
- (UIView *)noteUi:(DPNote *)n {
    NSMutableAttributedString *text = [[NSMutableAttributedString alloc]
        initWithString:n.friendlyName
            attributes:@{
                NSFontAttributeName: [DPTheme listTitleFontWithSize:24],
                NSForegroundColorAttributeName: DPTheme.plateInk,
            }];
    NSString *glyph = nil;
    switch (n.accidental.get) {
        case Sharp:
            glyph = SHARP_STRING;
            break;
        case Flat:
            glyph = FLAT_STRING;
            break;
        default:
            break;
    }
    if (glyph != nil) {
        [text appendAttributedString:[[NSAttributedString alloc]
            initWithString:glyph
                attributes:@{
                    NSFontAttributeName: [UIFont fontWithName:@"NoteHedz" size:26] ?: [DPTheme listTitleFontWithSize:24],
                    NSForegroundColorAttributeName: DPTheme.plateInk,
                }]];
    }
    [text appendAttributedString:[[NSAttributedString alloc]
        initWithString:[NSString stringWithFormat:@"%d", n.octave]
            attributes:@{
                NSFontAttributeName: [DPTheme listTitleFontWithSize:14],
                NSForegroundColorAttributeName: DPTheme.plateInkSecondary,
                NSBaselineOffsetAttributeName: @(-5),
            }]];
    UILabel *label = [[UILabel alloc] init];
    label.attributedText = text;
    label.backgroundColor = [UIColor clearColor];
    label.userInteractionEnabled = NO;
    [label sizeToFit];
    return label;
}

- (void)setNote:(DPNote *)newNote {
    note = newNote;
    
    HLayoutView *flow = [[HLayoutView alloc] init];
    [flow addSubview:[self noteUi:newNote]];
    
    flow.frame = CGRectInset(self.frame, 10, 0);
    flow.hAlignment = UIControlContentHorizontalAlignmentLeft;
    
    if (note.alternate) {
        UILabel *slash = [[UILabel alloc] init];
        slash.text = @"/";
        slash.textColor = DPTheme.plateInkSecondary;
        slash.font = [DPTheme listTitleFontWithSize:24];
        slash.backgroundColor = [UIColor clearColor];
        [slash sizeToFit];

        [flow addSubview:slash];
        
        [flow addSubview:[self noteUi:note.alternate]];
    }
    
    [self.contentView addSubview:flow];
    
    // The measurement readout, as on the Android row: monospaced, tracked, secondary ink.
    self.detailTextLabel.attributedText = [[NSAttributedString alloc]
        initWithString:[NSString stringWithFormat:@"%1.2f Hz", note.frequency]
            attributes:@{
                NSFontAttributeName: [DPTheme monospacedFontWithSize:14],
                NSForegroundColorAttributeName: DPTheme.plateInkSecondary,
                NSKernAttributeName: @(14 * 0.04),
            }];
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchStarted:note forCell:self];
    [super touchesBegan:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchEnded:note forCell:self];
    [super touchesEnded:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchEnded:note forCell:self];
    [super touchesCancelled:touches withEvent:event];
}

@end

@interface DPNotesViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) NSArray *notes;
@property (nonatomic, strong) UITableView *tableView;
@property (nonatomic, strong) UIBarButtonItem *settingsButton;

@end

@implementation DPNotesViewController

@synthesize bannerView, notes, tableView, settingsButton;

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UINavigationItem *navigationItem = self.topNavigationItem;
    
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimensionWithStars:1],
                                 [DPGridDimension dimension]
                                 ];
    
    notes = [DPNote prunedNotes];
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] init];
    bannerView.adUnitID = [DPAppDelegate bannerAdUnitID];
    bannerView.adSize = GADPortraitAnchoredAdaptiveBannerAdSizeWithWidth(
        self.view.frame.size.width
    );
    [self resetBannerViewSize];
    
    bannerView.rootViewController = self;
    bannerView.delegate = (id<GADBannerViewDelegate>)UIApplication.sharedApplication.delegate;
    
    [rootLayout addSubview:bannerView row:1 column:0];
    
    UIScrollView *background = [[UIScrollView alloc] init];
    background.scrollEnabled = NO;
    background.backgroundColor = [DPTheme staffBackgroundColor];
    [self.view setBackgroundColor:[UIColor systemBackgroundColor]];
    background.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:background];
    // The glass bars sample this full-bleed scroll surface; without it iOS 26
    // paints an opaque hard edge over non-scrolling content.
    [self setContentScrollView:background forEdge:NSDirectionalRectEdgeAll];
    // DPToolbarViewController (shared, pre-safe-area) opts out of extended
    // layout; Pitch Perfect runs its score surface under the glass bars.
    self.edgesForExtendedLayout = UIRectEdgeAll;
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(background)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(background)]];
    
    // Loaded after layout in resetBannerViewSize so the creative uses the full screen width.
    
    tableView = [[UITableView alloc] init];
    tableView.dataSource = self;
    tableView.allowsSelection = NO;
    tableView.backgroundColor = [DPTheme staffBackgroundColor];
    tableView.backgroundView = [[UIView alloc] initWithFrame:CGRectZero];
    tableView.backgroundView.backgroundColor = [DPTheme staffBackgroundColor];
    tableView.opaque = NO;
    [rootLayout addSubview:tableView row:0 column:0];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [self->tableView scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:(self->notes.count / 2) inSection:0] atScrollPosition:UITableViewScrollPositionMiddle animated:NO];
    });
    
    navigationItem.title = @"Notes";
    settingsButton = [DPCommon getSettingsButtonWithTarget:self
                                                  selector:@selector(openSettings)];
    navigationItem.rightBarButtonItem = settingsButton;
    
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    [self.view addSubview:rootLayout];
        
    // Full-bleed: the score background runs under the glass bars; content
    // starts at the safe area so nothing hides beneath them.
    [rootLayout.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor].active = YES;
    [rootLayout.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor].active = YES;
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[rootLayout]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
}

- (void)resetBannerViewSize {
    [DPAppDelegate resizeAndReloadBannerView:self.bannerView forViewController:self];
}

- (void)viewDidAppear:(BOOL)animated {
    [self resetBannerViewSize];
    [super viewDidAppear:animated];
}

- (void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator {
    [coordinator notifyWhenInteractionChangesUsingBlock:^(id<UIViewControllerTransitionCoordinatorContext>  _Nonnull context) {
        [self resetBannerViewSize];
    }];
    [super viewWillTransitionToSize:size withTransitionCoordinator:coordinator];
}

- (void)viewDidDisappear:(BOOL)animated {
    for (int x = 0; x < notes.count; x++) {
        [[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:x inSection:0]] setHighlighted:NO animated:NO];
        DPNote *note = [notes objectAtIndex:x];
        [note stop];
    }
    [super viewDidDisappear:animated];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    DPNote *note = [notes objectAtIndex:indexPath.row];
    DPNoteCell *cell = [[DPNoteCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:@"Cell"];
    [DPTheme styleListCell:cell];
    cell.note = note;
    
    return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return notes.count;
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    (void)tableView;
    return 1;
}

- (void)openSettings {
    [DPCommon openSettings:self barButtonItem:settingsButton];
}

@end
