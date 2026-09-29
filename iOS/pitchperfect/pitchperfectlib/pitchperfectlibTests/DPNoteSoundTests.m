//
//  DPNoteSoundTests.m
//  pitchperfectlibTests
//
//  The sound ids, the waves (docs/pitchperfect-note-sounds.md) and how DPNote
//  routes an instrument to the app's MIDI player.
//

#import <XCTest/XCTest.h>
#import "DPAccidental.h"
#import "DPAudioSynthesizer.h"
#import "DPNote.h"
#import "DPNoteSound.h"

@interface DPFakeInstrumentPlayer : NSObject <DPNoteInstrumentPlayer>
@property (nonatomic) NSMutableArray<NSString *> *log;
/// Whether startNote:sound: refuses, as a player with no engine would.
@property (nonatomic) BOOL refuses;
@end

@implementation DPFakeInstrumentPlayer
- (instancetype)init {
    if (self = [super init]) _log = [NSMutableArray array];
    return self;
}
- (id)startNote:(DPNote *)note sound:(NSString *)sound {
    if (self.refuses) return nil;
    NSString *token = [NSString stringWithFormat:@"%@ %d", sound, note.midiKey];
    [self.log addObject:[@"start " stringByAppendingString:token]];
    return token;
}
- (void)stopNote:(id)token {
    [self.log addObject:[@"stop " stringByAppendingString:token]];
}
@end

@interface DPNoteSoundTests : XCTestCase
@end

@implementation DPNoteSoundTests

- (void)tearDown {
    DPNote.sound = nil;
    DPNote.instrumentPlayer = nil;
    [super tearDown];
}

#pragma mark - Ids

- (void)testTheSoundsAreThePitchPipeThenSustainedThenWavesThenPluckedAndStruck {
    NSArray *sustained = @[@"organ", @"reedOrgan", @"accordion", @"harmonica", @"strings", @"choir",
                           @"trumpet", @"clarinet", @"flute"];
    NSArray *waves = @[@"sine", @"triangle", @"square", @"sawtooth"];
    NSArray *plucked = @[@"piano", @"electricPiano", @"harpsichord", @"vibraphone", @"guitar", @"harp"];
    XCTAssertEqualObjects([DPNoteSound sustainedInstruments], sustained);
    XCTAssertEqualObjects([DPNoteSound waves], waves);
    XCTAssertEqualObjects([DPNoteSound pluckedInstruments], plucked);
    XCTAssertEqualObjects([DPNoteSound instruments], [sustained arrayByAddingObjectsFromArray:plucked]);
    NSMutableArray *expected = [NSMutableArray arrayWithObject:@"pitchPipe"];
    [expected addObjectsFromArray:sustained];
    [expected addObjectsFromArray:waves];
    [expected addObjectsFromArray:plucked];
    XCTAssertEqualObjects([DPNoteSound allSounds], expected);
    XCTAssertEqual([DPNoteSound allSounds].count, 20u);
}

- (void)testInstrumentsCarryTheirGeneralMIDIPrograms {
    NSDictionary *expected = @{@"piano": @0, @"electricPiano": @4, @"harpsichord": @6, @"vibraphone": @11,
                               @"organ": @19, @"reedOrgan": @20, @"accordion": @21, @"harmonica": @22,
                               @"guitar": @24, @"harp": @46,
                               @"strings": @48, @"choir": @52, @"trumpet": @56, @"clarinet": @71, @"flute": @73};
    for (NSString *sound in [DPNoteSound instruments]) {
        XCTAssertEqual([DPNoteSound programForSound:sound], [expected[sound] intValue], @"%@", sound);
        XCTAssertTrue([DPNoteSound isInstrument:sound]);
        XCTAssertFalse([DPNoteSound isWave:sound]);
    }
    XCTAssertEqual([DPNoteSound programForSound:@"sine"], -1);
    XCTAssertEqual([DPNoteSound programForSound:@"pitchPipe"], -1);
    XCTAssertEqual([DPNoteSound programForSound:nil], -1);
}

- (void)testAnUnknownSoundReadsAsThePitchPipe {
    XCTAssertEqualObjects([DPNoteSound validated:@"theremin"], @"pitchPipe");
    XCTAssertEqualObjects([DPNoteSound validated:nil], @"pitchPipe");
    XCTAssertEqualObjects([DPNoteSound validated:@"choir"], @"choir");
    DPNote.sound = @"kazoo";
    XCTAssertEqualObjects(DPNote.sound, @"pitchPipe");
    DPNote.sound = @"square";
    XCTAssertEqualObjects(DPNote.sound, @"square");
    DPNote.sound = nil;
    XCTAssertEqualObjects(DPNote.sound, @"pitchPipe");
}

- (void)testAKeyComesFromTheStoredA440Frequency {
    XCTAssertEqual([DPNoteSound keyForA440Frequency:440], 69);
    XCTAssertEqual([DPNoteSound keyForA440Frequency:261.6255653], 60);
    XCTAssertEqual([DPNoteSound keyForA440Frequency:32.70319566], 24);
    XCTAssertEqual([DPNoteSound keyForA440Frequency:16.35159783], 12, @"octave 0 is not clamped here");
    DPNote *c4 = [DPNote C4];
    XCTAssertEqual(c4.midiKey, 60);
    DPNote.referencePitch = 415;
    XCTAssertEqual(c4.midiKey, 60, @"the tuning doesn't move the key");
    DPNote.referencePitch = 440;
}

