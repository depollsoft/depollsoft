//
//  DPPitchedSongEdgeCaseTests.m
//  pitchperfectlibTests
//
//  Edge case and coverage tests for DPPitchedSong - tests actual business logic
//

#import <XCTest/XCTest.h>
#import "DPPitchedSong.h"
#import "DPKey.h"
#import "DPKeyType.h"
#import "DPNote.h"
#import "DPAccidental.h"

// Mock note for testing without audio
@interface MockDPNote : DPNote
@property (nonatomic) NSInteger playCallCount;
@property (nonatomic) NSInteger stopCallCount;
@end

@implementation MockDPNote

- (void)play {
    self.playCallCount++;
    self.isPlaying = YES;
}

- (void)stop {
    self.stopCallCount++;
    self.isPlaying = NO;
}

@end

@interface DPPitchedSongEdgeCaseTests : XCTestCase
@end

@implementation DPPitchedSongEdgeCaseTests

#pragma mark - Unique ID Generation Tests

- (void)testEachSongHasUniqueId {
    NSMutableSet *ids = [NSMutableSet set];
    
    for (int i = 0; i < 100; i++) {
        DPPitchedSong *song = [[DPPitchedSong alloc] init];
        XCTAssertFalse([ids containsObject:song.id], @"ID %@ should be unique", song.id);
        [ids addObject:song.id];
    }
    
    XCTAssertEqual(ids.count, 100, @"All 100 songs should have unique IDs");
}

#pragma mark - Equality Logic Tests

- (void)testEqualityBasedOnIdOnly {
    DPPitchedSong *song1 = [[DPPitchedSong alloc] init];
    DPPitchedSong *song2 = [[DPPitchedSong alloc] init];
    
    // Initially different IDs means not equal
    XCTAssertFalse([song1 isEqual:song2]);
    
    // Set same ID - should now be equal regardless of other properties
    song2.id = song1.id;
    song1.name = @"Song A";
    song2.name = @"Song B";
    song1.key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                    keyType:[DPKeyType enumWithInt:Major]
                             numAccidentals:0];
    song2.key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"G" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                    keyType:[DPKeyType enumWithInt:Major]
                             numAccidentals:1];
    
    XCTAssertTrue([song1 isEqual:song2], @"Songs with same ID should be equal regardless of other properties");
}

- (void)testSongNotEqualToDifferentTypes {
    DPPitchedSong *song = [[DPPitchedSong alloc] init];
    
    XCTAssertFalse([song isEqual:nil]);
    XCTAssertFalse([song isEqual:song.id], @"Song should not equal its own ID string");
    XCTAssertFalse([song isEqual:@123]);
    XCTAssertFalse([song isEqual:[[DPKey alloc] init]]);
    XCTAssertFalse([song isEqual:[DPNote C4]]);
}

#pragma mark - Play/Stop State Management Tests

- (void)testPlayStopDelegatestoNoteAndTracksState {
    MockDPNote *mockNote = [[MockDPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:440.0];
    DPKey *key = [[DPKey alloc] initWithNote:mockNote keyType:[DPKeyType enumWithInt:Major] numAccidentals:0];
    
    DPPitchedSong *song = [[DPPitchedSong alloc] init];
    song.key = key;
    
    XCTAssertFalse([song isPlaying], @"Should not be playing initially");
    
    [song play];
    XCTAssertTrue([song isPlaying], @"Should be playing after play");
    XCTAssertEqual(mockNote.playCallCount, 1, @"Play should call note.play");
    
    [song stop];
    XCTAssertFalse([song isPlaying], @"Should not be playing after stop");
    XCTAssertEqual(mockNote.stopCallCount, 1, @"Stop should call note.stop");
}

- (void)testRepeatedPlayStopCalls {
    MockDPNote *mockNote = [[MockDPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:440.0];
    DPKey *key = [[DPKey alloc] initWithNote:mockNote keyType:[DPKeyType enumWithInt:Major] numAccidentals:0];
    
    DPPitchedSong *song = [[DPPitchedSong alloc] init];
    song.key = key;
    
    // Multiple play calls should each invoke note.play
    [song play];
    [song play];
    [song play];
    XCTAssertEqual(mockNote.playCallCount, 3);
    
    // Multiple stop calls should each invoke note.stop
    [song stop];
    [song stop];
    XCTAssertEqual(mockNote.stopCallCount, 2);
}

#pragma mark - Key Type Handling Tests

- (void)testPlayWorksWithAllKeyTypes {
    // Test with major key
    MockDPNote *majorNote = [[MockDPNote alloc] initWithFriendlyName:@"C" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:261.63];
    DPKey *majorKey = [[DPKey alloc] initWithNote:majorNote keyType:[DPKeyType enumWithInt:Major] numAccidentals:0];
    DPPitchedSong *majorSong = [[DPPitchedSong alloc] init];
    majorSong.key = majorKey;
    [majorSong play];
    XCTAssertTrue([majorSong isPlaying], @"Should play with major key");
    [majorSong stop];
    
    // Test with minor key
    MockDPNote *minorNote = [[MockDPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:440.0];
    DPKey *minorKey = [[DPKey alloc] initWithNote:minorNote keyType:[DPKeyType enumWithInt:Minor] numAccidentals:0];
    DPPitchedSong *minorSong = [[DPPitchedSong alloc] init];
    minorSong.key = minorKey;
    [minorSong play];
    XCTAssertTrue([minorSong isPlaying], @"Should play with minor key");
    [minorSong stop];
    
    // Test with sharp key
    MockDPNote *sharpNote = [[MockDPNote alloc] initWithFriendlyName:@"F" octave:4 accidental:[DPAccidental enumWithInt:Sharp] frequency:370.0];
    DPKey *sharpKey = [[DPKey alloc] initWithNote:sharpNote keyType:[DPKeyType enumWithInt:Major] numAccidentals:6];
    DPPitchedSong *sharpSong = [[DPPitchedSong alloc] init];
    sharpSong.key = sharpKey;
    [sharpSong play];
    XCTAssertTrue([sharpSong isPlaying], @"Should play with sharp key");
    [sharpSong stop];
}

#pragma mark - Song Identity Invariants

- (void)testIdRemainsStableAcrossPropertyChanges {
    DPPitchedSong *song = [[DPPitchedSong alloc] init];
    NSString *originalId = song.id;
    
    // Change properties
    song.name = @"New Name";
    XCTAssertEqualObjects(song.id, originalId, @"ID should not change when name is changed");
    
    song.key = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                   keyType:[DPKeyType enumWithInt:Major]
                            numAccidentals:0];
    XCTAssertEqualObjects(song.id, originalId, @"ID should not change when key is changed");
}

- (void)testMultipleSongsCanShareKeyWithoutConflict {
    DPKey *sharedKey = [[DPKey alloc] initWithNote:[DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4]
                                           keyType:[DPKeyType enumWithInt:Major]
                                    numAccidentals:0];
    
    DPPitchedSong *song1 = [[DPPitchedSong alloc] init];
    song1.name = @"Song 1";
    song1.key = sharedKey;
    
    DPPitchedSong *song2 = [[DPPitchedSong alloc] init];
    song2.name = @"Song 2";
    song2.key = sharedKey;
    
    XCTAssertEqualObjects(song1.key, song2.key, @"Multiple songs can share the same key");
    XCTAssertFalse([song1 isEqual:song2], @"Songs with different IDs should not be equal");
}

@end
