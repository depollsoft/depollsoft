//
//  DPSongEditorViewController.m
//  pitchperfect
//
//  Created by David Poll on 6/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "DPSongEditorViewController.h"
#import "DPKey.h"
#import "GoogleMobileAdsStub.h"
#import "DPAppDelegate.h"
#import "DPAppDelegate+Ads.h"
#import "pitchperfect-Swift.h"

@interface DPSongEditorViewController ()

@property (nonatomic, strong) GADBannerView *bannerView;
@property (nonatomic, strong) NSLayoutConstraint *bannerWidth;
@property (nonatomic, strong) NSLayoutConstraint *bannerHeight;
@property (nonatomic, strong) DPSongEditor *editor;

@end

@implementation DPSongEditorViewController

@synthesize bannerView, bannerWidth, bannerHeight, song, editor, completionCallback;

- (void)viewDidLoad
{
    [super viewDidLoad];

    UINavigationItem *navigationItem = self.topNavigationItem;

    if ([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        self.view.frame = CGRectMake(0, 0, 320, 480);
    }

    bannerView = [[GADBannerView alloc] init];
    bannerView.adUnitID = [DPAppDelegate bannerAdUnitID];
    bannerView.adSize = GADPortraitAnchoredAdaptiveBannerAdSizeWithWidth(self.view.frame.size.width);
    [self resetBannerViewSize];
    bannerView.rootViewController = self;
    bannerView.delegate = (id<GADBannerViewDelegate>)UIApplication.sharedApplication.delegate;
    bannerView.translatesAutoresizingMaskIntoConstraints = NO;

    UIScrollView *background = [[UIScrollView alloc] init];
    background.scrollEnabled = NO;
    background.backgroundColor = [DPTheme staffBackgroundColor];
    self.view.backgroundColor = [UIColor systemBackgroundColor];
    background.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:background];
    // The glass bars sample this full-bleed scroll surface; without it iOS 26
    // paints an opaque hard edge over non-scrolling content.
    [self setContentScrollView:background forEdge:NSDirectionalRectEdgeAll];
    self.edgesForExtendedLayout = UIRectEdgeAll;
    [NSLayoutConstraint activateConstraints:@[
        [background.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [background.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [background.topAnchor constraintEqualToAnchor:self.view.topAnchor],
        [background.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor],
    ]];

    editor = [[DPSongEditor alloc] initWithTitle:song.name ?: @"" key:song.key];
    UIViewController *hosted = [editor makeViewController];
    [self addChildViewController:hosted];
    hosted.view.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:hosted.view];
    [hosted didMoveToParentViewController:self];

    UILayoutGuide *safe = self.view.safeAreaLayoutGuide;
    NSLayoutYAxisAnchor *bottomAnchor = safe.bottomAnchor;
    if ([UIDevice currentDevice].userInterfaceIdiom == UIUserInterfaceIdiomPhone) {
        [self.view addSubview:bannerView];
        // Auto Layout owns the banner's frame, so the ad size becomes constraints.
        CGSize adSize = CGSizeFromGADAdSize(bannerView.adSize);
        bannerWidth = [bannerView.widthAnchor constraintEqualToConstant:adSize.width];
        bannerHeight = [bannerView.heightAnchor constraintEqualToConstant:adSize.height];
        [NSLayoutConstraint activateConstraints:@[
            [bannerView.centerXAnchor constraintEqualToAnchor:self.view.centerXAnchor],
            [bannerView.bottomAnchor constraintEqualToAnchor:safe.bottomAnchor],
            bannerWidth,
            bannerHeight,
        ]];
        bottomAnchor = bannerView.topAnchor;
    }
    [NSLayoutConstraint activateConstraints:@[
        [hosted.view.topAnchor constraintEqualToAnchor:safe.topAnchor],
        [hosted.view.leadingAnchor constraintEqualToAnchor:safe.leadingAnchor],
        [hosted.view.trailingAnchor constraintEqualToAnchor:safe.trailingAnchor],
        [hosted.view.bottomAnchor constraintEqualToAnchor:bottomAnchor constant:-8],
    ]];

    UIBarButtonItem *doneItem = [DPCommon barButtonWithSystemName:@"checkmark" target:self selector:@selector(complete)];
    UIBarButtonItem *cancelItem = [DPCommon barButtonWithSystemName:@"xmark" target:self selector:@selector(cancel)];
    navigationItem.title = song.name.length > 0 ? @"Edit Song" : @"Add Song";
    navigationItem.leftBarButtonItem = cancelItem;
    navigationItem.rightBarButtonItem = doneItem;
}

- (void)resetBannerViewSize {
    [DPAppDelegate resizeAndReloadBannerView:self.bannerView forViewController:self];
    CGSize adSize = CGSizeFromGADAdSize(self.bannerView.adSize);
    bannerWidth.constant = adSize.width;
    bannerHeight.constant = adSize.height;
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

#pragma mark - Editing state

- (void)setTitleText:(NSString *)titleText {
    editor.title = titleText;
}

- (void)selectKey:(DPKey *)key {
    [editor select:key];
}

- (DPKey *)selectedKey {
    return editor.selectedKey;
}

- (BOOL)isMinorKeySelected {
    return editor.isMinor;
}

- (BOOL)isTitleErrorVisible {
    return editor.titleErrorVisible;
}

- (void)complete {
    if (![editor requireTitle]) {
        UINotificationFeedbackGenerator *feedback = [[UINotificationFeedbackGenerator alloc] init];
        [feedback notificationOccurred:UINotificationFeedbackTypeError];
        UIAccessibilityPostNotification(UIAccessibilityAnnouncementNotification, @"Song title is required");
        return;
    }
    song.name = editor.title;
    song.key = editor.selectedKey;
    UINotificationFeedbackGenerator *feedback = [[UINotificationFeedbackGenerator alloc] init];
    [feedback notificationOccurred:UINotificationFeedbackTypeSuccess];
    [self onComplete:NO];
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)cancel {
    [self onComplete:YES];
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)onComplete:(BOOL)cancelled {
    if (completionCallback) {
        completionCallback(cancelled);
    }
}

@end
