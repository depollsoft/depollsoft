//
//  DPTagModelExtendedTests.m
//  tagmasterTests
//
//  Extended tests for DPTag model class covering additional scenarios.
//

#import <XCTest/XCTest.h>
#import "DPTag.h"
#import "DPVideo.h"
#import "DPTrack.h"
#import "DPRemoteLocation.h"
#import "DPNote.h"
#import "DPAccidental.h"

@interface DPTagModelExtendedTests : XCTestCase
@property (nonatomic, strong) DPTag *tag;
@end

@implementation DPTagModelExtendedTests

- (void)setUp {
    [super setUp];
    self.tag = [[DPTag alloc] init];
}

- (void)tearDown {
    self.tag = nil;
    [super tearDown];
}

#pragma mark - Helper Methods

- (DPRemoteLocation *)makeRemoteLocationWithURL:(NSString *)urlString type:(NSString *)type {
    DPRemoteLocation *loc = [[DPRemoteLocation alloc] init];
    loc.uri = [NSURL URLWithString:urlString];
    loc.type = type;
    return loc;
}

#pragma mark - Initialization Tests

- (void)testInitSetsAppVersion {
    DPTag *tag = [[DPTag alloc] init];
    XCTAssertEqual(tag.appVersion, 1, @"App version should be initialized to 1");
}

#pragma mark - Track Generation Tests

- (void)testTracksWithAllParts {
    self.tag.allPartsTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/all.mp3" type:@"mp3"];
    
    NSArray *tracks = self.tag.tracks;
    XCTAssertEqual(tracks.count, 1);
    XCTAssertEqualObjects(((DPTrack *)tracks[0]).title, @"All Parts");
}

- (void)testTracksWithTenor {
    self.tag.tenorTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/tenor.mp3" type:@"mp3"];
    
    NSArray *tracks = self.tag.tracks;
    XCTAssertEqual(tracks.count, 1);
    XCTAssertEqualObjects(((DPTrack *)tracks[0]).title, @"Tenor");
}

- (void)testTracksWithLead {
    self.tag.leadTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/lead.mp3" type:@"mp3"];
    
    NSArray *tracks = self.tag.tracks;
    XCTAssertEqual(tracks.count, 1);
    XCTAssertEqualObjects(((DPTrack *)tracks[0]).title, @"Lead");
}

- (void)testTracksWithBaritone {
    self.tag.baritoneTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/bari.mp3" type:@"mp3"];
    
    NSArray *tracks = self.tag.tracks;
    XCTAssertEqual(tracks.count, 1);
    XCTAssertEqualObjects(((DPTrack *)tracks[0]).title, @"Baritone");
}

- (void)testTracksWithBass {
    self.tag.bassTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/bass.mp3" type:@"mp3"];
    
    NSArray *tracks = self.tag.tracks;
    XCTAssertEqual(tracks.count, 1);
    XCTAssertEqualObjects(((DPTrack *)tracks[0]).title, @"Bass");
}

- (void)testTracksWithOtherParts {
    self.tag.other1TrackUri = [self makeRemoteLocationWithURL:@"https://example.com/other1.mp3" type:@"mp3"];
    self.tag.other2TrackUri = [self makeRemoteLocationWithURL:@"https://example.com/other2.mp3" type:@"mp3"];
    self.tag.other3TrackUri = [self makeRemoteLocationWithURL:@"https://example.com/other3.mp3" type:@"mp3"];
    self.tag.other4TrackUri = [self makeRemoteLocationWithURL:@"https://example.com/other4.mp3" type:@"mp3"];
    
    NSArray *tracks = self.tag.tracks;
    XCTAssertEqual(tracks.count, 4);
    XCTAssertEqualObjects(((DPTrack *)tracks[0]).title, @"Other 1");
    XCTAssertEqualObjects(((DPTrack *)tracks[1]).title, @"Other 2");
    XCTAssertEqualObjects(((DPTrack *)tracks[2]).title, @"Other 3");
    XCTAssertEqualObjects(((DPTrack *)tracks[3]).title, @"Other 4");
}

