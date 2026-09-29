//
//  Use this file to import your target's public headers that you would like to expose to Swift.
//

#import "DPPitchedSong.h"
#import "DPJsonSerializer.h"
#import "DPJsonPrimitive.h"
#import "DPPitchPipeModel.h"
#import "DPNote.h"
#import "DPNoteSound.h"
#import "DPAudioSynthesizer.h"
#import "DPKey.h"
#import "DPKeyType.h"
#import "DPAccidental.h"

/// A counter the audio render thread bumps and another thread reads
/// (MIDINotePlayer's EngineSampler.renderCount). Swift has no atomics before
/// iOS 18, so both sides go through these.
static inline void DPAtomicCounterIncrement(long *counter) {
    __atomic_fetch_add(counter, 1, __ATOMIC_RELEASE);
}

static inline long DPAtomicCounterLoad(long *counter) {
    return __atomic_load_n(counter, __ATOMIC_ACQUIRE);
}
