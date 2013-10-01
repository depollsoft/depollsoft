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
#import "DPAppDelegate.h"
#import <MessageUI/MessageUI.h>

@interface DPTagViewController () <UIActionSheetDelegate, MFMessageComposeViewControllerDelegate, MFMailComposeViewControllerDelegate>

@property (nonatomic, strong) DPTag *tag;

@property (nonatomic, strong) DPTagSummaryController *summaryController;
@property (nonatomic, strong) DPTagDetailController *detailController;
@property (nonatomic, strong) DPTagTracksController *tagTracksController;

@property (nonatomic, strong) UIActionSheet *actions;
@property (nonatomic) NSInteger favoriteButtonIndex;
@property (nonatomic) NSInteger teachableButtonIndex;
@property (nonatomic) NSInteger emailButtonIndex;
@property (nonatomic) NSInteger smsButtonIndex;

@property (nonatomic, strong) UIBarButtonItem *actionBarButton;

@end

@implementation DPTagViewController

@synthesize tagId, tag;

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)setTagId:(int)tId {
    tagId = tId;
    [self loadTag:NO];
}

- (void)loadTag:(BOOL)refresh {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        DPTag *t = [DPTag loadTagById:tagId refresh:refresh];
        dispatch_async(dispatch_get_main_queue(), ^{
            self.tag = t;
        });
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
    [controllers addObject:self.summaryController];
    
    self.detailController = [[DPTagDetailController alloc] init];
    self.detailController.tabBarItem = [[UITabBarItem alloc] init];
    self.detailController.tabBarItem.title = @"Details";
    [controllers addObject:self.detailController];
    
    self.tagTracksController = [[DPTagTracksController alloc] init];
    self.tagTracksController.tabBarItem = [[UITabBarItem alloc] init];
    self.tagTracksController.tabBarItem.title = @"Tracks";
    [controllers addObject:self.tagTracksController];
    
    self.viewControllers = controllers;
    
    self.navigationItem.rightBarButtonItems = @[
                          self.actionBarButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAction target:self action:@selector(showActions)],
                          [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemRefresh target:self action:@selector(refreshTag)]
                          ];
    
    [self setTag:self.tag];
}

- (void)showActions {
    UIActionSheet *actions = [[UIActionSheet alloc] initWithTitle:nil
                                                         delegate:self
                                                cancelButtonTitle:nil
                                           destructiveButtonTitle:nil
                                                otherButtonTitles:nil];
    
    if (![DPAppDelegate containsFavorite:self.tagId]) {
        self.favoriteButtonIndex = [actions addButtonWithTitle:@"Add Favorite"];
    } else {
        self.favoriteButtonIndex = [actions addButtonWithTitle:@"Remove Favorite"];
    }
    if (![DPAppDelegate containsTeachable:self.tagId]) {
        self.teachableButtonIndex = [actions addButtonWithTitle:@"Mark as Teachable"];
    } else {
        self.teachableButtonIndex = [actions addButtonWithTitle:@"Unmark as Teachable"];
    }
    
    if ([MFMessageComposeViewController canSendText]) {
        self.smsButtonIndex = [actions addButtonWithTitle:@"Send as SMS"];
    } else {
        self.smsButtonIndex = -1;
    }
    if ([MFMailComposeViewController canSendMail]) {
        self.emailButtonIndex = [actions addButtonWithTitle:@"Send as Email"];
    } else {
        self.emailButtonIndex = -1;
    }
    
    actions.cancelButtonIndex = [actions addButtonWithTitle:@"Cancel"];
    [actions showFromBarButtonItem:self.actionBarButton animated:YES];
}

- (void)refreshTag {
    [self loadTag:YES];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)actionSheet:(UIActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex {
    if (buttonIndex == self.smsButtonIndex) {
        MFMessageComposeViewController *smsController = [[MFMessageComposeViewController alloc] init];
        smsController.messageComposeDelegate = self;
        smsController.body = [NSString stringWithFormat:@"%@ %@ - Sent from Tag Master for iOS", self.tag.title, self.tag.tagUri];
        [self presentViewController:smsController animated:YES completion:nil];
    } else if (buttonIndex == self.emailButtonIndex) {
        MFMailComposeViewController *mailController = [[MFMailComposeViewController alloc] init];
        [mailController setSubject:[NSString stringWithFormat:@"Tag: %@", self.tag.title]];
        [mailController setMessageBody:[NSString stringWithFormat:@"%@\n%@\n\nSent from Tag Master for iOS\nhttp://apps.depoll.com/barbershop/tag-master", self.tag.title, self.tag.tagUri] isHTML:NO];
        mailController.mailComposeDelegate = self;
        [self presentViewController:mailController animated:YES completion:nil];
    } else if (buttonIndex == self.favoriteButtonIndex) {
        if ([DPAppDelegate containsFavorite:self.tagId]) {
            [DPAppDelegate removeFavorite:self.tagId];
        } else {
            [DPAppDelegate addFavorite:self.tagId];
        }
    } else if (buttonIndex == self.teachableButtonIndex) {
        if ([DPAppDelegate containsTeachable:self.tagId]) {
            [DPAppDelegate removeTeachable:self.tagId];
        } else {
            [DPAppDelegate addTeachable:self.tagId];
        }
    }
}

- (void)messageComposeViewController:(MFMessageComposeViewController *)controller didFinishWithResult:(MessageComposeResult)result {
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)mailComposeController:(MFMailComposeViewController *)controller didFinishWithResult:(MFMailComposeResult)result error:(NSError *)error {
    [self dismissViewControllerAnimated:YES completion:nil];
}

@end
