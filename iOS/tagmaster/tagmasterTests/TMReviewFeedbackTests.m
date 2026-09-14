#import <XCTest/XCTest.h>
#import <AVKit/AVKit.h>
#import <objc/runtime.h>
#import <limits.h>
#import "DPAppDelegate.h"
#import "DPTagTracksController.h"
#import "DPFileCache.h"
#import "TMBarberPoleLoadingView.h"
#import "tagmaster-Swift.h"

@interface TMReviewAuth : NSObject
@property BOOL accepts;
@property NSUInteger calls;
- (BOOL)canHandleURL:(NSURL *)url;
@end
@implementation TMReviewAuth
- (BOOL)canHandleURL:(NSURL *)url { self.calls++; return self.accepts; }
@end

@interface TMReviewRoutingTests : XCTestCase
@property (nonatomic, strong) TMReviewAuth *auth;
@property Method authMethod;
@property IMP originalAuth;
@property IMP fakeAuth;
@end

@implementation TMReviewRoutingTests
- (void)setUp {
    [super setUp];
    self.auth = [TMReviewAuth new];
    self.authMethod = class_getClassMethod(NSClassFromString(@"FIRAuth"), NSSelectorFromString(@"auth"));
    XCTAssertNotEqual(self.authMethod, NULL);
    self.originalAuth = method_getImplementation(self.authMethod);
    TMReviewAuth *auth = self.auth;
    self.fakeAuth = imp_implementationWithBlock(^id(id cls) { return auth; });
    method_setImplementation(self.authMethod, self.fakeAuth);
}
- (void)tearDown {
    method_setImplementation(self.authMethod, self.originalAuth);
    imp_removeBlock(self.fakeAuth);
    [super tearDown];
}
- (void)checkURLs:(NSArray<NSString *> *)urls expected:(NSArray<NSNumber *> *)expected {
    Method show = class_getClassMethod(DPAppDelegate.class, @selector(showTagWithId:from:));
    IMP original = method_getImplementation(show);
    __block NSMutableArray *routed = [NSMutableArray array];
    IMP capture = imp_implementationWithBlock(^(id cls, int identifier, UIViewController *sender) {
        [routed addObject:@(identifier)];
    });
    method_setImplementation(show, capture);
    @try {
        DPAppDelegate *delegate = [DPAppDelegate new];
        [urls enumerateObjectsUsingBlock:^(NSString *text, NSUInteger index, BOOL *stop) {
            [routed removeAllObjects];
            NSURL *url = [NSURL URLWithString:text];
            XCTAssertNotNil(url, @"%@", text);
            BOOL handled = [delegate application:UIApplication.sharedApplication openURL:url options:@{}];
            XCTAssertEqual(handled, expected[index].intValue > 0, @"%@", text);
            XCTAssertEqualObjects(routed, expected[index].intValue > 0 ? @[expected[index]] : @[], @"%@", text);
        }];
    } @finally {
        method_setImplementation(show, original);
        imp_removeBlock(capture);
    }
}

- (void)testSupportedRoutesAndIntegerBoundaries {
    NSMutableArray *urls = [NSMutableArray array], *values = [NSMutableArray array];
    for (NSString *prefix in @[@"tagmaster://open/tag/", @"tagmaster://tag/", @"tagmaster:///tag/"]) {
        for (NSString *identifier in @[@"1", @"12", @"00012", @"2147483647"]) {
            [urls addObject:[prefix stringByAppendingString:identifier]];
            [values addObject:@(identifier.intValue)];
        }
    }
    [urls addObjectsFromArray:@[@"TAGMASTER://tag/1", @"tagmaster://open/tag/12?source=share#track", @"tagmaster://tag/%31"]];
    [values addObjectsFromArray:@[@1, @12, @1]];
    [self checkURLs:urls expected:values];
}

- (void)testMalformedIDsNeverRoute {
    NSMutableArray *urls = [NSMutableArray array], *values = [NSMutableArray array];
    for (NSString *prefix in @[@"tagmaster://open/tag/", @"tagmaster://tag/", @"tagmaster:///tag/"]) {
        for (NSString *identifier in @[@"0", @"000", @"-1", @"+1", @"12junk", @"1.0", @"2147483648",
                                       @"4294967297", @"99999999999999999999999999999999", @"１２", @"١٢", @"%2012", @"12%20", @"12%00", @"1%2F2", @""]) {
            [urls addObject:[prefix stringByAppendingString:identifier]];
            [values addObject:@0];
        }
    }
    [self checkURLs:urls expected:values];
}

