//
//  DPNoteSound.h
//  pitchperfectlib
//
//  The sounds a note can play in (docs/pitchperfect-note-sounds.md): the
//  original pitch pipe, four waves generated here, and General MIDI
//  instruments the app plays through its MIDI synth.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

extern NSString *const DPNoteSoundPitchPipe;

/// The sound ids, stored and synced as they are.
@interface DPNoteSound : NSObject

/// The default and original voice, then the waves, then the instruments.
+ (NSArray<NSString *> *)allSounds;
+ (NSArray<NSString *> *)waves;
+ (NSArray<NSString *> *)instruments;
+ (BOOL)isKnown:(nullable NSString *)sound;
+ (BOOL)isWave:(nullable NSString *)sound;
+ (BOOL)isInstrument:(nullable NSString *)sound;
/// `sound` if this version knows it, otherwise the pitch pipe.
+ (NSString *)validated:(nullable NSString *)sound;
/// An instrument's General MIDI program; -1 for any other sound.
+ (int)programForSound:(nullable NSString *)sound;

/// The MIDI key of a note stored at `frequency` Hz at A440.
+ (int)keyForA440Frequency:(double)frequency;

@end

/// A wave's shape.
typedef NS_ENUM(int, DPWaveShape) {
    DPWaveShapeSine,
    DPWaveShapeTriangle,
    DPWaveShapeSquare,
    DPWaveShapeSawtooth,
};

/// One sounding wave: plain C so the audio thread never messages an object.
typedef struct {
    DPWaveShape shape;
    /// In [0, 1).
    double phase;
    /// Phase advanced per sample: frequency / sample rate.
    double step;
    /// Samples since the note started, for the opening ramp.
    long elapsed;
} DPWaveState;

/// The shape for a wave id; NO for anything else.
BOOL DPWaveShapeForSound(NSString *_Nullable sound, DPWaveShape *shape);

/// A wave starting from its beginning.
DPWaveState DPWaveStateMake(DPWaveShape shape, double frequency, double sampleRate);

/// The next `count` samples, in [-1, 1]: 0.89 of full scale, ramped in over
/// the first 220 samples, square and sawtooth band-limited with PolyBLEP.
void DPWaveRender(DPWaveState *state, float *buffer, NSUInteger count);

/// PolyBLEP's correction at phase `t` for a step of `dt`.
double DPPolyBlep(double t, double dt);

NS_ASSUME_NONNULL_END
