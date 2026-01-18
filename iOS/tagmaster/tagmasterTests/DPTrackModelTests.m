//
//  DPTrackModelTests.m
//  tagmasterTests
//
//  Comprehensive tests for DPTrack model class.
//

#import <XCTest/XCTest.h>
#import "DPTrack.h"
#import "DPRemoteLocation.h"

@interface DPTrackModelTests : XCTestCase
@property (nonatomic, strong) DPTrack *track;
@property (nonatomic, strong) DPRemoteLocation *sampleSource;
@end

@implementation DPTrackModelTests

- (void)setUp {
    [super setUp];
    self.track = [[DPTrack alloc] init];
    self.sampleSource = [[DPRemoteLocation alloc] init];
    self.sampleSource.uri = [NSURL URLWithString:@"https://example.com/track.mp3"];
    self.sampleSource.type = @"mp3";
}

- (void)tearDown {
    self.track = nil;
    self.sampleSource = nil;
    [super tearDown];
}

#pragma mark - Factory Method Tests

- (void)testTrackFactoryCreatesInstanceWithTitleAndSource {
    DPRemoteLocation *source = [[DPRemoteLocation alloc] init];
    source.uri = [NSURL URLWithString:@"https://example.com/lead.mp3"];
    source.type = @"mp3";
    
    DPTrack *track = [DPTrack trackWithTitle:@"Lead" source:source];
    
    XCTAssertNotNil(track);
    XCTAssertEqualObjects(track.title, @"Lead");
    XCTAssertEqual(track.source, source);
}

- (void)testTrackFactoryWithNilTitle {
    DPTrack *track = [DPTrack trackWithTitle:nil source:self.sampleSource];
    
    XCTAssertNotNil(track);
    XCTAssertNil(track.title);
    XCTAssertEqual(track.source, self.sampleSource);
}

- (void)testTrackFactoryWithNilSource {
    DPTrack *track = [DPTrack trackWithTitle:@"Test Track" source:nil];
    
    XCTAssertNotNil(track);
    XCTAssertEqualObjects(track.title, @"Test Track");
    XCTAssertNil(track.source);
}

- (void)testTrackFactoryWithBothNil {
    DPTrack *track = [DPTrack trackWithTitle:nil source:nil];
    
    XCTAssertNotNil(track);
    XCTAssertNil(track.title);
    XCTAssertNil(track.source);
}

#pragma mark - Basic Property Tests

- (void)testTitleProperty {
    self.track.title = @"Tenor Track";
    XCTAssertEqualObjects(self.track.title, @"Tenor Track");
    
    // Test various track names
    NSArray *titles = @[@"Lead", @"Tenor", @"Baritone", @"Bass", @"All Parts", @"Other 1"];
    for (NSString *title in titles) {
        self.track.title = title;
        XCTAssertEqualObjects(self.track.title, title);
    }
}

- (void)testSourceProperty {
    self.track.source = self.sampleSource;
    XCTAssertEqual(self.track.source, self.sampleSource);
    XCTAssertEqualObjects(self.track.source.uri.absoluteString, @"https://example.com/track.mp3");
    XCTAssertEqualObjects(self.track.source.type, @"mp3");
}

#pragma mark - Property Serialization Tests

- (void)testAllPropertiesRoundTrip {
    DPRemoteLocation *source = [[DPRemoteLocation alloc] init];
    source.uri = [NSURL URLWithString:@"https://cdn.example.com/audio/bass.m4a"];
    source.type = @"m4a";
    
    self.track.title = @"Bass";
    self.track.source = source;
    
    XCTAssertEqualObjects(self.track.title, @"Bass");
    XCTAssertNotNil(self.track.source);
    XCTAssertEqualObjects(self.track.source.uri.absoluteString, @"https://cdn.example.com/audio/bass.m4a");
    XCTAssertEqualObjects(self.track.source.type, @"m4a");
}

#pragma mark - Edge Case Tests

- (void)testTitleWithSpecialCharacters {
    self.track.title = @"Track with 'quotes'";
    XCTAssertEqualObjects(self.track.title, @"Track with 'quotes'");
    
    self.track.title = @"Track & More";
    XCTAssertEqualObjects(self.track.title, @"Track & More");
    
    self.track.title = @"Émojis 🎵🎶";
    XCTAssertEqualObjects(self.track.title, @"Émojis 🎵🎶");
}

- (void)testTitleWithWhitespace {
    // Leading/trailing whitespace
    self.track.title = @"  Padded Title  ";
    XCTAssertEqualObjects(self.track.title, @"  Padded Title  ");
    
    // Empty string
    self.track.title = @"";
    XCTAssertEqualObjects(self.track.title, @"");
    
    // Whitespace only
    self.track.title = @"   ";
    XCTAssertEqualObjects(self.track.title, @"   ");
}

- (void)testSourceWithVariousURLSchemes {
    // HTTPS
    DPRemoteLocation *httpsSource = [[DPRemoteLocation alloc] init];
    httpsSource.uri = [NSURL URLWithString:@"https://secure.example.com/track.mp3"];
    self.track.source = httpsSource;
    XCTAssertEqualObjects(self.track.source.uri.scheme, @"https");
    
    // HTTP
    DPRemoteLocation *httpSource = [[DPRemoteLocation alloc] init];
    httpSource.uri = [NSURL URLWithString:@"http://example.com/track.mp3"];
    self.track.source = httpSource;
    XCTAssertEqualObjects(self.track.source.uri.scheme, @"http");
}

