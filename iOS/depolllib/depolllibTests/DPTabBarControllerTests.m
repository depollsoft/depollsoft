
#import <XCTest/XCTest.h>
#import "DPTabBarController.h"

@interface DPTabBarControllerTests : XCTestCase
@end

@implementation DPTabBarControllerTests

- (void)testLifecycleAndSelection {
    DPTabBarController *tabController = [[DPTabBarController alloc] init];
    XCTAssertNotNil(tabController.tabBar);
    XCTAssertNotNil(tabController.rootView);
    
    UIViewController *vc1 = [[UIViewController alloc] init];
    vc1.title = @"VC1";
    vc1.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Item1" image:nil tag:0];
    
    UIViewController *vc2 = [[UIViewController alloc] init];
    vc2.title = @"VC2";
    vc2.tabBarItem = [[UITabBarItem alloc] initWithTitle:@"Item2" image:nil tag:1];
    
    [tabController setViewControllers:@[vc1, vc2]];
    
    XCTAssertEqual(tabController.viewControllers.count, 2);
    XCTAssertEqual(tabController.tabBar.items.count, 2);
    XCTAssertEqual(tabController.tabBar.selectedItem, vc1.tabBarItem);
    XCTAssertEqual(tabController.childViewControllers.count, 2);
    
    // Verify VC1 view is added
    [tabController.view layoutIfNeeded]; // Trigger viewDidLoad
    XCTAssertTrue([vc1.view isDescendantOfView:tabController.rootView]);
    XCTAssertFalse([vc2.view isDescendantOfView:tabController.rootView]);
    
    // Switch to VC2
    [tabController tabBar:tabController.tabBar didSelectItem:vc2.tabBarItem];
    XCTAssertTrue([vc2.view isDescendantOfView:tabController.rootView]);
    XCTAssertFalse([vc1.view isDescendantOfView:tabController.rootView]);
}

@end
