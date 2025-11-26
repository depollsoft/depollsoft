#import <XCTest/XCTest.h>
#import <UIKit/UIKit.h>
#import <objc/runtime.h>

#import "DPAppDelegate.h"
#import "DPHomeViewController.h"
#import "DPBrowseViewController.h"
#import "DPTagViewController.h"
#import "DPTagSummaryController.h"
#import "DPTagDetailController.h"
#import "DPTagTracksController.h"
#import "DPTagVideoController.h"
#import "DPTagPageControllerBase.h"
#import "DPTagCell.h"
#import "DPBusyIndicator.h"
#import "DPSearchViewController.h"
#import "DPTagQueryViewController.h"

#import "DPTag.h"
#import "DPTagQueryResult.h"
#import "DPRemoteLocation.h"
#import "DPTrack.h"
#import "DPVideo.h"
#import "DPFileCache.h"
#import "DPSettingsController.h"

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

- (void)testTagComponentControllersRefresh {
    DPTag *tag = [self buildSampleTagWithIdentifier:123];
    
    DPTagSummaryController *summary = [[DPTagSummaryController alloc] init];
    (void)summary.view;
    summary.tag = tag;
    [summary.view layoutIfNeeded];
    XCTAssertEqualObjects(summary.view.subviews.firstObject.class, [UIScrollView class]);
    
    DPTagDetailController *detail = [[DPTagDetailController alloc] init];
    (void)detail.view;
    detail.tag = tag;
    [detail.view layoutIfNeeded];
    XCTAssertNotNil([detail.view valueForKeyPath:@"subviews"]);
    
    DPTagTracksController *tracks = [[DPTagTracksController alloc] init];
    (void)tracks.view;
    tracks.tag = tag;
    UITableView *partsTable = [tracks valueForKey:@"partsTable"];
    id<UITableViewDataSource> tracksDataSource = partsTable.dataSource;
    NSInteger trackRowCount = [tracksDataSource tableView:partsTable numberOfRowsInSection:0];
    XCTAssertEqual(trackRowCount, (NSInteger)tag.tracks.count);
    UITableViewCell *trackCell = [tracksDataSource tableView:partsTable cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]];
    XCTAssertNotNil(trackCell);
    
    DPTagVideoController *video = [[DPTagVideoController alloc] init];
    (void)video.view;
    video.tag = tag;
    UITableView *videoTable = [video valueForKey:@"tableView"];
    id<UITableViewDataSource> videoDataSource = videoTable.dataSource;
    NSInteger sectionCount = 1;
    if ([videoDataSource respondsToSelector:@selector(numberOfSectionsInTableView:)]) {
        sectionCount = [videoDataSource numberOfSectionsInTableView:videoTable];
    }
    XCTAssertEqual(sectionCount, 2);
    UITableViewCell *teachCell = [videoDataSource tableView:videoTable cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]];
    XCTAssertNotNil(teachCell);
    UITableViewCell *userCell = [videoDataSource tableView:videoTable cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:1]];
    XCTAssertNotNil(userCell);
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
    XCTAssertEqual([home numberOfSectionsInTableView:table], 2);
    XCTAssertEqual([home tableView:table numberOfRowsInSection:1], 1);
    UITableViewCell *favoriteCell = [home tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:1]];
    XCTAssertNotNil(favoriteCell);
    
    NSArray *items = [home navigationItems];
    XCTAssertTrue(items.count >= 4);
    
    __block DPTag *capturedTag = tag;
    SEL querySelector = @selector(query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:minimumRating:minimumDownloads:cache:fieldList:);
    IMP originalQuery = [self replaceClassMethod:querySelector onClass:[DPTag class] withBlock:^DPTagQueryResult *(Class _self, SEL _cmd, NSString *query, int numberOfResults, int start, NSNumber *parts, NSNumber *learningTracks, NSNumber *sheetMusic, enum DPTagCollection collection, enum DPTagSortOptions sortBy, NSNumber *minRating, NSNumber *minDownloads, BOOL cacheFlag, NSString *fieldList) {
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.available = 1;
        if (numberOfResults == 0) {
            result.count = 0;
            result.tags = @[];
        } else {
            result.count = 1;
            result.tags = @[capturedTag];
        }
        return result;
    }];
    
    IMP originalLoad = [self replaceClassMethod:@selector(loadTagById:refresh:) onClass:[DPTag class] withBlock:^DPTag *(Class _self, SEL _cmd, int identifier, BOOL refresh) {
        return capturedTag;
    }];
    
    NSUInteger randomIndex = [items indexOfObjectPassingTest:^BOOL(NSDictionary *obj, NSUInteger idx, BOOL *stop) {
        return [obj[@"title"] isEqualToString:@"Random Tag"];
    }];
    XCTAssertNotEqual(randomIndex, NSNotFound);
    
    void (^randomAction)(void) = items[randomIndex][@"action"];
    randomAction();
    XCTAssertNotNil([home valueForKey:@"busyIndicator"]);
    
    Method queryMethod = class_getClassMethod([DPTag class], querySelector);
    method_setImplementation(queryMethod, originalQuery);
    Method loadMethod = class_getClassMethod([DPTag class], @selector(loadTagById:refresh:));
    method_setImplementation(loadMethod, originalLoad);
}

