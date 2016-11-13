//
//  DPLoginViewController.m
//  pitchperfect
//
//  Created by David Poll on 3/23/15.
//  Copyright (c) 2015 DepollSoft. All rights reserved.
//

#import "DPLoginViewController.h"
#import <GoogleMobileAds/GoogleMobileAds.h>
#import "DPAppDelegate.h"
#import "DPGridLayout.h"
#import "UIToolbar+DPUtils.h"
#import "UIView+DPUtils.h"
#import "DPUtils+UIControl.h"
#import <Parse/Parse.h>
#import <ParseFacebookUtilsV4/PFFacebookUtils.h>
#import <Bolts/Bolts.h>
#import "DPSettingsModel.h"
#import "DPSongsModel.h"
#import <FBSDKLoginKit/FBSDKLoginKit.h>

@interface DPLoginViewController () <FBSDKLoginButtonDelegate>

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, readonly) BFTaskCompletionSource *loginTaskCompletionSource;

@end

@implementation DPLoginViewController

@synthesize bannerView, loginTaskCompletionSource;

- (instancetype)init {
    if (self = [super init]) {
        loginTaskCompletionSource = [BFTaskCompletionSource taskCompletionSource];
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

- (BFTask *)loginTask {
    return self.loginTaskCompletionSource.task;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.
    bannerView = [[GADBannerView alloc] initWithAdSize:kGADAdSizeSmartBannerPortrait];
    bannerView.adUnitID = @"a14fd7eba4542f0";
    
    bannerView.rootViewController = self;
    self.view.backgroundColor = [UIColor whiteColor];
    UIView *background = [[UIView alloc] init];
    background.backgroundColor = [[UIColor colorWithPatternImage:[UIImage imageNamed:@"panobackground.png"]] colorWithAlphaComponent:0.5];
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
    
    UIToolbar *toolbar = [[UIToolbar alloc] init];
    toolbar.barStyle = UIBarStyleDefault;
    
    [toolbar sizeToFit];
    
    [toolbar addTitle:@"Log In To Pitch Perfect"];
    
    UIBarButtonItem *skipItem = [[UIBarButtonItem alloc] initWithTitle:@"Skip" style:UIBarButtonItemStylePlain target:nil action:@selector(skip)];
    
    UIBarButtonItem *flexibleSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
    
    
    toolbar.items = [NSArray arrayWithObjects:flexibleSpace, skipItem, nil];
    
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
    
    [rootLayout addSubview:toolbar row:0 column:0];
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
        [rootLayout addSubview:bannerView row:1 column:0];
        
        [bannerView loadRequest:DPAppDelegate.adRequest];
    }
    
    NSString *explanationHtml;
    if (!self.isHoomiLogout) {
        explanationHtml = @"<style>* {font-family: 'HelveticaNeue'; font-size: 18px;}</style>"
        "<p><b>Recommended:</b> Log in to Pitch Perfect and we\'ll save your settings and song list to the cloud.</p>"
        "<p>"
        "When you log in to Pitch Perfect, we\'ll automatically synchronize your settings and song list from device to device. "
        "Whether you just want to back up your songs or are working with multiple phones or tablets, logging in ensures that your "
        "data goes where you go."
        "</p>"
        "<p>"
        "Pitch Perfect does not collect any of your personal data for this free service."
        "</p>";
    } else {
        explanationHtml = @"<style>* {font-family: 'HelveticaNeue'; font-size: 18px;}</style>"
        "<p><b>Notice:</b> You have previously logged into Pitch Perfect with Hoomi, which is being discontinued. "
        "You have been logged out of Pitch Perfect. "
        "You may choose to log in with Facebook, or continue without logging in.</p>"
        "<p>"
        "When you log in to Pitch Perfect, we\'ll automatically synchronize your settings and song list from device to device. "
        "Whether you just want to back up your songs or are working with multiple phones or tablets, logging in ensures that your "
        "data goes where you go."
        "</p>"
        "<p>"
        "Pitch Perfect does not collect any of your personal data for this free service."
        "</p>";
    }
    NSAttributedString *explanationText = [[NSAttributedString alloc] initWithData:[explanationHtml dataUsingEncoding:NSUTF8StringEncoding]
                                                                           options:@{NSDocumentTypeDocumentAttribute: NSHTMLTextDocumentType}
                                                                documentAttributes:nil
                                                                             error:nil];
    
    UITextView *explanation = [[UITextView alloc] init];
    explanation.editable = NO;
    explanation.backgroundColor = [UIColor clearColor];
    explanation.attributedText = explanationText;
    
    [rootLayout addSubview:explanation row:2 column:0];
    
    FBSDKLoginButton *fbLoginButton = [[FBSDKLoginButton alloc] init];
    fbLoginButton.readPermissions = @[@"public_profile"];
    UIView *fbLoginContainer = [[fbLoginButton fixHeight:50] pad:4];
    [rootLayout addSubview:fbLoginContainer row:4 column:0];
    fbLoginButton.delegate = self;
    
    [self.view addSubview:rootLayout];
    rootLayout.translatesAutoresizingMaskIntoConstraints = NO;
    
    id topLayoutGuide = self.topLayoutGuide;
    id bottomLayoutGuide = self.bottomLayoutGuide;
    
    self.edgesForExtendedLayout = UIRectEdgeNone;
    
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:[topLayoutGuide][rootLayout][bottomLayoutGuide]"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(topLayoutGuide, rootLayout, bottomLayoutGuide)]];
    [self.view addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[rootLayout]|"
                                                                      options:0
                                                                      metrics:nil
                                                                        views:NSDictionaryOfVariableBindings(rootLayout)]];
    
}

- (void)loginButton:(FBSDKLoginButton *)loginButton
didCompleteWithResult:(FBSDKLoginManagerLoginResult *)result
              error:(NSError *)error {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (error || result.isCancelled) {
            return;
        }
        [self dismissViewControllerAnimated:YES completion:^{
        }];
        [[PFFacebookUtils logInInBackgroundWithAccessToken:result.token]
         continueWithExecutor:[BFExecutor mainThreadExecutor]
         withBlock:^id(BFTask *task) {
             PFUser *user = task.result;
             [self completeLogIn:user.isNew];
             return nil;
         }];
    });
}

- (void)loginButtonDidLogOut:(FBSDKLoginButton *)loginButton {
    
}

- (void)completeLogIn:(BOOL)isNew {
    if (!isNew) {
        [[DPSettingsModel sharedInstance] restoreUser];
        [[DPSongsModel sharedInstance] refreshFromParse];
    } else {
        [[DPSettingsModel sharedInstance] refreshUser];
        [[DPSongsModel sharedInstance] saveAllToParse:YES];
    }
    [loginTaskCompletionSource trySetResult:nil];
}

- (void)skip {
    [self dismissViewControllerAnimated:YES completion:^{
    }];
    [loginTaskCompletionSource trySetResult:nil];
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

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation {
    [self resetBannerViewSize];
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

- (void)viewDidAppear:(BOOL)animated {
    [self resetBannerViewSize];
    [super viewDidAppear:animated];
}

@end
