package depollsoft.tagmaster;

import java.io.File;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.Binding;
import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.ToStringConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.ui.Hyperlink;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.ContentCache;
import depollsoft.lib.util.Function;
import depollsoft.lib.util.Property;
import depollsoft.lib.util.ReflectedProperty;
import depollsoft.tagmaster.lib.RatingConverter;
import depollsoft.tagmaster.barbershop.*;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

public class TagSummaryActivity extends Activity {
  public boolean getCanRate() {
    return !RatingsModel.isRated((((TagDetailActivity) this.getParent())
        .getTag()).getId());
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagsummaryview);

    UiBinder.bind(this, R.id.titleTextView, "Text", "Parent.Tag.Title");
    UiBinder.bind(this, R.id.titleTextView, "Visibility", "Parent.Tag.Title",
        BoolConverter.get());

    UiBinder
        .bind(this, R.id.akaTextView, "Text", "Parent.Tag.AlternativeTitle");
    UiBinder.bind(this, R.id.akaLayout, "Visibility",
        "Parent.Tag.AlternativeTitle", BoolConverter.get());

    UiBinder.bind(this, R.id.versionTextView, "Text", "Parent.Tag.Version");
    UiBinder.bind(this, R.id.versionLayout, "Visibility", "Parent.Tag.Version",
        BoolConverter.get());

    UiBinder.bind(this, R.id.ratingTextView, "Text", "Parent.Tag.Rating",
        new ToStringConverter("%3.2f"));
    UiBinder.bind(this, R.id.ratingTextView, "Visibility", "Parent.Tag.Rating",
        BoolConverter.get());
    UiBinder.bind(this, R.id.ratingProgressBar, "Progress",
        "Parent.Tag.Rating", new RatingConverter());
    UiBinder.bind(this, R.id.rateButton, "Enabled", "CanRate");

    UiBinder.bind(this, R.id.partsTextView, "Text", "Parent.Tag.Parts",
        new ToStringConverter());
    UiBinder.bind(this, R.id.partsRow, "Visibility", "Parent.Tag.Parts",
        BoolConverter.get());

    UiBinder.bind(this, R.id.tagTypeTextView, "Text", "Parent.Tag.TagType",
        new ToStringConverter());

    UiBinder.bind(this, R.id.playKeyNoteButton, "Note", "Parent.Tag.KeyNote");
    UiBinder
        .bind(this, R.id.playKeyNoteButton, "Text", "Parent.Tag.WrittenKey");
    UiBinder.bind(this, R.id.keyRow, "Visibility", "Parent.Tag.WrittenKey",
        BoolConverter.get());

    UiBinder.bind(this, R.id.classicTagTextView, "Text",
        "Parent.Tag.ClassicTagNumber", new ToStringConverter());
    UiBinder.bind(this, R.id.classicTagRow, "Visibility",
        "Parent.Tag.ClassicTagNumber", BoolConverter.get());

    UiBinder.bind(this, R.id.notesTextView, "Text", "Parent.Tag.Notes");
    UiBinder.bind(this, R.id.notesRow, "Visibility", "Parent.Tag.Notes",
        BoolConverter.get());

    UiBinder.bind(this, R.id.lyricsTextView, "Text", "Parent.Tag.Lyrics");
    UiBinder.bind(this, R.id.lyricsRow, "Visibility", "Parent.Tag.Lyrics",
        BoolConverter.get());

    UiBinder.bind(this, R.id.sheetMusicLink, "HyperlinkUri",
        "Parent.Tag.SheetMusicUri.Uri");
    UiBinder.bind(this, R.id.sheetMusicLink, "Visibility",
        "Parent.Tag.SheetMusicUri", BoolConverter.get());

    UiBinder.registerBinding(
        this,
        new Binding(new ReflectedProperty(this
            .findViewById(R.id.favoriteMarkerTextView), "Visibility"),
            new Property<Boolean>(new Function<Boolean>() {

              public Boolean evaluate() {
                return FavoritesModel
                    .getIsFavorite(((TagDetailActivity) TagSummaryActivity.this
                        .getParent()).getTag().getId());
              }
            }, null, Boolean.class), BindingMode.OneWay, BoolConverter.get())
            .bind(this));
    UiBinder.registerBinding(
        this,
        new Binding(new ReflectedProperty(this
            .findViewById(R.id.teachableMarkerTextView), "Visibility"),
            new Property<Boolean>(new Function<Boolean>() {

              public Boolean evaluate() {
                return TeachableTagsModel
                    .getIsTeachableTag(((TagDetailActivity) TagSummaryActivity.this
                        .getParent()).getTag().getId());
              }
            }, null, Boolean.class), BindingMode.OneWay, BoolConverter.get())
            .bind(this));

