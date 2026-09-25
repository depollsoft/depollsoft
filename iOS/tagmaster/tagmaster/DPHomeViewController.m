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
    [NSNotificationCenter.defaultCenter addObserver:self selector:@selector(tm_splitSelectionChanged:) name:TMTagSelectionDidChangeNotification object:nil];
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
        linkConfiguration.baseForegroundColor = [DPAppDelegate accentColor];
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
    // Search stays outermost; Settings sits beside it as an icon, as on Android, rather than
    // spending a row of the table.
    self.navigationItem.rightBarButtonItems = @[
        [DPAppDelegate barButtonItemWithSystemName:@"magnifyingglass" target:self action:@selector(search)],
        [DPAppDelegate barButtonItemWithSystemName:@"gearshape" target:self action:@selector(openSettings)]
    ];
    // The favorites row for the tag open beside this list stays selected
    // instead of clearing when the screen reappears in an expanded split.
    self.clearsSelectionOnViewWillAppear = !(self.splitViewController && !self.splitViewController.isCollapsed);
    [self viewDidLoadExtension];
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [self tm_syncSelectionForSplit];
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

// A change that arrived mid-scroll was held back rather than dropped; the table
// is settled enough to take it as soon as the finger and the momentum are gone.
- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate {
    if (!decelerate) [self tm_applyPendingRefreshIfIdle];
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView {
    [self tm_applyPendingRefreshIfIdle];
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
    [self tm_syncSelectionForSplit];
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
    return section == TMHomeFavoritesSection && [DPAppDelegate favorites].count == 0 ? @"No favorites yet. Open a tag and use Favorite and Teachable options to add a favorite." : nil;
}

- (void)search {
    [self.navigationController pushViewController:[[DPSearchViewController alloc] init] animated:YES];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    [self.tableView reloadData];
    // This reload shows whatever was waiting, so nothing is left pending.
    self.tm_pendingListRefresh = NO;
    [self updateEditButton];
    [self tm_syncSelectionForSplit];
}

// Edit only has work to do when there are favorites or lists to reorder or remove.
- (void)updateEditButton {
    BOOL hasWork = [DPAppDelegate favorites].count > 0 || [self tm_customListKeys].count > 0;
    self.editButtonItem.enabled = hasWork;
    if (!hasWork && self.isEditing) {
        [self setEditing:NO animated:YES];
    }
}

- (NSArray<NSString *> *)tm_customListKeys {
    return [TMTagLists customKeys];
}

/// The custom list a Lists-group row stands for, or nil for Teachable Tags and New list.
- (NSString *)listKeyForRow:(NSInteger)row {
    NSArray<NSString *> *keys = [self tm_customListKeys];
    NSInteger index = row - 1;
    return (index >= 0 && index < (NSInteger)keys.count) ? keys[index] : nil;
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

    return arr;
}

- (void)openSettings {
    [self.navigationController pushViewController:[[DPSettingsController alloc] init] animated:YES];
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
    return 3;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    switch (section) {
        case TMHomeNavigationSection: return [self navigationItems].count;
        // Teachable Tags, then one row per custom list, then New list.
        case TMHomeListsSection: return [self tm_customListKeys].count + 2;
        default: return [DPAppDelegate favorites].count;
    }
}

- (void)tm_showCount:(NSUInteger)count in:(UITableViewCell *)cell {
    cell.detailTextLabel.text = count == 1 ? @"1 tag" : [NSString stringWithFormat:@"%lu tags", (unsigned long)count];
    cell.detailTextLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    cell.detailTextLabel.adjustsFontForContentSizeCategory = YES;
    cell.detailTextLabel.textColor = [UIColor secondaryLabelColor];
}

- (UITableViewCell *)makeListsCellForRow:(NSInteger)row {
    NSArray<NSString *> *keys = [self tm_customListKeys];
    NSString *key = [self listKeyForRow:row];
    BOOL isNewList = row == (NSInteger)keys.count + 1;
    UITableViewCell *cell = [[UITableViewCell alloc] initWithStyle:isNewList ? UITableViewCellStyleDefault : UITableViewCellStyleValue1
                                                   reuseIdentifier:nil];
    cell.backgroundColor = [UIColor clearColor];
    cell.textLabel.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody];
    cell.textLabel.adjustsFontForContentSizeCategory = YES;
    cell.textLabel.numberOfLines = 0;
    if (row == 0) {
        cell.textLabel.text = @"Teachable Tags";
        [self tm_showCount:[DPAppDelegate teachable].count in:cell];
        cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
        cell.accessibilityIdentifier = @"home.lists.teachable";
        return cell;
    }
    if (row == (NSInteger)keys.count + 1) {
        cell.textLabel.text = @"New list…";
        cell.imageView.image = [UIImage systemImageNamed:@"plus.circle"];
        cell.imageView.tintColor = [DPAppDelegate accentColor];
        cell.accessibilityIdentifier = @"home.lists.new";
        cell.accessibilityTraits = UIAccessibilityTraitButton;
        return cell;
    }
    cell.textLabel.text = [TMTagLists nameFor:key];
    [self tm_showCount:[TMTagLists idsFor:key].count in:cell];
    cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
    cell.accessibilityIdentifier = [NSString stringWithFormat:@"home.list.%@", key];
    return cell;
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.section == TMHomeListsSection) {
        return [self makeListsCellForRow:indexPath.row];
    }
    if (indexPath.section == TMHomeNavigationSection) {
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
    BOOL expanded = self.splitViewController && !self.splitViewController.isCollapsed;
    tagCell.accessoryType = expanded ? UITableViewCellAccessoryNone : UITableViewCellAccessoryDisclosureIndicator;
    return tagCell;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section {
    if (section == TMHomeListsSection) return @"Lists";
    return section == TMHomeFavoritesSection ? @"Favorites" : nil;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {
    BOOL expanded = self.splitViewController && !self.splitViewController.isCollapsed;
    if (indexPath.section == TMHomeListsSection) {
        [tableView deselectRowAtIndexPath:indexPath animated:YES];
        NSArray<NSString *> *keys = [self tm_customListKeys];
        if (indexPath.row == 0) {
            [self.navigationController pushViewController:[[DPTeachableTagsController alloc] init] animated:YES];
        } else if (indexPath.row == (NSInteger)keys.count + 1) {
            [self promptNewList];
        } else {
            [self.navigationController pushViewController:[[TMTagListController alloc] initWithListKey:keys[indexPath.row - 1]] animated:YES];
        }
        return;
    }
    if (indexPath.section == TMHomeNavigationSection) {
        // Navigation rows always deselect; only a tag row stays lit beside its detail.
        [tableView deselectRowAtIndexPath:indexPath animated:YES];
        NSArray *items = [self navigationItems];
        if (indexPath.row < items.count) {
            void (^block)(void) = items[indexPath.row][@"action"];
            block();
        }
    } else {
        if (!expanded) [tableView deselectRowAtIndexPath:indexPath animated:YES];
        NSArray<NSNumber *> *favorites = [DPAppDelegate favorites];
        if (indexPath.row < favorites.count) {
            [DPAppDelegate showTagWithId:favorites[indexPath.row].intValue from:self];
        }
    }
}

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath {
    // Teachable Tags and New list are permanent; only a user's own lists move or go.
    if (indexPath.section == TMHomeListsSection) return [self listKeyForRow:indexPath.row] != nil;
    return indexPath.section == TMHomeFavoritesSection;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath {
    if (editingStyle != UITableViewCellEditingStyleDelete) return;
    if (indexPath.section == TMHomeListsSection) {
        NSString *key = [self listKeyForRow:indexPath.row];
        if (!key) return;
        // Deleting a list is never silent, and the half-open swipe is the user's
        // place in the gesture: it stays put under the confirmation and closes
        // only once that is answered. Reloading the row here instead would snap
        // it shut first and read as a swipe that failed.
        BOOL swiped = !self.editing;
        __weak DPHomeViewController *weakSelf = self;
        [self confirmDeleteList:key settled:^{
            if (swiped) [weakSelf.tableView setEditing:NO animated:YES];
        }];
        return;
    }
    NSArray<NSNumber *> *favorites = [DPAppDelegate favorites];
    if (indexPath.row < favorites.count) {
        [DPAppDelegate removeFavorite:favorites[indexPath.row].intValue];
    }
    [self updateEditButton];
    [self tm_applyPendingRefreshIfIdle];
}

- (UIMenu *)tm_menuForListKey:(NSString *)key {
    __weak DPHomeViewController *weakSelf = self;
    UIAction *rename = [UIAction actionWithTitle:@"Rename…"
                                            image:[UIImage systemImageNamed:@"pencil"]
                                       identifier:nil
                                          handler:^(UIAction *action) { [weakSelf promptRenameList:key]; }];
    UIAction *remove = [UIAction actionWithTitle:@"Delete…"
                                            image:[UIImage systemImageNamed:@"trash"]
                                       identifier:nil
                                          handler:^(UIAction *action) { [weakSelf confirmDeleteList:key]; }];
    remove.attributes = UIMenuElementAttributesDestructive;
    return [UIMenu menuWithTitle:@"" children:@[rename, remove]];
}

- (UIContextMenuConfiguration *)tableView:(UITableView *)tableView contextMenuConfigurationForRowAtIndexPath:(NSIndexPath *)indexPath point:(CGPoint)point {
    NSString *key = indexPath.section == TMHomeListsSection ? [self listKeyForRow:indexPath.row] : nil;
    if (!key) return nil;
    __weak DPHomeViewController *weakSelf = self;
    return [UIContextMenuConfiguration configurationWithIdentifier:nil
                                                   previewProvider:nil
                                                    actionProvider:^UIMenu *(NSArray<UIMenuElement *> *suggested) {
        return [weakSelf tm_menuForListKey:key];
    }];
}

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath {
    return UITableViewAutomaticDimension;
}

- (NSIndexPath *)tableView:(UITableView *)tableView targetIndexPathForMoveFromRowAtIndexPath:(NSIndexPath *)sourceIndexPath toProposedIndexPath:(NSIndexPath *)proposedDestinationIndexPath {
    // A list stays among the lists, between Teachable Tags and New list; a
    // favorite stays among the favorites.
    if (sourceIndexPath.section == TMHomeListsSection) {
        NSInteger last = (NSInteger)[self tm_customListKeys].count;
        NSInteger row = proposedDestinationIndexPath.section == TMHomeListsSection
            ? proposedDestinationIndexPath.row
            : (proposedDestinationIndexPath.section < TMHomeListsSection ? 1 : last);
        row = MAX(1, MIN(last, row));
        return [NSIndexPath indexPathForRow:row inSection:TMHomeListsSection];
    }
    if (proposedDestinationIndexPath.section != TMHomeFavoritesSection) {
        return [NSIndexPath indexPathForRow:0 inSection:TMHomeFavoritesSection];
    }
    return proposedDestinationIndexPath;
}


// Override to support rearranging the table view.
- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath {
    if (fromIndexPath.section == TMHomeListsSection) {
        // The table already shows the new order; reloading from the registry's
        // change notification would fight the drag that produced it.
        self.tm_applyingLocalListChange = YES;
        [TMTagLists moveListFrom:fromIndexPath.row - 1 to:toIndexPath.row - 1];
        self.tm_applyingLocalListChange = NO;
        // The drag is over and the rows already match the registry; anything
        // that arrived from elsewhere while it ran is shown on the next turn,
        // once the table has finished committing this move.
        __weak DPHomeViewController *weakSelf = self;
        dispatch_async(dispatch_get_main_queue(), ^{ [weakSelf tm_applyPendingRefreshIfIdle]; });
        return;
    }
    [DPAppDelegate moveFavoriteAt:fromIndexPath.row to:toIndexPath.row];
}

// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath {
    if (indexPath.section == TMHomeListsSection) return [self listKeyForRow:indexPath.row] != nil;
    return indexPath.section == TMHomeFavoritesSection;
}

#pragma mark - TMTagListSource

- (NSArray<NSNumber *> *)tm_listedTagIds {
    return [DPAppDelegate favorites];
}

- (void)tm_didStepToTagId:(int)tagId {
    NSArray<NSNumber *> *favorites = [DPAppDelegate favorites];
    NSUInteger index = [favorites indexOfObject:@(tagId)];
    if (index == NSNotFound) return;
    NSIndexPath *path = [NSIndexPath indexPathForRow:index inSection:TMHomeFavoritesSection];
    [self.tableView selectRowAtIndexPath:path animated:!UIAccessibilityIsReduceMotionEnabled() scrollPosition:UITableViewScrollPositionNone];
    [self.tableView scrollToRowAtIndexPath:path atScrollPosition:UITableViewScrollPositionNone animated:!UIAccessibilityIsReduceMotionEnabled()];
}

- (void)tm_splitSelectionChanged:(NSNotification *)notification {
    [self tm_syncSelectionForSplit];
}

- (void)tm_syncSelectionForSplit {
    if (!self.isViewLoaded) return;
    BOOL expanded = self.splitViewController && !self.splitViewController.isCollapsed;
    self.clearsSelectionOnViewWillAppear = !expanded;
    for (UITableViewCell *cell in self.tableView.visibleCells) {
        if ([cell isKindOfClass:DPTagCell.class]) {
            UITableViewCellAccessoryType accessory = expanded ? UITableViewCellAccessoryNone : UITableViewCellAccessoryDisclosureIndicator;
            if (cell.accessoryType != accessory) cell.accessoryType = accessory;
        }
    }
    NSNumber *current = expanded ? [DPAppDelegate currentSplitTagIdFor:self] : nil;
    NSUInteger index = current ? [[DPAppDelegate favorites] indexOfObject:current] : NSNotFound;
    NSIndexPath *path = index == NSNotFound ? nil : [NSIndexPath indexPathForRow:index inSection:TMHomeFavoritesSection];
    NSIndexPath *selected = self.tableView.indexPathForSelectedRow;
    if (selected && ![selected isEqual:path]) [self.tableView deselectRowAtIndexPath:selected animated:NO];
    if (path && ![selected isEqual:path]) [self.tableView selectRowAtIndexPath:path animated:NO scrollPosition:UITableViewScrollPositionNone];
}

@end
