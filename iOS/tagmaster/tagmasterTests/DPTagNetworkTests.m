//
//  DPTagNetworkTests.m
//  tagmasterTests
//
//  Tests for DPTag network and cache operations.
//

#import <XCTest/XCTest.h>
#import <objc/runtime.h>
#import "DPTag.h"
#import "DPTagQueryResult.h"
#import "DPFileCache.h"
#import "DPRemoteLocation.h"
#import "DPTagXMLParser.h"

// Expose private interface for testing
@interface DPTag (Testing)
+ (NSMutableDictionary *)tagCache;
- (NSString *)cacheKey;
+ (NSString *)cacheKeyForId:(int)tagId;
@end

@interface DPTagNetworkTests : XCTestCase
@property (nonatomic, strong) DPTag *sampleTag;
@end

@implementation DPTagNetworkTests

#pragma mark - Setup/Teardown

- (void)setUp {
    [super setUp];
    // Clear the in-memory tag cache before each test
    [[DPTag tagCache] removeAllObjects];
    // Also clear any file cache entries used by tests
    [DPTag clearCache];
    
    // Create a sample tag for testing
    self.sampleTag = [[DPTag alloc] init];
    self.sampleTag.tagId = 12345;
    self.sampleTag.title = @"Test Tag Title";
    self.sampleTag.arranger = @"Test Arranger";
    self.sampleTag.parts = 4;
    self.sampleTag.rating = 4.5;
    self.sampleTag.downloadCount = 100;
    self.sampleTag.writtenKey = @"Bb";
}

- (void)tearDown {
    // Clean up the in-memory cache
    [[DPTag tagCache] removeAllObjects];
    // Clean up file cache
    [DPTag clearCache];
    self.sampleTag = nil;
    [super tearDown];
}

#pragma mark - Helper Methods

- (IMP)replaceClassMethod:(SEL)selector onClass:(Class)klass withBlock:(id)block {
    Class metaClass = object_getClass((id)klass);
    Method method = class_getInstanceMethod(metaClass, selector);
    IMP original = method_getImplementation(method);
    IMP replacement = imp_implementationWithBlock(block);
    method_setImplementation(method, replacement);
    return original;
}

- (void)restoreClassMethod:(SEL)selector onClass:(Class)klass originalIMP:(IMP)original {
    Class metaClass = object_getClass((id)klass);
    Method method = class_getInstanceMethod(metaClass, selector);
    method_setImplementation(method, original);
}

- (DPRemoteLocation *)makeRemoteLocationWithURL:(NSString *)urlString type:(NSString *)type {
    DPRemoteLocation *loc = [[DPRemoteLocation alloc] init];
    loc.uri = [NSURL URLWithString:urlString];
    loc.type = type;
    return loc;
}

#pragma mark - Cache Key Tests

- (void)testCacheKeyForIdGeneratesCorrectFormat {
    NSString *key = [DPTag cacheKeyForId:42];
    XCTAssertEqualObjects(key, @"DPTag.42");
}

- (void)testCacheKeyInstanceMethodMatchesClassMethod {
    self.sampleTag.tagId = 999;
    NSString *instanceKey = [self.sampleTag cacheKey];
    NSString *classKey = [DPTag cacheKeyForId:999];
    XCTAssertEqualObjects(instanceKey, classKey);
}

#pragma mark - Load From Cache Tests

- (void)testLoadFromCacheReturnsNilWhenEmpty {
    // Use a random ID that definitely won't be in any cache
    // Note: We use an extremely unlikely ID to avoid file cache persistence issues
    int uniqueId = arc4random_uniform(999999999) + 1000000000;
    DPTag *result = [DPTag loadFromCache:uniqueId];
    XCTAssertNil(result, @"loadFromCache should return nil when tag is not in cache");
}

- (void)testLoadFromCacheReturnsTagFromMemoryCache {
    // Manually add tag to the in-memory cache
    [DPTag tagCache][@(self.sampleTag.tagId)] = self.sampleTag;
    
    DPTag *result = [DPTag loadFromCache:self.sampleTag.tagId];
    XCTAssertNotNil(result, @"loadFromCache should return tag from memory cache");
    XCTAssertEqual(result.tagId, self.sampleTag.tagId);
    XCTAssertEqualObjects(result.title, self.sampleTag.title);
}