- (void)testTracksWithAllStandardParts {
    self.tag.allPartsTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/all.mp3" type:@"mp3"];
    self.tag.tenorTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/tenor.mp3" type:@"mp3"];
    self.tag.leadTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/lead.mp3" type:@"mp3"];
    self.tag.baritoneTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/bari.mp3" type:@"mp3"];
    self.tag.bassTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/bass.mp3" type:@"mp3"];
    
    NSArray *tracks = self.tag.tracks;
    XCTAssertEqual(tracks.count, 5);
    
    // Verify order: All Parts, Tenor, Lead, Baritone, Bass
    XCTAssertEqualObjects(((DPTrack *)tracks[0]).title, @"All Parts");
    XCTAssertEqualObjects(((DPTrack *)tracks[1]).title, @"Tenor");
    XCTAssertEqualObjects(((DPTrack *)tracks[2]).title, @"Lead");
    XCTAssertEqualObjects(((DPTrack *)tracks[3]).title, @"Baritone");
    XCTAssertEqualObjects(((DPTrack *)tracks[4]).title, @"Bass");
}

- (void)testTracksWithNoTracks {
    // Tag with no track URIs
    NSArray *tracks = self.tag.tracks;
    XCTAssertNotNil(tracks);
    XCTAssertEqual(tracks.count, 0);
}

- (void)testTracksAreCached {
    self.tag.leadTrackUri = [self makeRemoteLocationWithURL:@"https://example.com/lead.mp3" type:@"mp3"];
    
    NSArray *tracks1 = self.tag.tracks;
    NSArray *tracks2 = self.tag.tracks;
    
    // Same reference should be returned (cached)
    XCTAssertEqual(tracks1, tracks2);
}

#pragma mark - Key Note Parsing Tests

- (void)testKeyNoteWithNilKey {
    self.tag.writtenKey = nil;
    DPNote *note = [self.tag keyNote];
    XCTAssertNil(note);
}

- (void)testKeyNoteWithEmptyKey {
    self.tag.writtenKey = @"";
    // Note: The actual code crashes on empty string due to substringToIndex:
    // This documents the current behavior - a fix would make this return nil
    XCTAssertThrowsSpecificNamed([self.tag keyNote], NSException, NSRangeException,
                                  @"keyNote should throw NSRangeException on empty key");
}

- (void)testKeyNoteWithFlatKey {
    self.tag.writtenKey = @"Bb";
    DPNote *note = [self.tag keyNote];
    XCTAssertNotNil(note);
    XCTAssertEqualObjects(note.friendlyName, @"B");
    XCTAssertEqualObjects(note.accidental, [DPAccidental enumWithInt:Flat]);
}

- (void)testKeyNoteWithSharpKey {
    self.tag.writtenKey = @"F#";
    DPNote *note = [self.tag keyNote];
    XCTAssertNotNil(note);
    XCTAssertEqualObjects(note.friendlyName, @"F");
    XCTAssertEqualObjects(note.accidental, [DPAccidental enumWithInt:Sharp]);
}

- (void)testKeyNoteWithNaturalKey {
    self.tag.writtenKey = @"C";
    DPNote *note = [self.tag keyNote];
    XCTAssertNotNil(note);
    XCTAssertEqualObjects(note.friendlyName, @"C");
    XCTAssertEqualObjects(note.accidental, [DPAccidental enumWithInt:Natural]);
}

- (void)testKeyNoteWithMajorSuffix {
    self.tag.writtenKey = @"G Major";
    DPNote *note = [self.tag keyNote];
    XCTAssertNotNil(note);
    XCTAssertEqualObjects(note.friendlyName, @"G");
}

- (void)testKeyNoteWithMinorSuffix {
    self.tag.writtenKey = @"A Minor";
    DPNote *note = [self.tag keyNote];
    XCTAssertNotNil(note);
    XCTAssertEqualObjects(note.friendlyName, @"A");
}

- (void)testKeyNoteWithColonSuffix {
    self.tag.writtenKey = @"D: Major";
    DPNote *note = [self.tag keyNote];
    XCTAssertNotNil(note);
    XCTAssertEqualObjects(note.friendlyName, @"D");
}

- (void)testKeyNoteWithLowercaseKey {
    self.tag.writtenKey = @"eb";
    DPNote *note = [self.tag keyNote];
    XCTAssertNotNil(note);
    XCTAssertEqualObjects(note.friendlyName, @"E");
    XCTAssertEqualObjects(note.accidental, [DPAccidental enumWithInt:Flat]);
}

- (void)testKeyNoteWithExtraWhitespace {
    self.tag.writtenKey = @"  Bb Major  ";
    DPNote *note = [self.tag keyNote];
    XCTAssertNotNil(note);
    XCTAssertEqualObjects(note.friendlyName, @"B");
}

