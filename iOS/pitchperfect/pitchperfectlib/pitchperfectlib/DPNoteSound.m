//
//  DPNoteSound.m
//  pitchperfectlib
//

#import "DPNoteSound.h"
#include <math.h>

NSString *const DPNoteSoundPitchPipe = @"pitchPipe";

@implementation DPNoteSound

+ (NSArray<NSString *> *)waves {
    return @[@"sine", @"triangle", @"square", @"sawtooth"];
}

+ (NSDictionary<NSString *, NSNumber *> *)programs {
    static NSDictionary *programs;
    static dispatch_once_t once;
    dispatch_once(&once, ^{
        programs = @{
            @"piano": @0, @"electricPiano": @4, @"harpsichord": @6, @"vibraphone": @11,
            @"organ": @19, @"accordion": @21, @"guitar": @24, @"harp": @46,
            @"strings": @48, @"choir": @52, @"trumpet": @56, @"clarinet": @71, @"flute": @73,
        };
    });
    return programs;
}

+ (NSArray<NSString *> *)instruments {
    return @[@"piano", @"electricPiano", @"harpsichord", @"vibraphone", @"organ", @"accordion",
             @"guitar", @"harp", @"strings", @"choir", @"trumpet", @"clarinet", @"flute"];
}

+ (NSArray<NSString *> *)allSounds {
    return [[@[DPNoteSoundPitchPipe] arrayByAddingObjectsFromArray:[self waves]]
            arrayByAddingObjectsFromArray:[self instruments]];
}

+ (BOOL)isWave:(NSString *)sound {
    return sound && [[self waves] containsObject:sound];
}

+ (BOOL)isInstrument:(NSString *)sound {
    return sound && [self programs][sound] != nil;
}

+ (BOOL)isKnown:(NSString *)sound {
    return [sound isEqualToString:DPNoteSoundPitchPipe] || [self isWave:sound] || [self isInstrument:sound];
}

+ (NSString *)validated:(NSString *)sound {
    return [self isKnown:sound] ? sound : DPNoteSoundPitchPipe;
}

+ (int)programForSound:(NSString *)sound {
    NSNumber *program = sound ? [self programs][sound] : nil;
    return program ? program.intValue : -1;
}

+ (int)keyForA440Frequency:(double)frequency {
    if (!(frequency > 0)) {
        return 69;
    }
    return (int)lround(69 + 12 * log2(frequency / 440.0));
}

@end

BOOL DPWaveShapeForSound(NSString *sound, DPWaveShape *shape) {
    NSUInteger index = sound ? [[DPNoteSound waves] indexOfObject:sound] : NSNotFound;
    if (index == NSNotFound) {
        return NO;
    }
    *shape = (DPWaveShape)index;
    return YES;
}

DPWaveState DPWaveStateMake(DPWaveShape shape, double frequency, double sampleRate) {
    DPWaveState state = { shape, 0, frequency / sampleRate, 0, -1 };
    return state;
}

double DPPolyBlep(double t, double dt) {
    if (t < dt) {
        double x = t / dt;
        return x + x - x * x - 1;
    }
    if (t > 1 - dt) {
        double x = (t - 1) / dt;
        return x * x + x + x + 1;
    }
    return 0;
}

static const double kWaveLevel = 0.89;
static const long kRampSamples = 220;

void DPWaveRender(DPWaveState *state, float *buffer, NSUInteger count) {
    double p = state->phase;
    double dt = state->step;
    for (NSUInteger i = 0; i < count; i++) {
        double value;
        switch (state->shape) {
            case DPWaveShapeSine:
                value = sin(2 * M_PI * p);
                break;
            case DPWaveShapeTriangle:
                value = 1 - 4 * fabs(fmod(p + 0.25, 1.0) - 0.5);
                break;
            case DPWaveShapeSquare:
                value = (p < 0.5 ? 1 : -1) + DPPolyBlep(p, dt) - DPPolyBlep(fmod(p + 0.5, 1.0), dt);
                break;
            case DPWaveShapeSawtooth:
                value = (2 * p - 1) - DPPolyBlep(p, dt);
                break;
        }
        double ramp = state->elapsed < kRampSamples ? (double)state->elapsed / kRampSamples : 1;
        if (state->released >= 0) {
            ramp *= state->released < kRampSamples ? 1 - (double)state->released / kRampSamples : 0;
            state->released++;
        }
        buffer[i] = (float)(kWaveLevel * value * ramp);
        state->elapsed++;
        p += dt;
        if (p >= 1) {
            p -= floor(p);
        }
    }
    state->phase = p;
}

void DPWaveRelease(DPWaveState *state) {
    if (state->released < 0) {
        state->released = 0;
    }
}

BOOL DPWaveIsSilent(const DPWaveState *state) {
    return state->released >= kRampSamples;
}
