#import <XCTest/XCTest.h>
#import <UIKit/UIKit.h>
#import <objc/runtime.h>

#import "DPAppDelegate.h"
#import "DPHomeViewController.h"
#import "DPBrowseViewController.h"
#import "DPTagViewController.h"
#import "DPTagSummaryController.h"
#import "DPTagDetailController.h"
#import "TMDetailLayout.h"
#import "DPTagTracksController.h"
#import "DPTagVideoController.h"
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
- (void)fetchTagId:(int)identifier refresh:(BOOL)refresh completion:(void (^)(DPTag *))completion;
- (void)updateLoadingState;
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

- (void)testInitialPendingBeforeViewHidesPlaceholderPages {
    Method method = class_getClassMethod(DPTag.class, @selector(loadTagById:refresh:));
    IMP original = method_getImplementation(method);
    IMP mock = imp_implementationWithBlock(^DPTag *(id cls, int identifier, BOOL refresh) { return nil; });
    method_setImplementation(method, mock);
    TMTestDetail *detail = [TMTestDetail new];
    @try {
        detail.tagId = 1809;
        XCTAssertFalse(detail.isViewLoaded);
        [detail loadViewIfNeeded];
        XCTAssertTrue([[detail valueForKey:@"rootView"] isHidden]);
        XCTAssertTrue(detail.tabBar.hidden);
        XCTAssertEqual(detail.navigationItem.rightBarButtonItems.count, 0);
        [self waitUntil:^BOOL { return [[detail valueForKey:@"busyIndicator"] busyCount] == 0; }];
    } @finally { method_setImplementation(method, original); imp_removeBlock(mock); }
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
        [self waitUntil:^BOOL { return [[detail valueForKey:@"loadFailed"] boolValue]; }];
        XCTAssertEqual([[detail valueForKey:@"busyIndicator"] busyCount], 0);
        XCTAssertFalse([[detail valueForKey:@"retryButton"] isHidden]);
        XCTAssertFalse([[detail valueForKey:@"shareBarButton"] isEnabled]);
        fail = NO;
        [[detail valueForKey:@"retryButton"] sendActionsForControlEvents:UIControlEventTouchUpInside];
        [self waitUntil:^BOOL { return [detail valueForKey:@"tag"] == tag; }];
        XCTAssertNil(detail.contentUnavailableConfiguration);
        XCTAssertTrue([[detail valueForKey:@"shareBarButton"] isEnabled]);
        fail = YES;
        detail.errorMessage = nil;
        [detail beginAppearanceTransition:YES animated:NO];
        [detail endAppearanceTransition];
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
- (void)setUp {
    [super setUp];
    self.requests = [NSMutableArray array];
    __weak TMLoadingRegressionTests *weakSelf = self;
    Method fetch = class_getInstanceMethod(DPTagViewController.class, @selector(fetchTagId:refresh:completion:));
    self.originalFetch = method_getImplementation(fetch);
    self.fetchMock = imp_implementationWithBlock(^(DPTagViewController *controller, int identifier, BOOL refresh, void (^completion)(DPTag *)) {
        TMLoadingRegressionTests *test = weakSelf;
        [test.requests addObject:[@{@"id": @(identifier), @"refresh": @(refresh), @"completion": [completion copy]} mutableCopy]];
        if (test.immediateTag) {
            completion(test.immediateTag);
            test.requests.lastObject[@"completion"] = NSNull.null;
        }
    });
    method_setImplementation(fetch, self.fetchMock);
    Method motion = class_getInstanceMethod(NSClassFromString(@"TMQuartetStaffView"), NSSelectorFromString(@"reduceMotionEnabled"));
    self.originalMotion = method_getImplementation(motion);
    self.motionMock = imp_implementationWithBlock(^BOOL(id view) { return weakSelf.reduceMotion; });
    method_setImplementation(motion, self.motionMock);
}
- (void)tearDown {
    self.window.hidden = YES;
    self.window.rootViewController = nil;
    self.window = nil;
    [self.previousKeyWindow makeKeyWindow];
    method_setImplementation(class_getInstanceMethod(DPTagViewController.class, @selector(fetchTagId:refresh:completion:)), self.originalFetch);
    method_setImplementation(class_getInstanceMethod(NSClassFromString(@"TMQuartetStaffView"), NSSelectorFromString(@"reduceMotionEnabled")), self.originalMotion);
    imp_removeBlock(self.fetchMock);
    imp_removeBlock(self.motionMock);
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
// Let UIKit finish scheduled appearance/layout transactions, not a timed loading delay.
- (void)drainUIKit {
    XCTestExpectation *turn = [self expectationWithDescription:@"UIKit transaction turn"];
    dispatch_async(dispatch_get_main_queue(), ^{
        dispatch_async(dispatch_get_main_queue(), ^{ [turn fulfill]; });
    });
    [self waitForExpectations:@[turn] timeout:2];
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
    XCTNSPredicateExpectation *rootShown = [[XCTNSPredicateExpectation alloc] initWithPredicate:
        [NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return observer.shown == previous; }] object:nil];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[rootShown] timeout:3], XCTWaiterResultCompleted);
    [navigation pushViewController:detail animated:NO];
    __weak TMPageViewController *weakDetail = detail;
    XCTNSPredicateExpectation *visible = [[XCTNSPredicateExpectation alloc] initWithPredicate:
        [NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) {
            return observer.shown == weakDetail && [[weakDetail valueForKey:@"appeared"] boolValue] && !weakDetail.transitionCoordinator;
        }] object:nil];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[visible] timeout:3], XCTWaiterResultCompleted);
    navigation.delegate = nil;
    [self layout];
    return navigation;
}
- (void)assertNotes:(DPTagViewController *)detail animated:(BOOL)animated {
    NSArray<CALayer *> *notes = [[detail valueForKey:@"quartetStaff"] valueForKey:@"notes"];
    XCTAssertEqual(notes.count, 4);
    for (CALayer *note in notes) XCTAssertEqual(note.animationKeys.count, animated ? 1 : 0);
}
- (void)assertPending:(DPTagViewController *)detail {
    XCTAssertNil([detail valueForKey:@"tag"]);
    XCTAssertTrue([[detail valueForKey:@"tagFetchPending"] boolValue]);
    UIView *root = [detail valueForKey:@"rootView"];
    XCTAssertTrue(root.hidden);
    XCTAssertTrue(root.accessibilityElementsHidden);
    XCTAssertTrue(detail.tabBar.hidden);
    XCTAssertTrue(detail.tabBar.accessibilityElementsHidden);
    XCTAssertFalse([[detail valueForKey:@"initialLoadingView"] isHidden]);
    XCTAssertEqualObjects([[detail valueForKey:@"loadingStatus"] text], ([NSString stringWithFormat:@"Loading tag %d…", detail.tagId]));
    XCTAssertEqualObjects([[detail valueForKey:@"loadingHeading"] text], @"Gathering the quartet…");
    XCTAssertTrue([[detail valueForKey:@"quartetStaff"] accessibilityElementsHidden]);
    XCTAssertEqual(detail.navigationItem.rightBarButtonItems.count, 0);
    XCTAssertFalse([[detail valueForKey:@"shareBarButton"] isEnabled]);
    XCTAssertFalse([[detail valueForKey:@"actionBarButton"] isEnabled]);
    for (DPTagPageControllerBase *page in detail.viewControllers) XCTAssertNil(page.tag);
}
- (void)assertLoaded:(DPTagViewController *)detail tag:(DPTag *)tag {
    XCTAssertEqual([detail valueForKey:@"tag"], tag);
    XCTAssertTrue([[detail valueForKey:@"initialLoadingView"] isHidden]);
    XCTAssertFalse([[detail valueForKey:@"rootView"] isHidden]);
    XCTAssertFalse([[detail valueForKey:@"rootView"] accessibilityElementsHidden]);
    XCTAssertFalse(detail.tabBar.hidden);
    XCTAssertEqual(detail.navigationItem.rightBarButtonItems.count, 3);
    XCTAssertTrue([[detail valueForKey:@"shareBarButton"] isEnabled]);
    XCTAssertTrue([[detail valueForKey:@"actionBarButton"] isEnabled]);
    for (DPTagPageControllerBase *page in detail.viewControllers) XCTAssertEqual(page.tag, tag);
    [self assertNotes:detail animated:NO];
}
- (void)testMatchedQuartetNativeFramesAndGlassPendingJourney {
    NSString *device = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? @"ipad" : @"phone";
    NSString *directory = [NSTemporaryDirectory() stringByAppendingPathComponent:@"quartet-frames"];
    [NSFileManager.defaultManager createDirectoryAtPath:directory withIntermediateDirectories:YES attributes:nil error:nil];
    for (NSNumber *style in @[@(UIUserInterfaceStyleLight), @(UIUserInterfaceStyleDark)]) {
        NSString *theme = style.integerValue == UIUserInterfaceStyleDark ? @"dark" : @"light";
        DPTagViewController *detail = [DPTagViewController new];
        detail.tagId = 1809;
        NSUInteger request = self.requests.count - 1;
        [self attach:detail size:UIScreen.mainScreen.bounds.size style:style.integerValue large:NO];
        [NSNotificationCenter.defaultCenter postNotificationName:UIApplicationDidBecomeActiveNotification object:nil];
        [self assertPending:detail];
        [self assertNotes:detail animated:YES];
        NSArray<CALayer *> *liveNotes = [[detail valueForKey:@"quartetStaff"] valueForKey:@"notes"];
        CFTimeInterval start = [[liveNotes.firstObject animationForKey:@"gather"] beginTime];
        for (NSUInteger i = 0; i < liveNotes.count; i++) {
            CAKeyframeAnimation *animation = (CAKeyframeAnimation *)[liveNotes[i] animationForKey:@"gather"];
            XCTAssertEqualObjects(animation.values, TMQuartetSamples(i));
            XCTAssertEqualObjects(animation.keyPath, @"opacity");
            XCTAssertEqualWithAccuracy(TMQuartetMidi[i], ((CGFloat[]){60, 64, 67, 70})[i], .001);
            XCTAssertEqual(liveNotes.count, 4);
            XCTAssertTrue(CGPathEqualToPath(((CAShapeLayer *)liveNotes[i]).path, TMQuartetNotePath()));
            XCTAssertFalse(CGPathIsEmpty(TMQuartetFlatPath()));
            XCTAssertFalse(CGPathIsEmpty(TMQuartetLedgerPath()));
            XCTAssertEqualObjects(animation.calculationMode, kCAAnimationLinear);
            XCTAssertEqualWithAccuracy(animation.duration, 2.8, 0.00001);
            XCTAssertEqual(animation.beginTime, start);
            XCTAssertNil(animation.timingFunctions);
            XCTAssertEqualObjects(animation.values.firstObject, animation.values.lastObject);
        }
        [self capture:[NSString stringWithFormat:@"tagmaster-quartet-matched-ios-%@-%@-pending", device, theme]];
        UIView *staff = [[NSClassFromString(@"TMQuartetStaffView") alloc] initWithFrame:CGRectZero];
        staff.overrideUserInterfaceStyle = style.integerValue;
        [staff updateTraitsIfNeeded];
        for (NSNumber *scale in @[@1, @10]) {
            CGSize size = CGSizeMake(TMQuartetWidth * scale.integerValue, TMQuartetHeight * scale.integerValue);
            staff.frame = (CGRect){CGPointZero, size};
            [staff setNeedsLayout]; [staff layoutIfNeeded];
            for (NSString *label in @[@"0", @"0.125", @"0.25", @"0.5", @"0.75", @"1", @"still"]) {
                NSArray<CALayer *> *notes = [staff valueForKey:@"notes"];
                [CATransaction begin]; [CATransaction setDisableActions:YES];
                for (NSUInteger i = 0; i < notes.count; i++) {
                    NSArray<NSNumber *> *values = TMQuartetSamples(i);
                    CGFloat position = label.doubleValue * (values.count - 1);
                    NSUInteger index = MIN((NSUInteger)position, values.count - 2);
                    CGFloat y = [label isEqualToString:@"still"] ? TMQuartetStill[i] : values[index].doubleValue + (values[index + 1].doubleValue - values[index].doubleValue) * (position - index);
                    notes[i].opacity = y;
                    XCTAssertEqualWithAccuracy(notes[i].position.x, 108, .001);
                    XCTAssertEqualWithAccuracy(notes[i].position.y, 77 - 12 * i, .001);
                    XCTAssertTrue(CATransform3DIsIdentity(notes[i].transform));
                }
                [CATransaction commit];
                CGColorSpaceRef space = CGColorSpaceCreateWithName(kCGColorSpaceSRGB);
                CGContextRef context = CGBitmapContextCreate(NULL, size.width, size.height, 8, 0, space, kCGImageAlphaPremultipliedLast | kCGBitmapByteOrder32Big);
                CGContextTranslateCTM(context, 0, size.height); CGContextScaleCTM(context, 1, -1);
                [staff.layer renderInContext:context];
                CGImageRef image = CGBitmapContextCreateImage(context);
                NSData *png = UIImagePNGRepresentation([UIImage imageWithCGImage:image]);
                NSString *name = [NSString stringWithFormat:@"ios-%@-%@-%@-%@.png", device, theme, scale, label];
                XCTAssertTrue([png writeToFile:[directory stringByAppendingPathComponent:name] atomically:YES]);
                CGImageRelease(image); CGContextRelease(context); CGColorSpaceRelease(space);
            }
        }
        [self finish:request tag:[self tag:1809]];
        [self assertLoaded:detail tag:[detail valueForKey:@"tag"]];
        [self drainUIKit]; [self layout];
        [self checkTabDistribution:detail];
        [self capture:[NSString stringWithFormat:@"tagmaster-ios-glass-tabs-%@-%@-summary", device, theme]];
        [self exercisePageSelection:detail];
        self.window.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryAccessibilityExtraExtraExtraLarge;
        [self drainUIKit]; [self layout]; [self layout];
        [self checkTabDistribution:detail];
        [self capture:[NSString stringWithFormat:@"tagmaster-ios-glass-tabs-%@-%@-ax5", device, theme]];
        // Material and accessibility policy now belong to UIKit, not a swizzled app effect.
        XCTAssertTrue([detail.tabBar isKindOfClass:UITabBar.class]);
    }
}

