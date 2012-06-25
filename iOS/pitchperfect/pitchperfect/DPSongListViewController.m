//
//  DPSongListViewController.m
//  pitchperfect
//
//  Created by David Poll on 6/22/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPSongListViewController.h"
#import "GADBannerView.h"
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

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"

@interface DPSongCell : UITableViewCell

@property (nonatomic, strong) DPPitchedSong *song;

@end

@implementation DPSongCell

@synthesize song;

- (UIView *)keyUi:(DPKey *)k {
    NSArray *flats = [NSArray arrayWithObjects:@"", @"\u00A8", @"\u00A9", @"\u00AA", @"\u00AB", @"\u00AC", @"\u20AC", @"\u00AE", nil];
    NSArray *sharps = [NSArray arrayWithObjects:@"", @"\u00A1", @"\u00A2", @"\u00A3", @"\u00A4", @"\u00A5", @"\u00A6", @"\u00A7", nil];
    UILabel *label = [[UILabel alloc] init];
    NSMutableString *string = [NSMutableString stringWithString:@"&"];
    if (k.numAccidentals > 0) {
        [string appendFormat:[sharps objectAtIndex:k.numAccidentals]];
    } else if (k.numAccidentals < 0) {
        [string appendString:[flats objectAtIndex:-k.numAccidentals]];
    }
    label.text = [NSString stringWithString:string];
    label.font = [UIFont fontWithName:@"MusiQwik" size:30];
    label.textColor = self.textLabel.textColor.invert;
    label.backgroundColor = [UIColor clearColor];
    [label sizeToFit];
    return label;
}

- (UIView *)noteUi:(DPKey *)k {
    DPNote *n = k.note;
    HLayoutView *flow = [[HLayoutView alloc] init];
    UILabel *noteName = [[UILabel alloc] init];
    noteName.font = [UIFont boldSystemFontOfSize:16];
    noteName.text = k.friendlyName;
    noteName.textColor = self.textLabel.textColor.invert;
    noteName.backgroundColor = [UIColor clearColor];
    [noteName sizeToFit];
    [flow addSubview:noteName];
    
    UILabel *accidental = [[UILabel alloc] init];
    accidental.font = [UIFont fontWithName:@"NoteHedz" size:24];
    accidental.textColor = self.textLabel.textColor.invert;
    accidental.backgroundColor = [UIColor clearColor];
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
    
    [flow sizeToFit];
    return flow;
}

- (void)setSong:(DPPitchedSong *)newSong {
    song = newSong;
    
    HLayoutView *flowRight = [[HLayoutView alloc] init];
    [flowRight addSubview:[self noteUi:song.key]];
    
    flowRight.frame = CGRectInset(self.frame, 10, 0);
    flowRight.hAlignment = UIControlContentHorizontalAlignmentRight;
    flowRight.autoresizingMask = UIViewAutoresizingFlexibleLeftMargin;
    
    self.textLabel.text = song.name;
    self.textLabel.textColor = self.textLabel.textColor.invert;
    
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

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchEnded:song.key.note forCell:self];
    [super touchesMoved:touches withEvent:event];
}

@end

@interface DPSongListViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) UITableView *tableView;
@property (nonatomic, strong) UIBarButtonItem *editItem;
@property (nonatomic, strong) UIBarButtonItem *doneItem;
@property (nonatomic, strong) UIBarButtonItem *addItem;
@property (nonatomic, strong) NSArray *editingButtons;
@property (nonatomic, strong) NSArray *normalButtons;
@property (nonatomic, strong) UIToolbar *toolbar;

@end

@implementation DPSongListViewController

@synthesize bannerView, tableView, editItem, doneItem, addItem, editingButtons, normalButtons, toolbar;

- (void)viewDidLoad
{
    [super viewDidLoad];
    
	// Do any additional setup after loading the view.
    VLayoutView *topLayout = [[VLayoutView alloc] initWithFrame:self.view.bounds spacing:4];
    topLayout.vAlignment = UIControlContentVerticalAlignmentTop;
    
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] initWithAdSize:kGADAdSizeSmartBannerPortrait];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    
    bannerView.rootViewController = self;
    
    toolbar = [[UIToolbar alloc] init];
    toolbar.barStyle = UIBarStyleBlackTranslucent;
    
    [toolbar sizeToFit];
    [topLayout addSubview:toolbar];
    [topLayout addSubview:bannerView];
    
    UIView *background = [[UIView alloc] initWithFrame:self.view.frame];
    background.backgroundColor = [UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]];
    [self.view setBackgroundColor:[UIColor blackColor]];
    [self.view addSubview:background];
    
    GADRequest *request = [GADRequest request];
    request.testing = [DPAppDelegate testAds];
    [bannerView loadRequest:request];
    
    [topLayout sizeToFit];
    
    [self.view addSubview:topLayout];
    
    tableView = [[UITableView alloc] init];
    tableView.dataSource = self;
    tableView.delegate = self;
    tableView.allowsSelection = NO;
    tableView.frame = CGRectMake(0, topLayout.frame.size.height, self.view.frame.size.width, self.view.frame.size.height - topLayout.frame.size.height - self.tabBarController.tabBar.frame.size.height);
    tableView.backgroundColor = [UIColor clearColor];
    [self.view addSubview:tableView];
    
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    UIBarButtonItem *settingsButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemPageCurl target:self action:@selector(openSettings)];
    
    UIBarButtonItem *addButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAdd target:self action:@selector(addSong)];
    
    editItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemEdit target:self action:@selector(edit)];
    
    doneItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone target:self action:@selector(doneEditing)];
    
    normalButtons = [NSArray arrayWithObjects:flexibleSpace, editItem, settingsButton, nil];
    editingButtons = [NSArray arrayWithObjects:addButton, flexibleSpace, doneItem, settingsButton, nil];
    
    toolbar.items = normalButtons;
    
    [DPSongsModel sharedInstance].delegate = self;
}

