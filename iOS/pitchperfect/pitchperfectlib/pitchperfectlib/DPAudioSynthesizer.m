//
//  DPAudioSynthesizer.m
//  pitchperfectlib
//
//  Created by David Poll on 6/1/12.
//  Copyright (c) 2012 DepollSoft. All rights reserved.
//

#include <AudioUnit/AudioUnit.h>
#import "DPAudioSynthesizer.h"

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
@property (readonly) BOOL isPlaying;

@end

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

- (void)start {
    if (isPlaying) {
        return;
    }
    isPlaying = YES;
    AudioOutputUnitStart(audioUnit);
    time = 0;
}

- (void)stop {
    if (!isPlaying) {
        return;
    }
    AudioOutputUnitStop(audioUnit);
    isPlaying = NO;
}

- (OSStatus)renderAudioWithFlags:(AudioUnitRenderActionFlags *)actionFlags
                       timeStamp:(const AudioTimeStamp *)timeStamp
                       busNumber:(UInt32)busNumber
                    numberFrames:(UInt32)numberFrames
                            data:(AudioBufferList *)data {
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

- (void)dealloc {
    [self stop];
    [super dealloc];
}

@end
