//
//  DPKeysViewController.m
//  pitchperfect
//
//  Created by David Poll on 6/22/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPKeysViewController.h"
#import "GADBannerView.h"
#import "DPNote.h"
#import "DPKey.h"
#import "DPAccidental.h"
#import "LayoutManagers.h"
#import "DPUtils+UIControl.h"
#import "DPUtils+UIColor.h"
#import "DPSettingsViewController.h"
#import "DPAppDelegate.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"

@interface DPKeyCell : UITableViewCell

@property (nonatomic, strong) DPKey *key;

@end

@implementation DPKeyCell

@synthesize key;

- (UIView *)keyUi:(DPKey *)k {
    NSArray *flats = [NSArray arrayWithObjects:@"", @"\u00A8", @"\u00A9", @"\u00AA", @"\u00AB", @"\u00AC", @"\u20AC", @"\u00AE", nil];
    NSArray *sharps = [NSArray arrayWithObjects:@"", @"\u00A1", @"\u00A2", @"\u00A3", @"\u00A4", @"\u00A5", @"\u00A6", @"\u00A7", nil];
    UILabel *label = [[UILabel alloc] init];
    NSMutableString *string = [NSMutableString stringWithString:@"&"];
    if (k.numAccidentals > 0) {
        [string appendString:[sharps objectAtIndex:k.numAccidentals]];
    } else if (k.numAccidentals < 0) {
        [string appendString:[flats objectAtIndex:-k.numAccidentals]];
    }
    label.text = [NSString stringWithString:string];
    label.font = [UIFont fontWithName:@"MusiQwik" size:30];
    label.textColor = self.textLabel.textColor.invert;
    label.backgroundColor = [UIColor clearColor];
    label.userInteractionEnabled = NO;
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
    
    flow.userInteractionEnabled = NO;
    [flow addSubview:accidental];
    
    [flow sizeToFit];
    return flow;
}

- (void)setKey:(DPKey *)newKey {
    key = newKey;
    
    HLayoutView *flowRight = [[HLayoutView alloc] init];
    [flowRight addSubview:[self noteUi:newKey]];
    
    flowRight.frame = CGRectInset(self.frame, 10, 0);
    flowRight.hAlignment = UIControlContentHorizontalAlignmentRight;
    flowRight.userInteractionEnabled = NO;
    flowRight.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    
    HLayoutView *flowLeft = [[HLayoutView alloc] init];
    [flowLeft addSubview:[self keyUi:newKey]];
    
    flowLeft.frame = CGRectInset(self.frame, 10, 0);
    flowLeft.hAlignment = UIControlContentHorizontalAlignmentLeft;
    flowRight.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    flowLeft.userInteractionEnabled = NO;
    
    [self.contentView addSubview:flowRight];
    [self.contentView addSubview:flowLeft];
    [self sizeToFit];
        
    self.frame = CGRectInset(self.frame, 0, -20);
    self.contentMode = UIControlContentVerticalAlignmentCenter | UIControlContentVerticalAlignmentFill | UIControlContentHorizontalAlignmentFill;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchStarted:key.note forCell:self];
    [super touchesBegan:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchEnded:key.note forCell:self];
    [super touchesEnded:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event {
    [DPAppDelegate noteTouchEnded:key.note forCell:self];
    [super touchesCancelled:touches withEvent:event];
}

@end

@interface DPKeysViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) UITableView *tableView;
@property (nonatomic, strong) NSArray *keys;
@property (nonatomic, strong) UIPopoverController *popover;
@property (nonatomic, strong) UIBarButtonItem *settingsButton;

@end

@implementation DPKeysViewController

@synthesize bannerView, tableView, keys, popover, settingsButton;

- (void)viewDidLoad
{
    [super viewDidLoad];
    
	// Do any additional setup after loading the view.
    VLayoutView *topLayout = [[VLayoutView alloc] initWithFrame:self.view.bounds spacing:4];
    topLayout.vAlignment = UIControlContentVerticalAlignmentTop;
    
    keys = [DPKey majorKeys];
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
    //[self.view setBackgroundColor:[UIColor blackColor]];
    [self.view addSubview:background];
    
    [bannerView loadRequest:DPAppDelegate.adRequest];
    
    UISegmentedControl *majorMinorChooser = [[UISegmentedControl alloc] initWithItems:[NSArray arrayWithObjects:@"Major", @"Minor", nil]];
    majorMinorChooser.selectedSegmentIndex = 0;
    majorMinorChooser.segmentedControlStyle = UISegmentedControlStyleBar;
    [majorMinorChooser sizeToFit];
    
    [majorMinorChooser addBlock:^{
        for (DPKey *key in self.keys) {
            [key.note stop];
        }
        switch(majorMinorChooser.selectedSegmentIndex) {
            case 0:
                keys = [DPKey majorKeys];
                break;
            case 1:
                keys = [DPKey minorKeys];
                break;
        }
        [tableView reloadData];
    } forControlEvents:UIControlEventValueChanged];
    
    [topLayout sizeToFit];
    
    [self.view addSubview:topLayout];
    
    tableView = [[UITableView alloc] init];
    tableView.dataSource = self;
    tableView.allowsSelection = NO;
    tableView.frame = CGRectMake(0, topLayout.frame.size.height, self.view.frame.size.width, self.view.frame.size.height - topLayout.frame.size.height - self.tabBarController.tabBar.frame.size.height);
    tableView.backgroundColor = [UIColor clearColor];
    [self.view addSubview:tableView];
    
    [tableView scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:(keys.count / 2) inSection:0] atScrollPosition:UITableViewScrollPositionMiddle animated:NO];
    
    UIBarButtonItem *majorMinorChooserItem = [[UIBarButtonItem alloc] initWithCustomView:majorMinorChooser];
        
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    settingsButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemPageCurl target:self action:@selector(openSettings)];
    toolbar.items = [NSArray arrayWithObjects:flexibleSpace, majorMinorChooserItem, flexibleSpace, settingsButton, nil];
}

- (void)viewDidDisappear:(BOOL)animated {
    for (int x = 0; x < keys.count; x++) {
        [[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:x inSection:0]] setHighlighted:NO animated:NO];
        DPKey *key = [keys objectAtIndex:x];
        [key.note stop];
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
    DPKey *key = [keys objectAtIndex:indexPath.row];
    DPKeyCell *cell = [[DPKeyCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:@"Cell"];
    cell.key = key;
    return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return keys.count;
}

- (void)openSettings {
    DPSettingsViewController *settings = [DPSettingsViewController sharedInstance];
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        if (self.popover.isPopoverVisible) {
            [popover dismissPopoverAnimated:YES];
            return;
        }
        settings.contentSizeForViewInPopover = CGSizeMake(320, 480);
        popover = [[UIPopoverController alloc] initWithContentViewController:settings];
        settings.popoverController = popover;
        [popover presentPopoverFromBarButtonItem:settingsButton permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
        
    } else {
        settings.modalTransitionStyle = UIModalTransitionStylePartialCurl;
        [self presentViewController:settings animated:YES completion:^{
        }];
    }
}

@end
