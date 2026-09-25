#import <XCTest/XCTest.h>
#import <UIKit/UIKit.h>
#import <objc/runtime.h>

#import "DPAppDelegate.h"
#import "DPHomeViewController.h"
#import "DPBrowseViewController.h"
#import "DPTagPageControllerBase.h"
#import "DPTagCell.h"
#import "DPBusyIndicator.h"
#import "DPSearchViewController.h"
#import "DPTagQueryViewController.h"
#import "TMLogoArtwork.h"
#import "TMQuartetArtwork.h"
#import "TMLogoBackgroundView.h"

#import "DPTag.h"
#import "DPTagQueryResult.h"
#import "DPRemoteLocation.h"
#import "DPTrack.h"
#import "DPVideo.h"
#import "DPFileCache.h"
#import "DPSettingsController.h"
#import <AVFoundation/AVFoundation.h>
#import "tagmaster-Swift.h"
#import "TMReviewLoader.h"

// XCTNSPredicateExpectation re-evaluates its predicate on a one-second repeating
// timer, so every wait whose condition was not already true cost a full second of
// wall clock even when UIKit had settled microseconds later. This spins the main
// run loop in short slices instead and returns the instant the condition holds,
// keeping the same generous ceiling so a genuinely stuck condition still fails.
static BOOL TMSpinUntil(NSTimeInterval timeout, BOOL (^condition)(void)) {
    NSDate *limit = [NSDate dateWithTimeIntervalSinceNow:timeout];
    while (YES) {
        @autoreleasepool { if (condition()) return YES; }
        if (limit.timeIntervalSinceNow <= 0) break;
        // Run the slice to completion rather than returning after one source:
        // UIKit commits CA transactions and finishes containment transitions from
        // kCFRunLoopBeforeWaiting observers, which only fire once the loop idles.
        @autoreleasepool { CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0.005, false); }
    }
    @autoreleasepool { return condition(); }
}

// Reports at the caller's line, exactly as the XCTWaiter assertions it replaces.
#define TMAssertEventually(timeout, conditionBlock) \
    XCTAssertTrue(TMSpinUntil((timeout), (conditionBlock)), \
                  @"Condition never held within %g seconds", (double)(timeout))

static NSData *TMSheetMusicFixturePDF(void) {
    UIGraphicsPDFRenderer *renderer = [[UIGraphicsPDFRenderer alloc] initWithBounds:CGRectMake(0, 0, 612, 792)];
    return [renderer PDFDataWithActions:^(UIGraphicsPDFRendererContext *context) {
        [context beginPage];
        [@"Sheet music test fixture" drawAtPoint:CGPointMake(32, 32)
                                withAttributes:@{NSFontAttributeName:[UIFont systemFontOfSize:20]}];
    }];
}

@interface DPHomeViewController (Testing)
- (NSArray<NSDictionary *> *)navigationItems;
@end

@interface DPAppDelegate (ListsTesting)
+ (NSArray<NSNumber *> *)favorites;
+ (NSArray<NSNumber *> *)teachable;
+ (void)setFavorites:(NSArray<NSNumber *> *)favorites;
+ (void)setTeachable:(NSArray<NSNumber *> *)teachable;
@end

@interface TestingNavigationController : UINavigationController
@end

@implementation TestingNavigationController

- (void)pushViewController:(UIViewController *)viewController animated:(BOOL)animated {
    if (self.viewControllers.count == 0) {
        [super pushViewController:viewController animated:animated];
        return;
    }
    NSMutableArray<UIViewController *> *stack = [self.viewControllers mutableCopy];
    [stack addObject:viewController];
    [self setViewControllers:stack animated:NO];
}

@end

static NSString *const kUserDataChangedNotification = @"tagmaster.userDataChanged";
static NSString *const kListsDefaultsKey = @"depollsoft.pitchperfect.lists";

@interface TagmasterAppLogicTests : XCTestCase
@property (nonatomic, strong) NSData *placeholderImageData;
@end

@implementation TagmasterAppLogicTests

- (void)setUp {
    [super setUp];
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults removeObjectForKey:kListsDefaultsKey];
    [defaults removeObjectForKey:@"random.minRating"];
    [defaults removeObjectForKey:@"random.sheetMusic"];
    [defaults removeObjectForKey:@"random.learningTracks"];
    [defaults synchronize];
    self.placeholderImageData = [@"img" dataUsingEncoding:NSUTF8StringEncoding];
}

- (void)tearDown {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults removeObjectForKey:kListsDefaultsKey];
    [defaults synchronize];
    [super tearDown];
}

#pragma mark - Helpers

- (DPRemoteLocation *)remoteLocationWithURLString:(NSString *)urlString type:(NSString *)type {
    DPRemoteLocation *location = [[DPRemoteLocation alloc] init];
    location.uri = [NSURL URLWithString:urlString];
    location.type = type;
    NSString *filename = [NSString stringWithFormat:@"%@.%@", [[NSUUID UUID] UUIDString], type];
    location.cachedUri = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:filename]];
    return location;
}

- (void)primeThumbnailCacheForCode:(NSString *)code {
    NSURL *thumbnailURL = [NSURL URLWithString:[NSString stringWithFormat:@"https://img.youtube.com/vi/%@/2.jpg", code]];
    NSString *key = [DPFileCache keyForURL:thumbnailURL];
    [DPFileCache writeData:self.placeholderImageData forKey:key];
}

- (DPTag *)buildSampleTagWithIdentifier:(int)identifier {
    DPTag *tag = [[DPTag alloc] init];
    tag.tagId = identifier;
    tag.title = [NSString stringWithFormat:@"Sample Tag %d", identifier];
    tag.alternativeTitle = @"Alternate Title";
    tag.rating = 4.5;
    tag.parts = 4;
    tag.tagType = @"Ballad";
    tag.writtenKey = @"C Major";
    tag.classicTagNumber = 77;
    tag.sheetMusicUri = [self remoteLocationWithURLString:@"https://example.com/sheet.pdf" type:@"pdf"];
    tag.lyrics = @"La la la la";
    tag.notes = @"Practice slowly and listen for overtones.";
    tag.downloadCount = 1234;
    tag.provider = @"BarbershopTags.com";
    tag.providerWebsite = [NSURL URLWithString:@"https://provider.example"];
    tag.arranger = @"Jane Arranger";
    tag.arrangerWebsite = [NSURL URLWithString:@"https://arranger.example"];
    tag.yearArranged = 2020;
    tag.posted = [NSDate dateWithTimeIntervalSince1970:1600000000];
    tag.lastRefreshed = [NSDate dateWithTimeIntervalSince1970:1600000100];
    tag.sungBy = @"Harmony Quartet";
    tag.sungYear = 2019;
    tag.sungByWebsite = [NSURL URLWithString:@"https://quartet.example"];
    tag.recordingMethod = @"Studio Recording";
    tag.learningTrackQuartet = @"Learning Quartet";
    tag.learningTrackQuartetWebsite = [NSURL URLWithString:@"https://learning.example"];
    tag.teacher = @"Coach Doe";
    tag.teacherWebsite = [NSURL URLWithString:@"https://coach.example"];
    tag.teachingVideo = @"teach123";
    
    tag.allPartsTrackUri = [self remoteLocationWithURLString:@"https://example.com/allparts.mp3" type:@"mp3"];
    tag.tenorTrackUri = [self remoteLocationWithURLString:@"https://example.com/tenor.mp3" type:@"mp3"];
    tag.leadTrackUri = [self remoteLocationWithURLString:@"https://example.com/lead.mp3" type:@"mp3"];
    tag.baritoneTrackUri = [self remoteLocationWithURLString:@"https://example.com/bari.mp3" type:@"mp3"];
    tag.bassTrackUri = [self remoteLocationWithURLString:@"https://example.com/bass.mp3" type:@"mp3"];
    
    DPVideo *video = [[DPVideo alloc] init];
    video.youTubeCode = @"vid123";
    video.sungBy = @"Showcase Chorus";
    video.sungKey = @"G";
    video.posted = [NSDate dateWithTimeIntervalSince1970:1500000000];
    video.isMultitrack = YES;
    tag.videos = @[video];
    
    [self primeThumbnailCacheForCode:tag.teachingVideo];
    [self primeThumbnailCacheForCode:video.youTubeCode];
    
    return tag;
}

- (IMP)replaceClassMethod:(SEL)selector onClass:(Class)klass withBlock:(id)block {
    Class metaClass = object_getClass((id)klass);
    Method method = class_getInstanceMethod(metaClass, selector);
    IMP original = method_getImplementation(method);
    IMP replacement = imp_implementationWithBlock(block);
    method_setImplementation(method, replacement);
    return original;
}

- (IMP)replaceInstanceMethod:(SEL)selector onClass:(Class)klass withBlock:(id)block {
    Method method = class_getInstanceMethod(klass, selector);
    IMP original = method_getImplementation(method);
    IMP replacement = imp_implementationWithBlock(block);
    method_setImplementation(method, replacement);
    return original;
}

#pragma mark - Tests

- (void)testFavoritesLifecycle {
    XCTestExpectation *notificationExpectation = [self expectationWithDescription:@"userDataChanged"];
    __block NSInteger notificationCount = 0;
    id token = [[NSNotificationCenter defaultCenter] addObserverForName:kUserDataChangedNotification
                                                                object:nil
                                                                 queue:nil
                                                            usingBlock:^(NSNotification * _Nonnull note) {
        notificationCount += 1;
        [notificationExpectation fulfill];
    }];
    
    [DPAppDelegate setFavorites:@[@1, @2]];
    [self waitForExpectations:@[notificationExpectation] timeout:1.0];
    [[NSNotificationCenter defaultCenter] removeObserver:token];
    
    NSArray *favorites = [DPAppDelegate favorites];
    XCTAssertEqualObjects(favorites, (@[@1, @2]));
    XCTAssertTrue([DPAppDelegate containsFavorite:1]);
    
    [DPAppDelegate moveFavoriteAt:0 to:1];
    favorites = [DPAppDelegate favorites];
    XCTAssertEqualObjects(favorites, (@[@2, @1]));
    
    [DPAppDelegate removeFavorite:2];
    XCTAssertFalse([DPAppDelegate containsFavorite:2]);
    
    [DPAppDelegate addFavorite:3];
    favorites = [DPAppDelegate favorites];
    XCTAssertEqualObjects(favorites, (@[@1, @3]));
    
    [DPAppDelegate setTeachable:@[@11, @22]];
    XCTAssertTrue([DPAppDelegate containsTeachable:11]);
    [DPAppDelegate moveTeachableAt:0 to:1];
    NSArray *teachables = [DPAppDelegate teachable];
    XCTAssertEqualObjects(teachables, (@[@22, @11]));
    [DPAppDelegate removeTeachable:22];
    XCTAssertFalse([DPAppDelegate containsTeachable:22]);
}

- (void)testHomeViewControllerNavigationWithRandomTag {
    DPTag *tag = [self buildSampleTagWithIdentifier:321];
    [tag cache];
    [DPAppDelegate setFavorites:@[@(tag.tagId)]];
    
    DPHomeViewController *home = [[DPHomeViewController alloc] initWithStyle:UITableViewStyleGrouped];
    TestingNavigationController *nav = [[TestingNavigationController alloc] initWithRootViewController:home];
    UIWindow *window = [[UIWindow alloc] initWithFrame:CGRectMake(0, 0, 320, 640)];
    window.rootViewController = nav;
    [window makeKeyAndVisible];
    (void)home.view;
    
    UITableView *table = home.tableView;
    XCTAssertEqual([home numberOfSectionsInTableView:table], 3);
    XCTAssertEqual([home tableView:table numberOfRowsInSection:TMHomeFavoritesSection], 1);
    UITableViewCell *favoriteCell = [home tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:TMHomeFavoritesSection]];
    XCTAssertNotNil(favoriteCell);
    
    NSArray *items = [home navigationItems];
    XCTAssertTrue(items.count >= 3);
    
    NSUInteger randomIndex = [items indexOfObjectPassingTest:^BOOL(NSDictionary *obj, NSUInteger idx, BOOL *stop) {
        return [obj[@"title"] isEqualToString:@"Random Tag"];
    }];
    XCTAssertNotEqual(randomIndex, NSNotFound);

    void (^randomAction)(void) = items[randomIndex][@"action"];
    XCTAssertNotNil(randomAction);
    XCTAssertNotNil([home valueForKey:@"busyIndicator"]);

    [DPAppDelegate setFavorites:@[]];
    [home.tableView reloadData];
    [home.tableView layoutIfNeeded];
    window.hidden = YES;
    window.rootViewController = nil;
}

- (void)testSearchAndQueryControllersDisplayResults
{
    DPTag *tag = [self buildSampleTagWithIdentifier:777];
    [tag cache];
    
    // Test search controller setup and settings persistence
    DPSearchViewController *searchController = [[DPSearchViewController alloc] init];
    (void)searchController.view;
    
    UISearchBar *searchBar = [searchController valueForKey:@"searchBar"];
    UISegmentedControl *sortBy = [searchController valueForKey:@"sortBy"];
    UISegmentedControl *sheetMusic = [searchController valueForKey:@"sheetMusic"];
    UISegmentedControl *learningTracks = [searchController valueForKey:@"learningTracks"];
    UISegmentedControl *parts = [searchController valueForKey:@"parts"];
    UISegmentedControl *collection = [searchController valueForKey:@"collection"];
    
    XCTAssertNotNil(searchBar);
    XCTAssertNotNil(sortBy);
    XCTAssertNotNil(sheetMusic);
    XCTAssertNotNil(learningTracks);
    XCTAssertNotNil(parts);
    XCTAssertNotNil(collection);

    searchBar.text = @"sample";
    sortBy.selectedSegmentIndex = 3;
    sheetMusic.selectedSegmentIndex = 2;
    learningTracks.selectedSegmentIndex = 1;
    parts.selectedSegmentIndex = 2;
    collection.selectedSegmentIndex = 1;
    
    // Trigger settings save
    [searchController viewDidDisappear:NO];
    
    // Verify settings were persisted
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.sortBy"], 3);
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.sheetMusic"], 2);
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.learningTracks"], 1);
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.parts"], 2);
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.collection"], 1);
}

#pragma mark - KVO

- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary<NSKeyValueChangeKey,id> *)change
                       context:(void *)context {
    XCTestExpectation *expectation = (__bridge XCTestExpectation *)context;
    if ([keyPath isEqualToString:@"tag"]) {
        [expectation fulfill];
    }
}

@end

#import "DPTeachableTagsController.h"
#import "DPPitchPipeButton.h"
#import <QuickLook/QuickLook.h>


@interface DPTagQueryViewController (PolishTests)
- (void)refresh;
- (void)refreshViews;
- (void)tm_syncSelectionForSplit;
@end

// Capture only presentation. Production action and background-work paths still run.
#define TM_CAPTURE \
@property (nonatomic, strong) UIViewController *captured; \
@property (nonatomic, copy) NSString *errorMessage; \
@property (nonatomic, copy) void (^retry)(void);
#define TM_CAPTURE_IMPL \
- (void)presentViewController:(UIViewController *)controller animated:(BOOL)animated completion:(void (^)(void))completion { self.captured = controller; } \
- (void)tm_showError:(NSString *)message retry:(void (^)(void))retry { self.errorMessage = message; self.retry = retry; }
@interface TMTestHome : DPHomeViewController
TM_CAPTURE
@end
@implementation TMTestHome
TM_CAPTURE_IMPL
@end
@interface TMTestLocation : DPRemoteLocation
@end
@implementation TMTestLocation
- (NSString *)cacheKey { return [self.uri.lastPathComponent stringByAppendingPathExtension:self.type]; }
@end

@interface TMRatingTag : DPTag
@property BOOL failRating;
@property NSUInteger submittedRating;
@end
@implementation TMRatingTag
- (void)rate:(NSUInteger)rating {
    if (self.failRating) [NSException raise:@"offline" format:@"private diagnostic"];
    self.submittedRating = rating;
}
@end

@interface TMAlertHost : UIViewController
@property (nonatomic, strong) UIAlertController *captured;
@end
@implementation TMAlertHost
- (void)presentViewController:(UIViewController *)controller animated:(BOOL)animated completion:(void (^)(void))completion { self.captured = (id)controller; }
@end

@interface TMPolishRegressionTests : XCTestCase
@end
@implementation TMPolishRegressionTests

