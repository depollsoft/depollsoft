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

/// Every sound in the picker's order: the default and original voice, the
/// instruments that sustain on their own, the waves, then the plucked and
/// struck instruments.
+ (NSArray<NSString *> *)allSounds;
+ (NSArray<NSString *> *)waves;
/// Every instrument: the sustained ones, then the plucked and struck ones.
+ (NSArray<NSString *> *)instruments;
+ (NSArray<NSString *> *)sustainedInstruments;
+ (NSArray<NSString *> *)pluckedInstruments;
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
    /// Samples since the note started, for the fade-in.
    long elapsed;
    /// Samples since the note was released, for the fade-out; -1 while held.
    long released;
} DPWaveState;

/// The shape for a wave id; NO for anything else.
BOOL DPWaveShapeForSound(NSString *_Nullable sound, DPWaveShape *shape);

/// A wave starting from its beginning.
DPWaveState DPWaveStateMake(DPWaveShape shape, double frequency, double sampleRate);

/// The next `count` samples, in [-1, 1]: 0.89 of full scale, faded in over
/// the first 882 samples (20 ms) on a raised cosine, square and sawtooth
/// band-limited with PolyBLEP. After DPWaveRelease it fades out the same way
/// over 882 samples, then stays silent.
void DPWaveRender(DPWaveState *state, float *buffer, NSUInteger count);

/// Starts the wave's fade-out, so stopping it doesn't click.
void DPWaveRelease(DPWaveState *state);

/// Whether a released wave has finished its fade-out.
BOOL DPWaveIsSilent(const DPWaveState *state);

/// PolyBLEP's correction at phase `t` for a step of `dt`.
double DPPolyBlep(double t, double dt);

NS_ASSUME_NONNULL_END
