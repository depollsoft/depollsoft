
#import <XCTest/XCTest.h>
#import "DPAudioSynthesizer.h"
#import "DPPitchPipeButton.h"
#import "DPNote.h"
#import "DPAccidental.h"

@interface DPPitchPipeComponentsTests : XCTestCase
@end

@implementation DPPitchPipeComponentsTests

- (void)testAudioSynthesizerLifecycle {
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    XCTAssertNotNil(synth);
    
    // We can't easily test audio output in simulator without a host app context sometimes, 
    // but we can call start/stop to ensure no crashes.
    [synth start];
    [synth stop];
}

- (void)testPitchPipeButtonInitialization {
    DPPitchPipeButton *btn = [[DPPitchPipeButton alloc] initWithFrame:CGRectMake(0, 0, 100, 100)];
    XCTAssertNotNil(btn);
    XCTAssertNotNil(btn.button);
    
    DPNote *note = [DPNote findNoteWithName:@"A" accidental:[DPAccidental enumWithInt:Natural] octave:4];
    btn.note = note;
    XCTAssertEqual(btn.note, note);
    
    btn.toggle = YES;
    XCTAssertTrue(btn.toggle);
    
    // Trigger layout
    [btn layoutSubviews];
    XCTAssertTrue(btn.button.layer.masksToBounds);
}

@end
