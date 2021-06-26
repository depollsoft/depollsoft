//
//  DPSecondViewController.m
//  pitchperfect
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPNotesViewController.h"
#import <GoogleMobileAds/GoogleMobileAds.h>
#import "LayoutManagers.h"
#import "DPNote.h"
#import "DPUtils+UIColor.h"
#import "DPUtils+UIControl.h"
#import "DPAccidental.h"
#import "DPSettingsViewController.h"
#import "DPAppDelegate.h"
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

- (UIView *)noteUi:(DPNote *)n {
    HLayoutView *flow = [[HLayoutView alloc] init];
    UILabel *noteName = [[UILabel alloc] init];
    noteName.font = [UIFont boldSystemFontOfSize:16];
    noteName.text = n.friendlyName;
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
    
    UILabel *octave = [[UILabel alloc] init];
    octave.font = [UIFont systemFontOfSize:10];
    octave.text = [NSString stringWithFormat:@"%d", n.octave];
    octave.textColor = self.textLabel.textColor;
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
        slash.textColor = self.textLabel.textColor;
        slash.font = [UIFont systemFontOfSize:24];
        slash.backgroundColor = [UIColor clearColor];
        [slash sizeToFit];

        [flow addSubview:slash];
        
        [flow addSubview:[self noteUi:note.alternate]];
    }
    
    [self.contentView addSubview:flow];
    
    self.detailTextLabel.text = [NSString stringWithFormat:@"%1.2f Hz", note.frequency];
    self.detailTextLabel.textColor = [UIColor systemGrayColor];
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
    
    UIToolbar *toolbar = self.toolbar;
    
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimensionWithStars:1]
                                 ];
    
    notes = [DPNote prunedNotes];
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] init];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    [self resetBannerViewSize];
    
    bannerView.rootViewController = self;
    
    [rootLayout addSubview:bannerView row:1 column:0];
    
    UIView *background = [[UIView alloc] init];
    background.backgroundColor = [[UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]] colorWithAlphaComponent:0.5];
    [self.view setBackgroundColor:[UIColor systemBackgroundColor]];
    background.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:background];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(background)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[toolbar][background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(toolbar, background)]];
    
    [bannerView loadRequest:DPAppDelegate.adRequest];
    
    tableView = [[UITableView alloc] init];
    tableView.dataSource = self;
    tableView.allowsSelection = NO;
    tableView.backgroundColor = [UIColor clearColor];
    [rootLayout addSubview:tableView row:2 column:0];
    
    dispatch_async(dispatch_get_main_queue(), ^{
        [self->tableView scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:(self->notes.count / 2) inSection:0] atScrollPosition:UITableViewScrollPositionMiddle animated:NO];
    });
    
    [toolbar addTitle:@"Pitch Perfect"];
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    settingsButton = [DPCommon getSettingsButtonWithTarget:self selector:@selector(openSettings)];
    toolbar.items = [NSArray arrayWithObjects:flexibleSpace, settingsButton, nil];
    [toolbar sizeToFit];
    
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    [self.view addSubview:rootLayout];
        
    self.edgesForExtendedLayout = UIRectEdgeNone;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[toolbar][rootLayout]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(toolbar, rootLayout)]];
    [rootLayout.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor].active = YES;
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[rootLayout]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
}

- (void)resetBannerViewSize {
    switch ([UIApplication sharedApplication].windows.firstObject.windowScene.interfaceOrientation) {
        case UIInterfaceOrientationLandscapeLeft:
        case UIInterfaceOrientationLandscapeRight:
            self.bannerView.adSize = GADLandscapeAnchoredAdaptiveBannerAdSizeWithWidth(self.view.frame.size.width);
            break;
        case UIInterfaceOrientationPortrait:
        case UIInterfaceOrientationPortraitUpsideDown:
            self.bannerView.adSize = GADPortraitAnchoredAdaptiveBannerAdSizeWithWidth(self.view.frame.size.width);
            break;
        default:
            break;
    }
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
    cell.backgroundColor = [UIColor clearColor];
    cell.note = note;
    
    return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return notes.count;
}

- (void)openSettings {
    [DPCommon openSettings:self barButtonItem:settingsButton];
}

@end
