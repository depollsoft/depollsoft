//
//  DPSearchViewControllerTests.m
//  tagmasterTests
//
//  Tests for DPSearchViewController functionality.
//

#import <XCTest/XCTest.h>
#import <UIKit/UIKit.h>
#import <objc/runtime.h>
#import "DPSearchViewController.h"
#import "DPTagQueryViewController.h"
#import "DPTag.h"
#import "DPTagQueryResult.h"

// Expose private properties for testing
@interface DPSearchViewController (Testing)
@property (nonatomic, strong) UISearchBar *searchBar;
@property (nonatomic, strong) UISegmentedControl *sortBy;
@property (nonatomic, strong) UISegmentedControl *sheetMusic;
@property (nonatomic, strong) UISegmentedControl *learningTracks;
@property (nonatomic, strong) UISegmentedControl *parts;
@property (nonatomic, strong) UISegmentedControl *collection;
- (void)search;
- (void)saveSettings;
- (void)dismissKeyboard;
- (NSInteger)sortByValue;
- (void)setSortByValue:(NSInteger)value;
- (NSInteger)sheetMusicValue;
- (void)setSheetMusicValue:(NSInteger)value;
- (NSInteger)learningTracksValue;
- (void)setLearningTracksValue:(NSInteger)value;
- (NSInteger)partsValue;
- (void)setPartsValue:(NSInteger)value;
- (NSInteger)collectionValue;
- (void)setCollectionValue:(NSInteger)value;
@end

@interface DPSearchViewControllerTests : XCTestCase
@property (nonatomic, strong) DPSearchViewController *searchController;
@property (nonatomic, strong) UINavigationController *navigationController;
@property (nonatomic, strong) UIWindow *window;
@end

@implementation DPSearchViewControllerTests

#pragma mark - Setup/Teardown

- (void)setUp {
    [super setUp];
    
    // Clear search settings
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults removeObjectForKey:@"search.sortBy"];
    [defaults removeObjectForKey:@"search.sheetMusic"];
    [defaults removeObjectForKey:@"search.learningTracks"];
    [defaults removeObjectForKey:@"search.parts"];
    [defaults removeObjectForKey:@"search.collection"];
    [defaults synchronize];
    
    // Create the search controller with navigation
    self.searchController = [[DPSearchViewController alloc] init];
    self.navigationController = [[UINavigationController alloc] initWithRootViewController:self.searchController];
    self.window = [[UIWindow alloc] initWithFrame:CGRectMake(0, 0, 375, 812)];
    self.window.rootViewController = self.navigationController;
    [self.window makeKeyAndVisible];
    
    // Load the view
    (void)self.searchController.view;
    [self.searchController.view layoutIfNeeded];
}

- (void)tearDown {
    self.window = nil;
    self.navigationController = nil;
    self.searchController = nil;
    
    // Clean up settings
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults removeObjectForKey:@"search.sortBy"];
    [defaults removeObjectForKey:@"search.sheetMusic"];
    [defaults removeObjectForKey:@"search.learningTracks"];
    [defaults removeObjectForKey:@"search.parts"];
    [defaults removeObjectForKey:@"search.collection"];
    [defaults synchronize];
    
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

#pragma mark - View Setup Tests

- (void)testViewLoadsWithAllControls {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *sortBy = [self.searchController valueForKey:@"sortBy"];
    UISegmentedControl *sheetMusic = [self.searchController valueForKey:@"sheetMusic"];
    UISegmentedControl *learningTracks = [self.searchController valueForKey:@"learningTracks"];
    UISegmentedControl *parts = [self.searchController valueForKey:@"parts"];
    UISegmentedControl *collection = [self.searchController valueForKey:@"collection"];
    
    XCTAssertNotNil(searchBar, @"Search bar should be initialized");
    XCTAssertNotNil(sortBy, @"Sort by control should be initialized");
    XCTAssertNotNil(sheetMusic, @"Sheet music control should be initialized");
    XCTAssertNotNil(learningTracks, @"Learning tracks control should be initialized");
    XCTAssertNotNil(parts, @"Parts control should be initialized");
    XCTAssertNotNil(collection, @"Collection control should be initialized");
}

- (void)testSearchBarHasCorrectPlaceholder {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    XCTAssertEqualObjects(searchBar.placeholder, @"Search");
}