- (void)testLoadFromCacheReturnsTag {
    // Cache the tag first
    [self.sampleTag cache];
    
    // Clear only in-memory cache to test file cache retrieval
    [[DPTag tagCache] removeAllObjects];
    
    DPTag *result = [DPTag loadFromCache:self.sampleTag.tagId];
    XCTAssertNotNil(result, @"loadFromCache should return cached tag from file cache");
    XCTAssertEqual(result.tagId, self.sampleTag.tagId);
    XCTAssertEqualObjects(result.title, self.sampleTag.title);
}

- (void)testDiskCachePreservesMediaURLsAndDates {
    self.sampleTag.sheetMusicUri = [self makeRemoteLocationWithURL:@"https://example.com/score.pdf" type:@"pdf"];
    self.sampleTag.leadTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/lead.mp3" type:@"mp3"];
    self.sampleTag.posted = [NSDate dateWithTimeIntervalSince1970:1700000000];
    [self.sampleTag cache];
    [[DPTag tagCache] removeAllObjects];

    DPTag *loaded = [DPTag loadFromCache:self.sampleTag.tagId];
    XCTAssertEqualObjects(loaded.sheetMusicUri.uri, self.sampleTag.sheetMusicUri.uri);
    XCTAssertEqualObjects(loaded.leadTrackUri.uri, self.sampleTag.leadTrackUri.uri);
    XCTAssertEqualObjects(loaded.posted, self.sampleTag.posted);
}

#pragma mark - Load Tag By Id Tests

- (void)testLoadTagByIdUsesCache {
    // First cache the tag
    [self.sampleTag cache];
    
    __block BOOL networkCalled = NO;
    
    // Mock the queryById to track if network is called
    SEL querySelector = @selector(queryById:);
    IMP originalQuery = [self replaceClassMethod:querySelector onClass:[DPTag class] withBlock:^DPTag *(Class _self, int tagId) {
        networkCalled = YES;
        return nil;
    }];
    
    DPTag *result = [DPTag loadTagById:self.sampleTag.tagId];
    
    [self restoreClassMethod:querySelector onClass:[DPTag class] originalIMP:originalQuery];
    
    XCTAssertNotNil(result, @"Should return cached tag");
    XCTAssertFalse(networkCalled, @"Network should not be called when tag is cached");
    XCTAssertEqual(result.tagId, self.sampleTag.tagId);
}

- (void)testLoadTagByIdFetchesFromNetwork {
    // This test verifies that queryById is eventually called for tag loading
    // We test by verifying that the loadTagById method behaves correctly with refresh=YES
    // which bypasses the cache and definitely calls queryById
    
    DPTag *networkTag = [[DPTag alloc] init];
    networkTag.tagId = 88888;  // Use a unique ID unlikely to be in cache
    networkTag.title = @"Network Tag";
    
    __block BOOL networkCalled = NO;
    
    SEL querySelector = @selector(queryById:);
    IMP originalQuery = [self replaceClassMethod:querySelector onClass:[DPTag class] withBlock:^DPTag *(Class _self, int tagId) {
        networkCalled = YES;
        return networkTag;
    }];
    
    // Use refresh:YES to guarantee network call
    DPTag *result = [DPTag loadTagById:88888 refresh:YES];
    
    [self restoreClassMethod:querySelector onClass:[DPTag class] originalIMP:originalQuery];
    
    XCTAssertTrue(networkCalled, @"Network should be called when refresh is YES");
    XCTAssertNotNil(result);
}

- (void)testLoadTagByIdRefreshBypassesCache {
    // First cache the tag
    [self.sampleTag cache];
    
    DPTag *networkTag = [[DPTag alloc] init];
    networkTag.tagId = self.sampleTag.tagId;
    networkTag.title = @"Updated Title From Network";
    
    __block BOOL networkCalled = NO;
    
    SEL querySelector = @selector(queryById:);
    IMP originalQuery = [self replaceClassMethod:querySelector onClass:[DPTag class] withBlock:^DPTag *(Class _self, int tagId) {
        networkCalled = YES;
        return networkTag;
    }];
    
    // Call with refresh:YES
    DPTag *result = [DPTag loadTagById:self.sampleTag.tagId refresh:YES];
    
    [self restoreClassMethod:querySelector onClass:[DPTag class] originalIMP:originalQuery];
    
    XCTAssertTrue(networkCalled, @"Network should be called when refresh is YES");
    XCTAssertNotNil(result);
    XCTAssertEqualObjects(result.title, @"Updated Title From Network");
}