- (void)testPreViewPendingDelayedSuccessAndSingleAnnouncement {
    DPTagViewController *detail = [DPTagViewController new];
    detail.tagId = 1809;
    XCTAssertFalse(detail.isViewLoaded);
    [self attach:detail size:UIScreen.mainScreen.bounds.size style:UIUserInterfaceStyleLight large:NO];
    [self assertPending:detail];
    XCTAssertEqual(self.requests.count, 1);
    [detail loadTag:NO];
    detail.tagId = 1809;
    XCTAssertEqual(self.requests.count, 1, @"Mounting or repeating the same pending request must not fetch twice");
    // Programmatic selection cannot expose unpopulated pages over the quartet.
    for (NSUInteger index = 0; index < 4; index++) {
        detail.selectedIndex = index;
        [self drainUIKit]; [self layout];
        [self assertPending:detail];
    }
    detail.selectedIndex = 0;
    NSUInteger announced = [[detail valueForKey:@"announcedGeneration"] unsignedIntegerValue];
    XCTAssertGreaterThan(announced, 0);
    [detail updateLoadingState];
    [[detail valueForKey:@"busyIndicator"] incrementBusyCount];
    [[detail valueForKey:@"busyIndicator"] decrementBusyCount];
    XCTAssertEqual([[detail valueForKey:@"announcedGeneration"] unsignedIntegerValue], announced);
    DPTag *tag = [self tag:1809];
    [self finish:0 tag:tag];
    [self assertLoaded:detail tag:tag];
    XCTAssertEqual([[detail valueForKey:@"busyIndicator"] busyCount], 0);
}
- (void)testSynchronousCacheSuccessBeforeViewNeverShowsLoader {
    self.immediateTag = [self tag:1809];
    DPTagViewController *detail = [DPTagViewController new];
    detail.tagId = 1809;
    XCTAssertFalse(detail.isViewLoaded);
    [detail loadViewIfNeeded];
    [self assertLoaded:detail tag:self.immediateTag];
    XCTAssertEqual([[detail valueForKey:@"announcedGeneration"] unsignedIntegerValue], 0);
}
- (void)testFailureBeforeMountAndInlineRetryUsesCurrentID {
    DPTagViewController *detail = [DPTagViewController new];
    detail.tagId = 1809;
    [self finish:0 tag:nil];
    XCTAssertFalse(detail.isViewLoaded);
    [self attach:detail size:UIScreen.mainScreen.bounds.size style:UIUserInterfaceStyleLight large:NO];
    XCTAssertEqualObjects([[detail valueForKey:@"loadingHeading"] text], @"Tag unavailable");
    XCTAssertFalse([[detail valueForKey:@"retryButton"] isHidden]);
    XCTAssertTrue([[detail valueForKey:@"rootView"] accessibilityElementsHidden]);
    [self assertNotes:detail animated:NO];
    [[detail valueForKey:@"retryButton"] sendActionsForControlEvents:UIControlEventTouchUpInside];
    XCTAssertEqualObjects(self.requests.lastObject[@"id"], @1809);
    [self assertPending:detail];
    [self finish:1 tag:nil];
    XCTAssertFalse([[detail valueForKey:@"retryButton"] isHidden]);
    [self assertNotes:detail animated:NO];
    [[detail valueForKey:@"retryButton"] sendActionsForControlEvents:UIControlEventTouchUpInside];
    DPTag *tag = [self tag:1809];
    [self finish:2 tag:tag];
    [self assertLoaded:detail tag:tag];
    XCTAssertEqual([[detail valueForKey:@"busyIndicator"] busyCount], 0);
}
- (void)testRefreshFailureRetainsPagesAndRetryReplacesCurrentTag {
    TMTestDetail *detail = [TMTestDetail new];
    detail.tagId = 1809;
    DPTag *tag = [self tag:1809];
    [self finish:0 tag:tag];
    [self attach:detail size:UIScreen.mainScreen.bounds.size style:UIUserInterfaceStyleLight large:NO];
    detail.selectedIndex = 1;
    [detail loadTag:YES];
    [self assertLoaded:detail tag:tag];
    XCTAssertEqual(detail.selectedIndex, 1);
    XCTAssertEqualObjects(self.requests.lastObject[@"refresh"], @YES);
    XCTAssertEqual(detail.navigationItem.rightBarButtonItems.lastObject, [detail valueForKey:@"loadingBarButton"]);
    [self finish:1 tag:nil];
    [self assertLoaded:detail tag:tag];
    XCTAssertNotNil(detail.retry);
    detail.retry();
    DPTag *updated = [self tag:1809]; updated.title = @"Updated tag";
    [self finish:2 tag:updated];
    [self assertLoaded:detail tag:updated];
    XCTAssertEqualObjects(detail.title, @"Updated tag");
    XCTAssertEqual(detail.selectedIndex, 1);
    XCTAssertEqual(detail.tabBar.selectedItem, detail.tabBar.items[1]);
    XCTAssertEqual([[detail valueForKey:@"busyIndicator"] busyCount], 0);
    [[detail valueForKey:@"busyIndicator"] incrementBusyCount];
    [self assertLoaded:detail tag:updated];
    [[detail valueForKey:@"busyIndicator"] decrementBusyCount];
}
- (void)testOutOfOrderCompletionAndOldRetryCannotPopulateDifferentTag {
    TMTestDetail *detail = [TMTestDetail new];
    detail.tagId = 1809;
    [self attach:detail size:UIScreen.mainScreen.bounds.size style:UIUserInterfaceStyleLight large:NO];
    detail.tagId = 42;
    [self finish:0 tag:[self tag:1809]];
    [self assertPending:detail];
    DPTag *current = [self tag:42];
    [self finish:1 tag:current];
    [self assertLoaded:detail tag:current];
    [detail loadTag:YES];
    [self finish:2 tag:nil];
    void (^oldRetry)(void) = detail.retry;
    detail.tagId = 99;
    oldRetry();
    XCTAssertEqual(self.requests.count, 4);
    DPTag *next = [self tag:99];
    [self finish:3 tag:next];
    [self assertLoaded:detail tag:next];
    XCTAssertEqual([[detail valueForKey:@"busyIndicator"] busyCount], 0);
}
- (void)testNewSuccessThenOldFailureDoesNotReplaceCurrentScreen {
    DPTagViewController *detail = [DPTagViewController new];
    detail.tagId = 1809;
    detail.tagId = 42;
    [detail loadViewIfNeeded];
    DPTag *tag = [self tag:42];
    [self finish:1 tag:tag];
    [self finish:0 tag:nil];
    [self assertLoaded:detail tag:tag];
    XCTAssertFalse([[detail valueForKey:@"loadFailed"] boolValue]);
    XCTAssertEqual([[detail valueForKey:@"busyIndicator"] busyCount], 0);
}
- (void)testBackWhilePendingStopsMotionAndDoesNotRetainController {
    __weak DPTagViewController *weakDetail;
    TMBusyIndicator *busy;
    @autoreleasepool {
        DPTagViewController *detail = [DPTagViewController new];
        weakDetail = detail;
        detail.tagId = 1809;
        UINavigationController *nav = [self attach:detail size:UIScreen.mainScreen.bounds.size style:UIUserInterfaceStyleLight large:NO];
        busy = [detail valueForKey:@"busyIndicator"];
        XCTAssertFalse(nav.navigationBarHidden);
        XCTAssertFalse(detail.navigationItem.hidesBackButton);
        XCTAssertTrue(nav.interactivePopGestureRecognizer.enabled);
        XCTAssertNotNil(nav.navigationBar.backItem);
        TMLayoutNavigationObserver *observer = [TMLayoutNavigationObserver new];
        nav.delegate = observer;
        __weak UIViewController *root = nav.viewControllers.firstObject;
        NSLog(@"TM_LAYOUT_PROBE before pop appeared=%@ visible=%@", [detail valueForKey:@"appeared"], [detail valueForKey:@"screenVisible"]);
        [nav popViewControllerAnimated:NO];
        XCTNSPredicateExpectation *popped = [[XCTNSPredicateExpectation alloc] initWithPredicate:
            [NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return observer.shown == root; }] object:nil];
        XCTAssertEqual([XCTWaiter waitForExpectations:@[popped] timeout:3], XCTWaiterResultCompleted);
        nav.delegate = nil;
        [self assertNotes:detail animated:NO];
        XCTAssertEqual(nav.viewControllers.count, 1);
    }
    XCTAssertNil(weakDetail);
    [self finish:0 tag:[self tag:1809]];
    XCTAssertEqual(busy.busyCount, 0);
}
- (void)testMotionPolicyBackgroundVisibilityDetachmentAndCompletion {
    DPTagViewController *detail = [DPTagViewController new];
    detail.tagId = 1809;
    [self attach:detail size:UIScreen.mainScreen.bounds.size style:UIUserInterfaceStyleDark large:NO];
    [NSNotificationCenter.defaultCenter postNotificationName:UIApplicationDidBecomeActiveNotification object:nil];
    [self assertNotes:detail animated:YES];
    self.reduceMotion = YES;
    [NSNotificationCenter.defaultCenter postNotificationName:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
    [self assertNotes:detail animated:NO];
    XCTAssertFalse([[detail valueForKey:@"quartetStaff"] isHidden]);
    self.reduceMotion = NO;
    [NSNotificationCenter.defaultCenter postNotificationName:UIAccessibilityReduceMotionStatusDidChangeNotification object:nil];
    [self assertNotes:detail animated:YES];
    [NSNotificationCenter.defaultCenter postNotificationName:UIApplicationWillResignActiveNotification object:nil];
    [self assertNotes:detail animated:NO];
    [NSNotificationCenter.defaultCenter postNotificationName:UIApplicationDidBecomeActiveNotification object:nil];
    [self assertNotes:detail animated:YES];
    [detail beginAppearanceTransition:NO animated:NO];
    [detail endAppearanceTransition];
    [self assertNotes:detail animated:NO];
    [detail beginAppearanceTransition:YES animated:NO];
    [detail endAppearanceTransition];
    [self assertNotes:detail animated:YES];
    UIView *staff = [detail valueForKey:@"quartetStaff"];
    UIScrollView *scroll = ((UIView *)[detail valueForKey:@"initialLoadingView"]).subviews.firstObject;
    CGPoint offset = scroll.contentOffset;
    scroll.contentOffset = CGPointMake(0, 10000);
    [self assertNotes:detail animated:NO];
    scroll.contentOffset = offset;
    [self assertNotes:detail animated:YES];
    staff.hidden = YES;
    [self assertNotes:detail animated:NO];
    staff.hidden = NO;
    [self assertNotes:detail animated:YES];
    UIStackView *stack = (UIStackView *)staff.superview;
    [stack removeArrangedSubview:staff];
    [staff removeFromSuperview];
    XCTAssertNil(staff.window);
    [self assertNotes:detail animated:NO];
    [stack insertArrangedSubview:staff atIndex:0];
    XCTAssertNotNil(staff.window);
    [self assertNotes:detail animated:YES];
    [self finish:0 tag:[self tag:1809]];
    [NSNotificationCenter.defaultCenter postNotificationName:UIApplicationDidBecomeActiveNotification object:nil];
    [self assertNotes:detail animated:NO];
}
- (void)checkTabDistribution:(TMPageViewController *)controller {
    XCTNSPredicateExpectation *settled = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) {
        [self layout];
        return [controller.pageTabController.selectedViewController.view isDescendantOfView:controller.rootView] && !controller.pageTabController.transitionCoordinator;
    }] object:nil];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[settled] timeout:3], XCTWaiterResultCompleted);
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
    XCTNSPredicateExpectation *selected = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return pages[1].appearances == 1 && pages[0].disappearances == 1; }] object:nil];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[selected] timeout:3], XCTWaiterResultCompleted);
    XCTAssertEqual(pages[0].disappearances, 1);
    XCTAssertEqual(pages[0].detachments, 0);
    XCTAssertEqual(pages[0].parentViewController, controller.pageTabController);
    XCTAssertEqual(pages[1].appearances, 1);
    XCTAssertEqual(pages[1].attachments, 1);
    controller.selectedIndex = 1;
    XCTAssertEqual(pages[1].appearances, 1);
    TMPageLifecycleSpy *cover = [TMPageLifecycleSpy new];
    [nav pushViewController:cover animated:NO];
    XCTNSPredicateExpectation *covered = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return cover.appearances == 1; }] object:nil];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[covered] timeout:3], XCTWaiterResultCompleted);
    XCTAssertEqual(pages[1].disappearances, 1);
    [nav popViewControllerAnimated:NO];
    XCTNSPredicateExpectation *returned = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return pages[1].appearances == 2; }] object:nil];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[returned] timeout:3], XCTWaiterResultCompleted);
    XCTAssertEqual(pages[1].appearances, 2);
    XCTAssertEqual(pages[0].appearances, 1);
    // Removing the selected destination must show a surviving native page.
    controller.viewControllers = @[pages[0], pages[2], pages[3]];
    XCTNSPredicateExpectation *removed = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return !pages[1].view.window && pages[1].disappearances == 2; }] object:nil];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[removed] timeout:3], XCTWaiterResultCompleted);
    XCTAssertEqual(pages[1].disappearances, 2);
    UIViewController *survivor = controller.pageTabController.selectedViewController;
    XCTAssertTrue([controller.viewControllers containsObject:survivor]);
    XCTAssertTrue([survivor.view isDescendantOfView:controller.rootView]);
    XCTAssertNil(pages[1].view.window);
    XCTAssertNil(pages[1].parentViewController);
    XCTAssertEqual(controller.tabBar.items.count, 3);
}

