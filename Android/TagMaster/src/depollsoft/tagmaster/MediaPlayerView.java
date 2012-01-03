package depollsoft.tagmaster;

import java.io.File;
import java.io.FileInputStream;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.ContentCache;
import depollsoft.tagmaster.barbershop.RemoteLocation;
import android.app.ProgressDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MediaPlayerView extends LinearLayout {
  private MediaPlayer player;
  private Timer timer;
  private TimerTask timerTask;
  private TrackableField<RemoteLocation> remoteLocation = new TrackableField<RemoteLocation>();
  private SeekBar balanceBar;
  private SeekBar playbackBar;
  private ToggleButton playPauseButton;
  private Button stopButton;
  private ContentCache cache;
  private boolean rlChangedSinceLastPlay;
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
    return this.audioLength.getValue();
  }

  public int getAudioPosition() {
    return this.audioPosition.getValue();
  }

  public int getBalance() {
    return this.balance.getValue();
  }

  public boolean getIsPlaying() {
    return this.isPlaying.getValue();
  }

  public String getPositionString() {
    int position = this.getAudioPosition();
    int length = this.getAudioLength();

    double posSec = position / 1000d;
    double lenSec = length / 1000d;

    return String.format("%1.1f/%1.1fs", posSec, lenSec);
  }

  public RemoteLocation getRemoteLocation() {
    return this.remoteLocation.getValue();
  }

  private void init() {
    LayoutInflater inflater = (LayoutInflater) this.getContext()
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.mediaplayerview, this, true);

    this.player = new MediaPlayer();
    this.cache = new ContentCache(this.getContext());
    this.setBalance(500);

    this.balanceBar = (SeekBar) this.findViewById(R.id.balanceSeekBar);
    this.playbackBar = (SeekBar) this.findViewById(R.id.counterSeekBar);
    this.playPauseButton = (ToggleButton) this
        .findViewById(R.id.playPauseButton);
    this.stopButton = (Button) this.findViewById(R.id.stopButton);

    this.timer = new Timer();

    this.player.setOnPreparedListener(new OnPreparedListener() {
      public void onPrepared(MediaPlayer mp) {
        MediaPlayerView.this.setAudioLength(MediaPlayerView.this.player
            .getDuration());
      }
    });
    this.player.setOnCompletionListener(new OnCompletionListener() {

      public void onCompletion(MediaPlayer mp) {
        MediaPlayerView.this.setIsPlaying(false);
      }
    });
    this.player.setOnErrorListener(new OnErrorListener() {

      public boolean onError(MediaPlayer mp, int what, int extra) {
        Toast.makeText(MediaPlayerView.this.getContext(),
            "Failed to load track.", Toast.LENGTH_SHORT).show();
        MediaPlayerView.this.setIsPlaying(false);
        return true;
      }
    });

    this.playbackBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar seekBar, int progress,
          boolean fromUser) {
        if (fromUser && !MediaPlayerView.this.refreshing)
          MediaPlayerView.this.player.seekTo(progress);
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
      }
    });

    this.balanceBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

      public void onProgressChanged(SeekBar seekBar, int progress,
          boolean fromUser) {
        MediaPlayerView.this.setBalance(progress);
      }

      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      public void onStopTrackingTouch(SeekBar seekBar) {
      }

    });

    this.playPauseButton.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
        if (MediaPlayerView.this.getIsPlaying()) {
          MediaPlayerView.this.pause();
          return;
        }
        else if (!MediaPlayerView.this.rlChangedSinceLastPlay) {
          MediaPlayerView.this.player.start();
          MediaPlayerView.this.setIsPlaying(true);
        }
        else {
          MediaPlayerView.this.rlChangedSinceLastPlay = false;
          final ProgressDialog dialog = new ProgressDialog(
              ((TagTracksActivity) MediaPlayerView.this.getContext())
                  .getParent());
          dialog.setMessage("Loading track...");
          dialog.show();
          MediaPlayerView.this.cache.loadContentPublic(
              MediaPlayerView.this.getRemoteLocation().getUri(),
              MediaPlayerView.this.getRemoteLocation().getType(), false)
              .continueWith(new Action<File>() {
                public void invoke(final File parameter) {
                  MediaPlayerView.this.post(new Runnable() {

                    public void run() {
                      try {
                        FileInputStream fis = new FileInputStream(parameter);
                        MediaPlayerView.this.player.reset();
                        MediaPlayerView.this.player.setDataSource(fis.getFD());
                        MediaPlayerView.this.player.prepare();
                        MediaPlayerView.this
                            .setAudioLength(MediaPlayerView.this.player
                                .getDuration());
                        fis.close();
                        MediaPlayerView.this.player.start();
                        MediaPlayerView.this.setIsPlaying(true);
                      }
                      catch (Exception e) {
                        Toast.makeText(MediaPlayerView.this.getContext(),
                            "Failed to load track.", Toast.LENGTH_SHORT).show();
                        MediaPlayerView.this.setIsPlaying(false);
                      }
                      finally {
                        dialog.dismiss();
                      }
                    }
                  });
                }
              }, new Action<Exception>() {
                public void invoke(Exception parameter) {
                  MediaPlayerView.this.post(new Runnable() {

                    public void run() {
                      dialog.dismiss();
                      Toast.makeText(MediaPlayerView.this.getContext(),
                          "Failed to load track.", Toast.LENGTH_SHORT).show();
                    }
                  });
                }
              });
        }
        GoogleAnalyticsTracker.getInstance().trackEvent("MediaViews",
            "PlayTrack", MediaPlayerView.this.getRemoteLocation().getUri(), 0);
      }
    });

    this.stopButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        MediaPlayerView.this.stop();
      }
    });
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
    super.onDetachedFromWindow();

    this.stop();
    UiBinder.unbind(this);
  }

  private void pause() {
    if (this.player.isPlaying()) {
      this.player.pause();
      this.setIsPlaying(false);
    }
  }

  public void setAudioLength(int value) {
    this.audioLength.setValue(value);
  }

  public void setAudioPosition(int value) {
    this.audioPosition.setValue(value);
  }

  public void setBalance(int value) {
    this.balance.setValue(value);
    float rightPercentage = 1.0f * value / 1000;
    float leftPercentage = 1.0f * (1000 - value) / 1000;
    float max = Math.max(leftPercentage, rightPercentage);
    leftPercentage /= max;
    rightPercentage /= max;
    this.player.setVolume(leftPercentage, rightPercentage);
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
    this.isPlaying.setValue(value);
    if (value) {
      this.timerTask = new TimerTask() {

        @Override
        public void run() {
          MediaPlayerView.this.post(new Runnable() {
            public void run() {
              MediaPlayerView.this.refreshing = true;
              MediaPlayerView.this.setAudioPosition(MediaPlayerView.this.player
                  .getCurrentPosition());
              MediaPlayerView.this.refreshing = false;
            }
          });
        }
      };
      this.timer.scheduleAtFixedRate(this.timerTask, 0, 25);
    }
    else if (this.timerTask != null)
      this.timerTask.cancel();
  }

  public void setRemoteLocation(RemoteLocation value) {
    this.remoteLocation.setValue(value);
    this.rlChangedSinceLastPlay = true;
    this.stop();
  }

  private void stop() {
    if (this.player.isPlaying())
      this.player.stop();
    this.setIsPlaying(false);
    this.rlChangedSinceLastPlay = true;
  }
}
