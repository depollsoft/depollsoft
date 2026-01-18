//
//  DPSettingsModelTests.m
//  pitchperfectTests
//

#import <XCTest/XCTest.h>
#import "pitchperfect-Swift.h"

@interface DPSettingsModelTests : XCTestCase
@end

@implementation DPSettingsModelTests

- (void)setUp {
    [super setUp];
    // Reset defaults used by settings
    NSUserDefaults *d = [NSUserDefaults standardUserDefaults];
    [d removeObjectForKey:@"depollsoft.pitchperfect.WakeLock"];
    [d removeObjectForKey:@"depollsoft.pitchperfect.ToggleNote"];
}

- (void)testWakeLockAndToggleNotesPostNotifications {
    XCTestExpectation *exp1 = [self expectationWithDescription:@"settingsChanged1"];
    id token1 = [[NSNotificationCenter defaultCenter] addObserverForName:[DPSettingsModel settingsChangedNotificationName]
                                                                  object:nil
                                                                   queue:[NSOperationQueue mainQueue]
                                                              usingBlock:^(NSNotification * _Nonnull note) {
        [exp1 fulfill];
    }];
    DPSettingsModel *m = [DPSettingsModel sharedInstance];
    BOOL current = m.wakeLock;
    m.wakeLock = !current;
    [self waitForExpectations:@[exp1] timeout:2.0];
    [[NSNotificationCenter defaultCenter] removeObserver:token1];

    XCTestExpectation *exp2 = [self expectationWithDescription:@"settingsChanged2"];
    id token2 = [[NSNotificationCenter defaultCenter] addObserverForName:[DPSettingsModel settingsChangedNotificationName]
                                                                  object:nil
                                                                   queue:[NSOperationQueue mainQueue]
                                                              usingBlock:^(NSNotification * _Nonnull note) {
        [exp2 fulfill];
    }];
    BOOL cur2 = m.toggleNotes;
    m.toggleNotes = !cur2;
    [self waitForExpectations:@[exp2] timeout:2.0];
    [[NSNotificationCenter defaultCenter] removeObserver:token2];
}

@end

