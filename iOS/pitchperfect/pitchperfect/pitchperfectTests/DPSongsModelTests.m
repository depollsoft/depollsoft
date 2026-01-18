//
//  DPSongsModelTests.m
//  pitchperfectTests
//

#import <XCTest/XCTest.h>
#import "pitchperfect-Swift.h"
#import "DPPitchedSong.h"

@interface DPSongsModelTests : XCTestCase
@end

@implementation DPSongsModelTests

- (void)setUp {
    [super setUp];
    [[NSUserDefaults standardUserDefaults] removeObjectForKey:@"depollsoft.pitchperfect.SongLists"];
}

- (void)testAddRemoveSortAndStore {
    DPSongList *list = [[DPSongList alloc] initWithId:@"unit"];
    list.name = @"Unit";
    DPPitchedSong *a = [[DPPitchedSong alloc] init]; a.name = @"b Song";
    DPPitchedSong *b = [[DPPitchedSong alloc] init]; b.name = @"A Song";

    [list addSong:a];
    [list addSong:b];
    XCTAssertEqual(list.songs.count, 2);

    [list sortSongs];
    XCTAssertEqualObjects(((DPPitchedSong *)list.songs[0]).name, @"A Song");

    [list removeSong:a];
    XCTAssertEqual(list.songs.count, 1);

    [list storeValue];
    NSDictionary *stored = [[NSUserDefaults standardUserDefaults] dictionaryForKey:@"depollsoft.pitchperfect.SongLists"];
    XCTAssertNotNil(stored[@"unit"]);
}

@end

