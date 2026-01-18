//
//  TagmasterModelsTests.m
//  tagmasterTests
//

#import <XCTest/XCTest.h>
#import "DPTrack.h"
#import "DPVideo.h"
#import "DPRemoteLocation.h"
#import "DPTag.h"
#import "DPNote.h"
#import "DPAccidental.h"

@interface TagmasterModelsTests : XCTestCase
@end

@implementation TagmasterModelsTests

- (void)testTrackFactorySetsTitleAndSource {
    DPRemoteLocation *src = [[DPRemoteLocation alloc] init];
    src.uri = [NSURL URLWithString:@"https://example.com/t.mp3"];
    src.type = @"mp3";
    DPTrack *t = [DPTrack trackWithTitle:@"Lead" source:src];
    XCTAssertEqualObjects(t.title, @"Lead");
    XCTAssertEqual(t.source, src);
}

- (void)testVideoPropertiesRoundTrip {
    DPVideo *v = [[DPVideo alloc] init];
    v.videoId = 9;
    v.sungBy = @"Quartet";
    v.youTubeCode = @"ABC";
    v.description = @"Desc";
    v.sungKey = @"G";
    v.isMultitrack = YES;
    v.sungWebsite = [NSURL URLWithString:@"https://example.com"]; 
    v.posted = [NSDate dateWithTimeIntervalSince1970:1000];

    XCTAssertEqual(v.videoId, 9);
    XCTAssertEqualObjects(v.sungBy, @"Quartet");
    XCTAssertEqualObjects(v.youTubeCode, @"ABC");
    XCTAssertEqualObjects(v.description, @"Desc");
    XCTAssertEqualObjects(v.sungKey, @"G");
    XCTAssertTrue(v.isMultitrack);
    XCTAssertNotNil(v.sungWebsite);
    XCTAssertNotNil(v.posted);
}

- (void)testTagPropertiesRoundTrip {
    DPTag *tag = [[DPTag alloc] init];
    tag.tagId = 123;
    tag.title = @"My Tag";
    tag.arranger = @"Arranger";
    tag.writtenKey = @"Bb";
    tag.parts = 4;
    tag.tagType = @"Ballad";
    tag.recordingMethod = @"Studio";
    tag.teachingVideo = @"vid123";
    tag.notes = @"Notes";
    tag.yearArranged = 2020;
    tag.sungBy = @"Quartet";
    tag.sungYear = 2021;
    tag.learningTrackQuartet = @"LTQ";
    tag.teacher = @"Teacher";
    tag.provider = @"Provider";
    tag.posted = [NSDate dateWithTimeIntervalSince1970:1000];
    tag.classicTagNumber = 5;
    tag.rating = 4.5;
    tag.downloadCount = 100;
    tag.lyrics = @"Lyrics";
    
    XCTAssertEqual(tag.tagId, 123);
    XCTAssertEqualObjects(tag.title, @"My Tag");
    XCTAssertEqualObjects(tag.arranger, @"Arranger");
    XCTAssertEqualObjects(tag.writtenKey, @"Bb");
    XCTAssertEqual(tag.parts, 4);
    XCTAssertEqualObjects(tag.tagType, @"Ballad");
    XCTAssertEqualObjects(tag.recordingMethod, @"Studio");
    XCTAssertEqualObjects(tag.teachingVideo, @"vid123");
    XCTAssertEqualObjects(tag.notes, @"Notes");
    XCTAssertEqual(tag.yearArranged, 2020);
    XCTAssertEqualObjects(tag.sungBy, @"Quartet");
    XCTAssertEqual(tag.sungYear, 2021);
    XCTAssertEqualObjects(tag.learningTrackQuartet, @"LTQ");
    XCTAssertEqualObjects(tag.teacher, @"Teacher");
    XCTAssertEqualObjects(tag.provider, @"Provider");
    XCTAssertEqualObjects(tag.posted, [NSDate dateWithTimeIntervalSince1970:1000]);
    XCTAssertEqual(tag.classicTagNumber, 5);
    XCTAssertEqual(tag.rating, 4.5);
    XCTAssertEqual(tag.downloadCount, 100);
    XCTAssertEqualObjects(tag.lyrics, @"Lyrics");
}

- (void)testTagTracksGeneration {
    DPTag *tag = [[DPTag alloc] init];
    DPRemoteLocation *loc = [[DPRemoteLocation alloc] init];
    loc.uri = [NSURL URLWithString:@"http://example.com/lead.mp3"];
    tag.leadTrackUri = loc;
    
    NSArray *tracks = tag.tracks;
    XCTAssertEqual(tracks.count, 1);
    DPTrack *track = tracks[0];
    XCTAssertEqualObjects(track.title, @"Lead");
    XCTAssertEqualObjects(track.source, loc);
    
    // Add another track
    DPRemoteLocation *loc2 = [[DPRemoteLocation alloc] init];
    loc2.uri = [NSURL URLWithString:@"http://example.com/tenor.mp3"];
    tag.tenorTrackUri = loc2;
    
    // tracks is cached, so we might need to recreate tag or handle cache invalidation if we want to test dynamic updates,
    // but DPTag implementation caches tracks in associated object on first access.
    // So let's create a new tag for multi-track test.
    
    DPTag *tag2 = [[DPTag alloc] init];
    tag2.leadTrackUri = loc;
    tag2.tenorTrackUri = loc2;
    
    NSArray *tracks2 = tag2.tracks;
    XCTAssertEqual(tracks2.count, 2);
}

- (void)testTagUri {
    DPTag *tag = [[DPTag alloc] init];
    tag.tagId = 555;
    NSURL *uri = [tag tagUri];
    XCTAssertEqualObjects(uri.absoluteString, @"http://tags.depoll.com/tag.php?id=555");
}

- (void)testKeyNoteParsing {
    DPTag *tag = [[DPTag alloc] init];
    tag.writtenKey = @"Bb Major";
    DPNote *note = [tag keyNote];
    XCTAssertEqualObjects(note.friendlyName, @"B");
    XCTAssertEqualObjects(note.accidental, [DPAccidental enumWithInt:Flat]);
    
    tag.writtenKey = @"F#";
    note = [tag keyNote];
    XCTAssertEqualObjects(note.friendlyName, @"F");
    XCTAssertEqualObjects(note.accidental, [DPAccidental enumWithInt:Sharp]);
    
    tag.writtenKey = @"C";
    note = [tag keyNote];
    XCTAssertEqualObjects(note.friendlyName, @"C");
    XCTAssertEqualObjects(note.accidental, [DPAccidental enumWithInt:Natural]);
}

@end

