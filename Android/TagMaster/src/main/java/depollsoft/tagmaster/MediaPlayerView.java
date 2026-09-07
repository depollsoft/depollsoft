package depollsoft.tagmaster;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.UiBinder;

import bolts.Continuation;
import bolts.Task;
import depollsoft.lib.util.ContentCache;
import depollsoft.tagmaster.barbershop.RemoteLocation;

public class MediaPlayerView extends LinearLayout {
  private MediaPlayer player;
  private final Handler playbackHandler = new Handler(Looper.getMainLooper());
  private long playbackRequest;
  private boolean loading;
  private final Runnable updatePosition = new Runnable() {
    @Override
    public void run() {
      if (player == null || !getIsPlaying()) return;
      refreshing = true;
      try {
        setAudioPosition(player.getCurrentPosition());
      } catch (IllegalStateException e) {
        failPlayback();
        return;
      } finally {
        refreshing = false;
      }
      playbackHandler.postDelayed(this, 25);
    }
  };
  private TrackableField<RemoteLocation> remoteLocation = new TrackableField<RemoteLocation>();
  private SeekBar balanceBar;
  private SeekBar playbackBar;
  private MaterialButton playPauseButton;
  private MaterialButton stopButton;
  private ContentCache cache;
  private boolean rlChangedSinceLastPlay = true;
  private boolean refreshing;
  private TrackableField<Boolean> isPlaying = new TrackableField<Boolean>(false);

  private TrackableField<Integer> audioPosition = new TrackableField<Integer>(0);

  private TrackableField<Integer> audioLength = new TrackableField<Integer>(0);

  private TrackableField<Integer> balance = new TrackableField<Integer>();

  public MediaPlayerView(Context context) {
    super(context);
    this.init();
  }

  public MediaPlayerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  public int getAudioLength() {
    return this.audioLength.get();
  }

  public int getAudioPosition() {
    return this.audioPosition.get();
  }

  public int getBalance() {
    return this.balance.get();
  }

  public boolean getIsPlaying() {
    return this.isPlaying.get();
  }

  public String getPositionString() {
    int position = this.getAudioPosition();
    int length = this.getAudioLength();

    double posSec = position / 1000d;
    double lenSec = length / 1000d;

    return String.format(Locale.US, "%1.1f/%1.1fs", posSec, lenSec);
  }

  public RemoteLocation getRemoteLocation() {
    return this.remoteLocation.get();
  }

  private void init() {
    LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.mediaplayerview, this, true);

    this.cache = new ContentCache(this.getContext());
    this.setBalance(500);

    this.balanceBar = (SeekBar) this.findViewById(R.id.balanceSeekBar);
    this.playbackBar = (SeekBar) this.findViewById(R.id.counterSeekBar);
    this.playPauseButton = (MaterialButton) this.findViewById(R.id.playPauseButton);
    this.stopButton = (MaterialButton) this.findViewById(R.id.stopButton);