- (void)testTagViewControllerLoadsTag {
    DPTag *tag = [self buildSampleTagWithIdentifier:555];
    [tag cache];
    
    DPTagViewController *controller = [[DPTagViewController alloc] init];
    UINavigationController *nav = [[UINavigationController alloc] initWithRootViewController:controller];
    UIWindow *window = [[UIWindow alloc] initWithFrame:CGRectMake(0, 0, 320, 640)];
    window.rootViewController = nav;
    [window makeKeyAndVisible];
    (void)controller.view;
    
    __block DPTag *capturedTag = tag;
    IMP originalLoad = [self replaceClassMethod:@selector(loadTagById:refresh:) onClass:[DPTag class] withBlock:^DPTag *(Class _self, SEL _cmd, int identifier, BOOL refresh) {
        return capturedTag;
    }];
    
    XCTestExpectation *tagExpectation = [self expectationWithDescription:@"tag loaded"];
    [controller addObserver:self
                 forKeyPath:@"tag"
                    options:NSKeyValueObservingOptionNew
                    context:(__bridge void * _Nullable)(tagExpectation)];
    
    controller.tagId = tag.tagId;
    [self waitForExpectations:@[tagExpectation] timeout:1.0];
    [controller removeObserver:self forKeyPath:@"tag"];
    
    Method loadMethod = class_getClassMethod([DPTag class], @selector(loadTagById:refresh:));
    method_setImplementation(loadMethod, originalLoad);
    
    XCTAssertEqualObjects([[controller valueForKey:@"tag"] title], tag.title);
}