- (DPTag *)tag {
    DPTag *tag = [DPTag new];
    tag.tagId = 1809;
    tag.title = @"A long tag title that should wrap onto several lines at larger reading sizes";
    tag.alternativeTitle = @"Another long title for testing wrapping";
    tag.posted = [NSDate dateWithTimeIntervalSince1970:1600000000];
    tag.rating = 4.5;
    tag.parts = 4;
    tag.writtenKey = @"C";
    return tag;
}

- (void)waitUntil:(BOOL (^)(void))condition {
    // UI transactions and main-queue completions can be delayed on standard
    // hosted simulators, so the ceiling stays at ten seconds. Polling is now
    // millisecond-grained rather than the one-second predicate-timer cadence.
    TMAssertEventually(10, condition);
}

- (void)testAvailabilityUsesGreenWithoutChangingSpokenLabels {
    DPTagCell *cell = [[DPTagCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:nil];
    DPTag *tag = [self tag];
    cell.tagInstance = tag;
    UIImageView *sheet = [cell valueForKey:@"hasSheetMusic"];
    UIImageView *tracks = [cell valueForKey:@"hasLearningTracks"];
    XCTAssertEqualObjects(sheet.tintColor, [UIColor secondaryLabelColor]);
    XCTAssertEqualObjects(tracks.tintColor, [UIColor secondaryLabelColor]);
    XCTAssertTrue([cell.accessibilityLabel containsString:@"Sheet music unavailable"]);
    // DPTag caches its derived tracks; reuse the cell with a newly loaded tag.
    tag = [self tag];
    tag.sheetMusicUri = [DPRemoteLocation new];
    tag.tenorTrackUri = [DPRemoteLocation new];
    cell.tagInstance = tag;
    XCTAssertEqualObjects(sheet.image, [UIImage systemImageNamed:@"checkmark"]);
    XCTAssertEqualObjects(tracks.image, [UIImage systemImageNamed:@"checkmark"]);
    XCTAssertEqualObjects(sheet.tintColor, [UIColor systemGreenColor]);
    XCTAssertEqualObjects(tracks.tintColor, [UIColor systemGreenColor]);
    XCTAssertTrue([cell.accessibilityLabel containsString:@"Sheet music available"]);
    XCTAssertTrue([cell.accessibilityLabel containsString:@"Learning tracks available"]);
}

- (void)testRecoveryAlertHasRetryAndCancel {
    TMAlertHost *host = [TMAlertHost new];
    [host tm_showError:@"Check your connection and try again." retry:^{}];
    XCTAssertEqualObjects(host.captured.message, @"Check your connection and try again.");
    XCTAssertEqualObjects([host.captured.actions valueForKey:@"title"], (@[@"Retry", @"Cancel"]));
    [host tm_showError:@"No results." retry:nil];
    XCTAssertEqualObjects([host.captured.actions valueForKey:@"title"], (@[@"Cancel"]));
}

- (void)testQueryFailureRetryAndExhaustedRefresh {
    Method method = class_getClassMethod(DPTag.class, @selector(query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:));
    IMP original = method_getImplementation(method);
    __block BOOL fail = YES;
    __block int calls = 0;
    DPTag *tag = [self tag];
    IMP mock = imp_implementationWithBlock(^DPTagQueryResult *(id cls, NSString *query, int number, int start, NSNumber *parts, NSNumber *tracks, NSNumber *sheet, enum DPTagCollection collection, enum DPTagSortOptions sort) {
        calls++;
        if (fail) [NSException raise:@"offline" format:@"private diagnostic"];
        DPTagQueryResult *result = [DPTagQueryResult new];
        result.tags = @[tag]; result.available = 1; result.count = 1; result.start = 0;
        return result;
    });
    method_setImplementation(method, mock);
    @try {
        DPTagQueryViewController *query = [DPTagQueryViewController new];
        [query loadViewIfNeeded];
        [self waitUntil:^BOOL { return [[query valueForKey:@"failed"] boolValue] && !query.isLoading; }];
        UILabel *label = [query valueForKey:@"statusLabel"];
        XCTAssertTrue([label isKindOfClass:UILabel.class]);
        XCTAssertFalse([query.statusText containsString:@"private diagnostic"]);
        fail = NO;
        [[query valueForKey:@"retryButton"] sendActionsForControlEvents:UIControlEventTouchUpInside];
        [self waitUntil:^BOOL { return query.tags.count == 1 && !query.isLoading; }];
        XCTAssertFalse(query.hasMoreResults);
        [query refresh];
        [self waitUntil:^BOOL { return calls == 3 && !query.isLoading; }];
        XCTAssertEqual(query.tags.count, 1);
    } @finally { method_setImplementation(method, original); imp_removeBlock(mock); }
}

- (void)testRandomFailureAndEmptyResultRecover {
    Method method = class_getClassMethod(DPTag.class, @selector(query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:minimumRating:minimumDownloads:cache:fieldList:));
    IMP original = method_getImplementation(method);
    __block BOOL fail = YES;
    IMP mock = imp_implementationWithBlock(^DPTagQueryResult *(id cls, NSString *q, int n, int start, NSNumber *parts, NSNumber *tracks, NSNumber *sheet, enum DPTagCollection collection, enum DPTagSortOptions sort, NSNumber *rating, NSNumber *downloads, BOOL cache, NSString *fields) {
        XCTAssertFalse(cache); XCTAssertEqualObjects(fields, @"id");
        if (fail) [NSException raise:@"offline" format:@"private diagnostic"];
        DPTagQueryResult *result = [DPTagQueryResult new]; result.available = 0; return result;
    });
    method_setImplementation(method, mock);
    @try {
        TMTestHome *home = [TMTestHome new]; [home loadViewIfNeeded];
        NSDictionary *item = [[home navigationItems] filteredArrayUsingPredicate:[NSPredicate predicateWithFormat:@"title == 'Random Tag'"]].firstObject;
        ((void (^)(void))item[@"action"])();
        [self waitUntil:^BOOL { return home.retry != nil; }];
        XCTAssertEqual([[home valueForKey:@"busyIndicator"] busyCount], 0);
        home.errorMessage = nil; fail = NO; home.retry();
        [self waitUntil:^BOOL { return home.errorMessage != nil; }];
        XCTAssertTrue([home.errorMessage containsString:@"Settings"]);
        XCTAssertEqual([[home valueForKey:@"busyIndicator"] busyCount], 0);
    } @finally { method_setImplementation(method, original); imp_removeBlock(mock); }
}

- (void)testSelfSizingRowsAndSpokenAvailability {
    DPTagCell *cell = [[DPTagCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:nil];
    cell.tagInstance = [self tag];
    CGFloat normal = [cell.contentView systemLayoutSizeFittingSize:CGSizeMake(320, 0) withHorizontalFittingPriority:UILayoutPriorityRequired verticalFittingPriority:UILayoutPriorityFittingSizeLevel].height;
    UITraitCollection *large = [UITraitCollection traitCollectionWithPreferredContentSizeCategory:UIContentSizeCategoryAccessibilityExtraExtraExtraLarge];
    for (NSString *key in @[@"title", @"aka", @"details"]) {
        UILabel *label = [cell valueForKey:key];
        XCTAssertTrue(label.adjustsFontForContentSizeCategory);
        XCTAssertEqual(label.numberOfLines, 0);
        label.font = [UIFont preferredFontForTextStyle:UIFontTextStyleBody compatibleWithTraitCollection:large];
    }
    CGFloat enlarged = [cell.contentView systemLayoutSizeFittingSize:CGSizeMake(320, 0) withHorizontalFittingPriority:UILayoutPriorityRequired verticalFittingPriority:UILayoutPriorityFittingSizeLevel].height;
    XCTAssertGreaterThan(enlarged, normal);
    XCTAssertTrue([cell.accessibilityLabel containsString:@"Sheet music unavailable"]);
    XCTAssertTrue([cell.accessibilityLabel containsString:@"Learning tracks unavailable"]);
    DPHomeViewController *home = [DPHomeViewController new]; [home loadViewIfNeeded];
    XCTAssertEqual([home tableView:home.tableView heightForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:TMHomeFavoritesSection]], UITableViewAutomaticDimension);
}

- (void)testTeachableAlwaysDiscoverableAndEmptyGuidance {
    NSArray *saved = [DPAppDelegate teachable];
    @try {
        [DPAppDelegate setTeachable:@[]];
        DPHomeViewController *home = [DPHomeViewController new]; [home loadViewIfNeeded];
        UITableViewCell *teachableRow = [home tableView:home.tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:TMHomeListsSection]];
        XCTAssertEqualObjects(teachableRow.textLabel.text, @"Teachable Tags");
        XCTAssertEqualObjects([home tableView:home.tableView titleForHeaderInSection:TMHomeListsSection], @"Lists");
        DPTeachableTagsController *teachable = [DPTeachableTagsController new]; [teachable loadViewIfNeeded];
        XCTAssertEqual([teachable tableView:teachable.tableView numberOfRowsInSection:0], 0);
        UIView *header = teachable.tableView.tableHeaderView;
        XCTAssertNotNil(header);
        NSMutableArray<UIView *> *pending = [NSMutableArray array];
        if (header) [pending addObject:header];
        UIButton *browse = nil;
        BOOL hasGuidance = NO;
        while (pending.count) {
            UIView *view = pending.lastObject;
            [pending removeLastObject];
            [pending addObjectsFromArray:view.subviews];
            if ([view isKindOfClass:UIButton.class] && [view.accessibilityIdentifier isEqualToString:@"teachable.browse"]) browse = (UIButton *)view;
            if ([view isKindOfClass:UILabel.class] && [((UILabel *)view).text containsString:@"Mark as Teachable"]) hasGuidance = YES;
        }
        XCTAssertEqualObjects([browse titleForState:UIControlStateNormal], @"Browse Tags");
        XCTAssertTrue(hasGuidance);
        XCTAssertTrue(browse.enabled);
        XCTAssertTrue([[browse actionsForTarget:teachable forControlEvent:UIControlEventTouchUpInside] containsObject:@"browseTags"]);
        XCTAssertFalse(teachable.editButtonItem.enabled);
    } @finally { [DPAppDelegate setTeachable:saved]; }
}

- (void)testAccessibilityFilterMenuKeepsSegmentMapping {
    DPTagPageControllerBase *controller = [DPTagPageControllerBase new];
    UISegmentedControl *segments = [[UISegmentedControl alloc] initWithItems:@[@"Not Important", @"Yes", @"No"]];
    segments.selectedSegmentIndex = 0;
    UIView *filter = [controller makeFilterControl:segments label:@"Sheet Music"];
    filter.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryAccessibilityExtraExtraExtraLarge;
    [filter updateTraitsIfNeeded];
    [filter performSelector:NSSelectorFromString(@"updateFilter")];
    UIButton *menu = [filter valueForKey:@"menuButton"];
    XCTAssertTrue(segments.hidden);
    XCTAssertFalse(menu.hidden);
    XCTAssertEqual(menu.menu.children.count, 3);
    XCTAssertEqualObjects(menu.accessibilityValue, @"Not Important");
    NSInvocation *invocation = [NSInvocation invocationWithMethodSignature:[filter methodSignatureForSelector:NSSelectorFromString(@"selectIndex:")]];
    invocation.target = filter;
    invocation.selector = NSSelectorFromString(@"selectIndex:");
    NSInteger index = 2;
    [invocation setArgument:&index atIndex:2];
    [invocation invoke];
    XCTAssertEqual(segments.selectedSegmentIndex, 2);
    XCTAssertEqualObjects(menu.accessibilityValue, @"No");
    XCTAssertEqual(((UIAction *)menu.menu.children[2]).state, UIMenuElementStateOn);
    filter.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryLarge;
    [filter updateTraitsIfNeeded];
    [filter performSelector:NSSelectorFromString(@"updateFilter")];
    XCTAssertFalse(segments.hidden);
    XCTAssertTrue(menu.hidden);
    XCTAssertEqual(segments.selectedSegmentIndex, 2);
}

@end

// Held completions replace only the fetch boundary. The real controller, child pages,
// navigation, request identity, busy indicator and native window all run unchanged.
@interface TMPageLifecycleSpy : UIViewController
@property NSUInteger appearances;
@property NSUInteger disappearances;
@property NSUInteger attachments;
@property NSUInteger detachments;
@property (nonatomic, weak) UIViewController *lastParent;
@end
@implementation TMPageLifecycleSpy
- (void)viewDidAppear:(BOOL)animated { [super viewDidAppear:animated]; self.appearances++; }
- (void)viewDidDisappear:(BOOL)animated { [super viewDidDisappear:animated]; self.disappearances++; }
- (void)didMoveToParentViewController:(UIViewController *)parent {
    [super didMoveToParentViewController:parent];
    // UIKit 26 may repeat didMove during the first viewDidAppear. Count actual
    // parent changes, while appearance callbacks below still require exact counts.
    if (parent != self.lastParent) {
        if (parent) self.attachments++; else self.detachments++;
        self.lastParent = parent;
    }
}
@end

// Observe real navigation completion without retaining the controller under test.
@interface TMLayoutNavigationObserver : NSObject <UINavigationControllerDelegate>
@property (nonatomic, weak) UIViewController *shown;
@end
@implementation TMLayoutNavigationObserver
- (void)navigationController:(UINavigationController *)navigationController didShowViewController:(UIViewController *)viewController animated:(BOOL)animated {
    self.shown = viewController;
}
@end

@interface TMTabletTestSplit : UISplitViewController
@property BOOL simulateCollapse;
@end
@implementation TMTabletTestSplit
- (BOOL)isCollapsed { return self.simulateCollapse || super.isCollapsed; }
@end

@interface TMLoadingRegressionTests : XCTestCase
@property NSMutableArray<NSMutableDictionary *> *requests;
@property DPTag *immediateTag;
@property BOOL reduceMotion;
@property IMP originalFetch;
@property IMP fetchMock;
@property IMP originalMotion;
@property IMP motionMock;
@property UIWindow *window;
@property UIWindow *previousKeyWindow;
@end

