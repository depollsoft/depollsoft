package depollsoft.lib.audio;

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import depollsoft.lib.util.Action;

public class StreamingAudioTrack extends AudioTrack {
  private static final String TAG = "StreamingAudioTrack";

  /** The name of the thread that keeps a playing track's buffer full. */
  static final String WATCHER_THREAD_NAME = "StreamingAudioTrack watcher";

  private class TrackWatcherThread extends Thread {
    private volatile boolean keepGoing = true;

    TrackWatcherThread() {
      super(WATCHER_THREAD_NAME);
    }

    @Override
    public void run() {
      try {
        int lastHead = StreamingAudioTrack.this.getPlaybackHeadPosition();
        long lastMoved = System.nanoTime();
        boolean started = false;
        while (this.keepGoing) {
          int playbackHeadPosition = StreamingAudioTrack.this
              .getPlaybackHeadPosition();
          long now = System.nanoTime();
          if (playbackHeadPosition != lastHead) {
            started = true;
            lastHead = playbackHeadPosition;
            lastMoved = now;
          }
          // The platform holds a streaming track back until its whole buffer is full, and the
          // mixer drops one that runs dry until something writes to it again. A track restarted
          // before the server discarded what a flush left can come up a few frames short, and
          // filling only to the threshold would never send them. So until the head first moves,
          // and whenever it has stood still a while, fill the whole buffer.
          boolean stalled = !started || now - lastMoved >= STALL_NANOS;
          int target = stalled ? StreamingAudioTrack.this.capacityFrames
              : StreamingAudioTrack.this.bufferFrameThreshold;
          // Fill to the target unless stopped, and only while the filler gets something in: one
          // that can't write (its track is gone) would otherwise hold the watcher here forever.
          do {
            int before = StreamingAudioTrack.this.writtenFrames;
            int requestedAmount = target - (before - playbackHeadPosition);
            Action<Integer> filler = StreamingAudioTrack.this.bufferFiller;
            if (filler != null) {
              synchronized (filler) {
                filler.invoke(requestedAmount);
              }
            }
            if (StreamingAudioTrack.this.writtenFrames == before)
              break;
          } while (this.keepGoing && StreamingAudioTrack.this.writtenFrames - playbackHeadPosition < target);
          try {
            Thread.sleep(1);
          }
          catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
      catch (IllegalStateException e) {
        // The platform dropped the native track (released, or never made): there's nothing
        // left to fill, and an exception escaping this thread would end the app.
        Log.w(TAG, "Stopped filling a track that can no longer play", e);
      }
    }

    public void stopRunning() {
      this.keepGoing = false;
    }
  }

  private static final int FADE_SETTLE_MILLIS = 40;

  /**
   * How long a playing track's head may stand still before the watcher fills its whole buffer, as
   * it does before the track first starts. Longer than any mixer period, so a track that is playing
   * never looks stalled between the mixer's reads.
   */
  static final long STALL_NANOS = 100_000_000L;

  private TrackWatcherThread trackWatcherThread;

  // Frames the platform has taken since the track was made or last flushed. Only the filler
  // writes, holding its lock; play() and the watcher read it from their own threads.
  private volatile int writtenFrames;
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

  /** Frames the platform has taken since the track was made or last flushed. */
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
      // The platform can fail to make a track (too many open, the audio server busy). Such a
      // track can't play, and a watcher polling it would throw on its own thread.
      if (this.getState() != STATE_INITIALIZED)
        throw new IllegalStateException("play() called on uninitialized AudioTrack.");
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
      try {
        super.play();
      }
      catch (IllegalStateException e) {
        this.trackWatcherThread.stopRunning();
        this.trackWatcherThread = null;
        throw e;
      }
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

  // Both writes count only what the platform took, which can be less than they were given. A
  // track that isn't playing yet writes without blocking, into a buffer the server may not have
  // emptied since a flush. Counting the rest would leave the watcher waiting on audio it never sent.

  @Override
  public int write(byte[] audioData, int offsetInBytes, int sizeInBytes) {
    int written = this.writeToPlatform(audioData, offsetInBytes, sizeInBytes);
    if (written > 0)
      this.writtenFrames += written / this.numChannels / this.bytesPerSample;
    return written;
  }

  @Override
  public int write(short[] audioData, int offsetInShorts, int sizeInShorts) {
    int written = this.writeToPlatform(audioData, offsetInShorts, sizeInShorts);
    if (written > 0)
      this.writtenFrames += written * 2 / this.numChannels / this.bytesPerSample;
    return written;
  }

  /** AudioTrack's own write, which tests replace to stand for a platform that takes less. */
  int writeToPlatform(byte[] audioData, int offsetInBytes, int sizeInBytes) {
    return super.write(audioData, offsetInBytes, sizeInBytes);
  }

  /** AudioTrack's own write, which tests replace to stand for a platform that takes less. */
  int writeToPlatform(short[] audioData, int offsetInShorts, int sizeInShorts) {
    return super.write(audioData, offsetInShorts, sizeInShorts);
  }
}
