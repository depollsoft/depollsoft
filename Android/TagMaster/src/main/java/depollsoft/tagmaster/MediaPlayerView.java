package depollsoft.tagmaster;

import java.io.FileInputStream;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.TrackableBoolean;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.UiBinder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import depollsoft.lib.util.ContentCache;
import depollsoft.tagmaster.barbershop.RemoteLocation;

public class MediaPlayerView extends LinearLayout {
  private MediaPlayer player;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final TrackableField<RemoteLocation> remoteLocation = new TrackableField<>();
  private Slider balanceBar;
  private Slider playbackBar;
  private MaterialButton playPauseButton;
  private MaterialButton stopButton;
  private ContentCache cache;
  private boolean rlChangedSinceLastPlay = true;
  private boolean refreshing;
  private boolean prepared;
  // Invalidates downloads when the part changes, playback stops, or the view detaches.
  private int loadGeneration;
  private final TrackableField<Boolean> isPlaying = new TrackableField<>(false);
  private final TrackableBoolean isLoading = new TrackableBoolean(false);
  private final TrackableField<Integer> audioPosition = new TrackableField<>(0);
  private final TrackableField<Integer> audioLength = new TrackableField<>(0);
  private final TrackableField<Integer> balance = new TrackableField<>(500);

  private final Runnable updatePosition = new Runnable() {
    @Override
    public void run() {
      if (!isUsable() || player == null || !prepared || !getIsPlaying()) return;
      setAudioPosition(player.getCurrentPosition());
      handler.postDelayed(this, 250);
    }
  };

  public MediaPlayerView(Context context) {
    super(context);
    init();
  }

  public MediaPlayerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public int getAudioLength() {
    return audioLength.get();
  }

  public int getAudioPosition() {
    return audioPosition.get();
  }

  public int getBalance() {
    return balance.get();
  }

  public boolean getIsPlaying() {
    return isPlaying.get();
  }

  public boolean getIsLoading() {
    return isLoading.get();
  }

  public String getPositionString() {
    double posSec = getAudioPosition() / 1000d;
    double lenSec = getAudioLength() / 1000d;
    return String.format(Locale.US, "%1.1f/%1.1fs", posSec, lenSec);
  }

  public RemoteLocation getRemoteLocation() {
    return remoteLocation.get();
  }

  private void init() {
    LayoutInflater.from(getContext()).inflate(R.layout.mediaplayerview, this, true);
    cache = new ContentCache(getContext());
    balanceBar = findViewById(R.id.balanceSeekBar);
    playbackBar = findViewById(R.id.counterSeekBar);
    playPauseButton = findViewById(R.id.playPauseButton);
    stopButton = findViewById(R.id.stopButton);
    createPlayer();

    playbackBar.addOnChangeListener((slider, value, fromUser) -> {
      if (fromUser && !refreshing && prepared && player != null) {
        player.seekTo(Math.round(value));
        setAudioPosition(Math.round(value));
      }
    });
    balanceBar.addOnChangeListener((slider, value, fromUser) -> {
      if (fromUser && !refreshing) setBalance(Math.round(value));
    });
    playPauseButton.setOnClickListener(v -> {
      if (getIsPlaying()) pause();
      else play();
    });
    stopButton.setOnClickListener(v -> stop());
    updateControls();
  }

  private void createPlayer() {
    player = new MediaPlayer();
    player.setOnPreparedListener(mp -> {
      if (mp != player || !getIsLoading() || !isUsable()) return;
      prepared = true;
      rlChangedSinceLastPlay = false;
      setAudioLength(mp.getDuration());
      setBalance(getBalance());
      setIsLoading(false);
      mp.start();
      setIsPlaying(true);
    });
    player.setOnCompletionListener(mp -> {
      if (mp != player || !prepared) return;
      setAudioPosition(mp.getDuration());
      setIsPlaying(false);
    });
    player.setOnErrorListener((mp, what, extra) -> {
      if (mp == player) loadFailed();
      return true;
    });
    setBalance(getBalance());
  }

  private boolean isUsable() {
    if (!isAttachedToWindow()) return false;
    Context context = getContext();
    while (context instanceof ContextWrapper) {
      if (context instanceof Activity) {
        Activity activity = (Activity) context;
        return !activity.isFinishing() && !activity.isDestroyed();
      }
      context = ((ContextWrapper) context).getBaseContext();
    }
    return true;
  }

