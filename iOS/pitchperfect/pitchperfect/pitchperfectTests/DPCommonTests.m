//
//  DPCommonTests.m
//  pitchperfectTests
//

#import <XCTest/XCTest.h>
#import "pitchperfect-Swift.h"

@interface DummyTarget : NSObject
@end
@implementation DummyTarget
- (void)action {}
@end

@interface DPCommonTests : XCTestCase
@end

@implementation DPCommonTests

- (void)testGetSettingsButton {
    DummyTarget *t = [DummyTarget new];
    UIBarButtonItem *item = [DPCommon getSettingsButtonWithTarget:t selector:@selector(action)];
    XCTAssertNotNil(item);
}

@end

