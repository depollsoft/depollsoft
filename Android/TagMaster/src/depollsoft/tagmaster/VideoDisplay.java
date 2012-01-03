package depollsoft.tagmaster;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.ToStringConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.ui.BoundUi;
import depollsoft.tagmaster.barbershop.Video;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class VideoDisplay extends LinearLayout implements BoundUi<Video> {

  private TrackableField<Video> video = new TrackableField<Video>();

  private TrackableField<String> thumbnailUri = new TrackableField<String>();

  private TrackableField<String> watchUri = new TrackableField<String>();

  public VideoDisplay(Context context) {
    super(context);
    this.init();
  }

  public VideoDisplay(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  public void bind(Video dataSource) {
    this.setVideo(dataSource);
  }

  public String getThumbnailUri() {
    return this.thumbnailUri.getValue();
  }

  public Video getVideo() {
    return this.video.getValue();
  }

  public String getWatchUri() {
    return this.watchUri.getValue();
  }

  private void init() {
    LayoutInflater inflater = (LayoutInflater) this.getContext()
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.videodisplay, this, true);

    this.setClickable(true);
    this.setOnClickListener(new OnClickListener() {

      public void onClick(View arg0) {
        if (VideoDisplay.this.getWatchUri() != null) {
          GoogleAnalyticsTracker.getInstance().trackEvent("MediaViews",
              "ViewVideo", VideoDisplay.this.getWatchUri(), 0);
          Intent i = new Intent(Intent.ACTION_VIEW);
          i.setData(Uri.parse(VideoDisplay.this.getWatchUri()));
          VideoDisplay.this.getContext().startActivity(i);
        }
      }
    });
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    UiBinder.bind(this, R.id.sungByTextView, "Text", "Video.SungBy");

    UiBinder.bind(this, R.id.keyTextView, "Text", "Video.SungKey");
    UiBinder.bind(this, R.id.keyRow, "Visibility", "Video.SungKey",
        BoolConverter.get());

    UiBinder.bind(this, R.id.postedTextView, "Text", "Video.Posted",
        new ToStringConverter("%1$tA, %1$tB %1$te, %1$tY"));

    UiBinder.bind(this, R.id.multitrackCheckBox, "Checked",
        "Video.IsMultitrack");

    UiBinder.bind(this, R.id.videoPreview, "Source", "ThumbnailUri");
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    UiBinder.unbind(this);
  }

  public void setThumbnailUri(String value) {
    this.thumbnailUri.setValue(value);
  }

  public void setVideo(Video value) {
    this.video.setValue(value);
    this.setThumbnailUri(String.format("http://img.youtube.com/vi/%s/2.jpg",
        value.getYouTubeCode()));
    this.setWatchUri(String.format("http://www.youtube.com/watch?v=%s",
        value.getYouTubeCode()));
  }

  public void setWatchUri(String value) {
    this.watchUri.setValue(value);
  }
}
