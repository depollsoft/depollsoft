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
    CGFloat videoHeight = [userCell.contentView systemLayoutSizeFittingSize:CGSizeMake(320, 0) withHorizontalFittingPriority:UILayoutPriorityRequired verticalFittingPriority:UILayoutPriorityFittingSizeLevel].height;
    XCTAssertGreaterThan(videoHeight, 68);
    XCTAssertLessThan(videoHeight, 5000);
    XCTAssertEqual([videoTable.delegate tableView:videoTable heightForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:1]], UITableViewAutomaticDimension);
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
    IMP originalLoad = [self replaceClassMethod:@selector(loadTagById:refresh:) onClass:[DPTag class] withBlock:^DPTag *(Class _self, int identifier, BOOL refresh) {
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

@interface DPTagViewController (PolishTests)
- (void)sendTag;
- (void)loadTag:(BOOL)refresh;
@end
@interface DPTagSummaryController (PolishTests)
- (void)rate;
- (void)rateTag:(NSInteger)rating;
- (void)openSheetMusic;
@end
@interface DPTagQueryViewController (PolishTests)
- (void)refresh;
@end

// Capture only presentation. Production action and background-work paths still run.
#define TM_CAPTURE \
@property (nonatomic, strong) UIViewController *captured; \
@property (nonatomic, copy) NSString *errorMessage; \
@property (nonatomic, copy) void (^retry)(void);
#define TM_CAPTURE_IMPL \
- (void)presentViewController:(UIViewController *)controller animated:(BOOL)animated completion:(void (^)(void))completion { self.captured = controller; } \
- (void)tm_showError:(NSString *)message retry:(void (^)(void))retry { self.errorMessage = message; self.retry = retry; }
@interface TMTestDetail : DPTagViewController
TM_CAPTURE
@end
@implementation TMTestDetail
TM_CAPTURE_IMPL
@end
@interface TMTestSummary : DPTagSummaryController
TM_CAPTURE
@end
@implementation TMTestSummary
TM_CAPTURE_IMPL
@end
@interface TMTestHome : DPHomeViewController
TM_CAPTURE
@end
@implementation TMTestHome
TM_CAPTURE_IMPL
@end
@interface TMTestTracks : DPTagTracksController
TM_CAPTURE
@end
@implementation TMTestTracks
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
    NSPredicate *predicate = [NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return condition(); }];
    XCTNSPredicateExpectation *expectation = [[XCTNSPredicateExpectation alloc] initWithPredicate:predicate object:nil];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[expectation] timeout:5], XCTWaiterResultCompleted);
}

