#import <XCTest/XCTest.h>
#import <AVKit/AVKit.h>
#import <objc/runtime.h>
#import <limits.h>
#import "DPAppDelegate.h"
#import "DPTagTracksController.h"
#import "DPFileCache.h"
#import "TMBarberPoleLoadingView.h"
#import "tagmaster-Swift.h"
#import "TMReviewLoader.h"

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
@property (nonatomic, strong) TMTrackLoader *lastLoader;
@property (nonatomic, strong) DPTrack *presentedTrack;
@property (nonatomic, strong) AVAudioPCMBuffer *presentedBuffer;
@property (nonatomic, copy) void (^retry)(void);
@property NSUInteger loads;
@property NSUInteger errors;
@property NSUInteger presentations;
@property NSTimeInterval testTimeout;
@property BOOL realPlayback;
@property BOOL showInlinePlayer;
@end
@implementation TMReviewTracks
- (NSTimeInterval)playbackReadyTimeout { return self.testTimeout > 0 ? self.testTimeout : 30; }
- (TMTrackLoader *)makeTrackLoaderWithUrl:(NSURL *)url cacheKey:(NSString *)cacheKey {
    self.loads++;
    self.lastLoader = self.realPlayback ? [super makeTrackLoaderWithUrl:url cacheKey:cacheKey]
        : TMCreateReviewLoader(url, cacheKey);
    return self.lastLoader;
}
- (void)presentPlayerFor:(DPTrack *)track buffer:(AVAudioPCMBuffer *)buffer {
    NSAssert(NSThread.isMainThread, @"Presentation must be on main");
    self.presentations++;
    self.presentedTrack = track;
    self.presentedBuffer = buffer;
    if (self.showInlinePlayer) [super presentPlayerFor:track buffer:buffer];
}
- (void)tm_showError:(NSString *)message retry:(void (^)(void))retry {
    NSAssert(NSThread.isMainThread, @"Recovery must be on main");
    self.errors++;
    self.retry = retry;
}
@end

@interface TMReviewPlaybackTests : XCTestCase
@property (nonatomic, strong) NSMutableArray<NSURL *> *files;
@property (nonatomic, strong) NSMutableArray<NSString *> *cachePaths;
@end

