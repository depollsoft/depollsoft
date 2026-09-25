#import <XCTest/XCTest.h>
#import <objc/runtime.h>
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
// The app hands every opened URL to the sign-in providers first and routes a
// tag link only when none of them claims it (TagMasterApp's onOpenURL); the
// tag-link grammar itself is covered by TMRouterTests.
- (void)testAuthProviderGetsFirstRefusalBeforeTagValidation {
    self.auth.accepts = YES;
    for (NSString *text in @[@"review-auth://callback?code=local", @"tagmaster://tag/0"]) {
        XCTAssertTrue([DPAppDelegate handleAuthURL:[NSURL URLWithString:text]]);
    }
    XCTAssertEqual(self.auth.calls, 2);
    self.auth.accepts = NO;
    XCTAssertFalse([DPAppDelegate handleAuthURL:[NSURL URLWithString:@"tagmaster://tag/12"]]);
}
@end

// The track playback review tests moved to TagTracksPlaybackTests.swift with the
// SwiftUI Tracks page.