#pragma mark - Waves

- (float *)render:(NSString *)sound frequency:(double)frequency count:(NSUInteger)count {
    DPWaveShape shape;
    XCTAssertTrue(DPWaveShapeForSound(sound, &shape));
    DPWaveState state = DPWaveStateMake(shape, frequency, 44100);
    float *buffer = malloc(sizeof(float) * count);
    // In two pieces, as the audio thread asks for it.
    DPWaveRender(&state, buffer, 333);
    DPWaveRender(&state, buffer + 333, count - 333);
    return buffer;
}

- (void)testOnlyWavesHaveAShape {
    DPWaveShape shape;
    XCTAssertFalse(DPWaveShapeForSound(@"pitchPipe", &shape));
    XCTAssertFalse(DPWaveShapeForSound(@"piano", &shape));
    XCTAssertFalse(DPWaveShapeForSound(nil, &shape));
    XCTAssertTrue(DPWaveShapeForSound(@"sawtooth", &shape));
    XCTAssertEqual(shape, DPWaveShapeSawtooth);
}

- (void)testTheSineRampsInOver5msThenHoldsItsLevel {
    // 441 Hz: a hundred samples a cycle.
    float *sine = [self render:@"sine" frequency:441 count:1000];
    XCTAssertEqual(sine[0], 0);
    XCTAssertEqualWithAccuracy(sine[25], 0.89 * 25 / 220.0, 1e-6, @"a quarter cycle in, ramped");
    XCTAssertEqualWithAccuracy(sine[125], 0.89 * 125 / 220.0, 1e-6);
    XCTAssertEqualWithAccuracy(sine[325], 0.89, 1e-6, @"full level, -1 dBFS");
    XCTAssertEqualWithAccuracy(sine[375], -0.89, 1e-6);
    free(sine);
}

- (void)testAReleasedWaveRampsOutOver5msThenStaysSilent {
    DPWaveShape shape;
    DPWaveShapeForSound(@"square", &shape);
    DPWaveState state = DPWaveStateMake(shape, 441, 44100);
    float held[520]; // mid-cycle, away from the square's edge
    DPWaveRender(&state, held, 520);
    XCTAssertFalse(DPWaveIsSilent(&state));
    DPWaveRelease(&state);
    DPWaveRelease(&state); // a second release doesn't restart the ramp
    float out[300];
    DPWaveRender(&state, out, 300);
    for (int i = 0; i < 220; i++) {
        double limit = 0.89 * 1.2 * (1 - i / 220.0) + 1e-6; // PolyBLEP may overshoot a little
        XCTAssertLessThanOrEqual(fabsf(out[i]), limit, @"sample %d is within the closing ramp", i);
    }
    XCTAssertEqualWithAccuracy(fabsf(out[0]), 0.89, 0.2, @"the ramp starts from full level");
    for (int i = 220; i < 300; i++) {
        XCTAssertEqual(out[i], 0, @"silent after the ramp");
    }
    XCTAssertTrue(DPWaveIsSilent(&state));
    DPWaveState fresh = DPWaveStateMake(shape, 441, 44100);
    XCTAssertFalse(DPWaveIsSilent(&fresh), @"a new note starts held");
}

- (void)testTheTriangleStartsAtZeroRisingLikeTheSine {
    float *triangle = [self render:@"triangle" frequency:441 count:1000];
    XCTAssertEqualWithAccuracy(triangle[300], 0, 1e-6);
    XCTAssertEqualWithAccuracy(triangle[310], 0.89 * 0.4, 1e-6);
    XCTAssertEqualWithAccuracy(triangle[325], 0.89, 1e-6);
    XCTAssertEqualWithAccuracy(triangle[350], 0, 1e-6);
    XCTAssertEqualWithAccuracy(triangle[375], -0.89, 1e-6);
    free(triangle);
}

- (void)testTheSquareAndSawtoothSmoothTheirEdges {
    float *square = [self render:@"square" frequency:441 count:1000];
    XCTAssertEqualWithAccuracy(square[325], 0.89, 1e-6);
    XCTAssertEqualWithAccuracy(square[375], -0.89, 1e-6);
    // At an edge PolyBLEP lands on the midpoint, not a jump: 1 + polyBlep(0, dt) = 0.
    XCTAssertEqualWithAccuracy(square[300], 0, 1e-6);
    XCTAssertEqualWithAccuracy(square[350], 0, 1e-6, @"and the falling one");
    float *saw = [self render:@"sawtooth" frequency:441 count:1000];
    XCTAssertEqualWithAccuracy(saw[300], 0, 1e-6, @"the reset lands halfway");
    XCTAssertEqualWithAccuracy(saw[350], 0, 1e-6);
    XCTAssertEqualWithAccuracy(saw[375], 0.89 * 0.5, 1e-6);
    for (int i = 220; i < 1000; i++) {
        XCTAssertLessThanOrEqual(fabsf(square[i]), 0.89f + 1e-6f);
        XCTAssertLessThanOrEqual(fabsf(saw[i]), 0.89f + 1e-6f);
    }
    free(square);
    free(saw);
}

