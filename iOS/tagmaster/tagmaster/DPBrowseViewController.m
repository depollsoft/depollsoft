//
//  DPBrowseViewController.m
//  tagmaster
//
//  Created by David Poll on 9/27/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPBrowseViewController.h"
#import "DPTagQueryViewController.h"
#import "DPAppDelegate.h"
#import "tagmaster-Swift.h"

static NSString *const DPBrowseSelectionKey = @"browse.collection";

@interface DPBrowseViewController ()

@property (nonatomic, strong) UISegmentedControl *collectionPicker;
@property (nonatomic, strong) TMAdaptiveChoiceView *collectionChoice;
@property (nonatomic, strong) UIView *container;
@property (nonatomic, strong) NSMutableDictionary<NSNumber *, DPTagQueryViewController *> *controllers;
@property (nonatomic, strong) DPTagQueryViewController *visibleController;
@property (nonatomic, readwrite) NSInteger selectedCollectionIndex;

@end

@implementation DPBrowseViewController

+ (NSArray<NSString *> *)collectionTitles {
    return @[@"Latest", @"Top Rated", @"Downloads", @"Classic"];
}

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        self.title = @"Browse";
        self.controllers = [NSMutableDictionary dictionary];
        _selectedCollectionIndex = -1;
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.view];
    self.navigationItem.title = @"Browse";
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] init];
    self.navigationItem.backBarButtonItem.title = @"Browse";

    self.collectionPicker =
        [[UISegmentedControl alloc] initWithItems:[DPBrowseViewController collectionTitles]];
    self.collectionPicker.translatesAutoresizingMaskIntoConstraints = NO;
    self.collectionPicker.accessibilityLabel = @"Collection";
    [self.collectionPicker addTarget:self
                              action:@selector(collectionPickerChanged)
                    forControlEvents:UIControlEventValueChanged];

    self.container = [[UIView alloc] init];
    self.container.translatesAutoresizingMaskIntoConstraints = NO;

    self.collectionChoice = [[TMAdaptiveChoiceView alloc] initWithSegments:self.collectionPicker];
    self.collectionChoice.translatesAutoresizingMaskIntoConstraints = NO;
    [self.view addSubview:self.collectionChoice];
    [self.view addSubview:self.container];

    UILayoutGuide *safe = self.view.safeAreaLayoutGuide;
    UILayoutGuide *readable = self.view.readableContentGuide;
    [NSLayoutConstraint activateConstraints:@[
        [self.collectionChoice.topAnchor constraintEqualToAnchor:safe.topAnchor
                                                        constant:TMTheme.spaceS],
        [self.collectionChoice.leadingAnchor constraintEqualToAnchor:readable.leadingAnchor],
        [self.collectionChoice.trailingAnchor constraintEqualToAnchor:readable.trailingAnchor],
        [self.collectionChoice.heightAnchor
            constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget],

        [self.container.topAnchor constraintEqualToAnchor:self.collectionChoice.bottomAnchor
                                                 constant:TMTheme.spaceS],
        [self.container.leadingAnchor constraintEqualToAnchor:self.view.leadingAnchor],
        [self.container.trailingAnchor constraintEqualToAnchor:self.view.trailingAnchor],
        [self.container.bottomAnchor constraintEqualToAnchor:self.view.bottomAnchor]
    ]];

    NSInteger remembered = [[NSUserDefaults standardUserDefaults] integerForKey:DPBrowseSelectionKey];
    if (remembered < 0 || remembered >= (NSInteger)[DPBrowseViewController collectionTitles].count) {
        remembered = 0;
    }
    [self selectCollectionAtIndex:remembered];
}

- (void)collectionPickerChanged {
    [TMTheme selected];
    [self selectCollectionAtIndex:self.collectionPicker.selectedSegmentIndex];
}

- (void)selectCollectionAtIndex:(NSInteger)index {
    if (index < 0 || index >= (NSInteger)[DPBrowseViewController collectionTitles].count) {
        return;
    }
    if (self.selectedCollectionIndex == index && self.visibleController) {
        return;
    }
    _selectedCollectionIndex = index;
    self.collectionPicker.selectedSegmentIndex = index;
    [[NSUserDefaults standardUserDefaults] setInteger:index forKey:DPBrowseSelectionKey];

    DPTagQueryViewController *next = [self controllerForIndex:index];
    if (next == self.visibleController) {
        return;
    }

    if (self.visibleController) {
        [self.visibleController willMoveToParentViewController:nil];
        [self.visibleController.view removeFromSuperview];
        [self.visibleController removeFromParentViewController];
    }

    [self addChildViewController:next];
    next.view.translatesAutoresizingMaskIntoConstraints = NO;
    [self.container addSubview:next.view];
    [NSLayoutConstraint activateConstraints:@[
        [next.view.topAnchor constraintEqualToAnchor:self.container.topAnchor],
        [next.view.leadingAnchor constraintEqualToAnchor:self.container.leadingAnchor],
        [next.view.trailingAnchor constraintEqualToAnchor:self.container.trailingAnchor],
        [next.view.bottomAnchor constraintEqualToAnchor:self.container.bottomAnchor]
    ]];
    [next didMoveToParentViewController:self];
    self.visibleController = next;
}

- (DPTagQueryViewController *)controllerForIndex:(NSInteger)index {
    DPTagQueryViewController *existing = self.controllers[@(index)];
    if (existing) {
        return existing;
    }

    DPTagQueryViewController *controller = [[DPTagQueryViewController alloc] init];
    controller.embedded = YES;
    switch (index) {
        case 0:
            controller.sortBy = DPTagSortPosted;
            break;
        case 1:
            controller.sortBy = DPTagSortRating;
            break;
        case 2:
            controller.sortBy = DPTagSortDownloaded;
            break;
        case 3:
            controller.sortBy = DPTagSortClassic;
            controller.collection = DPTagCollectionClassicTags;
            break;
        default:
            break;
    }
    self.controllers[@(index)] = controller;
    return controller;
}

/// Exposed so tests can confirm the collection each segment queries.
- (DPTagQueryViewController *)queryControllerForIndex:(NSInteger)index {
    return [self controllerForIndex:index];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