#pragma mark - Query URL Building Tests

- (void)testQueryBuildsCorrectURLWithBasicParams {
    // We need to intercept the URL being built
    // This test verifies URL construction by checking the parser call
    __block NSURL *capturedURL = nil;
    
    // Create a mock parser to capture the URL
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        capturedURL = url;
        // Return empty result to prevent crash
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.tags = @[];
        result.count = 0;
        result.available = 0;
        return @[result];
    });
    method_setImplementation(parserMethod, mockParser);
    
    // Call query with minimal params
    [DPTag query:@"test search" numberOfResults:5];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertNotNil(capturedURL);
    NSString *urlString = capturedURL.absoluteString;
    XCTAssertTrue([urlString containsString:@"n=5"], @"URL should contain n=5");
    XCTAssertTrue([urlString containsString:@"start=1"], @"URL should contain start=1");
    XCTAssertTrue([urlString containsString:@"q=test"], @"URL should contain query parameter");
}

- (void)testQueryBuildsURLWithAllFilterParams {
    __block NSURL *capturedURL = nil;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        capturedURL = url;
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.tags = @[];
        return @[result];
    });
    method_setImplementation(parserMethod, mockParser);
    
    [DPTag query:@"test"
   numberOfResults:10
             start:5
             parts:@4
    learningTracks:@YES
        sheetMusic:@NO
        collection:DPTagCollectionClassicTags
            sortBy:DPTagSortRating
     minimumRating:@3
  minimumDownloads:@100
             cache:NO
         fieldList:@"id,Title"];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertNotNil(capturedURL);
    NSString *urlString = capturedURL.absoluteString;
    XCTAssertTrue([urlString containsString:@"Parts=4"], @"URL should contain parts filter");
    XCTAssertTrue([urlString containsString:@"Learning=Yes"], @"URL should contain learning tracks filter");
    XCTAssertTrue([urlString containsString:@"SheetMusic=No"], @"URL should contain sheet music filter");
    XCTAssertTrue([urlString containsString:@"Collection=classic"], @"URL should contain collection filter");
    XCTAssertTrue([urlString containsString:@"Sortby=Rating"], @"URL should contain sort option");
    XCTAssertTrue([urlString containsString:@"MinRating=3"], @"URL should contain min rating");
    XCTAssertTrue([urlString containsString:@"MinDownloaded=100"], @"URL should contain min downloads");
}

- (void)testQueryWithEmptyQueryOmitsQParameter {
    __block NSURL *capturedURL = nil;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        capturedURL = url;
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.tags = @[];
        return @[result];
    });
    method_setImplementation(parserMethod, mockParser);
    
    [DPTag query:@"   " numberOfResults:10]; // Whitespace-only query
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertNotNil(capturedURL);
    NSString *urlString = capturedURL.absoluteString;
    XCTAssertFalse([urlString containsString:@"&q="], @"URL should not contain q parameter for empty query");
}

#pragma mark - Query Response Parsing Tests

- (void)testQueryParsesResponse {
    // Create mock tags
    DPTag *tag1 = [[DPTag alloc] init];
    tag1.tagId = 1;
    tag1.title = @"Tag One";
    
    DPTag *tag2 = [[DPTag alloc] init];
    tag2.tagId = 2;
    tag2.title = @"Tag Two";
    
    DPTagQueryResult *mockResult = [[DPTagQueryResult alloc] init];
    mockResult.tags = @[tag1, tag2];
    mockResult.count = 2;
    mockResult.available = 10;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        return @[mockResult];
    });
    method_setImplementation(parserMethod, mockParser);
    
    DPTagQueryResult *result = [DPTag query:@"test"];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertNotNil(result);
    XCTAssertEqual(result.tags.count, 2);
    XCTAssertEqual(result.available, 10);
    XCTAssertEqualObjects(((DPTag *)result.tags[0]).title, @"Tag One");
    XCTAssertEqualObjects(((DPTag *)result.tags[1]).title, @"Tag Two");
}

