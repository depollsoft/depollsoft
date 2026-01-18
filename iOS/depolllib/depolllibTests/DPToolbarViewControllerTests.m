//
//  DPToolbarViewControllerTests.m
//  depolllibTests
//
//  Created by Codex on 2025-09-03.
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import <UIKit/UIKit.h>
#import "DPToolbarViewController.h"
#import "DPTabBarController.h"

@interface DPToolbarViewControllerTests : XCTestCase
@end

@implementation DPToolbarViewControllerTests

- (void)testToolbarIsCreatedAndPositionedAtTop {
    DPToolbarViewController *vc = [[DPToolbarViewController alloc] init];
    // Trigger view loading
    (void)vc.view;
    STAssertNotNil(vc.toolbar, @"Toolbar should be created on viewDidLoad");
    STAssertEquals(vc.toolbar.superview, vc.view, @"Toolbar should be added to the view hierarchy");

    // Exercise delegate method and memory warning
    id<UIBarPositioningDelegate> delegate = (id)vc;
    UIBarPosition pos = [delegate positionForBar:vc.toolbar];
    STAssertEquals(pos, UIBarPositionTopAttached, @"Toolbar should be at the top attached position");
    [vc didReceiveMemoryWarning];
}

@end

@interface DPTabBarControllerIntegratedTests : XCTestCase @end
@implementation DPTabBarControllerIntegratedTests

- (void)testSelectingTabsSwapsChildViews {
    DPTabBarController *vc = [[DPTabBarController alloc] init];
    (void)vc.view; // force load

    UIViewController *a = [[UIViewController alloc] init];
    a.view.backgroundColor = [UIColor redColor];
    a.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"A" image:nil tag:0];

    UIViewController *b = [[UIViewController alloc] init];
    b.view.backgroundColor = [UIColor blueColor];
    b.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"B" image:nil tag:1];

    vc.viewControllers = @[a, b];

    STAssertTrue([a.view isDescendantOfView:vc.view], @"A's view should be in hierarchy");
    STAssertFalse([b.view isDescendantOfView:vc.view], @"B's view should not be in hierarchy yet");

    id<UITabBarDelegate> delegate = (id<UITabBarDelegate>)vc;
    [delegate tabBar:vc.tabBar didSelectItem:b.tabBarItem];

    STAssertTrue([b.view isDescendantOfView:vc.view], @"B's view should be in hierarchy after selection");
}

@end
