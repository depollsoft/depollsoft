//
//  DPTagViewController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPTagViewController.h"
#import "DPBarbershop.h"
#import "DPTagSummaryController.h"
#import "DPTagDetailController.h"
#import "DPTagTracksController.h"
#import "DPTagVideoController.h"
#import "DPAppDelegate.h"
#import <MessageUI/MessageUI.h>

@interface DPTagViewController () <UIActionSheetDelegate, MFMessageComposeViewControllerDelegate, MFMailComposeViewControllerDelegate>

@property (nonatomic, strong) DPTag *tag;

@property (nonatomic, strong) DPTagSummaryController *summaryController;
@property (nonatomic, strong) DPTagDetailController *detailController;
@property (nonatomic, strong) DPTagTracksController *tagTracksController;
@property (nonatomic, strong) DPTagVideoController *tagVideoController;

@property (nonatomic, strong) UIBarButtonItem *actionBarButton;
@property (nonatomic, strong) UIBarButtonItem *shareBarButton;
@property (nonatomic, strong) UIBarButtonItem *refreshBarButton;
@property (nonatomic, strong) UIBarButtonItem *loadingBarButton;

@property (nonatomic, strong) TMBusyIndicator *busyIndicator;

@end

@implementation DPTagViewController

@synthesize tagId, tag;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        [self commonInit];
    }
    return self;
}

- (id)init {
    if (self = [super init]) {
        [self commonInit];
    }
    return self;
}

- (void)commonInit {
    // Counts in-flight work; progress is shown inline so navigation stays usable.
    self.busyIndicator = [[TMBusyIndicator alloc] init];
    __weak DPTagViewController *weakSelf = self;
    self.busyIndicator.onBusyCountChanged = ^(NSUInteger busyCount) {
        [weakSelf updateLoadingState];
    };
}

- (void)setTagId:(int)tId {
    tagId = tId;
    [self loadTag:NO];
}

- (void)loadTag:(BOOL)refresh {
    if (!self.tag && self.isViewLoaded) {
        UIContentUnavailableConfiguration *loading = [UIContentUnavailableConfiguration loadingConfiguration];
        loading.text = @"Loading tag…";
        self.contentUnavailableConfiguration = loading;
    }
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            DPTag *t = [DPTag loadTagById:self->tagId refresh:refresh];
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                if (!t) {
                    [self showLoadError:refresh];
                    return;
                }
                self.contentUnavailableConfiguration = nil;
                self.tag = t;
            });
        }
        @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self showLoadError:refresh];
            });
        }
    });
}

- (void)showLoadError:(BOOL)refresh {
    if (!self.tag) {
        UIContentUnavailableConfiguration *state = [UIContentUnavailableConfiguration emptyConfiguration];
        state.image = [UIImage systemImageNamed:@"wifi.exclamationmark"];
        state.text = @"Tag unavailable";
        state.secondaryText = @"Check your connection and tag ID, then tap Refresh to try again.";
        self.contentUnavailableConfiguration = state;
    }
    [self tm_showError:@"The tag couldn't be loaded. Check your connection and tag ID, then try again. Your saved tags are unchanged." retry:^{ [self loadTag:refresh]; }];
}

- (void)updateLoadingState {
    if (!self.isViewLoaded) return;
    BOOL busy = self.busyIndicator.busyCount > 0;
    UIActivityIndicatorView *spinner = (UIActivityIndicatorView *)self.loadingBarButton.customView;
    if (busy) [spinner startAnimating]; else [spinner stopAnimating];
    self.navigationItem.rightBarButtonItems = @[self.shareBarButton,
                                                self.actionBarButton,
                                                busy ? self.loadingBarButton : self.refreshBarButton];
}