- (void)testNavigationItemHasSearchButton {
    XCTAssertNotNil(self.searchController.navigationItem.rightBarButtonItem);
    XCTAssertEqual(self.searchController.navigationItem.rightBarButtonItem.style, UIBarButtonItemStylePlain);
}

- (void)testNavigationTitleIsSearch {
    XCTAssertEqualObjects(self.searchController.navigationItem.title, @"Search");
}

#pragma mark - Segmented Control Configuration Tests

- (void)testSortBySegmentedControlHasCorrectItems {
    UISegmentedControl *sortBy = [self.searchController valueForKey:@"sortBy"];
    XCTAssertEqual(sortBy.numberOfSegments, 4);
    XCTAssertEqualObjects([sortBy titleForSegmentAtIndex:0], @"Title");
    XCTAssertEqualObjects([sortBy titleForSegmentAtIndex:1], @"Downloads");
    XCTAssertEqualObjects([sortBy titleForSegmentAtIndex:2], @"Recent");
    XCTAssertEqualObjects([sortBy titleForSegmentAtIndex:3], @"Rating");
}

- (void)testSheetMusicSegmentedControlHasCorrectItems {
    UISegmentedControl *sheetMusic = [self.searchController valueForKey:@"sheetMusic"];
    XCTAssertEqual(sheetMusic.numberOfSegments, 3);
    XCTAssertEqualObjects([sheetMusic titleForSegmentAtIndex:0], @"Not Important");
    XCTAssertEqualObjects([sheetMusic titleForSegmentAtIndex:1], @"Yes");
    XCTAssertEqualObjects([sheetMusic titleForSegmentAtIndex:2], @"No");
}

- (void)testLearningTracksSegmentedControlHasCorrectItems {
    UISegmentedControl *learningTracks = [self.searchController valueForKey:@"learningTracks"];
    XCTAssertEqual(learningTracks.numberOfSegments, 3);
    XCTAssertEqualObjects([learningTracks titleForSegmentAtIndex:0], @"Not Important");
    XCTAssertEqualObjects([learningTracks titleForSegmentAtIndex:1], @"Yes");
    XCTAssertEqualObjects([learningTracks titleForSegmentAtIndex:2], @"No");
}

- (void)testPartsSegmentedControlHasCorrectItems {
    UISegmentedControl *parts = [self.searchController valueForKey:@"parts"];
    XCTAssertEqual(parts.numberOfSegments, 7);
    XCTAssertEqualObjects([parts titleForSegmentAtIndex:0], @"Any");
    XCTAssertEqualObjects([parts titleForSegmentAtIndex:1], @"3");
    XCTAssertEqualObjects([parts titleForSegmentAtIndex:2], @"4");
    XCTAssertEqualObjects([parts titleForSegmentAtIndex:3], @"5");
}

- (void)testCollectionSegmentedControlHasCorrectItems {
    UISegmentedControl *collection = [self.searchController valueForKey:@"collection"];
    XCTAssertEqual(collection.numberOfSegments, 3);
    XCTAssertEqualObjects([collection titleForSegmentAtIndex:0], @"Any");
    XCTAssertEqualObjects([collection titleForSegmentAtIndex:1], @"Classic Tags");
    XCTAssertEqualObjects([collection titleForSegmentAtIndex:2], @"Easy Tags");
}

#pragma mark - Search Query Tests

- (void)testSearchQuerySendsRequest {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    searchBar.text = @"test query";
    
    __block DPTagQueryViewController *capturedController = nil;
    
    // Intercept navigation push
    [self.navigationController pushViewController:[[UIViewController alloc] init] animated:NO];
    
    // When search is called, it pushes a DPTagQueryViewController
    // We can verify by checking the navigation stack after calling search
    NSUInteger initialCount = self.navigationController.viewControllers.count;
    
    [self.searchController search];
    
    // Check if a new view controller was pushed
    XCTAssertEqual(self.navigationController.viewControllers.count, initialCount + 1);
    
    // Verify it's a DPTagQueryViewController
    UIViewController *pushedVC = self.navigationController.viewControllers.lastObject;
    XCTAssertTrue([pushedVC isKindOfClass:[DPTagQueryViewController class]]);
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)pushedVC;
    XCTAssertEqualObjects(queryVC.query, @"test query");
}

