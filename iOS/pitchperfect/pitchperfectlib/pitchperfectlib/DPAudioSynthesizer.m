//
//  DPAudioSynthesizer.m
//  pitchperfectlib
//
//  Created by David Poll on 6/1/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#include <AudioUnit/AudioUnit.h>
#include <stdatomic.h>
#import "DPAudioSynthesizer.h"
#import "DPNoteSound.h"

#define kOutputBus 0
#define kInputBus 1

@interface DPAudioSynthesizer ()

- (OSStatus)renderAudioWithFlags:(AudioUnitRenderActionFlags *)actionFlags
                       timeStamp:(const AudioTimeStamp *)timeStamp
                       busNumber:(UInt32)busNumber
                    numberFrames:(UInt32)numberFrames
                            data:(AudioBufferList *)data;

@property (nonatomic, readonly) AudioComponentInstance audioUnit;
@property (nonatomic, readonly) int sampleRate;
@property (nonatomic, readonly) double frequency;
@property (nonatomic, readonly) double time;
/// Whether `wave` is the voice; the pitch pipe otherwise.
@property (nonatomic, readonly) BOOL playsWave;

@end

@interface DPAudioSynthesizer () {
    DPWaveState wave;
    DPWaveShape waveShape;
    /// Whether the audio unit is running (it keeps running while a wave fades out).
    BOOL unitRunning;
    /// The latest start or stop for the render thread, which alone touches
    /// `wave` while the unit runs (DPWaveCommand). One word, so the render
    /// thread takes a command and the caller replaces it atomically: a stop
    /// that follows a restart is never lost.
    atomic_int pendingCommand;
    /// Set by the render thread once a released wave has faded out.
    atomic_int fadedOut;
    /// Bumped by every start, so a stop scheduled before it does nothing.
    NSUInteger generation;
}

@end

static atomic_long sRunningCount = 0;

/// Tests replace the audio unit's start and stop, so they can drive the render
/// callback themselves without the real output also pulling from it, and can
/// make a start fail. Nil: the real AudioOutputUnitStart/Stop.
static OSStatus (^sTestingUnitStart)(AudioUnit) = nil;
static OSStatus (^sTestingUnitStop)(AudioUnit) = nil;
/// Called on the main queue when the last running synthesizer stops.
static void (^sOnLastStopped)(void) = nil;

/// What the render thread should do to the wave next.
typedef NS_ENUM(int, DPWaveCommand) {
    DPWaveCommandNone = 0,
    DPWaveCommandRestart = 1,
    DPWaveCommandRelease = 2,
};

/// When a stopped wave's unit is first checked for the end of its fade-out
/// (882 samples, 20 ms at 44.1 kHz, from the next render), then how often
/// after that, and for how long at most.
static const double kWaveStopDelay = 0.03;
static const double kWaveStopPoll = 0.01;
static const int kWaveStopPolls = 50;

const double TWO_PI = M_PI * 2;

short amplitude(double frequency, double time, double scaleFactor) {
    double location = sin(time * frequency * TWO_PI);
    int value = (int)(location * SHRT_MAX * 3);
    if (value > SHRT_MAX) {
        return SHRT_MAX;
    }
    if (value < SHRT_MIN) {
        return SHRT_MIN;
    }
    return (short)value;
}

OSStatus renderAudio (void *inRefCon,
                      AudioUnitRenderActionFlags *ioActionFlags,
                      const AudioTimeStamp *inTimeStamp,
                      UInt32 inBusNumber,
                      UInt32 inNumberFrames,
                      AudioBufferList *ioData) {
    return [((DPAudioSynthesizer *)inRefCon) renderAudioWithFlags:ioActionFlags
                                                        timeStamp:inTimeStamp
                                                        busNumber:inBusNumber
                                                     numberFrames:inNumberFrames
                                                             data:ioData];
}

@implementation DPAudioSynthesizer

@synthesize audioUnit;
@synthesize sampleRate;
@synthesize frequency;
@synthesize isPlaying;
@synthesize time;
@synthesize playsWave;

- (UInt32)LPCMFlagsWithValidBitsPerChannel:(UInt32)validBitsPerChannel
                       totalBitsPerChannel:(UInt32)totalBitsPerChannel
                                   isFloat:(BOOL)isFloat
                               isBigEndian:(BOOL)isBigEndian
                          isNonInterleaved:(BOOL)isNonInterleaved {
    return (isFloat ? kAudioFormatFlagIsFloat : kAudioFormatFlagIsSignedInteger) |
    (isBigEndian ? ((UInt32)kAudioFormatFlagIsBigEndian) : 0) |
    ((!isFloat && (validBitsPerChannel == totalBitsPerChannel)) ? kAudioFormatFlagIsPacked : kAudioFormatFlagIsAlignedHigh) |
    (isNonInterleaved ? ((UInt32)kAudioFormatFlagIsNonInterleaved) : 0);
}

