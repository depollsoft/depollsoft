
#import <XCTest/XCTest.h>
#import "DPAccidental.h"
#import "DPKeyType.h"

@interface DPEnumImplTests : XCTestCase
@end

@implementation DPEnumImplTests

- (void)testAccidentalEnum {
    DPAccidental *flat = [DPAccidental enumWithInt:Flat];
    XCTAssertEqual(flat.value, Flat);
    XCTAssertEqualObjects([flat description], @"Flat");
    
    DPAccidental *natural = [DPAccidental enumWithInt:Natural];
    XCTAssertEqual(natural.value, Natural);
    XCTAssertEqualObjects([natural description], @"Natural");
    
    DPAccidental *sharp = [DPAccidental enumWithInt:Sharp];
    XCTAssertEqual(sharp.value, Sharp);
    XCTAssertEqualObjects([sharp description], @"Sharp");
    
    // Test equality
    XCTAssertEqualObjects(flat, [DPAccidental enumWithInt:Flat]);
    XCTAssertNotEqualObjects(flat, sharp);
}

- (void)testKeyTypeEnum {
    DPKeyType *major = [DPKeyType enumWithInt:Major];
    XCTAssertEqual(major.value, Major);
    XCTAssertEqualObjects([major description], @"Major");
    
    DPKeyType *minor = [DPKeyType enumWithInt:Minor];
    XCTAssertEqual(minor.value, Minor);
    XCTAssertEqualObjects([minor description], @"Minor");
    
    XCTAssertEqualObjects(major, [DPKeyType enumWithInt:Major]);
    XCTAssertNotEqualObjects(major, minor);
}

@end