- (void)testNativeTabsKeepBottomPlacementAndContentClearance {
    DPTagViewController *detail = [DPTagViewController new];
    detail.tagId = 1809;
    [self finish:0 tag:[self tag:1809]];
    [self attach:detail size:UIScreen.mainScreen.bounds.size style:UIUserInterfaceStyleLight large:NO];
    [self checkTabDistribution:detail];
}
- (void)exercisePageSelection:(TMPageViewController *)controller {
    for (NSUInteger i = 0; i < 4; i++) {
        controller.selectedIndex = i;
        XCTNSPredicateExpectation *visible = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) {
            [self layout];
            if (![controller.viewControllers[i].view isDescendantOfView:controller.rootView]) return NO;
            for (UIViewController *other in controller.viewControllers) {
                if (other != controller.viewControllers[i] && other.viewIfLoaded.window && !other.viewIfLoaded.hidden) return NO;
            }
            return YES;
        }] object:nil];
        XCTAssertEqual([XCTWaiter waitForExpectations:@[visible] timeout:3], XCTWaiterResultCompleted);
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
}
- (void)testBrowseAndDetailWidthsRotationLargeTextSplitAndSelection {
    Method query = class_getClassMethod(DPTag.class, @selector(query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:));
    IMP original = method_getImplementation(query);
    DPTag *tag = [self tag:1809];
    IMP mock = imp_implementationWithBlock(^DPTagQueryResult *(id cls, NSString *text, int number, int start, NSNumber *parts, NSNumber *tracks, NSNumber *sheet, enum DPTagCollection collection, enum DPTagSortOptions sort) {
        DPTagQueryResult *result = [DPTagQueryResult new];
        result.tags = @[tag]; result.available = 1; result.count = 1; result.start = 0;
        return result;
    });
    method_setImplementation(query, mock);
    @try {
        CGSize portrait = UIScreen.mainScreen.bounds.size;
        NSString *device = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? @"ipad" : @"phone";
        for (NSUInteger page = 0; page < 2; page++) {
            TMPageViewController *controller;
            if (page == 0) controller = [DPBrowseViewController new];
            else {
                DPTagViewController *detail = [DPTagViewController new];
                detail.tagId = 1809;
                controller = detail;
            }
            UINavigationController *nav = [self attach:controller size:portrait style:UIUserInterfaceStyleDark large:NO];
            XCTAssertEqual(controller.selectedIndex, 0);
            if (page == 1) {
                [self assertPending:(DPTagViewController *)controller];
                [self finish:self.requests.count - 1 tag:tag];
                [self assertLoaded:(DPTagViewController *)controller tag:tag];
            }
            [self exercisePageSelection:controller];
            if (page == 0) {
                for (DPTagQueryViewController *child in controller.viewControllers) {
                    XCTNSPredicateExpectation *loaded = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return !child.isLoading && child.tags.count == 1; }] object:nil];
                    XCTAssertEqual([XCTWaiter waitForExpectations:@[loaded] timeout:3], XCTWaiterResultCompleted);
                }
            }
            for (NSUInteger variant = 0; variant < 4; variant++) {
                BOOL landscape = variant % 2;
                self.window.frame = (CGRect){CGPointZero, landscape ? CGSizeMake(portrait.height, portrait.width) : portrait};
                self.window.traitOverrides.preferredContentSizeCategory = variant < 2 ? UIContentSizeCategoryLarge : UIContentSizeCategoryAccessibilityExtraExtraExtraLarge;
                [self drainUIKit]; [self layout]; [self layout];
                [self checkTabDistribution:controller];
                XCTAssertEqual(controller.selectedIndex, 1);
            }
            self.window.frame = (CGRect){CGPointZero, portrait};
            self.window.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryLarge;
            controller.selectedIndex = 0;
            [self drainUIKit]; [self layout]; [self layout];
            if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
                self.window.rootViewController = nil;
                UISplitViewController *split = [[UISplitViewController alloc] initWithStyle:UISplitViewControllerStyleDoubleColumn];
                split.preferredDisplayMode = UISplitViewControllerDisplayModeOneBesideSecondary;
                split.preferredSplitBehavior = UISplitViewControllerSplitBehaviorTile;
                UIViewController *sidebar = [UIViewController new];
                sidebar.title = @"Tags";
                sidebar.view.backgroundColor = UIColor.secondarySystemBackgroundColor;
                [split setViewController:[[UINavigationController alloc] initWithRootViewController:sidebar] forColumn:UISplitViewControllerColumnPrimary];
                [split setViewController:nav forColumn:UISplitViewControllerColumnSecondary];
                self.window.rootViewController = split;
                XCTNSPredicateExpectation *tiled = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) {
                    [self layout];
                    CGRect first = [controller.tabBar convertRect:controller.tabBar.bounds toView:self.window];
                    CGRect side = [sidebar.view convertRect:sidebar.view.bounds toView:self.window];
                    return split.displayMode == UISplitViewControllerDisplayModeOneBesideSecondary && CGRectGetMinX(first) >= CGRectGetMaxX(side);
                }] object:nil];
                XCTAssertEqual([XCTWaiter waitForExpectations:@[tiled] timeout:3], XCTWaiterResultCompleted);
                [self drainUIKit]; [self layout]; [self layout];
                [self checkTabDistribution:controller];
                CGRect first = [controller.tabBar convertRect:controller.tabBar.bounds toView:self.window];
                CGRect side = [sidebar.view convertRect:sidebar.view.bounds toView:self.window];
                XCTAssertGreaterThanOrEqual(CGRectGetMinX(first), CGRectGetMaxX(side));
                for (NSNumber *width in @[@600, @(portrait.height)]) {
                    self.window.frame = CGRectMake(0, 0, width.doubleValue, portrait.width);
                    self.window.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryAccessibilityExtraExtraExtraLarge;
                    [self drainUIKit]; [self layout]; [self layout];
                    [self checkTabDistribution:controller];
                }
                self.window.frame = (CGRect){CGPointZero, portrait};
                self.window.traitOverrides.preferredContentSizeCategory = UIContentSizeCategoryLarge;
                [self drainUIKit]; [self layout]; [self layout];
            }
            [self checkTabDistribution:controller];
            [self capture:[NSString stringWithFormat:@"tagmaster-ios-glass-tabs-%@-%@", device, page == 0 ? @"browse" : @"detail"]];
            controller.selectedIndex = 2;
            [nav pushViewController:[UIViewController new] animated:NO];
            [self drainUIKit];
            [nav popViewControllerAnimated:NO];
            [self drainUIKit]; [self layout];
            XCTAssertEqual(controller.selectedIndex, 2);
            [self checkTabDistribution:controller];
        }
    } @finally {
        method_setImplementation(query, original);
        imp_removeBlock(mock);
    }
}
- (CGFloat)luminance:(UIColor *)color on:(UIColor *)background traits:(UITraitCollection *)traits {
    CGFloat r, g, b, a, br, bg, bb, ba;
    [[color resolvedColorWithTraitCollection:traits] getRed:&r green:&g blue:&b alpha:&a];
    [[background resolvedColorWithTraitCollection:traits] getRed:&br green:&bg blue:&bb alpha:&ba];
    CGFloat components[] = {r * a + br * (1 - a), g * a + bg * (1 - a), b * a + bb * (1 - a)};
    for (NSUInteger i = 0; i < 3; i++) components[i] = components[i] <= 0.04045 ? components[i] / 12.92 : pow((components[i] + 0.055) / 1.055, 2.4);
    return components[0] * 0.2126 + components[1] * 0.7152 + components[2] * 0.0722;
}
- (void)testNativeTabMaterialAndSelectionRemainSystemOwned {
    DPTagViewController *detail = [DPTagViewController new];
    detail.tagId = 1809;
    [self finish:0 tag:[self tag:1809]];
    [self attach:detail size:UIScreen.mainScreen.bounds.size style:UIUserInterfaceStyleLight large:NO];
    for (NSNumber *style in @[@(UIUserInterfaceStyleLight), @(UIUserInterfaceStyleDark)]) {
        self.window.overrideUserInterfaceStyle = style.integerValue;
        [self drainUIKit]; [self layout];
        XCTAssertEqual(detail.tabBar.delegate, detail.pageTabController);
        XCTAssertNil(detail.tabBar.backgroundImage);
        XCTAssertNil(detail.tabBar.selectionIndicatorImage);
        XCTAssertNil(detail.tabBar.barTintColor);
        XCTAssertEqualObjects(detail.tabBar.tintColor, UIColor.systemBlueColor);
        XCTAssertEqualObjects([detail.tabBar.items valueForKey:@"title"], (@[@"Summary", @"Details", @"Tracks", @"Videos"]));
        for (UITabBarItem *item in detail.tabBar.items) XCTAssertNotNil(item.image);
        [self exercisePageSelection:detail];
        [self checkTabDistribution:detail];
    }
}
- (void)testNativePendingAndLoadedCapturesAndAdaptiveLayout {
    NSString *device = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? @"ipad" : @"phone";
    CGSize portrait = UIScreen.mainScreen.bounds.size;
    for (NSUInteger variant = 0; variant < 3; variant++) {
        BOOL large = variant == 2;
        UIUserInterfaceStyle style = variant == 0 ? UIUserInterfaceStyleLight : UIUserInterfaceStyleDark;
        TMTestDetail *detail = [TMTestDetail new];
        detail.tagId = 1809;
        NSUInteger request = self.requests.count - 1;
        [self attach:detail size:portrait style:style large:large];
        [self assertPending:detail];
        UILabel *status = [detail valueForKey:@"loadingStatus"];
        UIColor *background = [[detail valueForKey:@"initialLoadingView"] backgroundColor];
        CGFloat textLuminance = [self luminance:status.textColor on:background traits:status.traitCollection];
        CGFloat backgroundLuminance = [self luminance:background on:background traits:status.traitCollection];
        XCTAssertGreaterThanOrEqual((MAX(textLuminance, backgroundLuminance) + 0.05) / (MIN(textLuminance, backgroundLuminance) + 0.05), 4.5);
        XCTAssertEqualObjects(status.traitCollection.preferredContentSizeCategory, large ? UIContentSizeCategoryAccessibilityExtraExtraExtraLarge : UIContentSizeCategoryLarge);
        XCTAssertEqualWithAccuracy(status.font.pointSize, [UIFont preferredFontForTextStyle:UIFontTextStyleBody compatibleWithTraitCollection:status.traitCollection].pointSize, 0.1);
        for (NSString *key in @[@"loadingHeading", @"loadingStatus"]) {
            UILabel *label = [detail valueForKey:key];
            XCTAssertGreaterThan(label.bounds.size.width, 0);
            XCTAssertGreaterThanOrEqual(label.bounds.size.height + 1, [label sizeThatFits:CGSizeMake(label.bounds.size.width, CGFLOAT_MAX)].height);
        }
        UIView *staff = [detail valueForKey:@"quartetStaff"];
        XCTAssertEqualWithAccuracy(staff.bounds.size.width, TMQuartetWidth, 1);
        XCTAssertEqualWithAccuracy(staff.bounds.size.height, TMQuartetHeight, 1);
        NSString *suffix = variant == 0 ? @"light" : (large ? @"dark-ax5" : @"dark");
        NSString *name = [NSString stringWithFormat:@"tagmaster-ios-loading-%@-%@", device, suffix];
        [self capture:[name stringByAppendingString:@"-pending"]];
        if (large) {
            self.window.frame = CGRectMake(0, 0, portrait.height, portrait.width);
            [self layout];
            [self assertPending:detail];
            UIScrollView *scroll = ((UIView *)[detail valueForKey:@"initialLoadingView"]).subviews.firstObject;
            CGRect textRect = [status convertRect:status.bounds toView:scroll];
            [scroll scrollRectToVisible:textRect animated:NO];
            XCTAssertLessThanOrEqual(CGRectGetMaxY(textRect), scroll.contentSize.height + 1);
            [self capture:[name stringByAppendingString:@"-landscape-pending"]];
            self.window.frame = (CGRect){CGPointZero, portrait};
        }
        DPTag *tag = [self tag:1809];
        [self finish:request tag:tag];
        [self drainUIKit];
        [self layout];
        [self assertLoaded:detail tag:tag];
        [self capture:[name stringByAppendingString:@"-loaded"]];
    }
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
        BOOL hidden = NO;
        for (UIView *ancestor = label; ancestor && ancestor != grid; ancestor = ancestor.superview) hidden |= ancestor.hidden;
        if (hidden) continue;
        CGFloat textHeight = [label sizeThatFits:CGSizeMake(label.bounds.size.width, CGFLOAT_MAX)].height;
        CGRect frame = [label convertRect:label.bounds toView:scroll];
        NSLog(@"TM_SUMMARY cycle=%ld %@ width=%.1f y=%.1f height=%.1f text=%.1f hugging=%.0f compression=%.0f",
              (long)cycle, key, label.bounds.size.width, frame.origin.y, frame.size.height, textHeight,
              [label contentHuggingPriorityForAxis:UILayoutConstraintAxisVertical],
              [label contentCompressionResistancePriorityForAxis:UILayoutConstraintAxisVertical]);
        XCTAssertGreaterThan(label.bounds.size.width, 0);
        XCTAssertEqualWithAccuracy(label.bounds.size.height, textHeight, 1, @"%@ must hug its text", key);
        expectedHeight += ceil(textHeight);
    }
    // Allow the rating, key and sheet controls plus ordinary padding, not viewport-sized slack.
    for (NSString *key in @[@"ratingButton", @"keyButton", @"sheetMusicButton"]) {
        UIView *control = [summary valueForKey:key];
        if ([control isDescendantOfView:grid]) expectedHeight += control.bounds.size.height;
    }
    DPPitchPipeButton *pitch = [summary valueForKey:@"keyButton"];
    CGFloat keyHeight = [pitch.button sizeThatFits:CGSizeMake(pitch.button.bounds.size.width, CGFLOAT_MAX)].height;
    UIButton *sheetButton = [summary valueForKey:@"sheetMusicButton"];
    CGFloat sheetHeight = [sheetButton sizeThatFits:CGSizeMake(sheetButton.bounds.size.width, CGFLOAT_MAX)].height;
    XCTAssertEqualWithAccuracy(pitch.bounds.size.height, MAX(44, MAX(keyHeight, sheetHeight)), 1, @"Matched faces fit the larger title without a widget inset");
    UILabel *rating = [summary valueForKey:@"ratingLabel"];
    UIButton *rate = [summary valueForKey:@"ratingButton"];
    UIProgressView *bar = [summary valueForKey:@"ratingBar"];
    CGFloat ratingTextHeight = [rating sizeThatFits:CGSizeMake(rating.bounds.size.width, CGFLOAT_MAX)].height;
    XCTAssertEqualWithAccuracy(rating.bounds.size.height, ratingTextHeight, 1);
    CGFloat ratingHeight = ratingTextHeight + 4 + bar.bounds.size.height;
    UIStackView *ratingUnit = (UIStackView *)rating.superview.superview;
    BOOL stackedRating = ratingUnit.axis == UILayoutConstraintAxisVertical;
    expectedHeight += stackedRating ? ratingHeight + ratingUnit.spacing : MAX(0, ratingHeight - rate.bounds.size.height);
    XCTAssertGreaterThanOrEqual(rating.bounds.size.width + .5, [rating.text sizeWithAttributes:@{NSFontAttributeName:rating.font}].width, @"Full rating survives page switches");
    if (stackedRating) {
        CGRect valueFrame = [rating.superview convertRect:rating.superview.bounds toView:ratingUnit];
        CGRect actionFrame = [rate.superview convertRect:rate.superview.bounds toView:ratingUnit];
        XCTAssertEqualWithAccuracy(CGRectGetMinY(actionFrame) - CGRectGetMaxY(valueFrame), 8, .5, @"Only the regular rating group gap is added");
    }
    for (NSString *key in @[@"ratingHeader", @"partsHeader", @"typeHeader", @"classicTagNumberHeader", @"keyHeader", @"lyricsHeader", @"notesHeader"]) {
        UILabel *header = [summary valueForKey:key];
        BOOL hidden = NO;
        for (UIView *ancestor = header; ancestor && ancestor != grid; ancestor = ancestor.superview) hidden |= ancestor.hidden;
        if (!hidden) expectedHeight += ceil([header sizeThatFits:CGSizeMake(header.bounds.size.width, CGFLOAT_MAX)].height);
    }
    // Eight section/pair gaps, four within-block gaps, and the 8pt identity-to-facts gap.
    XCTAssertLessThanOrEqual(scroll.contentSize.height, expectedHeight + 8 * 16 + 4 * 4 + 8);
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
        XCTAssertEqualWithAccuracy(CGRectGetMinY(text), CGRectGetMaxY(heading) + 4, 1, @"%@ starts below its heading", name);
        XCTAssertEqualWithAccuracy(text.origin.x, heading.origin.x, .5);
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
                    for (NSNumber *index in @[@1, @0]) {
                        detail.selectedIndex = index.unsignedIntegerValue;
                        XCTNSPredicateExpectation *selected = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) {
                            [self settleLayout:window];
                            return detail.pageTabController.selectedViewController.view.window == window && !detail.pageTabController.transitionCoordinator;
                        }] object:nil];
                        XCTAssertEqual([XCTWaiter waitForExpectations:@[selected] timeout:3], XCTWaiterResultCompleted);
                    }
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
                UIView *view = [summary valueForKey:key];
                BOOL collapsed = NO;
                for (UIView *ancestor = view; ancestor && ancestor != grid; ancestor = ancestor.superview) collapsed |= ancestor.hidden;
                XCTAssertTrue(collapsed, @"%@ should collapse with its section", key);
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
    XCTAssertEqual([XCTWaiter waitForExpectations:@[transaction] timeout:3], XCTWaiterResultCompleted);
    [self.window layoutIfNeeded];
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
    NSLog(@"TM_LAYOUT_PROBE Parts wrapper=%.1f segments=%.1f count=%ld hidden=%d", wrapper.bounds.size.width, parts.bounds.size.width, (long)parts.numberOfSegments, parts.hidden);
    XCTAssertLessThan(wrapper.bounds.size.width, 308);
    XCTAssertTrue(parts.hidden, @"Seven 44pt targets cannot fit below 308pt");
    XCTAssertFalse([[wrapper valueForKey:@"menuButton"] isHidden]);
}
- (void)testDetailsAX5ValueDoesNotBecomeOneCharacterColumn {
    DPTagDetailController *details = [DPTagDetailController new];
    DPTag *tag = [DPTag new]; tag.title = @"Lost"; tag.tagId = 1809;
    tag.lastRefreshed = [NSDate dateWithTimeIntervalSince1970:1600000000];
    details.tag = tag;
    [self mount:details width:393 category:UIContentSizeCategoryAccessibilityExtraExtraExtraLarge];
    UILabel *value = [details valueForKey:@"tagIdLabel"];
    CGFloat lexicalWidth = [value.text sizeWithAttributes:@{NSFontAttributeName:value.font}].width;
    NSLog(@"TM_LAYOUT_PROBE ID width=%.1f lexical=%.1f height=%.1f", value.bounds.size.width, lexicalWidth, value.bounds.size.height);
    XCTAssertGreaterThanOrEqual(value.bounds.size.width + 1, lexicalWidth);
    XCTAssertLessThanOrEqual(value.bounds.size.height, value.font.lineHeight + 1);
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
- (void)testDetailsPairsFitResizeOmitAndKeepReadingOrder {
    for (UIContentSizeCategory category in @[UIContentSizeCategoryLarge, UIContentSizeCategoryExtraExtraExtraLarge, UIContentSizeCategoryAccessibilityExtraExtraExtraLarge]) {
        DPTagDetailController *details = [DPTagDetailController new];
        DPTag *tag = [self layoutTag]; details.tag = tag;
        [self mount:details width:393 category:category];
        for (NSNumber *width in @[@320, @393, @834, @320, @834]) {
            self.window.frame = CGRectMake(0, 0, width.doubleValue, 852); [self settle];
            UIStackView *root = [details valueForKey:@"metadataStack"];
            UIScrollView *scroll = (id)root.superview.superview;
            NSArray<TMDetailPair *> *pairs = [details valueForKey:@"metadataPairs"];
            CGFloat lastBottom = 0;
            for (TMDetailPair *pair in pairs) {
                UILabel *caption = [pair valueForKey:@"caption"];
                UIView *value = [pair valueForKey:@"value"];
                XCTAssertEqualObjects(pair.accessibilityElements, (@[caption, value]));
                [self assertWholeLabel:caption];
                XCTAssertGreaterThanOrEqual(value.bounds.size.width, 44);
                if ([value isKindOfClass:UIButton.class]) {
                    UIButton *button = (id)value;
                    XCTAssertGreaterThanOrEqual(button.bounds.size.height, 44);
                    XCTAssertGreaterThanOrEqual(button.bounds.size.height + 1, [button sizeThatFits:CGSizeMake(button.bounds.size.width, CGFLOAT_MAX)].height);
                } else [self assertWholeLabel:(id)value];
                CGRect frame = [pair convertRect:pair.bounds toView:scroll];
                XCTAssertGreaterThanOrEqual(frame.origin.y, lastBottom - 1);
                lastBottom = CGRectGetMaxY(frame);
                XCTAssertLessThanOrEqual(CGRectGetMaxX([value convertRect:value.bounds toView:root]), root.bounds.size.width + 1);
                XCTAssertFalse(pair.hasAmbiguousLayout);
                if ([category isEqualToString:UIContentSizeCategoryLarge]) XCTAssertEqual(pair.axis, UILayoutConstraintAxisHorizontal);
            }
            UILabel *identifier = [details valueForKey:@"tagIdLabel"];
            XCTAssertEqualObjects(identifier.text, @"1809");
            XCTAssertLessThanOrEqual(identifier.bounds.size.height, identifier.font.lineHeight + 1);
            CGRect lastLine = CGRectMake(0, lastBottom - 1, 1, 1);
            [scroll scrollRectToVisible:lastLine animated:NO];
            XCTAssertTrue(CGRectContainsRect(CGRectInset(scroll.bounds, -.5, -.5), lastLine));
            CGFloat full = scroll.contentSize.height;
            DPTag *minimal = [DPTag new]; minimal.title = @"Lost"; minimal.tagId = 1809;
            details.tag = minimal; [self settle];
            for (NSNumber *index in @[@4, @6, @7, @8, @9]) XCTAssertTrue(pairs[index.integerValue].hidden);
            XCTAssertLessThan(scroll.contentSize.height, full);
            details.tag = tag; [self settle];
            XCTAssertEqualWithAccuracy(scroll.contentSize.height, full, 1);
        }
    }
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
- (void)testSummaryHasExactlyOneConditionalProseBreak {
    for (NSNumber *large in @[@NO, @YES]) {
        DPTagSummaryController *summary = [DPTagSummaryController new];
        [self mount:summary width:393 category:large.boolValue ? UIContentSizeCategoryAccessibilityExtraExtraExtraLarge : UIContentSizeCategoryLarge];
        for (NSNumber *sheet in @[@NO, @YES]) for (NSInteger prose = 0; prose < 4; prose++) {
            DPTag *tag = [self layoutTag];
            if (!sheet.boolValue) tag.sheetMusicUri = nil;
            if (!(prose & 1)) tag.lyrics = nil;
            if (!(prose & 2)) tag.notes = nil;
            summary.tag = tag; [self settle];
            UIView *grid = [summary valueForKey:@"grid"];
            UIStackView *lyrics = [summary valueForKey:@"lyricsSection"];
            UIStackView *notes = [summary valueForKey:@"notesSection"];
            XCTAssertEqual(lyrics.hidden, !(prose & 1));
            XCTAssertEqual(notes.hidden, !(prose & 2));
            XCTAssertEqual(lyrics.superview.hidden, prose == 0);
            if (prose) {
                UIView *first = prose & 1 ? lyrics : notes;
                UIView *performance = ((UIStackView *)first.superview.superview).arrangedSubviews.firstObject;
                XCTAssertEqualWithAccuracy([first convertRect:first.bounds toView:grid].origin.y,
                    CGRectGetMaxY([performance convertRect:performance.bounds toView:grid]) + 16, 1);
                if (prose == 3) XCTAssertEqualWithAccuracy(notes.frame.origin.y, CGRectGetMaxY(lyrics.frame) + 16, 1);
            }
            UIButton *button = [summary valueForKey:@"sheetMusicButton"];
            if (sheet.boolValue) {
                CGRect frame = button.frame;
                TMBarberPoleLoadingView *loader = [summary valueForKey:@"sheetMusicLoading"];
                [loader startAnimating]; [self settle]; XCTAssertTrue(CGRectEqualToRect(button.frame, frame));
                [loader stopAnimating]; [self settle]; XCTAssertTrue(CGRectEqualToRect(button.frame, frame));
            }
        }
    }
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
- (void)testDetailCompositionOrdinaryPairedCapturesAndGeometry {
    for (NSNumber *dark in @[@NO, @YES]) {
        DPTag *tag = [self layoutTag];
        tag.tagId = 2; tag.title = @"I Love to Sing 'Em"; tag.alternativeTitle = nil;
        tag.rating = 3.35; tag.writtenKey = @"Major:Eb"; tag.classicTagNumber = 1;
        tag.provider = @"Daniel Gillis"; tag.arranger = @"Mac Huff"; tag.sungBy = nil;
        tag.yearArranged = 0; tag.sungYear = 0; tag.lyrics = nil; tag.notes = nil;
        tag.downloadCount = 70274; tag.posted = [NSDate dateWithTimeIntervalSince1970:1228608000];
        DPTagViewController *detail = [DPTagViewController new];
        UINavigationController *navigation = [[UINavigationController alloc] initWithRootViewController:detail];
        [self mount:navigation width:427 category:UIContentSizeCategoryLarge];
        self.window.overrideUserInterfaceStyle = dark.boolValue ? UIUserInterfaceStyleDark : UIUserInterfaceStyleLight;
        [detail setValue:tag forKey:@"tag"]; [self settle];
        DPTagSummaryController *summary = [detail valueForKey:@"summaryController"];
        UIView *root = [summary valueForKey:@"grid"];
        TMDetailMetadata *facts = [summary valueForKey:@"facts"];
        CGFloat factAxis = -1;
        for (TMDetailPair *pair in facts.arrangedSubviews) {
            if (pair.hidden) continue;
            CGFloat x = [pair.value convertRect:pair.value.bounds toView:root].origin.x;
            if (factAxis < 0) factAxis = x;
            XCTAssertEqualWithAccuracy(x, factAxis, .5, @"All Summary fact values share one axis");
            UILabel *label = (id)pair.value.viewForFirstBaselineLayout;
            if ([label isKindOfClass:UILabel.class]) {
                CGFloat captionBaseline = [pair.caption convertPoint:CGPointMake(0, pair.caption.font.ascender) toView:root].y;
                CGFloat valueBaseline = [label convertPoint:CGPointMake(0, label.font.ascender) toView:root].y;
                XCTAssertEqualWithAccuracy(captionBaseline, valueBaseline, 2, @"Summary first visible baseline");
            }
        }
        UILabel *title = [summary valueForKey:@"titleLabel"];
        CGFloat leading = [title convertRect:title.bounds toView:root].origin.x;
        for (NSString *key in @[@"akaLabel", @"lyricsHeader", @"lyricsLabel", @"notesHeader", @"notesLabel"]) {
            UIView *view = [summary valueForKey:key];
            XCTAssertEqualWithAccuracy([view convertRect:view.bounds toView:root].origin.x, leading, .5, @"%@ reading edge", key);
        }
        DPPitchPipeButton *pitch = [summary valueForKey:@"keyButton"];
        UIButton *sheet = [summary valueForKey:@"sheetMusicButton"];
        CGRect keyFrame = [pitch.button convertRect:pitch.button.bounds toView:root];
        CGRect sheetFrame = [sheet convertRect:sheet.bounds toView:root];
        XCTAssertEqualWithAccuracy(keyFrame.origin.x, sheetFrame.origin.x, .5);
        XCTAssertEqualWithAccuracy(CGRectGetMinX(keyFrame), 0, .5, @"Actual readable column leading edge");
        XCTAssertEqualWithAccuracy(CGRectGetMaxX(keyFrame), root.bounds.size.width, .5, @"Actual readable column trailing edge");
        NSLog(@"SPACING_IOS content=%@ key=%@ sheet=%@", NSStringFromCGRect(root.bounds), NSStringFromCGRect(keyFrame), NSStringFromCGRect(sheetFrame));
        XCTAssertEqualWithAccuracy(keyFrame.size.width, sheetFrame.size.width, .5);
        XCTAssertEqualWithAccuracy(keyFrame.size.height, sheetFrame.size.height, .5);
        XCTAssertGreaterThanOrEqual(keyFrame.size.height, 44);
        UILabel *rating = [summary valueForKey:@"ratingLabel"];
        UIButton *rate = [summary valueForKey:@"ratingButton"];
        XCTAssertEqualObjects(rating.text, @"3.35");
        XCTAssertGreaterThanOrEqual(rating.bounds.size.width + .5, [rating.text sizeWithAttributes:@{NSFontAttributeName:rating.font}].width);
        UILabel *type = [summary valueForKey:@"typeLabel"];
        XCTAssertEqualObjects(type.text, tag.tagType);
        XCTAssertGreaterThanOrEqual(type.bounds.size.height + .5, [type sizeThatFits:CGSizeMake(type.bounds.size.width, CGFLOAT_MAX)].height, @"Previously empty values acquire their visible text height");
        XCTAssertGreaterThanOrEqual(type.bounds.size.width, [type.text sizeWithAttributes:@{NSFontAttributeName:type.font}].width);
        CGRect ratingFrame = [rating convertRect:rating.bounds toView:root];
        CGRect rateFrame = [rate convertRect:rate.bounds toView:root];
        XCTAssertLessThanOrEqual(rateFrame.origin.x - CGRectGetMaxX(ratingFrame), 16);
        TMBarberPoleLoadingView *loader = [summary valueForKey:@"sheetMusicLoading"];
        for (NSNumber *busy in @[@YES, @NO]) {
            if (busy.boolValue) [loader startAnimating]; else [loader stopAnimating];
            sheet.enabled = !busy.boolValue; [self settle];
            XCTAssertTrue(CGRectEqualToRect(sheetFrame, [sheet convertRect:sheet.bounds toView:root]));
            XCTAssertTrue(CGRectEqualToRect(keyFrame, [pitch.button convertRect:pitch.button.bounds toView:root]));
        }
        [self captureComposition:[NSString stringWithFormat:@"ordinary-%@-summary", dark.boolValue ? @"dark" : @"light"]];
        detail.selectedIndex = 1; [self settle];
        DPTagDetailController *details = [detail valueForKey:@"detailController"];
        NSArray<TMDetailPair *> *pairs = [details valueForKey:@"metadataPairs"];
        CGFloat axis = -1, previousBaseline = -1;
        for (TMDetailPair *pair in pairs) {
            if (pair.hidden) continue;
            UILabel *caption = [pair valueForKey:@"caption"];
            UIView *value = [pair valueForKey:@"value"];
            CGFloat x = [value convertRect:value.bounds toView:details.view].origin.x;
            if (axis < 0) axis = x;
            XCTAssertEqualWithAccuracy(x, axis, .5);
            UILabel *label = [value isKindOfClass:UIButton.class] ? ((UIButton *)value).titleLabel : (UILabel *)value;
            CGFloat cBaseline = [caption convertPoint:CGPointMake(0, (caption.bounds.size.height - caption.font.lineHeight) / 2 + caption.font.ascender) toView:details.view].y;
            CGFloat vBaseline = [label convertPoint:CGPointMake(0, (label.bounds.size.height - [label sizeThatFits:label.bounds.size].height) / 2 + label.font.ascender) toView:details.view].y;
            XCTAssertEqualWithAccuracy(cBaseline, vBaseline, 2, @"%@ first visible baseline", caption.text);
            XCTAssertEqualWithAccuracy(pair.bounds.size.height, 44, .5, @"Uniform ordinary row %@", caption.text);
            if (previousBaseline >= 0) XCTAssertEqualWithAccuracy(cBaseline - previousBaseline, 44, .5, @"Visible baseline stride, not just wrapper height");
            previousBaseline = cBaseline;
            NSLog(@"SPACING_IOS row=%@ frame=%@ captionBaseline=%.2f valueBaseline=%.2f value=%@ label=%@ fit=%@", caption.text, NSStringFromCGRect([pair convertRect:pair.bounds toView:details.view]), cBaseline, vBaseline, NSStringFromCGRect(value.frame), NSStringFromCGRect([label convertRect:label.bounds toView:value]), NSStringFromCGSize([label sizeThatFits:label.bounds.size]));
        }
        [self captureComposition:[NSString stringWithFormat:@"ordinary-%@-details", dark.boolValue ? @"dark" : @"light"]];
    }
}
@end

@implementation TMLayoutRegressionTests (DetailCompositionAdaptive)
- (void)testDetailCompositionAdaptiveGroupsAndCaptures {
    for (UIContentSizeCategory category in @[UIContentSizeCategoryLarge, UIContentSizeCategoryExtraExtraExtraLarge, UIContentSizeCategoryAccessibilityExtraExtraExtraLarge]) {
        for (NSNumber *width in @[@320, @393, @834]) {
            DPTag *tag = [self layoutTag];
            tag.title = @"Lost"; tag.alternativeTitle = @"In Your Eyes";
            tag.provider = @"David Wright"; tag.arranger = @"David Wright"; tag.sungBy = @"The New Tradition"; tag.notes = @"Hold the last chord.";
            DPTagViewController *detail = [DPTagViewController new];
            UINavigationController *navigation = [[UINavigationController alloc] initWithRootViewController:detail];
            [self mount:navigation width:width.doubleValue category:category];
            if (width.intValue == 834) self.window.frame = CGRectMake(0, 0, 834, 393);
            [detail setValue:tag forKey:@"tag"]; [self settle];
            DPTagSummaryController *summary = [detail valueForKey:@"summaryController"];
            UIView *root = [summary valueForKey:@"grid"];
            UIStackView *lyrics = [summary valueForKey:@"lyricsSection"];
            UIStackView *columns = (UIStackView *)lyrics.superview.superview;
            BOOL wide = root.bounds.size.width >= 560 * [UIFont preferredFontForTextStyle:UIFontTextStyleBody compatibleWithTraitCollection:self.window.traitCollection].pointSize / 17 + 16;
            XCTAssertEqual(columns.axis, wide ? UILayoutConstraintAxisHorizontal : UILayoutConstraintAxisVertical);
            DPPitchPipeButton *pitch = [summary valueForKey:@"keyButton"];
            UIButton *sheet = [summary valueForKey:@"sheetMusicButton"];
            CGRect k = [pitch.button convertRect:pitch.button.bounds toView:root], s = [sheet convertRect:sheet.bounds toView:root];
            XCTAssertEqualWithAccuracy(k.origin.x, s.origin.x, .5); XCTAssertEqualWithAccuracy(k.size.width, s.size.width, .5); XCTAssertEqualWithAccuracy(k.size.height, s.size.height, .5);
            XCTAssertGreaterThanOrEqual(k.size.height + .001, 44); // Floating-point conversion, not a sub-point target allowance.
            UILabel *rating = [summary valueForKey:@"ratingLabel"];
            UIButton *rate = [summary valueForKey:@"ratingButton"];
            XCTAssertGreaterThanOrEqual(rating.bounds.size.width + .5, ceil([rating.text sizeWithAttributes:@{NSFontAttributeName:rating.font}].width), @"Full numeric rating at %@ / %@", width, category);
            XCTAssertGreaterThanOrEqual(rate.titleLabel.bounds.size.width + .5, [rate.titleLabel.text sizeWithAttributes:@{NSFontAttributeName:rate.titleLabel.font}].width, @"Full Rate title");
            XCTAssertGreaterThanOrEqual(rate.bounds.size.height, 44);
            CGRect ratingFrame = [rating convertRect:rating.bounds toView:root], rateFrame = [rate convertRect:rate.bounds toView:root];
            XCTAssertFalse(CGRectIntersectsRect(ratingFrame, rateFrame));
            NSLog(@"SPACING_IOS_RATING width=%@ category=%@ number=%@ fit=%.2f rate=%@", width, category, NSStringFromCGRect(ratingFrame), [rating.text sizeWithAttributes:@{NSFontAttributeName:rating.font}].width, NSStringFromCGRect(rateFrame));
            BOOL captureLarge = width.intValue == 393 && [category isEqualToString:UIContentSizeCategoryAccessibilityExtraExtraExtraLarge];
            if (captureLarge) {
                UIScrollView *ratingScroll = (id)root.superview.superview;
                CGRect unit = [rating.superview.superview convertRect:rating.superview.superview.bounds toView:ratingScroll];
                [ratingScroll scrollRectToVisible:unit animated:NO];
                [self captureComposition:@"large-rating"];
            }
            BOOL captureWide = width.intValue == 834 && [category isEqualToString:UIContentSizeCategoryLarge];
            UIScrollView *scroll = (id)root.superview.superview;
            if (captureLarge) [scroll scrollRectToVisible:[sheet convertRect:sheet.bounds toView:scroll] animated:NO];
            if (captureLarge || captureWide) [self captureComposition:captureLarge ? @"large-summary" : @"wide-summary"];
            detail.selectedIndex = 1; [self settle];
            DPTagDetailController *details = [detail valueForKey:@"detailController"];
            NSArray<TMDetailPair *> *pairs = [details valueForKey:@"metadataPairs"];
            CGFloat axis = -1;
            for (TMDetailPair *pair in pairs) {
                if (pair.hidden) continue;
                XCTAssertEqual(pair.axis, pairs.firstObject.axis);
                CGFloat x = pair.value.frame.origin.x;
                if (axis < 0) axis = x;
                XCTAssertEqualWithAccuracy(x, axis, .5);
                XCTAssertGreaterThanOrEqual(pair.value.bounds.size.width, 96);
            }
            if (captureLarge || captureWide) [self captureComposition:captureLarge ? @"large-details" : @"wide-details"];
            if (captureLarge) {
                UIStackView *metadata = [details valueForKey:@"metadataStack"];
                UIScrollView *scroller = (id)metadata.superview.superview;
                UIView *last = pairs.lastObject;
                [scroller scrollRectToVisible:[last convertRect:last.bounds toView:scroller] animated:NO];
                [self captureComposition:@"large-details-last-link"];
            }
            detail.selectedIndex = 0; [self settle];
            tag.sheetMusicUri = nil; tag.writtenKey = nil; tag.alternativeTitle = @""; tag.lyrics = nil; tag.notes = nil;
            [summary refreshView]; [self settle];
            XCTAssertTrue(lyrics.superview.hidden);
            XCTAssertEqual(columns.axis, UILayoutConstraintAxisVertical);
            XCTAssertTrue([[summary valueForKey:@"keySection"] isHidden]);
            XCTAssertTrue([[summary valueForKey:@"sheetMusicAction"] isHidden]);
            if (captureWide) [self captureComposition:@"wide-missing"];
        }
    }
}
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
            XCTAssertEqual([home.tableView numberOfRowsInSection:1], count.integerValue);
            if (count.integerValue > 0) {
                NSIndexPath *last = [NSIndexPath indexPathForRow:count.integerValue - 1 inSection:1];
                [home.tableView scrollToRowAtIndexPath:last atScrollPosition:UITableViewScrollPositionMiddle animated:NO]; [self settle];
                UITableViewCell *cell = [home.tableView cellForRowAtIndexPath:last];
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
                    [self checkLabelsIn:header];
                    XCTAssertGreaterThanOrEqual(browse.bounds.size.height, 44);
                    CGRect target = [browse convertRect:browse.bounds toView:teachableController.tableView];
                    [teachableController.tableView scrollRectToVisible:target animated:NO]; [self settle];
                    CGRect visible = UIEdgeInsetsInsetRect(teachableController.tableView.bounds, teachableController.tableView.adjustedContentInset);
                    XCTAssertTrue(CGRectContainsRect(visible, target), @"Full Browse action reachable at %@pt: %@ in %@", width, NSStringFromCGRect(target), NSStringFromCGRect(visible));
                    CGPoint center = [browse convertPoint:CGPointMake(CGRectGetMidX(browse.bounds), CGRectGetMidY(browse.bounds)) toView:self.window];
                    UIView *hit = [self.window hitTest:center withEvent:nil];
                    XCTAssertTrue(hit == browse || [hit isDescendantOfView:browse]);
                    UILabel *heading = (id)stack.arrangedSubviews.firstObject;
                    CGRect firstLine = [heading convertRect:CGRectMake(0, 0, heading.bounds.size.width, heading.font.lineHeight) toView:teachableController.tableView];
                    [teachableController.tableView scrollRectToVisible:firstLine animated:NO]; [self settle];
                    XCTAssertTrue(CGRectIntersectsRect(teachableController.tableView.bounds, firstLine));
                    NSLog(@"TM_LAYOUT_PROBE empty width=%@ content=%.1f browse=%@ reachable=1", width, teachableController.tableView.contentSize.height, NSStringFromCGRect(target));
                }
                ids = @[@1809]; [teachableController.tableView reloadData]; [self settle];
                XCTAssertNil(teachableController.tableView.tableHeaderView);
                XCTAssertEqual([teachableController.tableView numberOfRowsInSection:0], 1);
                ids = @[]; [teachableController.tableView reloadData]; [self settle];
                XCTAssertNotNil(teachableController.tableView.tableHeaderView);
            } else {
                XCTAssertNil(teachableController.tableView.tableHeaderView);
                NSIndexPath *last = [NSIndexPath indexPathForRow:count.integerValue - 1 inSection:0];
                for (NSNumber *width in @[@320, @834, @320]) for (NSNumber *editing in @[@YES, @NO, @YES]) {
                    self.window.frame = CGRectMake(0, 0, width.doubleValue, 852);
                    [teachableController setEditing:editing.boolValue animated:NO]; [self settle];
                    [teachableController.tableView scrollToRowAtIndexPath:last atScrollPosition:UITableViewScrollPositionBottom animated:NO]; [self settle];
                    UITableViewCell *cell = [teachableController.tableView cellForRowAtIndexPath:last];
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
                [teachableController.tableView scrollToRowAtIndexPath:last atScrollPosition:UITableViewScrollPositionBottom animated:NO]; [self settle];
                UITableViewCell *dense = [teachableController.tableView cellForRowAtIndexPath:last];
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
- (void)testMediaEmptyAndLongMetadataBounds {
    // Thumbnail fetches are the only remote boundary here; no video or audio opens.
    Method fetch = class_getClassMethod(DPRemoteLocation.class, @selector(dataWithContentsOfURL:error:));
    IMP oldFetch = method_getImplementation(fetch);
    IMP noNetwork = imp_implementationWithBlock(^NSData *(id owner, NSURL *url, NSError **error) { return nil; });
    method_setImplementation(fetch, noNetwork);
    @try {
        for (NSNumber *populated in @[@NO, @YES]) {
            DPTag *tag = [self layoutTag];
            tag.recordingMethod = @"Part predominant, one voice louder and other parts quieter. Practice each part in turn, then sing together and balance the chord.";
            if (populated.boolValue) {
                tag.tenorTrackUri = [DPRemoteLocation new]; tag.leadTrackUri = [DPRemoteLocation new];
                tag.baritoneTrackUri = [DPRemoteLocation new]; tag.bassTrackUri = [DPRemoteLocation new];
                tag.allPartsTrackUri = [DPRemoteLocation new]; tag.other1TrackUri = [DPRemoteLocation new];
                DPVideo *video = [DPVideo new]; video.sungBy = tag.sungBy; video.posted = tag.posted; video.sungKey = @"G minor"; video.youTubeCode = @"layout-no-fetch";
                DPVideo *missing = [DPVideo new]; missing.youTubeCode = @"layout-missing-no-fetch";
                tag.videos = @[video, missing]; tag.teachingVideo = @"layout-teacher-no-fetch"; tag.teacher = tag.provider;
            }
            DPTagTracksController *tracks = [DPTagTracksController new]; tracks.tag = tag;
            [self mount:tracks width:393 category:UIContentSizeCategoryAccessibilityExtraExtraExtraLarge];
            UITableView *table = [tracks valueForKey:@"partsTable"];
            XCTAssertEqual([table numberOfRowsInSection:0], populated.boolValue ? 6 : 0);
            [self checkLabelsIn:table.tableHeaderView];
            XCTAssertEqual([[tracks valueForKey:@"apology"] isHidden], populated.boolValue);
            if (populated.boolValue) {
                [table scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:5 inSection:0] atScrollPosition:UITableViewScrollPositionBottom animated:NO]; [self settle];
                XCTAssertNotNil([table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:5 inSection:0]]);
            }
            [self captureLayout:populated.boolValue ? @"tracks-long-ax5" : @"tracks-empty-ax5"];
            DPTagVideoController *videos = [DPTagVideoController new]; videos.tag = tag;
            [self mount:videos width:393 category:UIContentSizeCategoryAccessibilityExtraExtraExtraLarge];
            UITableView *videoTable = [videos valueForKey:@"tableView"];
            XCTAssertEqual(videoTable.numberOfSections, populated.boolValue ? 2 : 1);
            if (populated.boolValue) {
                for (NSIndexPath *path in @[[NSIndexPath indexPathForRow:0 inSection:0], [NSIndexPath indexPathForRow:0 inSection:1], [NSIndexPath indexPathForRow:1 inSection:1]]) {
                    [videoTable scrollToRowAtIndexPath:path atScrollPosition:UITableViewScrollPositionTop animated:NO]; [self settle];
                    UITableViewCell *cell = [videoTable cellForRowAtIndexPath:path]; XCTAssertNotNil(cell);
                    [self checkLabelsIn:cell.contentView];
                    if (path.section == 1 && path.row == 0) [self captureLayout:@"videos-long-ax5"];
                }
            } else [self captureLayout:@"videos-empty-ax5"];
        }
    } @finally { method_setImplementation(fetch, oldFetch); imp_removeBlock(noNetwork); }
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
            NSPredicate *done = [NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return !query.isLoading; }];
            XCTAssertEqual([XCTWaiter waitForExpectations:@[[[XCTNSPredicateExpectation alloc] initWithPredicate:done object:nil]] timeout:5], XCTWaiterResultCompleted);
            [self settle];
            UITableView *table = [query valueForKey:@"tagTable"];
            XCTAssertEqual([table numberOfRowsInSection:0], mode == 2 ? 1 : 0);
            if (mode < 2) {
                UILabel *label = [query valueForKey:@"statusLabel"]; [self assertWholeLabel:label];
                UIButton *retry = [query valueForKey:@"retryButton"];
                XCTAssertEqual(retry.hidden, mode == 0);
                self.window.frame = CGRectMake(0, 0, 852, 393); [self settle];
                if (mode == 1) {
                    CGRect rect = [retry convertRect:retry.bounds toView:table];
                    [table scrollRectToVisible:rect animated:NO];
                    XCTAssertTrue(CGRectContainsRect(CGRectInset(table.bounds, -.5, -.5), rect));
                    XCTAssertGreaterThanOrEqual(retry.bounds.size.height, 44);
                    [self captureLayout:@"query-error-landscape-ax5"];
                }
            } else [self checkLabelsIn:[table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]].contentView];
        }
    } @finally { method_setImplementation(method, original); imp_removeBlock(mock); }
}
- (void)testValidSheetPreviewKeyAndResizedBounds {
    TMTestSummary *summary = [TMTestSummary new];
    DPTag *tag = [self layoutTag]; tag.title = @"Four-part score layout fixture";
    TMTestLocation *location = [TMTestLocation new];
    location.type = @"pdf";
    location.uri = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:[NSUUID.UUID.UUIDString stringByAppendingPathExtension:@"pdf"]]];
    tag.sheetMusicUri = location; summary.tag = tag;
    UIGraphicsPDFRenderer *renderer = [[UIGraphicsPDFRenderer alloc] initWithBounds:CGRectMake(0, 0, 612, 792)];
    NSData *pdf = [renderer PDFDataWithActions:^(UIGraphicsPDFRendererContext *context) {
        [context beginPage];
        [@"TOP OF SCORE - four-part layout fixture" drawAtPoint:CGPointMake(32, 32) withAttributes:@{NSFontAttributeName:[UIFont systemFontOfSize:20]}];
        [@"END OF SCORE" drawAtPoint:CGPointMake(32, 746) withAttributes:@{NSFontAttributeName:[UIFont systemFontOfSize:20]}];
        for (NSInteger staff = 0; staff < 4; staff++) for (NSInteger line = 0; line < 5; line++) {
            UIBezierPath *path = [UIBezierPath bezierPath];
            [path moveToPoint:CGPointMake(32, 130 + staff * 140 + line * 10)];
            [path addLineToPoint:CGPointMake(580, 130 + staff * 140 + line * 10)]; [path stroke];
        }
    }];
    XCTAssertTrue([pdf writeToURL:location.uri atomically:YES]);
    @try {
        [self mount:summary width:393 category:UIContentSizeCategoryLarge];
        [summary openSheetMusic];
        NSPredicate *ready = [NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return summary.captured != nil; }];
        XCTAssertEqual([XCTWaiter waitForExpectations:@[[[XCTNSPredicateExpectation alloc] initWithPredicate:ready object:nil]] timeout:5], XCTWaiterResultCompleted);
        QLPreviewController *preview = (id)summary.captured;
        XCTAssertTrue([preview isKindOfClass:NSClassFromString(@"QLPreviewController")]);
        // Quick Look inserts its own Share/Markup items on iPad. The app's key
        // must remain present, but need not remain the rightmost bar item.
        UIBarButtonItem *keyItem = preview.navigationItem.rightBarButtonItem;
        UIViewController *root = [UIViewController new];
        UINavigationController *navigation = [[UINavigationController alloc] initWithRootViewController:root];
        TMLayoutNavigationObserver *observer = [TMLayoutNavigationObserver new];
        navigation.delegate = observer;
        [self mount:navigation width:393 category:UIContentSizeCategoryLarge];
        XCTNSPredicateExpectation *rootReady = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return observer.shown == root; }] object:nil];
        XCTAssertEqual([XCTWaiter waitForExpectations:@[rootReady] timeout:3], XCTWaiterResultCompleted);
        [navigation pushViewController:preview animated:NO];
        XCTNSPredicateExpectation *previewReady = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return observer.shown == preview && !preview.transitionCoordinator; }] object:nil];
        XCTAssertEqual([XCTWaiter waitForExpectations:@[previewReady] timeout:5], XCTWaiterResultCompleted);
        for (NSNumber *landscape in @[@NO, @YES]) {
            self.window.frame = landscape.boolValue ? CGRectMake(0, 0, 852, 393) : CGRectMake(0, 0, 393, 852);
            [self settle];
            XCTNSPredicateExpectation *fitted = [[XCTNSPredicateExpectation alloc] initWithPredicate:[NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) {
                return preview.currentPreviewItem != nil && [preview.navigationItem.rightBarButtonItems containsObject:keyItem] && keyItem.customView.window == self.window && keyItem.customView.bounds.size.width > 0 && !preview.transitionCoordinator;
            }] object:nil];
            XCTAssertEqual([XCTWaiter waitForExpectations:@[fitted] timeout:5], XCTWaiterResultCompleted);
            [self settle];
            XCTAssertTrue([preview.navigationItem.rightBarButtonItems containsObject:keyItem]);
            UIView *key = keyItem.customView;
            CGRect face = [key convertRect:key.bounds toView:self.window];
            CGRect target = CGRectMake(CGRectGetMidX(face) - 22, CGRectGetMidY(face) - 22, 44, 44);
            XCTAssertTrue(self.window.isKeyWindow);
            XCTAssertEqual(key.window, self.window);
            BOOL hits = YES;
            for (CGFloat x = CGRectGetMinX(target) + .5; x < CGRectGetMaxX(target); x += 1) for (CGFloat y = CGRectGetMinY(target) + .5; y < CGRectGetMaxY(target); y += 1) {
                UIView *hit = [self.window hitTest:CGPointMake(x, y) withEvent:nil];
                if (hit != key && ![hit isDescendantOfView:key]) hits = NO;
            }
            NSLog(@"TM_LAYOUT_PROBE sheet face=%@ nav=%@ target44hits=%d translates=%d intrinsic=%@", NSStringFromCGRect(face), NSStringFromCGRect(navigation.navigationBar.frame), hits, key.translatesAutoresizingMaskIntoConstraints, NSStringFromCGSize(key.intrinsicContentSize));
            XCTAssertGreaterThanOrEqual(key.bounds.size.width, 44);
            XCTAssertTrue(hits, @"Native hit testing must reach the key across a 44pt square, independently of its visible face");
            XCTAssertTrue(CGRectContainsRect(self.window.bounds, [key convertRect:key.bounds toView:self.window]));
            XCTAssertGreaterThan(preview.view.bounds.size.height, 200);
            // Only chrome and hit testing are asserted here. Actual Quick Look
            // rendering and device rotation belong to the separate native UI test.
        }
        [navigation popViewControllerAnimated:NO]; XCTAssertEqual(navigation.viewControllers.count, 1);
    } @finally {
        [NSFileManager.defaultManager removeItemAtURL:location.uri error:nil];
        [NSFileManager.defaultManager removeItemAtPath:[DPFileCache pathForKey:location.cacheKey] error:nil];
    }
}