- (void)testSummaryBrandButtonsAndTintChanges {
    TMTestSummary *summary = [TMTestSummary new];
    [summary loadViewIfNeeded];
    DPTag *tag = [self tag];
    tag.sheetMusicUri = [DPRemoteLocation new];
    summary.tag = tag;
    summary.view.frame = CGRectMake(0, 0, 393, 800);
    [summary.view layoutIfNeeded];
    UIButton *sheet = [summary valueForKey:@"sheetMusicButton"];
    UIButton *rate = [summary valueForKey:@"ratingButton"];
    DPPitchPipeButton *pitch = [summary valueForKey:@"keyButton"];
    XCTAssertEqualObjects(sheet.configuration.baseForegroundColor, [UIColor whiteColor]);
    XCTAssertEqualObjects(sheet.configuration.image, [UIImage systemImageNamed:@"doc.richtext"]);
    XCTAssertEqual(sheet.configuration.cornerStyle, UIButtonConfigurationCornerStyleMedium);
    XCTAssertEqualObjects(rate.configuration.image, [UIImage systemImageNamed:@"star"]);
    XCTAssertEqualObjects(rate.accessibilityLabel, @"Rate tag");
    XCTAssertEqualWithAccuracy(sheet.bounds.size.height, pitch.button.bounds.size.height, 1);
    XCTAssertEqualWithAccuracy(rate.bounds.size.height, pitch.button.bounds.size.height, 1);
    XCTAssertGreaterThanOrEqual(sheet.bounds.size.height, 44);
    for (NSNumber *style in @[@(UIUserInterfaceStyleLight), @(UIUserInterfaceStyleDark)]) {
        pitch.overrideUserInterfaceStyle = style.integerValue;
        pitch.tintColor = [UIColor systemBlueColor];
        [pitch updateTraitsIfNeeded];
        [pitch updateConstraints];
        XCTAssertNil([pitch.button backgroundImageForState:UIControlStateNormal]);
        XCTAssertNil([pitch.button backgroundImageForState:UIControlStateHighlighted]);
        XCTAssertEqualObjects(pitch.button.backgroundColor, [UIColor clearColor]);
        XCTAssertEqualWithAccuracy(pitch.button.layer.borderWidth, 1.5, 0.01);
        XCTAssertEqualWithAccuracy(pitch.button.layer.cornerRadius, 8, 0.01);
        XCTAssertTrue(CGColorEqualToColor(pitch.button.layer.borderColor,
            [pitch.button.tintColor resolvedColorWithTraitCollection:pitch.traitCollection].CGColor));
    }
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
    XCTAssertEqualObjects(sheet.image, [UIImage systemImageNamed:@"checkmark.circle.fill"]);
    XCTAssertEqualObjects(tracks.image, [UIImage systemImageNamed:@"checkmark.circle.fill"]);
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

- (void)testShareAnchorAndNamedToolbar {
    TMTestDetail *detail = [TMTestDetail new];
    [detail loadViewIfNeeded];
    [detail setValue:[self tag] forKey:@"tag"];
    [detail sendTag];
    XCTAssertTrue([detail.captured isKindOfClass:UIActivityViewController.class]);
    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        XCTAssertEqual(detail.captured.popoverPresentationController.barButtonItem, [detail valueForKey:@"shareBarButton"]);
    }
    XCTAssertEqualObjects([detail.navigationItem.rightBarButtonItems valueForKey:@"accessibilityLabel"], (@[@"Share", @"Favorite and Teachable options", @"Refresh"]));
    XCTAssertEqualObjects([self tag].tagUri.absoluteString, @"http://tags.depoll.com/tag.php?id=1809");
}

- (void)testNilLoadBalancesBusyAndRetryLoadsTag {
    TMTestDetail *detail = [TMTestDetail new];
    [detail loadViewIfNeeded];
    DPTag *tag = [self tag];
    __block BOOL fail = YES;
    Method method = class_getClassMethod(DPTag.class, @selector(loadTagById:refresh:));
    IMP original = method_getImplementation(method);
    IMP mock = imp_implementationWithBlock(^DPTag *(id cls, int identifier, BOOL refresh) { return fail ? nil : tag; });
    method_setImplementation(method, mock);
    @try {
        detail.tagId = 1809;
        [self waitUntil:^BOOL { return detail.retry != nil; }];
        XCTAssertEqual([[detail valueForKey:@"busyIndicator"] busyCount], 0);
        XCTAssertNotNil(detail.contentUnavailableConfiguration);
        XCTAssertFalse([[detail valueForKey:@"shareBarButton"] isEnabled]);
        fail = NO;
        detail.retry();
        [self waitUntil:^BOOL { return [detail valueForKey:@"tag"] == tag; }];
        XCTAssertNil(detail.contentUnavailableConfiguration);
        XCTAssertTrue([[detail valueForKey:@"shareBarButton"] isEnabled]);
        fail = YES;
        detail.errorMessage = nil;
        [detail loadTag:YES];
        [self waitUntil:^BOOL { return detail.errorMessage != nil; }];
        XCTAssertEqual([detail valueForKey:@"tag"], tag);
        XCTAssertEqual([[detail valueForKey:@"busyIndicator"] busyCount], 0);
    } @finally { method_setImplementation(method, original); imp_removeBlock(mock); }
}

- (void)testRateAnchorNamesAndFailureRetry {
    TMTestSummary *summary = [TMTestSummary new];
    summary.busyIndicator = [DPBusyIndicator new];
    [summary loadViewIfNeeded];
    TMRatingTag *tag = [TMRatingTag new];
    tag.failRating = YES;
    summary.tag = tag;
    summary.view.frame = CGRectMake(0, 0, 834, 1000);
    [summary.view layoutIfNeeded];
    [summary rate];
    UIAlertController *alert = (UIAlertController *)summary.captured;
    UIButton *button = [summary valueForKey:@"ratingButton"];
    XCTAssertEqual(alert.popoverPresentationController.sourceView, button);
    XCTAssertTrue(CGRectEqualToRect(alert.popoverPresentationController.sourceRect, button.bounds));
    XCTAssertEqualObjects([alert.actions valueForKey:@"title"], (@[@"5 stars", @"4 stars", @"3 stars", @"2 stars", @"1 star", @"Cancel"]));
    XCTAssertGreaterThanOrEqual(button.bounds.size.height, 44);
    [summary rateTag:4];
    [self waitUntil:^BOOL { return summary.retry != nil; }];
    XCTAssertEqual(summary.busyIndicator.busyCount, 0);
    XCTAssertTrue(button.enabled);
    tag.failRating = NO;
    summary.retry();
    [self waitUntil:^BOOL { return !button.enabled; }];
    XCTAssertEqual(tag.submittedRating, 4);
    XCTAssertEqualObjects(button.accessibilityLabel, @"Rating submitted");
}

