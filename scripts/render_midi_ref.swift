import AVFoundation
import Foundation

// Renders a multi-track .mid file offline using macOS's General MIDI DLS soundbank
// so Suno Audio-to-Audio / Start-from-Audio receives accurate melodic/harmonic audio seeds.

struct RenderArgs {
    let midPath: String
    let outPath: String
    let chordProgram: UInt8
    let melodyProgram: UInt8
}

func parseArgs() -> RenderArgs {
    let args = CommandLine.arguments
    guard args.count >= 3 else {
        fputs("Usage: render_midi_ref.swift <mid_path> <out_wav_path> [chord_program] [melody_program]\n", stderr)
        exit(2)
    }
    let chordProg = args.count >= 4 ? UInt8(clamping: Int(args[3]) ?? 48) : 48
    let melodyProg = args.count >= 5 ? UInt8(clamping: Int(args[4]) ?? 25) : 25
    return RenderArgs(midPath: args[1], outPath: args[2], chordProgram: chordProg, melodyProgram: melodyProg)
}

func run() throws {
    let cfg = parseArgs()
    let midURL = URL(fileURLWithPath: cfg.midPath)
    let outURL = URL(fileURLWithPath: cfg.outPath)
    let dlsURL = URL(fileURLWithPath: "/System/Library/Components/CoreAudio.component/Contents/Resources/gs_instruments.dls")

    let engine = AVAudioEngine()
    let chordSampler = AVAudioUnitSampler()
    let melodySampler = AVAudioUnitSampler()
    engine.attach(chordSampler)
    engine.attach(melodySampler)

    guard let stereoFormat = AVAudioFormat(standardFormatWithSampleRate: 44100, channels: 2) else {
        throw NSError(domain: "MidiRender", code: 1, userInfo: [NSLocalizedDescriptionKey: "Failed to allocate stereo format"])
    }

    // Main mixer node handles stereo summing cleanly.
    engine.connect(chordSampler, to: engine.mainMixerNode, format: nil)
    engine.connect(melodySampler, to: engine.mainMixerNode, format: nil)

    // Give melody slightly higher prominence than chord accompaniment.
    chordSampler.volume = 0.72
    melodySampler.volume = 1.0

    try engine.enableManualRenderingMode(.offline, format: stereoFormat, maximumFrameCount: 4096)
    try engine.start()

    try chordSampler.loadSoundBankInstrument(
        at: dlsURL,
        program: cfg.chordProgram,
        bankMSB: UInt8(kAUSampler_DefaultMelodicBankMSB),
        bankLSB: UInt8(kAUSampler_DefaultBankLSB)
    )
    try melodySampler.loadSoundBankInstrument(
        at: dlsURL,
        program: cfg.melodyProgram,
        bankMSB: UInt8(kAUSampler_DefaultMelodicBankMSB),
        bankLSB: UInt8(kAUSampler_DefaultBankLSB)
    )

    let sequencer = AVAudioSequencer(audioEngine: engine)
    try sequencer.load(from: midURL, options: [])

    let tracks = sequencer.tracks
    if tracks.count >= 2 {
        tracks[0].destinationAudioUnit = chordSampler
        tracks[1].destinationAudioUnit = melodySampler
    } else if tracks.count == 1 {
        tracks[0].destinationAudioUnit = melodySampler
    }

    sequencer.prepareToPlay()
    try sequencer.start()

    var maxSeconds = 0.0
    for t in tracks {
        if t.lengthInSeconds > maxSeconds {
            maxSeconds = t.lengthInSeconds
        }
    }
    let totalFrames = AVAudioFramePosition((maxSeconds + 1.2) * 44100.0)
    let outFile = try AVAudioFile(forWriting: outURL, settings: stereoFormat.settings)
    guard let buffer = AVAudioPCMBuffer(pcmFormat: engine.manualRenderingFormat, frameCapacity: 4096) else {
        throw NSError(domain: "MidiRender", code: 2, userInfo: [NSLocalizedDescriptionKey: "Failed to allocate PCM buffer"])
    }

    while engine.manualRenderingSampleTime < totalFrames {
        let remaining = totalFrames - engine.manualRenderingSampleTime
        let chunk = AVAudioFrameCount(min(4096, remaining))
        let status = try engine.renderOffline(chunk, to: buffer)
        switch status {
        case .success:
            try outFile.write(from: buffer)
        case .insufficientDataFromInputNode:
            break
        case .cannotDoInCurrentContext:
            usleep(1000)
        case .error:
            throw NSError(domain: "MidiRender", code: 3, userInfo: [NSLocalizedDescriptionKey: "Offline render failed"])
        @unknown default:
            throw NSError(domain: "MidiRender", code: 4, userInfo: [NSLocalizedDescriptionKey: "Unknown offline render status"])
        }
    }

    sequencer.stop()
    engine.stop()
}

try run()
