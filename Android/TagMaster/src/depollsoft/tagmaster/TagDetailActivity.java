package depollsoft.tagmaster;

import depollsoft.lib.binding.Binding;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.ui.ThreadSwitchProperty;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.ContentCache;
import depollsoft.lib.util.ReflectedProperty;
import depollsoft.tagmaster.barbershop.RemoteLocation;
import depollsoft.tagmaster.barbershop.Tag;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class TagDetailActivity extends TabActivity {
  public static final String TAG_ID_EXTRA = "depollsoft.tagmaster.tagid";
  private TrackableField<Tag> tag = new TrackableField<Tag>();
  private ProgressDialog progress;

  public TagDetailActivity() {
  }

  public Tag getTag() {
    return this.tag.getValue();
  }

  private void loadQueryItem(boolean refresh) {
    Tag original = this.getTag();
    if (original != null) {
      ContentCache cache = new ContentCache(this);
      ArrayList<RemoteLocation> contentToDelete = new ArrayList<RemoteLocation>();
      contentToDelete.add(original.getAllPartsTrackUri());
      contentToDelete.add(original.getBaritoneTrackUri());
      contentToDelete.add(original.getBassTrackUri());
      contentToDelete.add(original.getLeadTrackUri());
      contentToDelete.add(original.getNotationUri());
      contentToDelete.add(original.getOther1TrackUri());
      contentToDelete.add(original.getOther2TrackUri());
      contentToDelete.add(original.getOther3TrackUri());
      contentToDelete.add(original.getOther4TrackUri());
      contentToDelete.add(original.getTenorTrackUri());
      contentToDelete.add(original.getSheetMusicUri());
      for (RemoteLocation loc : contentToDelete) {
        if (loc != null) {
          cache.deletePrivateContent(loc.getUri(), loc.getType());
          cache.deletePublicContent(loc.getUri(), loc.getType());
        }
      }
    }
    if (this.progress.isShowing())
      return;
    this.progress.setIndeterminate(true);
    this.progress.setMessage("Loading...");
    this.progress.show();

    int tagId = this.getIntent().getExtras()
        .getInt(TagDetailActivity.TAG_ID_EXTRA);

    Tag.loadTagById(tagId, refresh).continueWith(new Action<Tag>() {

      public void invoke(final Tag parameter) {
        TagDetailActivity.this.runOnUiThread(new Runnable() {

          public void run() {
            TagDetailActivity.this.setTag(null);
            TagDetailActivity.this.setTag(parameter);
            TagDetailActivity.this.progress.dismiss();
          }
        });
      }
    }, new Action<Exception>() {

      public void invoke(Exception parameter) {
        TagDetailActivity.this.progress.dismiss();
      }
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagdetailview);
    this.progress = new ProgressDialog(this);

    Intent summaryIntent = new Intent(this, TagSummaryActivity.class);
    summaryIntent.putExtras(this.getIntent().getExtras());
    this.getTabHost().addTab(
        this.getTabHost().newTabSpec("summary").setContent(summaryIntent)
            .setIndicator("Summary"));

    Intent detailsIntent = new Intent(this, TagMiscActivity.class);
    detailsIntent.putExtras(this.getIntent().getExtras());
    this.getTabHost().addTab(
        this.getTabHost().newTabSpec("details").setContent(detailsIntent)
            .setIndicator("Details"));

    Intent tracksIntent = new Intent(this, TagTracksActivity.class);
    detailsIntent.putExtras(this.getIntent().getExtras());
    this.getTabHost().addTab(
        this.getTabHost().newTabSpec("tracks").setContent(tracksIntent)
            .setIndicator("Tracks"));

    Intent videosIntent = new Intent(this, TagVideosActivity.class);
    videosIntent.putExtras(this.getIntent().getExtras());
    this.getTabHost().addTab(
        this.getTabHost().newTabSpec("videos").setContent(videosIntent)
            .setIndicator("Videos"));

    UiBinder.registerBinding(this, new Binding(
        new ThreadSwitchProperty<Object>(new ReflectedProperty(this, "Title"),
            this), new ReflectedProperty(this, "Tag.Title")).bind(this));

    UiBinder.bind(this, R.id.tabContentHolder, "Visibility", "Tag",
        BoolConverter.get());

    this.loadQueryItem(false);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (this.getTag() == null)
      return false;
    this.getMenuInflater().inflate(R.menu.tagdetailmenu, menu);
    return true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    UiBinder.unbind(this);
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (item.getItemId() == R.id.addFavoriteMenuItem) {
      FavoritesModel.addFavorite(this.getTag().getId());
    }
    else if (item.getItemId() == R.id.removeFavoriteMenuItem) {
      FavoritesModel.removeFavorite(this.getTag().getId());
    }
    if (item.getItemId() == R.id.addTeachableTagMenuItem) {
      TeachableTagsModel.addTeachableTag(this.getTag().getId());
    }
    else if (item.getItemId() == R.id.removeTeachableTagMenuItem) {
      TeachableTagsModel.removeTeachableTag(this.getTag().getId());
    }
    else if (item.getItemId() == R.id.emailMenuItem) {
      Intent i = new Intent(Intent.ACTION_SEND);
      i.putExtra(Intent.EXTRA_SUBJECT, this.getTag().getTitle()
          + " - Tag Master for Android");
      i.putExtra(
          Intent.EXTRA_TEXT,
          String
              .format(
                  "Tag Title: %s\n%s\n\n\nSent from Tag Master for Android\nhttp://www.davidpoll.com/applications/tag-master",
                  this.getTag().getTitle(), this.getTag().getTagUri()));
      i.setType("text/plain");
      this.startActivity(i);
    }
    else if (item.getItemId() == R.id.smsMenuItem) {
      Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
      i.putExtra("sms_body", String.format("%s %s - Sent from Tag Master", this
          .getTag().getTitle(), this.getTag().getTagUri()));
      this.startActivity(i);
    }
    else if (item.getItemId() == R.id.refreshMenuItem) {
      this.loadQueryItem(true);
      return true;
    }
    return super.onMenuItemSelected(featureId, item);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.addFavoriteMenuItem).setVisible(
        !FavoritesModel.getIsFavorite(this.getTag().getId()));
    menu.findItem(R.id.removeFavoriteMenuItem).setVisible(
        FavoritesModel.getIsFavorite(this.getTag().getId()));
    menu.findItem(R.id.addTeachableTagMenuItem).setVisible(
        !TeachableTagsModel.getIsTeachableTag(this.getTag().getId()));
    menu.findItem(R.id.removeTeachableTagMenuItem).setVisible(
        TeachableTagsModel.getIsTeachableTag(this.getTag().getId()));
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  protected void onResume() {
    super.onResume();
    GoogleAnalyticsTracker.getInstance().trackPageView(
        "TagDetailActivity/"
            + this.getIntent().getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1));
  }

  public void setTag(Tag value) {
    this.tag.setValue(value);
  }
}
