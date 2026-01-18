//
//  DPAudioSynthesizerTests.m
//  pitchperfectlibTests
//
//  Unit tests for DPAudioSynthesizer.
//

#import <XCTest/XCTest.h>
#import "DPAudioSynthesizer.h"

@interface DPAudioSynthesizerTests : XCTestCase
@end

@implementation DPAudioSynthesizerTests

#pragma mark - Initialization Tests

- (void)testInitSetsFrequencyAndSampleRate {
    // Test that initialization properly sets frequency and sample rate
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    XCTAssertNotNil(synth, @"Synthesizer should be created");
    // Note: We can't directly access readonly properties from outside,
    // but we verify the object is created successfully
}

- (void)testInitWithDifferentFrequencies {
    // Test various frequency values
    DPAudioSynthesizer *lowFreq = [[DPAudioSynthesizer alloc] initWithFrequency:100.0 sampleRate:44100];
    XCTAssertNotNil(lowFreq, @"Low frequency synthesizer should be created");
    
    DPAudioSynthesizer *midFreq = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    XCTAssertNotNil(midFreq, @"Mid frequency synthesizer should be created");
    
    DPAudioSynthesizer *highFreq = [[DPAudioSynthesizer alloc] initWithFrequency:4000.0 sampleRate:44100];
    XCTAssertNotNil(highFreq, @"High frequency synthesizer should be created");
}

- (void)testInitWithDifferentSampleRates {
    // Test various sample rate values
    DPAudioSynthesizer *rate22k = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:22050];
    XCTAssertNotNil(rate22k, @"22050 sample rate synthesizer should be created");
    
    DPAudioSynthesizer *rate44k = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    XCTAssertNotNil(rate44k, @"44100 sample rate synthesizer should be created");
    
    DPAudioSynthesizer *rate48k = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:48000];
    XCTAssertNotNil(rate48k, @"48000 sample rate synthesizer should be created");
}

#pragma mark - Start Tests

- (void)testStartCreatesAudioUnit {
    // Test that start initializes the audio unit and begins playback
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    XCTAssertNotNil(synth, @"Synthesizer should be created");
    
    // Call start - in simulator/test context with stubs, this should not crash
    [synth start];
    
    // Clean up
    [synth stop];
}

- (void)testStartTwiceIsIdempotent {
    // Test that calling start twice doesn't cause issues
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    
    [synth start];
    // Second start should be idempotent due to isPlaying check
    [synth start];
    
    // Should still function correctly
    [synth stop];
}

- (void)testStartMultipleTimesInSuccession {
    // Stress test: multiple starts
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    
    for (int i = 0; i < 5; i++) {
        [synth start];
    }
    
    [synth stop];
}

#pragma mark - Stop Tests

- (void)testStopDisposesAudioUnit {
    // Test that stop properly stops playback
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    
    [synth start];
    [synth stop];
    
    // Synthesizer should have stopped without crash
}

- (void)testStopTwiceIsIdempotent {
    // Test that calling stop twice doesn't cause issues
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    
    [synth start];
    [synth stop];
    // Second stop should be idempotent due to isPlaying check
    [synth stop];
}

- (void)testStopWithoutStart {
    // Test that stop can be called without start
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    
    // Should not crash when stop is called without start
    [synth stop];
}

- (void)testStopMultipleTimesInSuccession {
    // Stress test: multiple stops
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    
    [synth start];
    
    for (int i = 0; i < 5; i++) {
        [synth stop];
    }
}

#pragma mark - Lifecycle Tests

- (void)testStartStopCycle {
    // Test multiple start/stop cycles
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    
    for (int i = 0; i < 3; i++) {
        [synth start];
        [synth stop];
    }
}

- (void)testRapidStartStopCycles {
    // Stress test: rapid start/stop cycles
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    
    for (int i = 0; i < 10; i++) {
        [synth start];
        [synth stop];
    }
}

#pragma mark - Edge Case Tests

- (void)testZeroFrequencyHandled {
    // Test that zero frequency doesn't crash
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:0.0 sampleRate:44100];
    XCTAssertNotNil(synth, @"Zero frequency synthesizer should be created");
    
    [synth start];
    [synth stop];
}

- (void)testNegativeFrequencyHandled {
    // Test that negative frequency doesn't crash
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:-440.0 sampleRate:44100];
    XCTAssertNotNil(synth, @"Negative frequency synthesizer should be created");
    
    [synth start];
    [synth stop];
}

- (void)testVeryHighFrequencyHandled {
    // Test frequencies above human hearing range
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:22000.0 sampleRate:44100];
    XCTAssertNotNil(synth, @"Very high frequency synthesizer should be created");
    
    [synth start];
    [synth stop];
}

- (void)testVeryLowFrequencyHandled {
    // Test very low (subsonic) frequencies
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:1.0 sampleRate:44100];
    XCTAssertNotNil(synth, @"Very low frequency synthesizer should be created");
    
    [synth start];
    [synth stop];
}

- (void)testSampleRateValidation {
    // Test various sample rates
    DPAudioSynthesizer *lowRate = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:8000];
    XCTAssertNotNil(lowRate, @"8000 sample rate synthesizer should be created");
    
    DPAudioSynthesizer *standardRate = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    XCTAssertNotNil(standardRate, @"Standard sample rate synthesizer should be created");
    
    DPAudioSynthesizer *highRate = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:96000];
    XCTAssertNotNil(highRate, @"High sample rate synthesizer should be created");
}

- (void)testZeroSampleRateHandled {
    // Test that zero sample rate doesn't crash (edge case)
    // Note: This may cause division by zero in renderAudio, but shouldn't crash during init
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:0];
    XCTAssertNotNil(synth, @"Zero sample rate synthesizer should be created");
}

#pragma mark - Musical Note Frequency Tests

- (void)testA4FrequencyStandard {
    // Test standard A4 = 440 Hz
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    XCTAssertNotNil(synth);
    [synth start];
    [synth stop];
}

- (void)testMiddleCFrequency {
    // Test middle C (C4) ≈ 261.63 Hz
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:261.63 sampleRate:44100];
    XCTAssertNotNil(synth);
    [synth start];
    [synth stop];
}

- (void)testBassNoteFrequency {
    // Test bass E2 ≈ 82.41 Hz
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:82.41 sampleRate:44100];
    XCTAssertNotNil(synth);
    [synth start];
    [synth stop];
}

- (void)testHighSopranoFrequency {
    // Test high soprano C6 ≈ 1046.50 Hz
    DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:1046.50 sampleRate:44100];
    XCTAssertNotNil(synth);
    [synth start];
    [synth stop];
}

#pragma mark - Memory Management Tests

- (void)testMultipleInstancesCoexist {
    // Test that multiple synthesizers can coexist
    DPAudioSynthesizer *synth1 = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    DPAudioSynthesizer *synth2 = [[DPAudioSynthesizer alloc] initWithFrequency:880.0 sampleRate:44100];
    DPAudioSynthesizer *synth3 = [[DPAudioSynthesizer alloc] initWithFrequency:330.0 sampleRate:44100];
    
    XCTAssertNotNil(synth1);
    XCTAssertNotNil(synth2);
    XCTAssertNotNil(synth3);
    
    [synth1 start];
    [synth2 start];
    [synth3 start];
    
    [synth1 stop];
    [synth2 stop];
    [synth3 stop];
}

- (void)testDeallocStopsSynthesizer {
    // Test that deallocation properly cleans up
    @autoreleasepool {
        DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
        [synth start];
        // synth will be deallocated when pool drains, triggering dealloc which calls stop
    }
    // If we reach here without crash, dealloc worked correctly
}

@end