@implementation TMReviewPlaybackTests
- (void)setUp { [super setUp]; self.files = [NSMutableArray array]; self.cachePaths = [NSMutableArray array]; }
- (void)tearDown {
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
- (AVAudioPCMBuffer *)buffer {
    AVAudioFormat *format = [[AVAudioFormat alloc] initStandardFormatWithSampleRate:44100 channels:2];
    AVAudioPCMBuffer *buffer = [[AVAudioPCMBuffer alloc] initWithPCMFormat:format frameCapacity:4410];
    buffer.frameLength = 4410;
    for (NSUInteger channel = 0; channel < 2; channel++) memset(buffer.floatChannelData[channel], 0, 4410 * sizeof(float));
    return buffer;
}
- (void)assertSettled:(TMReviewBusy *)busy loader:(TMControlledTrackLoader *)loader {
    XCTAssertEqual(busy.busyCount, 0);
    XCTAssertEqual(busy.settlements, 1);
    XCTAssertEqual(loader.cancels, 1);
}
- (void)testReadySettlesRowOnceAndPresentsBuffer {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMControlledTrackLoader *loader = (id)tracks.lastLoader;
    TMReviewBusy *busy = (id)tracks.busyIndicator;
    UITableViewCell *cell = [[self table:tracks] cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]];
    TMBarberPoleLoadingView *spinner = (id)cell.accessoryView;
    XCTAssertNotNil(spinner);
    XCTAssertTrue(spinner.isAnimating);
    XCTAssertEqualObjects(cell.accessibilityLabel, @"Tenor, loading");
    XCTAssertEqual(busy.busyCount, 1);
    AVAudioPCMBuffer *buffer = [self buffer];
    [loader succeedWithBuffer:buffer];
    [self drain];
    XCTAssertEqual(tracks.presentations, 1);
    XCTAssertEqual(tracks.presentedTrack, tracks.tag.tracks[0]);
    XCTAssertEqual(tracks.presentedBuffer, buffer);
    XCTAssertNil(tracks.playbackSession);
    XCTAssertEqual(busy.busyCount, 0);
    XCTAssertEqual(busy.settlements, 1);
    XCTAssertFalse(spinner.isAnimating);
    XCTAssertNil(cell.accessoryView);
    XCTAssertNil(cell.accessibilityLabel);
    [loader succeedWithBuffer:buffer];
    [loader fail];
    [self drain];
    [tracks cancelPlayback];
    XCTAssertEqual(tracks.presentations, 1);
    XCTAssertEqual(tracks.errors, 0);
    XCTAssertEqual(busy.settlements, 1);
    XCTAssertEqual(loader.cancels, 0);
}
- (void)testFailureBeforeReadyOffersOneRetryAndCancelsLoader {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMControlledTrackLoader *loader = (id)tracks.lastLoader;
    TMTrackPlaybackSession *session = tracks.playbackSession;
    [loader fail];
    [loader fail];
    [loader succeedWithBuffer:[self buffer]];
    [self drain];
    XCTAssertEqual(tracks.errors, 1);
    XCTAssertNotNil(tracks.retry);
    XCTAssertEqual(tracks.playbackSession, session);
    XCTAssertEqual(tracks.presentations, 0);
    [tracks cancelPlayback];
    [self assertSettled:(id)tracks.busyIndicator loader:loader];
}
- (void)testPendingControllerReleaseCancelsAndDoesNotRetainUntilDeadline {
    __weak TMReviewTracks *weakTracks;
    __weak TMTrackPlaybackSession *weakSession;
    TMReviewBusy *busy;
    TMControlledTrackLoader *loader;
    @autoreleasepool {
        TMReviewTracks *tracks = [self tracks];
        tracks.testTimeout = 0.05;
        [self select:tracks];
        weakTracks = tracks;
        weakSession = tracks.playbackSession;
        loader = (id)tracks.lastLoader;
        busy = (id)tracks.busyIndicator;
    }
    XCTAssertNil(weakTracks);
    XCTAssertNil(weakSession);
    [self assertSettled:busy loader:loader];
    [loader succeedWithBuffer:[self buffer]];
    [loader fail];
    [self waitPastTimeout];
    [self assertSettled:busy loader:loader];
}
- (void)testPendingLoaderReleasesWithoutOwnershipCycle {
    __weak TMTrackLoader *weakLoader;
    @autoreleasepool {
        TMReviewTracks *tracks = [self tracks];
        [self select:tracks];
        weakLoader = tracks.lastLoader;
    }
    [self drain];
    XCTAssertNil(weakLoader);
}
- (void)testLeavingPendingPageCancelsReadyFailureAndTimeout {
    TMReviewTracks *tracks = [self tracks];
    tracks.testTimeout = 0.05;
    [self select:tracks];
    TMControlledTrackLoader *loader = (id)tracks.lastLoader;
    [tracks viewWillDisappear:NO];
    XCTAssertNil(tracks.playbackSession);
    [loader succeedWithBuffer:[self buffer]];
    [loader fail];
    [self waitPastTimeout];
    XCTAssertEqual(tracks.presentations, 0);
    XCTAssertEqual(tracks.errors, 0);
    [self assertSettled:(id)tracks.busyIndicator loader:(id)tracks.lastLoader];
}
- (void)testTagChangeCancelsQueuedReadyCallback {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    [(TMControlledTrackLoader *)tracks.lastLoader succeedWithBuffer:[self buffer]];
    tracks.tag = [DPTag new];
    [self drain];
    XCTAssertNil(tracks.playbackSession);
    XCTAssertEqual(tracks.presentations, 0);
    XCTAssertEqual(tracks.errors, 0);
    [self assertSettled:(id)tracks.busyIndicator loader:(id)tracks.lastLoader];
}
- (void)testTimeoutOffersRetryAndLateReadyCannotPresent {
    TMReviewTracks *tracks = [self tracks];
    tracks.testTimeout = 0.01;
    [self select:tracks];
    [self waitUntil:^BOOL { return tracks.errors == 1; }];
    [(TMControlledTrackLoader *)tracks.lastLoader succeedWithBuffer:[self buffer]];
    [self drain];
    XCTAssertNotNil(tracks.retry);
    XCTAssertEqual(tracks.presentations, 0);
    [self assertSettled:(id)tracks.busyIndicator loader:(id)tracks.lastLoader];
}
- (void)testReadyCancelsPreparationTimeout {
    TMReviewTracks *tracks = [self tracks];
    tracks.testTimeout = 0.05;
    [self select:tracks];
    [(TMControlledTrackLoader *)tracks.lastLoader succeedWithBuffer:[self buffer]];
    [self drain];
    [self waitPastTimeout];
    XCTAssertEqual(tracks.errors, 0);
    XCTAssertEqual(tracks.presentations, 1);
    XCTAssertNil(tracks.playbackSession);
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 1);
    [tracks cancelPlayback];
}
- (void)testRetryCreatesFreshSessionAndIgnoresOldEvents {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    TMControlledTrackLoader *oldLoader = (id)tracks.lastLoader;
    [(TMControlledTrackLoader *)tracks.lastLoader fail];
    [self drain];
    TMTrackPlaybackSession *oldSession = tracks.playbackSession;
    void (^oldRetry)(void) = tracks.retry;
    oldRetry();
    XCTAssertNotEqual(tracks.playbackSession, oldSession);
    XCTAssertNotEqual(tracks.lastLoader, oldLoader);
    TMTrackPlaybackSession *newSession = tracks.playbackSession;
    oldRetry();
    XCTAssertEqual(tracks.playbackSession, newSession);
    [oldLoader succeedWithBuffer:[self buffer]];
    [oldLoader fail];
    [self drain];
    XCTAssertEqual(tracks.presentations, 0);
    XCTAssertEqual(tracks.errors, 1);
    [(TMControlledTrackLoader *)tracks.lastLoader succeedWithBuffer:[self buffer]];
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
    [(TMControlledTrackLoader *)tracks.lastLoader fail];
    [self drain];
    [items exchangeObjectAtIndex:0 withObjectAtIndex:1];
    tracks.retry();
    XCTAssertEqualObjects(tracks.lastLoader.url, selected.source.uri);
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
    TMControlledTrackLoader *oldLoader = (id)tracks.lastLoader;
    [oldLoader fail];
    [self drain];
    void (^oldRetry)(void) = tracks.retry;
    UITableView *table = [self table:tracks];
    [table.delegate tableView:table didSelectRowAtIndexPath:[NSIndexPath indexPathForRow:1 inSection:0]];
    TMTrackLoader *newLoader = tracks.lastLoader;
    oldRetry();
    [oldLoader succeedWithBuffer:[self buffer]];
    [oldLoader fail];
    [self drain];
    XCTAssertEqual(tracks.lastLoader, newLoader);
    XCTAssertEqualObjects(tracks.lastLoader.url, other.uri);
    XCTAssertNotEqualObjects(tracks.lastLoader.url, first.source.uri);
    XCTAssertEqual(tracks.errors, 1);
    XCTAssertEqual(tracks.presentations, 0);
    [tracks cancelPlayback];
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 2);
}
- (void)testRetryCannotPlayRemovedTrack {
    TMReviewTracks *tracks = [self tracks];
    [self select:tracks];
    [(TMControlledTrackLoader *)tracks.lastLoader fail];
    [self drain];
    TMTrackLoader *oldLoader = tracks.lastLoader;
    [(NSMutableArray *)tracks.tag.tracks removeAllObjects];
    tracks.retry();
    XCTAssertEqual(tracks.lastLoader, oldLoader);
    XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
    [tracks cancelPlayback];
}
- (void)testRetryAfterTagChangeOrLeavingIsNoOp {
    for (NSNumber *changeTag in @[@YES, @NO]) {
        TMReviewTracks *tracks = [self tracks];
        [self select:tracks];
        [(TMControlledTrackLoader *)tracks.lastLoader fail];
        [self drain];
        TMTrackLoader *oldLoader = tracks.lastLoader;
        if (changeTag.boolValue) tracks.tag = [DPTag new];
        else [tracks viewWillDisappear:NO];
        tracks.retry();
        XCTAssertEqual(tracks.lastLoader, oldLoader);
        XCTAssertNil(tracks.playbackSession);
        XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
    }
}
- (void)testRemoteURIAndExistingCacheKeyPathArePreserved {
    TMReviewTracks *tracks = [self tracks];
    DPTrack *track = tracks.tag.tracks[0];
    track.source.uri = [NSURL URLWithString:@"https://example.invalid/learning-track.wav"];
    [self select:tracks];
    XCTAssertEqualObjects(tracks.lastLoader.url, track.source.uri);
    XCTAssertEqualObjects(tracks.lastLoader.cacheKey, track.source.cacheKey);
    [tracks cancelPlayback];
    [DPFileCache writeData:[@"local fixture" dataUsingEncoding:NSUTF8StringEncoding] forKey:track.source.cacheKey];
    [self select:tracks];
    XCTAssertEqualObjects(tracks.lastLoader.url.path, [DPFileCache pathForKey:track.source.cacheKey]);
    XCTAssertTrue(tracks.lastLoader.url.isFileURL);
    XCTAssertEqualObjects(tracks.lastLoader.cacheKey, track.source.cacheKey);
    [tracks cancelPlayback];
}