- (void)fillAudioFormat:(AudioStreamBasicDescription *)asbd
             sampleRate:(Float64)inSampleRate
       channelsPerFrame:(UInt32)channelsPerFrame
    validBitsPerChannel:(UInt32)validBitsPerChannel
    totalBitsPerChannel:(UInt32)totalBitsPerChannel
                isFloat:(BOOL)isFloat
            isBigEndian:(BOOL)isBigEndian
       isNonInterleaved:(BOOL)isNonInterleaved {
    asbd->mSampleRate = sampleRate;
    asbd->mFormatID = kAudioFormatLinearPCM;
    asbd->mFormatFlags = [self LPCMFlagsWithValidBitsPerChannel:validBitsPerChannel
                                            totalBitsPerChannel:totalBitsPerChannel
                                                        isFloat:isFloat
                                                    isBigEndian:isBigEndian
                                               isNonInterleaved:isNonInterleaved];
    asbd->mBytesPerPacket = (isNonInterleaved ? 1 : channelsPerFrame) * (totalBitsPerChannel / 8);
    asbd->mFramesPerPacket = 1;
    asbd->mBytesPerFrame = (isNonInterleaved ? 1 : channelsPerFrame) * (totalBitsPerChannel / 8);
    asbd->mChannelsPerFrame = channelsPerFrame;
    asbd->mBitsPerChannel = validBitsPerChannel;
}

- (void)setup {    
    // Describe audio component
    AudioComponentDescription desc;
    desc.componentType = kAudioUnitType_Output;
    desc.componentSubType = kAudioUnitSubType_RemoteIO;
    desc.componentFlags = 0;
    desc.componentFlagsMask = 0;
    desc.componentManufacturer = kAudioUnitManufacturer_Apple;
    
    // Get component
    AudioComponent inputComponent = AudioComponentFindNext(NULL, &desc);
    
    // Get audio units
    AudioComponentInstanceNew(inputComponent, &audioUnit);
    
    UInt32 flag = 1;
    // Enable IO for playback
    AudioUnitSetProperty(audioUnit,
                                  kAudioOutputUnitProperty_EnableIO,
                                  kAudioUnitScope_Output,
                                  kOutputBus,
                                  &flag,
                                  sizeof(flag));
    
    // Describe format
    AudioStreamBasicDescription audioFormat;
    [self fillAudioFormat:&audioFormat
               sampleRate:sampleRate
         channelsPerFrame:2
      validBitsPerChannel:16
      totalBitsPerChannel:16
                  isFloat:NO
              isBigEndian:NO
         isNonInterleaved:NO];
    
    // Apply format
    AudioUnitSetProperty(audioUnit,
                                  kAudioUnitProperty_StreamFormat,
                                  kAudioUnitScope_Input,
                                  kOutputBus,
                                  &audioFormat,
                                  sizeof(audioFormat));
    
    // Set output callback
    AURenderCallbackStruct callbackStruct;
    callbackStruct.inputProc = renderAudio;
    callbackStruct.inputProcRefCon = self;
    AudioUnitSetProperty(audioUnit,
                                  kAudioUnitProperty_SetRenderCallback,
                                  kAudioUnitScope_Global,
                                  kOutputBus,
                                  &callbackStruct,
                                  sizeof(callbackStruct));
    
    // Initialize
    AudioUnitInitialize(audioUnit);
}

- (id)initWithFrequency:(double)newFrequency
             sampleRate:(int)newSampleRate {
    if (self = [super init]) {
        frequency = newFrequency;
        sampleRate = newSampleRate;
        [self setup];
    }
    return self;
}

- (id)initWithFrequency:(double)newFrequency
             sampleRate:(int)newSampleRate
                  sound:(NSString *)sound {
    if (self = [self initWithFrequency:newFrequency sampleRate:newSampleRate]) {
        playsWave = DPWaveShapeForSound(sound, &waveShape);
    }
    return self;
}

+ (void)setOnLastStopped:(void (^)(void))block {
    void (^copied)(void) = [block copy];
    [sOnLastStopped release];
    sOnLastStopped = copied;
}

+ (void)setTestingUnitStart:(OSStatus (^)(AudioUnit))start stop:(OSStatus (^)(AudioUnit))stop {
    [sTestingUnitStart release];
    [sTestingUnitStop release];
    sTestingUnitStart = [start copy];
    sTestingUnitStop = [stop copy];
}

+ (NSInteger)runningCount {
    return atomic_load(&sRunningCount);
}

/// Starts the audio unit; it counts as running only if it really started.
- (BOOL)startUnit {
    if (unitRunning) {
        return YES;
    }
    OSStatus status = sTestingUnitStart ? sTestingUnitStart(audioUnit) : AudioOutputUnitStart(audioUnit);
    if (status != noErr) {
        NSLog(@"DPAudioSynthesizer: AudioOutputUnitStart failed: %d", (int)status);
        return NO;
    }
    unitRunning = YES;
    atomic_fetch_add(&sRunningCount, 1);
    return YES;
}

