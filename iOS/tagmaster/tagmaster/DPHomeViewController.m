//
//  DPHomeViewController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPHomeViewController.h"
#import "DPAppDelegate.h"
#import "DPTagCell.h"
#import "DPBrowseViewController.h"
#import "DPTagViewController.h"
#import "DPTeachableTagsController.h"
#import "DPSearchViewController.h"
#import "DPSettingsController.h"

@interface DPHomeViewController ()

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
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.view];
    
    [self.tableView registerClass:[DPTagCell class] forCellReuseIdentifier:@"Tag"];
    self.tableView.backgroundColor = [UIColor clearColor];
    
    self.navigationItem.title = @"Tag Master";
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] init];
    self.navigationItem.backBarButtonItem.title = @"Home";
    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
    
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    self.navigationItem.leftBarButtonItem = self.editButtonItem;
    
    self.navigationItem.rightBarButtonItem = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemSearch
                                                                                           target:self
                                                                                           action:@selector(search)];
}

- (void)search {
    [self.navigationController pushViewController:[[DPSearchViewController alloc] init] animated:YES];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self.tableView reloadData];
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
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
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
            });
        });
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
        void (^block)() = [self navigationItems][indexPath.row][@"action"];
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
        [self.tableView reloadData];
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
