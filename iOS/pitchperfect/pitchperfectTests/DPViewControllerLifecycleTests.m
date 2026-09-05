#import <XCTest/XCTest.h>
#import "DPPitchPipeViewController.h"
#import "DPKeysViewController.h"
#import "DPSongListViewController.h"
#import "DPSettingsViewController.h"
#import "DPNotesViewController.h"
#import "DPLoginViewController.h"
#import "DPSongEditorViewController.h"
#import "DPPitchedSong.h"
#import "DPKey.h"
#import <UIKit/UIKit.h>
@import Firebase;

@interface DPPitchPipeViewController (Testing)
- (void)refreshButtons;
- (void)stopNotes;
@end

@interface DPSongListViewController (Testing)
- (void)edit;
- (void)sort;
- (void)doneEditing;
- (void)songsChanged;
@end

@interface DPSettingsViewController (Testing)
- (void)complete;
@end

@interface DPSongEditorViewController (Testing)
- (void)complete;
- (void)cancel;
- (void)setTitleText:(NSString *)titleText;
- (void)selectKey:(DPKey *)key;
- (DPKey *)selectedKey;
- (BOOL)isMinorKeySelected;
- (BOOL)isTitleErrorVisible;
@end

@interface DPLoginViewController (Testing)
- (void)skip;
@end

@interface GADBannerView : UIView
@property (nonatomic, copy) NSString *adUnitID;
- (void)loadRequest:(id)request;
@end

@interface DPViewControllerLifecycleTests : XCTestCase
@property (nonatomic, strong) UIWindow *window;
@end

@implementation DPViewControllerLifecycleTests

- (void)setUp
{
    [super setUp];
    if ([FIRApp defaultApp] == nil) {
        [FIRApp configure];
    }
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
}

- (void)tearDown
{
    self.window.hidden = YES;
    self.window = nil;
    [super tearDown];
}

- (void)performOnMain:(void (^)(void))block
{
    XCTestExpectation *expectation = [self expectationWithDescription:@"main-thread-task"];
    dispatch_async(dispatch_get_main_queue(), ^{
        block();
        [expectation fulfill];
    });
    // Use a longer timeout to account for CI environment variability
    [self waitForExpectations:@[expectation] timeout:10.0];
}

- (void)exerciseController:(UIViewController *)controller actions:(void (^)(UIViewController *controller))actions
{
    [self performOnMain:^{
        self.window.rootViewController = controller;
        [self.window makeKeyAndVisible];
        (void)controller.view;
        [controller beginAppearanceTransition:YES animated:NO];
        [controller endAppearanceTransition];
        if (actions) {
            actions(controller);
        }
        [controller beginAppearanceTransition:NO animated:NO];
        [controller endAppearanceTransition];
        self.window.rootViewController = nil;
    }];
}

- (void)testPitchPipeViewControllerLifecycle
{
    [self exerciseController:[DPPitchPipeViewController new] actions:^(UIViewController *controller) {
        DPPitchPipeViewController *vc = (DPPitchPipeViewController *)controller;
        [vc refreshButtons];
        NSArray *noteButtons = [vc valueForKey:@"noteButtons"];
        for (id wrapper in noteButtons) {
            UIControl *button = [wrapper valueForKey:@"button"];
            [button sendActionsForControlEvents:UIControlEventTouchDown];
            [button sendActionsForControlEvents:UIControlEventTouchUpInside];
        }
        [vc stopNotes];
    }];
}

- (void)testKeysViewControllerDataSource
{
    [self exerciseController:[DPKeysViewController new] actions:^(UIViewController *controller) {
        DPKeysViewController *vc = (DPKeysViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        NSInteger rows = [vc tableView:table numberOfRowsInSection:0];
        if (rows > 0) {
            (void)[vc tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]];
        }
    }];
}

- (void)testSongListViewControllerFlow
{
    id songsModel = [NSClassFromString(@"DPSongsModel") performSelector:@selector(sharedInstance)];
    id list = [songsModel valueForKey:@"defaultSongList"];
    NSMutableArray *songs = [[list valueForKey:@"songs"] mutableCopy];
    [list setValue:songs forKey:@"songs"];
    [songs removeAllObjects];
    for (NSInteger index = 0; index < 2; index++) {
        DPPitchedSong *song = [DPPitchedSong new];
        song.name = [NSString stringWithFormat:@"Test %ld", (long)index];
        song.key = [DPKey majorKeys].firstObject;
        SEL addSelector = NSSelectorFromString(@"addSong:");
        if ([list respondsToSelector:addSelector]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [list performSelector:addSelector withObject:song];
            #pragma clang diagnostic pop
        }
    }

    [self exerciseController:[DPSongListViewController new] actions:^(UIViewController *controller) {
        DPSongListViewController *vc = (DPSongListViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        NSInteger rows = [vc tableView:table numberOfRowsInSection:0];
        if (rows > 0) {
            (void)[vc tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:0]];
        }
        SEL editSel = @selector(edit);
        if ([vc respondsToSelector:editSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:editSel];
            #pragma clang diagnostic pop
        }
        SEL sortSel = @selector(sort);
        if ([vc respondsToSelector:sortSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:sortSel];
            #pragma clang diagnostic pop
        }
        if (rows > 1) {
            NSIndexPath *from = [NSIndexPath indexPathForRow:0 inSection:0];
            NSIndexPath *to = [NSIndexPath indexPathForRow:rows - 1 inSection:0];
            [vc tableView:table moveRowAtIndexPath:from toIndexPath:to];
        }
        SEL doneSel = @selector(doneEditing);
        if ([vc respondsToSelector:doneSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:doneSel];
            #pragma clang diagnostic pop
        }
        SEL changedSel = @selector(songsChanged);
        if ([vc respondsToSelector:changedSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:changedSel];
            #pragma clang diagnostic pop
        }
    }];
}