- (void)stopUnit {
    if (unitRunning) {
        if (sTestingUnitStop) {
            sTestingUnitStop(audioUnit);
        } else {
            AudioOutputUnitStop(audioUnit);
        }
        unitRunning = NO;
        if (atomic_fetch_sub(&sRunningCount, 1) == 1) {
            void (^onLastStopped)(void) = [[sOnLastStopped retain] autorelease];
            if (onLastStopped) {
                dispatch_async(dispatch_get_main_queue(), onLastStopped);
            }
        }
    }
}

- (void)start {
    if (isPlaying) {
        return;
    }
    generation++;
    if (unitRunning) {
        // Still fading out: the render thread starts the wave again.
        atomic_store(&pendingCommand, DPWaveCommandRestart);
    } else {
        // A wave starts again from its beginning, fade-in and all. A command
        // the render thread never took (the unit stopped first, say for an
        // interruption) is stale now.
        atomic_store(&pendingCommand, DPWaveCommandNone);
        wave = DPWaveStateMake(waveShape, frequency, sampleRate);
    }
    atomic_store(&fadedOut, 0);
    time = 0;
    isPlaying = [self startUnit];
}

- (void)stop {
    if (!isPlaying) {
        return;
    }
    isPlaying = NO;
    if (!playsWave) {
        [self stopUnit];
        return;
    }
    atomic_store(&pendingCommand, DPWaveCommandRelease);
    [self stopUnitAfterFadeOut:generation delay:kWaveStopDelay polls:kWaveStopPolls];
}

/// Stops the unit once the render thread has played the fade-out, unless the
/// note started again meanwhile. The block keeps the synthesizer alive.
- (void)stopUnitAfterFadeOut:(NSUInteger)stopping delay:(double)delay polls:(int)polls {
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delay * NSEC_PER_SEC)),
                   dispatch_get_main_queue(), ^{
        if (self->generation != stopping || self->isPlaying) {
            return;
        }
        if (atomic_load(&self->fadedOut) || polls <= 0) {
            [self stopUnit];
        } else {
            [self stopUnitAfterFadeOut:stopping delay:kWaveStopPoll polls:polls - 1];
        }
    });
}

- (OSStatus)renderAudioWithFlags:(AudioUnitRenderActionFlags *)actionFlags
                       timeStamp:(const AudioTimeStamp *)timeStamp
                       busNumber:(UInt32)busNumber
                    numberFrames:(UInt32)numberFrames
                            data:(AudioBufferList *)data {
    if (playsWave) {
        return [self renderWaveInto:data];
    }
    // double time = timeStamp->mSampleTime;
    double timeDelta = 1.0 / sampleRate;
    for (UInt32 i = 0; i < data->mNumberBuffers; i++) {
        int numSamples = data->mBuffers[i].mDataByteSize / 4;
        UInt32 *buffer = data->mBuffers[i].mData;
        for (UInt32 n = 0; n < numSamples; n++) {
            UInt32 sample = 0;
            UInt32 channelAmplitude = amplitude(frequency, time + timeDelta * n, 1.0);
            sample = channelAmplitude | (channelAmplitude << 16);
            buffer[n] = sample;
        }
        time += timeDelta * numSamples;
    }
    return errno;
}

/// A wave's samples, in both channels.
- (OSStatus)renderWaveInto:(AudioBufferList *)data {
    switch (atomic_exchange(&pendingCommand, DPWaveCommandNone)) {
        case DPWaveCommandRestart:
            wave = DPWaveStateMake(waveShape, frequency, sampleRate);
            break;
        case DPWaveCommandRelease:
            DPWaveRelease(&wave);
            break;
        default:
            break;
    }
    float scratch[512];
    for (UInt32 i = 0; i < data->mNumberBuffers; i++) {
        UInt32 numSamples = data->mBuffers[i].mDataByteSize / 4;
        UInt32 *buffer = data->mBuffers[i].mData;
        UInt32 done = 0;
        while (done < numSamples) {
            UInt32 chunk = MIN(numSamples - done, (UInt32)(sizeof(scratch) / sizeof(float)));
            DPWaveRender(&wave, scratch, chunk);
            for (UInt32 n = 0; n < chunk; n++) {
                long value = lrintf(scratch[n] * SHRT_MAX);
                if (value > SHRT_MAX) value = SHRT_MAX;
                if (value < SHRT_MIN) value = SHRT_MIN;
                UInt32 channel = (UInt16)(short)value;
                buffer[done + n] = channel | (channel << 16);
            }
            done += chunk;
        }
    }
    if (DPWaveIsSilent(&wave)) {
        atomic_store(&fadedOut, 1);
    }
    return noErr;
}

- (void)dealloc {
    isPlaying = NO;
    [self stopUnit];
    [super dealloc];
}

@end