- (void)testUnrelatedSchemesAndPathsNeverRoute {
    NSArray *urls = @[@"https://open/tag/12", @"other://tag/12", @"/tag/12", @"tagmaster://other/tag/12",
                      @"tagmaster://open/extra/tag/12", @"tagmaster://open/tag/12/extra", @"tagmaster://tag/tag/12",
                      @"tagmaster://open/tag//12", @"tagmaster://open/tag/12/", @"tagmaster://tag/12/",
                      @"tagmaster://user@open/tag/12", @"tagmaster://open:80/tag/12", @"tagmaster://open/not-tag/12"];
    NSMutableArray *values = [NSMutableArray array];
    for (__unused NSString *url in urls) [values addObject:@0];
    [self checkURLs:urls expected:values];
}

- (void)testAuthProviderGetsFirstRefusalBeforeTagValidation {
    self.auth.accepts = YES;
    DPAppDelegate *delegate = [DPAppDelegate new];
    for (NSString *text in @[@"review-auth://callback?code=local", @"tagmaster://tag/0"]) {
        XCTAssertTrue([delegate application:UIApplication.sharedApplication openURL:[NSURL URLWithString:text] options:@{}]);
    }
    XCTAssertEqual(self.auth.calls, 2);
}
@end

// A real AVPlayerItem with controlled status. Production still installs Foundation
// KVO and the actual AVPlayerItemFailedToPlayToEndTime notification observer.
@interface TMReviewItem : AVPlayerItem
@property AVPlayerItemStatus controlledStatus;
@property NSUInteger statusRemovals;
- (void)sendStatus:(AVPlayerItemStatus)status;
@end
@implementation TMReviewItem
- (AVPlayerItemStatus)status { return self.controlledStatus; }
- (void)sendStatus:(AVPlayerItemStatus)status {
    [self willChangeValueForKey:@"status"];
    self.controlledStatus = status;
    [self didChangeValueForKey:@"status"];
}
- (void)removeObserver:(NSObject *)observer forKeyPath:(NSString *)keyPath context:(void *)context {
    if ([keyPath isEqualToString:@"status"]) self.statusRemovals++;
    [super removeObserver:observer forKeyPath:keyPath context:context];
}
@end

// Do not attach the controlled item to AVFoundation's loader. Its real KVO is
// driven by the test, while the production detach/play/pause calls are recorded.
@interface TMReviewPlayer : AVPlayer
@property (nonatomic, strong) AVPlayerItem *controlledItem;
@property NSUInteger detaches;
@property NSUInteger pauses;
@property NSUInteger plays;
@end
@implementation TMReviewPlayer
- (AVPlayerItem *)currentItem { return self.controlledItem; }
- (void)replaceCurrentItemWithPlayerItem:(AVPlayerItem *)item {
    self.controlledItem = item;
    if (!item) self.detaches++;
}
- (void)pause { self.pauses++; }
- (void)play { self.plays++; }
@end

@interface TMReviewBusy : DPBusyIndicator
@property NSUInteger settlements;
@end
@implementation TMReviewBusy
- (void)decrementBusyCount {
    NSAssert(NSThread.isMainThread, @"Busy delivery must be on main");
    self.settlements++;
    [super decrementBusyCount];
}
@end

@interface TMReviewLocation : DPRemoteLocation
@property (nonatomic, copy) NSString *testCacheKey;
@end
@implementation TMReviewLocation
- (NSString *)cacheKey { return self.testCacheKey; }
@end

