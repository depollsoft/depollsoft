package depollsoft.lib.audio;

import android.media.AudioFormat;
import android.media.AudioTrack;

import com.bindroid.utils.Action;

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

  private TrackWatcherThread trackWatcherThread;

  private int writtenFrames;
  private int numChannels;

  private int bytesPerSample;
  private int bufferFrameThreshold;
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
    this.trackWatcherThread = new TrackWatcherThread();
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

  @Override
  public void flush() {
    super.flush();
    this.writtenFrames = 0;
  }

  @Override
  public void pause() throws IllegalStateException {
    synchronized (this.threadLock) {
      this.trackWatcherThread.stopRunning();
      this.trackWatcherThread = null;
      super.pause();
    }
  }

  @Override
  public void play() throws IllegalStateException {
    synchronized (this.threadLock) {
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