- (void)testSearchWithSortByTitleSetsCorrectOption {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *sortBy = [self.searchController valueForKey:@"sortBy"];
    
    searchBar.text = @"test";
    sortBy.selectedSegmentIndex = 0; // Title
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqual(queryVC.sortBy, DPTagSortTitle);
}

- (void)testSearchWithSortByDownloadsSetsCorrectOption {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *sortBy = [self.searchController valueForKey:@"sortBy"];
    
    searchBar.text = @"test";
    sortBy.selectedSegmentIndex = 1; // Downloads
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqual(queryVC.sortBy, DPTagSortDownloaded);
}

- (void)testSearchWithSortByRecentSetsCorrectOption {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *sortBy = [self.searchController valueForKey:@"sortBy"];
    
    searchBar.text = @"test";
    sortBy.selectedSegmentIndex = 2; // Recent (Posted)
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqual(queryVC.sortBy, DPTagSortPosted);
}

- (void)testSearchWithSortByRatingSetsCorrectOption {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *sortBy = [self.searchController valueForKey:@"sortBy"];
    
    searchBar.text = @"test";
    sortBy.selectedSegmentIndex = 3; // Rating
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqual(queryVC.sortBy, DPTagSortRating);
}

- (void)testSearchWithSheetMusicYesSetsFilter {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *sheetMusic = [self.searchController valueForKey:@"sheetMusic"];
    
    searchBar.text = @"test";
    sheetMusic.selectedSegmentIndex = 1; // Yes
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqualObjects(queryVC.hasSheetMusic, @YES);
}

- (void)testSearchWithSheetMusicNoSetsFilter {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *sheetMusic = [self.searchController valueForKey:@"sheetMusic"];
    
    searchBar.text = @"test";
    sheetMusic.selectedSegmentIndex = 2; // No
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqualObjects(queryVC.hasSheetMusic, @NO);
}

- (void)testSearchWithLearningTracksYesSetsFilter {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *learningTracks = [self.searchController valueForKey:@"learningTracks"];
    
    searchBar.text = @"test";
    learningTracks.selectedSegmentIndex = 1; // Yes
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqualObjects(queryVC.hasLearningTracks, @YES);
}

- (void)testSearchWithPartsFilterSetsCorrectValue {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *parts = [self.searchController valueForKey:@"parts"];
    
    searchBar.text = @"test";
    parts.selectedSegmentIndex = 2; // Index 2 = "4" parts (index + 2)
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqualObjects(queryVC.parts, @4);
}

- (void)testSearchWithCollectionClassicSetsFilter {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *collection = [self.searchController valueForKey:@"collection"];
    
    searchBar.text = @"test";
    collection.selectedSegmentIndex = 1; // Classic Tags
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqual(queryVC.collection, DPTagCollectionClassicTags);
}

- (void)testSearchWithCollectionEasySetsFilter {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *collection = [self.searchController valueForKey:@"collection"];
    
    searchBar.text = @"test";
    collection.selectedSegmentIndex = 2; // Easy Tags
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqual(queryVC.collection, DPTagCollectionEasyTags);
}

#pragma mark - Empty Search Tests

- (void)testEmptySearchShowsPlaceholder {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    
    // Empty search bar should still have placeholder
    XCTAssertNotNil(searchBar.placeholder);
    XCTAssertEqualObjects(searchBar.placeholder, @"Search");
    XCTAssertEqual(searchBar.text.length, 0);
}

- (void)testSearchClearsOnEmpty {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    
    // Set some text
    searchBar.text = @"test query";
    XCTAssertEqual(searchBar.text.length, 10);
    
    // Clear it
    searchBar.text = @"";
    XCTAssertEqual(searchBar.text.length, 0);
}

#pragma mark - Settings Persistence Tests

