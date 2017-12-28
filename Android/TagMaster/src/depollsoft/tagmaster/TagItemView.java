package depollsoft.tagmaster;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.bindroid.converters.BoolConverter;
import com.bindroid.converters.ToStringConverter;
import com.bindroid.trackable.TrackableField;
import com.bindroid.ui.BoundUi;
import com.bindroid.ui.UiBinder;

import depollsoft.tagmaster.barbershop.Tag;

public class TagItemView extends LinearLayout implements BoundUi<Tag> {

  private TrackableField<Tag> tag = new TrackableField<Tag>();

  public TagItemView(Context context) {
    super(context);
    this.init();
  }

  public TagItemView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.init();
  }

  public void bind(Tag dataSource) {
    this.setTag(dataSource);
  }

  @Override
  public Tag getTag() {
    return this.tag.get();
  }

  private void init() {
    LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
    inflater.inflate(R.layout.tagitemview, this, true);

    this.setClickable(true);
    this.setLongClickable(false);
    this.setOnClickListener(new OnClickListener() {

      public void onClick(View arg0) {
        if (TagItemView.this.getTag() != null) {
          Intent i = new Intent(TagItemView.this.getContext(), TagDetailActivity.class);
          Bundle b = new Bundle();
          b.putInt(TagDetailActivity.TAG_ID_EXTRA, TagItemView.this.getTag().getId());
          i.putExtras(b);
          TagItemView.this.getContext().startActivity(i);
        }
      }
    });
    this.setOnLongClickListener(new OnLongClickListener() {

      public boolean onLongClick(View v) {
        return false;
      }
    });
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    UiBinder.bind(this, R.id.titleTextView, "Text", "Tag.Title");

    UiBinder.bind(this, R.id.akaTextView, "Text", "Tag.AlternativeTitle", new ToStringConverter(
            "a.k.a. %s"));
    UiBinder
            .bind(this, R.id.akaTextView, "Visibility", "Tag.AlternativeTitle", BoolConverter.get());

    UiBinder.bind(this, R.id.idTextView, "Text", "Tag.Id", new ToStringConverter());

    UiBinder.bind(this, R.id.ratingTextView, "Text", "Tag.Rating", new ToStringConverter(" %3.2f"));
    UiBinder.bind(this, R.id.ratingContainer, "Visibility", "Tag.Rating", BoolConverter.get());

    UiBinder.bind(this, R.id.postedTextView, "Text", "Tag.Posted", new ToStringConverter(" %tD"));

    UiBinder.bind(this, R.id.downloadsTextView, "Text", "Tag.DownloadCount", new ToStringConverter(
            " %d"));
    UiBinder.bind(this, R.id.downloadsContainer, "Visibility", "Tag.DownloadCount",
            BoolConverter.get());

    UiBinder.bind(this, R.id.sheetMusicCheckBox, "Checked", "Tag.SheetMusicUri",
            BoolConverter.get());

    UiBinder.bind(this, R.id.learningTracksCheckBox, "Checked", "Tag.Tracks",
            BoolConverter.get(false, true));
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
  }

  public void setTag(Tag value) {
    this.tag.set(value);
  }
}
