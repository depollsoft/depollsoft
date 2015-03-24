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
#import <GoogleMobileAds/GoogleMobileAds.h>
#import "KJGridLayoutView.h"
#import "DPUtils+UIControl.h"
#import "DPPitchPipeModel.h"
#import "DPPitchPipeButton.h"
#import "LayoutManagers.h"
#import "DPSettingsViewController.h"
#import "DPAppDelegate.h"
#import "DPGridLayout.h"
#import "UIView+DPUtils.h"

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
    
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        self.view.frame = CGRectMake(0, 0, 320, 480);
    }
    
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimensionWithStars:1]
                                 ];
    
	// Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] initWithAdSize:kGADAdSizeSmartBannerPortrait];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    
    bannerView.rootViewController = self;
    
    NSMutableArray *keys = [NSMutableArray array];
    [keys addObjectsFromArray:[DPKey majorKeys]];
    [keys addObjectsFromArray:[DPKey minorKeys]];
    allKeys = [NSArray arrayWithArray:keys];
    
    UIView *background = [[UIView alloc] init];
    background.backgroundColor = [[UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]] colorWithAlphaComponent:0.5];
    //[self.view setBackgroundColor:[UIColor blackColor]];
    background.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:background];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(background)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[background]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(background)]];
    self.view.backgroundColor = [UIColor whiteColor];
    
    UIToolbar *toolbar = [[UIToolbar alloc] init];
    
    [toolbar sizeToFit];
    toolbar.frame = CGRectMake(0, 0, self.view.frame.size.width, toolbar.frame.size.height);
    
    nameField = [[UITextField alloc] init];
    UILabel *nameLabel = [[UILabel alloc] init];
    nameLabel.text = @" Name:";
    nameLabel.textColor = [UIColor lightGrayColor];
    [nameLabel sizeToFit];
    nameField.leftView = nameLabel;
    nameField.leftViewMode = UITextFieldViewModeAlways;
    nameField.text = song.name;
    nameField.contentMode = UIControlContentVerticalAlignmentCenter;
    nameField.borderStyle = UITextBorderStyleRoundedRect;
    nameField.returnKeyType = UIReturnKeyNext;
    nameField.autocapitalizationType = UITextAutocapitalizationTypeWords;
    nameField.delegate = self;
    [nameField sizeToFit];
    
    [rootLayout addSubview:toolbar row:0 column:0];
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
        [rootLayout addSubview:bannerView row:1 column:0];
        
        [bannerView loadRequest:DPAppDelegate.adRequest];
    }
    [rootLayout addSubview:[nameField pad:5] row:2 column:0];
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    keyPicker = [[UIPickerView alloc] init];
    keyPicker.dataSource = self;
    keyPicker.delegate = self;
    keyPicker.showsSelectionIndicator = YES;
    [keyPicker sizeToFit];
    [keyPicker selectRow:[allKeys indexOfObject:song.key] inComponent:0 animated:YES];
    
    [rootLayout addSubview:[keyPicker alignBottom] row:3 column:0];
    
    [self.view addSubview:rootLayout];
    
    UIBarButtonItem *doneItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemDone target:self action:@selector(complete)];
    
    UIBarButtonItem *cancelItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemCancel target:self action:@selector(cancel)];
    
    UIBarButtonItem *titleItem = [[UIBarButtonItem alloc] initWithTitle:@"Pitch Perfect" style:UIBarButtonItemStylePlain target:nil action:nil];
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    toolbar.items = [NSArray arrayWithObjects:cancelItem, flexibleSpace, titleItem, flexibleSpace, doneItem, nil];
    
    id topLayoutGuide = self.topLayoutGuide;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[rootLayout]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[topLayoutGuide][rootLayout]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout, topLayoutGuide)]];
    
}

- (void)resetBannerViewSize {
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        return;
    }
    switch ([UIApplication sharedApplication].statusBarOrientation) {
        case UIInterfaceOrientationLandscapeLeft:
        case UIInterfaceOrientationLandscapeRight:
            self.bannerView.adSize = kGADAdSizeSmartBannerLandscape;
            break;
        case UIInterfaceOrientationPortrait:
        case UIInterfaceOrientationPortraitUpsideDown:
            self.bannerView.adSize = kGADAdSizeSmartBannerPortrait;
            break;
        default:
            break;
    }
}

- (void)viewDidAppear:(BOOL)animated {
    [self resetBannerViewSize];
    [super viewDidAppear:animated];
}

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation {
    [self resetBannerViewSize];
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
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

- (void)viewDidUnload
{
    [super viewDidUnload];
    // Release any retained subviews of the main view.
    bannerView = nil;
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