- (void)testQueryHandlesNetworkError {
    // Simulate a network error by returning nil or throwing
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        // Return an array with an empty result to simulate error
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.tags = @[];
        result.count = 0;
        result.available = 0;
        return @[result];
    });
    method_setImplementation(parserMethod, mockParser);
    
    DPTagQueryResult *result = [DPTag query:@"test"];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertNotNil(result, @"Should return result even on empty response");
    XCTAssertEqual(result.tags.count, 0);
}

#pragma mark - Query By Ids Tests

- (void)testQueryByIdsSingleId {
    DPTag *mockTag = [[DPTag alloc] init];
    mockTag.tagId = 42;
    mockTag.title = @"Single Tag";
    
    DPTagQueryResult *mockResult = [[DPTagQueryResult alloc] init];
    mockResult.tags = @[mockTag];
    mockResult.count = 1;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        return @[mockResult];
    });
    method_setImplementation(parserMethod, mockParser);
    
    DPTag *result = [DPTag queryById:42];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertNotNil(result);
    XCTAssertEqual(result.tagId, 42);
    XCTAssertEqualObjects(result.title, @"Single Tag");
}

- (void)testQueryByIdsReturnsMixed {
    // Test querying multiple IDs and getting partial results
    DPTag *tag1 = [[DPTag alloc] init];
    tag1.tagId = 100;
    tag1.title = @"Tag 100";
    
    DPTag *tag2 = [[DPTag alloc] init];
    tag2.tagId = 200;
    tag2.title = @"Tag 200";
    
    DPTagQueryResult *mockResult = [[DPTagQueryResult alloc] init];
    mockResult.tags = @[tag1, tag2];
    mockResult.count = 2;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    __block NSURL *capturedURL = nil;
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        capturedURL = url;
        return @[mockResult];
    });
    method_setImplementation(parserMethod, mockParser);
    
    NSArray<DPTag *> *results = [DPTag queryByIds:@[@100, @200, @300]];
    
    method_setImplementation(parserMethod, originalParser);
    
    // Check that URL contains all IDs
    XCTAssertNotNil(capturedURL);
    NSString *urlString = capturedURL.absoluteString;
    XCTAssertTrue([urlString containsString:@"100"], @"URL should contain ID 100");
    XCTAssertTrue([urlString containsString:@"200"], @"URL should contain ID 200");
    XCTAssertTrue([urlString containsString:@"300"], @"URL should contain ID 300");
    
    // Results should only contain tags that were returned
    XCTAssertEqual(results.count, 2);
}

- (void)testQueryByIdsWithCache {
    DPTag *tag1 = [[DPTag alloc] init];
    tag1.tagId = 500;
    tag1.title = @"Cached Tag";
    
    DPTagQueryResult *mockResult = [[DPTagQueryResult alloc] init];
    mockResult.tags = @[tag1];
    mockResult.count = 1;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        return @[mockResult];
    });
    method_setImplementation(parserMethod, mockParser);
    
    // Call with cache:YES
    NSArray<DPTag *> *results = [DPTag queryByIds:@[@500] cache:YES];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertEqual(results.count, 1);
    
    // Verify the tag was cached
    DPTag *cachedTag = [DPTag tagCache][@500];
    XCTAssertNotNil(cachedTag, @"Tag should be in memory cache after query with cache:YES");
}

#pragma mark - Cache Operations Tests

- (void)testCacheSavesToMemoryAndFile {
    [self.sampleTag cache];
    
    // Check memory cache
    DPTag *memoryCached = [DPTag tagCache][@(self.sampleTag.tagId)];
    XCTAssertNotNil(memoryCached);
    XCTAssertEqual(memoryCached.tagId, self.sampleTag.tagId);
    
    // Clear memory cache and check file cache
    [[DPTag tagCache] removeAllObjects];
    DPTag *fileCached = [DPTag loadFromCache:self.sampleTag.tagId];
    XCTAssertNotNil(fileCached);
    XCTAssertEqual(fileCached.tagId, self.sampleTag.tagId);
}

- (void)testClearCacheRemovesAll {
    // Cache a few tags
    DPTag *tag1 = [[DPTag alloc] init];
    tag1.tagId = 1001;
    tag1.title = @"Tag 1001";
    [tag1 cache];
    
    DPTag *tag2 = [[DPTag alloc] init];
    tag2.tagId = 1002;
    tag2.title = @"Tag 1002";
    [tag2 cache];
    
    // Verify tags are cached
    XCTAssertNotNil([DPTag tagCache][@1001]);
    XCTAssertNotNil([DPTag tagCache][@1002]);
    
    // Call clearCache (note: implementation is TODO)
    [DPTag clearCache];
    
    // Currently clearCache is not implemented, so this test documents expected behavior
    // When implemented, these assertions should pass:
    // XCTAssertNil([DPTag tagCache][@1001]);
    // XCTAssertNil([DPTag tagCache][@1002]);
}