- (void)testNativeDetailsSummaryAndSettingsCaptureBatch {
    DPTag *tag = [self layoutTag];
    for (NSNumber *large in @[@NO, @YES]) {
        DPTagViewController *detail = [DPTagViewController new];
        CGFloat width = UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad ? 834 : 393;
        [self mount:[[UINavigationController alloc] initWithRootViewController:detail] width:width category:large.boolValue ? UIContentSizeCategoryAccessibilityExtraExtraExtraLarge : UIContentSizeCategoryLarge];
        self.window.overrideUserInterfaceStyle = large.boolValue ? UIUserInterfaceStyleDark : UIUserInterfaceStyleLight;
        [detail setValue:tag forKey:@"tag"]; [self settle];
        [self captureLayout:large.boolValue ? @"summary-long-dark-ax5" : @"summary-light-default"];
        detail.selectedIndex = 1; [self settle];
        [self captureLayout:large.boolValue ? @"details-dark-ax5" : @"details-light-default"];
        if (large.boolValue) {
            DPTagDetailController *details = [detail valueForKey:@"detailController"];
            UIView *last = [details valueForKey:@"yearSungLabel"];
            UIStackView *root = [details valueForKey:@"metadataStack"];
            UIScrollView *scroll = (id)root.superview.superview;
            [scroll scrollRectToVisible:[last convertRect:last.bounds toView:scroll] animated:NO];
            [self captureLayout:@"details-final-row-dark-ax5"];
        }
    }
    TMLayoutSettings *settings = [TMLayoutSettings new];
    [self mount:[[UINavigationController alloc] initWithRootViewController:settings] width:320 category:UIContentSizeCategoryLarge];
    UITableView *table = [settings valueForKey:@"tableView"];
    [table scrollToRowAtIndexPath:[NSIndexPath indexPathForRow:3 inSection:2] atScrollPosition:UITableViewScrollPositionBottom animated:NO];
    [self captureLayout:@"settings-narrow-default"];
}
@end

