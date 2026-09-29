// Renders notes through AVAudioUnitSampler, as the iOS app plays them, for
// measure_instruments.py. Arguments: soundfont, output directory, then
// program:key pairs. Writes <dir>/<program>_<key>.f32 (mono float, 44.1 kHz).

import AVFoundation

let arguments = CommandLine.arguments
let soundfont = URL(fileURLWithPath: arguments[1])
let directory = arguments[2]
let seconds = 3.0

for pair in arguments.dropFirst(3) {
    let parts = pair.split(separator: ":").compactMap { UInt8($0) }
    let (program, key) = (parts[0], parts[1])
    let engine = AVAudioEngine()
    let sampler = AVAudioUnitSampler()
    engine.attach(sampler)
    engine.connect(sampler, to: engine.mainMixerNode, format: nil)
    let format = AVAudioFormat(standardFormatWithSampleRate: 44100, channels: 2)!
    try engine.enableManualRenderingMode(.offline, format: format, maximumFrameCount: 4096)
    try sampler.loadSoundBankInstrument(
        at: soundfont, program: program,
        bankMSB: UInt8(kAUSampler_DefaultMelodicBankMSB), bankLSB: 0)
    try engine.start()
    sampler.startNote(key, withVelocity: 100, onChannel: 0)
    var samples: [Float] = []
    let buffer = AVAudioPCMBuffer(pcmFormat: engine.manualRenderingFormat, frameCapacity: 4096)!
    while Double(samples.count) < seconds * 44100 {
        _ = try engine.renderOffline(4096, to: buffer)
        let left = buffer.floatChannelData![0], right = buffer.floatChannelData![1]
        for i in 0..<Int(buffer.frameLength) { samples.append((left[i] + right[i]) / 2) }
    }
    engine.stop()
    try samples.withUnsafeBufferPointer { Data(buffer: $0) }
        .write(to: URL(fileURLWithPath: "\(directory)/\(program)_\(key).f32"))
}