#pragma mark - Tag URI Tests

- (void)testTagUriFormat {
    self.tag.tagId = 12345;
    NSURL *uri = [self.tag tagUri];
    XCTAssertEqualObjects(uri.absoluteString, @"http://tags.depoll.com/tag.php?id=12345");
}

- (void)testTagUriWithZeroId {
    self.tag.tagId = 0;
    NSURL *uri = [self.tag tagUri];
    XCTAssertEqualObjects(uri.absoluteString, @"http://tags.depoll.com/tag.php?id=0");
}

- (void)testTagUriWithLargeId {
    self.tag.tagId = 999999;
    NSURL *uri = [self.tag tagUri];
    XCTAssertTrue([uri.absoluteString containsString:@"999999"]);
}

#pragma mark - Description Tests

- (void)testDescriptionFormat {
    self.tag.tagId = 42;
    self.tag.title = @"Test Title";
    NSString *desc = [self.tag description];
    XCTAssertEqualObjects(desc, @"{Tag id: 42 Title: Test Title}");
}

- (void)testDescriptionWithNilTitle {
    self.tag.tagId = 100;
    self.tag.title = nil;
    NSString *desc = [self.tag description];
    XCTAssertTrue([desc containsString:@"100"]);
}

#pragma mark - Property Tests

- (void)testAllStringProperties {
    self.tag.title = @"Test Title";
    self.tag.alternativeTitle = @"Alt Title";
    self.tag.version = @"1.0";
    self.tag.writtenKey = @"Bb";
    self.tag.tagType = @"Ballad";
    self.tag.recordingMethod = @"Studio";
    self.tag.teachingVideo = @"vid123";
    self.tag.notes = @"Some notes";
    self.tag.arranger = @"John Doe";
    self.tag.sungBy = @"Quartet";
    self.tag.learningTrackQuartet = @"LT Quartet";
    self.tag.teacher = @"Teacher";
    self.tag.provider = @"Provider";
    self.tag.lyrics = @"La la la";
    
    XCTAssertEqualObjects(self.tag.title, @"Test Title");
    XCTAssertEqualObjects(self.tag.alternativeTitle, @"Alt Title");
    XCTAssertEqualObjects(self.tag.version, @"1.0");
    XCTAssertEqualObjects(self.tag.writtenKey, @"Bb");
    XCTAssertEqualObjects(self.tag.tagType, @"Ballad");
    XCTAssertEqualObjects(self.tag.recordingMethod, @"Studio");
    XCTAssertEqualObjects(self.tag.teachingVideo, @"vid123");
    XCTAssertEqualObjects(self.tag.notes, @"Some notes");
    XCTAssertEqualObjects(self.tag.arranger, @"John Doe");
    XCTAssertEqualObjects(self.tag.sungBy, @"Quartet");
    XCTAssertEqualObjects(self.tag.learningTrackQuartet, @"LT Quartet");
    XCTAssertEqualObjects(self.tag.teacher, @"Teacher");
    XCTAssertEqualObjects(self.tag.provider, @"Provider");
    XCTAssertEqualObjects(self.tag.lyrics, @"La la la");
}

- (void)testAllIntegerProperties {
    self.tag.tagId = 123;
    self.tag.parts = 4;
    self.tag.yearArranged = 2020;
    self.tag.sungYear = 2021;
    self.tag.classicTagNumber = 77;
    self.tag.downloadCount = 1000;
    
    XCTAssertEqual(self.tag.tagId, 123);
    XCTAssertEqual(self.tag.parts, 4);
    XCTAssertEqual(self.tag.yearArranged, 2020);
    XCTAssertEqual(self.tag.sungYear, 2021);
    XCTAssertEqual(self.tag.classicTagNumber, 77);
    XCTAssertEqual(self.tag.downloadCount, 1000);
}

- (void)testRatingProperty {
    self.tag.rating = 4.5;
    XCTAssertEqual(self.tag.rating, 4.5);
    
    self.tag.rating = 0.0;
    XCTAssertEqual(self.tag.rating, 0.0);
    
    self.tag.rating = 5.0;
    XCTAssertEqual(self.tag.rating, 5.0);
}

