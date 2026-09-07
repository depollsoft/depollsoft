#import "TMDensityFixture.h"
#if DEBUG
#import "DPAppDelegate.h"
#import "DPBarbershop.h"
#import "tagmaster-Swift.h"
#import <objc/runtime.h>

static NSArray<DPTag *> *fixtureTags;
static BOOL installed;
static NSString *const listsKey = @"depollsoft.pitchperfect.lists";

@interface DPTag (DensityFixture)
@end
@implementation DPTag (DensityFixture)
+ (DPTag *)densityCached:(int)tagId {
    for (DPTag *tag in fixtureTags) { if (tag.tagId == tagId) { return tag; } }
    return nil;
}
+ (DPTag *)densityLoad:(int)tagId refresh:(BOOL)refresh {
    return [self loadFromCache:tagId];
}
+ (DPTagQueryResult *)densityQuery:(NSString *)query numberOfResults:(int)numberOfResults start:(int)start parts:(NSNumber *)parts learningTracks:(NSNumber *)learningTracks sheetMusic:(NSNumber *)sheetMusic collection:(enum DPTagCollection)collection sortBy:(enum DPTagSortOptions)sortBy minimumRating:(NSNumber *)minimumRating minimumDownloads:(NSNumber *)minimumDownloads cache:(BOOL)cache fieldList:(NSString *)fieldList {
    DPTagQueryResult *result = [DPTagQueryResult new];
    result.start = start;
    result.available = (int)fixtureTags.count;
    NSUInteger first = MIN(MAX(start, 0), fixtureTags.count);
    NSUInteger count = MIN(MAX(numberOfResults, 0), fixtureTags.count - first);
    result.tags = [fixtureTags subarrayWithRange:NSMakeRange(first, count)];
    result.count = (int)count;
    return result;
}
@end

@implementation TMDensityFixture
+ (NSString *)backupPath {
    return [NSTemporaryDirectory() stringByAppendingPathComponent:@"tagmaster-density-defaults.plist"];
}
+ (void)exchangeMethods {
    NSArray *pairs = @[
        @[@"loadFromCache:", @"densityCached:"],
        @[@"loadTagById:refresh:", @"densityLoad:refresh:"],
        @[@"query:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:minimumRating:minimumDownloads:cache:fieldList:",
          @"densityQuery:numberOfResults:start:parts:learningTracks:sheetMusic:collection:sortBy:minimumRating:minimumDownloads:cache:fieldList:"]
    ];
    for (NSArray *pair in pairs) {
        method_exchangeImplementations(class_getClassMethod(DPTag.class, NSSelectorFromString(pair[0])),
                                       class_getClassMethod(DPTag.class, NSSelectorFromString(pair[1])));
    }
}
+ (void)activateWithCount:(NSInteger)count {
    [self restore];
    NSUserDefaults *defaults = NSUserDefaults.standardUserDefaults;
    NSMutableDictionary *backup = [NSMutableDictionary dictionary];
    for (NSString *key in @[listsKey, @"browse.collection"]) {
        id value = [defaults objectForKey:key];
        if (value) { backup[key] = value; }
    }
    if (![backup writeToFile:[self backupPath] atomically:YES]) {
        [NSException raise:NSInternalInconsistencyException format:@"Cannot back up density fixture preferences"];
    }
    NSMutableArray *tags = [NSMutableArray array];
    NSMutableArray *ids = [NSMutableArray array];
    NSArray *names = @[@"Evening harmony", @"Sing it again", @"Until we meet", @"A little blue", @"Homeward bound", @"Stay awhile", @"One more song", @"When the last note carries all the way across the room and brings us home"];
    for (NSInteger i = 0; i < count; i++) {
        DPTag *tag = [DPTag new];
        tag.tagId = (int)(900001 + i);
        tag.title = [NSString stringWithFormat:@"%@ %02ld", names[i % names.count], (long)i + 1];
        tag.alternativeTitle = i % 7 == 6 ? @"A song for old friends" : (i % 3 == 0 ? tag.title : @"");
        tag.rating = 3.17;
        tag.downloadCount = 1234;
        tag.posted = [NSDate dateWithTimeIntervalSince1970:1700000000];
        tag.writtenKey = @"C";
        tag.parts = 4;
        tag.lyrics = @"Synthetic local density fixture. No account or remote catalog data.";
        if (i % 4 != 3) { tag.sheetMusicUri = [DPRemoteLocation new]; }
        if (i % 4 < 2) { tag.allPartsTrackUri = [DPRemoteLocation new]; }
        [tags addObject:tag];
        [ids addObject:@(tag.tagId)];
    }
    fixtureTags = tags;
    [self exchangeMethods];
    installed = YES;
    [DPAppDelegate setFavorites:ids doSave:NO];
    [DPAppDelegate setTeachable:ids doSave:NO];
    [defaults setInteger:0 forKey:@"browse.collection"];
}
+ (NSArray<DPTag *> *)tags { return fixtureTags; }
+ (void)restore {
    if (installed) { [self exchangeMethods]; installed = NO; }
    fixtureTags = nil;
    NSDictionary *backup = [NSDictionary dictionaryWithContentsOfFile:[self backupPath]];
    if (backup) {
        NSUserDefaults *defaults = NSUserDefaults.standardUserDefaults;
        for (NSString *key in @[listsKey, @"browse.collection"]) {
            if (backup[key]) { [defaults setObject:backup[key] forKey:key]; }
            else { [defaults removeObjectForKey:key]; }
        }
        [defaults synchronize];
        [NSFileManager.defaultManager removeItemAtPath:[self backupPath] error:nil];
    }
}
+ (BOOL)launchIfRequested:(DPAppDelegate *)app {
    NSArray *args = NSProcessInfo.processInfo.arguments;
    BOOL cleanup = [args containsObject:@"--density-cleanup"];
    NSUInteger index = [args indexOfObject:@"--density-fixture"];
    if (!cleanup && index == NSNotFound) {
        // A terminated UI test may not have reached its cleanup launch.
        // Restore before normal startup can initialize account synchronization.
        [self restore];
        return NO;
    }
    if (cleanup) { [self restore]; }
    else { [self activateWithCount:index + 1 < args.count ? [args[index + 1] integerValue] : 100]; }
    app.window = [[UIWindow alloc] initWithFrame:UIScreen.mainScreen.bounds];
    [TMTheme applyTo:app.window];
    if (cleanup) {
        UIViewController *done = [UIViewController new];
        done.view.accessibilityIdentifier = @"density.restored";
        app.window.rootViewController = done;
    } else {
        TMRootController *root = [TMRootController make];
        app.window.rootViewController = root;
        app.rootController = root;
    }
    [app.window makeKeyAndVisible];
    return YES;
}
@end
#endif
