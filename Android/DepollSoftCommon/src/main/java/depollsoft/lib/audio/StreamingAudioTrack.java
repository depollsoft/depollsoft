package depollsoft.lib.audio;

import android.media.AudioFormat;
import android.media.AudioTrack;

import depollsoft.lib.util.Action;

public class StreamingAudioTrack extends AudioTrack {
  private class TrackWatcherThread extends Thread {
    private boolean keepGoing = true;

    @Override
    public void run() {
      while (this.keepGoing) {
        int playbackHeadPosition = StreamingAudioTrack.this
            .getPlaybackHeadPosition();
        do {
          int requestedAmount = StreamingAudioTrack.this.bufferFrameThreshold
              - (StreamingAudioTrack.this.writtenFrames - playbackHeadPosition);
          Action<Integer> filler = StreamingAudioTrack.this.bufferFiller;
          if (filler != null) {
            synchronized (filler) {
              filler.invoke(requestedAmount);
            }
          }
        } while (StreamingAudioTrack.this.writtenFrames - playbackHeadPosition < StreamingAudioTrack.this.bufferFrameThreshold);
        try {
          Thread.sleep(1);
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    public void stopRunning() {
      this.keepGoing = false;
    }
  }

  private static final int FADE_SETTLE_MILLIS = 40;

  private TrackWatcherThread trackWatcherThread;

  private int writtenFrames;
  private int numChannels;

  private int bytesPerSample;
  private int bufferFrameThreshold;
  private final int capacityFrames;
  private boolean primesBeforePlay;
  private Object threadLock = new Object();
  private Action<Integer> bufferFiller;

  public StreamingAudioTrack(int streamType, int sampleRateInHz,
      int channelConfig, int audioFormat, int bufferSizeInBytes, int mode) {
    this(streamType, sampleRateInHz, channelConfig, audioFormat,
        bufferSizeInBytes, mode, sampleRateInHz / 5);
  }

  public StreamingAudioTrack(int streamType, int sampleRateInHz,
      int channelConfig, int audioFormat, int bufferSizeInBytes, int mode,
      int bufferSampleThreshold) throws IllegalArgumentException {
    super(streamType, sampleRateInHz, channelConfig, audioFormat,
        bufferSizeInBytes, mode);
    this.numChannels = channelConfig == AudioFormat.CHANNEL_CONFIGURATION_STEREO ? 2
        : 1;
    this.bytesPerSample = audioFormat == AudioFormat.ENCODING_PCM_16BIT ? 2 : 1;
    this.bufferFrameThreshold = bufferSampleThreshold * this.numChannels;
    this.capacityFrames = bufferSizeInBytes / this.bytesPerSample / this.numChannels;
    this.trackWatcherThread = new TrackWatcherThread();
  }

  /**
   * Whether {@link #play()} fills the whole buffer before the track starts. A streaming track
   * doesn't start until its buffer holds its start threshold (the whole buffer by default), so
   * starting first and filling from the watcher thread races the start.
   */
  public void setPrimesBeforePlay(boolean value) {
    this.primesBeforePlay = value;
  }

  /** Frames written but not yet played. */
  public int getQueuedFrames() {
    return this.writtenFrames - this.getPlaybackHeadPosition();
  }

  /** The buffer's size in frames. */
  public int getCapacityFrames() {
    return this.capacityFrames;
  }

  /**
   * Fades the track out over {@code fadeMillis} with a raised-cosine volume curve, then pauses it
   * and discards whatever it had queued, so its next {@link #play()} starts from silence. The
   * buffer filler is stopped first and waited for, so it can't write after the flush; it must not
   * block, which a filler that only writes what {@link #getQueuedFrames()} leaves room for won't.
   */
  public void fadeOutAndReset(final int fadeMillis, final Action<Void> completionCallback) {
    final TrackWatcherThread watcher;
    synchronized (this.threadLock) {
      watcher = this.trackWatcherThread;
      if (watcher != null)
        watcher.stopRunning();
    }
    Thread t = new Thread() {
      @Override
      public void run() {
        int steps = Math.max(1, fadeMillis / 2);
        try {
          for (int i = 1; i <= steps; i++) {
            float volume = (float) (0.5 * (1 + Math.cos(Math.PI * i / steps)));
            StreamingAudioTrack.this.setStereoVolume(volume, volume);
            Thread.sleep(Math.max(1, fadeMillis / steps));
          }
          // The mixer applies a volume over its next period; let the last one land.
          Thread.sleep(FADE_SETTLE_MILLIS);
          if (watcher != null && watcher != Thread.currentThread())
            watcher.join(500);
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        StreamingAudioTrack.this.pause();
        StreamingAudioTrack.this.flush();
        StreamingAudioTrack.this.setStereoVolume(1, 1);
        completionCallback.invoke(null);
      }
    };
    t.start();
  }

  public void drainBuffer(final Action<Void> completionCallback) {
    this.trackWatcherThread.stopRunning();
    this.setStereoVolume(0, 0);
    Thread t = new Thread() {
      @Override
      public void run() {
        int prevHead;
        do {
          prevHead = StreamingAudioTrack.this.getPlaybackHeadPosition();
          try {
            Thread.sleep(100);
          }
          catch (InterruptedException e) {
            e.printStackTrace();
          }
        } while (StreamingAudioTrack.this.getPlaybackHeadPosition() > prevHead);
        StreamingAudioTrack.this.pause();
        StreamingAudioTrack.this.setStereoVolume(1, 1);
        completionCallback.invoke(null);
      }
    };
    t.start();
  }

  /** Frames written since the track was made or last flushed. */
  int getWrittenFrames() {
    return this.writtenFrames;
  }

  @Override
  public void flush() {
    super.flush();
    this.writtenFrames = 0;
  }

  @Override
  public void pause() throws IllegalStateException {
    synchronized (this.threadLock) {
      if (this.trackWatcherThread != null)
        this.trackWatcherThread.stopRunning();
      this.trackWatcherThread = null;
      super.pause();
    }
  }

  @Override
  public void play() throws IllegalStateException {
    synchronized (this.threadLock) {
      Action<Integer> filler = this.bufferFiller;
      if (this.primesBeforePlay && filler != null && this.getPlayState() != PLAYSTATE_PLAYING) {
        synchronized (filler) {
          filler.invoke(this.capacityFrames - this.getQueuedFrames());
        }
      }
      if (this.trackWatcherThread != null)
        this.trackWatcherThread.stopRunning();
      this.trackWatcherThread = new TrackWatcherThread();
      this.trackWatcherThread.start();
      super.play();
    }
  }

  public void setBufferFiller(Action<Integer> filler) {
    this.bufferFiller = filler;
  }

  @Override
  public void stop() throws IllegalStateException {
    synchronized (this.threadLock) {
      if (this.trackWatcherThread != null)
        this.trackWatcherThread.stopRunning();
      this.trackWatcherThread = null;
      super.stop();
    }
  }

  @Override
  public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
    this.writtenFrames += sizeInBytes / this.numChannels / this.bytesPerSample;
    return super.write(audioData, offsetInBytes, sizeInBytes);
  }

  @Override
  public int write(short[] audioData, int offsetInShorts, int sizeInShorts) {
    this.writtenFrames += sizeInShorts * 2 / this.numChannels
        / this.bytesPerSample;
    return super.write(audioData, offsetInShorts, sizeInShorts);
  }
}