// In-memory Home fixtures never write favorites or contact the tag provider.
@interface TMFooterPitchTests : TMPolishRegressionTests
@property UIWindow *window;
@property UIWindow *previousWindow;
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
    XCTestExpectation *turn = [self expectationWithDescription:@"native layout"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0.12 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{ [turn fulfill]; });
    [self waitForExpectations:@[turn] timeout:2];
    [self.window layoutIfNeeded];
    [CATransaction flush];
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
    slot.hidden = YES; [self settle]; XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
    slot.hidden = NO; [self settle]; XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
    pole.frame = CGRectOffset(pole.frame, 0, 100); [self settle]; XCTAssertNil([stripes animationForKey:@"rotationStripes"]);
    pole.frame = CGRectOffset(pole.frame, 0, -100); [self settle]; XCTAssertNotNil([stripes animationForKey:@"rotationStripes"]);
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
- (void)testCompactHomeButtonsAndRefreshPending {
    for (NSNumber *dark in @[@NO, @YES]) {
        NSString *prefix = [NSString stringWithFormat:@"tagmaster-consistent-loading-ios-%@", dark.boolValue ? @"dark" : @"light"];
        Method query = class_getClassMethod(DPTag.class, @selector(query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:minimumRating:minimumDownloads:cache:fieldList:));
        IMP oldQuery = method_getImplementation(query);
        dispatch_semaphore_t gate = dispatch_semaphore_create(0);
        IMP queryMock = imp_implementationWithBlock(^DPTagQueryResult *(id cls, NSString *q, int n, int start, NSNumber *parts, NSNumber *tracks, NSNumber *sheet, enum DPTagCollection collection, enum DPTagSortOptions sort, NSNumber *rating, NSNumber *downloads, BOOL cache, NSString *fields) {
            dispatch_semaphore_wait(gate, dispatch_time(DISPATCH_TIME_NOW, 20 * NSEC_PER_SEC));
            [NSException raise:@"offline" format:@"Controlled pending fixture"]; return nil;
        });
        method_setImplementation(query, queryMock);
        TMTestHome *home = [TMTestHome new];
        @try {
            [self mount:home width:UIScreen.mainScreen.bounds.size.width dark:dark.boolValue large:YES];
            NSDictionary *item = [[home navigationItems] filteredArrayUsingPredicate:[NSPredicate predicateWithFormat:@"title == 'Random Tag'"]].firstObject;
            ((void (^)(void))item[@"action"])(); [self settle];
            NSIndexPath *index = [home performSelector:NSSelectorFromString(@"randomTagIndexPath")];
            [home.tableView scrollToRowAtIndexPath:index atScrollPosition:UITableViewScrollPositionMiddle animated:NO]; [self settle];
            UITableViewCell *cell = [home.tableView cellForRowAtIndexPath:index];
            TMBarberPoleLoadingView *pole = (id)cell.accessoryView; [self assertCompact:pole pending:YES];
            XCTAssertEqualObjects(cell.accessibilityLabel, @"Random Tag, loading"); XCTAssertFalse(pole.isAccessibilityElement);
            [self capture:[prefix stringByAppendingString:@"-random-row"]];
            dispatch_semaphore_signal(gate);
            [self waitUntil:^BOOL { return home.retry != nil; }]; [self settle];
            XCTAssertNil([home.tableView cellForRowAtIndexPath:index].accessoryView);
        } @finally { dispatch_semaphore_signal(gate); method_setImplementation(query, oldQuery); imp_removeBlock(queryMock); }

        TMHeldRatingTag *tag = [TMHeldRatingTag new]; tag.title = @"Lost"; tag.parts = 4; tag.rating = 4.5; tag.writtenKey = @"C";
        tag.gate = dispatch_semaphore_create(0); tag.failRating = YES;
        TMTestLocation *location = [TMTestLocation new]; location.type = @"pdf";
        location.uri = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:NSUUID.UUID.UUIDString]];
        tag.sheetMusicUri = location;
        TMTestSummary *summary = [TMTestSummary new]; summary.busyIndicator = [TMBusyIndicator new]; summary.tag = tag;
        [self mount:summary width:UIScreen.mainScreen.bounds.size.width dark:dark.boolValue large:NO];
        UIButton *button = [summary valueForKey:@"ratingButton"], *sheetButton = [summary valueForKey:@"sheetMusicButton"];
        CGRect ratingFrame = button.frame, sheetFrame = sheetButton.frame;
        UIImage *star = button.configuration.image, *sheetIcon = sheetButton.configuration.image;
        [summary rateTag:4]; [self settle];
        TMBarberPoleLoadingView *ratingPole = [summary valueForKey:@"ratingLoading"];
        [self assertCompact:ratingPole pending:YES]; XCTAssertFalse(button.enabled);
        XCTAssertEqualObjects(button.configuration.image, star);
        [self capture:[prefix stringByAppendingString:@"-rating"]];
        dispatch_semaphore_signal(tag.gate);
        [self waitUntil:^BOOL { return summary.retry != nil; }]; [self settle];
        [self assertCompact:ratingPole pending:NO]; XCTAssertTrue(button.enabled);
        XCTAssertTrue(CGRectEqualToRect(ratingFrame, button.frame)); XCTAssertEqualObjects(button.configuration.image, star);
        tag.gate = nil; tag.failRating = NO; summary.retry();
        [self waitUntil:^BOOL { return [button.accessibilityLabel isEqualToString:@"Rating submitted"]; }];
        [self assertCompact:ratingPole pending:NO]; XCTAssertEqualObjects(button.configuration.image, star);

        Method download = class_getClassMethod(DPRemoteLocation.class, @selector(dataWithContentsOfURL:error:));
        IMP oldDownload = method_getImplementation(download);
        dispatch_semaphore_t sheetGate = dispatch_semaphore_create(0);
        IMP downloadMock = imp_implementationWithBlock(^NSData *(id cls, NSURL *url, NSError **error) {
            dispatch_semaphore_wait(sheetGate, dispatch_time(DISPATCH_TIME_NOW, 20 * NSEC_PER_SEC)); return nil;
        });
        method_setImplementation(download, downloadMock); summary.retry = nil;
        @try {
            [summary openSheetMusic]; [self settle];
            TMBarberPoleLoadingView *sheetPole = [summary valueForKey:@"sheetMusicLoading"];
            [self assertCompact:sheetPole pending:YES]; XCTAssertFalse(sheetButton.enabled);
            XCTAssertEqualObjects(sheetButton.configuration.image, sheetIcon);
            CGRect poleFrame = [sheetPole convertRect:sheetPole.bounds toView:sheetButton];
            XCTAssertTrue(CGRectContainsRect(sheetButton.bounds, poleFrame), @"Pending pole stays inside full-width action");
            XCTAssertFalse(CGRectIntersectsRect(poleFrame, [sheetButton.titleLabel convertRect:sheetButton.titleLabel.bounds toView:sheetButton]), @"Pole must not cover the operation label");
            XCTAssertFalse(CGRectIntersectsRect(poleFrame, [sheetButton.imageView convertRect:sheetButton.imageView.bounds toView:sheetButton]), @"Pole must not cover the action icon");
            [self capture:[prefix stringByAppendingString:@"-sheet-button"]];
            dispatch_semaphore_signal(sheetGate); [self waitUntil:^BOOL { return summary.retry != nil; }]; [self settle];
            [self assertCompact:sheetPole pending:NO]; XCTAssertTrue(sheetButton.enabled);
            XCTAssertEqualObjects(sheetButton.configuration.image, sheetIcon); XCTAssertTrue(CGRectEqualToRect(sheetFrame, sheetButton.frame));
        } @finally { dispatch_semaphore_signal(sheetGate); method_setImplementation(download, oldDownload); imp_removeBlock(downloadMock); }

        Method fetch = class_getInstanceMethod(DPTagViewController.class, @selector(fetchTagId:refresh:completion:));
        IMP oldFetch = method_getImplementation(fetch);
        __block void (^complete)(DPTag *);
        IMP fetchMock = imp_implementationWithBlock(^(DPTagViewController *controller, int identifier, BOOL refresh, void (^completion)(DPTag *)) { complete = [completion copy]; });
        method_setImplementation(fetch, fetchMock);
        @try {
            TMTestDetail *detail = [TMTestDetail new]; detail.tagId = 1809; complete(tag);
            [self mount:detail width:UIScreen.mainScreen.bounds.size.width dark:dark.boolValue large:NO];
            [detail loadTag:YES]; [self settle];
            TMBarberPoleLoadingView *navPole = (id)((UIBarButtonItem *)[detail valueForKey:@"loadingBarButton"]).customView;
            [self assertCompact:navPole pending:YES]; XCTAssertTrue(navPole.darkSurface);
            XCTAssertEqual(detail.navigationItem.rightBarButtonItems.count, 3);
            [self capture:[prefix stringByAppendingString:@"-refresh-nav"]];
            [detail beginAppearanceTransition:NO animated:NO]; [detail endAppearanceTransition];
            XCTAssertNil([[navPole valueForKey:@"stripes"] animationForKey:@"rotationStripes"]);
            [detail beginAppearanceTransition:YES animated:NO]; [detail endAppearanceTransition];
            complete(nil); [self settle]; [self assertCompact:navPole pending:NO];
            XCTAssertEqual([detail valueForKey:@"tag"], tag); XCTAssertTrue([detail.navigationItem.rightBarButtonItems containsObject:[detail valueForKey:@"refreshBarButton"]]);
            [detail loadTag:YES]; complete(tag); [self settle]; [self assertCompact:navPole pending:NO];
        } @finally { method_setImplementation(fetch, oldFetch); imp_removeBlock(fetchMock); }
    }
}
- (void)testCompactTrackAccessoryReadinessAndFailure {
    Method getter = class_getInstanceMethod(AVPlayerItem.class, @selector(status));
    IMP original = method_getImplementation(getter);
    __block AVPlayerItemStatus status = AVPlayerItemStatusUnknown;
    __block AVPlayerItem *observed;
    IMP mock = imp_implementationWithBlock(^AVPlayerItemStatus(AVPlayerItem *item) { observed = item; return status; });
    method_setImplementation(getter, mock);
    @try {
        TMTestTracks *tracks = [TMTestTracks new]; tracks.busyIndicator = [TMBusyIndicator new];
        DPTag *tag = [self tag]; tag.title = @"Lost";
        TMTestLocation *location = [TMTestLocation new]; location.type = @"mp3";
        location.uri = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:NSUUID.UUID.UUIDString]];
        tag.tenorTrackUri = location; tracks.tag = tag;
        [self mount:tracks width:UIScreen.mainScreen.bounds.size.width dark:YES large:YES];
        UITableView *table = [tracks valueForKey:@"partsTable"]; NSIndexPath *index = [NSIndexPath indexPathForRow:0 inSection:0];
        [table.delegate tableView:table didSelectRowAtIndexPath:index]; [self settle];
        UITableViewCell *cell = [table cellForRowAtIndexPath:index];
        TMBarberPoleLoadingView *pole = (id)cell.accessoryView; [self assertCompact:pole pending:YES];
        [self capture:@"tagmaster-consistent-loading-ios-dark-track-row"];
        XCTAssertNotNil(observed);
        [observed willChangeValueForKey:@"status"]; status = AVPlayerItemStatusFailed; [observed didChangeValueForKey:@"status"];
        [self waitUntil:^BOOL { return tracks.retry != nil; }];
        XCTAssertNil(cell.accessoryView); XCTAssertFalse(pole.isAnimating); XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
        status = AVPlayerItemStatusUnknown; tracks.retry(); [self settle];
        TMBarberPoleLoadingView *retryPole = (id)cell.accessoryView; [self assertCompact:retryPole pending:YES];
        // If the table changes its accessory meanwhile, an old completion must not clear it.
        UIView *replacement = [UIView new]; cell.accessoryView = replacement;
        [observed willChangeValueForKey:@"status"]; status = AVPlayerItemStatusReadyToPlay; [observed didChangeValueForKey:@"status"];
        [self waitUntil:^BOOL { return [tracks.captured isKindOfClass:AVPlayerViewController.class]; }];
        XCTAssertEqual(cell.accessoryView, replacement); XCTAssertFalse(retryPole.isAnimating); XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
    } @finally { method_setImplementation(getter, original); imp_removeBlock(mock); }
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
            XCTAssertEqual([home.tableView numberOfRowsInSection:1], 1);
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
        XCTAssertEqualWithAccuracy(CGColorGetAlpha(fill.CGColor), 0, 0.01);
        XCTAssertGreaterThan([self pixelsIn:button matching:button.tintColor], 5);
    }
    XCTAssertEqualWithAccuracy(button.layer.borderWidth, 1.5, 0.01);
    XCTAssertEqualWithAccuracy(button.layer.cornerRadius, 8, 0.01);
}
- (void)waitForPitchInterval:(NSTimeInterval)interval {
    XCTestExpectation *deadline = [self expectationWithDescription:@"cross timed activation deadline"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, interval * NSEC_PER_SEC), dispatch_get_main_queue(), ^{ [deadline fulfill]; });
    [self waitForExpectations:@[deadline] timeout:interval + 2];
}
- (void)testPitchTimedCancellationCannotStopLaterHold {
    DPTagSummaryController *summary = [DPTagSummaryController new];
    [summary loadViewIfNeeded]; summary.tag = [self tag];
    [self mount:summary width:UIScreen.mainScreen.bounds.size.width dark:NO large:NO];
    DPPitchPipeButton *pitch = [summary valueForKey:@"keyButton"];
    UIButton *button = pitch.button;
    // Retain the cancelled owner across its deadline, just as a previous screen can remain alive.
    TMTestSummary *previous = [TMTestSummary new];
    [previous loadViewIfNeeded]; previous.tag = [self tag];
    DPPitchPipeButton *previousPitch = [previous valueForKey:@"keyButton"];
    XCTAssertEqual(previousPitch.note, pitch.note);
    XCTAssertTrue([previousPitch.button accessibilityActivate]);
    [previousPitch.button sendActionsForControlEvents:UIControlEventTouchCancel];
    XCTAssertFalse(pitch.note.isPlaying);
    CGRect idle = button.frame;
    [button sendActionsForControlEvents:UIControlEventTouchDown];
    XCTAssertTrue(pitch.note.isPlaying);
    [self waitForPitchInterval:1.65];
    [self logPitch:button phase:@"held past cancelled deadline"];
    XCTAssertTrue(pitch.note.isPlaying, @"A cancelled activation must not stop a later same-note hold");
    [self assertKey:button playing:YES];
    XCTAssertTrue(CGRectEqualToRect(idle, button.frame));
    [button sendActionsForControlEvents:UIControlEventTouchCancel];
    XCTAssertFalse(pitch.note.isPlaying);
    [self assertKey:button playing:NO];
    XCTAssertNotNil(previous.view); // Keep the prior controller alive through the check.

    for (NSString *interruption in @[@"touch", @"replacement", @"detach"]) {
        DPTagSummaryController *owner = [DPTagSummaryController new];
        [owner loadViewIfNeeded]; owner.tag = [self tag];
        [self mount:owner width:UIScreen.mainScreen.bounds.size.width dark:NO large:NO];
        DPPitchPipeButton *activePitch = [owner valueForKey:@"keyButton"];
        XCTAssertTrue([activePitch.button accessibilityActivate]);
        if ([interruption isEqualToString:@"replacement"]) {
            DPNote *original = activePitch.note;
            activePitch.note = DPNote.commonNotes.lastObject;
            XCTAssertFalse(original.isPlaying);
            activePitch.note = original;
        } else if ([interruption isEqualToString:@"detach"]) {
            [activePitch.button removeFromSuperview];
            XCTAssertFalse(activePitch.note.isPlaying);
            XCTAssertNil([activePitch.button valueForKey:@"noteTimer"]);
            DPTagSummaryController *later = [DPTagSummaryController new];
            [later loadViewIfNeeded]; later.tag = [self tag];
            [self mount:later width:UIScreen.mainScreen.bounds.size.width dark:NO large:NO];
            activePitch = [later valueForKey:@"keyButton"];
        }
        UIButton *held = activePitch.button;
        CGRect frame = held.frame;
        [held sendActionsForControlEvents:UIControlEventTouchDown];
        [self waitForPitchInterval:1.65];
        XCTAssertTrue(activePitch.note.isPlaying, @"%@ must invalidate the previous deadline", interruption);
        [self assertKey:held playing:YES];
        XCTAssertTrue(CGRectEqualToRect(frame, held.frame));
        [held sendActionsForControlEvents:UIControlEventTouchCancel];
        [self assertKey:held playing:NO];
        XCTAssertNotNil(owner.view);
    }
}
- (void)testPitchTimedReactivationKeepsNewDeadline {
    DPTagSummaryController *summary = [DPTagSummaryController new];
    [summary loadViewIfNeeded]; summary.tag = [self tag];
    [self mount:summary width:UIScreen.mainScreen.bounds.size.width dark:NO large:NO];
    DPPitchPipeButton *pitch = [summary valueForKey:@"keyButton"];
    UIButton *button = pitch.button;
    XCTAssertTrue([button accessibilityActivate]);
    [self waitForPitchInterval:0.9];
    XCTAssertTrue([button accessibilityActivate]);
    [self waitForPitchInterval:0.75];
    [self logPitch:button phase:@"reactivated past first deadline"];
    XCTAssertTrue(pitch.note.isPlaying, @"The first activation must not shorten the second activation");
    XCTAssertTrue([[button valueForKey:@"showingPlayback"] boolValue]);
    XCTAssertGreaterThan([self pixelsIn:button matching:button.configuration.background.backgroundColor], button.bounds.size.width * button.bounds.size.height * .45);
    [self waitUntil:^BOOL { return !pitch.note.isPlaying; }];
    [self assertKey:button playing:NO];
}
- (void)testPitchRenderedLifecycle {
    for (NSNumber *dark in @[@NO, @YES]) {
        DPTagViewController *detail = [DPTagViewController new];
        [detail loadViewIfNeeded];
        DPTag *tag = [self tag]; tag.title = @"Lost"; tag.alternativeTitle = @"In Your Eyes";
        [detail setValue:tag forKey:@"tag"];
        [self mount:detail width:UIScreen.mainScreen.bounds.size.width dark:dark.boolValue large:NO];
        DPTagSummaryController *summary = [detail valueForKey:@"summaryController"];
        DPPitchPipeButton *pitch = [summary valueForKey:@"keyButton"];
        UIButton *button = pitch.button;
        NSString *prefix = [NSString stringWithFormat:@"tagmaster-ios-footer-pitch-%@", dark.boolValue ? @"dark" : @"light"];
        [self assertKey:button playing:NO];
        [self capture:[prefix stringByAppendingString:@"-idle"]];
        CGRect idle = button.frame;
        XCTAssertEqual(summary.tabBarController.selectedViewController, summary);
        XCTAssertEqual(button.window, self.window);
        XCTAssertTrue([[button valueForKey:@"noteTimer"] isValid]);
        XCTAssertNil(summary.transitionCoordinator);
        [self logPitch:button phase:@"before first gesture"];
        // Deterministic native held-touch fixture invokes the real shared sound targets.
        button.highlighted = YES;
        [button sendActionsForControlEvents:UIControlEventTouchDown];
        XCTAssertTrue(pitch.note.isPlaying);
        [self assertKey:button playing:YES];
        [self capture:[prefix stringByAppendingString:@"-held"]];
        XCTAssertTrue(CGRectEqualToRect(idle, button.frame));
        [button sendActionsForControlEvents:UIControlEventTouchUpInside]; button.highlighted = NO;
        XCTAssertFalse(pitch.note.isPlaying);
        [self assertKey:button playing:NO];
        [self capture:[prefix stringByAppendingString:@"-released"]];
        XCTAssertTrue(CGRectEqualToRect(idle, button.frame), @"Released pitch keeps its visible face");
        [button sendActionsForControlEvents:UIControlEventTouchDown];
        [button sendActionsForControlEvents:UIControlEventTouchCancel]; button.highlighted = NO;
        XCTAssertFalse(pitch.note.isPlaying); [self assertKey:button playing:NO];
        button.enabled = NO;
        XCTAssertFalse([button accessibilityActivate]); [self assertKey:button playing:NO];
        button.enabled = YES;
        XCTAssertTrue([button accessibilityActivate]); [self assertKey:button playing:YES];
        [self waitUntil:^BOOL { return !pitch.note.isPlaying; }]; [self assertKey:button playing:NO];
        pitch.toggle = YES;
        [button sendActionsForControlEvents:UIControlEventTouchDown];
        [button sendActionsForControlEvents:UIControlEventTouchUpInside];
        [self assertKey:button playing:YES];
        [button sendActionsForControlEvents:UIControlEventTouchDown];
        [button sendActionsForControlEvents:UIControlEventTouchUpInside];
        [self assertKey:button playing:NO]; pitch.toggle = NO;
        [button sendActionsForControlEvents:UIControlEventTouchDown];
        DPNote *old = pitch.note; pitch.note = [DPNote commonNotes].lastObject;
        XCTAssertFalse(old.isPlaying); [self assertKey:button playing:NO];
        pitch.note = tag.keyNote;
        [button sendActionsForControlEvents:UIControlEventTouchDown];
        [button removeFromSuperview];
        XCTAssertFalse(pitch.note.isPlaying);
        XCTAssertNil([button valueForKey:@"noteTimer"]);
        [self assertKey:button playing:NO];
    }
}
// Render the production layer tree at explicit local-axis phases. These checks run
// while the real query is held, not against a replacement illustration fixture.
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
        if (calls == 1) { [entered fulfill]; dispatch_semaphore_wait(gate, dispatch_time(DISPATCH_TIME_NOW, 15 * NSEC_PER_SEC)); }
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
        // Sample the live presentation layer at two bounded points in its loop.
        [self capture:@"tagmaster-ios-shared-vector-light-phase1"];
        CGFloat first = stripes.presentationLayer.transform.m42;
        XCTestExpectation *phase = [self expectationWithDescription:@"second stripe phase"];
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0.6 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{ [phase fulfill]; });
        [self waitForExpectations:@[phase] timeout:2];
        [self capture:@"tagmaster-ios-shared-vector-light-phase2"];
        CGFloat second = stripes.presentationLayer.transform.m42;
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