@implementation TMLoadingRegressionTests
- (void)tearDown {
    [DPAppDelegate removeSharedBackground];
    self.window.hidden = YES;
    self.window.rootViewController = nil;
    self.window = nil;
    [self.previousKeyWindow makeKeyWindow];
    [super tearDown];
}
- (DPTag *)tag:(int)identifier {
    DPTag *tag = [DPTag new];
    tag.tagId = identifier;
    tag.title = @"Lost";
    tag.alternativeTitle = @"In Your Eyes";
    tag.parts = 4;
    tag.tagType = @"Barbershop";
    tag.writtenKey = @"Minor:G";
    tag.rating = 3.49;
    tag.lyrics = @"There I go lost in your eyes.";
    tag.posted = [NSDate dateWithTimeIntervalSince1970:1600000000];
    return tag;
}
- (void)finish:(NSUInteger)index tag:(DPTag *)tag {
    void (^completion)(DPTag *) = self.requests[index][@"completion"];
    self.requests[index][@"completion"] = NSNull.null;
    completion(tag);
}
// Wait for actual UIKit appearance/layout state. Shared CI runners can take
// longer to service the main queue; the predicates still require completion.
- (void)drainUIKit {
    XCTestExpectation *turn = [self expectationWithDescription:@"UIKit transaction turn"];
    dispatch_async(dispatch_get_main_queue(), ^{
        dispatch_async(dispatch_get_main_queue(), ^{ [turn fulfill]; });
    });
    [self waitForExpectations:@[turn] timeout:5];
}
- (void)layout {
    [self.window updateTraitsIfNeeded];
    [self.window setNeedsLayout];
    [self.window layoutIfNeeded];
    [CATransaction flush];
}
- (UINavigationController *)attach:(TMPageViewController *)detail size:(CGSize)size style:(UIUserInterfaceStyle)style large:(BOOL)large {
    if (!self.window) {
        for (UIWindow *candidate in UIApplication.sharedApplication.windows) {
            if (candidate.isKeyWindow) self.previousKeyWindow = candidate;
        }
    }
    self.window.hidden = YES;
    self.window.rootViewController = nil;
    self.window = [[UIWindow alloc] initWithFrame:(CGRect){CGPointZero, size}];
    self.window.overrideUserInterfaceStyle = style;
    self.window.traitOverrides.preferredContentSizeCategory = large ? UIContentSizeCategoryAccessibilityExtraExtraExtraLarge : UIContentSizeCategoryLarge;
    self.window.tintColor = UIColor.systemBlueColor;
    UIViewController *previous = [UIViewController new];
    previous.title = @"Tags";
    UINavigationController *navigation = [[UINavigationController alloc] initWithRootViewController:previous];
    // Match the app's charcoal navigation, without launching accounts, sync or networking.
    UINavigationBarAppearance *appearance = [UINavigationBarAppearance new];
    [appearance configureWithOpaqueBackground];
    appearance.backgroundColor = [UIColor colorWithWhite:55.0 / 255.0 alpha:1];
    appearance.titleTextAttributes = @{NSForegroundColorAttributeName: UIColor.whiteColor};
    navigation.navigationBar.standardAppearance = appearance;
    navigation.navigationBar.scrollEdgeAppearance = appearance;
    navigation.navigationBar.compactAppearance = appearance;
    navigation.navigationBar.compactScrollEdgeAppearance = appearance;
    navigation.navigationBar.tintColor = UIColor.whiteColor;
    navigation.navigationBar.overrideUserInterfaceStyle = UIUserInterfaceStyleDark;
    navigation.navigationBar.translucent = NO;
    TMLayoutNavigationObserver *observer = [TMLayoutNavigationObserver new];
    navigation.delegate = observer;
    self.window.rootViewController = navigation;
    [self.window makeKeyAndVisible];
    TMAssertEventually(10, ^BOOL{ return observer.shown == previous; });
    [navigation pushViewController:detail animated:NO];
    __weak TMPageViewController *weakDetail = detail;
    TMAssertEventually(10, ^BOOL{
        return observer.shown == weakDetail && [[weakDetail valueForKey:@"appeared"] boolValue] && !weakDetail.transitionCoordinator;
    });
    navigation.delegate = nil;
    [self layout];
    return navigation;
}
- (void)checkTabDistribution:(TMPageViewController *)controller {
    TMAssertEventually(3, ^BOOL{
        [self layout];
        return [controller.pageTabController.selectedViewController.view isDescendantOfView:controller.rootView] && !controller.pageTabController.transitionCoordinator;
    });
    UITabBar *bar = controller.tabBar;
    XCTAssertTrue([bar isKindOfClass:UITabBar.class]);
    XCTAssertEqual(bar, controller.pageTabController.tabBar);
    XCTAssertEqual(bar.delegate, controller.pageTabController);
    XCTAssertFalse(bar.hidden);
    XCTAssertEqual(bar.items.count, 4);
    XCTAssertNotNil(bar.window);
    CGRect rect = [bar convertRect:bar.bounds toView:controller.view];
    CGRect safe = controller.view.safeAreaLayoutGuide.layoutFrame;
    XCTAssertGreaterThan(CGRectGetMinY(rect), CGRectGetMidY(safe));
    XCTAssertGreaterThanOrEqual(CGRectGetMinX(rect), CGRectGetMinX(controller.view.bounds) - 1);
    XCTAssertLessThanOrEqual(CGRectGetMaxX(rect), CGRectGetMaxX(controller.view.bounds) + 1);
    UIViewController *page = controller.pageTabController.selectedViewController;
    XCTAssertTrue([page.view isDescendantOfView:controller.rootView]);
    CGRect content = [page.view convertRect:page.view.safeAreaLayoutGuide.layoutFrame toView:controller.view];
    XCTAssertLessThanOrEqual(CGRectGetMaxY(content), CGRectGetMinY(rect) + 1);
    XCTAssertEqual(page.traitCollection.horizontalSizeClass, controller.traitCollection.horizontalSizeClass);
    for (NSUInteger i = 0; i < bar.items.count; i++) {
        XCTAssertEqual(bar.items[i], controller.viewControllers[i].tabBarItem);
        XCTAssertEqualObjects(bar.items[i].accessibilityIdentifier, [@"page-" stringByAppendingString:bar.items[i].title]);
    }
    NSLog(@"TM_NATIVE_TABS %@ bar=%@ content=%@ hostTrait=%ld pageTrait=%ld", NSStringFromClass(controller.class), NSStringFromCGRect(rect), NSStringFromCGRect(content), (long)controller.traitCollection.horizontalSizeClass, (long)page.traitCollection.horizontalSizeClass);
}
- (void)capture:(NSString *)name {
    [self layout];
    CGSize size = self.window.bounds.size;
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat];
    format.scale = MIN(1, 800 / MAX(size.width, size.height));
    UIGraphicsImageRenderer *renderer = [[UIGraphicsImageRenderer alloc] initWithSize:size format:format];
    UIImage *image = [renderer imageWithActions:^(UIGraphicsImageRendererContext *context) {
        [self.window drawViewHierarchyInRect:self.window.bounds afterScreenUpdates:YES];
    }];
    NSData *jpeg = UIImageJPEGRepresentation(image, 0.85);
    NSString *directory = [NSTemporaryDirectory() stringByAppendingPathComponent:@"tm-loading-captures"];
    [NSFileManager.defaultManager createDirectoryAtPath:directory withIntermediateDirectories:YES attributes:nil error:nil];
    XCTAssertTrue([jpeg writeToFile:[directory stringByAppendingPathComponent:[name stringByAppendingPathExtension:@"jpg"]] atomically:YES]);
    XCTAttachment *attachment = [XCTAttachment attachmentWithData:jpeg uniformTypeIdentifier:@"public.jpeg"];
    attachment.name = name;
    attachment.lifetime = XCTAttachmentLifetimeKeepAlways;
    [self addAttachment:attachment];
}
- (void)testPageContainmentAndAppearanceFollowOnlySelectedChild {
    TMPageViewController *controller = [TMPageViewController new];
    NSMutableArray<TMPageLifecycleSpy *> *pages = [NSMutableArray array];
    for (NSString *title in @[@"One", @"Two", @"Three", @"Four"]) {
        TMPageLifecycleSpy *page = [TMPageLifecycleSpy new];
        page.tabBarItem = [[UITabBarItem alloc] initWithTitle:title image:nil tag:pages.count];
        [pages addObject:page];
    }
    controller.viewControllers = pages;
    UINavigationController *nav = [self attach:controller size:UIScreen.mainScreen.bounds.size style:UIUserInterfaceStyleLight large:NO];
    XCTAssertEqual(pages[0].appearances, 1);
    XCTAssertEqual(pages[0].attachments, 1);
    XCTAssertEqual(pages[1].appearances, 0);
    controller.selectedIndex = 1;
    TMAssertEventually(3, ^BOOL{ return pages[1].appearances == 1 && pages[0].disappearances == 1; });
    XCTAssertEqual(pages[0].disappearances, 1);
    XCTAssertEqual(pages[0].detachments, 0);
    XCTAssertEqual(pages[0].parentViewController, controller.pageTabController);
    XCTAssertEqual(pages[1].appearances, 1);
    XCTAssertEqual(pages[1].attachments, 1);
    controller.selectedIndex = 1;
    XCTAssertEqual(pages[1].appearances, 1);
    TMPageLifecycleSpy *cover = [TMPageLifecycleSpy new];
    [nav pushViewController:cover animated:NO];
    TMAssertEventually(3, ^BOOL{ return cover.appearances == 1; });
    XCTAssertEqual(pages[1].disappearances, 1);
    [nav popViewControllerAnimated:NO];
    TMAssertEventually(3, ^BOOL{ return pages[1].appearances == 2; });
    XCTAssertEqual(pages[1].appearances, 2);
    XCTAssertEqual(pages[0].appearances, 1);
    // Removing the selected destination must show a surviving native page.
    controller.viewControllers = @[pages[0], pages[2], pages[3]];
    TMAssertEventually(3, ^BOOL{ return !pages[1].view.window && pages[1].disappearances == 2; });
    XCTAssertEqual(pages[1].disappearances, 2);
    UIViewController *survivor = controller.pageTabController.selectedViewController;
    XCTAssertTrue([controller.viewControllers containsObject:survivor]);
    XCTAssertTrue([survivor.view isDescendantOfView:controller.rootView]);
    XCTAssertNil(pages[1].view.window);
    XCTAssertNil(pages[1].parentViewController);
    XCTAssertEqual(controller.tabBar.items.count, 3);
}

- (void)exercisePageSelection:(TMPageViewController *)controller {
    // UIKit animates each tab selection for roughly two thirds of a second and this
    // loop waits for four of those transitions to fully complete. Run CoreAnimation
    // on a compressed clock rather than skipping the animation: the same transition
    // runs, with the same transition coordinator and the same completion callbacks,
    // so every containment assertion below still observes a genuinely finished
    // transition. Only the wall clock shrinks.
    float shippingSpeed = self.window.layer.speed;
    self.window.layer.speed = 12;
    for (NSUInteger i = 0; i < 4; i++) {
        controller.selectedIndex = i;
        TMAssertEventually(3, ^BOOL{
            [self layout];
            if (![controller.viewControllers[i].view isDescendantOfView:controller.rootView]) return NO;
            for (UIViewController *other in controller.viewControllers) {
                if (other != controller.viewControllers[i] && other.viewIfLoaded.window && !other.viewIfLoaded.hidden) return NO;
            }
            return YES;
        });
        XCTAssertEqual(controller.selectedIndex, i);
        XCTAssertEqual(controller.tabBar.selectedItem, controller.viewControllers[i].tabBarItem);
        XCTAssertEqual(controller.pageTabController.selectedViewController, controller.viewControllers[i]);
        XCTAssertEqual(controller.viewControllers[i].parentViewController, controller.pageTabController);
        XCTAssertTrue([controller.viewControllers[i].view isDescendantOfView:controller.rootView]);
        XCTAssertFalse(controller.viewControllers[i].view.hidden);
        for (UIViewController *other in controller.viewControllers) {
            if (other != controller.viewControllers[i]) XCTAssertTrue(!other.viewIfLoaded.window || other.viewIfLoaded.hidden);
        }
    }
    controller.viewControllers = [controller.viewControllers copy];
    XCTAssertEqual(controller.selectedIndex, 3);
    controller.selectedIndex = 1;
    XCTAssertEqual(controller.tabBar.selectedItem, controller.tabBar.items[1]);
    self.window.layer.speed = shippingSpeed;
}
- (TMTabletTestSplit *)tabletSplitWithList:(UIViewController *)list {
    for (UIWindow *candidate in UIApplication.sharedApplication.windows) {
        if (candidate.isKeyWindow) self.previousKeyWindow = candidate;
    }
    self.window = [[UIWindow alloc] initWithFrame:CGRectMake(0, 0, 1194, 834)];
    self.window.traitOverrides.horizontalSizeClass = UIUserInterfaceSizeClassRegular;
    TMTabletTestSplit *split = [[TMTabletTestSplit alloc] initWithStyle:UISplitViewControllerStyleDoubleColumn];
    split.preferredDisplayMode = UISplitViewControllerDisplayModeOneBesideSecondary;
    split.preferredSplitBehavior = UISplitViewControllerSplitBehaviorTile;
    split.minimumPrimaryColumnWidth = 320;
    split.maximumPrimaryColumnWidth = 400;
    split.preferredPrimaryColumnWidthFraction = 0.36;
    split.delegate = (id<UISplitViewControllerDelegate>)UIApplication.sharedApplication.delegate;
    [DPAppDelegate installSharedBackgroundIn:split.view]; // as the iPad launch does, before any column loads
    [split setViewController:[[UINavigationController alloc] initWithRootViewController:list] forColumn:UISplitViewControllerColumnPrimary];
    UIViewController *placeholder = [NSClassFromString(@"TMTagPlaceholderController") new];
    [split setViewController:[[UINavigationController alloc] initWithRootViewController:placeholder] forColumn:UISplitViewControllerColumnSecondary];
    self.window.rootViewController = split;
    [self.window makeKeyAndVisible];
    [self drainUIKit]; [self layout];
    XCTAssertFalse(split.isCollapsed);
    return split;
}

- (DPTagViewController *)tabletDetail:(UISplitViewController *)split {
    UINavigationController *nav = (id)[split viewControllerForColumn:UISplitViewControllerColumnSecondary];
    XCTAssertTrue([nav.viewControllers.firstObject isKindOfClass:DPTagViewController.class]);
    return (id)nav.viewControllers.firstObject;
}

- (void)testTabletSavedListsReselectAfterReloadAndClearWhenCurrentTagIsRemoved {
    NSArray *favorites = [DPAppDelegate favorites];
    NSArray *teachable = [DPAppDelegate teachable];
    Method cache = class_getClassMethod(DPTag.class, @selector(loadFromCache:));
    IMP original = method_getImplementation(cache);
    NSDictionary *tags = @{@42: [self tag:42], @99: [self tag:99]};
    IMP mock = imp_implementationWithBlock(^DPTag *(id cls, int identifier) { return tags[@(identifier)]; });
    method_setImplementation(cache, mock);
    @try {
        [DPAppDelegate setFavorites:@[@42, @99]];
        [DPAppDelegate setTeachable:@[@42, @99]];
        DPHomeViewController *home = [DPHomeViewController new];
        TMTabletTestSplit *split = [self tabletSplitWithList:home];
        DPTeachableTagsController *teachableList = [DPTeachableTagsController new];
        for (UITableViewController<TMTagListSource> *list in @[(id)home, (id)teachableList]) {
            if (list != home) [home.navigationController pushViewController:list animated:NO];
            [list loadViewIfNeeded];
            [DPAppDelegate showTagWithId:42 from:list];
            XCTAssertEqualObjects([list tm_listedTagIds], (@[@42, @99]));
            [list.tableView reloadData];
            [list performSelector:@selector(tm_syncSelectionForSplit)];
            NSIndexPath *selected = [NSIndexPath indexPathForRow:0 inSection:list == home ? TMHomeFavoritesSection : 0];
            XCTAssertEqualObjects(list.tableView.indexPathForSelectedRow, selected);
            XCTAssertFalse(list.clearsSelectionOnViewWillAppear);
            [[self tabletDetail:split] stepToNextTag];
            XCTAssertEqual(list.tableView.indexPathForSelectedRow.row, 1);
            if (list == home) [DPAppDelegate setFavorites:@[@42]];
            else [DPAppDelegate setTeachable:@[@42]];
            [self drainUIKit];
            XCTAssertNil(list.tableView.indexPathForSelectedRow);
            XCTAssertFalse([[self tabletDetail:split] canPerformAction:@selector(stepToNextTag) withSender:nil]);
        }
    } @finally {
        [DPAppDelegate setFavorites:favorites];
        [DPAppDelegate setTeachable:teachable];
        method_setImplementation(cache, original);
        imp_removeBlock(mock);
    }
}

- (NSArray<UIView *> *)logoBackgroundsIn:(UIView *)view {
    NSMutableArray *found = [NSMutableArray array];
    if ([view isKindOfClass:TMLogoBackgroundView.class]) [found addObject:view];
    for (UIView *child in view.subviews) [found addObjectsFromArray:[self logoBackgroundsIn:child]];
    return found;
}

- (CGFloat)luminance:(UIColor *)color on:(UIColor *)background traits:(UITraitCollection *)traits {
    CGFloat r, g, b, a, br, bg, bb, ba;
    [[color resolvedColorWithTraitCollection:traits] getRed:&r green:&g blue:&b alpha:&a];
    [[background resolvedColorWithTraitCollection:traits] getRed:&br green:&bg blue:&bb alpha:&ba];
    CGFloat components[] = {r * a + br * (1 - a), g * a + bg * (1 - a), b * a + bb * (1 - a)};
    for (NSUInteger i = 0; i < 3; i++) components[i] = components[i] <= 0.04045 ? components[i] / 12.92 : pow((components[i] + 0.055) / 1.055, 2.4);
    return components[0] * 0.2126 + components[1] * 0.7152 + components[2] * 0.0722;
}
@end

// Exercise the real tab/scroll/grid hierarchy without catalog or media requests.
@interface TMSummaryLayoutRegressionTests : XCTestCase
@end
@implementation TMSummaryLayoutRegressionTests

- (void)settleLayout:(UIView *)view {
    [view updateTraitsIfNeeded];
    [view setNeedsLayout];
    [view layoutIfNeeded];
    [[NSRunLoop mainRunLoop] runUntilDate:[NSDate dateWithTimeIntervalSinceNow:0.05]];
    [view layoutIfNeeded];
}

@end

#import "TMBarberPoleLoadingView.h"
#import <AVKit/AVKit.h>

@interface TMHeldRatingTag : TMRatingTag
@property (nonatomic, strong) dispatch_semaphore_t gate;
@end
@implementation TMHeldRatingTag
- (void)rate:(NSUInteger)rating {
    if (self.gate) dispatch_semaphore_wait(self.gate, dispatch_time(DISPATCH_TIME_NOW, 20 * NSEC_PER_SEC));
    [super rate:rating];
}
@end

