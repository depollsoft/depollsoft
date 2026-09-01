//
//  DPLoginViewController.m
//  pitchperfect
//
//  Created by David Poll on 3/23/15.
//  Copyright (c) 2015 DepollSoft. All rights reserved.
//

#import "DPLoginViewController.h"
// Ads removed during SDK migration
#import "DPAppDelegate.h"
#import "DPGridLayout.h"
#import "UIToolbar+DPUtils.h"
#import "UIView+DPUtils.h"
#import "DPUtils+UIControl.h"
#import "DPSettingsModel.h"
#import "DPSongsModel.h"
// Facebook Login removed during SDK migration
#import "pitchperfect-Swift.h"

@interface DPLoginViewController ()
@end

@implementation DPLoginViewController

- (instancetype)init {
    if (self = [super init]) {
        self.modalTransitionStyle = UIModalTransitionStyleCoverVertical;
        if ([UIDevice currentDevice].systemVersion.floatValue >= 8.0) {
            self.providesPresentationContextTransitionStyle = YES;
            self.definesPresentationContext = YES;
            self.modalPresentationStyle = UIModalPresentationOverCurrentContext;
            
        } else {
            self.modalPresentationStyle = UIModalPresentationCurrentContext;
        }
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    
    UINavigationItem *navigationItem = self.topNavigationItem;
    
    // Do any additional setup after loading the view, typically from a nib.
    self.view.backgroundColor = [UIColor systemBackgroundColor];
    UIView *background = [[UIView alloc] init];
    background.backgroundColor = [DPTheme staffBackgroundColor];
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
    
    navigationItem.title = @"Log In To Pitch Perfect";
    navigationItem.rightBarButtonItem =
        [[UIBarButtonItem alloc] initWithTitle:@"Skip"
                                        style:UIBarButtonItemStylePlain
                                       target:self
                                       action:@selector(skip)];
    
    DPGridLayout *rootLayout = [[DPGridLayout alloc] init];
    rootLayout.rowDimensions = @[
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimensionWithStars:1],
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimension],
                                 [DPGridDimension dimension]
                                 ];
    rootLayout.columnDimensions = @[
                                    [DPGridDimension dimensionWithStars:1]
                                    ];
    
    // Ads removed
    
    NSString *explanationHtml = @"<style>* {font-family: 'HelveticaNeue'; font-size: 18px;}</style>"
    "<p><b>Recommended:</b> Log in to Pitch Perfect and we\'ll save your settings and song list to the cloud.</p>"
    "<p>"
    "When you log in to Pitch Perfect, we\'ll automatically synchronize your settings and song list from device to device. "
    "Whether you just want to back up your songs or are working with multiple phones or tablets, logging in ensures that your "
    "data goes where you go."
    "</p>"
    "<p>"
    "Pitch Perfect does not collect any of your personal data for this free service."
    "</p>";
    NSAttributedString *explanationText = [[NSAttributedString alloc] initWithData:[explanationHtml dataUsingEncoding:NSUTF8StringEncoding]
                                                                           options:@{NSDocumentTypeDocumentAttribute: NSHTMLTextDocumentType}
                                                                documentAttributes:nil
                                                                             error:nil];
    
    UITextView *explanation = [[UITextView alloc] init];
    explanation.editable = NO;
    explanation.backgroundColor = [UIColor clearColor];
    explanation.attributedText = explanationText;
    explanation.textColor = [UIColor labelColor];
    
    [rootLayout addSubview:explanation row:2 column:0];
    
    UIButton *loginButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [loginButton setTitle:@"Sign up or log in" forState:UIControlStateNormal];
    [loginButton layoutSubviews];
    [loginButton addTarget:self action:@selector(logInClick) forControlEvents:UIControlEventTouchUpInside];
    [rootLayout addSubview:loginButton row:4 column:0];
    
    // Facebook login removed
    
    [self.view addSubview:rootLayout];
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
        
    self.edgesForExtendedLayout = UIRectEdgeNone;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[rootLayout]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
    [rootLayout.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor].active = YES;
    [rootLayout.leftAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.leftAnchor].active = YES;
    [rootLayout.rightAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.rightAnchor].active = YES;
    
}

- (void)completeLogIn:(BOOL)isNew {
    if (!isNew) {
        [[DPSettingsModel sharedInstance] attachToFirestore];
        [[DPSongsModel sharedInstance] attachToFirestoreWithStore:NO];
    } else {
        [[DPSettingsModel sharedInstance] attachToFirestore];
        [[DPSongsModel sharedInstance] attachToFirestoreWithStore:YES];
    }
    if (self.loginCompletion) {
        self.loginCompletion();
    }
}

- (void)skip {
    [self dismissViewControllerAnimated:YES completion:^{
    }];
    if (self.loginCompletion) {
        self.loginCompletion();
    }
}

// Ads removed

// Ads removed

@end
