//
//  DPSongListViewController.m
//  pitchperfect
//
//  Created by David Poll on 6/22/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPSongListViewController.h"
#import "GoogleMobileAdsStub.h"
#import "DPAppDelegate.h"
#import "DPAppDelegate+Ads.h"
#import "DPNote.h"
#import "DPKey.h"
#import "DPAccidental.h"
#import "DPPitchedSong.h"
#import "LayoutManagers.h"
#import "DPUtils+UIControl.h"
#import "DPUtils+UIColor.h"
#import "DPSongsModel.h"
#import "DPSettingsViewController.h"
#import "DPSongEditorViewController.h"
#import "DPAppDelegate.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "UIToolbar+DPUtils.h"
#import "pitchperfect-Swift.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"

@interface DPSongCell : UITableViewCell

@property (nonatomic, strong) DPPitchedSong *song;

@end

@implementation DPSongCell

@synthesize song;

- (UIView *)noteUi:(DPKey *)k {
    DPNote *n = k.note;
    HLayoutView *flow = [[HLayoutView alloc] init];
    UILabel *noteName = [[UILabel alloc] init];
    noteName.font = [UIFont boldSystemFontOfSize:16];
    noteName.text = k.friendlyName;
    noteName.textColor = self.textLabel.textColor;
    noteName.backgroundColor = [UIColor clearColor];
    noteName.userInteractionEnabled = NO;
    
    [noteName sizeToFit];
    [flow addSubview:noteName];
    
    UILabel *accidental = [[UILabel alloc] init];
    accidental.font = [UIFont fontWithName:@"NoteHedz" size:24];
    accidental.textColor = self.textLabel.textColor;
    accidental.backgroundColor = [UIColor clearColor];
    accidental.userInteractionEnabled = NO;
    switch (n.accidental.get) {
        case Sharp:
            accidental.text = SHARP_STRING;
            break;
        case Flat:
            accidental.text = FLAT_STRING;
            break;
        default:
            break;
    }
    [accidental sizeToFit];
    
    [flow addSubview:accidental];
    flow.userInteractionEnabled = NO;
    
    [flow sizeToFit];
    return flow;
}

- (void)setSong:(DPPitchedSong *)newSong {
    song = newSong;
    
    HLayoutView *flowRight = [[HLayoutView alloc] init];
    [flowRight addSubview:[self noteUi:song.key]];
    
    flowRight.frame = CGRectInset(self.frame, 10, 0);
    flowRight.hAlignment = UIControlContentHorizontalAlignmentRight;
    flowRight.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
    flowRight.userInteractionEnabled = NO;
    
    self.textLabel.text = song.name;
    self.textLabel.textColor = self.textLabel.textColor;
    self.textLabel.userInteractionEnabled = NO;
    
    [self.contentView addSubview:flowRight];
    [self sizeToFit];
    
    self.frame = CGRectInset(self.frame, 0, -20);
    self.contentMode = UIControlContentVerticalAlignmentCenter | UIControlContentVerticalAlignmentFill;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchStarted:song.key.note forCell:self];
    [super touchesBegan:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchEnded:song.key.note forCell:self];
    [super touchesEnded:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchEnded:song.key.note forCell:self];
    [super touchesCancelled:touches withEvent:event];
}

@end

@interface DPSongListViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) UITableView *tableView;
@property (nonatomic, strong) UIBarButtonItem *editItem;
@property (nonatomic, strong) UIBarButtonItem *sortItem;
@property (nonatomic, strong) UIBarButtonItem *doneItem;
@property (nonatomic, strong) UIBarButtonItem *addItem;
@property (nonatomic, strong) UIBarButtonItem *addButton;
@property (nonatomic, strong) UIBarButtonItem *settingsButton;

@end

@implementation DPSongListViewController

@synthesize bannerView, tableView, editItem, doneItem, sortItem, addItem, addButton, settingsButton;

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UINavigationItem *navigationItem = self.topNavigationItem;
    
	// Do any additional setup after loading the view.
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimensionWithStars:1],
                                 [DPGridDimension dimension]
                                 ];
    
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
    tableView.delegate = self;
    tableView.allowsSelection = NO;
    tableView.backgroundColor = [DPTheme staffBackgroundColor];
    tableView.backgroundView = [[UIView alloc] initWithFrame:CGRectZero];
    tableView.backgroundView.backgroundColor = [DPTheme staffBackgroundColor];
    tableView.opaque = NO;
    [rootLayout addSubview:tableView row:0 column:0];
    
    
    settingsButton = [DPCommon getSettingsButtonWithTarget:self selector:@selector(openSettings)];

    addButton = [DPCommon barButtonWithSystemName:@"plus"
                                                  target:self
                                                selector:@selector(addSong)];
    
    editItem = [DPCommon barButtonWithSystemName:@"pencil"
                                            target:self
                                          selector:@selector(edit)];
    
    sortItem = [[UIBarButtonItem alloc] initWithTitle:@"Sort Alphabetically" style:UIBarButtonItemStylePlain target:self action:@selector(sort)];
    
    doneItem = [DPCommon barButtonWithSystemName:@"checkmark"
                                            target:self
                                          selector:@selector(doneEditing)];
    
    navigationItem.leftBarButtonItem = editItem;
    navigationItem.rightBarButtonItem = settingsButton;
    
    [NSNotificationCenter.defaultCenter addObserver:self
                                           selector:@selector(songsChanged)
                                               name:[DPSongsModel songsChangedNotificationName]
                                             object:[DPSongsModel sharedInstance].defaultSongList];
    
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

