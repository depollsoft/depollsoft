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

  /** A track that plays {@code source}'s wave, filled as the pitch pipe's track is. */
  public static AudioTrack getWaveAudioTrack(final WaveSource source,
      final int sampleRate, final int channelConfig, final int bufferTime) {
    final int numChannels = (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO ? 2
        : 1);
    final int bufferSizeInBytes = Math.max(AudioTrack.getMinBufferSize(
        sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT), sampleRate
        * bufferTime / 1000);
    final short[] samples = new short[bufferSizeInBytes / 2];
    final StreamingAudioTrack track = PitchAudioTrackGenerator.getTrack(
        channelConfig, sampleRate, bufferSizeInBytes);
    track.setBufferFiller(new Action<Integer>() {
      public void invoke(Integer value) {
        try {
          for (int x = 0; x < samples.length; x += numChannels) {
            short result = source.nextSample();
            samples[x] = result;
            if (numChannels == 2)
              samples[x + 1] = result;
          }
          track.write(samples, 0, samples.length);
        }
        catch (Exception e) {
        }
      }
    });
    return track;
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
      StreamingAudioTrack track = new StreamingAudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
          channelConfig, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes,
          AudioTrack.MODE_STREAM);
      PitchAudioTrackGenerator.trackShapes.put(track,
          new TrackShape(sampleRate, channelConfig, bufferSizeInBytes));
      return track;
    }
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
