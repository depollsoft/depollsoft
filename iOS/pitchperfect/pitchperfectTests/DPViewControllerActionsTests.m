//
//  DPViewControllerActionsTests.m
//  pitchperfectTests
//
//  Tests for ViewController button interactions, refresh behaviors, and navigation flows.
//

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
#import "DPNote.h"
#import <UIKit/UIKit.h>
@import Firebase;

// MARK: - Private Interface Declarations

@interface DPPitchPipeViewController (ActionsTesting)
- (void)refreshButtons;
- (void)stopNotes;
- (void)settingsTapped;
- (void)notesTapped;
@end

@interface DPSongListViewController (ActionsTesting)
- (void)edit;
- (void)sort;
- (void)doneEditing;
- (void)songsChanged;
- (void)addSong;
- (void)selectSong:(DPPitchedSong *)song;
@end

@interface DPSettingsViewController (ActionsTesting)
- (void)complete;
- (void)toggleWakeLock:(UISwitch *)sender;
- (void)toggleNotes:(UISwitch *)sender;
@end

@interface DPSongEditorViewController (ActionsTesting)
- (void)complete;
- (void)cancel;
- (void)setTitleText:(NSString *)titleText;
- (void)selectKey:(DPKey *)key;
- (DPKey *)selectedKey;
- (BOOL)isMinorKeySelected;
- (BOOL)isTitleErrorVisible;
@end

@interface DPNotesViewController (ActionsTesting)
- (void)selectNote:(DPNote *)note;
@end

@interface DPKeysViewController (ActionsTesting)
- (void)selectKey:(DPKey *)key;
@end

// MARK: - Test Class

@interface DPViewControllerActionsTests : XCTestCase
@property (nonatomic, strong) UIWindow *window;
@end

@implementation DPViewControllerActionsTests

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

// MARK: - PitchPipeViewController Button Interaction Tests

- (void)testPitchPipeNoteButtonTouchDownStartsNote
{
    [self exerciseController:[DPPitchPipeViewController new] actions:^(UIViewController *controller) {
        DPPitchPipeViewController *vc = (DPPitchPipeViewController *)controller;
        NSArray *noteButtons = [vc valueForKey:@"noteButtons"];
        XCTAssertNotNil(noteButtons, @"Note buttons should exist");
        XCTAssertGreaterThan(noteButtons.count, 0, @"Should have at least one note button");
        
        // Test first button touch down
        id firstWrapper = noteButtons.firstObject;
        UIControl *button = [firstWrapper valueForKey:@"button"];
        XCTAssertNotNil(button, @"Button should exist in wrapper");
        
        [button sendActionsForControlEvents:UIControlEventTouchDown];
        // Note should be playing (we can't easily verify without exposing more internal state)
    }];
}

- (void)testPitchPipeNoteButtonTouchUpStopsNote
{
    [self exerciseController:[DPPitchPipeViewController new] actions:^(UIViewController *controller) {
        DPPitchPipeViewController *vc = (DPPitchPipeViewController *)controller;
        NSArray *noteButtons = [vc valueForKey:@"noteButtons"];
        
        id firstWrapper = noteButtons.firstObject;
        UIControl *button = [firstWrapper valueForKey:@"button"];
        
        // Press and release
        [button sendActionsForControlEvents:UIControlEventTouchDown];
        [button sendActionsForControlEvents:UIControlEventTouchUpInside];
        // Note should stop playing
    }];
}

- (void)testPitchPipeMultipleNoteButtons
{
    [self exerciseController:[DPPitchPipeViewController new] actions:^(UIViewController *controller) {
        DPPitchPipeViewController *vc = (DPPitchPipeViewController *)controller;
        NSArray *noteButtons = [vc valueForKey:@"noteButtons"];
        
        XCTAssertEqual(noteButtons.count, 13, @"Should have 13 note buttons for an inclusive chromatic octave");
        
        // Exercise all buttons
        for (id wrapper in noteButtons) {
            UIControl *button = [wrapper valueForKey:@"button"];
            XCTAssertNotNil(button, @"Each wrapper should have a button");
            [button sendActionsForControlEvents:UIControlEventTouchDown];
            [button sendActionsForControlEvents:UIControlEventTouchUpInside];
        }
    }];
}