@interface TMReviewTracks : DPTagTracksController
@property (nonatomic, strong) AVPlayerItem *lastItem;
@property (nonatomic, strong) AVPlayer *lastPlayer;
@property (nonatomic, strong) NSURL *requestedURL;
@property (nonatomic, strong) AVPlayerViewController *capturedPlayer;
@property (nonatomic, copy) void (^retry)(void);
@property (nonatomic, copy) void (^presentationCompletion)(void);
@property NSUInteger errors;
@property NSUInteger presentations;
@property NSTimeInterval testTimeout;
@property BOOL nativePresentation;
@property BOOL realPlayback;
@property BOOL delayPresentationCompletion;
@end
@implementation TMReviewTracks
- (NSTimeInterval)playbackReadyTimeout { return self.testTimeout > 0 ? self.testTimeout : 30; }
- (AVPlayerItem *)makePlaybackItemWithUrl:(NSURL *)url {
    self.requestedURL = url;
    self.lastItem = self.realPlayback ? [super makePlaybackItemWithUrl:url] : [[TMReviewItem alloc] initWithURL:url];
    return self.lastItem;
}
- (AVPlayer *)makePlaybackPlayerWithItem:(AVPlayerItem *)item {
    if (self.realPlayback) self.lastPlayer = [super makePlaybackPlayerWithItem:item];
    else {
        TMReviewPlayer *player = [TMReviewPlayer new];
        player.controlledItem = item;
        self.lastPlayer = player;
    }
    return self.lastPlayer;
}
- (void)presentViewController:(UIViewController *)controller animated:(BOOL)animated completion:(void (^)(void))completion {
    NSAssert(NSThread.isMainThread, @"Presentation must be on main");
    if ([controller isKindOfClass:AVPlayerViewController.class]) {
        self.capturedPlayer = (id)controller;
        self.presentations++;
    }
    if (self.nativePresentation) [super presentViewController:controller animated:NO completion:completion];
    else if (self.delayPresentationCompletion) self.presentationCompletion = completion;
    else if (completion) completion();
}
- (UIViewController *)presentedViewController {
    return self.nativePresentation ? super.presentedViewController : self.capturedPlayer;
}
- (void)tm_showError:(NSString *)message retry:(void (^)(void))retry {
    NSAssert(NSThread.isMainThread, @"Recovery must be on main");
    self.errors++;
    self.retry = retry;
    if (self.nativePresentation) {
        NSAssert(![self.presentedViewController isKindOfClass:AVPlayerViewController.class], @"Dismiss player before showing recovery");
        [super tm_showError:message retry:retry];
    }
}
@end

@interface TMReviewPlaybackTests : XCTestCase
@property (nonatomic, strong) NSMutableArray<NSURL *> *files;
@property (nonatomic, strong) NSMutableArray<NSString *> *cachePaths;
@property (nonatomic, strong) UIWindow *window;
@end

