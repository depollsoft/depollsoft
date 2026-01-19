//
//  DPHomeViewController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPHomeViewController.h"

@import ParseCore;

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
    return [self initWithStyle:UITableViewStyleGrouped];
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
    UIFont *font = [UIFont fontWithName:@"wickhop handwriting" size:20];
    
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.view];
    
    self.busyIndicator = [[DPBusyIndicator alloc] init];
    self.busyIndicator.translatesAutoresizingMaskIntoConstraints = NO;
    
    [self.tableView registerClass:[DPTagCell class] forCellReuseIdentifier:@"Tag"];
    self.tableView.backgroundColor = [UIColor clearColor];
    
    DPGridLayout *aboutFooter = [[DPGridLayout alloc] init];
    aboutFooter.rowDimensions = @[
                                  [DPGridDimension dimensionWithSize:[UIFont smallSystemFontSize] * 1.5],
                                  [DPGridDimension dimensionWithSize:[UIFont smallSystemFontSize] * 1.5],
                                  [DPGridDimension dimensionWithSize:[UIFont smallSystemFontSize] * 1.5],
                                  [DPGridDimension dimensionWithSize:[UIFont smallSystemFontSize] * 1.5]
                                  ];
    UIButton *copyrightButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    NSString *copyright = [NSString stringWithFormat:@"Depollsoft © %@", [@__DATE__ substringFromIndex:11-4]];
    [copyrightButton setTitle:copyright forState:UIControlStateNormal];
    copyrightButton.url = [NSURL URLWithString:@"http://apps.depoll.com"];
    copyrightButton.titleLabel.font = [UIFont systemFontOfSize:[UIFont smallSystemFontSize]];
    UIButton *bbsTagsButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [bbsTagsButton setTitle:@"Content provided by BarbershopTags.com" forState:UIControlStateNormal];
    bbsTagsButton.url = [NSURL URLWithString:@"http://www.barbershoptags.com"];
    bbsTagsButton.titleLabel.font = [UIFont systemFontOfSize:[UIFont smallSystemFontSize]];
    UIButton *touButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [touButton setTitle:@"Terms of Use" forState:UIControlStateNormal];
    touButton.url = [NSURL URLWithString:@"http://apps.depoll.com/terms-of-use"];
    touButton.titleLabel.font = [UIFont systemFontOfSize:[UIFont smallSystemFontSize]];
    UIButton *donateButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
    [donateButton setTitle:@"Donate" forState:UIControlStateNormal];
    donateButton.url = [NSURL URLWithString:@"http://www.davidpoll.com/applications/tag-master/donate"];
    donateButton.titleLabel.font = [UIFont systemFontOfSize:[UIFont smallSystemFontSize]];
    [aboutFooter addSubview:copyrightButton row:0 column:0];
    [aboutFooter addSubview:bbsTagsButton row:1 column:0];
    [aboutFooter addSubview:touButton row:2 column:0];
    [aboutFooter addSubview:donateButton row:3 column:0];
    CGSize aboutFooterSize = [aboutFooter systemLayoutSizeFittingSize:UILayoutFittingCompressedSize];
    aboutFooter.frame = CGRectMake(0, 0, aboutFooterSize.width, aboutFooterSize.height);
    self.tableView.tableFooterView = aboutFooter;
    
    UILabel *titleLabel = [[UILabel alloc] init];
    titleLabel.font = font;
    titleLabel.text = @"Tag Master";
    [titleLabel sizeToFit];
    titleLabel.frame = CGRectMake(titleLabel.frame.origin.x, titleLabel.frame.origin.y, titleLabel.frame.size.width, titleLabel.frame.size.height * 2);
    self.navigationItem.titleView = titleLabel;
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] init];
    self.navigationItem.backBarButtonItem.title = @"Home";
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
    
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    self.navigationItem.leftBarButtonItem = self.editButtonItem;
    
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemSearch
                                                                                           target:self
                                                                                           action:@selector(search)];
    [self viewDidLoadExtension];
}

- (void)search {
    [self.navigationController pushViewController:[[DPSearchViewController alloc] init] animated:YES];
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
                     @"title": @"Browse",
                     @"action": ^() {
        [self.navigationController pushViewController:[[DPBrowseViewController alloc] init] animated:YES];
    }
                     }];
    if ([DPAppDelegate teachable].count > 0) {
        [arr addObject:@{
                         @"title": @"Teachable Tags",
                         @"action": ^() {
            [self.navigationController pushViewController:[[DPTeachableTagsController alloc] init] animated:YES];
        }
                         }];
    }
    [arr addObject:@{
                     @"title": @"Random Tag",
                     @"action": ^() {
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
                int chosenResult = arc4random_uniform(result.available);
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
                DPTag *tag = result.tags[0];
                dispatch_async(dispatch_get_main_queue(), ^{
                    DPTagViewController *tagController = [[DPTagViewController alloc] init];
                    tagController.tagId = tag.tagId;
                    [self.navigationController pushViewController:tagController animated:YES];
                    [self.busyIndicator decrementBusyCount];
                });
            }
            @catch (NSException *exception) {
                [self.busyIndicator decrementBusyCount];
            }
        });
    }
                     }];
    
    [arr addObject:@{
                     @"title": @"Open Tag",
                     @"action": ^() {
        [self openTag];
    }
                     }];
    
    [arr addObject:@{
                     @"title": @"Settings",
                     @"action": ^() {
        [self.navigationController pushViewController:[[DPSettingsController alloc] init] animated:YES];
    }
                     }];
    
    return arr;
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
        UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:nil];
        cell.textLabel.text = [self navigationItems][indexPath.row][@"title"];
        cell.backgroundColor = [UIColor clearColor];
        return cell;
    }
    
    int tagId = [[DPAppDelegate favorites][indexPath.row] intValue];
    DPTagCell *tagCell = [self.tableView dequeueReusableCellWithIdentifier:@"Tag" forIndexPath:indexPath];
    tagCell.tagId = tagId;
    return tagCell;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    switch (section) {
        case 0:
            return @"Main";
        case 1:
            return @"Favorites";
        default:
            break;
    }
    return nil;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    if (indexPath.section == 0) {
        void (^block)(void) = [self navigationItems][indexPath.row][@"action"];
        block();
    } else {
        int tagId = [[DPAppDelegate favorites][indexPath.row] intValue];
        DPTagViewController *tagViewController = [[DPTagViewController alloc] init];
        tagViewController.tagId = tagId;
        [self.navigationController pushViewController:tagViewController animated:YES];
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
        [DPAppDelegate removeFavorite:[[DPAppDelegate favorites][indexPath.row] intValue]];
    }
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.section == 0) {
        return [super tableView:tableView heightForRowAtIndexPath:indexPath];
    } else {
        DPTag *tag = [DPTag loadFromCache:[[DPAppDelegate favorites][indexPath.row] intValue]];
        return [DPTagCell tagHeight:tag];
    }
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
}

// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.section == 0) {
        return NO;
    }
    return YES;
}

@end