// A hosted XCTest app deliberately skips Firebase setup. Model signed-out status
// at the account boundary rather than bootstrapping authentication for layout tests.
@interface TMLayoutSettings : DPSettingsController
@end
@implementation TMLayoutSettings
- (BOOL)isSignedIn { return NO; }
@end

// Local layout fixtures use production controllers without submitting queries or changing lists.
@interface TMLayoutRegressionTests : XCTestCase
@property UIWindow *window;
@property UIWindow *previousWindow;
@end
@implementation TMLayoutRegressionTests
- (void)tearDown {
    self.window.hidden = YES;
    self.window.rootViewController = nil;
    [self.previousWindow makeKeyWindow];
    [super tearDown];
}
- (void)settle {
    [self.window updateTraitsIfNeeded];
    [self.window setNeedsLayout];
    [self.window layoutIfNeeded];
    XCTestExpectation *transaction = [[XCTestExpectation alloc] initWithDescription:@"Layout transaction committed"];
    [CATransaction begin];
    [CATransaction setCompletionBlock:^{ [transaction fulfill]; }];
    [self.window layoutIfNeeded];
    [CATransaction commit];
    [CATransaction flush];
    // Generous ceiling: a hosted runner under load can take seconds to land the
    // commit, and this only bounds how long a stuck transaction can hang a test.
    XCTAssertEqual([XCTWaiter waitForExpectations:@[transaction] timeout:15], XCTWaiterResultCompleted);
    [self.window layoutIfNeeded];
}
// A table realizes and sizes a row over several passes after a width, edit-mode or
// data change, so a scroll issued against the interim geometry lands short and the
// row the caller wants is left outside the viewport. Re-issue the scroll until the
// row is realized and its edge is inside the viewport, or until scrolling a settled
// table twice produces identical geometry, and hand back the realized cell. The
// caller's assertions are unchanged: a row that never comes to rest still fails.
- (UITableViewCell *)scrollRow:(NSIndexPath *)path in:(UITableView *)table position:(UITableViewScrollPosition)position {
    __block UITableViewCell *cell = nil;
    __block NSString *previous = nil;
    TMSpinUntil(3, ^BOOL{
        [table scrollToRowAtIndexPath:path atScrollPosition:position animated:NO];
        [self settle];
        cell = [table cellForRowAtIndexPath:path];
        if (!cell || cell.layer.needsLayout) { previous = nil; return NO; }
        CGRect row = [table rectForRowAtIndexPath:path];
        CGFloat edgeY = position == UITableViewScrollPositionBottom ? CGRectGetMaxY(row) - 1
                      : position == UITableViewScrollPositionTop ? CGRectGetMinY(row) : CGRectGetMidY(row);
        if (CGRectContainsRect(table.bounds, CGRectMake(CGRectGetMidX(row), edgeY, 1, 1))) return YES;
        // A row taller than the viewport can never be contained; stop once scrolling
        // an already-settled table changes nothing rather than burning the ceiling.
        NSString *state = [NSString stringWithFormat:@"%@|%@|%@", NSStringFromCGRect(row),
                           NSStringFromCGRect(table.bounds), NSStringFromCGSize(table.contentSize)];
        BOOL settled = [state isEqualToString:previous];
        previous = state;
        return settled;
    });
    return cell;
}
- (void)mount:(UIViewController *)controller width:(CGFloat)width category:(UIContentSizeCategory)category {
    if (!self.previousWindow) for (UIWindow *candidate in UIApplication.sharedApplication.windows) if (candidate.isKeyWindow) self.previousWindow = candidate;
    self.window.hidden = YES;
    self.window.rootViewController = nil;
    self.window = [[UIWindow alloc] initWithFrame:CGRectMake(0, 0, width, 852)];
    self.window.traitOverrides.preferredContentSizeCategory = category;
    self.window.rootViewController = controller;
    self.window.backgroundColor = UIColor.systemBackgroundColor;
    self.window.tintColor = UIColor.systemBlueColor;
    [self.window makeKeyAndVisible];
    [self settle];
}
- (void)testNarrowPartsUsesMenuBeforeTargetsShrink {
    DPSearchViewController *search = [DPSearchViewController new];
    [self mount:search width:320 category:UIContentSizeCategoryLarge];
    UITableView *table = [search valueForKey:@"tableView"];
    [table scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:3 inSection:0] atScrollPosition:UITableViewScrollPositionMiddle animated:NO];
    [self settle];
    UIView *wrapper = [search valueForKey:@"filterControls"][3];
    UISegmentedControl *parts = [search valueForKey:@"parts"];
    // The wrapper reaches its real width only once the scrolled-to row has been laid
    // out, and an unlaid-out wrapper is trivially narrower than 308pt.
    TMSpinUntil(3, ^BOOL{ return wrapper.bounds.size.width > 0 && !wrapper.layer.needsLayout; });
    NSLog(@"TM_LAYOUT_PROBE Parts wrapper=%.1f segments=%.1f count=%ld hidden=%d", wrapper.bounds.size.width, parts.bounds.size.width, (long)parts.numberOfSegments, parts.hidden);
    XCTAssertLessThan(wrapper.bounds.size.width, 308);
    XCTAssertTrue(parts.hidden, @"Seven 44pt targets cannot fit below 308pt");
    XCTAssertFalse([[wrapper valueForKey:@"menuButton"] isHidden]);
}
@end

@implementation TMLayoutRegressionTests (FitMatrix)
- (DPTag *)layoutTag {
    DPTag *tag = [DPTag new];
    tag.tagId = 1809; tag.title = @"Lost with a long title for a group of singers";
    tag.parts = 4; tag.tagType = @"Barbershop"; tag.writtenKey = @"Minor:G"; tag.rating = 3.49;
    tag.lastRefreshed = [NSDate dateWithTimeIntervalSince1970:1600000000]; tag.posted = tag.lastRefreshed;
    tag.downloadCount = 12345; tag.provider = @"Alexandria Montgomery and the International Harmony Society";
    tag.arranger = @"Alexandria Montgomery and the International Harmony Society";
    tag.sungBy = @"The International Harmony Society Quartet"; tag.yearArranged = 2020; tag.sungYear = 2021;
    tag.providerWebsite = [NSURL URLWithString:@"https://example.invalid/provider"];
    tag.sungByWebsite = [NSURL URLWithString:@"https://example.invalid/quartet"];
    tag.lyrics = @"And I will wait to face the skies,\never roaming in your eyes.\nThere I go lost in your eyes.";
    tag.notes = @"Sing the phrase together, then hold the last chord. Listen to the lead and balance the other parts. Repeat quietly before returning to full voice.";
    tag.sheetMusicUri = [DPRemoteLocation new];
    return tag;
}
- (void)assertWholeLabel:(UILabel *)label {
    XCTAssertGreaterThan(label.bounds.size.width, 0, @"%@", label.text);
    XCTAssertGreaterThanOrEqual(label.bounds.size.height + 1, [label sizeThatFits:CGSizeMake(label.bounds.size.width, CGFLOAT_MAX)].height, @"%@", label.text);
}
- (void)captureLayout:(NSString *)name {
    [self settle];
    CGSize size = self.window.bounds.size;
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat];
    format.scale = MIN(1, 800 / MAX(size.width, size.height));
    UIImage *image = [[[UIGraphicsImageRenderer alloc] initWithSize:size format:format] imageWithActions:^(UIGraphicsImageRendererContext *context) {
        [self.window drawViewHierarchyInRect:self.window.bounds afterScreenUpdates:YES];
    }];
    NSString *device = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? @"ipad" : @"phone";
    NSString *file = [NSString stringWithFormat:@"tagmaster-layout-after-ios-%@-%@.jpg", device, name];
    XCTAssertTrue([UIImageJPEGRepresentation(image, .88) writeToFile:[NSTemporaryDirectory() stringByAppendingPathComponent:file] atomically:YES]);
}
- (void)testEveryFilterFitsAndKeepsOptionsAcrossResize {
    NSDictionary *defaults = NSUserDefaults.standardUserDefaults.dictionaryRepresentation;
    for (UIViewController *controller in @[[DPSearchViewController new], [TMLayoutSettings new]]) {
        [self mount:controller width:393 category:UIContentSizeCategoryLarge];
        NSArray *wrappers = [controller valueForKey:@"filterControls"];
        XCTAssertEqual(wrappers.count, [controller isKindOfClass:DPSearchViewController.class] ? 5 : 4);
        // Exercise each real consumer at its wrapper boundary, independent of offscreen cell reuse.
        for (UIView *wrapper in wrappers) {
            UISegmentedControl *control = [wrapper valueForKey:@"control"];
            UIButton *menu = [wrapper valueForKey:@"menuButton"];
            NSInteger original = control.selectedSegmentIndex;
            NSArray *titles = [menu.menu.children valueForKey:@"title"];
            for (UIContentSizeCategory category in @[UIContentSizeCategoryLarge, UIContentSizeCategoryAccessibilityExtraExtraExtraLarge]) {
                wrapper.traitOverrides.preferredContentSizeCategory = category; [wrapper updateTraitsIfNeeded];
                for (NSNumber *width in @[@256, @329, @700, @256, @700]) {
                    wrapper.bounds = CGRectMake(0, 0, width.doubleValue, 44);
                    [wrapper performSelector:NSSelectorFromString(@"updateFilter")];
                    BOOL hidden = control.hidden;
                    for (NSInteger cycle = 0; cycle < 3; cycle++) [wrapper performSelector:NSSelectorFromString(@"updatePresentation")];
                    XCTAssertEqual(control.hidden, hidden);
                    XCTAssertEqual(menu.hidden, !hidden);
                    if (!hidden) for (NSInteger index = 0; index < control.numberOfSegments; index++) XCTAssertGreaterThanOrEqual([control widthForSegmentAtIndex:index], 44);
                    if ([category isEqualToString:UIContentSizeCategoryAccessibilityExtraExtraExtraLarge]) XCTAssertTrue(hidden);
                    XCTAssertEqual(control.selectedSegmentIndex, original);
                    XCTAssertEqualObjects([menu.menu.children valueForKey:@"title"], titles);
                }
            }
        }
    }
    for (NSString *key in defaults) if ([key hasPrefix:@"search."] || [key hasPrefix:@"random."]) XCTAssertEqualObjects([NSUserDefaults.standardUserDefaults objectForKey:key], defaults[key]);
}
@end

@implementation TMLayoutRegressionTests (DetailComposition)
- (void)captureComposition:(NSString *)name {
    // Native tab selection is animated. Capture only after its real transition settles.
    XCTestExpectation *frame = [self expectationWithDescription:@"native tab presentation settled"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, .4 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{ [frame fulfill]; });
    [self waitForExpectations:@[frame] timeout:2];
    [self settle];
    CGSize size = self.window.bounds.size;
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat];
    format.scale = MIN(1, 800 / MAX(size.width, size.height));
    UIImage *image = [[[UIGraphicsImageRenderer alloc] initWithSize:size format:format] imageWithActions:^(UIGraphicsImageRendererContext *context) {
        [self.window drawViewHierarchyInRect:self.window.bounds afterScreenUpdates:YES];
    }];
    NSString *device = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? @"ipad" : @"phone";
    NSString *file = [NSString stringWithFormat:@"tagmaster-detail-composition-ios-%@-%@.jpg", device, name];
    XCTAssertTrue([UIImageJPEGRepresentation(image, .88) writeToFile:[NSTemporaryDirectory() stringByAppendingPathComponent:file] atomically:YES]);
}
@end

@implementation TMLayoutRegressionTests (DetailCompositionAdaptive)
@end