- (void)testPolyBLEPCutsTheSquaresAliasing {
    // B7's 11th harmonic (43.46 kHz) folds back to 638 Hz; the smoothed edges
    // leave much less of it there than a naive square does.
    double frequency = 3951.07;
    double alias = 44100 - 11 * frequency;
    NSUInteger count = 44100;
    float *square = [self render:@"square" frequency:frequency count:count];
    double naiveI = 0, naiveQ = 0, smoothI = 0, smoothQ = 0;
    for (NSUInteger n = 220; n < count; n++) {
        double p = fmod(n * frequency / 44100, 1.0);
        double naive = p < 0.5 ? 0.89 : -0.89;
        double w = 2 * M_PI * alias * n / 44100;
        naiveI += naive * sin(w);
        naiveQ += naive * cos(w);
        smoothI += square[n] * sin(w);
        smoothQ += square[n] * cos(w);
    }
    double naive = hypot(naiveI, naiveQ), smooth = hypot(smoothI, smoothQ);
    XCTAssertGreaterThan(naive, 1000, @"the naive square does alias there");
    XCTAssertLessThan(smooth, naive / 2);
    free(square);
}

- (void)testPolyBLEPFollowsTheContract {
    XCTAssertEqualWithAccuracy(DPPolyBlep(0, 0.1), -1, 1e-12);
    XCTAssertEqualWithAccuracy(DPPolyBlep(0.05, 0.1), 2 * 0.5 - 0.25 - 1, 1e-12);
    XCTAssertEqualWithAccuracy(DPPolyBlep(0.5, 0.1), 0, 1e-12);
    XCTAssertEqualWithAccuracy(DPPolyBlep(0.95, 0.1), 0.25 - 1 + 1, 1e-12);
}

#pragma mark - The synthesizer and DPNote

- (void)testEverySoundMakesASynthesizer {
    for (NSString *sound in [DPNoteSound allSounds]) {
        DPAudioSynthesizer *synth = [[DPAudioSynthesizer alloc] initWithFrequency:440 sampleRate:44100 sound:sound];
        XCTAssertNotNil(synth, @"%@", sound);
        [synth start];
        [synth stop];
    }
}

- (DPNote *)freshA4 {
    return [[DPNote alloc] initWithFriendlyName:@"A" octave:4 accidental:[DPAccidental enumWithInt:Natural] frequency:440];
}

- (void)testAnInstrumentPlaysThroughTheInstrumentPlayer {
    DPFakeInstrumentPlayer *player = [DPFakeInstrumentPlayer new];
    DPNote.instrumentPlayer = player;
    DPNote.sound = @"choir";
    DPNote *a4 = [self freshA4];
    [a4 play];
    XCTAssertTrue(a4.isPlaying);
    [a4 play];
    [a4 stop];
    XCTAssertFalse(a4.isPlaying);
    XCTAssertEqualObjects(player.log, (@[@"start choir 69", @"stop choir 69"]));
}

- (void)testASoundingNoteKeepsItsVoiceUntilPlayedAgain {
    DPFakeInstrumentPlayer *player = [DPFakeInstrumentPlayer new];
    DPNote.instrumentPlayer = player;
    DPNote.sound = @"organ";
    DPNote *a4 = [self freshA4];
    [a4 play];
    DPNote.sound = @"sine";
    DPNote.instrumentPlayer = nil;
    [a4 stop];
    XCTAssertEqualObjects(player.log, (@[@"start organ 69", @"stop organ 69"]),
                          @"the player that started it stops it");
    [a4 play];
    [a4 stop];
    XCTAssertEqual(player.log.count, 2u, @"the wave plays in the synthesizer");
}

- (void)testWavesAndThePitchPipeNeverReachTheInstrumentPlayer {
    DPFakeInstrumentPlayer *player = [DPFakeInstrumentPlayer new];
    DPNote.instrumentPlayer = player;
    for (NSString *sound in [@[@"pitchPipe"] arrayByAddingObjectsFromArray:[DPNoteSound waves]]) {
        DPNote.sound = sound;
        DPNote *a4 = [self freshA4];
        [a4 play];
        XCTAssertTrue(a4.isPlaying);
        [a4 stop];
    }
    XCTAssertEqual(player.log.count, 0u);
}

- (void)testAnInstrumentWithNoPlayerSoundsInThePitchPipeVoice {
    DPNote.sound = @"piano";
    DPNote *a4 = [self freshA4];
    [a4 play];
    XCTAssertTrue(a4.isPlaying);
    [a4 stop];
    DPFakeInstrumentPlayer *player = [DPFakeInstrumentPlayer new];
    player.refuses = YES;
    DPNote.instrumentPlayer = player;
    [a4 play];
    XCTAssertTrue(a4.isPlaying, @"a player that can't play falls back too");
    [a4 stop];
    XCTAssertFalse(a4.isPlaying);
}

@end
