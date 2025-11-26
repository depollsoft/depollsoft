// Minimal compatibility macros to map OCUnit STAssert* to XCTest
#import <XCTest/XCTest.h>

#define STAssertTrue(cond, desc, ...) XCTAssertTrue((cond), desc)
#define STAssertFalse(cond, desc, ...) XCTAssertFalse((cond), desc)
#define STAssertNil(obj, desc, ...) XCTAssertNil((obj), desc)
#define STAssertNotNil(obj, desc, ...) XCTAssertNotNil((obj), desc)
#define STAssertEqualObjects(a, b, desc, ...) XCTAssertEqualObjects((a), (b), desc)
#define STAssertEquals(a, b, desc, ...) XCTAssertEqual((a), (b))
#define STAssertEqualsWithAccuracy(a, b, acc, desc, ...) XCTAssertEqualWithAccuracy((a), (b), (acc))
#define STFail(desc, ...) XCTFail(desc)

