package depollsoft.tagmaster;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.flurry.android.FlurryAgent;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.Binding;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.compat.ui.Activities;
import depollsoft.lib.compat.ui.CompatTabHostWrapper;
import depollsoft.lib.compat.ui.MenuItems;
import depollsoft.lib.ui.ThreadSwitchProperty;
import depollsoft.lib.util.Action;
import depollsoft.lib.util.ContentCache;
import depollsoft.lib.util.IntentUtilities;
import depollsoft.lib.util.ReflectedProperty;
import depollsoft.tagmaster.barbershop.RemoteLocation;
import depollsoft.tagmaster.barbershop.Tag;

public class TagDetailActivity extends TabActivity {
  public static final String TAG_ID_EXTRA = "depollsoft.tagmaster.tagid";
  private CompatTabHostWrapper tabHost;
  private TrackableField<Tag> tag = new TrackableField<Tag>();
  private ProgressDialog progress;

  public TagDetailActivity() {
  }

  private Intent getEmailIntent() {
    Intent i = new Intent(Intent.ACTION_SEND);
    i.putExtra(Intent.EXTRA_SUBJECT, this.getTag().getTitle() + " - Tag Master for Android");
    i.putExtra(
        Intent.EXTRA_TEXT,
        String
            .format(
                "Tag Title: %s\n%s\n\n\nSent from Tag Master for Android\nhttp://www.davidpoll.com/applications/tag-master",
                this.getTag().getTitle(), this.getTag().getTagUri()));
    i.setType("text/plain");
    return i;
  }

  private Intent getSmsIntent() {
    Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
    i.putExtra("sms_body", String.format("%s %s - Sent from Tag Master", this.getTag().getTitle(),
        this.getTag().getTagUri()));
    return i;
  }

  public Tag getTag() {
    return this.tag.getValue();
  }

  public int getTagId() {
    return this.getIntent().getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1);
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

    int tagId = this.getIntent().getExtras().getInt(TagDetailActivity.TAG_ID_EXTRA);

    Tag.loadTagById(tagId, refresh).continueWith(new Action<Tag>() {

      public void invoke(final Tag parameter) {
        TagDetailActivity.this.runOnUiThread(new Runnable() {

          public void run() {
            TagDetailActivity.this.setTag(null);
            TagDetailActivity.this.setTag(parameter);
            TagDetailActivity.this.progress.dismiss();
            Activities.invalidateOptionsMenu(TagDetailActivity.this);
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

    this.tabHost = new CompatTabHostWrapper(this, this.getTabHost());

    this.progress = new ProgressDialog(this);

    Intent summaryIntent = new Intent(this, TagSummaryActivity.class);
    summaryIntent.putExtras(this.getIntent().getExtras());
    this.tabHost.addTab(this.tabHost.newTabSpec("summary").setContent(summaryIntent)
        .setIndicator("Summary"));

    Intent detailsIntent = new Intent(this, TagMiscActivity.class);
    detailsIntent.putExtras(this.getIntent().getExtras());
    this.tabHost.addTab(this.tabHost.newTabSpec("details").setContent(detailsIntent)
        .setIndicator("Details"));

    Intent tracksIntent = new Intent(this, TagTracksActivity.class);
    detailsIntent.putExtras(this.getIntent().getExtras());
    this.tabHost.addTab(this.tabHost.newTabSpec("tracks").setContent(tracksIntent)
        .setIndicator("Tracks"));

    Intent videosIntent = new Intent(this, TagVideosActivity.class);
    videosIntent.putExtras(this.getIntent().getExtras());
    this.tabHost.addTab(this.tabHost.newTabSpec("videos").setContent(videosIntent)
        .setIndicator("Videos"));

    UiBinder.registerBinding(this, new Binding(new ThreadSwitchProperty<Object>(
        new ReflectedProperty(this, "Title"), this), new ReflectedProperty(this, "Tag.Title"))
        .bind(this));

    UiBinder.bind(this, R.id.tabContentHolder, "Visibility", "Tag", BoolConverter.get());

    this.loadQueryItem(false);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (this.getTag() == null)
      return false;
    this.getMenuInflater().inflate(R.menu.tagdetailmenu, menu);
    MenuItems.setShowAsAction(menu.findItem(R.id.addFavoriteMenuItem),
        MenuItems.SHOW_AS_ACTION_IF_ROOM);
    MenuItems.setShowAsAction(menu.findItem(R.id.removeFavoriteMenuItem),
        MenuItems.SHOW_AS_ACTION_IF_ROOM);
    MenuItems.setShowAsAction(menu.findItem(R.id.smsMenuItem), MenuItems.SHOW_AS_ACTION_IF_ROOM);
    menu.findItem(R.id.smsMenuItem).setVisible(
        IntentUtilities.isIntentAvailable(this, this.getSmsIntent()));
    menu.findItem(R.id.emailMenuItem).setVisible(
        IntentUtilities.isIntentAvailable(this, this.getEmailIntent()));
    return true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    UiBinder.unbind(this);
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    try {
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
        Intent i = this.getEmailIntent();
        this.startActivity(i);
      }
      else if (item.getItemId() == R.id.smsMenuItem) {
        Intent i = this.getSmsIntent();
        this.startActivity(i);
      }
      else if (item.getItemId() == R.id.refreshMenuItem) {
        this.loadQueryItem(true);
        return true;
      }
      return super.onMenuItemSelected(featureId, item);
    }
    finally {
      Activities.invalidateOptionsMenu(this);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == ActionBars.HOME_MENU_ITEM_ID) {
      Intent intent = new Intent(this, MeActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      this.startActivity(intent);
      return true;
    }
    return super.onOptionsItemSelected(item);
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
  protected void onRestoreInstanceState(Bundle state) {
    super.onRestoreInstanceState(state);
    this.tabHost.restoreInstanceState("tabs", state);
  }

  @Override
  protected void onResume() {
    super.onResume();
    GoogleAnalyticsTracker.getInstance().trackPageView(
        "TagDetailActivity/" + this.getIntent().getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1));
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    this.tabHost.saveInstanceState("tabs", outState);
  }

  @Override
  protected void onStart() {
    super.onStart();
    FlurryAgent.onStartSession(this, "V5L1948BNDQCKZFPARJ9");
  }

  @Override
  protected void onStop() {
    super.onStop();
    FlurryAgent.onEndSession(this);
  }

  public void setTag(Tag value) {
    this.tag.setValue(value);
  }
}