- (void)testSheetKeyRenderedLifecycle {
    TMTestSummary *summary = [TMTestSummary new];
    [summary loadViewIfNeeded];
    DPTag *tag = [self tag];
    DPRemoteLocation *location = [TMTestLocation new];
    location.uri = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:NSUUID.UUID.UUIDString]];
    location.type = @"pdf"; tag.sheetMusicUri = location; summary.tag = tag;
    [@"%PDF-1.4 test" writeToURL:location.uri atomically:YES encoding:NSUTF8StringEncoding error:nil];
    @try {
        [summary openSheetMusic];
        [self waitUntil:^BOOL { return summary.captured != nil; }];
        UIView *keyView = summary.captured.navigationItem.rightBarButtonItem.customView;
        UIButton *button = (id)keyView.subviews.firstObject;
        XCTAssertTrue([button isKindOfClass:UIButton.class]);
        XCTAssertEqualObjects(button.accessibilityIdentifier, @"sheet.key");
        UIViewController *host = [UIViewController new];
        host.navigationItem.rightBarButtonItem = summary.captured.navigationItem.rightBarButtonItem;
        [self mount:host width:UIScreen.mainScreen.bounds.size.width dark:NO large:NO];
        [self assertKey:button playing:NO];
        [self capture:@"tagmaster-ios-footer-pitch-sheet-idle"];
        [button sendActionsForControlEvents:UIControlEventTouchDown]; [self assertKey:button playing:YES];
        [self capture:@"tagmaster-ios-footer-pitch-sheet-held"];
        [button sendActionsForControlEvents:UIControlEventTouchCancel]; [self assertKey:button playing:NO];
        [self capture:@"tagmaster-ios-footer-pitch-sheet-cancelled"];
        for (NSNumber *release in @[@(UIControlEventTouchUpInside), @(UIControlEventTouchUpOutside)]) {
            [button sendActionsForControlEvents:UIControlEventTouchDown]; [self assertKey:button playing:YES];
            [button sendActionsForControlEvents:release.unsignedIntegerValue]; [self assertKey:button playing:NO];
        }
        XCTAssertTrue([button accessibilityActivate]); [self assertKey:button playing:YES];
        [self waitUntil:^BOOL { return !tag.keyNote.isPlaying; }]; [self assertKey:button playing:NO];
    } @finally {
        [tag.keyNote stop];
        [NSFileManager.defaultManager removeItemAtURL:location.uri error:nil];
        [NSFileManager.defaultManager removeItemAtPath:[DPFileCache pathForKey:location.cacheKey] error:nil];
    }
}
@end
