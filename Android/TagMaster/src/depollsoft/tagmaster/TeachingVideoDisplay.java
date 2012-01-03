package depollsoft.tagmaster;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.tagmaster.barbershop.Tag;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class TeachingVideoDisplay extends LinearLayout {

  private TrackableField<String> thumbnailUri = new TrackableField<String>();

  private TrackableField<String> watchUri = new TrackableField<String>();

  private TrackableField<Tag> tag = new TrackableField<Tag>();

  public TeachingVideoDisplay(Context context) {
    super(context);
    this.init();
  }

  public TeachingVideoDisplay(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  @Override
  public Tag getTag() {
    return this.tag.getValue();
  }

  public String getThumbnailUri() {
    return this.thumbnailUri.getValue();
  }

  public String getWatchUri() {
    return this.watchUri.getValue();
  }

  private void init() {
    LayoutInflater inflater = (LayoutInflater) this.getContext()
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.teachingvideodisplay, this, true);

    this.setClickable(true);
    this.setOnClickListener(new OnClickListener() {

      public void onClick(View arg0) {
        if (TeachingVideoDisplay.this.getWatchUri() != null) {
          GoogleAnalyticsTracker.getInstance().trackEvent("MediaViews",
              "ViewTeachingVideo", TeachingVideoDisplay.this.getWatchUri(), 0);
          Intent i = new Intent(Intent.ACTION_VIEW);
          i.setData(Uri.parse(TeachingVideoDisplay.this.getWatchUri()));
          TeachingVideoDisplay.this.getContext().startActivity(i);
        }
      }
    });
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    UiBinder.bind(this, R.id.teacherTextView, "Text", "Tag.Teacher");

    UiBinder.bind(this, R.id.videoPreview, "Source", "ThumbnailUri");
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    UiBinder.unbind(this);
  }

  public void setTag(Tag value) {
    this.tag.setValue(value);
    this.setThumbnailUri(String.format("http://img.youtube.com/vi/%s/2.jpg",
        value.getTeachingVideo()));
    this.setWatchUri(String.format("http://www.youtube.com/watch?v=%s",
        value.getTeachingVideo()));
  }

  public void setThumbnailUri(String value) {
    this.thumbnailUri.setValue(value);
  }

  public void setWatchUri(String value) {
    this.watchUri.setValue(value);
  }
}
