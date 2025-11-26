//
//  DPTagXMLParserTests.m
//  tagmasterTests
//
//  Adds coverage for DPTagXMLParser by parsing a small,
//  representative XML payload that exercises tags, videos,
//  and remote location fields.
//

#import <XCTest/XCTest.h>
#import "DPTagXMLParser.h"
#import "DPTag.h"
#import "DPVideo.h"
#import "DPTagQueryResult.h"
#import "DPRemoteLocation.h"

@interface DPTagXMLParserTests : XCTestCase
@end

@implementation DPTagXMLParserTests

- (void)testParseSimpleXMLPayload {
    NSString *xml = @
    "<tags count=\"1\" available=\"1\">"
     "  <tag>"
     "    <id>42</id>"
     "    <Title>My Tag</Title>"
     "    <Parts>4</Parts>"
     "    <SheetMusic type=\"pdf\">https://example.com/file.pdf</SheetMusic>"
     "    <AllParts type=\"mp3\">https://example.com/all.mp3</AllParts>"
     "    <videos count=\"1\">"
     "      <video>"
     "        <id>7</id>"
     "        <SungKey>C&#9839;</SungKey>"
     "        <Desc>Demo</Desc>"
     "        <Multitrack>Yes</Multitrack>"
     "        <Code>XYZ</Code>"
     "        <SungBy>Choir</SungBy>"
     "        <SungWebsite>https://example.com</SungWebsite>"
     "        <Posted>Mon, 1 Jan 2018</Posted>"
     "      </video>"
     "    </videos>"
     "  </tag>"
     "</tags>";

    NSData *data = [xml dataUsingEncoding:NSUTF8StringEncoding];
    DPTagXMLParser *parser = [[DPTagXMLParser alloc] init];
    NSArray *result = [parser parseWithData:data];
    XCTAssertNotNil(result);
    XCTAssertTrue(result.count > 0);

    DPTagQueryResult *qr = (DPTagQueryResult *)result.lastObject;
    XCTAssertEqual(qr.count, 1);
    XCTAssertEqual(qr.available, 1);
    XCTAssertEqual(qr.tags.count, 1);

    DPTag *tag = (DPTag *)qr.tags.firstObject;
    XCTAssertEqual(tag.tagId, 42);
    XCTAssertEqualObjects(tag.title, @"My Tag");
    XCTAssertEqual(tag.parts, 4);

    // Remote locations mapped
    XCTAssertNotNil(tag.sheetMusicUri);
    XCTAssertEqualObjects(tag.sheetMusicUri.type, @"pdf");
    XCTAssertEqualObjects(tag.sheetMusicUri.uri.absoluteString, @"https://example.com/file.pdf");
    XCTAssertNotNil(tag.allPartsTrackUri);
    XCTAssertEqualObjects(tag.allPartsTrackUri.type, @"mp3");

    // Videos aggregated and fields parsed
    XCTAssertNotNil(tag.videos);
    XCTAssertEqual(tag.videos.count, 1);
    DPVideo *v = (DPVideo *)tag.videos.firstObject;
    XCTAssertEqual(v.videoId, 7);
    XCTAssertEqualObjects(v.description, @"Demo");
    XCTAssertEqualObjects(v.youTubeCode, @"XYZ");
    XCTAssertEqualObjects(v.sungBy, @"Choir");
    // HTML entities converted to Unicode sharp
    XCTAssertEqualObjects(v.sungKey, @"C\u266F");
    XCTAssertTrue(v.isMultitrack);
    XCTAssertNotNil(v.posted);
}

@end