- (void)setTag:(DPTag *)t {
    tag = t;
    
    self.title = t.title;
    self.shareBarButton.enabled = t != nil;
    self.actionBarButton.enabled = t != nil;
    for (DPTagPageControllerBase *page in self.viewControllers) {
        page.tag = t;
    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
    [DPAppDelegate setUpBackground:self.view];
    // Keep native page tabs usable when UIKit reports only the safe-area height.
    [self.tabBar.heightAnchor constraintGreaterThanOrEqualToConstant:83].active = YES;
    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        // The detail column extends under the floating sidebar; page content already follows
        // the safe area, so the page tabs must too or the first tab hides under the sidebar.
        NSMutableArray<NSLayoutConstraint *> *edges = [NSMutableArray array];
        for (NSLayoutConstraint *constraint in self.view.constraints) {
            BOOL aboutTabBar = constraint.firstItem == self.tabBar || constraint.secondItem == self.tabBar;
            BOOL horizontal = constraint.firstAttribute == NSLayoutAttributeLeading || constraint.firstAttribute == NSLayoutAttributeTrailing
                || constraint.firstAttribute == NSLayoutAttributeLeft || constraint.firstAttribute == NSLayoutAttributeRight;
            if (aboutTabBar && horizontal) [edges addObject:constraint];
        }
        [NSLayoutConstraint deactivateConstraints:edges];
        [NSLayoutConstraint activateConstraints:@[
            [self.tabBar.leadingAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.leadingAnchor],
            [self.tabBar.trailingAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.trailingAnchor]
        ]];
    }

    NSMutableArray *controllers = [NSMutableArray array];
    
    self.summaryController = [[DPTagSummaryController alloc] init];
    self.summaryController.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Summary" image:[UIImage systemImageNamed:@"doc.text"] tag:0];
    self.summaryController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.summaryController];
    
    self.detailController = [[DPTagDetailController alloc] init];
    self.detailController.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Details" image:[UIImage systemImageNamed:@"info.circle"] tag:1];
    self.detailController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.detailController];
    
    self.tagTracksController = [[DPTagTracksController alloc] init];
    self.tagTracksController.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Tracks" image:[UIImage systemImageNamed:@"waveform"] tag:2];
    self.tagTracksController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.tagTracksController];
    
    self.tagVideoController = [[DPTagVideoController alloc] init];
    self.tagVideoController.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Videos" image:[UIImage systemImageNamed:@"play.rectangle"] tag:3];
    self.tagVideoController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.tagVideoController];
    
    self.viewControllers = controllers;
    
    self.shareBarButton = [DPAppDelegate barButtonItemWithSystemName:@"square.and.arrow.up"
                                                              target:self
                                                              action:@selector(sendTag)];
    self.actionBarButton = [DPAppDelegate barButtonItemWithSystemName:@"tag"
                                                               target:self
                                                               action:@selector(showActions)];
    self.refreshBarButton = [DPAppDelegate barButtonItemWithSystemName:@"arrow.clockwise"
                                                                target:self
                                                                action:@selector(refreshTag)];
    UIActivityIndicatorView *spinner = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleMedium];
    spinner.hidesWhenStopped = NO;
    self.loadingBarButton = [[UIBarButtonItem alloc] initWithCustomView:spinner];
    self.loadingBarButton.accessibilityLabel = @"Loading";
    self.loadingBarButton.accessibilityTraits = UIAccessibilityTraitStaticText;
    [self updateLoadingState];
    
    if (!self.tag && self.busyIndicator.busyCount > 0) {
        UIContentUnavailableConfiguration *loading = [UIContentUnavailableConfiguration loadingConfiguration];
        loading.text = @"Loading tag…";
        self.contentUnavailableConfiguration = loading;
    }
    [self setTag:self.tag];
}

- (void)showActions {
    UIAlertController *actions = [UIAlertController alertControllerWithTitle:nil
                                                                     message:nil
                                                              preferredStyle:UIAlertControllerStyleActionSheet];
    actions.popoverPresentationController.barButtonItem = self.actionBarButton;
    
    if (![DPAppDelegate containsFavorite:self.tagId]) {
        [actions addAction:[UIAlertAction actionWithTitle:@"Add Favorite"
                                                    style:UIAlertActionStyleDefault
                                                  handler:^(UIAlertAction * _Nonnull action) {
            [DPAppDelegate addFavorite:self.tagId];
        }]];
    } else {
        [actions addAction:[UIAlertAction actionWithTitle:@"Remove Favorite"
                                                    style:UIAlertActionStyleDefault
                                                  handler:^(UIAlertAction * _Nonnull action) {
            [DPAppDelegate removeFavorite:self.tagId];
        }]];
    }
    if (![DPAppDelegate containsTeachable:self.tagId]) {
        [actions addAction:[UIAlertAction actionWithTitle:@"Mark as Teachable"
                                                    style:UIAlertActionStyleDefault
                                                  handler:^(UIAlertAction * _Nonnull action) {
            [DPAppDelegate addTeachable:self.tagId];
        }]];
    } else {
        [actions addAction:[UIAlertAction actionWithTitle:@"Unmark as Teachable"
                                                    style:UIAlertActionStyleDefault
                                                  handler:^(UIAlertAction * _Nonnull action) {
            [DPAppDelegate removeTeachable:self.tagId];
        }]];
    }
    
    [actions addAction:[UIAlertAction actionWithTitle:@"Cancel"
                                                style:UIAlertActionStyleCancel
                                              handler:nil]];
    
    [self presentViewController:actions animated:YES completion:nil];
}

- (void)sendTag {
    if (!self.tag) return;
    NSString *string = [NSString stringWithFormat:@"%@ - Tag Master for iOS", self.tag.title];
    NSURL *url = self.tag.tagUri;
    UIActivityViewController *activityController = [[UIActivityViewController alloc] initWithActivityItems:@[string, url] applicationActivities:nil];
    activityController.popoverPresentationController.barButtonItem = self.shareBarButton;
    [self presentViewController:activityController animated:YES completion:nil];
}

- (void)refreshTag {
    if (self.busyIndicator.busyCount > 0) return;
    [self loadTag:YES];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)messageComposeViewController:(MFMessageComposeViewController *)controller didFinishWithResult:(MessageComposeResult)result {
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)mailComposeController:(MFMailComposeViewController *)controller didFinishWithResult:(MFMailComposeResult)result error:(NSError *)error {
    [self dismissViewControllerAnimated:YES completion:nil];
}

@end