    this.playbackBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser && !MediaPlayerView.this.refreshing
                && MediaPlayerView.this.player != null
                && !MediaPlayerView.this.rlChangedSinceLastPlay)
          MediaPlayerView.this.player.seekTo(progress);
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });

    this.balanceBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        MediaPlayerView.this.setBalance(progress);
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
      }

    });

    this.playPauseButton.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
        MediaPlayerView.this.playOrPause();
      }
    });

    this.stopButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        MediaPlayerView.this.stop();
      }
    });
  }

  private void createPlayer() {
    this.player = new MediaPlayer();
    this.setBalance(this.getBalance());
    this.player.setOnCompletionListener(new OnCompletionListener() {
      public void onCompletion(MediaPlayer mp) {
        if (mp != MediaPlayerView.this.player) return;
        MediaPlayerView.this.setIsPlaying(false);
        MediaPlayerView.this.setAudioPosition(MediaPlayerView.this.getAudioLength());
      }
    });
    this.player.setOnErrorListener(new OnErrorListener() {
      public boolean onError(MediaPlayer mp, int what, int extra) {
        if (mp == MediaPlayerView.this.player) MediaPlayerView.this.failPlayback();
        return true;
      }
    });
  }

  private void playOrPause() {
    if (!this.isEnabled() || this.loading || this.getRemoteLocation() == null) return;
    if (this.getIsPlaying()) {
      this.pause();
      return;
    }
    if (!this.rlChangedSinceLastPlay && this.player != null) {
      try {
        this.player.start();
        this.setIsPlaying(true);
      } catch (IllegalStateException e) {
        this.failPlayback();
      }
      return;
    }

    final long request = ++this.playbackRequest;
    final RemoteLocation location = this.getRemoteLocation();
    // Keep the existing URL/type cache key and inline loading control.
    this.setLoading(true);
    try {
      this.cache.loadContentPublic(location.getUri(), location.getType(), false)
              .continueWith(new Continuation<File, Void>() {
        @Override
        public Void then(final Task<File> task) {
          // Use the main handler rather than View.post, which queues work until reattach.
          playbackHandler.post(new Runnable() {
            public void run() {
              if (request != playbackRequest) return;
              if (task.isCancelled()) {
                stop();
                return;
              }
              if (task.isFaulted() || task.getResult() == null) {
                failPlayback();
                return;
              }
              try (FileInputStream fis = new FileInputStream(task.getResult())) {
                createPlayer();
                player.setDataSource(fis.getFD());
                player.prepare();
                setAudioLength(player.getDuration());
                player.start();
                rlChangedSinceLastPlay = false;
                setIsPlaying(true);
                setLoading(false);
              } catch (java.io.IOException | IllegalStateException e) {
                failPlayback();
              }
            }
          });
          return null;
        }
      });
    } catch (RuntimeException e) {
      this.failPlayback();
    }
  }

  private void failPlayback() {
    this.stop();
    this.reportTrackFailure();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    UiBinder.bind(this, R.id.stopButton, "Enabled", "IsPlaying");
    UiBinder.bind(this, R.id.playPauseButton, "Checked", "IsPlaying");

    UiBinder.bind(this, R.id.counterSeekBar, "Progress", "AudioPosition");
    UiBinder.bind(this, R.id.counterSeekBar, "Max", "AudioLength");

    UiBinder.bind(this, R.id.balanceSeekBar, "Progress", "Balance");

    UiBinder.bind(this, R.id.counterTextView, "Text", "PositionString");
  }

  @Override
  protected void onDetachedFromWindow() {
    this.stop();
    super.onDetachedFromWindow();
  }

  /** The track could not be fetched or decoded; say so where the control is. */
  private void reportTrackFailure() {
    Snackbar.make(this, R.string.TrackLoadFailed, Snackbar.LENGTH_SHORT).show();
  }

  private void setLoading(boolean value) {
    this.loading = value;
    CircularProgressIndicator indicator = this.findViewById(R.id.trackLoading);
    if (indicator != null) {
      indicator.setVisibility(value ? VISIBLE : GONE);
    }
    this.playPauseButton.setVisibility(value ? GONE : VISIBLE);
  }

  private void pause() {
    try {
      if (this.player != null && this.getIsPlaying()) this.player.pause();
      this.setIsPlaying(false);
    } catch (IllegalStateException e) {
      this.failPlayback();
    }
  }

  public void setAudioLength(int value) {
    this.audioLength.set(value);
  }

  public void setAudioPosition(int value) {
    this.audioPosition.set(value);
  }

  public void setBalance(int value) {
    this.balance.set(value);
    float rightPercentage = 1.0f * value / 1000;
    float leftPercentage = 1.0f * (1000 - value) / 1000;
    float max = Math.max(leftPercentage, rightPercentage);
    leftPercentage /= max;
    rightPercentage /= max;
    if (this.player != null) this.player.setVolume(leftPercentage, rightPercentage);
  }

  @Override
  public void setEnabled(boolean value) {
    this.balanceBar.setEnabled(value);
    this.playbackBar.setEnabled(value);
    this.playPauseButton.setEnabled(value);
    this.stopButton.setEnabled(value);
    super.setEnabled(value);
  }

  private void setIsPlaying(boolean value) {
    this.playbackHandler.removeCallbacks(this.updatePosition);
    this.isPlaying.set(value);
    if (value) this.playbackHandler.post(this.updatePosition);
  }

  public void setRemoteLocation(RemoteLocation value) {
    this.stop();
    this.remoteLocation.set(value);
  }

  public void stop() {
    ++this.playbackRequest;
    this.setIsPlaying(false);
    this.setLoading(false);
    this.rlChangedSinceLastPlay = true;
    if (this.player != null) {
      MediaPlayer previous = this.player;
      this.player = null;
      // release is valid even after a decode error or before preparation.
      previous.release();
    }
    this.setAudioPosition(0);
    this.setAudioLength(0);
  }
}