@implementation TMLayoutRegressionTests (ScreenCoverage)
- (void)checkLabelsIn:(UIView *)view {
    if (view.hidden) return;
    if ([view isKindOfClass:UILabel.class] && ((UILabel *)view).text.length > 0) [self assertWholeLabel:(id)view];
    for (UIView *child in view.subviews) [self checkLabelsIn:child];
}
- (void)testHomeAndTeachableZeroOneManyReorderGeometry {
    Method favorites = class_getClassMethod(DPAppDelegate.class, @selector(favorites));
    Method teachable = class_getClassMethod(DPAppDelegate.class, @selector(teachable));
    Method cache = class_getClassMethod(DPTag.class, @selector(loadFromCache:));
    IMP oldFavorites = method_getImplementation(favorites), oldTeachable = method_getImplementation(teachable), oldCache = method_getImplementation(cache);
    __block NSArray *ids = @[];
    DPTag *tag = [self layoutTag];
    IMP list = imp_implementationWithBlock(^NSArray *(id owner) { return ids; });
    IMP cached = imp_implementationWithBlock(^DPTag *(id owner, int identifier) { return tag; });
    method_setImplementation(favorites, list); method_setImplementation(teachable, list); method_setImplementation(cache, cached);
    @try {
        for (NSNumber *count in @[@0, @1, @30]) {
            NSMutableArray *items = [NSMutableArray array];
            for (NSInteger i = 0; i < count.integerValue; i++) [items addObject:@(1809 + i)];
            ids = items;
            DPHomeViewController *home = [DPHomeViewController new];
            [self mount:home width:393 category:UIContentSizeCategoryLarge];
            XCTAssertEqual([home.tableView numberOfRowsInSection:TMHomeFavoritesSection], count.integerValue);
            if (count.integerValue > 0) {
                NSIndexPath *last = [NSIndexPath indexPathForRow:count.integerValue - 1 inSection:TMHomeFavoritesSection];
                UITableViewCell *cell = [self scrollRow:last in:home.tableView position:UITableViewScrollPositionMiddle];
                XCTAssertNotNil(cell); XCTAssertGreaterThan(cell.bounds.size.height, 44);
                [self checkLabelsIn:cell.contentView];
            }
            if (count.integerValue == 30) [self captureLayout:@"home-many-default"];
            DPTeachableTagsController *teachableController = [DPTeachableTagsController new];
            [self mount:teachableController width:320 category:UIContentSizeCategoryAccessibilityExtraExtraExtraLarge];
            XCTAssertEqual([teachableController.tableView numberOfRowsInSection:0], count.integerValue);
            if (count.integerValue == 0) {
                UIView *header = teachableController.tableView.tableHeaderView;
                XCTAssertNotNil(header);
                UIStackView *stack = (id)header.subviews.firstObject;
                UIButton *browse = (id)stack.arrangedSubviews.lastObject;
                XCTAssertEqualObjects(browse.currentTitle, @"Browse Tags");
                for (NSNumber *width in @[@320, @834, @320]) {
                    self.window.frame = CGRectMake(0, 0, width.doubleValue, 568); [self settle];
                    UITableView *emptyTable = teachableController.tableView;
                    UIWindow *window = self.window;
                    // The empty-state header restacks over several passes after a width
                    // change, so a scroll issued against the interim rect lands short.
                    // Wait for the reachable state the assertions below read.
                    TMSpinUntil(3, ^BOOL{
                        [emptyTable scrollRectToVisible:[browse convertRect:browse.bounds toView:emptyTable] animated:NO];
                        [self settle];
                        CGRect reachable = UIEdgeInsetsInsetRect(emptyTable.bounds, emptyTable.adjustedContentInset);
                        return browse.bounds.size.height >= 44
                            && CGRectContainsRect(reachable, [browse convertRect:browse.bounds toView:emptyTable]);
                    });
                    [self checkLabelsIn:header];
                    XCTAssertGreaterThanOrEqual(browse.bounds.size.height, 44);
                    CGRect target = [browse convertRect:browse.bounds toView:teachableController.tableView];
                    [teachableController.tableView scrollRectToVisible:target animated:NO]; [self settle];
                    CGRect visible = UIEdgeInsetsInsetRect(teachableController.tableView.bounds, teachableController.tableView.adjustedContentInset);
                    XCTAssertTrue(CGRectContainsRect(visible, target), @"Full Browse action reachable at %@pt: %@ in %@", width, NSStringFromCGRect(target), NSStringFromCGRect(visible));
                    // Hit testing reaches the button only once the header has finished
                    // placing it in the window; wait for that, not for a delay.
                    TMSpinUntil(3, ^BOOL{
                        CGPoint point = [browse convertPoint:CGPointMake(CGRectGetMidX(browse.bounds), CGRectGetMidY(browse.bounds)) toView:window];
                        UIView *candidate = [window hitTest:point withEvent:nil];
                        return candidate == browse || [candidate isDescendantOfView:browse];
                    });
                    CGPoint center = [browse convertPoint:CGPointMake(CGRectGetMidX(browse.bounds), CGRectGetMidY(browse.bounds)) toView:self.window];
                    UIView *hit = [self.window hitTest:center withEvent:nil];
                    XCTAssertTrue(hit == browse || [hit isDescendantOfView:browse]);
                    UILabel *heading = (id)stack.arrangedSubviews.firstObject;
                    TMSpinUntil(3, ^BOOL{
                        CGRect current = [heading convertRect:CGRectMake(0, 0, heading.bounds.size.width, heading.font.lineHeight) toView:emptyTable];
                        [emptyTable scrollRectToVisible:current animated:NO];
                        [self settle];
                        return CGRectIntersectsRect(emptyTable.bounds, [heading convertRect:CGRectMake(0, 0, heading.bounds.size.width, heading.font.lineHeight) toView:emptyTable]);
                    });
                    CGRect firstLine = [heading convertRect:CGRectMake(0, 0, heading.bounds.size.width, heading.font.lineHeight) toView:teachableController.tableView];
                    [teachableController.tableView scrollRectToVisible:firstLine animated:NO]; [self settle];
                    XCTAssertTrue(CGRectIntersectsRect(teachableController.tableView.bounds, firstLine));
                    NSLog(@"TM_LAYOUT_PROBE empty width=%@ content=%.1f browse=%@ reachable=1", width, teachableController.tableView.contentSize.height, NSStringFromCGRect(target));
                }
                ids = @[@1809]; [teachableController.tableView reloadData]; [self settle];
                TMSpinUntil(3, ^BOOL{ return teachableController.tableView.tableHeaderView == nil; });
                XCTAssertNil(teachableController.tableView.tableHeaderView);
                XCTAssertEqual([teachableController.tableView numberOfRowsInSection:0], 1);
                ids = @[]; [teachableController.tableView reloadData]; [self settle];
                TMSpinUntil(3, ^BOOL{ return teachableController.tableView.tableHeaderView != nil; });
                XCTAssertNotNil(teachableController.tableView.tableHeaderView);
            } else {
                XCTAssertNil(teachableController.tableView.tableHeaderView);
                NSIndexPath *last = [NSIndexPath indexPathForRow:count.integerValue - 1 inSection:0];
                for (NSNumber *width in @[@320, @834, @320]) for (NSNumber *editing in @[@YES, @NO, @YES]) {
                    self.window.frame = CGRectMake(0, 0, width.doubleValue, 852);
                    [teachableController setEditing:editing.boolValue animated:NO]; [self settle];
                    UITableViewCell *cell = [self scrollRow:last in:teachableController.tableView position:UITableViewScrollPositionBottom];
                    XCTAssertNotNil(cell); XCTAssertEqual(cell.editing, editing.boolValue);
                    XCTAssertTrue([teachableController tableView:teachableController.tableView canMoveRowAtIndexPath:last]);
                    [self checkLabelsIn:cell.contentView];
                    CGRect row = [teachableController.tableView rectForRowAtIndexPath:last];
                    CGRect lastLine = CGRectMake(CGRectGetMidX(row), CGRectGetMaxY(row) - 1, 1, 1);
                    XCTAssertTrue(CGRectContainsRect(teachableController.tableView.bounds, lastLine), @"Last row remains reachable after resizing and editing");
                    XCTAssertTrue(cell.isAccessibilityElement);
                    XCTAssertTrue([cell.accessibilityLabel containsString:@"Sheet music available"]);
                    NSLog(@"TM_LAYOUT_PROBE teachable count=%@ width=%@ edit=%@ rowHeight=%.1f contentWidth=%.1f fullText=1 lastReachable=1", count, width, editing, cell.bounds.size.height, cell.contentView.bounds.size.width);
                }
                self.window.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryLarge;
                self.window.frame = CGRectMake(0, 0, 393, 852);
                [teachableController setEditing:NO animated:NO]; [self settle];
                UITableViewCell *dense = [self scrollRow:last in:teachableController.tableView position:UITableViewScrollPositionBottom];
                XCTAssertNotNil(dense);
                [self checkLabelsIn:dense.contentView];
                XCTAssertLessThan(dense.bounds.size.height, 260, @"Default catalog rows stay dense");
            }
        }
    } @finally {
        self.window.hidden = YES; self.window.rootViewController = nil;
        method_setImplementation(favorites, oldFavorites); method_setImplementation(teachable, oldTeachable); method_setImplementation(cache, oldCache);
        imp_removeBlock(list); imp_removeBlock(cached);
    }
}
- (void)testQueryEmptyErrorAndPopulatedReachability {
    Method method = class_getClassMethod(DPTag.class, @selector(query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:));
    IMP original = method_getImplementation(method);
    __block NSInteger mode = 0;
    DPTag *tag = [self layoutTag];
    IMP mock = imp_implementationWithBlock(^DPTagQueryResult *(id cls, NSString *query, int number, int start, NSNumber *parts, NSNumber *tracks, NSNumber *sheet, enum DPTagCollection collection, enum DPTagSortOptions sort) {
        if (mode == 1) [NSException raise:@"offline" format:@"Deterministic layout fixture"];
        DPTagQueryResult *result = [DPTagQueryResult new]; result.tags = mode == 2 ? @[tag] : @[];
        result.count = (int)result.tags.count; result.available = result.count; result.start = 0; return result;
    });
    method_setImplementation(method, mock);
    @try {
        for (mode = 0; mode < 3; mode++) {
            DPTagQueryViewController *query = [DPTagQueryViewController new];
            [self mount:query width:393 category:UIContentSizeCategoryAccessibilityExtraExtraExtraLarge];
            TMAssertEventually(5, ^BOOL{ return !query.isLoading; });
            [self settle];
            UITableView *table = [query valueForKey:@"tagTable"];
            XCTAssertEqual([table numberOfRowsInSection:0], mode == 2 ? 1 : 0);
            if (mode < 2) {
                UILabel *label = [query valueForKey:@"statusLabel"]; [self assertWholeLabel:label];
                UIButton *retry = [query valueForKey:@"retryButton"];
                XCTAssertEqual(retry.hidden, mode == 0);
                self.window.frame = CGRectMake(0, 0, 852, 393); [self settle];
                if (mode == 1) {
                    // The error view places its button a few passes after the landscape
                    // resize; scroll until that placement has come to rest.
                    TMSpinUntil(5, ^BOOL{
                        [table scrollRectToVisible:[retry convertRect:retry.bounds toView:table] animated:NO];
                        [self settle];
                        return retry.bounds.size.height >= 44
                            && CGRectContainsRect(CGRectInset(table.bounds, -.5, -.5), [retry convertRect:retry.bounds toView:table]);
                    });
                    CGRect rect = [retry convertRect:retry.bounds toView:table];
                    [table scrollRectToVisible:rect animated:NO];
                    XCTAssertTrue(CGRectContainsRect(CGRectInset(table.bounds, -.5, -.5), rect));
                    XCTAssertGreaterThanOrEqual(retry.bounds.size.height, 44);
                    [self captureLayout:@"query-error-landscape-ax5"];
                }
            } else [self checkLabelsIn:[self scrollRow:[NSIndexPath indexPathForRow:0 inSection:0] in:table position:UITableViewScrollPositionTop].contentView];
        }
    } @finally { method_setImplementation(method, original); imp_removeBlock(mock); }
}
@end

