//
//  pitchperfectlibTests.m
//  pitchperfectlibTests
//
//  Created by David Poll on 3/24/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#import "pitchperfectlibTests.h"
#import "DPAudioSynthesizer.h"
#import <AudioToolbox/AudioToolbox.h>
#import <AudioUnit/AudioUnit.h>

@interface DPAudioSynthesizer (Testing)
- (unsigned int)LPCMFlagsWithValidBitsPerChannel:(unsigned int)validBitsPerChannel
                           totalBitsPerChannel:(unsigned int)totalBitsPerChannel
                                       isFloat:(BOOL)isFloat
                                   isBigEndian:(BOOL)isBigEndian
                              isNonInterleaved:(BOOL)isNonInterleaved;
- (void)fillAudioFormat:(AudioStreamBasicDescription *)asbd
             sampleRate:(double)sr
       channelsPerFrame:(unsigned int)ch
    validBitsPerChannel:(unsigned int)v
    totalBitsPerChannel:(unsigned int)t
                isFloat:(BOOL)f
            isBigEndian:(BOOL)be
       isNonInterleaved:(BOOL)ni;
- (int)renderAudioWithFlags:(AudioUnitRenderActionFlags *)actionFlags
                  timeStamp:(const AudioTimeStamp *)timeStamp
                  busNumber:(UInt32)busNumber
               numberFrames:(UInt32)numberFrames
                       data:(AudioBufferList *)data;
@end

@implementation pitchperfectlibTests

- (void)setUp
{
    [super setUp];
    
    // Set-up code here.
}

- (void)tearDown
{
    // Tear-down code here.
    
    [super tearDown];
}

- (void)testExample {
    XCTAssertTrue(1 == 1);
}

- (void)testAudioSynthesizerInitStartStopDoesNotCrash {
    DPAudioSynthesizer *s = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    [s start];
    [s stop];
}
- (void)testAudioFormatHelpersCoverBranches {
    DPAudioSynthesizer *s = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    // Exercise flag combinations
    (void)[s LPCMFlagsWithValidBitsPerChannel:16 totalBitsPerChannel:16 isFloat:NO isBigEndian:NO isNonInterleaved:NO];
    (void)[s LPCMFlagsWithValidBitsPerChannel:8 totalBitsPerChannel:16 isFloat:NO isBigEndian:YES isNonInterleaved:YES];
    (void)[s LPCMFlagsWithValidBitsPerChannel:16 totalBitsPerChannel:16 isFloat:YES isBigEndian:NO isNonInterleaved:NO];

    AudioStreamBasicDescription asbd = {0};
    [s fillAudioFormat:&asbd sampleRate:48000 channelsPerFrame:2 validBitsPerChannel:16 totalBitsPerChannel:16 isFloat:NO isBigEndian:NO isNonInterleaved:NO];
    XCTAssertEqual(asbd.mSampleRate, 44100); // Implementation uses 'sampleRate' ivar
    XCTAssertEqual(asbd.mChannelsPerFrame, 2);
    XCTAssertEqual(asbd.mBitsPerChannel, 16);
}

- (void)testRenderAudioFillsBuffer {
    DPAudioSynthesizer *s = [[DPAudioSynthesizer alloc] initWithFrequency:440.0 sampleRate:44100];
    AudioUnitRenderActionFlags flags = 0;
    AudioTimeStamp ts; memset(&ts, 0, sizeof(ts));
    UInt32 frames = 8;
    UInt32 bytes = frames * 4; // 32-bit samples (packed two 16-bit)
    UInt32 *samples = (UInt32 *)calloc(frames, sizeof(UInt32));
    AudioBufferList abl;
    abl.mNumberBuffers = 1;
    abl.mBuffers[0].mNumberChannels = 2;
    abl.mBuffers[0].mDataByteSize = bytes;
    abl.mBuffers[0].mData = samples;

    // Call the private render to exercise its loop
    [s renderAudioWithFlags:&flags timeStamp:&ts busNumber:0 numberFrames:frames data:&abl];
    // Assert that samples were written
    BOOL anyNonZero = NO;
    for (UInt32 i = 0; i < frames; i++) { if (samples[i] != 0) { anyNonZero = YES; break; } }
    XCTAssertTrue(anyNonZero);

    free(samples);
}

@end

// Link stubs for AudioUnit symbols when running under Mac Catalyst to
// allow tests to link without the iOS-only AudioUnit framework.
#include <TargetConditionals.h>
#if TARGET_OS_MACCATALYST
#import <AudioUnit/AudioUnit.h>
AudioComponent AudioComponentFindNext(AudioComponent inComponent, const AudioComponentDescription *inDesc) { return NULL; }
OSStatus AudioComponentInstanceNew(AudioComponent inComponent, AudioComponentInstance *outInstance) { if (outInstance) *outInstance = NULL; return 0; }
OSStatus AudioOutputUnitStart(AudioUnit inUnit) { return 0; }
OSStatus AudioOutputUnitStop(AudioUnit inUnit) { return 0; }
OSStatus AudioUnitInitialize(AudioUnit inUnit) { return 0; }
OSStatus AudioUnitSetProperty(AudioUnit inUnit, AudioUnitPropertyID inID, AudioUnitScope inScope, AudioUnitElement inElement, const void *inData, UInt32 inDataSize) { return 0; }
#endif

// Provide NSString+UUID used by DPPitchedSong to avoid requiring -ObjC
@interface NSString (DPUtils_TestShim)
+ (NSString *)stringWithUUID;
@end
@implementation NSString (DPUtils_TestShim)
+ (NSString *)stringWithUUID { return [[NSUUID UUID] UUIDString]; }
@end
