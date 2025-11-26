//
//  DPPitchedSongTests.m
//

#import <XCTest/XCTest.h>
#import "STAssertCompat.h"
#import "DPPitchedSong.h"
#import "DPKey.h"
#import "DPKeyType.h"
#import "DPNote.h"
#import "DPAccidental.h"

@interface FakeNote : DPNote
@property (nonatomic) NSInteger playCount;
@property (nonatomic) NSInteger stopCount;
@end
@implementation FakeNote
- (void)play { self.playCount++; self.isPlaying = YES; }
- (void)stop { self.stopCount++; self.isPlaying = NO; }
@end

@interface DPPitchedSongTests : XCTestCase
@end

@implementation DPPitchedSongTests

- (void)testInitSetsIdAndEqualityOnId {
    DPPitchedSong *a = [[DPPitchedSong alloc] init];
    DPPitchedSong *b = [[DPPitchedSong alloc] init];
    STAssertNotNil(a.id, @"id should be set");
    STAssertFalse([a isEqual:b], @"Different ids not equal");
    b.id = a.id;
    STAssertTrue([a isEqual:b], @"Equal ids equal");
}

-(void)testPlayAndStopToggleIsPlayingAndInvokesNote {
    FakeNote *n = [[FakeNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:440.0];
    DPKey *k = [[DPKey alloc] initWithNote:n keyType:[DPKeyType enumWithInt:Major] numAccidentals:0];
    DPPitchedSong *s = [[DPPitchedSong alloc] init];
    s.key = k;

    [s play];
    STAssertTrue(n.isPlaying, @"Note should be playing");
    STAssertEquals(n.playCount, (NSInteger)1, @"play called once");

    [s stop];
    STAssertFalse(n.isPlaying, @"Note should be stopped");
    STAssertEquals(n.stopCount, (NSInteger)1, @"stop called once");
}

@end