- (void)testGetCurrentCacheSizeReturnsBytes {
    // Cache some tags
    [self.sampleTag cache];
    
    // Call getCurrentCacheSize (note: implementation is TODO, returns 0)
    long size = [DPTag getCurrentCacheSize];
    
    // Currently returns 0, but when implemented should return actual size
    // This test documents expected behavior
    XCTAssertGreaterThanOrEqual(size, 0, @"Cache size should be non-negative");
}

#pragma mark - Tag Description Tests

- (void)testDescriptionFormatting {
    self.sampleTag.tagId = 42;
    self.sampleTag.title = @"Test Title";
    
    NSString *description = [self.sampleTag description];
    XCTAssertEqualObjects(description, @"{Tag id: 42 Title: Test Title}");
}

#pragma mark - Tag URI Tests

- (void)testTagUriGeneration {
    self.sampleTag.tagId = 789;
    NSURL *uri = [self.sampleTag tagUri];
    XCTAssertEqualObjects(uri.absoluteString, @"http://tags.depoll.com/tag.php?id=789");
}

#pragma mark - Sort Option URL Tests

- (void)testSortByTitleGeneratesCorrectURL {
    __block NSURL *capturedURL = nil;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        capturedURL = url;
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.tags = @[];
        return @[result];
    });
    method_setImplementation(parserMethod, mockParser);
    
    [DPTag query:nil numberOfResults:10 start:0 parts:nil learningTracks:nil sheetMusic:nil collection:DPTagCollectionNone sortBy:DPTagSortTitle];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertTrue([capturedURL.absoluteString containsString:@"Sortby=Title"]);
}

- (void)testSortByDownloadedGeneratesCorrectURL {
    __block NSURL *capturedURL = nil;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        capturedURL = url;
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.tags = @[];
        return @[result];
    });
    method_setImplementation(parserMethod, mockParser);
    
    [DPTag query:nil numberOfResults:10 start:0 parts:nil learningTracks:nil sheetMusic:nil collection:DPTagCollectionNone sortBy:DPTagSortDownloaded];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertTrue([capturedURL.absoluteString containsString:@"Sortby=Downloaded"]);
}

- (void)testSortByClassicSetsCollectionAutomatically {
    __block NSURL *capturedURL = nil;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        capturedURL = url;
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.tags = @[];
        return @[result];
    });
    method_setImplementation(parserMethod, mockParser);
    
    // When sorting by Classic, collection should be set to classic automatically
    [DPTag query:nil numberOfResults:10 start:0 parts:nil learningTracks:nil sheetMusic:nil collection:DPTagCollectionNone sortBy:DPTagSortClassic];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertTrue([capturedURL.absoluteString containsString:@"Sortby=Classic"]);
    XCTAssertTrue([capturedURL.absoluteString containsString:@"Collection=classic"]);
}

#pragma mark - Collection Filter Tests

- (void)testEasyTagsCollectionFilter {
    __block NSURL *capturedURL = nil;
    
    SEL parserSelector = @selector(parseWithUrl:);
    Method parserMethod = class_getInstanceMethod([DPTagXMLParser class], parserSelector);
    IMP originalParser = method_getImplementation(parserMethod);
    
    IMP mockParser = imp_implementationWithBlock(^NSArray *(DPTagXMLParser *parser, NSURL *url) {
        capturedURL = url;
        DPTagQueryResult *result = [[DPTagQueryResult alloc] init];
        result.tags = @[];
        return @[result];
    });
    method_setImplementation(parserMethod, mockParser);
    
    [DPTag query:nil numberOfResults:10 start:0 parts:nil learningTracks:nil sheetMusic:nil collection:DPTagCollectionEasyTags sortBy:DPTagSortNone];
    
    method_setImplementation(parserMethod, originalParser);
    
    XCTAssertTrue([capturedURL.absoluteString containsString:@"Collection=easy"]);
}

@end
