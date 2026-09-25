#import <XCTest/XCTest.h>
#import <UIKit/UIKit.h>
#import <objc/runtime.h>

#import "DPAppDelegate.h"

#import "DPTag.h"
#import "DPTagQueryResult.h"
#import "DPRemoteLocation.h"
#import "DPTrack.h"
#import "DPVideo.h"
#import "DPFileCache.h"
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