  private void play() {
    if (!isUsable() || !isEnabled() || getIsLoading() || getRemoteLocation() == null) return;
    if (player == null) createPlayer();
    if (!rlChangedSinceLastPlay && prepared) {
      player.start();
      setIsPlaying(true);
      return;
    }
    final int generation = ++loadGeneration;
    final RemoteLocation location = getRemoteLocation();
    prepared = false;
    setIsLoading(true);
    cache.loadContentPublic(location.getUri(), location.getType(), false).continueWith(task -> {
      handler.post(() -> {
        if (generation != loadGeneration || !isUsable() || player == null) return;
        if (task.isFaulted() || task.isCancelled() || task.getResult() == null) {
          loadFailed();
          return;
        }
        try (FileInputStream stream = new FileInputStream(task.getResult())) {
          player.reset();
          player.setDataSource(stream.getFD());
          // Playback begins only in OnPreparedListener, never on the UI thread's prepare path.
          player.prepareAsync();
        } catch (Exception e) {
          loadFailed();
        }
      });
      return null;
    });
  }

  private void loadFailed() {
    stop();
    if (!isUsable()) return;
    Snackbar.make(this, R.string.detail_track_failed, Snackbar.LENGTH_LONG)
        .setAction(R.string.detail_retry, v -> play())
        .show();
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (player == null) createPlayer();

    UiBinder.bind(this, R.id.counterTextView, "Text", "PositionString");
    UiBinder.bind(this, R.id.trackLoadingIndicator, "Loading", "IsLoading");
    updateSliderValues();
    updateControls();
  }

  @Override
  protected void onDetachedFromWindow() {
    stop();
    handler.removeCallbacksAndMessages(null);
    if (player != null) {
      player.release();
      player = null;
    }
    super.onDetachedFromWindow();
  }

  private void pause() {
    if (player != null && prepared && getIsPlaying()) {
      player.pause();
      setAudioPosition(player.getCurrentPosition());
    }
    setIsPlaying(false);
  }

  public void setAudioLength(int value) {
    audioLength.set(Math.max(0, value));
    updateSliderValues();
  }

  public void setAudioPosition(int value) {
    audioPosition.set(Math.max(0, value));
    updateSliderValues();
  }

  // Slider ranges must be nonempty floats, including before a track has loaded.
  private void updateSliderValues() {
    if (playbackBar == null) return;
    refreshing = true;
    try {
      float max = Math.max(1, getAudioLength());
      float position = Math.min(max, getAudioPosition());
      // Clamp before shrinking ValueTo, so Slider never observes an invalid value.
      if (playbackBar.getValue() > max) playbackBar.setValue(max);
      playbackBar.setValueTo(max);
      playbackBar.setValue(position);
    } finally {
      refreshing = false;
    }
  }

  public void setBalance(int value) {
    balance.set(value);
    float rightPercentage = 1.0f * value / 1000;
    float leftPercentage = 1.0f * (1000 - value) / 1000;
    float max = Math.max(leftPercentage, rightPercentage);
    leftPercentage /= max;
    rightPercentage /= max;
    if (player != null) player.setVolume(leftPercentage, rightPercentage);
    if (balanceBar != null) balanceBar.setValue(value);
  }

  @Override
  public void setEnabled(boolean value) {
    super.setEnabled(value);
    if (!value && player != null) stop();
    updateControls();
  }

  private void updateControls() {
    if (playPauseButton == null) return;
    playPauseButton.setEnabled(isEnabled() && getRemoteLocation() != null && !getIsLoading());
    playPauseButton.setIconResource(getIsPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
    playPauseButton.setContentDescription(getContext().getString(getIsPlaying() ? R.string.Pause : R.string.Play));
    stopButton.setEnabled(isEnabled() && (prepared || getIsLoading()));
    playbackBar.setEnabled(isEnabled() && prepared);
    balanceBar.setEnabled(isEnabled());
  }

  private void setIsLoading(boolean value) {
    isLoading.set(value);
    updateControls();
  }

  private void setIsPlaying(boolean value) {
    isPlaying.set(value);
    handler.removeCallbacks(updatePosition);
    if (value) handler.post(updatePosition);
    updateControls();
  }

  public void setRemoteLocation(RemoteLocation value) {
    remoteLocation.set(value);
    stop();
  }

  public void stop() {
    ++loadGeneration;
    handler.removeCallbacks(updatePosition);
    prepared = false;
    rlChangedSinceLastPlay = true;
    if (player != null) player.reset();
    setIsLoading(false);
    setIsPlaying(false);
    setAudioPosition(0);
  }
}