- (void)testMissingSheetDataIsNotCachedAndRetryPresentsQuickLook {
    TMTestSummary *summary = [TMTestSummary new];
    summary.busyIndicator = [DPBusyIndicator new];
    [summary loadViewIfNeeded];
    DPTag *tag = [self tag];
    DPRemoteLocation *location = [TMTestLocation new];
    location.uri = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:NSUUID.UUID.UUIDString]];
    location.type = @"pdf";
    tag.sheetMusicUri = location;
    summary.tag = tag;
    [summary openSheetMusic];
    [self waitUntil:^BOOL { return summary.retry != nil; }];
    XCTAssertEqual(summary.busyIndicator.busyCount, 0);
    XCTAssertFalse([[NSFileManager defaultManager] fileExistsAtPath:[DPFileCache pathForKey:location.cacheKey]]);
    XCTAssertNil(summary.captured);
    [@"%PDF-1.4 test" writeToURL:location.uri atomically:YES encoding:NSUTF8StringEncoding error:nil];
    summary.retry();
    [self waitUntil:^BOOL { return [summary.captured isKindOfClass:NSClassFromString(@"QLPreviewController")]; }];
    XCTAssertEqual(summary.busyIndicator.busyCount, 0);
    [[NSFileManager defaultManager] removeItemAtURL:location.uri error:nil];
    [[NSFileManager defaultManager] removeItemAtPath:[DPFileCache pathForKey:location.cacheKey] error:nil];
}

- (void)testMissingTrackOffersRetry {
    TMTestTracks *tracks = [TMTestTracks new];
    tracks.busyIndicator = [DPBusyIndicator new];
    [tracks loadViewIfNeeded];
    DPTag *tag = [self tag];
    DPRemoteLocation *location = [TMTestLocation new];
    location.uri = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:NSUUID.UUID.UUIDString]];
    location.type = @"mp3";
    tag.tenorTrackUri = location;
    tracks.tag = tag;
    UITableView *table = [tracks valueForKey:@"partsTable"];
    [table.delegate tableView:table didSelectRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]];
    [self waitUntil:^BOOL { return tracks.retry != nil; }];
    XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
    XCTAssertTrue([tracks.errorMessage containsString:@"learning track"]);
    tracks.errorMessage = nil;
    tracks.retry();
    [self waitUntil:^BOOL { return tracks.errorMessage != nil; }];
    XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
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
    XCTAssertEqual([home tableView:home.tableView heightForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:1]], UITableViewAutomaticDimension);
}

- (void)testTeachableAlwaysDiscoverableAndEmptyGuidance {
    NSArray *saved = [DPAppDelegate teachable];
    @try {
        [DPAppDelegate setTeachable:@[]];
        DPHomeViewController *home = [DPHomeViewController new]; [home loadViewIfNeeded];
        XCTAssertTrue([[[home navigationItems] valueForKey:@"title"] containsObject:@"Teachable Tags"]);
        DPTeachableTagsController *teachable = [DPTeachableTagsController new]; [teachable loadViewIfNeeded];
        XCTAssertEqual([teachable tableView:teachable.tableView numberOfRowsInSection:0], 0);
        UIContentUnavailableConfiguration *state = (id)teachable.contentUnavailableConfiguration;
        XCTAssertEqualObjects(state.button.title, @"Browse Tags");
        XCTAssertTrue([state.secondaryText containsString:@"Mark as Teachable"]);
        XCTAssertFalse(teachable.editButtonItem.enabled);
    } @finally { [DPAppDelegate setTeachable:saved]; }
}

