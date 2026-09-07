//
//  DPHomeViewController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//
//  The singing desk. Four actions a group reaches for — find, random, open by
//  ID, teachable — and then the favorites themselves, ready to sing from.
//

#import "DPHomeViewController.h"


#import "DPAppDelegate.h"
#import "DPTagCell.h"
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
    return [self initWithStyle:UITableViewStyleInsetGrouped];
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

    [self.tableView registerClass:[DPTagCell class] forCellReuseIdentifier:@"Tag"];
    self.tableView.estimatedRowHeight = 72;
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.cellLayoutMarginsFollowReadableWidth = YES;

    self.tableView.tableFooterView = [self makeDeskFooter];

    self.navigationItem.title = @"Home";
    self.navigationItem.titleView = [TMTheme wordmarkLabel:@"Tag Master"];
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] init];
    self.navigationItem.backBarButtonItem.title = @"Home";

    self.navigationItem.leftBarButtonItem = self.editButtonItem;
    self.editButtonItem.accessibilityLabel = @"Edit favorites";

    UIBarButtonItem *settings = [DPAppDelegate barButtonItemWithSystemName:@"gearshape"
                                                                    target:self
                                                                    action:@selector(openSettings)];
    settings.accessibilityLabel = @"Settings";
    self.navigationItem.rightBarButtonItem = settings;

    [self viewDidLoadExtension];
}

/// Attribution and the app's own quiet identity mark, at the foot of the desk.
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
        [TMTheme identityMarkWithHeight:64], links
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
    // Search is a destination of its own; bring it forward rather than stacking
    // a second copy of it on top of Home.
    UITabBarController *root = self.tabBarController;
    if ([root isKindOfClass:[TMRootController class]]) {
        [(TMRootController *)root focusSearch];
        return;
    }
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

    [arr addObject:@{
        @"title": @"Teachable Tags",
        @"detail": [self teachableDetailText],
        @"symbol": @"person.2.wave.2",
        @"action": ^() {
            [self.navigationController pushViewController:[[DPTeachableTagsController alloc] init]
                                                 animated:YES];
        }
    }];

    return arr;
}

- (NSString *)teachableDetailText {
    NSUInteger count = [DPAppDelegate teachable].count;
    if (count == 0) {
        return @"The tags you are ready to teach";
    }
    if (count == 1) {
        return @"1 tag you are ready to teach";
    }
    return [NSString stringWithFormat:@"%lu tags you are ready to teach", (unsigned long)count];
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
    if (section == 0) {
        return [self navigationItems].count;
    } else {
        return [DPAppDelegate favorites].count;
    }
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.section == 0) {
        UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle
                                                      reuseIdentifier:nil];
        NSArray *items = [self navigationItems];
        if (indexPath.row < items.count) {
            NSDictionary *item = items[indexPath.row];
            UIListContentConfiguration *content = [UIListContentConfiguration subtitleCellConfiguration];
            content.text = item[@"title"];
            content.textProperties.font = [TMTheme fontWithStyle:UIFontTextStyleBody
                                                          weight:UIFontWeightSemibold];
            content.textProperties.color = [TMTheme primaryText];
            content.secondaryText = item[@"detail"];
            content.secondaryTextProperties.font = [TMTheme metadataFont];
            content.secondaryTextProperties.color = [TMTheme secondaryText];
            content.secondaryTextProperties.numberOfLines = 0;
            content.image = [UIImage systemImageNamed:item[@"symbol"]];
            content.imageProperties.tintColor = [TMTheme tint];
            content.imageProperties.preferredSymbolConfiguration =
                [UIImageSymbolConfiguration configurationWithTextStyle:UIFontTextStyleTitle3];
            content.imageToTextPadding = TMTheme.spaceM;
            content.directionalLayoutMargins =
                NSDirectionalEdgeInsetsMake(TMTheme.spaceM, 0, TMTheme.spaceM, 0);
            cell.contentConfiguration = content;
            cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
            cell.accessibilityLabel = item[@"title"];
            cell.accessibilityHint = item[@"detail"];
        }
        return cell;
    }

    DPTagCell *tagCell = [self.tableView dequeueReusableCellWithIdentifier:@"Tag" forIndexPath:indexPath];
    NSArray<NSNumber *> *favorites = [DPAppDelegate favorites];
    if (indexPath.row < favorites.count) {
        tagCell.tagId = favorites[indexPath.row].intValue;
    }
    return tagCell;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    switch (section) {
        case 0:
            return nil;
        case 1:
            return @"Favorites";
        default:
            break;
    }
    return nil;
}

- (NSString *)tableView:(UITableView *)tableView titleForFooterInSection:(NSInteger)section {
    if (section == 1 && [DPAppDelegate favorites].count == 0) {
        return @"Tags you favorite land here, ready for the next afterglow. "
               @"Open any tag and use the tag menu to add it.";
    }
    return nil;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    if (indexPath.section == 0) {
        NSArray *items = [self navigationItems];
        if (indexPath.row < items.count) {
            void (^block)(void) = items[indexPath.row][@"action"];
            block();
        }
    } else {
        NSArray<NSNumber *> *favorites = [DPAppDelegate favorites];
        if (indexPath.row < favorites.count) {
            DPTagViewController *tagViewController = [[DPTagViewController alloc] init];
            tagViewController.tagId = favorites[indexPath.row].intValue;
            [self.navigationController pushViewController:tagViewController animated:YES];
        }
    }
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.section == 0) {
        return NO;
    }
    return YES;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        NSArray<NSNumber *> *favorites = [DPAppDelegate favorites];
        if (indexPath.row < favorites.count) {
            [DPAppDelegate removeFavorite:favorites[indexPath.row].intValue];
            [TMTheme saved];
        }
    }
}

- (NSString *)tableView:(UITableView *)tableView titleForDeleteConfirmationButtonForRowAtIndexPath:(NSIndexPath *)indexPath {
    return @"Remove";
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewAutomaticDimension;
}

- (NSIndexPath *)tableView:(UITableView *)tableView targetIndexPathForMoveFromRowAtIndexPath:(NSIndexPath *)sourceIndexPath toProposedIndexPath:(NSIndexPath *)proposedDestinationIndexPath {
    if (proposedDestinationIndexPath.section == 0) {
        return [NSIndexPath indexPathForRow:0 inSection:1];
    }
    return proposedDestinationIndexPath;
}


// Override to support rearranging the table view.
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
    [DPAppDelegate moveFavoriteAt:fromIndexPath.row to:toIndexPath.row];
    [TMTheme saved];
}

// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.section == 0) {
        return NO;
    }
    return YES;
}

@end
