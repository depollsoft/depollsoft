//
//  DPSecondViewController.m
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPNotesViewController.h"
#import "GADBannerView.h"
#import "LayoutManagers.h"
#import "DPNote.h"
#import "DPUtils+UIColor.h"
#import "DPUtils+UIControl.h"
#import "DPAccidental.h"
#import "DPSettingsViewController.h"
#import "DPAppDelegate.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"

@interface DPNoteCell : UITableViewCell

@property (nonatomic, strong) DPNote *note;

@end

@implementation DPNoteCell

@synthesize note;

- (UIView *)noteUi:(DPNote *)n {
    HLayoutView *flow = [[HLayoutView alloc] init];
    UILabel *noteName = [[UILabel alloc] init];
    noteName.font = [UIFont boldSystemFontOfSize:16];
    noteName.text = n.friendlyName;
    noteName.textColor = self.textLabel.textColor.invert;
    noteName.backgroundColor = [UIColor clearColor];
    noteName.userInteractionEnabled = NO;
    [noteName sizeToFit];
    [flow addSubview:noteName];
    
    UILabel *accidental = [[UILabel alloc] init];
    accidental.font = [UIFont fontWithName:@"NoteHedz" size:24];
    accidental.textColor = self.textLabel.textColor.invert;
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
    
    UILabel *octave = [[UILabel alloc] init];
    octave.font = [UIFont systemFontOfSize:10];
    octave.text = [NSString stringWithFormat:@"%d", n.octave];
    octave.textColor = self.textLabel.textColor.invert;
    octave.backgroundColor = [UIColor clearColor];
    octave.userInteractionEnabled = NO;
    [octave sizeToFit];
    [flow addSubview:octave];
    flow.userInteractionEnabled = NO;
    
    [flow sizeToFit];
    return flow;
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
        slash.textColor = self.textLabel.textColor.invert;
        slash.font = [UIFont systemFontOfSize:24];
        slash.backgroundColor = [UIColor clearColor];
        [slash sizeToFit];

        [flow addSubview:slash];
        
        [flow addSubview:[self noteUi:note.alternate]];
    }
    
    [self addSubview:flow];
    
    self.detailTextLabel.text = [NSString stringWithFormat:@"%1.2f Hz", note.frequency];
    self.detailTextLabel.textColor = [UIColor lightTextColor];
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

@end

@implementation DPNotesViewController

@synthesize bannerView, notes, tableView;

- (void)viewDidLoad
{
    [super viewDidLoad];
    VLayoutView *topLayout = [[VLayoutView alloc] initWithFrame:self.view.bounds spacing:4];
    topLayout.vAlignment = UIControlContentVerticalAlignmentTop;
    
    notes = [DPNote prunedNotes];
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] initWithAdSize:kGADAdSizeSmartBannerPortrait];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    
    bannerView.rootViewController = self;
    
    UIToolbar *toolbar = [[UIToolbar alloc] init];
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
    tableView.allowsSelection = NO;
    tableView.frame = CGRectMake(0, topLayout.frame.size.height, self.view.frame.size.width, self.view.frame.size.height - topLayout.frame.size.height - self.tabBarController.tabBar.frame.size.height);
    tableView.backgroundColor = [UIColor clearColor];
    [self.view addSubview:tableView];
    
    [tableView scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:(notes.count / 2) inSection:0] atScrollPosition:UITableViewScrollPositionMiddle animated:NO];
    
    UIBarButtonItem *titleItem = [[UIBarButtonItem alloc] initWithTitle:@"Pitch Perfect" style:UIBarButtonItemStylePlain target:nil action:nil];
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    UIBarButtonItem *settingsButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemPageCurl target:self action:@selector(openSettings)];
    toolbar.items = [NSArray arrayWithObjects:flexibleSpace, titleItem, flexibleSpace, settingsButton, nil];
}

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
    bannerView = nil;
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
    cell.note = note;
    
    return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return notes.count;
}

- (void)openSettings {
    DPSettingsViewController *settings = [DPSettingsViewController sharedInstance];
    settings.modalTransitionStyle = UIModalTransitionStylePartialCurl;
    [self presentViewController:settings animated:YES completion:^{
    }];
}

@end
