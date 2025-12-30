//
//  DPVideoModelTests.m
//  tagmasterTests
//
//  Comprehensive tests for DPVideo model class.
//

#import <XCTest/XCTest.h>
#import "DPVideo.h"

@interface DPVideoModelTests : XCTestCase
@property (nonatomic, strong) DPVideo *video;
@end

@implementation DPVideoModelTests

- (void)setUp {
    [super setUp];
    self.video = [[DPVideo alloc] init];
}

- (void)tearDown {
    self.video = nil;
    [super tearDown];
}

#pragma mark - Basic Property Tests

- (void)testVideoIdProperty {
    self.video.videoId = 42;
    XCTAssertEqual(self.video.videoId, 42);
    
    // Test different values
    self.video.videoId = 0;
    XCTAssertEqual(self.video.videoId, 0);
    
    self.video.videoId = INT_MAX;
    XCTAssertEqual(self.video.videoId, INT_MAX);
}

- (void)testDescriptionProperty {
    self.video.description = @"A test video description";
    XCTAssertEqualObjects(self.video.description, @"A test video description");
    
    // Test empty string
    self.video.description = @"";
    XCTAssertEqualObjects(self.video.description, @"");
    
    // Test nil
    self.video.description = nil;
    XCTAssertNil(self.video.description);
}

- (void)testSungKeyProperty {
    self.video.sungKey = @"Bb";
    XCTAssertEqualObjects(self.video.sungKey, @"Bb");
    
    // Test sharp
    self.video.sungKey = @"F#";
    XCTAssertEqualObjects(self.video.sungKey, @"F#");
    
    // Test natural
    self.video.sungKey = @"C";
    XCTAssertEqualObjects(self.video.sungKey, @"C");
    
    // Test Unicode sharp
    self.video.sungKey = @"C\u266F";
    XCTAssertEqualObjects(self.video.sungKey, @"C\u266F");
}

- (void)testIsMultitrackProperty {
    self.video.isMultitrack = YES;
    XCTAssertTrue(self.video.isMultitrack);
    
    self.video.isMultitrack = NO;
    XCTAssertFalse(self.video.isMultitrack);
}

- (void)testYouTubeCodeProperty {
    self.video.youTubeCode = @"dQw4w9WgXcQ";
    XCTAssertEqualObjects(self.video.youTubeCode, @"dQw4w9WgXcQ");
    
    // Test different code formats
    self.video.youTubeCode = @"ABC123";
    XCTAssertEqualObjects(self.video.youTubeCode, @"ABC123");
    
    // Test nil
    self.video.youTubeCode = nil;
    XCTAssertNil(self.video.youTubeCode);
}

- (void)testSungByProperty {
    self.video.sungBy = @"The Barbershop Quartet";
    XCTAssertEqualObjects(self.video.sungBy, @"The Barbershop Quartet");
    
    // Test with special characters
    self.video.sungBy = @"Quartet & Friends";
    XCTAssertEqualObjects(self.video.sungBy, @"Quartet & Friends");
}

- (void)testSungWebsiteProperty {
    NSURL *url = [NSURL URLWithString:@"https://example.com"];
    self.video.sungWebsite = url;
    XCTAssertEqualObjects(self.video.sungWebsite, url);
    
    // Test nil
    self.video.sungWebsite = nil;
    XCTAssertNil(self.video.sungWebsite);
}

- (void)testPostedProperty {
    NSDate *date = [NSDate dateWithTimeIntervalSince1970:1600000000];
    self.video.posted = date;
    XCTAssertEqualObjects(self.video.posted, date);
    
    // Test nil
    self.video.posted = nil;
    XCTAssertNil(self.video.posted);
}

#pragma mark - Property Serialization Tests

- (void)testAllPropertiesRoundTrip {
    // Set all properties
    self.video.videoId = 999;
    self.video.description = @"Full description";
    self.video.sungKey = @"G";
    self.video.isMultitrack = YES;
    self.video.youTubeCode = @"XYZ789";
    self.video.sungBy = @"Test Quartet";
    self.video.sungWebsite = [NSURL URLWithString:@"https://quartet.example.com"];
    self.video.posted = [NSDate dateWithTimeIntervalSince1970:1500000000];
    
    // Verify all properties
    XCTAssertEqual(self.video.videoId, 999);
    XCTAssertEqualObjects(self.video.description, @"Full description");
    XCTAssertEqualObjects(self.video.sungKey, @"G");
    XCTAssertTrue(self.video.isMultitrack);
    XCTAssertEqualObjects(self.video.youTubeCode, @"XYZ789");
    XCTAssertEqualObjects(self.video.sungBy, @"Test Quartet");
    XCTAssertEqualObjects(self.video.sungWebsite.absoluteString, @"https://quartet.example.com");
    XCTAssertNotNil(self.video.posted);
}

