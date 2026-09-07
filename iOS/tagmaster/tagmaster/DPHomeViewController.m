//
//  DPHomeViewController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  Discovery actions followed by two peer built-in list destinations.
//

#import "DPHomeViewController.h"


#import "DPAppDelegate.h"
#import "DPFavoritesViewController.h"
#import "DPBrowseViewController.h"
#import "DPTagViewController.h"
#import "DPTeachableTagsController.h"
#import "DPSearchViewController.h"
#import "DPSettingsController.h"
#import "tagmaster-Swift.h"

@interface DPHomeViewController ()

@property (nonatomic, strong) DPBusyIndicator *busyIndicator;

@end

@implementation DPHomeViewController

- (id)init {
    return [self initWithStyle:UITableViewStylePlain];
}

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.tableView];

    self.busyIndicator = [[DPBusyIndicator alloc] init];
    self.busyIndicator.translatesAutoresizingMaskIntoConstraints = NO;

    self.tableView.estimatedRowHeight = 72;
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.cellLayoutMarginsFollowReadableWidth = YES;

    self.tableView.tableFooterView = [self makeDeskFooter];

    self.navigationItem.title = @"Home";
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeNever;
    self.navigationItem.titleView = [TMTheme wordmarkLabel:@"Tag Master"];
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] init];
    self.navigationItem.backBarButtonItem.title = @"Home";


    UIBarButtonItem *settings = [DPAppDelegate barButtonItemWithSystemName:@"gearshape"
                                                                    target:self
                                                                    action:@selector(openSettings)];
    settings.accessibilityLabel = @"Settings";
    self.navigationItem.rightBarButtonItem = settings;

    [self viewDidLoadExtension];
}

/// Attribution remains reachable below the repertoire.
- (UIView *)makeDeskFooter {
    UIStackView *links = [[UIStackView alloc] init];
    links.axis = UILayoutConstraintAxisVertical;
    links.alignment = UIStackViewAlignmentCenter;
    links.spacing = TMTheme.spaceS;

    NSArray<NSArray *> *entries = @[
        @[@"Content provided by BarbershopTags.com", @"http://www.barbershoptags.com"],
        @[@"Terms of Use", @"http://apps.depoll.com/terms-of-use"],
        @[@"Donate", @"http://www.davidpoll.com/applications/tag-master/donate"],
        @[[NSString stringWithFormat:@"Depollsoft © %@", [@__DATE__ substringFromIndex:11-4]],
          @"http://apps.depoll.com"]
    ];
    for (NSArray *entry in entries) {
        UIButton *button = [UIButton buttonWithType:UIButtonTypeSystem];
        [button setTitle:entry[0] forState:UIControlStateNormal];
        button.url = [NSURL URLWithString:entry[1]];
        button.titleLabel.font = [TMTheme metadataFont];
        button.titleLabel.adjustsFontForContentSizeCategory = YES;
        button.titleLabel.numberOfLines = 0;
        button.titleLabel.textAlignment = NSTextAlignmentCenter;
        [button.heightAnchor constraintGreaterThanOrEqualToConstant:TMTheme.minimumTarget].active = YES;
        [links addArrangedSubview:button];
    }

    UIStackView *stack = [[UIStackView alloc] initWithArrangedSubviews:@[
        links
    ]];
    stack.axis = UILayoutConstraintAxisVertical;
    stack.alignment = UIStackViewAlignmentCenter;
    stack.spacing = TMTheme.spaceL;
    stack.layoutMargins = UIEdgeInsetsMake(TMTheme.spaceXL, TMTheme.spaceL,
                                           TMTheme.spaceXL, TMTheme.spaceL);
    stack.layoutMarginsRelativeArrangement = YES;

    CGSize size = [stack systemLayoutSizeFittingSize:UILayoutFittingCompressedSize];
    stack.frame = CGRectMake(0, 0, size.width, size.height);
    return stack;
}

- (void)openSettings {
    [self.navigationController pushViewController:[[DPSettingsController alloc] init] animated:YES];
}

- (void)search {
    [self.navigationController pushViewController:[[DPSearchViewController alloc] init] animated:YES];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self.tableView reloadData];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self.tableView reloadData];

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

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSArray *)navigationItems {
    NSMutableArray *arr = [NSMutableArray array];

    [arr addObject:@{
        @"title": @"Find a tag",
        @"detail": @"Search titles and lyrics, filter by parts and materials",
        @"symbol": @"magnifyingglass",
        @"action": ^() {
            [self search];
        }
    }];

    [arr addObject:@{
        @"title": @"Browse",
        @"detail": @"Latest, top rated, downloads and classic tags",
        @"symbol": @"list.bullet",
        @"action": ^() {
            [self.navigationController pushViewController:[[DPBrowseViewController alloc] init] animated:YES];
        }
    }];

    [arr addObject:@{
        @"title": @"Random Tag",
        @"detail": @"Something new to sing, inside your filters",
        @"symbol": @"shuffle",
        @"action": ^() {
            [self openRandomTag];
        }
    }];

    [arr addObject:@{
        @"title": @"Open Tag ID",
        @"detail": @"Jump straight to a number someone called out",
        @"symbol": @"number",
        @"action": ^() {
            [self openTag];
        }
    }];

    return arr;
}

- (NSArray *)listItems {
    return @[
        @{@"title": @"Favorites", @"count": @([DPAppDelegate favorites].count),
          @"identifier": @"home.favorites", @"symbol": @"star",
          @"detail": @"Open your favorites", @"action": ^{
            [self.navigationController pushViewController:[[DPFavoritesViewController alloc] init] animated:YES];
          }},
        @{@"title": @"Teachable Tags", @"count": @([DPAppDelegate teachable].count),
          @"identifier": @"home.teachable", @"symbol": @"person.2.wave.2",
          @"detail": @"Open the tags you are ready to teach", @"action": ^{
            [self.navigationController pushViewController:[[DPTeachableTagsController alloc] init] animated:YES];
          }}
    ];
}

