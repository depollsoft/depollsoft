#import <XCTest/XCTest.h>
#import <objc/runtime.h>
#import <limits.h>
#import "DPAppDelegate.h"
#import "DPFileCache.h"
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

// The track playback review tests moved to TagTracksPlaybackTests.swift with the
// SwiftUI Tracks page.
