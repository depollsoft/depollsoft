// Test-only stubs to satisfy AudioUnit symbol references when linking under Mac Catalyst
// These stubs allow running unit tests for pitchperfectlib without actual audio output.
#import <Foundation/Foundation.h>
#include <TargetConditionals.h>

#if TARGET_OS_MACCATALYST
typedef const struct OpaqueAudioComponent* AudioComponent;
typedef struct OpaqueAudioComponentInstance* AudioComponentInstance;
typedef struct OpaqueAudioUnit* AudioUnit;
typedef uint32_t OSStatus;
typedef uint32_t AudioUnitPropertyID;
typedef uint32_t AudioUnitScope;
typedef uint32_t AudioUnitElement;

AudioComponent AudioComponentFindNext(AudioComponent inComponent, const void *inDesc) {
    return NULL;
}

OSStatus AudioComponentInstanceNew(AudioComponent inComponent, AudioComponentInstance *outInstance) {
    if (outInstance) { *outInstance = NULL; }
    return 0;
}

OSStatus AudioOutputUnitStart(AudioUnit inUnit) { return 0; }
OSStatus AudioOutputUnitStop(AudioUnit inUnit) { return 0; }
OSStatus AudioUnitInitialize(AudioUnit inUnit) { return 0; }

OSStatus AudioUnitSetProperty(AudioUnit inUnit,
                              AudioUnitPropertyID inID,
                              AudioUnitScope inScope,
                              AudioUnitElement inElement,
                              const void *inData,
                              uint32_t inDataSize) {
    return 0;
}
#endif

