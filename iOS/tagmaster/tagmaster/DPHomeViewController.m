//
//  DPHomeViewController.m
//  tagmaster
//
//  Created by David Poll on 9/28/13.
//  Copyright (c) 2013 DepollSoft. All rights reserved.
//

#import "DPHomeViewController.h"
#import "TMBarberPoleLoadingView.h"


#import "DPAppDelegate.h"
#import "DPTagCell.h"
#import "DPBrowseViewController.h"
#import "DPTagViewController.h"
#import "DPTeachableTagsController.h"
#import "DPSearchViewController.h"
#import "DPSettingsController.h"
#import "DPTagPageControllerBase.h"
#import "tagmaster-Swift.h"

static NSString *const TMRandomTagTitle = @"Random Tag";

@interface DPHomeViewController ()

@property (nonatomic, strong) TMBusyIndicator *busyIndicator;
@property (nonatomic, strong) UIStackView *creditRow;
@property (nonatomic, strong) UIStackView *legalRow;

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

+ (UIFont *)handwritingFontForTextStyle:(UIFontTextStyle)style size:(CGFloat)size maximum:(CGFloat)maximum {
    UIFont *base = [UIFont fontWithName:@"wickhop handwriting" size:size] ?: [UIFont preferredFontForTextStyle:style];
    return [[UIFontMetrics metricsForTextStyle:style] scaledFontForFont:base maximumPointSize:maximum];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    [DPAppDelegate setUpBackground:self.view];
    
    // Random Tag shows its progress on its own row; nothing covers the screen.
    self.busyIndicator = [[TMBusyIndicator alloc] init];
    __weak DPHomeViewController *weakSelf = self;
    self.busyIndicator.onBusyCountChanged = ^(NSUInteger busyCount) {
        [weakSelf reloadRandomTagRow];
    };
    
    [self.tableView registerClass:[DPTagCell class] forCellReuseIdentifier:@"Tag"];
    self.tableView.backgroundColor = [UIColor clearColor];
    self.tableView.rowHeight = UITableViewAutomaticDimension;
    self.tableView.estimatedRowHeight = 100;
    
    UIStackView *aboutFooter = [[UIStackView alloc] init];
    aboutFooter.axis = UILayoutConstraintAxisVertical;
    aboutFooter.layoutMarginsRelativeArrangement = YES;
    aboutFooter.directionalLayoutMargins = NSDirectionalEdgeInsetsMake(8, 16, 16, 16);
    NSInteger year = [[NSCalendar currentCalendar] component:NSCalendarUnitYear fromDate:[NSDate date]];
    UIButton *copyrightButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    [copyrightButton setTitle:[NSString stringWithFormat:@"DepollSoft © %ld", (long)year] forState:UIControlStateNormal];
    copyrightButton.url = [NSURL URLWithString:@"https://apps.depoll.com"];
    UIButton *bbsTagsButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    [bbsTagsButton setTitle:@"Content provided by BarbershopTags.com" forState:UIControlStateNormal];
    bbsTagsButton.url = [NSURL URLWithString:@"https://www.barbershoptags.com"];
    UIButton *touButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    [touButton setTitle:@"Terms of Use" forState:UIControlStateNormal];
    touButton.url = [NSURL URLWithString:@"https://apps.depoll.com/terms-of-use"];
    UIButton *donateButton = [TMWrappingButton buttonWithType:UIButtonTypeSystem];
    [donateButton setTitle:@"Donate" forState:UIControlStateNormal];
    donateButton.url = [NSURL URLWithString:@"https://www.davidpoll.com/applications/tag-master/donate"];
    for (UIButton *button in @[copyrightButton, bbsTagsButton, touButton, donateButton]) {
        button.titleLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleFootnote];
        button.titleLabel.adjustsFontForContentSizeCategory = YES;
        button.titleLabel.numberOfLines = 0;
        button.titleLabel.textAlignment = NSTextAlignmentCenter;
        UIButtonConfiguration *linkConfiguration = [UIButtonConfiguration plainButtonConfiguration];
        // The iPad sidebar column resolves tint to the label color; links keep the link color.
        linkConfiguration.baseForegroundColor = [UIColor systemBlueColor];
        linkConfiguration.contentInsets = NSDirectionalEdgeInsetsMake(4, 4, 4, 4);
        linkConfiguration.titleLineBreakMode = NSLineBreakByWordWrapping;
        __weak UIButton *weakButton = button;
        linkConfiguration.titleTextAttributesTransformer = ^NSDictionary<NSAttributedStringKey, id> *(NSDictionary<NSAttributedStringKey, id> *incoming) {
            NSMutableDictionary *attributes = [incoming mutableCopy];
            attributes[NSFontAttributeName] = [UIFont preferredFontForTextStyle:UIFontTextStyleFootnote compatibleWithTraitCollection:weakButton.traitCollection];
            return attributes;
        };
        button.configuration = linkConfiguration;
        button.accessibilityTraits |= UIAccessibilityTraitLink;
        [button.heightAnchor constraintGreaterThanOrEqualToConstant:44].active = YES;
    }
    copyrightButton.accessibilityIdentifier = @"home.credit.developer";
    bbsTagsButton.accessibilityIdentifier = @"home.credit.attribution";
    touButton.accessibilityIdentifier = @"home.credit.terms";
    donateButton.accessibilityIdentifier = @"home.credit.donate";
    self.legalRow = [[UIStackView alloc] initWithArrangedSubviews:@[touButton, donateButton]];
    self.legalRow.spacing = 4;
    self.creditRow = [[UIStackView alloc] initWithArrangedSubviews:@[copyrightButton, self.legalRow]];
    self.creditRow.spacing = 4;
    [aboutFooter addArrangedSubview:bbsTagsButton];
    [aboutFooter addArrangedSubview:self.creditRow];
    CGSize aboutFooterSize = [aboutFooter systemLayoutSizeFittingSize:UILayoutFittingCompressedSize];
    aboutFooter.frame = CGRectMake(0, 0, aboutFooterSize.width, aboutFooterSize.height);
    self.tableView.tableFooterView = aboutFooter;
    
    // Large handwriting title at the top of the app; the same face collapses inline on scroll.
    self.navigationItem.title = @"Tag Master";
    self.navigationItem.largeTitleDisplayMode = UINavigationItemLargeTitleDisplayModeAlways;
    self.navigationItem.backBarButtonItem = [[UIBarButtonItem alloc] init];
    self.navigationItem.backBarButtonItem.title = @"Home";
    
    self.navigationItem.leftBarButtonItem = self.editButtonItem;
    self.navigationItem.rightBarButtonItem =
        [DPAppDelegate barButtonItemWithSystemName:@"magnifyingglass"
                                             target:self
                                             action:@selector(search)];
    [self viewDidLoadExtension];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    // The handwriting face belongs to Home only; pushed screens use the system title.
    UINavigationBar *bar = self.navigationController.navigationBar;
    bar.prefersLargeTitles = YES;
    UIFont *largeFont = [DPHomeViewController handwritingFontForTextStyle:UIFontTextStyleLargeTitle size:34 maximum:44];
    UIFont *inlineFont = [DPHomeViewController handwritingFontForTextStyle:UIFontTextStyleHeadline size:22 maximum:26];
    // Wickhop's descender extends below UIKit's title box. Lift the glyphs, not the bar.
    UINavigationBarAppearance *appearance = [bar.standardAppearance copy];
    appearance.largeTitleTextAttributes = @{
        NSFontAttributeName: largeFont,
        NSBaselineOffsetAttributeName: @(8 * largeFont.pointSize / 34),
        NSForegroundColorAttributeName: [UIColor whiteColor]
    };
    appearance.titleTextAttributes = @{
        NSFontAttributeName: inlineFont,
        NSBaselineOffsetAttributeName: @(6 * inlineFont.pointSize / 22),
        NSForegroundColorAttributeName: [UIColor whiteColor]
    };
    bar.standardAppearance = appearance;
    bar.scrollEdgeAppearance = appearance;
    bar.compactAppearance = appearance;
    bar.compactScrollEdgeAppearance = appearance;
    // Give the inline face a full-height text box; the standard title label clips Wickhop.
    UILabel *inlineTitle = [[UILabel alloc] init];
    inlineTitle.attributedText = [[NSAttributedString alloc] initWithString:@"Tag Master" attributes:appearance.titleTextAttributes];
    inlineTitle.textAlignment = NSTextAlignmentCenter;
    inlineTitle.accessibilityTraits = UIAccessibilityTraitHeader;
    [inlineTitle sizeToFit];
    inlineTitle.frame = CGRectMake(0, 0, CGRectGetWidth(inlineTitle.bounds), 44);
    self.navigationItem.titleView = inlineTitle;
    [self updateInlineTitleVisibility];
}

