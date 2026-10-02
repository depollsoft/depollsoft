package depollsoft.pitchperfect.lib;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import depollsoft.lib.util.Action;

import depollsoft.lib.audio.StreamingAudioTrack;
import depollsoft.pitchperfect.lib.sound.WaveSource;

public class PitchAudioTrackGenerator {
  private static final double TwoPi = Math.PI * 2;

  /** The rate and buffer a track was made with: a drained track is only reused for a note that needs both. */
  private static final class TrackShape {
    final int sampleRate;
    final int channelConfig;
    final int bufferSizeInBytes;

    TrackShape(int sampleRate, int channelConfig, int bufferSizeInBytes) {
      this.sampleRate = sampleRate;
      this.channelConfig = channelConfig;
      this.bufferSizeInBytes = bufferSizeInBytes;
    }

    boolean matches(int sampleRate, int channelConfig, int bufferSizeInBytes) {
      return this.sampleRate == sampleRate && this.channelConfig == channelConfig
          && this.bufferSizeInBytes == bufferSizeInBytes;
    }
  }

  private static final Queue<StreamingAudioTrack> trackPool = new LinkedList<StreamingAudioTrack>();
  private static final Map<StreamingAudioTrack, TrackShape> trackShapes = new WeakHashMap<StreamingAudioTrack, TrackShape>();

  private static short getAmplitude(double frequency, double time,
      double scaleFactor) {
    int value = (int) (Math.sin(time * frequency
        * PitchAudioTrackGenerator.TwoPi)
        * scaleFactor * Short.MAX_VALUE * 3);
    if (value > Short.MAX_VALUE)
      return Short.MAX_VALUE;
    if (value < Short.MIN_VALUE)
      return Short.MIN_VALUE;
    return (short) value;
  }

  public static AudioTrack getPitchAudioTrack(final double frequency,
      final int sampleRate, final int channelConfig, final int bufferTime) {
    final int numChannels = (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO ? 2
        : 1);
    final int bufferSizeInBytes = Math.max(AudioTrack.getMinBufferSize(
        sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT), sampleRate
        * bufferTime / 1000);
    // final int bufferSizeInBytes = sampleRate * 16 / 400 * numChannels;
    final short[] samples = new short[bufferSizeInBytes / 2];
    final double sampleTimeDelta = 1.0 / sampleRate;
    final StreamingAudioTrack track = PitchAudioTrackGenerator.getTrack(
        channelConfig, sampleRate, bufferSizeInBytes);
    final Action<Integer> listener = new Action<Integer>() {
      private double curTime = 0;

      public void invoke(Integer value) {
        try {
          for (int x = 0; x < samples.length; x += numChannels) {
            short result = PitchAudioTrackGenerator.getAmplitude(frequency,
                this.curTime, 1);
            samples[x] = result;
            if (numChannels == 2)
              samples[x + 1] = result;
            this.curTime += sampleTimeDelta;
          }
          track.write(samples, 0, samples.length);
        }
        catch (Exception e) {
        }
      }
    };
    track.setBufferFiller(listener);
    return track;
  }

  /** How long a stopped wave fades out before its track is reset for reuse. */
  public static final int WAVE_FADE_MILLIS = 20;

  /** The fewest frames a wave writes at once, so the filler doesn't wake for a handful. */
  private static final int WAVE_MIN_WRITE_FRAMES = 1024;