- (void)testSearchAndQueryControllersDisplayResults
{
    DPTag *tag = [self buildSampleTagWithIdentifier:777];
    [tag cache];
    
    TestingNavigationController *nav = [[TestingNavigationController alloc] init];
    DPSearchViewController *searchController = [[DPSearchViewController alloc] init];
    nav.viewControllers = @[searchController];
    (void)searchController.view;
    
    UISearchBar *searchBar = [searchController valueForKey:@"searchBar"];
    UISegmentedControl *sortBy = [searchController valueForKey:@"sortBy"];
    UISegmentedControl *sheetMusic = [searchController valueForKey:@"sheetMusic"];
    UISegmentedControl *learningTracks = [searchController valueForKey:@"learningTracks"];
    UISegmentedControl *parts = [searchController valueForKey:@"parts"];
    UISegmentedControl *collection = [searchController valueForKey:@"collection"];

    searchBar.text = @"sample";
    sortBy.selectedSegmentIndex = 3;
    sheetMusic.selectedSegmentIndex = 2;
    learningTracks.selectedSegmentIndex = 1;
    parts.selectedSegmentIndex = 2;
    collection.selectedSegmentIndex = 1;
    
    __block NSInteger queryInvocationCount = 0;
    XCTestExpectation *queryExpectation = [self expectationWithDescription:@"query completed"];
    __block BOOL queryFulfilled = NO;
    IMP originalQuery = [self replaceClassMethod:@selector(query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:) onClass:[DPTag class] withBlock:^DPTagQueryResult *(Class _self, SEL _cmd, NSString *query, int numberOfResults, int start, NSNumber *parts, NSNumber *learningTracks, NSNumber *sheetMusic, enum DPTagCollection collection, enum DPTagSortOptions sortBy) {
        queryInvocationCount += 1;
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.start = start;
        result.count = 1;
        result.available = 5;
        result.tags = @[tag];
        dispatch_async(dispatch_get_main_queue(), ^{
            if (!queryFulfilled) {
                queryFulfilled = YES;
                [queryExpectation fulfill];
            }
        });
        return result;
    }];
    
    IMP originalLoad = [self replaceClassMethod:@selector(loadTagById:refresh:) onClass:[DPTag class] withBlock:^DPTag *(Class _self, SEL _cmd, int identifier, BOOL refresh) {
        return tag;
    }];
    
    #pragma clang diagnostic push
    #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
    [searchController performSelector:@selector(search)];
    #pragma clang diagnostic pop
    
    XCTestExpectation *pushExpectation = [self expectationWithDescription:@"query controller pushed"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.1 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        if (nav.viewControllers.count == 2) {
            [pushExpectation fulfill];
        }
    });
    [self waitForExpectations:@[pushExpectation] timeout:1.0];
    
    XCTAssertEqual(nav.viewControllers.count, 2);
    DPTagQueryViewController *queryController = (DPTagQueryViewController *)nav.topViewController;
    (void)queryController.view;
    
    UITableView *table = [queryController valueForKey:@"tagTable"];
    id<UITableViewDataSource> dataSource = table.dataSource;
    [self waitForExpectations:@[queryExpectation] timeout:1.0];
    
    NSIndexPath *firstRow = [NSIndexPath indexPathForRow:0 inSection:0];
    UITableViewCell *cell = [dataSource tableView:table cellForRowAtIndexPath:firstRow];
    XCTAssertNotNil(cell);
    
    id<UITableViewDelegate> delegate = table.delegate;
    [delegate tableView:table didSelectRowAtIndexPath:firstRow];
    
    XCTAssertEqual(nav.viewControllers.count, 3);
    
    #pragma clang diagnostic push
    #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
    [queryController performSelector:@selector(refresh)];
    #pragma clang diagnostic pop
    NSPredicate *refreshPredicate = [NSPredicate predicateWithBlock:^BOOL(id _, NSDictionary * __unused bindings) {
        return queryInvocationCount >= 2;
    }];
    XCTNSPredicateExpectation *refreshExpectation = [[XCTNSPredicateExpectation alloc] initWithPredicate:refreshPredicate object:nil];
    [self waitForExpectations:@[refreshExpectation] timeout:1.0];
    
    UITableView *postRefreshTable = [queryController valueForKey:@"tagTable"];
    XCTAssertGreaterThan([dataSource tableView:postRefreshTable numberOfRowsInSection:0], 0);
    
    [searchController viewDidDisappear:NO];
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.sortBy"], 3);
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.sheetMusic"], 2);
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.learningTracks"], 1);
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.parts"], 2);
    XCTAssertEqual([[NSUserDefaults standardUserDefaults] integerForKey:@"search.collection"], 1);
    
    Method queryMethod = class_getClassMethod([DPTag class], @selector(query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:));
    method_setImplementation(queryMethod, originalQuery);
    Method loadMethod = class_getClassMethod([DPTag class], @selector(loadTagById:refresh:));
    method_setImplementation(loadMethod, originalLoad);
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