- (void)testSongEditorViewControllerFlow
{
    DPPitchedSong *song = [DPPitchedSong new];
    song.name = @"Original Song";
    song.key = [DPKey majorKeys].firstObject;

    DPSongEditorViewController *editor = [DPSongEditorViewController new];
    editor.song = song;

    __block NSMutableArray<NSNumber *> *completionRecords = [NSMutableArray array];
    editor.completionCallback = ^(BOOL cancelled) {
        [completionRecords addObject:@(cancelled)];
    };

    [self exerciseController:editor actions:^(UIViewController *controller) {
        DPSongEditorViewController *vc = (DPSongEditorViewController *)controller;
        [vc setTitleText:@"Edited Song"];
        [vc selectKey:[DPKey majorKeys].lastObject];

        [vc complete];
    }];

    XCTAssertTrue([[completionRecords lastObject] isEqualToNumber:@(NO)]);
    XCTAssertEqualObjects(song.name, @"Edited Song");
}

- (void)testSongEditorKeyDialSelection
{
    DPPitchedSong *song = [DPPitchedSong new];
    song.name = @"Test Song";
    song.key = [DPKey majorKeys].firstObject;

    DPSongEditorViewController *editor = [DPSongEditorViewController new];
    editor.song = song;

    [self exerciseController:editor actions:^(UIViewController *controller) {
        DPSongEditorViewController *vc = (DPSongEditorViewController *)controller;
        XCTAssertEqualObjects([vc selectedKey], song.key, @"The dial opens on the song's key");
        XCTAssertFalse([vc isMinorKeySelected]);

        [vc selectKey:[DPKey minorKeys][3]];
        XCTAssertTrue([vc isMinorKeySelected], @"Selecting a minor key flips the mode");
        [vc complete];
    }];
    XCTAssertEqualObjects(song.key, [DPKey minorKeys][3], @"Completing stores the dialled key");
}

- (void)testSongEditorRequiresTitle
{
    DPPitchedSong *song = [DPPitchedSong new];
    song.name = @"Keep Me";
    song.key = [DPKey majorKeys].firstObject;

    DPSongEditorViewController *editor = [DPSongEditorViewController new];
    editor.song = song;
    __block NSInteger completions = 0;
    editor.completionCallback = ^(BOOL cancelled) { completions++; };

    [self exerciseController:editor actions:^(UIViewController *controller) {
        DPSongEditorViewController *vc = (DPSongEditorViewController *)controller;
        [vc setTitleText:@"   "];
        [vc complete];
        XCTAssertTrue([vc isTitleErrorVisible], @"A blank title shows the requirement inline");
    }];
    XCTAssertEqual(completions, 0, @"A blank title never completes the editor");
    XCTAssertEqualObjects(song.name, @"Keep Me");
}

- (void)testSongEditorCancel
{
    DPPitchedSong *song = [DPPitchedSong new];
    song.name = @"Cancelable";
    song.key = [DPKey minorKeys].firstObject;

    DPSongEditorViewController *editor = [DPSongEditorViewController new];
    editor.song = song;

    __block NSMutableArray<NSNumber *> *completionRecords = [NSMutableArray array];
    editor.completionCallback = ^(BOOL cancelled) {
        [completionRecords addObject:@(cancelled)];
    };

    [self exerciseController:editor actions:^(UIViewController *controller) {
        DPSongEditorViewController *vc = (DPSongEditorViewController *)controller;
        [vc cancel];
    }];

    XCTAssertTrue([[completionRecords lastObject] isEqualToNumber:@(YES)]);
    XCTAssertEqualObjects(song.name, @"Cancelable");
}

- (void)testSettingsViewControllerLayout
{
    id settings = [NSClassFromString(@"DPSettingsModel") performSelector:@selector(sharedInstance)];
    [settings setValue:@NO forKey:@"wakeLock"];
    [settings setValue:@NO forKey:@"toggleNotes"];
    DPSettingsViewController *settingsVC = [DPSettingsViewController sharedInstance];
    [self exerciseController:settingsVC actions:^(UIViewController *controller) {
        DPSettingsViewController *vc = (DPSettingsViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        NSInteger sections = [vc numberOfSectionsInTableView:table];
        for (NSInteger section = 0; section < sections; section++) {
            NSInteger rows = [vc tableView:table numberOfRowsInSection:section];
            for (NSInteger row = 0; row < MIN(rows, 2); row++) {
                (void)[vc tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:row inSection:section]];
            }
        }
        SEL completeSel = @selector(complete);
        if ([vc respondsToSelector:completeSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:completeSel];
            #pragma clang diagnostic pop
        }
    }];
}

- (void)testNotesViewControllerLayout
{
    [self exerciseController:[DPNotesViewController new] actions:^(UIViewController *controller) {
        DPNotesViewController *vc = (DPNotesViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        NSInteger sections = [vc numberOfSectionsInTableView:table];
        for (NSInteger section = 0; section < sections; section++) {
            NSInteger rows = [vc tableView:table numberOfRowsInSection:section];
            if (rows > 0) {
                (void)[vc tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:0 inSection:section]];
            }
        }
    }];
}

- (void)testLoginViewControllerSkip
{
    [self exerciseController:[DPLoginViewController new] actions:^(UIViewController *controller) {
        SEL skipSelector = NSSelectorFromString(@"skip");
        if ([controller respondsToSelector:skipSelector]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [controller performSelector:skipSelector];
            #pragma clang diagnostic pop
        }
    }];
}

@end