- (void)updateInlineTitleVisibility {
    // UIKit does not fade custom titleViews with its large title. Show ours only
    // once the bar has collapsed to its standard height (44 to 54 points on iOS).
    self.navigationItem.titleView.hidden = CGRectGetHeight(self.navigationController.navigationBar.bounds) > 64;
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView {
    [self updateInlineTitleVisibility];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    UINavigationBar *bar = self.navigationController.navigationBar;
    UINavigationBarAppearance *appearance = [bar.standardAppearance copy];
    appearance.titleTextAttributes = @{NSForegroundColorAttributeName: [UIColor whiteColor]};
    appearance.largeTitleTextAttributes = @{NSForegroundColorAttributeName: [UIColor whiteColor]};
    bar.standardAppearance = appearance;
    bar.scrollEdgeAppearance = appearance;
    bar.compactAppearance = appearance;
    bar.compactScrollEdgeAppearance = appearance;
}

- (void)viewDidLayoutSubviews {
    [super viewDidLayoutSubviews];
    [self updateInlineTitleVisibility];
    UIView *footer = self.tableView.tableFooterView;
    CGFloat width = self.tableView.bounds.size.width;
    CGFloat available = MAX(0, width - 32);
    // Wrap whole link groups before compressing their titles. At accessibility
    // sizes the terms/donation pair can also become vertical.
    CGFloat legalWidth = self.legalRow.spacing;
    for (UIButton *button in self.legalRow.arrangedSubviews) {
        legalWidth += [button sizeThatFits:CGSizeMake(CGFLOAT_MAX, CGFLOAT_MAX)].width;
    }
    UIButton *developer = self.creditRow.arrangedSubviews.firstObject;
    CGFloat developerWidth = [developer sizeThatFits:CGSizeMake(CGFLOAT_MAX, CGFLOAT_MAX)].width;
    self.legalRow.axis = legalWidth > available ? UILayoutConstraintAxisVertical : UILayoutConstraintAxisHorizontal;
    self.creditRow.axis = developerWidth + legalWidth + self.creditRow.spacing > available ? UILayoutConstraintAxisVertical : UILayoutConstraintAxisHorizontal;
    CGFloat height = [footer systemLayoutSizeFittingSize:CGSizeMake(width, 0) withHorizontalFittingPriority:UILayoutPriorityRequired verticalFittingPriority:UILayoutPriorityFittingSizeLevel].height;
    if (footer.frame.size.height != height || footer.frame.size.width != width) {
        footer.frame = CGRectMake(0, 0, width, height);
        self.tableView.tableFooterView = footer;
    }
}

- (NSString *)tableView:(UITableView *)tableView titleForFooterInSection:(NSInteger)section {
    return section == 1 && [DPAppDelegate favorites].count == 0 ? @"No favorites yet. Open a tag and use Favorite and Teachable options to add a favorite." : nil;
}

- (void)search {
    [self.navigationController pushViewController:[[DPSearchViewController alloc] init] animated:YES];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self.tableView reloadData];
    [self updateEditButton];
}