- (void)viewDidDisappear:(BOOL)animated {
    for (int x = 0; x < [DPSongsModel sharedInstance].songs.count; x++) {
        [[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:x inSection:0]] setHighlighted:NO animated:NO];
        DPPitchedSong *song = [[DPSongsModel sharedInstance].songs objectAtIndex:x];
        [song.key.note stop];
    }
    [super viewDidDisappear:animated];
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    return (interfaceOrientation == UIInterfaceOrientationPortrait);
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    DPPitchedSong *song = [[[DPSongsModel sharedInstance] songs] objectAtIndex:indexPath.row];
    DPSongCell *cell = [[DPSongCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:@"Cell"];
    cell.editingAccessoryType = UITableViewCellAccessoryDetailDisclosureButton;
    cell.song = song;
    return cell;
}

- (void)tableView:(UITableView *)tableView accessoryButtonTappedForRowWithIndexPath:(NSIndexPath *)indexPath {
    [self editSong:[[DPSongsModel sharedInstance].songs objectAtIndex:indexPath.row]];
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return [[DPSongsModel sharedInstance] songs].count;
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    return YES;
}

- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)sourceIndexPath toIndexPath:(NSIndexPath *)destinationIndexPath {
    NSMutableArray *arr = [DPSongsModel sharedInstance].songs;
    [arr exchangeObjectAtIndex:sourceIndexPath.row withObjectAtIndex:destinationIndexPath.row];
    [DPSongsModel sharedInstance].songs = arr;
}

- (void)tableView:(UITableView *)view commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        [[DPSongsModel sharedInstance].songs removeObjectAtIndex:indexPath.row];
        [DPSongsModel sharedInstance].songs = [DPSongsModel sharedInstance].songs;
        [tableView reloadData];
    }
}

- (UITableViewCellEditingStyle)tableView:(UITableView *)tableView editingStyleForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewCellEditingStyleDelete;
}

- (void)edit {
    [tableView setEditing:YES animated:YES];
    [toolbar setItems:editingButtons animated:YES];
}

- (void)doneEditing {
    [tableView setEditing:NO animated:YES];
    [toolbar setItems:normalButtons animated:YES];
}

- (void)openSettings {
    DPSettingsViewController *settings = [DPSettingsViewController sharedInstance];
    settings.modalTransitionStyle = UIModalTransitionStylePartialCurl;
    [self presentViewController:settings animated:YES completion:^{
    }];
}

- (void)editSong:(DPPitchedSong *)song {
    DPSongEditorViewController *editor = [[DPSongEditorViewController alloc] init];
    editor.modalTransitionStyle = UIModalTransitionStyleFlipHorizontal;
    editor.song = song;
    editor.completionCallback = ^(BOOL cancelled) {
        if (!cancelled) {
            [tableView reloadData];
            [[DPSongsModel sharedInstance] storeValue];
        }
    };
    [self presentModalViewController:editor animated:YES];
}

- (void)addSong {
    DPSongEditorViewController *editor = [[DPSongEditorViewController alloc] init];
    editor.modalTransitionStyle = UIModalTransitionStyleCoverVertical;
    DPPitchedSong *newSong = [[DPPitchedSong alloc] init];
    newSong.key = [[DPKey majorKeys] objectAtIndex:[DPKey majorKeys].count / 2];
    editor.song = newSong;
    editor.completionCallback = ^(BOOL cancelled) {
        if (!cancelled) {
            [[DPSongsModel sharedInstance].songs addObject:newSong];
            [[DPSongsModel sharedInstance] storeValue];
            [tableView reloadData];
            [tableView scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:[DPSongsModel sharedInstance].songs.count - 1 inSection:0] atScrollPosition:UITableViewScrollPositionMiddle animated:YES];
        } else {
            [[DPSongsModel sharedInstance].songs removeObject:newSong];
            [[DPSongsModel sharedInstance] storeValue];
        }
    };
    [tableView reloadData];
    [self presentModalViewController:editor animated:YES];
}

- (void)songsChanged {
    [tableView reloadData];
}

@end