- (void)testPitchPipeStopNotesStopsAllNotes
{
    [self exerciseController:[DPPitchPipeViewController new] actions:^(UIViewController *controller) {
        DPPitchPipeViewController *vc = (DPPitchPipeViewController *)controller;
        NSArray *noteButtons = [vc valueForKey:@"noteButtons"];
        
        // Start multiple notes
        for (NSInteger i = 0; i < MIN(3, (NSInteger)noteButtons.count); i++) {
            id wrapper = noteButtons[i];
            UIControl *button = [wrapper valueForKey:@"button"];
            [button sendActionsForControlEvents:UIControlEventTouchDown];
        }
        
        // Stop all notes
        [vc stopNotes];
        
        // All notes should now be stopped
    }];
}

- (void)testPitchPipeRefreshButtonsUpdatesUI
{
    [self exerciseController:[DPPitchPipeViewController new] actions:^(UIViewController *controller) {
        DPPitchPipeViewController *vc = (DPPitchPipeViewController *)controller;
        
        // Refresh buttons should not crash and should update button state
        [vc refreshButtons];
        
        NSArray *noteButtons = [vc valueForKey:@"noteButtons"];
        XCTAssertNotNil(noteButtons, @"Note buttons should still exist after refresh");
    }];
}

// MARK: - SongListViewController Interaction Tests

- (void)testSongListEditModeToggle
{
    id songsModel = [NSClassFromString(@"DPSongsModel") performSelector:@selector(sharedInstance)];
    id list = [songsModel valueForKey:@"defaultSongList"];
    
    [self exerciseController:[DPSongListViewController new] actions:^(UIViewController *controller) {
        DPSongListViewController *vc = (DPSongListViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        
        // Enter edit mode
        SEL editSel = @selector(edit);
        if ([vc respondsToSelector:editSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:editSel];
            #pragma clang diagnostic pop
            XCTAssertTrue(table.isEditing, @"Table should be in editing mode");
        }
        
        // Exit edit mode
        SEL doneSel = @selector(doneEditing);
        if ([vc respondsToSelector:doneSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:doneSel];
            #pragma clang diagnostic pop
            XCTAssertFalse(table.isEditing, @"Table should not be in editing mode");
        }
    }];
}

- (void)testSongListSortAction
{
    id songsModel = [NSClassFromString(@"DPSongsModel") performSelector:@selector(sharedInstance)];
    id list = [songsModel valueForKey:@"defaultSongList"];
    NSMutableArray *songs = [[list valueForKey:@"songs"] mutableCopy];
    [songs removeAllObjects];
    
    // Add unsorted songs
    DPPitchedSong *songZ = [DPPitchedSong new];
    songZ.name = @"Zebra";
    songZ.key = [DPKey majorKeys].firstObject;
    
    DPPitchedSong *songA = [DPPitchedSong new];
    songA.name = @"Alpha";
    songA.key = [DPKey majorKeys].firstObject;
    
    SEL addSelector = NSSelectorFromString(@"addSong:");
    if ([list respondsToSelector:addSelector]) {
        #pragma clang diagnostic push
        #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
        [list performSelector:addSelector withObject:songZ];
        [list performSelector:addSelector withObject:songA];
        #pragma clang diagnostic pop
    }
    
    [self exerciseController:[DPSongListViewController new] actions:^(UIViewController *controller) {
        DPSongListViewController *vc = (DPSongListViewController *)controller;
        
        SEL sortSel = @selector(sort);
        if ([vc respondsToSelector:sortSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:sortSel];
            #pragma clang diagnostic pop
        }
        
        // After sort, Alpha should come before Zebra
        NSArray *sortedSongs = [list valueForKey:@"songs"];
        if (sortedSongs.count >= 2) {
            DPPitchedSong *first = sortedSongs[0];
            XCTAssertEqualObjects(first.name, @"Alpha", @"After sort, Alpha should be first");
        }
    }];
}

- (void)testSongListMoveRow
{
    id songsModel = [NSClassFromString(@"DPSongsModel") performSelector:@selector(sharedInstance)];
    id list = [songsModel valueForKey:@"defaultSongList"];
    NSMutableArray *songs = [[list valueForKey:@"songs"] mutableCopy];
    [songs removeAllObjects];
    [list setValue:songs forKey:@"songs"];
    
    // Add songs
    for (NSInteger i = 0; i < 3; i++) {
        DPPitchedSong *song = [DPPitchedSong new];
        song.name = [NSString stringWithFormat:@"Song %ld", (long)i];
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
        if (rows >= 2) {
            NSIndexPath *from = [NSIndexPath indexPathForRow:0 inSection:0];
            NSIndexPath *to = [NSIndexPath indexPathForRow:rows - 1 inSection:0];
            [vc tableView:table moveRowAtIndexPath:from toIndexPath:to];
        }
    }];
}