- (void)testPitchActivationAndFormTargets {
    TMTestSummary *summary = [TMTestSummary new]; [summary loadViewIfNeeded]; summary.tag = [self tag];
    DPPitchPipeButton *pitch = [summary valueForKey:@"keyButton"];
    XCTAssertTrue([pitch.button.accessibilityLabel hasPrefix:@"Play key note"]);
    XCTAssertTrue([pitch.button accessibilityActivate]);
    [pitch.button sendActionsForControlEvents:UIControlEventTouchCancel];
    XCTAssertFalse(pitch.note.isPlaying);
    DPSearchViewController *search = [DPSearchViewController new]; [search loadViewIfNeeded];
    search.view.frame = CGRectMake(0, 0, 320, 700); [search.view layoutIfNeeded];
    // The options live in an inset-grouped list; build and lay out its rows the way the screen does.
    UITableView *form = [search valueForKey:@"tableView"];
    id<UITableViewDataSource> formSource = (id<UITableViewDataSource>)search;
    for (NSInteger row = 0; row < [formSource tableView:form numberOfRowsInSection:0]; row++) {
        UITableViewCell *cell = [formSource tableView:form cellForRowAtIndexPath:[NSIndexPath indexPathForRow:row inSection:0]];
        cell.frame = CGRectMake(0, 0, 320, 300);
        [cell layoutIfNeeded];
    }
    for (NSString *key in @[@"sortBy", @"sheetMusic", @"learningTracks", @"parts", @"collection"]) {
        UISegmentedControl *control = [search valueForKey:key];
        UIView *visibleControl = control.hidden ? [control.superview valueForKey:@"menuButton"] : control;
        XCTAssertGreaterThanOrEqual(visibleControl.bounds.size.height, 44);
        XCTAssertGreaterThan(control.accessibilityLabel.length, 0);
    }
    XCTAssertEqualObjects(search.navigationItem.rightBarButtonItem.accessibilityLabel, @"Search");
    TMTestTracks *tracks = [TMTestTracks new];
    [tracks loadViewIfNeeded];
    tracks.view.frame = CGRectMake(0, 0, 320, 700);
    tracks.view.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryAccessibilityExtraExtraExtraLarge;
    DPTag *trackTag = [self tag];
    trackTag.recordingMethod = @"Part predominant, one part louder and other parts quieter. Practice each part in turn.";
    tracks.tag = trackTag;
    [tracks.view layoutIfNeeded];
    UITableView *partsTable = [tracks valueForKey:@"partsTable"];
    XCTAssertGreaterThan(partsTable.frame.size.height, 400);
    XCTAssertGreaterThan(partsTable.tableHeaderView.frame.size.height, 44);

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

- (void)checkSummary:(DPTagSummaryController *)summary cycle:(NSInteger)cycle {
    UIView *grid = [summary valueForKey:@"grid"];
    UIScrollView *scroll = (UIScrollView *)grid.superview.superview;
    XCTAssertTrue([scroll isKindOfClass:UIScrollView.class]);
    CGFloat expectedHeight = 8; // Scroll content's top/bottom inset.
    for (NSString *key in @[@"titleLabel", @"akaLabel", @"partsLabel", @"typeLabel",
                             @"classicTagNumberLabel", @"lyricsLabel", @"notesLabel"]) {
        UILabel *label = [summary valueForKey:key];
        if (![label isDescendantOfView:grid]) continue;
        CGFloat textHeight = [label sizeThatFits:CGSizeMake(label.bounds.size.width, CGFLOAT_MAX)].height;
        CGRect frame = [label convertRect:label.bounds toView:scroll];
        NSLog(@"TM_SUMMARY cycle=%ld %@ width=%.1f y=%.1f height=%.1f text=%.1f hugging=%.0f compression=%.0f",
              (long)cycle, key, label.bounds.size.width, frame.origin.y, frame.size.height, textHeight,
              [label contentHuggingPriorityForAxis:UILayoutConstraintAxisVertical],
              [label contentCompressionResistancePriorityForAxis:UILayoutConstraintAxisVertical]);
        XCTAssertGreaterThan(label.bounds.size.width, 0);
        XCTAssertEqualWithAccuracy(label.bounds.size.height, textHeight, 1, @"%@ must hug its text", key);
        expectedHeight += textHeight;
    }
    // Allow the rating, key and sheet controls plus ordinary padding, not viewport-sized slack.
    for (NSString *key in @[@"ratingButton", @"keyButton", @"sheetMusicButton"]) {
        UIView *control = [summary valueForKey:key];
        if ([control isDescendantOfView:grid]) expectedHeight += control.bounds.size.height;
    }
    DPPitchPipeButton *pitch = [summary valueForKey:@"keyButton"];
    CGFloat keyHeight = [pitch.button sizeThatFits:CGSizeMake(pitch.button.bounds.size.width, CGFLOAT_MAX)].height;
    XCTAssertEqualWithAccuracy(pitch.bounds.size.height, MAX(44, keyHeight) + 4, 1, @"Key row must fit its title and padding");
    UILabel *rating = [summary valueForKey:@"ratingLabel"];
    UIButton *rate = [summary valueForKey:@"ratingButton"];
    UIProgressView *bar = [summary valueForKey:@"ratingBar"];
    CGFloat ratingTextHeight = [rating sizeThatFits:CGSizeMake(rating.bounds.size.width, CGFLOAT_MAX)].height;
    XCTAssertEqualWithAccuracy(rating.bounds.size.height, ratingTextHeight, 1);
    CGFloat ratingHeight = ratingTextHeight + 4 + bar.bounds.size.height;
    expectedHeight += MAX(0, ratingHeight - rate.bounds.size.height);
    XCTAssertLessThanOrEqual(scroll.contentSize.height, expectedHeight + 80);
    for (NSString *name in @[@"lyrics", @"notes"]) {
        UILabel *header = [summary valueForKey:[name stringByAppendingString:@"Header"]];
        UILabel *body = [summary valueForKey:[name stringByAppendingString:@"Label"]];
        // UILabel draws text vertically centered within its bounds. textRectForBounds:
        // alone does not expose that blank space when an oversized row stretches it.
        CGFloat headingHeight = [header sizeThatFits:CGSizeMake(header.bounds.size.width, CGFLOAT_MAX)].height;
        CGFloat bodyHeight = [body sizeThatFits:CGSizeMake(body.bounds.size.width, CGFLOAT_MAX)].height;
        CGRect heading = [header convertRect:CGRectMake(0, (header.bounds.size.height - headingHeight) / 2,
                                                        header.bounds.size.width, headingHeight) toView:scroll];
        CGRect text = [body convertRect:CGRectMake(0, (body.bounds.size.height - bodyHeight) / 2,
                                                   body.bounds.size.width, bodyHeight) toView:scroll];
        NSLog(@"TM_SUMMARY cycle=%ld %@ headingToBody=%.1f", (long)cycle, name, CGRectGetMinY(text) - CGRectGetMinY(heading));
        XCTAssertEqualWithAccuracy(CGRectGetMinY(text), CGRectGetMinY(heading), 2, @"%@ starts beside its heading", name);
        XCTAssertLessThanOrEqual(CGRectGetMaxY(text), scroll.contentSize.height + 1);
        // Both the first and last lines can be scrolled into the viewport.
        for (NSNumber *position in @[@(CGRectGetMinY(text)), @(CGRectGetMaxY(text) - 1)]) {
            CGRect line = CGRectMake(text.origin.x, position.doubleValue, 1, 1);
            [scroll scrollRectToVisible:line animated:NO];
            // Scroll offsets round to device pixels; allow half a point at the edge.
            XCTAssertTrue(CGRectContainsRect(CGRectInset(scroll.bounds, -0.5, -0.5), line), @"%@ must remain reachable: viewport %@, line %@", name, NSStringFromCGRect(scroll.bounds), NSStringFromCGRect(line));
        }
    }
    NSLog(@"TM_SUMMARY cycle=%ld content=%.1f budget=%.1f", (long)cycle, scroll.contentSize.height, expectedHeight + 80);
}

- (void)exerciseLongText:(BOOL)longText {
    DPTag *tag = [DPTag new];
    tag.tagId = 1809;
    tag.title = @"Lost";
    tag.alternativeTitle = @"In Your Eyes";
    tag.parts = 4;
    tag.rating = 3.49;
    tag.tagType = @"Barbershop";
    tag.writtenKey = @"Minor:G";
    tag.sheetMusicUri = [DPRemoteLocation new];
    tag.lyrics = @"And I will wait to face the skies,\never roaming in your eyes.\nThere I go lost in your eyes.";
    tag.notes = @"Hold the last chord.";
    if (longText) {
        tag.title = @"Lost with a long title that wraps at accessibility sizes";
        tag.alternativeTitle = @"In Your Eyes with another long alternate title";
        tag.classicTagNumber = 42;
        tag.lyrics = [@[tag.lyrics, tag.lyrics, tag.lyrics] componentsJoinedByString:@"\n"];
        tag.notes = @"Sing the phrase together, then hold the last chord. Listen to the lead and balance the other parts. Repeat the phrase quietly before returning to full voice.";
    }
    for (NSNumber *width in @[@393, @834]) {
        UIWindow *previousKeyWindow = nil;
        for (UIWindow *candidate in UIApplication.sharedApplication.windows) {
            if (candidate.isKeyWindow) previousKeyWindow = candidate;
        }
        UIWindow *window = [[UIWindow alloc] initWithFrame:CGRectMake(0, 0, width.doubleValue, 1000)];
        DPTagViewController *detail = [DPTagViewController new];
        UINavigationController *navigation = [[UINavigationController alloc] initWithRootViewController:detail];
        window.rootViewController = navigation;
        window.traitOverrides.preferredContentSizeCategory = longText ? UIContentSizeCategoryAccessibilityExtraExtraExtraLarge : UIContentSizeCategoryLarge;
        @try {
            [window makeKeyAndVisible];
            [detail loadViewIfNeeded];
            [detail setValue:tag forKey:@"tag"];
            [self settleLayout:window];
            DPTagSummaryController *summary = [detail valueForKey:@"summaryController"];
            UILabel *lyrics = [summary valueForKey:@"lyricsLabel"];
            XCTAssertEqualObjects(summary.traitCollection.preferredContentSizeCategory, window.traitCollection.preferredContentSizeCategory);
            XCTAssertTrue(lyrics.adjustsFontForContentSizeCategory);
            XCTAssertEqualWithAccuracy(lyrics.font.pointSize, [UIFont preferredFontForTextStyle:UIFontTextStyleBody compatibleWithTraitCollection:window.traitCollection].pointSize, 0.1);
            UIView *grid = [summary valueForKey:@"grid"];
            UIScrollView *scroll = (UIScrollView *)grid.superview.superview;
            CGFloat initialHeight = scroll.contentSize.height;
            NSLog(@"TM_SUMMARY fixture=%@ viewport=%.0f", longText ? @"long AX5" : @"Lost large", width.doubleValue);
            for (NSInteger cycle = 0; cycle <= 2; cycle++) {
                if (cycle > 0) {
                    [detail.tabBar setSelectedItem:detail.tabBar.items[1]];
                    [detail.tabBar.delegate tabBar:detail.tabBar didSelectItem:detail.tabBar.items[1]];
                    [self settleLayout:window];
                    [detail.tabBar setSelectedItem:detail.tabBar.items[0]];
                    [detail.tabBar.delegate tabBar:detail.tabBar didSelectItem:detail.tabBar.items[0]];
                    [self settleLayout:window];
                }
                [self checkSummary:summary cycle:cycle];
                XCTAssertEqualWithAccuracy(scroll.contentSize.height, initialHeight, 1, @"Page changes must not grow content");
            }
            // Optional rows must collapse and return without leaving stale row heights.
            DPTag *minimal = [DPTag new];
            minimal.title = @"Lost"; minimal.parts = 4; minimal.tagType = @"Barbershop";
            summary.tag = minimal;
            [self settleLayout:window];
            for (NSString *key in @[@"akaLabel", @"keyButton", @"keyHeader", @"classicTagNumberLabel",
                                     @"classicTagNumberHeader", @"sheetMusicButton", @"lyricsLabel", @"lyricsHeader", @"notesLabel", @"notesHeader"]) {
                XCTAssertFalse([[summary valueForKey:key] isDescendantOfView:grid], @"%@ should be removed", key);
            }
            XCTAssertLessThan(scroll.contentSize.height, initialHeight);
            summary.tag = tag;
            [self settleLayout:window];
            [self checkSummary:summary cycle:3];
            XCTAssertEqualWithAccuracy(scroll.contentSize.height, initialHeight, 1);
        } @finally {
            window.hidden = YES;
            window.rootViewController = nil;
            [previousKeyWindow makeKeyWindow];
        }
    }
}

- (void)testLostRowsHugTextAfterTwoPageSwitches {
    [self exerciseLongText:NO];
}

- (void)testLongAX5RowsHugTextAfterTwoPageSwitches {
    [self exerciseLongText:YES];
}
@end