- (void)testBackgroundCompletionDeliveryUsesMainThread {
    for (NSNumber *succeed in @[@YES, @NO]) {
        TMReviewTracks *tracks = [self tracks];
        [self select:tracks];
        TMControlledTrackLoader *loader = (id)tracks.lastLoader;
        loader.backgroundDelivery = YES;
        if (succeed.boolValue) [loader succeedWithBuffer:[self buffer]];
        else [loader fail];
        [self waitUntil:^BOOL { return tracks.presentations + tracks.errors == 1; }];
        XCTAssertEqual(tracks.presentations, succeed.boolValue ? 1 : 0);
        XCTAssertEqual(tracks.errors, succeed.boolValue ? 0 : 1);
        XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
        XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 1);
    }
}

// Silent local PCM exercises the production loader, decoder, and inline player.
- (void)writeWAV:(NSURL *)url {
    uint32_t samples = 8000 / 4, dataSize = samples * 2, riffSize = dataSize + 36;
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
- (TMReviewTracks *)localTracks {
    TMReviewTracks *tracks = [self tracks];
    tracks.realPlayback = YES;
    tracks.showInlinePlayer = YES;
    [self writeWAV:((DPTrack *)tracks.tag.tracks[0]).source.uri];
    [self select:tracks];
    [self waitUntil:^BOOL { return tracks.presentations == 1; }];
    XCTAssertFalse(tracks.playerView.isHidden);
    XCTAssertTrue(tracks.playerView.player.isLoaded);
    XCTAssertEqual(tracks.playerView.track, tracks.tag.tracks[0]);
    XCTAssertEqualWithAccuracy(tracks.playerView.player.duration, 0.25, 0.001);
    XCTAssertNil(tracks.playbackSession);
    XCTAssertFalse(tracks.playbackHasLeft);
    XCTAssertEqual(tracks.errors, 0);
    XCTAssertEqual(tracks.busyIndicator.busyCount, 0);
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 1);
    return tracks;
}
- (void)assertUnloaded:(TMReviewTracks *)tracks {
    XCTAssertTrue(tracks.playerView.isHidden);
    XCTAssertFalse(tracks.playerView.player.isLoaded);
    XCTAssertFalse(tracks.playerView.player.isPlaying);
    XCTAssertNil(tracks.playerView.track);
    XCTAssertNil(tracks.playbackSession);
}
- (void)testRealLocalPlaybackStopUnloadsAndHidesPlayer {
    TMReviewTracks *tracks = [self localTracks];
    [tracks cancelPlayback];
    XCTAssertTrue(tracks.playerView.player.isLoaded);
    XCTAssertFalse(tracks.playerView.isHidden);
    [tracks stopPlayback];
    [self assertUnloaded:tracks];
}
- (void)testRealLocalPlaybackLeavingUnloadsAndHidesPlayer {
    TMReviewTracks *tracks = [self localTracks];
    [tracks viewWillDisappear:NO];
    XCTAssertTrue(tracks.playbackHasLeft);
    [self assertUnloaded:tracks];
    [self select:tracks];
    XCTAssertEqual(tracks.loads, 1);
}
- (void)testRealLocalPlaybackTagChangeAndRemovalUnloadPlayer {
    for (NSNumber *changeTag in @[@YES, @NO]) {
        TMReviewTracks *tracks = [self localTracks];
        if (changeTag.boolValue) tracks.tag = [DPTag new];
        else [tracks didMoveToParentViewController:nil];
        [self assertUnloaded:tracks];
    }
}
- (void)testSelectingLoadedTrackRestartsWithoutReloading {
    TMReviewTracks *tracks = [self localTracks];
    [tracks.playerView.player pause];
    [tracks.playerView.player seekTo:0.2];
    [self select:tracks];
    XCTAssertEqual(tracks.loads, 1);
    XCTAssertEqual(tracks.presentations, 1);
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 1);
    XCTAssertLessThan(tracks.playerView.player.currentTime, 0.2);
    [tracks stopPlayback];
}
- (void)testRealLocalDecodeFailureOffersRetryAndCanPlayAgain {
    TMReviewTracks *tracks = [self tracks];
    tracks.realPlayback = YES;
    tracks.showInlinePlayer = YES;
    NSURL *url = ((DPTrack *)tracks.tag.tracks[0]).source.uri;
    XCTAssertTrue([[@"not audio" dataUsingEncoding:NSUTF8StringEncoding] writeToURL:url atomically:YES]);
    [self select:tracks];
    [self waitUntil:^BOOL { return tracks.errors == 1; }];
    XCTAssertNotNil(tracks.retry);
    XCTAssertTrue(tracks.playerView.isHidden);
    XCTAssertFalse(tracks.playerView.player.isLoaded);
    TMTrackLoader *oldLoader = tracks.lastLoader;
    [self writeWAV:url];
    tracks.retry();
    [self waitUntil:^BOOL { return tracks.presentations == 1; }];
    XCTAssertNotEqual(tracks.lastLoader, oldLoader);
    XCTAssertTrue(tracks.playerView.player.isLoaded);
    XCTAssertFalse(tracks.playerView.isHidden);
    XCTAssertEqual(tracks.errors, 1);
    XCTAssertEqual(((TMReviewBusy *)tracks.busyIndicator).settlements, 2);
    [tracks stopPlayback];
}
@end