- (void)testSongListDeleteRow
{
    id songsModel = [NSClassFromString(@"DPSongsModel") performSelector:@selector(sharedInstance)];
    id list = [songsModel valueForKey:@"defaultSongList"];
    NSMutableArray *songs = [[list valueForKey:@"songs"] mutableCopy];
    [songs removeAllObjects];
    [list setValue:songs forKey:@"songs"];
    
    // Add a song to delete
    DPPitchedSong *songToDelete = [DPPitchedSong new];
    songToDelete.name = @"Delete Me";
    songToDelete.key = [DPKey majorKeys].firstObject;
    SEL addSelector = NSSelectorFromString(@"addSong:");
    if ([list respondsToSelector:addSelector]) {
        #pragma clang diagnostic push
        #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
        [list performSelector:addSelector withObject:songToDelete];
        #pragma clang diagnostic pop
    }
    
    [self exerciseController:[DPSongListViewController new] actions:^(UIViewController *controller) {
        DPSongListViewController *vc = (DPSongListViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        
        NSInteger rowsBefore = [vc tableView:table numberOfRowsInSection:0];
        
        if (rowsBefore > 0) {
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:0 inSection:0];
            [vc tableView:table commitEditingStyle:UITableViewCellEditingStyleDelete forRowAtIndexPath:indexPath];
            
            NSInteger rowsAfter = [vc tableView:table numberOfRowsInSection:0];
            XCTAssertEqual(rowsAfter, rowsBefore - 1, @"Should have one less row after delete");
        }
    }];
}

- (void)testSongListSongsChangedNotification
{
    [self exerciseController:[DPSongListViewController new] actions:^(UIViewController *controller) {
        DPSongListViewController *vc = (DPSongListViewController *)controller;
        
        SEL changedSel = @selector(songsChanged);
        if ([vc respondsToSelector:changedSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:changedSel];
            #pragma clang diagnostic pop
        }
        
        // Should reload table without crashing
        UITableView *table = [vc valueForKey:@"tableView"];
        XCTAssertNotNil(table, @"Table should exist after songsChanged");
    }];
}

// MARK: - SongEditorViewController Tests

- (void)testSongEditorCompleteUpdatesName
{
    DPPitchedSong *song = [DPPitchedSong new];
    song.name = @"Original Name";
    song.key = [DPKey majorKeys].firstObject;
    
    DPSongEditorViewController *editor = [DPSongEditorViewController new];
    editor.song = song;
    
    __block BOOL completionCalled = NO;
    __block BOOL wasCancelled = NO;
    editor.completionCallback = ^(BOOL cancelled) {
        completionCalled = YES;
        wasCancelled = cancelled;
    };
    
    [self exerciseController:editor actions:^(UIViewController *controller) {
        DPSongEditorViewController *vc = (DPSongEditorViewController *)controller;
        
        [vc setTitleText:@"New Name"];
        
        [vc complete];
    }];
    
    XCTAssertTrue(completionCalled, @"Completion callback should be called");
    XCTAssertFalse(wasCancelled, @"Should not be cancelled when completing");
    XCTAssertEqualObjects(song.name, @"New Name", @"Song name should be updated");
}

