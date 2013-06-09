package depollsoft.tagmaster;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.bindroid.converters.BoolConverter;
import com.bindroid.converters.ToStringConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.BoundUi;
import com.bindroid.ui.UiBinder;

import depollsoft.tagmaster.barbershop.Video;

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
    return this.thumbnailUri.get();
  }

  public Video getVideo() {
    return this.video.get();
  }

  public String getWatchUri() {
    return this.watchUri.get();
  }

  private void init() {
    LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.videodisplay, this, true);

    this.setClickable(true);
    this.setOnClickListener(new OnClickListener() {

      public void onClick(View arg0) {
        if (VideoDisplay.this.getWatchUri() != null) {
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
    UiBinder.bind(this, R.id.keyRow, "Visibility", "Video.SungKey", BoolConverter.get());

    UiBinder.bind(this, R.id.postedTextView, "Text", "Video.Posted", new ToStringConverter(
        "%1$tA, %1$tB %1$te, %1$tY"));

    UiBinder.bind(this, R.id.multitrackCheckBox, "Checked", "Video.IsMultitrack");

    UiBinder.bind(this, R.id.videoPreview, "Source", "ThumbnailUri");
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }

  public void setThumbnailUri(String value) {
    this.thumbnailUri.set(value);
  }

  public void setVideo(Video value) {
    this.video.set(value);
    this.setThumbnailUri(String.format("http://img.youtube.com/vi/%s/2.jpg", value.getYouTubeCode()));
    this.setWatchUri(String.format("http://www.youtube.com/watch?v=%s", value.getYouTubeCode()));
  }

  public void setWatchUri(String value) {
    this.watchUri.set(value);
  }
}