- (void)testAllURLProperties {
    NSURL *arrangerURL = [NSURL URLWithString:@"https://arranger.example.com"];
    NSURL *sungByURL = [NSURL URLWithString:@"https://sungby.example.com"];
    NSURL *ltqURL = [NSURL URLWithString:@"https://ltq.example.com"];
    NSURL *teacherURL = [NSURL URLWithString:@"https://teacher.example.com"];
    NSURL *providerURL = [NSURL URLWithString:@"https://provider.example.com"];
    
    self.tag.arrangerWebsite = arrangerURL;
    self.tag.sungByWebsite = sungByURL;
    self.tag.learningTrackQuartetWebsite = ltqURL;
    self.tag.teacherWebsite = teacherURL;
    self.tag.providerWebsite = providerURL;
    
    XCTAssertEqualObjects(self.tag.arrangerWebsite, arrangerURL);
    XCTAssertEqualObjects(self.tag.sungByWebsite, sungByURL);
    XCTAssertEqualObjects(self.tag.learningTrackQuartetWebsite, ltqURL);
    XCTAssertEqualObjects(self.tag.teacherWebsite, teacherURL);
    XCTAssertEqualObjects(self.tag.providerWebsite, providerURL);
}

- (void)testDateProperties {
    NSDate *posted = [NSDate dateWithTimeIntervalSince1970:1600000000];
    NSDate *lastRefreshed = [NSDate dateWithTimeIntervalSince1970:1600000100];
    
    self.tag.posted = posted;
    self.tag.lastRefreshed = lastRefreshed;
    
    XCTAssertEqualObjects(self.tag.posted, posted);
    XCTAssertEqualObjects(self.tag.lastRefreshed, lastRefreshed);
}

- (void)testRemoteLocationProperties {
    DPRemoteLocation *sheetMusic = [self makeRemoteLocationWithURL:@"https://example.com/sheet.pdf" type:@"pdf"];
    DPRemoteLocation *notation = [self makeRemoteLocationWithURL:@"https://example.com/notation.xml" type:@"xml"];
    
    self.tag.sheetMusicUri = sheetMusic;
    self.tag.notationUri = notation;
    
    XCTAssertEqual(self.tag.sheetMusicUri, sheetMusic);
    XCTAssertEqual(self.tag.notationUri, notation);
}

- (void)testVideosProperty {
    DPVideo *video1 = [[DPVideo alloc] init];
    video1.videoId = 1;
    video1.youTubeCode = @"ABC";
    
    DPVideo *video2 = [[DPVideo alloc] init];
    video2.videoId = 2;
    video2.youTubeCode = @"XYZ";
    
    self.tag.videos = @[video1, video2];
    
    XCTAssertNotNil(self.tag.videos);
    XCTAssertEqual(self.tag.videos.count, 2);
    XCTAssertEqual(((DPVideo *)self.tag.videos[0]).videoId, 1);
    XCTAssertEqual(((DPVideo *)self.tag.videos[1]).videoId, 2);
}

#pragma mark - Parts Value Tests

- (void)testPartsVariousValues {
    // Standard 4-part
    self.tag.parts = 4;
    XCTAssertEqual(self.tag.parts, 4);
    
    // 3-part (trio)
    self.tag.parts = 3;
    XCTAssertEqual(self.tag.parts, 3);
    
    // Extended parts
    for (int parts = 5; parts <= 8; parts++) {
        self.tag.parts = parts;
        XCTAssertEqual(self.tag.parts, parts);
    }
}

#pragma mark - Edge Cases

- (void)testPropertyOverwrite {
    self.tag.title = @"Original Title";
    XCTAssertEqualObjects(self.tag.title, @"Original Title");
    
    self.tag.title = @"New Title";
    XCTAssertEqualObjects(self.tag.title, @"New Title");
    
    self.tag.title = nil;
    XCTAssertNil(self.tag.title);
}

- (void)testSpecialCharactersInStrings {
    self.tag.title = @"Tag with 'quotes' and \"double quotes\"";
    XCTAssertTrue([self.tag.title containsString:@"'"]);
    
    self.tag.lyrics = @"Line 1\nLine 2\nLine 3";
    XCTAssertTrue([self.tag.lyrics containsString:@"\n"]);
    
    self.tag.notes = @"Special chars: <>&";
    XCTAssertTrue([self.tag.notes containsString:@"<"]);
}

- (void)testUnicodeInStrings {
    self.tag.title = @"Tag with émojis 🎵";
    XCTAssertTrue([self.tag.title containsString:@"🎵"]);
    
    self.tag.arranger = @"José García";
    XCTAssertEqualObjects(self.tag.arranger, @"José García");
}

@end