@implementation TMReviewPlaybackTests
- (void)setUp { [super setUp]; self.files = [NSMutableArray array]; self.cachePaths = [NSMutableArray array]; }
- (void)tearDown {
    [self.window.rootViewController dismissViewControllerAnimated:NO completion:nil];
    self.window.hidden = YES;
    self.window.rootViewController = nil;
    self.window = nil;
    for (NSURL *url in self.files) [[NSFileManager defaultManager] removeItemAtURL:url error:nil];
    for (NSString *path in self.cachePaths) [[NSFileManager defaultManager] removeItemAtPath:path error:nil];
    [super tearDown];
}
- (void)waitUntil:(BOOL (^)(void))condition {
    XCTNSPredicateExpectation *expectation = [[XCTNSPredicateExpectation alloc] initWithPredicate:
        [NSPredicate predicateWithBlock:^BOOL(id object, NSDictionary *bindings) { return condition(); }] object:nil];
    XCTAssertEqual([XCTWaiter waitForExpectations:@[expectation] timeout:5], XCTWaiterResultCompleted);
}
- (void)drain {
    XCTestExpectation *done = [self expectationWithDescription:@"main callbacks drained"];
    dispatch_async(dispatch_get_main_queue(), ^{ [done fulfill]; });
    [self waitForExpectations:@[done] timeout:2];
}
- (void)waitPastTimeout {
    XCTestExpectation *done = [self expectationWithDescription:@"cancelled deadline passed"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 150 * NSEC_PER_MSEC), dispatch_get_main_queue(), ^{ [done fulfill]; });
    [self waitForExpectations:@[done] timeout:2];
}
- (TMReviewTracks *)tracks {
    TMReviewTracks *tracks = [TMReviewTracks new];
    tracks.busyIndicator = [TMReviewBusy new];
    [tracks loadViewIfNeeded];
    DPTag *tag = [DPTag new];
    tag.tagId = 44;
    TMReviewLocation *location = [TMReviewLocation new];
    location.testCacheKey = [@"pr44-" stringByAppendingString:NSUUID.UUID.UUIDString];
    location.uri = [NSURL fileURLWithPath:[[NSTemporaryDirectory() stringByAppendingPathComponent:location.testCacheKey] stringByAppendingPathExtension:@"wav"]];
    location.type = @"wav";
    tag.tenorTrackUri = location;
    tracks.tag = tag;
    [self.files addObject:location.uri];
    [self.cachePaths addObject:[DPFileCache pathForKey:location.cacheKey]];
    tracks.view.frame = CGRectMake(0, 0, 393, 800);
    [tracks.view layoutIfNeeded];
    return tracks;
}
- (UITableView *)table:(TMReviewTracks *)tracks { return [tracks valueForKey:@"partsTable"]; }
- (void)select:(TMReviewTracks *)tracks {
    UITableView *table = [self table:tracks];
    [table.delegate tableView:table didSelectRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]];
}
- (void)failToEnd:(AVPlayerItem *)item {
    [NSNotificationCenter.defaultCenter postNotificationName:AVPlayerItemFailedToPlayToEndTimeNotification object:item];
}
- (void)assertSettled:(TMReviewBusy *)busy player:(TMReviewPlayer *)player {
    XCTAssertEqual(busy.busyCount, 0);
    XCTAssertEqual(busy.settlements, 1);
    XCTAssertNil(player.currentItem);
    XCTAssertEqual(player.detaches, 1);
    XCTAssertGreaterThanOrEqual(player.pauses, 1);
}
- (void)testReadySettlesRowOnceAndKeepsObserving {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMReviewItem *item = (id)tracks.lastItem;
    TMReviewBusy *busy = (id)tracks.busyIndicator;
    UITableViewCell *cell = [[self table:tracks] cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]];
    TMBarberPoleLoadingView *spinner = (id)cell.accessoryView;
    XCTAssertNotNil(spinner);
    XCTAssertTrue(spinner.isAnimating);
    XCTAssertEqual(busy.busyCount, 1);
    [item sendStatus:AVPlayerItemStatusReadyToPlay];
    [self drain];
    XCTAssertEqual(tracks.presentations, 1);
    XCTAssertEqual(busy.settlements, 1);
    XCTAssertFalse(spinner.isAnimating);
    XCTAssertNil(cell.accessoryView);
    XCTAssertNil(cell.accessibilityLabel);
    XCTAssertEqual(item.statusRemovals, 0);
    XCTAssertNotNil(tracks.lastPlayer.currentItem);
    [item sendStatus:AVPlayerItemStatusReadyToPlay];
    [self drain];
    XCTAssertEqual(tracks.presentations, 1);
    [tracks cancelPlayback];
    [self assertSettled:busy player:(id)tracks.lastPlayer];
    XCTAssertGreaterThan(item.statusRemovals, 0);
}
- (void)testFailureBeforeReadyOffersOneRetryAndDetaches {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMReviewItem *item = (id)tracks.lastItem;
    [item sendStatus:AVPlayerItemStatusFailed];
    [self failToEnd:item];
    [self drain];
    XCTAssertEqual(tracks.errors, 1);
    XCTAssertNotNil(tracks.retry);
    XCTAssertEqual(tracks.presentations, 0);
    [self assertSettled:(id)tracks.busyIndicator player:(id)tracks.lastPlayer];
    XCTAssertGreaterThan(item.statusRemovals, 0);
}
- (void)testPendingControllerReleaseDetachesAndDoesNotRetainUntilDeadline {
    __weak TMReviewTracks *weakTracks;
    __weak TMTrackPlaybackSession *weakSession;
    TMReviewPlayer *player;
    TMReviewBusy *busy;
    TMReviewItem *item;
    @autoreleasepool {
        TMReviewTracks *tracks = [self tracks];
        tracks.testTimeout = 0.05;
        [self select:tracks];
        weakTracks = tracks;
        weakSession = tracks.playbackSession;
        player = (id)tracks.lastPlayer;
        item = (id)tracks.lastItem;
        busy = (id)tracks.busyIndicator;
    }
    XCTAssertNil(weakTracks);
    XCTAssertNil(weakSession);
    [self assertSettled:busy player:player];
    XCTAssertGreaterThan(item.statusRemovals, 0);
    [item sendStatus:AVPlayerItemStatusReadyToPlay];
    [self failToEnd:item];
    [self waitPastTimeout];
    [self assertSettled:busy player:player];
}
- (void)testPendingPlayerAndItemReleaseWithoutOwnershipCycle {
    __weak AVPlayer *weakPlayer;
    __weak AVPlayerItem *weakItem;
    @autoreleasepool {
        TMReviewTracks *tracks = [self tracks];
        [self select:tracks];
        weakPlayer = tracks.lastPlayer;
        weakItem = tracks.lastItem;
    }
    [self drain];
    XCTAssertNil(weakPlayer);
    XCTAssertNil(weakItem);
}
- (void)testLeavingPendingPageCancelsReadyFailureAndTimeout {
    TMReviewTracks *tracks = [self tracks];
    tracks.testTimeout = 0.05;
    [self select:tracks];
    TMReviewItem *item = (id)tracks.lastItem;
    [tracks viewWillDisappear:NO];
    XCTAssertNil(tracks.playbackSession);
    [item sendStatus:AVPlayerItemStatusReadyToPlay];
    [self failToEnd:item];
    [self waitPastTimeout];
    XCTAssertEqual(tracks.presentations, 0);
    XCTAssertEqual(tracks.errors, 0);
    [self assertSettled:(id)tracks.busyIndicator player:(id)tracks.lastPlayer];
}
- (void)testTagChangeCancelsQueuedReadyCallback {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    [(TMReviewItem *)tracks.lastItem sendStatus:AVPlayerItemStatusReadyToPlay];
    tracks.tag = [DPTag new];
    [self drain];
    XCTAssertNil(tracks.playbackSession);
    XCTAssertEqual(tracks.presentations, 0);
    XCTAssertEqual(tracks.errors, 0);
    [self assertSettled:(id)tracks.busyIndicator player:(id)tracks.lastPlayer];
}
- (void)testTimeoutOffersRetryAndLateReadyCannotPresent {
    TMReviewTracks *tracks = [self tracks];
    tracks.testTimeout = 0.01;
    [self select:tracks];
    [self waitUntil:^BOOL { return tracks.errors == 1; }];
    [(TMReviewItem *)tracks.lastItem sendStatus:AVPlayerItemStatusReadyToPlay];
    [self drain];
    XCTAssertNotNil(tracks.retry);
    XCTAssertEqual(tracks.presentations, 0);
    [self assertSettled:(id)tracks.busyIndicator player:(id)tracks.lastPlayer];
}
- (void)testReadyCancelsPreparationTimeout {
    TMReviewTracks *tracks = [self tracks];
    tracks.testTimeout = 0.05;
    [self select:tracks];
    [(TMReviewItem *)tracks.lastItem sendStatus:AVPlayerItemStatusReadyToPlay];
    [self drain];
    [self waitPastTimeout];
    XCTAssertEqual(tracks.errors, 0);
    XCTAssertNotNil(tracks.lastPlayer.currentItem);
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 1);
    [tracks cancelPlayback];
}
- (void)testLateStatusFailureAndDuplicateNotificationOfferOneRetry {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMReviewItem *item = (id)tracks.lastItem;
    [item sendStatus:AVPlayerItemStatusReadyToPlay];
    [self drain];
    [item sendStatus:AVPlayerItemStatusFailed];
    [self failToEnd:item];
    [self drain];
    XCTAssertEqual(tracks.errors, 1);
    XCTAssertNotNil(tracks.retry);
    [self assertSettled:(id)tracks.busyIndicator player:(id)tracks.lastPlayer];
}
- (void)testFailureToEndWhileStatusReadyOffersRetryOnMain {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMReviewItem *item = (id)tracks.lastItem;
    [item sendStatus:AVPlayerItemStatusReadyToPlay];
    [self drain];
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_DEFAULT, 0), ^{ [self failToEnd:item]; });
    [self waitUntil:^BOOL { return tracks.errors == 1; }];
    [item sendStatus:AVPlayerItemStatusFailed];
    [self drain];
    XCTAssertEqual(tracks.errors, 1);
    XCTAssertNotNil(tracks.retry);
    [self assertSettled:(id)tracks.busyIndicator player:(id)tracks.lastPlayer];
}
- (void)testBackgroundStatusDeliveryUsesMainThread {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMReviewItem *item = (id)tracks.lastItem;
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_DEFAULT, 0), ^{ [item sendStatus:AVPlayerItemStatusFailed]; });
    [self waitUntil:^BOOL { return tracks.errors == 1; }];
    [self assertSettled:(id)tracks.busyIndicator player:(id)tracks.lastPlayer];
}
- (void)testNativePlayerDisappearanceCancelsSessionButPresentingItDoesNot {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMReviewItem *item = (id)tracks.lastItem;
    [item sendStatus:AVPlayerItemStatusReadyToPlay];
    [self drain];
    TMTrackPlaybackSession *session = tracks.playbackSession;
    [tracks viewWillDisappear:NO];
    XCTAssertEqual(tracks.playbackSession, session);
    XCTAssertFalse(tracks.playbackHasLeft);
    [tracks.capturedPlayer viewDidDisappear:NO];
    XCTAssertNil(tracks.playbackSession);
    [self failToEnd:item];
    [self drain];
    XCTAssertEqual(tracks.errors, 0);
    [self assertSettled:(id)tracks.busyIndicator player:(id)tracks.lastPlayer];
    XCTAssertGreaterThan(item.statusRemovals, 0);
}
- (void)testCancelledPresentationCompletionCannotStartPlayer {
    TMReviewTracks *tracks = [self tracks];
    tracks.delayPresentationCompletion = YES;
    [self select:tracks];
    [(TMReviewItem *)tracks.lastItem sendStatus:AVPlayerItemStatusReadyToPlay];
    [self drain];
    XCTAssertNotNil(tracks.presentationCompletion);
    [tracks cancelPlayback];
    tracks.presentationCompletion();
    XCTAssertEqual(((TMReviewPlayer *)tracks.lastPlayer).plays, 0);
}
- (void)testRetryCreatesFreshSessionAndIgnoresOldEvents {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMReviewItem *oldItem = (id)tracks.lastItem;
    [(TMReviewItem *)tracks.lastItem sendStatus:AVPlayerItemStatusFailed];
    [self drain];
    TMTrackPlaybackSession *oldSession = tracks.playbackSession;
    void (^oldRetry)(void) = tracks.retry;
    oldRetry();
    XCTAssertNotEqual(tracks.playbackSession, oldSession);
    XCTAssertNotEqual(tracks.lastItem, oldItem);
    TMTrackPlaybackSession *newSession = tracks.playbackSession;
    oldRetry();
    XCTAssertEqual(tracks.playbackSession, newSession);
    [oldItem sendStatus:AVPlayerItemStatusReadyToPlay];
    [self failToEnd:oldItem];
    [self drain];
    XCTAssertEqual(tracks.presentations, 0);
    XCTAssertEqual(tracks.errors, 1);
    [(TMReviewItem *)tracks.lastItem sendStatus:AVPlayerItemStatusReadyToPlay];
    [self drain];
    XCTAssertEqual(tracks.presentations, 1);
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 2);
    [tracks cancelPlayback];
}
- (void)testRetryFindsSelectedTrackAfterRowsReorder {
    TMReviewTracks *tracks = [self tracks];
    NSMutableArray *items = (id)tracks.tag.tracks;
    DPTrack *selected = items[0];
    TMReviewLocation *other = [TMReviewLocation new];
    other.testCacheKey = [@"pr44-other-" stringByAppendingString:NSUUID.UUID.UUIDString];
    other.uri = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:other.testCacheKey]];
    [items addObject:[DPTrack trackWithTitle:@"Other" source:other]];
    [[self table:tracks] reloadData];
    [tracks.view layoutIfNeeded];
    [self select:tracks];
    [(TMReviewItem *)tracks.lastItem sendStatus:AVPlayerItemStatusFailed];
    [self drain];
    [items exchangeObjectAtIndex:0 withObjectAtIndex:1];
    tracks.retry();
    XCTAssertEqualObjects(tracks.requestedURL, selected.source.uri);
    UITableViewCell *selectedCell = [[self table:tracks] cellForRowAtIndexPath:[NSIndexPath indexPathForRow:1 inSection:0]];
    // Track identity resolves the new row instead of replaying the old index.
    XCTAssertEqual(tracks.busyIndicator.busyCount, 1);
    if (selectedCell) XCTAssertTrue([selectedCell.accessibilityLabel hasPrefix:@"Tenor"]);
    [tracks cancelPlayback];
}
- (void)testSelectingAnotherTrackInvalidatesOldRetryAndEvents {
    TMReviewTracks *tracks = [self tracks];
    DPTrack *first = tracks.tag.tracks[0];
    TMReviewLocation *other = [TMReviewLocation new];
    other.testCacheKey = [@"pr44-next-" stringByAppendingString:NSUUID.UUID.UUIDString];
    other.uri = [NSURL fileURLWithPath:[NSTemporaryDirectory() stringByAppendingPathComponent:other.testCacheKey]];
    [(NSMutableArray *)tracks.tag.tracks addObject:[DPTrack trackWithTitle:@"Other" source:other]];
    [self select:tracks];
    TMReviewItem *oldItem = (id)tracks.lastItem;
    [oldItem sendStatus:AVPlayerItemStatusFailed];
    [self drain];
    void (^oldRetry)(void) = tracks.retry;
    UITableView *table = [self table:tracks];
    [table.delegate tableView:table didSelectRowAtIndexPath:[NSIndexPath indexPathForRow:1 inSection:0]];
    AVPlayerItem *newItem = tracks.lastItem;
    oldRetry();
    [oldItem sendStatus:AVPlayerItemStatusReadyToPlay];
    [self failToEnd:oldItem];
    [self drain];
    XCTAssertEqual(tracks.lastItem, newItem);
    XCTAssertEqualObjects(tracks.requestedURL, other.uri);
    XCTAssertNotEqualObjects(tracks.requestedURL, first.source.uri);
    XCTAssertEqual(tracks.errors, 1);
    XCTAssertEqual(tracks.presentations, 0);
    [tracks cancelPlayback];
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 2);
}
- (void)testRetryCannotPlayRemovedTrack {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    [(TMReviewItem *)tracks.lastItem sendStatus:AVPlayerItemStatusFailed];
    [self drain];
    AVPlayerItem *oldItem = tracks.lastItem;
    [(NSMutableArray *)tracks.tag.tracks removeAllObjects];
    tracks.retry();
    XCTAssertEqual(tracks.lastItem, oldItem);
    XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
    [tracks cancelPlayback];
}
- (void)testRetryAfterTagChangeOrLeavingIsNoOp {
    for (NSNumber *changeTag in @[@YES, @NO]) {
        TMReviewTracks *tracks = [self tracks];
        [self select:tracks];
        [(TMReviewItem *)tracks.lastItem sendStatus:AVPlayerItemStatusFailed];
        [self drain];
        AVPlayerItem *oldItem = tracks.lastItem;
        if (changeTag.boolValue) tracks.tag = [DPTag new];
        else [tracks viewWillDisappear:NO];
        tracks.retry();
        XCTAssertEqual(tracks.lastItem, oldItem);
        XCTAssertNil(tracks.playbackSession);
        XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
    }
}
- (void)testStreamFirstAndExistingCacheKeyPathArePreserved {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    DPTrack *track = tracks.tag.tracks[0];
    XCTAssertEqualObjects(tracks.requestedURL, track.source.uri);
    [tracks cancelPlayback];
    [DPFileCache writeData:[@"local fixture" dataUsingEncoding:NSUTF8StringEncoding] forKey:track.source.cacheKey];
    [self select:tracks];
    XCTAssertEqualObjects(tracks.requestedURL.path, [DPFileCache pathForKey:track.source.cacheKey]);
    XCTAssertTrue(tracks.requestedURL.isFileURL);
    [tracks cancelPlayback];
}

