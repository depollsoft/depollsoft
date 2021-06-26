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

@property (nonatomic, strong) DPBusyIndicator *busyIndicator;

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
    self.busyIndicator = [[DPBusyIndicator alloc] init];
    self.busyIndicator.translatesAutoresizingMaskIntoConstraints = NO;
}

- (void)setTagId:(int)tId {
    tagId = tId;
    [self loadTag:NO];
}

- (void)loadTag:(BOOL)refresh {
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            DPTag *t = [DPTag loadTagById:self->tagId refresh:refresh];
            dispatch_async(dispatch_get_main_queue(), ^{
                self.tag = t;
                [self.busyIndicator decrementBusyCount];
            });
        }
        @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
            });
        }
    });
}

- (void)setTag:(DPTag *)t {
    tag = t;
    
    self.title = t.title;
    for (DPTagPageControllerBase *page in self.viewControllers) {
        page.tag = t;
    }
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.view];

    NSMutableArray *controllers = [NSMutableArray array];
    
    self.summaryController = [[DPTagSummaryController alloc] init];
    self.summaryController.tabBarItem = [[UITabBarItem alloc] init];
    self.summaryController.tabBarItem.title = @"Summary";
    self.summaryController.tabBarItem.image = [UIImage imageNamed:@"TagSummary"];
    self.summaryController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.summaryController];
    
    self.detailController = [[DPTagDetailController alloc] init];
    self.detailController.tabBarItem = [[UITabBarItem alloc] init];
    self.detailController.tabBarItem.title = @"Details";
    self.detailController.tabBarItem.image = [UIImage imageNamed:@"MostViewed"];
    self.detailController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.detailController];
    
    self.tagTracksController = [[DPTagTracksController alloc] init];
    self.tagTracksController.tabBarItem = [[UITabBarItem alloc] init];
    self.tagTracksController.tabBarItem.title = @"Tracks";
    self.tagTracksController.tabBarItem.image = [UIImage imageNamed:@"Tracks"];
    self.tagTracksController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.tagTracksController];
    
    self.tagVideoController = [[DPTagVideoController alloc] init];
    self.tagVideoController.tabBarItem = [[UITabBarItem alloc] init];
    self.tagVideoController.tabBarItem.title = @"Videos";
    self.tagVideoController.tabBarItem.image = [UIImage imageNamed:@"Videos"];
    self.tagVideoController.busyIndicator = self.busyIndicator;
    [controllers addObject:self.tagVideoController];
    
    self.viewControllers = controllers;
    
    self.navigationItem.rightBarButtonItems = @[
        [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAction
                                                      target:self
                                                      action:@selector(sendTag)],
        self.actionBarButton = [[UIBarButtonItem alloc] initWithImage:[UIImage systemImageNamed:@"tag"] style:UIBarButtonItemStylePlain target:self action:@selector(showActions)],
        [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemRefresh target:self action:@selector(refreshTag)]
    ];
    
    [self setTag:self.tag];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    
    UIView *navView = self.navigationController.view;
    [navView addSubview:self.busyIndicator];
    [navView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"H:|[_busyIndicator]|"
                                                                    options:0
                                                                    metrics:nil
                                                                      views:NSDictionaryOfVariableBindings(_busyIndicator)]];
    [navView addConstraints:[NSLayoutConstraint constraintsWithVisualFormat:@"V:|[_busyIndicator]|"
                                                                    options:0
                                                                    metrics:nil
                                                                      views:NSDictionaryOfVariableBindings(_busyIndicator)]];
}

- (void)viewDidDisappear:(BOOL)animated {
    [self.busyIndicator removeFromSuperview];
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
    NSString *string = [NSString stringWithFormat:@"%@ - Tag Master for iOS", self.tag.title];
    NSURL *url = self.tag.tagUri;
    UIActivityViewController *activityController = [[UIActivityViewController alloc] initWithActivityItems:@[string, url] applicationActivities:nil];
    [self presentViewController:activityController animated:YES completion:nil];
}

- (void)refreshTag {
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