// Edit only has work to do when there are favorites to reorder or remove.
- (void)updateEditButton {
    BOOL hasFavorites = [DPAppDelegate favorites].count > 0;
    self.editButtonItem.enabled = hasFavorites;
    if (!hasFavorites && self.isEditing) {
        [self setEditing:NO animated:YES];
    }
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
    [arr addObject:@{
                         @"title": @"Teachable Tags",
                         @"action": ^() {
            [self.navigationController pushViewController:[[DPTeachableTagsController alloc] init] animated:YES];
        }
                         }];
    [arr addObject:@{
                     @"title": TMRandomTagTitle,
                     @"action": ^() {
        [self randomTag];
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

- (NSIndexPath *)randomTagIndexPath {
    NSArray *items = [self navigationItems];
    NSUInteger row = [items indexOfObjectPassingTest:^BOOL(NSDictionary *item, NSUInteger idx, BOOL *stop) {
        return [item[@"title"] isEqualToString:TMRandomTagTitle];
    }];
    return row == NSNotFound ? nil : [NSIndexPath indexPathForRow:row inSection:0];
}

- (void)reloadRandomTagRow {
    NSIndexPath *indexPath = [self randomTagIndexPath];
    if (!self.isViewLoaded || !indexPath || self.tableView.numberOfSections == 0) return;
    if ([self.tableView numberOfRowsInSection:0] <= indexPath.row) return;
    [self.tableView reloadRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationNone];
}

- (void)randomTag {
        if (self.busyIndicator.busyCount > 0) return;
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
                if (!result) { [NSException raise:@"RandomTagUnavailable" format:@"Missing results"]; }
                if (result.available <= 0) {
                    dispatch_async(dispatch_get_main_queue(), ^{
                        [self.busyIndicator decrementBusyCount];
                        [self tm_showError:@"No tag could be selected. Check your connection or adjust Random Tag Filters in Settings, then try again." retry:^{ [self randomTag]; }];
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
                        [self tm_showError:@"No tag could be selected. Check your connection or adjust Random Tag Filters in Settings, then try again." retry:^{ [self randomTag]; }];
                    });
                    return;
                }

                dispatch_async(dispatch_get_main_queue(), ^{
                    [self.busyIndicator decrementBusyCount];
                    [DPAppDelegate showTagWithId:tag.tagId from:self];
                });
            }
            @catch (NSException *exception) {
                dispatch_async(dispatch_get_main_queue(), ^{
                    [self.busyIndicator decrementBusyCount];
                    [self tm_showError:@"A random tag couldn't be loaded. Check your connection and try again." retry:^{ [self randomTag]; }];
                });
            }
        });
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
        NSArray *items = [self navigationItems];
        NSString *title = indexPath.row < items.count ? items[indexPath.row][@"title"] : nil;
        cell.textLabel.text = title;
        cell.textLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
        cell.textLabel.adjustsFontForContentSizeCategory = YES;
        cell.textLabel.numberOfLines = 0;
        cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
        cell.backgroundColor = [UIColor clearColor];
        BOOL loadingRandom = [title isEqualToString:TMRandomTagTitle] && self.busyIndicator.busyCount > 0;
        if (loadingRandom) {
            TMBarberPoleLoadingView *spinner = [[TMBarberPoleLoadingView alloc] initWithOperationName:@"Loading random tag"];
            spinner.isAccessibilityElement = NO; // The row names the operation once.
            [spinner startAnimating];
            cell.accessoryView = spinner;
            cell.selectionStyle = UITableViewCellSelectionStyleNone;
            cell.textLabel.textColor = [UIColor secondaryLabelColor];
            cell.accessibilityLabel = @"Random Tag, loading";
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
    return section == 1 ? @"Favorites" : nil;
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
            [DPAppDelegate showTagWithId:favorites[indexPath.row].intValue from:self];
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
        }
        [self updateEditButton];
    }
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
}

// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.section == 0) {
        return NO;
    }
    return YES;
}

@end