- (void)testSourceWithVariousFileTypes {
    NSArray *fileTypes = @[@"mp3", @"m4a", @"wav", @"aac", @"ogg"];
    
    for (NSString *type in fileTypes) {
        DPRemoteLocation *source = [[DPRemoteLocation alloc] init];
        source.uri = [NSURL URLWithString:[NSString stringWithFormat:@"https://example.com/track.%@", type]];
        source.type = type;
        
        self.track.source = source;
        XCTAssertEqualObjects(self.track.source.type, type);
    }
}

#pragma mark - Multiple Instance Tests

- (void)testMultipleTrackInstances {
    DPRemoteLocation *source1 = [[DPRemoteLocation alloc] init];
    source1.uri = [NSURL URLWithString:@"https://example.com/lead.mp3"];
    
    DPRemoteLocation *source2 = [[DPRemoteLocation alloc] init];
    source2.uri = [NSURL URLWithString:@"https://example.com/tenor.mp3"];
    
    DPTrack *track1 = [DPTrack trackWithTitle:@"Lead" source:source1];
    DPTrack *track2 = [DPTrack trackWithTitle:@"Tenor" source:source2];
    
    // Verify instances are independent
    XCTAssertNotEqualObjects(track1.title, track2.title);
    XCTAssertNotEqual(track1.source, track2.source);
    
    // Modify one shouldn't affect the other
    track1.title = @"Modified Lead";
    XCTAssertEqualObjects(track2.title, @"Tenor");
}

- (void)testTrackPropertyOverwrite {
    DPRemoteLocation *source1 = [[DPRemoteLocation alloc] init];
    source1.uri = [NSURL URLWithString:@"https://example.com/original.mp3"];
    
    DPRemoteLocation *source2 = [[DPRemoteLocation alloc] init];
    source2.uri = [NSURL URLWithString:@"https://example.com/replacement.mp3"];
    
    self.track.title = @"Original";
    self.track.source = source1;
    
    // Overwrite
    self.track.title = @"Replacement";
    self.track.source = source2;
    
    XCTAssertEqualObjects(self.track.title, @"Replacement");
    XCTAssertEqual(self.track.source, source2);
}

#pragma mark - Standard Track Names Tests

- (void)testStandardBarbershopPartNames {
    NSArray *standardParts = @[@"All Parts", @"Tenor", @"Lead", @"Baritone", @"Bass"];
    
    for (NSString *part in standardParts) {
        DPTrack *track = [DPTrack trackWithTitle:part source:self.sampleSource];
        XCTAssertEqualObjects(track.title, part);
    }
}

- (void)testOtherPartNames {
    // Tags can have additional parts beyond the standard 4
    NSArray *otherParts = @[@"Other 1", @"Other 2", @"Other 3", @"Other 4"];
    
    for (NSString *part in otherParts) {
        DPTrack *track = [DPTrack trackWithTitle:part source:self.sampleSource];
        XCTAssertEqualObjects(track.title, part);
    }
}

#pragma mark - Source URL Path Tests

- (void)testSourceWithComplexURLPath {
    DPRemoteLocation *source = [[DPRemoteLocation alloc] init];
    source.uri = [NSURL URLWithString:@"https://cdn.barbershoptags.com/tracks/2024/tag-123/lead-track.mp3"];
    source.type = @"mp3";
    
    self.track.source = source;
    
    XCTAssertTrue([self.track.source.uri.path containsString:@"tracks"]);
    XCTAssertTrue([self.track.source.uri.path containsString:@"lead-track.mp3"]);
}

- (void)testSourceWithQueryParameters {
    DPRemoteLocation *source = [[DPRemoteLocation alloc] init];
    source.uri = [NSURL URLWithString:@"https://example.com/track.mp3?token=abc123&expires=999"];
    source.type = @"mp3";
    
    self.track.source = source;
    
    XCTAssertNotNil(self.track.source.uri.query);
    XCTAssertTrue([self.track.source.uri.query containsString:@"token"]);
}

#pragma mark - Init Tests

- (void)testBasicInit {
    DPTrack *track = [[DPTrack alloc] init];
    
    XCTAssertNotNil(track);
    XCTAssertNil(track.title);
    XCTAssertNil(track.source);
}

#pragma mark - Source Reference Tests

- (void)testSourceIsStrongReference {
    __weak DPRemoteLocation *weakSource;
    
    @autoreleasepool {
        DPRemoteLocation *source = [[DPRemoteLocation alloc] init];
        source.uri = [NSURL URLWithString:@"https://example.com/track.mp3"];
        weakSource = source;
        
        self.track.source = source;
        
        // Source should still exist because track holds strong reference
        XCTAssertNotNil(weakSource);
    }
    
    // Source should still exist after autorelease pool because track holds it
    XCTAssertNotNil(weakSource);
    XCTAssertNotNil(self.track.source);
}

@end