    Hyperlink link = (Hyperlink) this.findViewById(R.id.sheetMusicLink);
    link.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
        Tag tag = ((TagDetailActivity) TagSummaryActivity.this.getParent())
            .getTag();
        final String sheetMusicType = tag.getSheetMusicUri().getType();
        final String sheetMusicUri = tag.getSheetMusicUri().getUri();
        final ProgressDialog progress = new ProgressDialog(
            TagSummaryActivity.this.getParent());
        progress.setIndeterminate(true);
        progress.setMessage("Loading...");
        progress.show();

        ContentCache cache = new ContentCache(TagSummaryActivity.this);
        cache.loadContentPublic(sheetMusicUri, sheetMusicType, false)
            .continueWith(new Action<File>() {

              public void invoke(File parameter) {
                try {
                  String contentPath = "content://depollsoft.tagmaster/"
                      + sheetMusicType + "/" + Uri.encode(sheetMusicUri);
                  Uri path = Uri.parse(contentPath);
                  Intent intent = new Intent(Intent.ACTION_VIEW);
                  if (sheetMusicType.toLowerCase().equals("pdf")) {
                    intent.setDataAndType(path, "application/pdf");
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                  }
                  else {
                    MimeTypeMap map = MimeTypeMap.getSingleton();
                    String mimeType = map
                        .getMimeTypeFromExtension(sheetMusicType.toLowerCase());
                    intent.setDataAndType(path, mimeType);
                  }
                  try {
                    TagSummaryActivity.this.startActivity(intent);
                    GoogleAnalyticsTracker.getInstance().trackEvent(
                        "MediaView", "ViewSheetMusic", contentPath, 0);
                  }
                  catch (ActivityNotFoundException e) {
                    TagSummaryActivity.this.runOnUiThread(new Runnable() {
                      public void run() {
                        Toast.makeText(
                            TagSummaryActivity.this,
                            "No application available to view this sheet music ("
                                + sheetMusicType + ").", Toast.LENGTH_SHORT)
                            .show();
                      }
                    });
                  }
                }
                catch (Exception e) {
                  e.printStackTrace();
                }
                finally {
                  progress.dismiss();
                }
              }
            }, new Action<Exception>() {

              public void invoke(Exception parameter) {
                TagSummaryActivity.this.runOnUiThread(new Runnable() {
                  public void run() {
                    Toast.makeText(TagSummaryActivity.this,
                        "Unable to load sheet music.  Please try again later.",
                        Toast.LENGTH_SHORT).show();
                    progress.dismiss();
                  }
                });
              }
            });
      }
    });

    Button rateButton = (Button) this.findViewById(R.id.rateButton);
    rateButton.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
        final RatingsPopup popup = new RatingsPopup(TagSummaryActivity.this
            .getParent());
        popup.setOnDismissListener(new OnDismissListener() {

          public void onDismiss(DialogInterface arg0) {
            if (popup.getRating() == null)
              return;
            final Tag tag = ((TagDetailActivity) TagSummaryActivity.this
                .getParent()).getTag();
            final ProgressDialog pd = new ProgressDialog(
                TagSummaryActivity.this.getParent());
            pd.setIndeterminate(true);
            pd.setMessage("Submitting rating...");
            pd.show();
            tag.rate(popup.getRating()).continueWith(new Action<Boolean>() {

              public void invoke(Boolean parameter) {
                RatingsModel.addRating(tag.getId());
                pd.dismiss();
              }
            }, new Action<Exception>() {

              public void invoke(Exception parameter) {
                TagSummaryActivity.this.runOnUiThread(new Runnable() {

                  public void run() {
                    Toast.makeText(TagSummaryActivity.this,
                        "Failed to submit rating.  Please try again later.",
                        Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                  }
                });
              }
            });
          }
        });
        popup.show();
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    UiBinder.unbind(this);
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (this.getParent() != null)
      return this.getParent().onMenuItemSelected(featureId, item);
    return false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    this.runOnUiThread(new Runnable() {
      public void run() {
        GoogleAnalyticsTracker.getInstance().trackPageView(
            "TagDetailActivity/"
                + ((TagDetailActivity) TagSummaryActivity.this.getParent())
                    .getTag().getId() + "/summary");
      }
    });
  }

}