#pragma mark - Edge Case Tests

- (void)testVideoIdEdgeCases {
    // Negative values
    self.video.videoId = -1;
    XCTAssertEqual(self.video.videoId, -1);
    
    // Large values
    self.video.videoId = 999999999;
    XCTAssertEqual(self.video.videoId, 999999999);
}

- (void)testDescriptionWithSpecialCharacters {
    self.video.description = @"Video with 'quotes' and \"double quotes\"";
    XCTAssertEqualObjects(self.video.description, @"Video with 'quotes' and \"double quotes\"");
    
    // Unicode
    self.video.description = @"Video with émojis 🎵";
    XCTAssertEqualObjects(self.video.description, @"Video with émojis 🎵");
    
    // Newlines
    self.video.description = @"Line 1\nLine 2\nLine 3";
    XCTAssertTrue([self.video.description containsString:@"\n"]);
}

- (void)testSungKeyVariousFormats {
    // Flat keys
    NSArray *flatKeys = @[@"Bb", @"Eb", @"Ab", @"Db", @"Gb"];
    for (NSString *key in flatKeys) {
        self.video.sungKey = key;
        XCTAssertEqualObjects(self.video.sungKey, key);
    }
    
    // Sharp keys
    NSArray *sharpKeys = @[@"F#", @"C#", @"G#", @"D#", @"A#"];
    for (NSString *key in sharpKeys) {
        self.video.sungKey = key;
        XCTAssertEqualObjects(self.video.sungKey, key);
    }
    
    // Natural keys
    NSArray *naturalKeys = @[@"C", @"D", @"E", @"F", @"G", @"A", @"B"];
    for (NSString *key in naturalKeys) {
        self.video.sungKey = key;
        XCTAssertEqualObjects(self.video.sungKey, key);
    }
}

- (void)testYouTubeCodeVariousFormats {
    // Standard 11 character code
    self.video.youTubeCode = @"dQw4w9WgXcQ";
    XCTAssertEqual(self.video.youTubeCode.length, 11);
    
    // Short codes
    self.video.youTubeCode = @"ABC";
    XCTAssertEqualObjects(self.video.youTubeCode, @"ABC");
    
    // Codes with special characters
    self.video.youTubeCode = @"abc-123_XYZ";
    XCTAssertEqualObjects(self.video.youTubeCode, @"abc-123_XYZ");
}

- (void)testSungWebsiteVariousURLs {
    // HTTPS URL
    self.video.sungWebsite = [NSURL URLWithString:@"https://secure.example.com"];
    XCTAssertEqualObjects(self.video.sungWebsite.scheme, @"https");
    
    // HTTP URL
    self.video.sungWebsite = [NSURL URLWithString:@"http://example.com"];
    XCTAssertEqualObjects(self.video.sungWebsite.scheme, @"http");
    
    // URL with path
    self.video.sungWebsite = [NSURL URLWithString:@"https://example.com/path/to/page"];
    XCTAssertNotNil(self.video.sungWebsite.path);
    
    // URL with query string
    self.video.sungWebsite = [NSURL URLWithString:@"https://example.com?param=value"];
    XCTAssertNotNil(self.video.sungWebsite.query);
}

- (void)testPostedDateVariousValues {
    // Past date
    self.video.posted = [NSDate dateWithTimeIntervalSince1970:0];
    XCTAssertNotNil(self.video.posted);
    
    // Future date
    self.video.posted = [NSDate dateWithTimeIntervalSinceNow:86400 * 365]; // 1 year from now
    XCTAssertNotNil(self.video.posted);
    
    // Current date
    self.video.posted = [NSDate date];
    XCTAssertNotNil(self.video.posted);
}

#pragma mark - Multiple Instance Tests

- (void)testMultipleVideoInstances {
    DPVideo *video1 = [[DPVideo alloc] init];
    video1.videoId = 1;
    video1.sungBy = @"Quartet 1";
    
    DPVideo *video2 = [[DPVideo alloc] init];
    video2.videoId = 2;
    video2.sungBy = @"Quartet 2";
    
    // Verify instances are independent
    XCTAssertNotEqual(video1.videoId, video2.videoId);
    XCTAssertNotEqualObjects(video1.sungBy, video2.sungBy);
    
    // Modify one shouldn't affect the other
    video1.sungBy = @"Modified Quartet";
    XCTAssertEqualObjects(video2.sungBy, @"Quartet 2");
}

- (void)testVideoPropertyOverwrite {
    // Set initial values
    self.video.videoId = 100;
    self.video.sungBy = @"Original Quartet";
    
    // Overwrite
    self.video.videoId = 200;
    self.video.sungBy = @"New Quartet";
    
    // Verify new values
    XCTAssertEqual(self.video.videoId, 200);
    XCTAssertEqualObjects(self.video.sungBy, @"New Quartet");
}

@end