// Silent local PCM exercises real AVFoundation loading, UIKit presentation,
// native dismissal, and the production recovery alert without internet access.
- (void)writeWAV:(NSURL *)url {
    uint32_t samples = 8000 * 30, dataSize = samples * 2, riffSize = dataSize + 36;
    uint32_t formatSize = 16, rate = 8000, byteRate = 16000;
    uint16_t format = 1, channels = 1, block = 2, bits = 16;
    NSMutableData *data = [NSMutableData data];
    [data appendBytes:"RIFF" length:4]; [data appendBytes:&riffSize length:4]; [data appendBytes:"WAVEfmt " length:8];
    [data appendBytes:&formatSize length:4]; [data appendBytes:&format length:2]; [data appendBytes:&channels length:2];
    [data appendBytes:&rate length:4]; [data appendBytes:&byteRate length:4]; [data appendBytes:&block length:2];
    [data appendBytes:&bits length:2]; [data appendBytes:"data" length:4]; [data appendBytes:&dataSize length:4];
    [data increaseLengthBy:dataSize];
    XCTAssertTrue([data writeToURL:url atomically:YES]);
}
- (TMReviewTracks *)nativeTracks {
    TMReviewTracks *tracks = [self tracks];
    tracks.realPlayback = YES;
    tracks.nativePresentation = YES;
    [self writeWAV:((DPTrack *)tracks.tag.tracks[0]).source.uri];
    UIWindowScene *scene = (id)UIApplication.sharedApplication.connectedScenes.anyObject;
    self.window = scene ? [[UIWindow alloc] initWithWindowScene:scene] : [[UIWindow alloc] initWithFrame:UIScreen.mainScreen.bounds];
    self.window.rootViewController = tracks;
    [self.window makeKeyAndVisible];
    [self select:tracks];
    [self waitUntil:^BOOL { return tracks.presentedViewController == tracks.capturedPlayer && tracks.capturedPlayer != nil; }];
    XCTAssertEqual(tracks.lastItem.status, AVPlayerItemStatusReadyToPlay, @"%@ at %@", tracks.lastItem.error, tracks.requestedURL);
    XCTAssertNotNil(tracks.playbackSession);
    XCTAssertFalse(tracks.playbackHasLeft);
    return tracks;
}
- (void)testRealLocalPlaybackDismissalTearsDownAndIgnoresLateFailure {
    TMReviewTracks *tracks = [self nativeTracks];
    AVPlayer *player = tracks.lastPlayer;
    AVPlayerItem *item = tracks.lastItem;
    __weak TMTrackPlaybackSession *weakSession = tracks.playbackSession;
    [tracks.capturedPlayer dismissViewControllerAnimated:NO completion:nil];
    [self waitUntil:^BOOL { return tracks.playbackSession == nil; }];
    XCTAssertNil(weakSession);
    XCTAssertNil(player.currentItem);
    XCTAssertNil(tracks.capturedPlayer.player);
    [self failToEnd:item];
    [self drain];
    XCTAssertEqual(tracks.errors, 0);
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 1);
}
- (void)testRealLocalPlaybackLateFailureDismissesThenShowsRetryAndCanPlayAgain {
    TMReviewTracks *tracks = [self nativeTracks];
    AVPlayer *oldPlayer = tracks.lastPlayer;
    AVPlayerItem *oldItem = tracks.lastItem;
    [self failToEnd:oldItem];
    [self failToEnd:oldItem];
    [self waitUntil:^BOOL { return [tracks.presentedViewController isKindOfClass:UIAlertController.class]; }];
    XCTAssertEqual(tracks.errors, 1);
    XCTAssertNil(oldPlayer.currentItem);
    UIAlertController *alert = (id)tracks.presentedViewController;
    XCTAssertEqualObjects([alert.actions valueForKey:@"title"], (@[@"Retry", @"Cancel"]));
    void (^retry)(void) = tracks.retry;
    [alert dismissViewControllerAnimated:NO completion:retry];
    [self waitUntil:^BOOL { return tracks.presentations == 2; }];
    XCTAssertNotEqual(tracks.lastPlayer, oldPlayer);
    [self failToEnd:oldItem];
    [self drain];
    XCTAssertEqual(tracks.errors, 1);
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 2);
    [tracks cancelPlayback];
}
@end
