//
//  DPKeysViewController.m
//  pitchperfect
//
//  Created by David Poll on 6/22/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPKeysViewController.h"
#import <GoogleMobileAds/GoogleMobileAds.h>
#import "DPNote.h"
#import "DPKey.h"
#import "DPAccidental.h"
#import "LayoutManagers.h"
#import "DPUtils+UIControl.h"
#import "DPUtils+UIColor.h"
#import "DPSettingsViewController.h"
#import "DPAppDelegate.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "pitchperfect-Swift.h"

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
    label.textColor = self.textLabel.textColor;
    label.backgroundColor = [UIColor clearColor];
    label.userInteractionEnabled = NO;
    [label sizeToFit];
    return label;
}

- (UIView *)noteUi:(DPKey *)k {
    DPGridLayout *flow = [[DPGridLayout alloc] init];
    flow.columnDimensions = @[[DPGridDimension dimension],
                              [DPGridDimension dimension]];
    flow.rowDimensions = @[[DPGridDimension dimension]];
    DPNote *n = k.note;
    UILabel *noteName = [[UILabel alloc] init];
    noteName.font = [UIFont boldSystemFontOfSize:16];
    noteName.text = k.friendlyName;
    noteName.textColor = self.textLabel.textColor;
    noteName.backgroundColor = [UIColor clearColor];
    noteName.userInteractionEnabled = NO;
    [noteName sizeToFit];
    [flow addSubview:noteName row:0 column:0];
    
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
    
    flow.userInteractionEnabled = NO;
    [flow addSubview:accidental row:0 column:1];
    
    return flow;
}

- (void)setKey:(DPKey *)newKey {
    key = newKey;
    
    DPGridLayout *gridLayout = [[DPGridLayout alloc] init];
    gridLayout.columnDimensions = @[[DPGridDimension dimension],
                                    [DPGridDimension dimensionWithStars:1],
                                    [DPGridDimension dimension]];
    gridLayout.rowDimensions = @[[DPGridDimension dimension]];
    gridLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    UIView *flowRight = [self noteUi:newKey];
    UIView *flowLeft = [self keyUi:newKey];
    
    [gridLayout addSubview:flowLeft row:0 column:0];
    [gridLayout addSubview:flowRight row:0 column:2];
    
    id leftLayoutGuide = self.contentView.leftSafeAreaLayoutGuide;
    id rightLayoutGuide = self.contentView.rightSafeAreaLayoutGuide;
    
    [self.contentView addSubview:gridLayout];
    
    [self.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:[leftLayoutGuide]-[gridLayout]-[rightLayoutGuide]"
                                                                             options:0
                                                                             metrics:nil
                                                                               views:NSDictionaryOfVariableBindings(gridLayout, leftLayoutGuide, rightLayoutGuide)]];
    [self.contentView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[gridLayout]|"
                                                                             options:0
                                                                             metrics:nil
                                                                               views:NSDictionaryOfVariableBindings(gridLayout)]];
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
@property (nonatomic, strong) UIBarButtonItem *settingsButton;

@end

@implementation DPKeysViewController

@synthesize bannerView, tableView, keys, settingsButton;

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
    
    keys = [DPKey majorKeys];
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
    
    UISegmentedControl *majorMinorChooser = [[UISegmentedControl alloc] initWithItems:[NSArray arrayWithObjects:@"Major", @"Minor", nil]];
    majorMinorChooser.selectedSegmentIndex = 0;
    [majorMinorChooser sizeToFit];
    
    __weak UISegmentedControl *weakMajorMinorChooser = majorMinorChooser;
    
    [majorMinorChooser addBlock:^{
        for (DPKey *key in self.keys) {
            [key.note stop];
        }
        switch(weakMajorMinorChooser.selectedSegmentIndex) {
            case 0:
                self->keys = [DPKey majorKeys];
                break;
            case 1:
                self->keys = [DPKey minorKeys];
                break;
        }
        [self->tableView reloadData];
    } forControlEvents:UIControlEventValueChanged];
    
    tableView = [[UITableView alloc] init];
    tableView.dataSource = self;
    tableView.allowsSelection = NO;
    tableView.backgroundColor = [UIColor clearColor];
    [rootLayout addSubview:tableView row:2 column:0];
    
    [tableView scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:(keys.count / 2) inSection:0] atScrollPosition:UITableViewScrollPositionMiddle animated:NO];
    
    UIBarButtonItem *majorMinorChooserItem = [[UIBarButtonItem alloc] initWithCustomView:majorMinorChooser];
        
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    settingsButton = [DPCommon getSettingsButtonWithTarget:self selector:@selector(openSettings)];
    self.toolbar.items = [NSArray arrayWithObjects:flexibleSpace, majorMinorChooserItem, flexibleSpace, settingsButton, nil];
    
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    [self.view addSubview:rootLayout];
    
    id bottomLayoutGuide = self.bottomLayoutGuide;
    
    self.edgesForExtendedLayout = UIRectEdgeNone;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[toolbar][rootLayout][bottomLayoutGuide]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(toolbar, rootLayout, bottomLayoutGuide)]];
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
    for (int x = 0; x < keys.count; x++) {
        [[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:x inSection:0]] setHighlighted:NO animated:NO];
        DPKey *key = [keys objectAtIndex:x];
        [key.note stop];
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
    DPKey *key = [keys objectAtIndex:indexPath.row];
    DPKeyCell *cell = [[DPKeyCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:@"Cell"];
    cell.backgroundColor = [UIColor clearColor];
    cell.key = key;
    return cell;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return keys.count;
}

- (void)openSettings {
    [DPCommon openSettings:self barButtonItem:settingsButton];
}

@end
