package depollsoft.tagmaster;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.UiBinder;

import depollsoft.tagmaster.barbershop.Tag;

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
    return this.tag.get();
  }

  public String getThumbnailUri() {
    return this.thumbnailUri.get();
  }

  public String getWatchUri() {
    return this.watchUri.get();
  }

  private void init() {
    LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.teachingvideodisplay, this, true);

    android.util.TypedValue background = new android.util.TypedValue();
    getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, background, true);
    setBackgroundResource(background.resourceId);
    this.setFocusable(true);
    this.setClickable(true);
    this.setOnClickListener(new OnClickListener() {

      public void onClick(View arg0) {
        if (TeachingVideoDisplay.this.getWatchUri() != null) {
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
  }

  public void setTag(Tag value) {
    this.tag.set(value);
    this.setThumbnailUri(String.format("https://img.youtube.com/vi/%s/2.jpg",
        value.getTeachingVideo()));
    this.setWatchUri(String.format("https://www.youtube.com/watch?v=%s", value.getTeachingVideo()));
  }

  public void setThumbnailUri(String value) {
    this.thumbnailUri.set(value);
  }

  public void setWatchUri(String value) {
    this.watchUri.set(value);
  }
}