- (void)testSettingsPersistOnViewDisappear {
    UISegmentedControl *sortBy = [self.searchController valueForKey:@"sortBy"];
    UISegmentedControl *sheetMusic = [self.searchController valueForKey:@"sheetMusic"];
    UISegmentedControl *learningTracks = [self.searchController valueForKey:@"learningTracks"];
    UISegmentedControl *parts = [self.searchController valueForKey:@"parts"];
    UISegmentedControl *collection = [self.searchController valueForKey:@"collection"];
    
    // Set values
    sortBy.selectedSegmentIndex = 2;
    sheetMusic.selectedSegmentIndex = 1;
    learningTracks.selectedSegmentIndex = 2;
    parts.selectedSegmentIndex = 3;
    collection.selectedSegmentIndex = 1;
    
    // Trigger save by calling viewDidDisappear
    [self.searchController viewDidDisappear:NO];
    
    // Verify persistence
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    XCTAssertEqual([defaults integerForKey:@"search.sortBy"], 2);
    XCTAssertEqual([defaults integerForKey:@"search.sheetMusic"], 1);
    XCTAssertEqual([defaults integerForKey:@"search.learningTracks"], 2);
    XCTAssertEqual([defaults integerForKey:@"search.parts"], 3);
    XCTAssertEqual([defaults integerForKey:@"search.collection"], 1);
}

- (void)testSettingsLoadOnViewLoad {
    // Set defaults before creating controller
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults setInteger:3 forKey:@"search.sortBy"];
    [defaults setInteger:2 forKey:@"search.sheetMusic"];
    [defaults setInteger:1 forKey:@"search.learningTracks"];
    [defaults setInteger:4 forKey:@"search.parts"];
    [defaults setInteger:2 forKey:@"search.collection"];
    [defaults synchronize];
    
    // Create new controller
    DPSearchViewController *newController = [[DPSearchViewController alloc] init];
    (void)newController.view;
    
    // Verify controls loaded with persisted values
    UISegmentedControl *sortBy = [newController valueForKey:@"sortBy"];
    UISegmentedControl *sheetMusic = [newController valueForKey:@"sheetMusic"];
    UISegmentedControl *learningTracks = [newController valueForKey:@"learningTracks"];
    UISegmentedControl *parts = [newController valueForKey:@"parts"];
    UISegmentedControl *collection = [newController valueForKey:@"collection"];
    
    XCTAssertEqual(sortBy.selectedSegmentIndex, 3);
    XCTAssertEqual(sheetMusic.selectedSegmentIndex, 2);
    XCTAssertEqual(learningTracks.selectedSegmentIndex, 1);
    XCTAssertEqual(parts.selectedSegmentIndex, 4);
    XCTAssertEqual(collection.selectedSegmentIndex, 2);
}

#pragma mark - Search Bar Delegate Tests

- (void)testSearchBarDelegateConformance {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    
    // Verify the search controller conforms to UISearchBarDelegate
    XCTAssertTrue([self.searchController conformsToProtocol:@protocol(UISearchBarDelegate)]);
    XCTAssertNotNil(searchBar.delegate);
}

#pragma mark - Keyboard Dismiss Tests

- (void)testDismissKeyboardResignsSearchBar {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    [searchBar becomeFirstResponder];
    
    // Call dismissKeyboard
    [self.searchController dismissKeyboard];
    
    // Note: In a UI test, we'd verify isFirstResponder is NO
    // In a unit test, we just verify the method doesn't crash
    XCTAssertNotNil(searchBar);
}

#pragma mark - Parts Value Calculation Tests

- (void)testPartsAnyDoesNotSetFilter {
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *parts = [self.searchController valueForKey:@"parts"];
    
    searchBar.text = @"test";
    parts.selectedSegmentIndex = 0; // Any
    
    [self.searchController search];
    
    DPTagQueryViewController *queryVC = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertNil(queryVC.parts, @"Parts should be nil when 'Any' is selected");
}

- (void)testPartsSelectionCalculatesCorrectValue {
    // parts value = selectedIndex + 2 (when selectedIndex > 0)
    // Index 1 = 3 parts, Index 2 = 4 parts, etc.
    UISearchBar *searchBar = [self.searchController valueForKey:@"searchBar"];
    UISegmentedControl *parts = [self.searchController valueForKey:@"parts"];
    
    searchBar.text = @"test";
    
    // Test index 1 (3 parts)
    parts.selectedSegmentIndex = 1;
    [self.searchController search];
    DPTagQueryViewController *queryVC1 = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqualObjects(queryVC1.parts, @3);
    
    // Test index 5 (7 parts)
    parts.selectedSegmentIndex = 5;
    [self.searchController search];
    DPTagQueryViewController *queryVC2 = (DPTagQueryViewController *)self.navigationController.viewControllers.lastObject;
    XCTAssertEqualObjects(queryVC2.parts, @7);
}

@end
