
#import <XCTest/XCTest.h>
#import "DPAudioSynthesizer.h"
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


@end