// In-memory Home fixtures never write favorites or contact the tag provider.
@interface TMFooterPitchTests : TMPolishRegressionTests
@property UIWindow *window;
@property UIWindow *previousWindow;
@property NSTimeInterval shippingKeyNoteDuration;
@end
@implementation TMFooterPitchTests
- (void)tearDown {
    [DPNote.C4 stop];
    self.window.hidden = YES;
    self.window.rootViewController = nil;
    [self.previousWindow makeKeyWindow];
    [super tearDown];
}
- (void)mount:(UIViewController *)controller width:(CGFloat)width dark:(BOOL)dark large:(BOOL)large {
    if (!self.previousWindow) for (UIWindow *window in UIApplication.sharedApplication.windows) if (window.isKeyWindow) self.previousWindow = window;
    self.window.hidden = YES;
    self.window.rootViewController = nil;
    self.window = [[UIWindow alloc] initWithFrame:CGRectMake(0, 0, width, UIScreen.mainScreen.bounds.size.height)];
    self.window.overrideUserInterfaceStyle = dark ? UIUserInterfaceStyleDark : UIUserInterfaceStyleLight;
    self.window.traitOverrides.preferredContentSizeCategory = large ? UIContentSizeCategoryAccessibilityExtraExtraExtraLarge : UIContentSizeCategoryLarge;
    self.window.tintColor = UIColor.systemBlueColor;
    self.window.backgroundColor = UIColor.systemBackgroundColor;
    UINavigationController *nav = [[UINavigationController alloc] initWithRootViewController:controller];
    UINavigationBarAppearance *appearance = [UINavigationBarAppearance new];
    [appearance configureWithOpaqueBackground];
    appearance.backgroundColor = [UIColor colorWithWhite:55.0 / 255 alpha:1];
    appearance.titleTextAttributes = @{NSForegroundColorAttributeName:UIColor.whiteColor};
    nav.navigationBar.standardAppearance = appearance;
    nav.navigationBar.scrollEdgeAppearance = appearance;
    nav.navigationBar.tintColor = UIColor.whiteColor;
    self.window.rootViewController = nav;
    [self.window makeKeyAndVisible];
    [self settle];
}
- (void)settle {
    [self.window updateTraitsIfNeeded];
    [self.window layoutIfNeeded];
    XCTestExpectation *transaction = [self expectationWithDescription:@"native layout committed"];
    [CATransaction begin];
    [CATransaction setCompletionBlock:^{ [transaction fulfill]; }];
    [self.window layoutIfNeeded];
    [CATransaction commit];
    [CATransaction flush];
    [self waitForExpectations:@[transaction] timeout:3];
    [self.window layoutIfNeeded];
}
- (void)capture:(NSString *)name {
    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad && [name containsString:@"barberpole"]) name = [name stringByAppendingString:@"-ipad"];
    [self settle];
    CGSize size = self.window.bounds.size;
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat];
    format.scale = MIN(1, 800 / MAX(size.width, size.height));
    UIImage *image = [[[UIGraphicsImageRenderer alloc] initWithSize:size format:format] imageWithActions:^(UIGraphicsImageRendererContext *context) {
        [self.window drawViewHierarchyInRect:self.window.bounds afterScreenUpdates:YES];
    }];
    NSString *dir = [NSTemporaryDirectory() stringByAppendingPathComponent:@"tm-footer-pitch-captures"];
    [NSFileManager.defaultManager createDirectoryAtPath:dir withIntermediateDirectories:YES attributes:nil error:nil];
    NSData *data = UIImageJPEGRepresentation(image, 0.85);
    XCTAssertTrue([data writeToFile:[dir stringByAppendingPathComponent:[name stringByAppendingPathExtension:@"jpg"]] atomically:YES]);
}
- (NSArray<UIButton *> *)links:(UIView *)view {
    NSMutableArray *links = [NSMutableArray array];
    for (UIView *child in view.subviews) {
        if ([child isKindOfClass:UIButton.class]) [links addObject:child];
        else [links addObjectsFromArray:[self links:child]];
    }
    return links;
}
- (void)assertCompact:(TMBarberPoleLoadingView *)pole pending:(BOOL)pending {
    XCTAssertTrue([pole isKindOfClass:TMBarberPoleLoadingView.class]);
    XCTAssertTrue(pole.compact);
    XCTAssertEqual(pole.isAnimating, pending);
    XCTAssertEqual(pole.hidden, !pending);
    XCTAssertEqualWithAccuracy(pole.bounds.size.height, TMLoaderCompactHeight, 0.1);
    // UIKit may reserve a 36pt navigation item; only the artwork is compact.
    XCTAssertLessThanOrEqual(((CALayer *)[pole valueForKey:@"logoLayer"]).frame.size.width, 20);
    CALayer *logo = [pole valueForKey:@"logoLayer"];
    XCTAssertLessThanOrEqual(logo.frame.size.height, TMLoaderCompactHeight + 0.01);
    XCTAssertEqualWithAccuracy(logo.transform.m11, logo.transform.m22, 0.000001);
}
- (void)compactComparisonDark:(BOOL)dark {
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat]; format.scale = 1;
    UIImage *image = [[[UIGraphicsImageRenderer alloc] initWithSize:CGSizeMake(500, 230) format:format] imageWithActions:^(UIGraphicsImageRendererContext *ctx) {
        [(dark ? [UIColor colorWithWhite:0.145 alpha:1] : [UIColor colorWithWhite:0.98 alpha:1]) setFill];
        UIRectFill(CGRectMake(0, 0, 500, 230));
        NSDictionary *text = @{NSFontAttributeName:[UIFont systemFontOfSize:18], NSForegroundColorAttributeName:dark ? UIColor.whiteColor : UIColor.blackColor};
        [@"iOS • same artwork, phase 0.25" drawAtPoint:CGPointMake(16, 8) withAttributes:text];
        for (NSUInteger i = 0; i < 2; i++) {
            TMBarberPoleLoadingView *pole = i == 0 ? [[TMBarberPoleLoadingView alloc] initWithOperationName:@"Loading"] : [[TMBarberPoleLoadingView alloc] initWithFrame:CGRectMake(0, 0, 34, 58)];
            pole.overrideUserInterfaceStyle = dark ? UIUserInterfaceStyleDark : UIUserInterfaceStyleLight;
            pole.hidden = NO;
            // A detached comparison view must resolve its override before reading its palette.
            [pole updateTraitsIfNeeded]; [pole layoutIfNeeded];
            CALayer *stripes = [pole valueForKey:@"stripes"];
            stripes.transform = CATransform3DMakeTranslation(0, .25 * TMLoaderStripeStep * TMLoaderPhaseMultiplier, 0);
            CGContextSaveGState(ctx.CGContext);
            CGContextTranslateCTM(ctx.CGContext, 70 + i * 230, 50); CGContextScaleCTM(ctx.CGContext, 2, 2);
            [pole.layer renderInContext:ctx.CGContext]; CGContextRestoreGState(ctx.CGContext);
            [(i == 0 ? @"Compact 19×32" : @"Search 34×58") drawAtPoint:CGPointMake(25 + i * 230, 188) withAttributes:text];
        }
    }];
    NSString *file = [NSString stringWithFormat:@"tagmaster-consistent-loading-ios-%@-comparison.jpg", dark ? @"dark" : @"light"];
    XCTAssertTrue([UIImageJPEGRepresentation(image, .9) writeToFile:[NSTemporaryDirectory() stringByAppendingPathComponent:file] atomically:YES]);
}
- (void)testCompactLifecycleAndSizing {
    UIViewController *host = [UIViewController new];
    [self mount:host width:UIScreen.mainScreen.bounds.size.width dark:NO large:YES];
    UIView *slot = [[UIView alloc] initWithFrame:CGRectMake(20, 130, 250, 60)]; slot.clipsToBounds = YES;
    [host.view addSubview:slot];
    TMBarberPoleLoadingView *pole = [[TMBarberPoleLoadingView alloc] initWithOperationName:@"Loading track"];
    [slot addSubview:pole]; [pole startAnimating]; [self settle];
    CALayer *stripes = [pole valueForKey:@"stripes"];
    XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
    pole.frame = CGRectMake(0, 0, 250, 60); [pole layoutIfNeeded];
    CALayer *logo = [pole valueForKey:@"logoLayer"];
    XCTAssertEqualWithAccuracy(logo.frame.size.height, 32, .01);
    XCTAssertEqualWithAccuracy(logo.position.x, 125, .01);
    // Ancestor visibility is observed by the production monitor. Await its
    // effect rather than assuming a fixed layout delay also fires that timer.
    slot.hidden = YES;
    [self waitUntil:^BOOL { return [stripes animationForKey:@"rotationStripes"] == nil; }];
    XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
    slot.hidden = NO;
    [self waitUntil:^BOOL { return [stripes animationForKey:@"rotationStripes"] != nil; }];
    XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
    pole.frame = CGRectOffset(pole.frame, 0, 100);
    [self waitUntil:^BOOL { return [stripes animationForKey:@"rotationStripes"] == nil; }];
    XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
    pole.frame = CGRectOffset(pole.frame, 0, -100);
    [self waitUntil:^BOOL { return [stripes animationForKey:@"rotationStripes"] != nil; }];
    XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
    [NSNotificationCenter.defaultCenter postNotificationName:UIApplicationWillResignActiveNotification object:nil];
    XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
    [NSNotificationCenter.defaultCenter postNotificationName:UIApplicationDidBecomeActiveNotification object:nil];
    XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
    Method motion = class_getInstanceMethod(TMBarberPoleLoadingView.class, NSSelectorFromString(@"reduceMotionEnabled"));
    IMP original = method_getImplementation(motion);
    __block BOOL reduced = YES;
    IMP mock = imp_implementationWithBlock(^BOOL(id view) { return reduced; });
    method_setImplementation(motion, mock);
    @try {
        [NSNotificationCenter.defaultCenter postNotificationName:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
        XCTAssertNil([stripes animationForKey:@"rotationStripes"]); XCTAssertTrue(pole.isAnimating);
        reduced = NO;
        [NSNotificationCenter.defaultCenter postNotificationName:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
        XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
    } @finally { method_setImplementation(motion, original); imp_removeBlock(mock); }
    [pole removeFromSuperview]; XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
    [pole stopAnimating]; [slot addSubview:pole]; [self settle]; XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
    XCTAssertFalse(pole.isAnimating);
    pole.darkSurface = YES; [pole layoutIfNeeded];
    XCTAssertTrue(CGColorEqualToColor(((CAShapeLayer *)[pole valueForKey:@"frameLayer"]).fillColor, TMLoaderColor(@"metalDark")));
    [self compactComparisonDark:NO]; [self compactComparisonDark:YES];
}
- (void)testFooterNativeSizes {
    Method favorites = class_getClassMethod(DPAppDelegate.class, @selector(favorites));
    Method cache = class_getClassMethod(DPTag.class, @selector(loadFromCache:));
    IMP oldFavorites = method_getImplementation(favorites), oldCache = method_getImplementation(cache);
    DPTag *tag = [self tag]; tag.title = @"Lost"; tag.alternativeTitle = @"In Your Eyes";
    IMP mockFavorites = imp_implementationWithBlock(^NSArray *(id owner) { return @[@1809]; });
    IMP mockCache = imp_implementationWithBlock(^DPTag *(id owner, int identifier) { return tag; });
    method_setImplementation(favorites, mockFavorites); method_setImplementation(cache, mockCache);
    @try {
        CGFloat width = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? 320 : UIScreen.mainScreen.bounds.size.width;
        for (NSNumber *large in @[@NO, @YES]) for (NSNumber *dark in @[@NO, @YES]) {
            DPHomeViewController *home = [DPHomeViewController new];
            [self mount:home width:width dark:dark.boolValue large:large.boolValue];
            [home.tableView reloadData]; [self settle];
            UIView *footer = home.tableView.tableFooterView;
            [home.tableView scrollRectToVisible:footer.frame animated:NO]; [self settle];
            NSArray<UIButton *> *links = [self links:footer];
            XCTAssertEqual(links.count, 4);
            XCTAssertEqual([home.tableView numberOfRowsInSection:TMHomeFavoritesSection], 1);
            NSLog(@"TM_FOOTER after width=%.0f large=%@ dark=%@ height=%.1f", width, large, dark, footer.bounds.size.height);
            if (!large.boolValue && width > 320) {
                XCTAssertGreaterThanOrEqual(footer.bounds.size.height, 110);
                XCTAssertLessThanOrEqual(footer.bounds.size.height, 140);
            }
            NSArray *destinations = @[@"https://www.barbershoptags.com", @"https://apps.depoll.com", @"https://apps.depoll.com/terms-of-use", @"https://www.davidpoll.com/applications/tag-master/donate"];
            XCTAssertEqualObjects([links valueForKeyPath:@"url.absoluteString"], destinations);
            XCTAssertEqualObjects(links.firstObject.currentTitle, @"Content provided by BarbershopTags.com");
            for (UIButton *button in links) {
                XCTAssertGreaterThanOrEqual(button.bounds.size.height, 44);
                XCTAssertGreaterThanOrEqual(button.bounds.size.width, 44);
                XCTAssertTrue(button.accessibilityTraits & UIAccessibilityTraitLink);
                XCTAssertEqualWithAccuracy(button.titleLabel.font.pointSize, [UIFont preferredFontForTextStyle:UIFontTextStyleFootnote compatibleWithTraitCollection:button.traitCollection].pointSize, 0.1);
                CGSize text = [button.titleLabel sizeThatFits:CGSizeMake(button.titleLabel.bounds.size.width, CGFLOAT_MAX)];
                XCTAssertGreaterThanOrEqual(button.titleLabel.bounds.size.height + 1, text.height);
                CGRect rect = [button convertRect:button.bounds toView:footer];
                XCTAssertTrue(CGRectContainsRect(CGRectInset(footer.bounds, -1, -1), rect));
                for (UIButton *other in links) if (other != button) {
                    XCTAssertFalse(CGRectIntersectsRect(CGRectInset(rect, 0.25, 0.25), [other convertRect:other.bounds toView:footer]), @"%@ overlaps %@", button.accessibilityIdentifier, other.accessibilityIdentifier);
                }
                CGRect tableRect = [button convertRect:button.bounds toView:home.tableView];
                [home.tableView scrollRectToVisible:tableRect animated:NO]; [self settle];
                CGPoint center = [button convertPoint:CGPointMake(button.bounds.size.width / 2, button.bounds.size.height / 2) toView:self.window];
                XCTAssertEqual([self.window hitTest:center withEvent:nil], button);
            }
            if (!large.boolValue) {
                XCTAssertEqualWithAccuracy([links[2] convertRect:links[2].bounds toView:footer].origin.y, [links[3] convertRect:links[3].bounds toView:footer].origin.y, 1);
            }
            [home.tableView scrollRectToVisible:footer.frame animated:NO];
            NSString *name = [NSString stringWithFormat:@"tagmaster-ios-footer-pitch-after-%@-%@-%@", width == 320 ? @"sidebar" : @"phone", dark.boolValue ? @"dark" : @"light", large.boolValue ? @"AX5" : @"default"];
            [self capture:name];
        }
    } @finally {
        method_setImplementation(favorites, oldFavorites); method_setImplementation(cache, oldCache);
        imp_removeBlock(mockFavorites); imp_removeBlock(mockCache);
    }
}
// Count actual rendered pixels, not UIButton highlighted flags or configuration alone.
- (NSUInteger)pixelsIn:(UIView *)view matching:(UIColor *)color {
    NSUInteger width = ceil(view.bounds.size.width), height = ceil(view.bounds.size.height);
    if (width == 0 || height == 0) return 0;
    unsigned char *bytes = calloc(width * height, 4);
    CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(bytes, width, height, 8, width * 4, space, kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGContextTranslateCTM(context, 0, height); CGContextScaleCTM(context, 1, -1);
    [view.layer renderInContext:context];
    CGFloat r, g, b, a; [[color resolvedColorWithTraitCollection:view.traitCollection] getRed:&r green:&g blue:&b alpha:&a];
    NSUInteger count = 0;
    for (NSUInteger i = 0; i < width * height; i++) {
        unsigned char *p = bytes + i * 4;
        if (p[3] > 220 && fabs(p[0] / 255.0 - r) < 0.06 && fabs(p[1] / 255.0 - g) < 0.06 && fabs(p[2] / 255.0 - b) < 0.06) count++;
    }
    CGContextRelease(context); CGColorSpaceRelease(space); free(bytes);
    return count;
}
- (CGFloat)luminance:(UIColor *)color traits:(UITraitCollection *)traits {
    CGFloat r, g, b, a; [[color resolvedColorWithTraitCollection:traits] getRed:&r green:&g blue:&b alpha:&a];
    CGFloat (^linear)(CGFloat) = ^CGFloat(CGFloat c) { return c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4); };
    return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b);
}
- (void)logPitch:(UIButton *)button phase:(NSString *)phase {
    DPNote *note = [button valueForKey:@"note"];
    NSTimer *timer = [button valueForKey:@"noteTimer"];
    UIColor *fill = button.configuration.background.backgroundColor;
    NSLog(@"TM_PITCH_STATE %@ %@ attached=%d timerValid=%d playing=%d showing=%@ highlighted=%d enabled=%d fill=%@ fillPixels=%lu", phase, button.accessibilityIdentifier, button.window != nil, timer.valid, note.isPlaying, [button valueForKey:@"showingPlayback"], button.highlighted, button.enabled, fill, (unsigned long)[self pixelsIn:button matching:fill]);
}
- (void)assertKey:(UIButton *)button playing:(BOOL)playing {
    if (playing) [self logPitch:button phase:@"before rendered wait"];
    [self settle];
    UIColor *fill = button.configuration.background.backgroundColor;
    if (playing) {
        // Enabling a configured UIKit button can still be animating its disabled fill.
        // Wait for rendered feedback, not merely the configuration or highlighted flag.
        [self waitUntil:^BOOL {
            [button layoutIfNeeded];
            return [self pixelsIn:button matching:button.configuration.background.backgroundColor] > button.bounds.size.width * button.bounds.size.height * .45;
        }];
        [self logPitch:button phase:@"after rendered wait"];
        XCTAssertTrue([[button valueForKey:@"note"] isPlaying], @"Rendered feedback must still represent a playing note");
        XCTAssertTrue([[button valueForKey:@"showingPlayback"] boolValue]);
        fill = button.configuration.background.backgroundColor;
        UIColor *foreground = button.configuration.baseForegroundColor;
        XCTAssertEqual(button.configuration.image.renderingMode, UIImageRenderingModeAlwaysOriginal);
        XCTAssertGreaterThan([self pixelsIn:button matching:fill], button.bounds.size.width * button.bounds.size.height * 0.45);
        XCTAssertGreaterThan([self pixelsIn:button.titleLabel matching:foreground], 2);
        XCTAssertGreaterThan([self pixelsIn:button.imageView matching:foreground], 2);
        CGFloat a = [self luminance:fill traits:button.traitCollection], b = [self luminance:foreground traits:button.traitCollection];
        CGFloat contrast = (MAX(a, b) + 0.05) / (MIN(a, b) + 0.05);
        NSLog(@"TM_PITCH %@ contrast=%.2f fillPixels=%lu textPixels=%lu iconPixels=%lu", button.accessibilityIdentifier, contrast, (unsigned long)[self pixelsIn:button matching:fill], (unsigned long)[self pixelsIn:button.titleLabel matching:foreground], (unsigned long)[self pixelsIn:button.imageView matching:foreground]);
        XCTAssertGreaterThanOrEqual(contrast, 4.5);
    } else {
        [self waitUntil:^BOOL {
            [button layoutIfNeeded];
            return CGColorGetAlpha(button.configuration.background.backgroundColor.CGColor) == 0
                && [self pixelsIn:button matching:button.tintColor] > 5;
        }];
        fill = button.configuration.background.backgroundColor;
        XCTAssertEqualWithAccuracy(CGColorGetAlpha(fill.CGColor), 0, 0.01);
        XCTAssertGreaterThan([self pixelsIn:button matching:button.tintColor], 5);
    }
    XCTAssertEqualWithAccuracy(button.layer.borderWidth, 1.5, 0.01);
    XCTAssertEqualWithAccuracy(button.layer.cornerRadius, 8, 0.01);
}
// Deliberately real elapsed time: the assertions that follow are negative
// ("the old deadline did not stop this note"), so the clock has to move. The
// interval is now derived from the injected deadline rather than the shipping
// 1.5 seconds, preserving each margin while costing a third of the wall clock.
- (void)waitForPitchInterval:(NSTimeInterval)interval {
    XCTestExpectation *deadline = [self expectationWithDescription:@"cross timed activation deadline"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, interval * NSEC_PER_SEC), dispatch_get_main_queue(), ^{ [deadline fulfill]; });
    [self waitForExpectations:@[deadline] timeout:interval + 2];
}
// 0.15 s past the timed deadline, exactly the grace the hard-coded 1.65 had over 1.5.
- (UIImage *)logoImage:(UIView *)pole {
    CALayer *logo = [pole valueForKey:@"logoLayer"];
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat];
    format.scale = 1;
    return [[[UIGraphicsImageRenderer alloc] initWithSize:CGSizeMake(150, 257) format:format] imageWithActions:^(UIGraphicsImageRendererContext *context) {
        CGContextScaleCTM(context.CGContext, 150 / 299.75076, 257 / 513.52234);
        [logo renderInContext:context.CGContext];
    }];
}
- (NSData *)logoPixels:(UIImage *)image {
    NSMutableData *pixels = [NSMutableData dataWithLength:150 * 257 * 4];
    CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(pixels.mutableBytes, 150, 257, 8, 150 * 4, space, kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    // CGImage already has raster row order. Unlike renderInContext:, drawing it
    // into the bitmap needs no UIKit flip before comparing canonical y coordinates.
    CGContextDrawImage(context, CGRectMake(0, 0, 150, 257), image.CGImage);
    CGContextRelease(context); CGColorSpaceRelease(space);
    return pixels;
}
- (TMLogoBackgroundView *)findLogoBackground:(UIView *)view {
    if ([view isKindOfClass:TMLogoBackgroundView.class]) return (id)view;
    for (UIView *child in view.subviews) {
        TMLogoBackgroundView *found = [self findLogoBackground:child];
        if (found) return found;
    }
    return nil;
}
- (void)assertNoArtworkAnimations:(CALayer *)layer {
    XCTAssertEqual(layer.animationKeys.count, 0);
    for (CALayer *child in layer.sublayers) [self assertNoArtworkAnimations:child];
}
- (UIImage *)artworkImage:(UIView *)view {
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat]; format.scale = 1;
    return [[[UIGraphicsImageRenderer alloc] initWithSize:view.bounds.size format:format] imageWithActions:^(UIGraphicsImageRendererContext *context) {
        [view.layer renderInContext:context.CGContext];
    }];
}
- (NSData *)artworkPixels:(UIImage *)image {
    NSUInteger width = CGImageGetWidth(image.CGImage), height = CGImageGetHeight(image.CGImage);
    NSMutableData *data = [NSMutableData dataWithLength:width * height * 4];
    CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(data.mutableBytes, width, height, 8, width * 4, space, kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
    CGContextDrawImage(context, CGRectMake(0, 0, width, height), image.CGImage);
    CGContextRelease(context); CGColorSpaceRelease(space);
    return data;
}
- (void)testSharedVectorArtwork {
    CGPathRef full = TMLogoFullPath(), silhouette = TMLogoSilhouettePath(), highlights = TMLogoHighlightsPath();
    for (NSUInteger i = 0; i < 100; i++) {
        XCTAssertEqual(full, TMLogoFullPath());
        XCTAssertEqual(silhouette, TMLogoSilhouettePath());
        XCTAssertEqual(highlights, TMLogoHighlightsPath());
    }
    // Independently split the public compound path to check part selection.
    NSMutableArray<UIBezierPath *> *contours = [NSMutableArray array];
    __block UIBezierPath *part;
    __block NSUInteger curves = 0, closes = 0;
    CGPathApplyWithBlock(full, ^(const CGPathElement *e) {
        switch (e->type) {
            case kCGPathElementMoveToPoint:
                part = [UIBezierPath bezierPath]; [contours addObject:part]; [part moveToPoint:e->points[0]]; break;
            case kCGPathElementAddCurveToPoint:
                curves++; [part addCurveToPoint:e->points[2] controlPoint1:e->points[0] controlPoint2:e->points[1]]; break;
            case kCGPathElementCloseSubpath: closes++; [part closePath]; break;
            default: XCTFail(@"Unexpected canonical path command"); break;
        }
    });
    XCTAssertEqual(contours.count, 9); XCTAssertEqual(curves, 122); XCTAssertEqual(closes, 9);
    XCTAssertTrue(CGPathEqualToPath(contours[0].CGPath, silhouette));
    UIBezierPath *shine = [UIBezierPath bezierPath];
    for (NSNumber *index in @[@1, @2, @7, @8]) [shine appendPath:contours[index.unsignedIntegerValue]];
    XCTAssertTrue(CGPathEqualToPath(shine.CGPath, highlights));
    for (NSValue *value in @[[NSValue valueWithCGPoint:CGPointMake(35, 438)], [NSValue valueWithCGPoint:CGPointMake(90, 424)], [NSValue valueWithCGPoint:CGPointMake(110, 402)], [NSValue valueWithCGPoint:CGPointMake(110, 330)], [NSValue valueWithCGPoint:CGPointMake(155, 242)], [NSValue valueWithCGPoint:CGPointMake(200, 153)], [NSValue valueWithCGPoint:CGPointMake(200, 88)], [NSValue valueWithCGPoint:CGPointMake(238, 20)]]) {
        XCTAssertTrue(CGPathContainsPoint(silhouette, NULL, value.CGPointValue, NO));
        XCTAssertFalse(CGPathContainsPoint(full, NULL, value.CGPointValue, NO), @"All eight inner contours must remain cutouts");
    }
    UIView *pole = [[NSClassFromString(@"TMBarberPoleLoadingView") alloc] initWithFrame:CGRectMake(0, 0, 34, 68)];
    CAShapeLayer *metal = [pole valueForKey:@"frameLayer"];
    // CAShapeLayer copies assigned paths; provider identity is checked above.
    XCTAssertTrue(CGPathEqualToPath(metal.path, TMLoaderMetalPath()));
    XCTAssertEqualObjects(metal.fillRule, kCAFillRuleEvenOdd);
    XCTAssertEqual(metal.sublayers.count, 0, @"Highlights are holes, not white paint");
    XCTAssertEqual(TMLoaderMetalPath(), TMLoaderMetalPath());
    XCTAssertEqual(TMLoaderShaftPath(), TMLoaderShaftPath());
    XCTAssertEqual(TMLoaderStripePath(), TMLoaderStripePath());
    NSString *fixturePath = [[NSBundle bundleForClass:self.class] pathForResource:@"screenbackground@2x" ofType:@"png"];
    XCTAssertNotNil(fixturePath);
    UIImage *fixture = [UIImage imageWithContentsOfFile:fixturePath];
    XCTAssertNotNil(fixture);
    XCTAssertEqual(CGImageGetWidth(fixture.CGImage), 480);
    XCTAssertEqual(CGImageGetHeight(fixture.CGImage), 800);
    XCTAssertNil([NSBundle.mainBundle pathForResource:@"screenbackground@2x" ofType:@"png"]);
    XCTAssertNotNil([UIImage imageNamed:@"LaunchWatermark"]);
    NSMutableArray<UIImage *> *captures = [NSMutableArray array];
    NSArray<NSValue *> *sizes = @[[NSValue valueWithCGSize:CGSizeMake(320, 568)], [NSValue valueWithCGSize:CGSizeMake(402, 874)], [NSValue valueWithCGSize:CGSizeMake(834, 1210)], [NSValue valueWithCGSize:CGSizeMake(1194, 834)]];
    for (NSNumber *tableMode in @[@NO, @YES]) for (NSNumber *dark in @[@NO, @YES]) for (NSValue *sizeValue in sizes) {
        CGSize size = sizeValue.CGSizeValue;
        UIView *root = tableMode.boolValue ? [[UITableView alloc] initWithFrame:(CGRect){CGPointZero, size}] : [[UIView alloc] initWithFrame:(CGRect){CGPointZero, size}];
        root.overrideUserInterfaceStyle = dark.boolValue ? UIUserInterfaceStyleDark : UIUserInterfaceStyleLight;
        [DPAppDelegate setUpBackground:root];
        [root layoutIfNeeded];
        UIView *owner = tableMode.boolValue ? ((UITableView *)root).backgroundView : root;
        [owner layoutIfNeeded];
        TMLogoBackgroundView *background = [self findLogoBackground:owner];
        XCTAssertNotNil(background); XCTAssertEqual(background.superview, owner);
        XCTAssertFalse(background.userInteractionEnabled); XCTAssertFalse(background.isAccessibilityElement); XCTAssertTrue(background.accessibilityElementsHidden);
        XCTAssertEqualWithAccuracy(background.frame.origin.y, 60, 0.01);
        XCTAssertEqualWithAccuracy(background.frame.size.height, size.height - 104, 0.01);
        XCTAssertEqualWithAccuracy(background.frame.size.width, size.width, 0.01);
        XCTAssertNil([background hitTest:CGPointMake(50, 50) withEvent:nil]);
        CAShapeLayer *art = (id)background.layer.sublayers.firstObject;
        CGPathRef layerPath = art.path;
        XCTAssertTrue(CGPathEqualToPath(layerPath, full)); XCTAssertEqualObjects(art.fillRule, kCAFillRuleNonZero);
        XCTAssertEqualWithAccuracy(art.affineTransform.a, art.affineTransform.d, 0.000001);
        CGFloat r, g, b, a; [[UIColor colorWithCGColor:art.fillColor] getRed:&r green:&g blue:&b alpha:&a];
        XCTAssertEqualWithAccuracy(r, 128.0/255, 0.000001); XCTAssertEqualWithAccuracy(g, r, 0.000001); XCTAssertEqualWithAccuracy(b, r, 0.000001); XCTAssertEqualWithAccuracy(a, 76.0/255, 0.000001);
        CGAffineTransform transform = art.affineTransform;
        [background setNeedsLayout]; [background layoutIfNeeded];
        XCTAssertTrue(CGAffineTransformEqualToTransform(transform, art.affineTransform)); XCTAssertEqual(art.path, layerPath);
        [self assertNoArtworkAnimations:background.layer];
        UIImageView *old = [[UIImageView alloc] initWithImage:fixture];
        old.frame = background.bounds; old.contentMode = UIViewContentModeScaleAspectFit;
        UIImage *oldImage = [self artworkImage:old], *newImage = [self artworkImage:background];
        NSData *oldData = [self artworkPixels:oldImage], *newData = [self artworkPixels:newImage];
        XCTAssertEqual(oldData.length, newData.length);
        const uint8_t *p = oldData.bytes, *q = newData.bytes;
        NSUInteger intersection = 0, unionCount = 0, interior = 0;
        double delta = 0;
        NSUInteger pixelWidth = CGImageGetWidth(newImage.CGImage);
        NSInteger minX[2] = {NSIntegerMax, NSIntegerMax}, minY[2] = {NSIntegerMax, NSIntegerMax}, maxX[2] = {0, 0}, maxY[2] = {0, 0};
        for (NSUInteger i = 0; i < oldData.length; i += 4) {
            BOOL oldInk = p[i+3] > 38, newInk = q[i+3] > 38;
            intersection += oldInk && newInk; unionCount += oldInk || newInk;
            delta += abs((int)p[i+3] - (int)q[i+3]);
            for (NSUInteger image = 0; image < 2; image++) if (image ? newInk : oldInk) {
                NSInteger x = (i / 4) % pixelWidth, y = (i / 4) / pixelWidth;
                minX[image] = MIN(minX[image], x); maxX[image] = MAX(maxX[image], x);
                minY[image] = MIN(minY[image], y); maxY[image] = MAX(maxY[image], y);
            }
            if (q[i+3] == 76) { interior++; XCTAssertEqualWithAccuracy(q[i], 38, 1); XCTAssertEqual(q[i], q[i+1]); XCTAssertEqual(q[i], q[i+2]); }
        }
        XCTAssertEqualWithAccuracy(minX[0], minX[1], 2); XCTAssertEqualWithAccuracy(maxX[0], maxX[1], 2);
        XCTAssertEqualWithAccuracy(minY[0], minY[1], 2); XCTAssertEqualWithAccuracy(maxY[0], maxY[1], 2);
        NSLog(@"TM_VECTOR_BOUNDS size=%@ old=(%ld,%ld)-(%ld,%ld) new=(%ld,%ld)-(%ld,%ld)", NSStringFromCGSize(size), (long)minX[0], (long)minY[0], (long)maxX[0], (long)maxY[0], (long)minX[1], (long)minY[1], (long)maxX[1], (long)maxY[1]);
        double overlap = (double)intersection / unionCount, meanAlphaError = delta / (oldData.length / 4) / 255;
        XCTAssertGreaterThan(overlap, 0.95); XCTAssertLessThan(meanAlphaError, 0.005); XCTAssertGreaterThan(interior, 1000);
        NSLog(@"TM_VECTOR table=%@ dark=%@ size=%@ overlap=%.6f meanAlphaError=%.6f interior=%lu", tableMode, dark, NSStringFromCGSize(size), overlap, meanAlphaError, (unsigned long)interior);
        if (!tableMode.boolValue && size.width == (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? 834 : 402)) {
            [captures addObject:oldImage]; [captures addObject:newImage];
        }
        // Detach from constraints before exercising a standalone bounds change.
        [background removeFromSuperview];
        background.translatesAutoresizingMaskIntoConstraints = YES;
        background.bounds = CGRectMake(0, 0, background.bounds.size.height, background.bounds.size.width);
        [background layoutIfNeeded];
        XCTAssertEqual(art.path, layerPath); XCTAssertFalse(CGAffineTransformEqualToTransform(transform, art.affineTransform));
        [self assertNoArtworkAnimations:background.layer];
    }
    XCTAssertEqual(captures.count, 4);
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat]; format.scale = 1;
    UIImage *sheet = [[[UIGraphicsImageRenderer alloc] initWithSize:CGSizeMake(780, 720) format:format] imageWithActions:^(UIGraphicsImageRendererContext *context) {
        for (NSUInteger theme = 0; theme < 2; theme++) {
            [(theme ? UIColor.blackColor : UIColor.whiteColor) setFill]; UIRectFill(CGRectMake(0, theme * 360, 780, 360));
            for (NSUInteger column = 0; column < 2; column++) {
                UIImage *image = captures[theme * 2 + column]; CGFloat s = MIN(350 / image.size.width, 318 / image.size.height);
                [(column ? @"Shared vector" : @"Original PNG fixture") drawAtPoint:CGPointMake(column * 390 + 20, theme * 360 + 10) withAttributes:@{NSFontAttributeName:[UIFont systemFontOfSize:14], NSForegroundColorAttributeName:theme ? UIColor.whiteColor : UIColor.blackColor}];
                [image drawInRect:CGRectMake(column * 390 + (390 - image.size.width * s) / 2, theme * 360 + 35, image.size.width * s, image.size.height * s)];
            }
        }
    }];
    NSString *device = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? @"ipad" : @"phone";
    NSString *file = [NSTemporaryDirectory() stringByAppendingPathComponent:[NSString stringWithFormat:@"tagmaster-ios-shared-vector-%@-watermark.jpg", device]];
    XCTAssertTrue([UIImageJPEGRepresentation(sheet, 0.9) writeToFile:file atomically:YES]);
}

