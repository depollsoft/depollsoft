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

@interface DPTagViewController ()

@property (nonatomic, strong) DPTag *tag;

@property (nonatomic, strong) DPTagSummaryController *summaryController;

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
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        DPTag *t = [DPTag loadTagById:tagId];
        dispatch_async(dispatch_get_main_queue(), ^{
            self.tag = t;
        });
    });
}

- (void)setTag:(DPTag *)t {
    tag = t;
    
    self.title = t.title;
    self.summaryController.tag = t;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	
    NSMutableArray *controllers = [NSMutableArray array];
    
    self.summaryController = [[DPTagSummaryController alloc] init];
    [controllers addObject:self.summaryController];
    
    self.viewControllers = controllers;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