  /**
   * A track that plays {@code source}'s wave. Unlike the pitch pipe's, it fills its whole buffer
   * before it starts, then writes only what the buffer has room for, so its filler never blocks
   * holding samples a later flush would let in.
   */
  public static AudioTrack getWaveAudioTrack(final WaveSource source,
      final int sampleRate, final int channelConfig, final int bufferTime) {
    final int numChannels = (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO ? 2
        : 1);
    final int bufferSizeInBytes = Math.max(AudioTrack.getMinBufferSize(
        sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT), sampleRate
        * bufferTime / 1000);
    final StreamingAudioTrack track = PitchAudioTrackGenerator.getTrack(
        channelConfig, sampleRate, bufferSizeInBytes);
    if (track.getPlayState() == AudioTrack.PLAYSTATE_PAUSED)
      track.flush();
    final short[] samples = new short[track.getCapacityFrames() * numChannels];
    track.setBufferFiller(new Action<Integer>() {
      public void invoke(Integer requested) {
        try {
          int room = track.getCapacityFrames() - track.getQueuedFrames();
          int frames = Math.min(room, Math.max(requested, WAVE_MIN_WRITE_FRAMES));
          if (requested <= 0 || frames <= 0)
            return;
          fillWave(source, samples, frames, numChannels);
          track.write(samples, 0, frames * numChannels);
        }
        catch (Exception e) {
        }
      }
    });
    track.setPrimesBeforePlay(true);
    return track;
  }

  /** Writes {@code frames} frames of {@code source} into {@code samples}, the same on every channel. */
  static void fillWave(WaveSource source, short[] samples, int frames, int numChannels) {
    for (int frame = 0; frame < frames; frame++) {
      short value = source.nextSample();
      for (int channel = 0; channel < numChannels; channel++)
        samples[frame * numChannels + channel] = value;
    }
  }

  /**
   * Stops a wave track: fades it out, discards what it had queued and returns it to the pool, so
   * the next note that takes it starts from silence.
   */
  public static void stopWave(AudioTrack track) {
    final StreamingAudioTrack realTrack = (StreamingAudioTrack) track;
    realTrack.fadeOutAndReset(WAVE_FADE_MILLIS, new Action<Void>() {
      public void invoke(Void parameter) {
        synchronized (PitchAudioTrackGenerator.trackPool) {
          PitchAudioTrackGenerator.trackPool.add(realTrack);
        }
      }
    });
  }

  private static StreamingAudioTrack getTrack(int channelConfig,
      int sampleRate, int bufferSizeInBytes) {
    synchronized (PitchAudioTrackGenerator.trackPool) {
      Iterator<StreamingAudioTrack> pooled = PitchAudioTrackGenerator.trackPool.iterator();
      while (pooled.hasNext()) {
        StreamingAudioTrack candidate = pooled.next();
        TrackShape shape = PitchAudioTrackGenerator.trackShapes.get(candidate);
        if (shape != null && shape.matches(sampleRate, channelConfig, bufferSizeInBytes)
            && candidate.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
          pooled.remove();
          candidate.setPlaybackRate(sampleRate);
          candidate.setStereoVolume(1, 1);
          return candidate;
        }
      }
      StreamingAudioTrack track = PitchAudioTrackGenerator.newTrack(channelConfig, sampleRate,
          bufferSizeInBytes);
      if (track.getState() != AudioTrack.STATE_INITIALIZED) {
        // The platform has run out of tracks for this app. The idle ones the pool keeps hold
        // some of them: let those go and ask once more. A track that still isn't made refuses
        // to play, and the note stays silent rather than crashing.
        track.release();
        for (StreamingAudioTrack idle : PitchAudioTrackGenerator.trackPool)
          idle.release();
        PitchAudioTrackGenerator.trackPool.clear();
        track = PitchAudioTrackGenerator.newTrack(channelConfig, sampleRate, bufferSizeInBytes);
      }
      PitchAudioTrackGenerator.trackShapes.put(track,
          new TrackShape(sampleRate, channelConfig, bufferSizeInBytes));
      return track;
    }
  }

  private static StreamingAudioTrack newTrack(int channelConfig, int sampleRate,
      int bufferSizeInBytes) {
    return new StreamingAudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
        AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes, AudioTrack.MODE_STREAM);
  }

  public static void stop(AudioTrack track) {
    final StreamingAudioTrack realTrack = (StreamingAudioTrack) track;
    realTrack.drainBuffer(new Action<Void>() {
      public void invoke(Void parameter) {
        synchronized (PitchAudioTrackGenerator.trackPool) {
          PitchAudioTrackGenerator.trackPool.add(realTrack);
        }
      }
    });
  }
}