// Render the production layer tree, not a second implementation, into explicit sRGB pixels.
- (void)exportUnifiedFrames:(UIView *)pole dark:(BOOL)dark {
    CALayer *logo = [pole valueForKey:@"logoLayer"], *stripes = [pole valueForKey:@"stripes"];
    [stripes removeAnimationForKey:@"rotationStripes"];
    NSString *device = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? @"ipad" : @"phone";
    for (NSNumber *width in @[@340, @34]) {
        size_t w = width.unsignedIntegerValue, h = w == 340 ? 580 : 58;
        NSMutableArray<NSData *> *frames = [NSMutableArray array];
        for (NSNumber *phase in @[@0, @0.25, @0.5, @0.75, @1, @0.001, @0.999]) {
            [CATransaction begin]; [CATransaction setDisableActions:YES];
            stripes.transform = CATransform3DMakeTranslation(0, (phase.doubleValue - floor(phase.doubleValue)) * TMLoaderStripeStep * TMLoaderPhaseMultiplier, 0);
            [CATransaction commit];
            NSMutableData *pixels = [NSMutableData dataWithLength:w * h * 4];
            CGColorSpaceRef space = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
            CGContextRef context = CGBitmapContextCreate(pixels.mutableBytes, w, h, 8, w * 4, space, kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
            CGContextTranslateCTM(context, 0, h); CGContextScaleCTM(context, 1, -1);
            CGFloat scale = MIN(w / TMLogoWidth, h / TMLogoHeight);
            CGContextTranslateCTM(context, (w - TMLogoWidth * scale) / 2, (h - TMLogoHeight * scale) / 2);
            CGContextScaleCTM(context, scale, scale);
            [logo renderInContext:context];
            CGImageRef cg = CGBitmapContextCreateImage(context);
            UIImage *image = [UIImage imageWithCGImage:cg];
            NSString *file = [NSString stringWithFormat:@"unified-ios-%@-%@-%zu-%@.png", device, dark ? @"dark" : @"light", w, phase];
            XCTAssertTrue([UIImagePNGRepresentation(image) writeToFile:[NSTemporaryDirectory() stringByAppendingPathComponent:file] atomically:YES]);
            [frames addObject:pixels];
            CGImageRelease(cg); CGContextRelease(context); CGColorSpaceRelease(space);
        }
        XCTAssertEqualObjects(frames[0], frames[4], @"Exact loop endpoint");
        const uint8_t *first = frames[0].bytes;
        for (NSUInteger phase = 1; phase < frames.count; phase++) {
            const uint8_t *next = frames[phase].bytes;
            NSUInteger changes = 0;
            for (NSUInteger i = 0; i < w * h * 4; i += 4) {
                XCTAssertEqual(first[i + 3], next[i + 3], @"Invariant outer alpha");
                if (memcmp(first + i, next + i, 4)) changes++;
            }
            if (phase >= 5) XCTAssertLessThan(changes, w * h / 20, @"Near-wrap continuity");
        }
    }
    stripes.transform = CATransform3DIdentity;
}

- (void)testBarberPoleLogoGeometry {
    UIView *pole = [[NSClassFromString(@"TMBarberPoleLoadingView") alloc] initWithFrame:CGRectMake(0, 0, 402, 68)];
    [pole layoutIfNeeded];
    CALayer *logo = [pole valueForKey:@"logoLayer"], *shaft = [pole valueForKey:@"cylinder"];
    CAShapeLayer *metal = [pole valueForKey:@"frameLayer"];
    XCTAssertEqualWithAccuracy(logo.frame.size.height, 58, 0.001);
    XCTAssertEqualWithAccuracy(logo.frame.size.width, 58 * 299.75076 / 513.52234, 0.001);
    XCTAssertEqualWithAccuracy(logo.position.x, 201, 0.001);
    XCTAssertEqualWithAccuracy(logo.frame.origin.y, 5, 0.001);
    XCTAssertEqualWithAccuracy(logo.transform.m11, logo.transform.m22, 0.000001);
    CGRect bounds = CGPathGetPathBoundingBox(metal.path);
    XCTAssertEqualWithAccuracy(bounds.size.width, 299.75076, 0.01);
    XCTAssertEqualWithAccuracy(bounds.size.height, 513.52234, 0.01);
    // Round finials contain their cardinal interiors, but not square corners.
    for (NSValue *value in @[[NSValue valueWithCGPoint:CGPointMake(248, 50)], [NSValue valueWithCGPoint:CGPointMake(50, 463)], [NSValue valueWithCGPoint:CGPointMake(205, 50)], [NSValue valueWithCGPoint:CGPointMake(290, 50)], [NSValue valueWithCGPoint:CGPointMake(50, 420)], [NSValue valueWithCGPoint:CGPointMake(50, 505)], [NSValue valueWithCGPoint:CGPointMake(272, 125)], [NSValue valueWithCGPoint:CGPointMake(23, 387)]]) {
        XCTAssertTrue(CGPathContainsPoint(metal.path, NULL, value.CGPointValue, NO));
    }
    XCTAssertFalse(CGPathContainsPoint(metal.path, NULL, CGPointMake(201, 5), NO));
    XCTAssertFalse(CGPathContainsPoint(metal.path, NULL, CGPointMake(5, 510), NO));
    __block NSUInteger curves = 0, highlights = 0;
    CGPathApplyWithBlock(metal.path, ^(const CGPathElement *element) { if (element->type == kCGPathElementAddCurveToPoint) curves++; });
    CGPathApplyWithBlock(metal.path, ^(const CGPathElement *element) { if (element->type == kCGPathElementMoveToPoint) highlights++; });
    XCTAssertEqual(curves, 82); XCTAssertEqual(highlights - 1, 4);
    XCTAssertEqualObjects(metal.fillRule, kCAFillRuleEvenOdd);
    CALayer *stripes = [pole valueForKey:@"stripes"];
    XCTAssertEqual(stripes.sublayers.count, 13);
    NSInteger index = TMLoaderRepeatMin;
    for (CAShapeLayer *band in stripes.sublayers) {
        XCTAssertTrue(CGPathEqualToPath(band.path, TMLoaderStripePath()));
        XCTAssertEqualWithAccuracy(band.transform.m42, index * TMLoaderStripeStep, 0.0001);
        XCTAssertLessThan(fabs(band.transform.m42), 800, @"Negative repeats must stay signed");
        index++;
    }
    CGRect repeatBounds = CGPathGetPathBoundingBox(TMLoaderStripePath());
    XCTAssertLessThan(CGRectGetMinY(repeatBounds) + TMLoaderRepeatMin * TMLoaderStripeStep + 216, 0);
    XCTAssertGreaterThan(CGRectGetMaxY(repeatBounds) + TMLoaderRepeatMax * TMLoaderStripeStep, TMLogoHeight);
    CALayer *axis = stripes.superlayer;
    XCTAssertEqual(axis.superlayer, shaft);
    XCTAssertTrue(CGPathEqualToPath(((CAShapeLayer *)shaft.mask).path, TMLoaderShaftPath()));
    CGPoint top = [axis convertPoint:CGPointMake(60, 0) toLayer:logo];
    CGPoint bottom = [axis convertPoint:CGPointMake(60, 307) toLayer:logo];
    XCTAssertGreaterThan(top.x, bottom.x);
    XCTAssertEqualWithAccuracy(atan2(top.x - bottom.x, bottom.y - top.y) * 180 / M_PI, 26, 0.0001);
}
- (void)verifyLogoPhases:(UIView *)pole reduced:(BOOL *)reduced {
    CALayer *stripes = [pole valueForKey:@"stripes"], *shaft = [pole valueForKey:@"cylinder"], *logo = [pole valueForKey:@"logoLayer"];
    CAShapeLayer *metal = [pole valueForKey:@"frameLayer"];
    CGPathRef stationary = CGPathCreateCopy(metal.path);
    CGPathRef shaftFringe = CGPathCreateCopyByStrokingPath(TMLoaderShaftPath(), NULL, 4, kCGLineCapRound, kCGLineJoinRound, 1);
    NSMutableArray<UIImage *> *images = [NSMutableArray array];
    NSMutableArray<UIImage *> *screens = [NSMutableArray array];
    for (NSNumber *dark in @[@NO, @YES]) {
        self.window.overrideUserInterfaceStyle = dark.boolValue ? UIUserInterfaceStyleDark : UIUserInterfaceStyleLight;
        [self settle];
        [self exportUnifiedFrames:pole dark:dark.boolValue];
        NSData *first = nil;
        TMLogoBackgroundView *background = [self findLogoBackground:self.window];
        XCTAssertNotNil(background);
        NSData *backgroundPixels = [self artworkPixels:[self artworkImage:background]];
        for (NSNumber *phase in @[@0, @0.25, @0.5, @1, @0]) {
            BOOL still = images.count % 5 == 4;
            *reduced = still;
            [NSNotificationCenter.defaultCenter postNotificationName:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
            if (still) XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
            else XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
            [stripes removeAnimationForKey:@"rotationStripes"];
            [CATransaction begin]; [CATransaction setDisableActions:YES];
            stripes.transform = CATransform3DMakeTranslation(0, (phase.doubleValue - floor(phase.doubleValue)) * TMLoaderStripeStep * TMLoaderPhaseMultiplier, 0);
            [CATransaction commit]; [CATransaction flush];
            [self assertNoArtworkAnimations:background.layer];
            XCTAssertEqualObjects(backgroundPixels, [self artworkPixels:[self artworkImage:background]], @"Watermark pixels must not change as loader phase advances");
            UIImage *image = [self logoImage:pole]; [images addObject:image];
            NSData *pixels = [self logoPixels:image];
            if (!first) first = pixels;
            const uint8_t *a = first.bytes, *b = pixels.bytes;
            NSUInteger outsideChanges = 0, insideChanges = 0, red = 0, blue = 0, white = 0, spill = 0, alphaChanges = 0;
            for (NSUInteger y = 0; y < 257; y++) for (NSUInteger x = 0; x < 150; x++) {
                CGPoint p = CGPointMake((x + 0.5) * 299.75076 / 150, (y + 0.5) * 513.52234 / 257);
                CGPoint local = [shaft convertPoint:p fromLayer:logo];
                // One raster pixel on either side of the shared shaft boundary.
                BOOL inShaft = CGPathContainsPoint(TMLoaderShaftPath(), NULL, local, NO) || CGPathContainsPoint(shaftFringe, NULL, local, NO);
                NSUInteger i = (y * 150 + x) * 4;
                BOOL changed = memcmp(a + i, b + i, 4) != 0;
                if (a[i + 3] != b[i + 3]) alphaChanges++;
                if (changed) { if (inShaft) insideChanges++; else outsideChanges++; }
                BOOL isRed = b[i] > 180 && b[i + 1] < 130 && b[i + 2] < 150;
                BOOL isBlue = b[i + 2] > 150 && b[i] < 50 && b[i + 1] < 130;
                if (isRed) red++; if (isBlue) blue++;
                if (inShaft && b[i] > 240 && b[i + 1] > 240 && b[i + 2] > 240) white++;
                if ((isRed || isBlue) && !CGPathContainsPoint(TMLogoSilhouettePath(), NULL, p, NO)) spill++;
            }
            XCTAssertEqual(outsideChanges, 0); XCTAssertEqual(spill, 0); XCTAssertEqual(alphaChanges, 0);
            XCTAssertGreaterThan(red, 800); XCTAssertGreaterThan(blue, 800); XCTAssertGreaterThan(white, 800);
            if (phase.doubleValue == 0.25 || phase.doubleValue == 0.5) XCTAssertGreaterThan(insideChanges, 2000);
            else XCTAssertEqualObjects(first, pixels, @"Loop end and reduced motion must equal phase zero, without seams");
            XCTAssertTrue(CGPathEqualToPath(stationary, metal.path));
            XCTAssertTrue(CATransform3DIsIdentity(metal.transform));
            XCTAssertEqual(metal.animationKeys.count, 0);
            NSLog(@"TM_LOGO dark=%@ phase=%@ still=%d red=%lu blue=%lu white=%lu changed=%lu outside=%lu spill=%lu", dark, phase, still, (unsigned long)red, (unsigned long)blue, (unsigned long)white, (unsigned long)insideChanges, (unsigned long)outsideChanges, (unsigned long)spill);
        }
        UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat]; format.scale = 1;
        [screens addObject:[[[UIGraphicsImageRenderer alloc] initWithSize:self.window.bounds.size format:format] imageWithActions:^(UIGraphicsImageRendererContext *context) {
            [self.window drawViewHierarchyInRect:self.window.bounds afterScreenUpdates:YES];
        }]];
    }
    CGPathRelease(stationary);
    CGPathRelease(shaftFringe);
    UIGraphicsImageRendererFormat *format = [UIGraphicsImageRendererFormat defaultFormat]; format.scale = 1;
    UIImage *sheet = [[[UIGraphicsImageRenderer alloc] initWithSize:CGSizeMake(780, 360) format:format] imageWithActions:^(UIGraphicsImageRendererContext *context) {
        for (NSUInteger theme = 0; theme < 2; theme++) {
            [(theme ? UIColor.blackColor : UIColor.whiteColor) setFill]; UIRectFill(CGRectMake(0, theme * 180, 780, 180));
            NSDictionary *attributes = @{NSFontAttributeName:[UIFont systemFontOfSize:12], NSForegroundColorAttributeName:theme ? UIColor.whiteColor : UIColor.blackColor};
            NSArray *labels = @[@"Phase 0", @"Phase .25", @"Phase .5", @"Loop end", @"Reduce Motion"];
            for (NSUInteger col = 0; col < 5; col++) {
                CGFloat x = col * 130, y = theme * 180;
                [labels[col] drawAtPoint:CGPointMake(x + 5, y + 8) withAttributes:attributes];
                [images[theme * 5 + col] drawInRect:CGRectMake(x + 5, y + 40, 58 * 299.75076 / 513.52234, 58)];
                [images[theme * 5 + col] drawInRect:CGRectMake(x + 48, y + 36, 116 * 299.75076 / 513.52234, 116)];
            }
            [@"Logo / pending" drawAtPoint:CGPointMake(655, theme * 180 + 8) withAttributes:attributes];
            TMLogoBackgroundView *referenceView = [[TMLogoBackgroundView alloc] initWithFrame:CGRectMake(0, 0, 480, 800)];
            [referenceView layoutIfNeeded];
            UIImage *reference = [self artworkImage:referenceView];
            XCTAssertTrue(CGPathEqualToPath(((CAShapeLayer *)referenceView.layer.sublayers.firstObject).path, TMLogoFullPath()));
            XCTAssertGreaterThan([self artworkPixels:reference].length, 0);
            CGFloat s = MIN(65 / reference.size.width, 116 / reference.size.height);
            [reference drawInRect:CGRectMake(655, theme * 180 + 36, reference.size.width * s, reference.size.height * s)];
            [screens[theme] drawInRect:CGRectMake(727, theme * 180 + 35, 49, 105)];
        }
    }];
    NSString *device = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? @"ipad" : @"phone";
    NSString *file = [NSTemporaryDirectory() stringByAppendingPathComponent:[NSString stringWithFormat:@"tagmaster-ios-shared-vector-%@-comparison.jpg", device]];
    XCTAssertTrue([UIImageJPEGRepresentation(sheet, 0.9) writeToFile:file atomically:YES]);
    *reduced = NO; stripes.transform = CATransform3DIdentity;
    self.window.overrideUserInterfaceStyle = UIUserInterfaceStyleLight;
    [NSNotificationCenter.defaultCenter postNotificationName:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
    [self settle];
}

- (void)testBarberPolePendingQueryLifecycle {
    Method method = class_getClassMethod(DPTag.class, @selector(query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:));
    IMP original = method_getImplementation(method);
    Method motion = class_getInstanceMethod(NSClassFromString(@"TMBarberPoleLoadingView"), NSSelectorFromString(@"reduceMotionEnabled"));
    IMP originalMotion = method_getImplementation(motion);
    __block BOOL reduced = NO;
    IMP mockMotion = imp_implementationWithBlock(^BOOL(id view) { return reduced; });
    method_setImplementation(motion, mockMotion);
    dispatch_semaphore_t gate = dispatch_semaphore_create(0);
    XCTestExpectation *entered = [self expectationWithDescription:@"real query pending"];
    __block NSUInteger calls = 0;
    DPTag *tag = [self tag]; tag.title = @"Lost";
    IMP mock = imp_implementationWithBlock(^DPTagQueryResult *(id cls, NSString *text, int number, int start, NSNumber *parts, NSNumber *tracks, NSNumber *sheet, enum DPTagCollection collection, enum DPTagSortOptions sort) {
        calls++;
        XCTAssertEqualObjects(text, @"Lost"); XCTAssertEqual(number, 20); XCTAssertEqual(start, 0);
        if (calls == 1) { [entered fulfill]; dispatch_semaphore_wait(gate, DISPATCH_TIME_FOREVER); }
        if (calls == 2) [NSException raise:@"offline" format:@"fixture"];
        DPTagQueryResult *result = [DPTagQueryResult new];
        result.tags = @[tag]; result.count = 1; result.available = 1; result.start = 0;
        return result;
    });
    method_setImplementation(method, mock);
    DPTagQueryViewController *query = [DPTagQueryViewController new]; query.query = @"Lost";
    @try {
        [query performSelector:NSSelectorFromString(@"fetchResults")];
        [self waitForExpectations:@[entered] timeout:3];
        [query loadViewIfNeeded];
        UIView *pole = [query valueForKey:@"activity"];
        CALayer *stripes = [pole valueForKey:@"stripes"], *frame = [pole valueForKey:@"frameLayer"];
        XCTAssertTrue(query.isLoading); XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
        [self mount:query width:UIScreen.mainScreen.bounds.size.width dark:NO large:YES];
        CABasicAnimation *animation = (id)[stripes animationForKey:@"rotationStripes"];
        XCTAssertNotNil(animation); XCTAssertEqualWithAccuracy(animation.duration, 2, 0.01);
        XCTAssertEqualObjects(animation.keyPath, @"transform.translation.y");
        XCTAssertEqual(frame.animationKeys.count, 0);
        CALayer *cylinder = [pole valueForKey:@"cylinder"];
        XCTAssertEqualObjects(animation.toValue, @216);
        XCTAssertEqualWithAccuracy(TMLoaderStripeStep * TMLoaderPhaseMultiplier, 216, 0.0001);
        XCTAssertEqual(stripes.sublayers.count, 13);
        XCTAssertNotNil(cylinder.mask);
        XCTAssertEqualWithAccuracy(atan2(stripes.superlayer.transform.m12, stripes.superlayer.transform.m11) * 180 / M_PI, 26, 0.0001);
        XCTAssertGreaterThanOrEqual(CGRectGetMaxY(stripes.frame), cylinder.bounds.size.height);
        XCTAssertTrue(CATransform3DIsIdentity(frame.transform));
        XCTAssertEqualObjects(pole.accessibilityLabel, @"Loading tags");
        XCTAssertTrue(pole.isAccessibilityElement);
        UITableView *table = [query valueForKey:@"tagTable"];
        XCTAssertEqual(table.tableFooterView, pole);
        XCTAssertEqualWithAccuracy(pole.bounds.size.width, table.bounds.size.width, 0.5);
        CGRect stationary = frame.frame;
        [self verifyLogoPhases:pole reduced:&reduced];
        // Observe actual movement. A fixed sleep plus screenshot rendering can
        // span a full loop and sample nearly the same phase on a busy runner.
        [self capture:@"tagmaster-ios-shared-vector-light-phase1"];
        XCTAssertNotNil(stripes.presentationLayer);
        CGFloat first = stripes.presentationLayer.transform.m42;
        __block CGFloat second = first;
        [self waitUntil:^BOOL {
            CALayer *presented = stripes.presentationLayer;
            if (!presented) return NO;
            second = presented.transform.m42;
            return fabs(first - second) > 4;
        }];
        [self capture:@"tagmaster-ios-shared-vector-light-phase2"];
        NSLog(@"TM_POLE phase1=%.2f phase2=%.2f frame=%@", first, second, NSStringFromCGRect(frame.frame));
        XCTAssertGreaterThan(fabs(first - second), 4);
        XCTAssertTrue(CGRectEqualToRect(stationary, frame.frame));
        self.window.overrideUserInterfaceStyle = UIUserInterfaceStyleDark;
        [self capture:@"tagmaster-ios-shared-vector-dark-pending"];
        reduced = YES;
        [NSNotificationCenter.defaultCenter postNotificationName:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
        XCTAssertNil([stripes animationForKey:@"rotationStripes"]); XCTAssertTrue(query.isLoading);
        [self capture:@"tagmaster-ios-shared-vector-dark-reduced-motion"];
        reduced = NO;
        [NSNotificationCenter.defaultCenter postNotificationName:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
        XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
        pole.hidden = YES; XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
        pole.hidden = NO; XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
        [NSNotificationCenter.defaultCenter postNotificationName:UIApplicationWillResignActiveNotification object:nil];
        XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
        [NSNotificationCenter.defaultCenter postNotificationName:UIApplicationDidBecomeActiveNotification object:nil];
        XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
        [query beginAppearanceTransition:NO animated:NO]; [query endAppearanceTransition];
        XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
        [query beginAppearanceTransition:YES animated:NO]; [query endAppearanceTransition];
        XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
        CGPoint offset = table.contentOffset;
        table.contentOffset = CGPointMake(0, 1000);
        [pole performSelector:NSSelectorFromString(@"updateAnimation")];
        XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
        table.contentOffset = offset;
        [pole performSelector:NSSelectorFromString(@"updateAnimation")];
        XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
        table.tableFooterView = nil; XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
        table.tableFooterView = pole; [self settle]; XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
        dispatch_semaphore_signal(gate);
        [self waitUntil:^BOOL { return !query.isLoading && query.tags.count == 1; }]; [self settle];
        XCTAssertNil([stripes animationForKey:@"rotationStripes"]); XCTAssertNil(table.tableFooterView);
        [query refresh];
        [self waitUntil:^BOOL { return !query.isLoading && [[query valueForKey:@"failed"] boolValue]; }]; [self settle];
        XCTAssertNil([stripes animationForKey:@"rotationStripes"]); XCTAssertNil(table.tableFooterView);
        [[query valueForKey:@"retryButton"] sendActionsForControlEvents:UIControlEventTouchUpInside];
        [self waitUntil:^BOOL { return calls == 3 && !query.isLoading; }];
        XCTAssertEqual(query.tags.count, 1);
    } @finally {
        dispatch_semaphore_signal(gate);
        [self waitUntil:^BOOL { return !query.isLoading; }];
        method_setImplementation(method, original); imp_removeBlock(mock);
        method_setImplementation(motion, originalMotion); imp_removeBlock(mockMotion);
    }
}

@end

// Keep this regression on the class used by the targeted xcodebuild filter.
@implementation TMPolishRegressionTests (TrackAccessory)
@end
