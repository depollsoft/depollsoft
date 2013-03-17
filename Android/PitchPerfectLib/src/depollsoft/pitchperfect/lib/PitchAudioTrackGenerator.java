package depollsoft.pitchperfect.lib;

import java.util.LinkedList;
import java.util.Queue;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.bindroid.utils.Action;

import depollsoft.lib.audio.StreamingAudioTrack;

public class PitchAudioTrackGenerator {
  private static final double TwoPi = Math.PI * 2;
  private static Queue<StreamingAudioTrack> trackPool;

  static {
    PitchAudioTrackGenerator.trackPool = new LinkedList<StreamingAudioTrack>();
  }

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

  private static StreamingAudioTrack getTrack(int channelConfig,
      int sampleRate, int bufferSizeInBytes) {
    synchronized (PitchAudioTrackGenerator.trackPool) {
      if (!PitchAudioTrackGenerator.trackPool.isEmpty()
          && PitchAudioTrackGenerator.trackPool.peek().getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
        StreamingAudioTrack track = PitchAudioTrackGenerator.trackPool.poll();
        track.setPlaybackRate(sampleRate);
        track.setStereoVolume(1, 1);
        return track;
      }
      else {
        return new StreamingAudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
            channelConfig, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes,
            AudioTrack.MODE_STREAM);
      }
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
