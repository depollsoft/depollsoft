//
//  DPSongEditorViewController.m
//  pitchperfect
//
//  Created by David Poll on 6/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPSongEditorViewController.h"
#import "DPNote.h"
#import "DPKey.h"
#import "DPAccidental.h"
#import "GoogleMobileAdsStub.h"
#import "DPUtils+UIControl.h"
#import "DPPitchPipeModel.h"
#import "DPPitchPipeButton.h"
#import "LayoutManagers.h"
#import "DPSettingsViewController.h"
#import "DPAppDelegate.h"
#import "DPAppDelegate+Ads.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"
#import "pitchperfect-Swift.h"

#define SHARP_STRING @"ì"
#define FLAT_STRING @"í"
#define LAYOUT_TAG 1337

@interface DPSongEditorViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) UITextField *nameField;
@property (nonatomic, strong) NSArray *allKeys;
@property (nonatomic, strong) UIPickerView *keyPicker;

@end

@implementation DPSongEditorViewController

@synthesize bannerView, song, nameField, allKeys, keyPicker, completionCallback;

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UINavigationItem *navigationItem = self.topNavigationItem;
    
    if ([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        self.view.frame = CGRectMake(0, 0, 320, 480);
    }
    
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimension],
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
    
    NSMutableArray *keys = [NSMutableArray array];
    [keys addObjectsFromArray:[DPKey majorKeys]];
    [keys addObjectsFromArray:[DPKey minorKeys]];
    allKeys = [NSArray arrayWithArray:keys];
    
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
    self.view.backgroundColor = [UIColor systemBackgroundColor];
    
    nameField = [[UITextField alloc] init];
    UILabel *nameLabel = [[UILabel alloc] init];
    nameLabel.text = @" Song Title:";
    nameLabel.textColor = [UIColor lightGrayColor];
    [nameLabel sizeToFit];
    nameField.leftView = nameLabel;
    nameField.leftViewMode = UITextFieldViewModeAlways;
    nameField.text = song.name;
    nameField.contentMode = UIViewContentModeCenter;
    nameField.borderStyle = UITextBorderStyleRoundedRect;
    nameField.returnKeyType = UIReturnKeyDone;
    nameField.autocapitalizationType = UITextAutocapitalizationTypeWords;
    nameField.delegate = self;

    // The key picker sits under the keyboard, so the keyboard must always be
    // dismissible: Done on the return key, Done above the keyboard, and a tap
    // anywhere outside the field.
    UIToolbar *accessoryBar = [[UIToolbar alloc] initWithFrame:CGRectMake(0, 0, self.view.frame.size.width, 44)];
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace
                                                                                   target:nil
                                                                                   action:nil];
    UIBarButtonItem *keyboardDone = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone
                                                                                  target:self
                                                                                  action:@selector(dismissKeyboard)];
    accessoryBar.items = @[flexibleSpace, keyboardDone];
    [accessoryBar sizeToFit];
    nameField.inputAccessoryView = accessoryBar;

    UITapGestureRecognizer *dismissTap = [[UITapGestureRecognizer alloc] initWithTarget:self
                                                                                 action:@selector(dismissKeyboard)];
    dismissTap.cancelsTouchesInView = NO;
    [self.view addGestureRecognizer:dismissTap];

    [nameField sizeToFit];
    
    if ([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPhone) {
        [rootLayout addSubview:bannerView row:2 column:0];
        
        // Loaded after layout in resetBannerViewSize so the creative uses the full screen width.
    }
    [rootLayout addSubview:[nameField pad:5] row:0 column:0];
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    keyPicker = [[UIPickerView alloc] init];
    keyPicker.dataSource = self;
    keyPicker.delegate = self;
    [keyPicker sizeToFit];
    [keyPicker selectRow:[allKeys indexOfObject:song.key] inComponent:0 animated:YES];
    
    [rootLayout addSubview:[keyPicker alignBottom] row:1 column:0];
    
    [self.view addSubview:rootLayout];
    
    UIBarButtonItem *doneItem = [DPCommon barButtonWithSystemName:@"checkmark"
                                                         target:self
                                                       selector:@selector(complete)];
    
    UIBarButtonItem *cancelItem = [DPCommon barButtonWithSystemName:@"xmark"
                                                           target:self
                                                         selector:@selector(cancel)];
    
    navigationItem.title = @"Pitch Perfect";
    navigationItem.leftBarButtonItem = cancelItem;
    navigationItem.rightBarButtonItem = doneItem;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[rootLayout]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
    [rootLayout.topAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.topAnchor].active = YES;
    [rootLayout.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor].active = YES;
    
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

- (void)dismissKeyboard {
    [self.view endEditing:YES];
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return NO;
}

- (void)complete {
    song.name = nameField.text;
    song.key = [allKeys objectAtIndex:[keyPicker selectedRowInComponent:0]];
    [self onComplete:NO];
    [self dismissViewControllerAnimated:YES completion:^{
        
    }];
}

- (void)cancel {
    [self onComplete:YES];
    [self dismissViewControllerAnimated:YES completion:^{
        
    }];
}

- (UIView *)pickerView:(UIPickerView *)pickerView viewForRow:(NSInteger)row forComponent:(NSInteger)component reusingView:(UIView *)view {
    DPKey *key = [allKeys objectAtIndex:row];
    return [self viewForKey:key withPicker:pickerView];
}

- (NSInteger)numberOfComponentsInPickerView:(UIPickerView *)pickerView {
    return 1;
}

- (NSInteger)pickerView:(UIPickerView *)pickerView numberOfRowsInComponent:(NSInteger)component {
    return allKeys.count;
}

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
    noteName.backgroundColor = [UIColor clearColor];
    [noteName sizeToFit];
    [flow addSubview:noteName];
    
    UILabel *accidental = [[UILabel alloc] init];
    accidental.font = [UIFont fontWithName:@"NoteHedz" size:24];
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

- (UIView *)viewForKey:(DPKey *)key withPicker:(UIPickerView *)picker {
    UIView *view = [[UIView alloc] initWithFrame:CGRectMake(0, 0, picker.frame.size.width * .9, 44)];
    HLayoutView *flowRight = [[HLayoutView alloc] init];
    [flowRight addSubview:[self noteUi:key]];
    
    flowRight.frame = CGRectInset(view.frame, 10, 0);
    flowRight.hAlignment = UIControlContentHorizontalAlignmentRight;
    flowRight.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    
    HLayoutView *flowLeft = [[HLayoutView alloc] init];
    [flowLeft addSubview:[self keyUi:key]];
    
    flowLeft.frame = CGRectInset(view.frame, 10, 0);
    flowLeft.hAlignment = UIControlContentHorizontalAlignmentLeft;
    flowRight.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    
    [view addSubview:flowRight];
    [view addSubview:flowLeft];
    
    view.contentMode = UIControlContentVerticalAlignmentCenter | UIControlContentVerticalAlignmentFill;
    view.autoresizingMask = UIViewAutoresizingFlexibleWidth;
    return view;
}

- (void)onComplete:(BOOL)cancelled {
    if (completionCallback) {
        completionCallback(cancelled);
    }
}

@end
