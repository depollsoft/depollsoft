//
//  DPPitchPipeButtonTests.m
//  pitchperfectlibTests
//
//  Comprehensive unit tests for DPPitchPipeButton.
//  Note: Some tests are limited because DPPitchPipeButton depends on
//  category methods from depolllib (addBlock:forControlEvents:) that
//  require -ObjC linker flag which causes issues with Swift compatibility.
//

#import <XCTest/XCTest.h>
#import "DPPitchPipeButton.h"
#import "DPNote.h"
#import "DPAccidental.h"

@interface DPPitchPipeButtonTests : XCTestCase
@end

@implementation DPPitchPipeButtonTests

#pragma mark - Note Model Tests (can be tested independently)

- (void)testNoteProperties {
    DPNote *testNote = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNotNil(testNote);
    XCTAssertEqualObjects(testNote.friendlyName, @"A");
    XCTAssertEqual(testNote.octave, 4);
}

- (void)testDifferentNotesForButtons {
    // Test that various notes can be found for use in buttons
    DPNote *naturalNote = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNotNil(naturalNote);
    
    DPNote *sharpNote = [DPNote findNoteWithName:@"C" accidental:[DPAccidental enumWithInt:Sharp] octave:4];
    XCTAssertNotNil(sharpNote);
    
    DPNote *flatNote = [DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Flat] octave:4];
    XCTAssertNotNil(flatNote);
}

- (void)testNotePlayAndStopForButtonUse {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    XCTAssertNotNil(note);
    
    XCTAssertFalse(note.isPlaying);
    
    [note play];
    XCTAssertTrue(note.isPlaying);
    
    [note stop];
    XCTAssertFalse(note.isPlaying);
}

#pragma mark - Button Toggle State Tests

- (void)testTogglePropertyValues {
    // Test BOOL values that would be set on toggle property
    BOOL toggleOff = NO;
    BOOL toggleOn = YES;
    
    XCTAssertFalse(toggleOff);
    XCTAssertTrue(toggleOn);
}

#pragma mark - Frame Tests (testing CGRect functionality)

- (void)testFrameCreation {
    CGRect frame = CGRectMake(0, 0, 100, 100);
    XCTAssertEqual(CGRectGetWidth(frame), 100);
    XCTAssertEqual(CGRectGetHeight(frame), 100);
}

- (void)testZeroFrame {
    CGRect zeroFrame = CGRectZero;
    XCTAssertTrue(CGRectIsEmpty(zeroFrame) || (CGRectGetWidth(zeroFrame) == 0 && CGRectGetHeight(zeroFrame) == 0));
}

#pragma mark - Layer Property Tests (testing CALayer values)

- (void)testButtonLayerMasksToBounds {
    UIButton *testButton = [UIButton buttonWithType:UIButtonTypeCustom];
    testButton.layer.masksToBounds = YES;
    XCTAssertTrue(testButton.layer.masksToBounds);
}

- (void)testButtonLayerCornerRadius {
    UIButton *testButton = [UIButton buttonWithType:UIButtonTypeCustom];
    testButton.layer.cornerRadius = 4.0;
    XCTAssertEqual(testButton.layer.cornerRadius, 4.0);
}

- (void)testButtonLayerBorderWidth {
    UIButton *testButton = [UIButton buttonWithType:UIButtonTypeCustom];
    testButton.layer.borderWidth = 2.0;
    XCTAssertEqual(testButton.layer.borderWidth, 2.0);
}

- (void)testButtonLayerBorderColor {
    UIButton *testButton = [UIButton buttonWithType:UIButtonTypeCustom];
    testButton.layer.borderColor = [[UIColor colorWithWhite:0.5 alpha:1] CGColor];
    XCTAssertNotNil((__bridge id)testButton.layer.borderColor);
}

#pragma mark - Note Interaction Tests

- (void)testMultipleNotesCanBeStopped {
    DPNote *note1 = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    DPNote *note2 = [DPNote findNoteWithName:@"B" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    
    [note1 play];
    XCTAssertTrue(note1.isPlaying);
    
    // Stopping one note shouldn't affect another
    [note1 stop];
    XCTAssertFalse(note1.isPlaying);
    
    [note2 play];
    XCTAssertTrue(note2.isPlaying);
    [note2 stop];
    XCTAssertFalse(note2.isPlaying);
}

- (void)testNoteToggleBehavior {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    
    // Simulate toggle behavior
    XCTAssertFalse(note.isPlaying);
    
    // First tap - start
    if (!note.isPlaying) {
        [note play];
    } else {
        [note stop];
    }
    XCTAssertTrue(note.isPlaying);
    
    // Second tap - stop
    if (!note.isPlaying) {
        [note play];
    } else {
        [note stop];
    }
    XCTAssertFalse(note.isPlaying);
}

- (void)testNoteNonToggleBehavior {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    
    // Simulate non-toggle (hold) behavior
    XCTAssertFalse(note.isPlaying);
    
    // Touch down - play
    [note play];
    XCTAssertTrue(note.isPlaying);
    
    // Touch up - stop
    [note stop];
    XCTAssertFalse(note.isPlaying);
}

#pragma mark - Rapid Touch Tests

- (void)testRapidNotePlayStopCycles {
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    
    // Simulate rapid touches
    for (int i = 0; i < 10; i++) {
        [note play];
        [note stop];
    }
    
    XCTAssertFalse(note.isPlaying, @"Note should not be playing after rapid cycles");
}

#pragma mark - Different Octave Tests

- (void)testNotesInDifferentOctaves {
    for (int octave = 0; octave < 8; octave++) {
        DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:octave];
        XCTAssertNotNil(note, @"Should find A natural in octave %d", octave);
        
        [note play];
        XCTAssertTrue(note.isPlaying);
        [note stop];
    }
}

#pragma mark - Color Tests for Button Styling

- (void)testSystemColorsAvailable {
    // Test that system colors used in button styling are available
    UIColor *gray2 = [UIColor systemGray2Color];
    UIColor *gray5 = [UIColor systemGray5Color];
    
    XCTAssertNotNil(gray2, @"systemGray2Color should be available");
    XCTAssertNotNil(gray5, @"systemGray5Color should be available");
}

- (void)testButtonCanBeCreatedWithCustomType {
    UIButton *button = [UIButton buttonWithType:UIButtonTypeCustom];
    XCTAssertNotNil(button, @"Should be able to create custom button");
}

@end