- (void)openRandomTag {
    [self.busyIndicator incrementBusyCount];
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        @try {
            DPTagQueryResult *result = [DPTag query:nil
                                    numberOfResults:0
                                              start:0
                                              parts:nil
                                     learningTracks:[DPSettingsController learningTracks]
                                         sheetMusic:[DPSettingsController sheetMusic]
                                         collection:DPTagCollectionNone
                                             sortBy:DPTagSortNone
                                      minimumRating:[DPSettingsController minRating]
                                   minimumDownloads:[DPSettingsController minDownloads]
                                              cache:NO
                                          fieldList:@"id"];
            if (result.available <= 0) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [self.busyIndicator decrementBusyCount];
                    [self reportRandomTagProblem:@"No tags match your random-tag filters."
                                        recovery:@"Loosen the minimum rating or downloads in Settings."];
                });
                return;
            }

            int chosenResult = arc4random_uniform((uint32_t)result.available);

            result = [DPTag query:nil
                  numberOfResults:1
                            start:chosenResult
                            parts:nil
                   learningTracks:[DPSettingsController learningTracks]
                       sheetMusic:[DPSettingsController sheetMusic]
                       collection:DPTagCollectionNone
                           sortBy:DPTagSortNone
                    minimumRating:[DPSettingsController minRating]
                 minimumDownloads:[DPSettingsController minDownloads]
                            cache:NO
                        fieldList:@"id"];
            DPTag *tag = result.tags.firstObject;

            if (!tag) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [self.busyIndicator decrementBusyCount];
                    [self reportRandomTagProblem:@"That tag could not be loaded."
                                        recovery:@"Try again in a moment."];
                });
                return;
            }

            dispatch_async(dispatch_get_main_queue(), ^{
                DPTagViewController *tagController = [[DPTagViewController alloc] init];
                tagController.tagId = tag.tagId;
                [self.navigationController pushViewController:tagController animated:YES];
                [self.busyIndicator decrementBusyCount];
            });
        }
        @catch (NSException *exception) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self.busyIndicator decrementBusyCount];
                [self reportRandomTagProblem:@"Tag Master could not reach BarbershopTags.com."
                                    recovery:@"Check your connection and try again."];
            });
        }
    });
}

- (void)reportRandomTagProblem:(NSString *)problem recovery:(NSString *)recovery {
    UIAlertController *alert =
        [UIAlertController alertControllerWithTitle:@"Random Tag"
                                            message:[NSString stringWithFormat:@"%@ %@", problem, recovery]
                                     preferredStyle:UIAlertControllerStyleAlert];
    [alert addAction:[UIAlertAction actionWithTitle:@"Try Again"
                                              style:UIAlertActionStyleDefault
                                            handler:^(UIAlertAction * _Nonnull action) {
        [self openRandomTag];
    }]];
    [alert addAction:[UIAlertAction actionWithTitle:@"OK" style:UIAlertActionStyleCancel handler:nil]];
    [self presentViewController:alert animated:YES completion:nil];
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    return 2;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    return section == 0 ? [self navigationItems].count : [self listItems].count;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    BOOL list = indexPath.section == 1;
    NSDictionary *item = (list ? [self listItems] : [self navigationItems])[indexPath.row];
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleValue1 reuseIdentifier:nil];
    UIListContentConfiguration *content = list ? [UIListContentConfiguration valueCellConfiguration]
                                              : [UIListContentConfiguration subtitleCellConfiguration];
    content.text = item[@"title"];
    content.textProperties.font = [TMTheme fontWithStyle:UIFontTextStyleBody weight:UIFontWeightSemibold];
    content.textProperties.numberOfLines = 0;
    content.textProperties.color = !list && indexPath.row == 0 ? [TMTheme tint] : [TMTheme primaryText];
    content.secondaryText = list ? [item[@"count"] stringValue] : (indexPath.row == 0 ? item[@"detail"] : nil);
    content.secondaryTextProperties.font = [TMTheme metadataFont];
    content.secondaryTextProperties.color = [TMTheme secondaryText];
    content.secondaryTextProperties.numberOfLines = 0;
    content.image = [UIImage systemImageNamed:item[@"symbol"]];
    content.imageProperties.tintColor = [TMTheme tint];
    content.imageProperties.preferredSymbolConfiguration =
        [UIImageSymbolConfiguration configurationWithTextStyle:UIFontTextStyleTitle3];
    content.imageToTextPadding = TMTheme.spaceM;
    content.directionalLayoutMargins = NSDirectionalEdgeInsetsMake(8, 0, 8, 0);
    cell.contentConfiguration = content;
    [cell.contentView.heightAnchor constraintGreaterThanOrEqualToConstant:48].active = YES;
    cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
    cell.accessibilityLabel = item[@"title"];
    cell.accessibilityHint = item[@"detail"];
    if (list) {
        cell.accessibilityIdentifier = item[@"identifier"];
        cell.accessibilityValue = [NSString stringWithFormat:@"%@ tags", item[@"count"]];
    }
    cell.backgroundColor = UIColor.clearColor;
    return cell;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    return section == 1 ? @"Your lists" : nil;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    NSDictionary *item = (indexPath.section == 1 ? [self listItems] : [self navigationItems])[indexPath.row];
    void (^action)(void) = item[@"action"];
    action();
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    return NO;
}

@end