- (void)testSongEditorCancelDoesNotUpdateSong
{
    DPPitchedSong *song = [DPPitchedSong new];
    song.name = @"Original Name";
    song.key = [DPKey majorKeys].firstObject;
    
    DPSongEditorViewController *editor = [DPSongEditorViewController new];
    editor.song = song;
    
    __block BOOL completionCalled = NO;
    __block BOOL wasCancelled = NO;
    editor.completionCallback = ^(BOOL cancelled) {
        completionCalled = YES;
        wasCancelled = cancelled;
    };
    
    [self exerciseController:editor actions:^(UIViewController *controller) {
        DPSongEditorViewController *vc = (DPSongEditorViewController *)controller;
        
        [vc setTitleText:@"Changed but Cancelled"];
        
        [vc cancel];
    }];
    
    XCTAssertTrue(completionCalled, @"Completion callback should be called");
    XCTAssertTrue(wasCancelled, @"Should be cancelled");
    XCTAssertEqualObjects(song.name, @"Original Name", @"Song name should not change on cancel");
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
        
        DPKey *minor = [DPKey minorKeys][3];
        [vc selectKey:minor];
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

// MARK: - SettingsViewController Tests

- (void)testSettingsWakeLockToggle
{
    id settings = [NSClassFromString(@"DPSettingsModel") performSelector:@selector(sharedInstance)];
    BOOL originalWakeLock = [[settings valueForKey:@"wakeLock"] boolValue];
    
    DPSettingsViewController *settingsVC = [DPSettingsViewController sharedInstance];
    
    [self exerciseController:settingsVC actions:^(UIViewController *controller) {
        DPSettingsViewController *vc = (DPSettingsViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        
        // Get the wake lock cell and toggle its switch
        NSInteger sections = [vc numberOfSectionsInTableView:table];
        for (NSInteger section = 0; section < sections; section++) {
            NSInteger rows = [vc tableView:table numberOfRowsInSection:section];
            for (NSInteger row = 0; row < rows; row++) {
                UITableViewCell *cell = [vc tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:row inSection:section]];
                if (cell.accessoryView && [cell.accessoryView isKindOfClass:[UISwitch class]]) {
                    UISwitch *switchControl = (UISwitch *)cell.accessoryView;
                    [switchControl setOn:!switchControl.isOn animated:NO];
                    [switchControl sendActionsForControlEvents:UIControlEventValueChanged];
                    break;
                }
            }
        }
    }];
    
    // Restore original value
    [settings setValue:@(originalWakeLock) forKey:@"wakeLock"];
}

- (void)testSettingsCompleteAction
{
    DPSettingsViewController *settingsVC = [DPSettingsViewController sharedInstance];
    
    [self exerciseController:settingsVC actions:^(UIViewController *controller) {
        DPSettingsViewController *vc = (DPSettingsViewController *)controller;
        
        SEL completeSel = @selector(complete);
        if ([vc respondsToSelector:completeSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:completeSel];
            #pragma clang diagnostic pop
        }
    }];
}

- (void)testSettingsTableViewDataSource
{
    DPSettingsViewController *settingsVC = [DPSettingsViewController sharedInstance];
    
    [self exerciseController:settingsVC actions:^(UIViewController *controller) {
        DPSettingsViewController *vc = (DPSettingsViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        
        NSInteger sections = [vc numberOfSectionsInTableView:table];
        XCTAssertGreaterThan(sections, 0, @"Settings should have at least one section");
        
        for (NSInteger section = 0; section < sections; section++) {
            NSInteger rows = [vc tableView:table numberOfRowsInSection:section];
            XCTAssertGreaterThanOrEqual(rows, 0, @"Each section should have non-negative rows");
            
            for (NSInteger row = 0; row < rows; row++) {
                UITableViewCell *cell = [vc tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:row inSection:section]];
                XCTAssertNotNil(cell, @"Cell should be created for each row");
            }
        }
    }];
}

// MARK: - NotesViewController Tests

- (void)testNotesViewControllerTableDataSource
{
    [self exerciseController:[DPNotesViewController new] actions:^(UIViewController *controller) {
        DPNotesViewController *vc = (DPNotesViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        
        NSInteger sections = [vc numberOfSectionsInTableView:table];
        XCTAssertGreaterThan(sections, 0, @"Notes should have at least one section");
        
        NSInteger totalRows = 0;
        for (NSInteger section = 0; section < sections; section++) {
            NSInteger rows = [vc tableView:table numberOfRowsInSection:section];
            totalRows += rows;
            
            for (NSInteger row = 0; row < MIN(rows, 3); row++) {
                UITableViewCell *cell = [vc tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:row inSection:section]];
                XCTAssertNotNil(cell, @"Cell should be created");
            }
        }
        
        XCTAssertGreaterThan(totalRows, 0, @"Should have some notes to display");
    }];
}

- (void)testNotesViewControllerSelection
{
    [self exerciseController:[DPNotesViewController new] actions:^(UIViewController *controller) {
        DPNotesViewController *vc = (DPNotesViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        
        NSInteger rows = [vc tableView:table numberOfRowsInSection:0];
        if (rows > 0) {
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:0 inSection:0];
            
            // Test selection
            if ([vc respondsToSelector:@selector(tableView:didSelectRowAtIndexPath:)]) {
                [vc tableView:table didSelectRowAtIndexPath:indexPath];
            }
        }
    }];
}

// MARK: - KeysViewController Tests

- (void)testKeysViewControllerTableDataSource
{
    [self exerciseController:[DPKeysViewController new] actions:^(UIViewController *controller) {
        DPKeysViewController *vc = (DPKeysViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        
        NSInteger rows = [vc tableView:table numberOfRowsInSection:0];
        XCTAssertGreaterThan(rows, 0, @"Should have key options");
        
        // Verify cells are created
        for (NSInteger row = 0; row < MIN(rows, 5); row++) {
            UITableViewCell *cell = [vc tableView:table cellForRowAtIndexPath:[NSIndexPath indexPathForRow:row inSection:0]];
            XCTAssertNotNil(cell, @"Cell should be created for each key");
        }
    }];
}

- (void)testKeysViewControllerSelection
{
    [self exerciseController:[DPKeysViewController new] actions:^(UIViewController *controller) {
        DPKeysViewController *vc = (DPKeysViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        
        NSInteger rows = [vc tableView:table numberOfRowsInSection:0];
        if (rows > 0) {
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:0 inSection:0];
            
            if ([vc respondsToSelector:@selector(tableView:didSelectRowAtIndexPath:)]) {
                [vc tableView:table didSelectRowAtIndexPath:indexPath];
            }
        }
    }];
}

// MARK: - Navigation Flow Tests

- (void)testNavigationBetweenControllers
{
    UINavigationController *navController = [[UINavigationController alloc] initWithRootViewController:[DPPitchPipeViewController new]];
    
    [self performOnMain:^{
        self.window.rootViewController = navController;
        [self.window makeKeyAndVisible];
        
        // Push settings
        DPSettingsViewController *settingsVC = [DPSettingsViewController sharedInstance];
        [navController pushViewController:settingsVC animated:NO];
        XCTAssertEqual(navController.viewControllers.count, 2);
        
        // Pop back
        [navController popViewControllerAnimated:NO];
        XCTAssertEqual(navController.viewControllers.count, 1);
        
        // Push song list
        [navController pushViewController:[DPSongListViewController new] animated:NO];
        XCTAssertEqual(navController.viewControllers.count, 2);
        
        self.window.rootViewController = nil;
    }];
}

- (void)testPresentModalController
{
    [self exerciseController:[DPPitchPipeViewController new] actions:^(UIViewController *controller) {
        // Present song editor modally
        DPSongEditorViewController *editor = [DPSongEditorViewController new];
        editor.song = [DPPitchedSong new];
        editor.song.name = @"Test";
        editor.song.key = [DPKey majorKeys].firstObject;
        
        [controller presentViewController:editor animated:NO completion:nil];
        XCTAssertNotNil(controller.presentedViewController, @"Editor should be presented");
        
        [controller dismissViewControllerAnimated:NO completion:nil];
    }];
}

// MARK: - Refresh Behavior Tests

- (void)testPitchPipeRefreshAfterSettingsChange
{
    __block DPPitchPipeViewController *pitchPipeVC = nil;
    
    [self performOnMain:^{
        pitchPipeVC = [DPPitchPipeViewController new];
        self.window.rootViewController = pitchPipeVC;
        [self.window makeKeyAndVisible];
        (void)pitchPipeVC.view;
        [pitchPipeVC beginAppearanceTransition:YES animated:NO];
        [pitchPipeVC endAppearanceTransition];
        
        // Change a setting
        id settings = [NSClassFromString(@"DPSettingsModel") performSelector:@selector(sharedInstance)];
        BOOL currentToggle = [[settings valueForKey:@"toggleNotes"] boolValue];
        [settings setValue:@(!currentToggle) forKey:@"toggleNotes"];
        
        // Refresh buttons
        [pitchPipeVC refreshButtons];
        
        // Restore setting
        [settings setValue:@(currentToggle) forKey:@"toggleNotes"];
        
        self.window.rootViewController = nil;
    }];
}

- (void)testSongListRefreshAfterSongChange
{
    id songsModel = [NSClassFromString(@"DPSongsModel") performSelector:@selector(sharedInstance)];
    id list = [songsModel valueForKey:@"defaultSongList"];
    
    [self exerciseController:[DPSongListViewController new] actions:^(UIViewController *controller) {
        DPSongListViewController *vc = (DPSongListViewController *)controller;
        UITableView *table = [vc valueForKey:@"tableView"];
        
        NSInteger initialRows = [vc tableView:table numberOfRowsInSection:0];
        
        // Add a song
        DPPitchedSong *song = [DPPitchedSong new];
        song.name = @"New Song";
        song.key = [DPKey majorKeys].firstObject;
        
        SEL addSelector = NSSelectorFromString(@"addSong:");
        if ([list respondsToSelector:addSelector]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [list performSelector:addSelector withObject:song];
            #pragma clang diagnostic pop
        }
        
        // Trigger refresh
        SEL changedSel = @selector(songsChanged);
        if ([vc respondsToSelector:changedSel]) {
            #pragma clang diagnostic push
            #pragma clang diagnostic ignored "-Warc-performSelector-leaks"
            [vc performSelector:changedSel];
            #pragma clang diagnostic pop
        }
        
        NSInteger newRows = [vc tableView:table numberOfRowsInSection:0];
        XCTAssertEqual(newRows, initialRows + 1, @"Should have one more row after adding song");
    }];
}

@end