- (void)sort {
    [[DPSongsModel sharedInstance].defaultSongList sortSongs];
    [[DPSongsModel sharedInstance].defaultSongList storeValue];
    [tableView reloadData];
}

- (void)viewDidDisappear:(BOOL)animated {
    for (int x = 0; x < [DPSongsModel sharedInstance].defaultSongList.songs.count; x++) {
        [[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:x inSection:0]] setHighlighted:NO animated:NO];
        DPPitchedSong *song = [[DPSongsModel sharedInstance].defaultSongList.songs objectAtIndex:x];
        [song.key.note stop];
    }
    [super viewDidDisappear:animated];
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    return UIInterfaceOrientationMaskPortrait;
}

- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation {
    return UIInterfaceOrientationPortrait;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    DPPitchedSong *song = [[[DPSongsModel sharedInstance].defaultSongList songs] objectAtIndex:indexPath.row];
    DPSongCell *cell = [[DPSongCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:@"Cell"];
    [DPTheme styleListCell:cell];
    UIButton *disclosureButton = [UIButton buttonWithType:UIButtonTypeDetailDisclosure];
    __block __weak UIButton *weakDisclosureButton = disclosureButton;
    cell.editingAccessoryView = disclosureButton;
    [disclosureButton addBlock:^{
        [self editSong:song fromUi:weakDisclosureButton];
    } forControlEvents:UIControlEventTouchUpInside];
    cell.song = song;
    return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return [[DPSongsModel sharedInstance].defaultSongList songs].count;
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)sourceIndexPath toIndexPath:(NSIndexPath *)destinationIndexPath {
    DPPitchedSong * song = [DPSongsModel sharedInstance].defaultSongList.songs[sourceIndexPath.row];
    [[DPSongsModel sharedInstance].defaultSongList removeSongAtIndex:sourceIndexPath.row];
    [[DPSongsModel sharedInstance].defaultSongList addSong:song atIndex:destinationIndexPath.row];
    [[DPSongsModel sharedInstance].defaultSongList storeValue];
}

- (void)tableView:(UITableView *)view commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        [[DPSongsModel sharedInstance].defaultSongList removeSongAtIndex:indexPath.row];
        [[DPSongsModel sharedInstance].defaultSongList storeValue];
        [tableView reloadData];
    }
}

- (UITableViewCellEditingStyle)tableView:(UITableView *)tableView editingStyleForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewCellEditingStyleDelete;
}

- (void)edit {
    [tableView setEditing:YES animated:YES];
    [self.topNavigationItem setLeftBarButtonItems:@[doneItem, sortItem]
                                         animated:YES];
    [self.topNavigationItem setRightBarButtonItem:addButton animated:YES];
}

- (void)doneEditing {
    [tableView setEditing:NO animated:YES];
    [self.topNavigationItem setLeftBarButtonItem:editItem animated:YES];
    [self.topNavigationItem setRightBarButtonItem:settingsButton animated:YES];
}

- (void)openSettings {
    [DPCommon openSettings:self barButtonItem:settingsButton];
}

- (void)editSong:(DPPitchedSong *)song fromUi:(UIView *)view {
    DPSongEditorViewController *editor = [[DPSongEditorViewController alloc] init];
    UINavigationController *navigationController =
        [[UINavigationController alloc] initWithRootViewController:editor];
    navigationController.preferredContentSize = CGSizeMake(320, 480);
    navigationController.modalTransitionStyle = UIModalTransitionStyleFlipHorizontal;
    navigationController.modalPresentationStyle = UIModalPresentationAutomatic;
    editor.song = song;
    editor.completionCallback = ^(BOOL cancelled) {
        if (!cancelled) {
            [self->tableView reloadData];
            [[DPSongsModel sharedInstance].defaultSongList storeValue];
        }
    };
    [self presentViewController:navigationController animated:YES completion:nil];
}

- (void)addSong {
    DPSongEditorViewController *editor = [[DPSongEditorViewController alloc] init];
    UINavigationController *navigationController =
        [[UINavigationController alloc] initWithRootViewController:editor];
    navigationController.preferredContentSize = CGSizeMake(320, 480);
    navigationController.modalTransitionStyle = UIModalTransitionStyleCoverVertical;
    navigationController.modalPresentationStyle = UIModalPresentationAutomatic;
    DPPitchedSong *newSong = [[DPPitchedSong alloc] init];
    newSong.key = [[DPKey majorKeys] objectAtIndex:[DPKey majorKeys].count / 2];
    editor.song = newSong;
    editor.completionCallback = ^(BOOL cancelled) {
        if (!cancelled) {
            [[DPSongsModel sharedInstance].defaultSongList addSong:newSong];
            [[DPSongsModel sharedInstance].defaultSongList storeValue];
            [self->tableView reloadData];
            [self->tableView scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:[DPSongsModel sharedInstance].defaultSongList.songs.count - 1 inSection:0] atScrollPosition:UITableViewScrollPositionMiddle animated:YES];
        } else {
            [[DPSongsModel sharedInstance].defaultSongList removeSong:newSong];
            [[DPSongsModel sharedInstance].defaultSongList storeValue];
        }
    };
    [self presentViewController:navigationController animated:YES completion:nil];
}

- (void)songsChanged {
    [tableView reloadData];
}

@end
