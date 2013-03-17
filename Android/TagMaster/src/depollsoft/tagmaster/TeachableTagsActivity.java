package depollsoft.tagmaster;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.bindroid.Binding;
import com.bindroid.BindingMode;
import com.bindroid.converters.AdapterConverter;
import com.bindroid.converters.BoolConverter;
import com.bindroid.trackable.TrackableCollection;
import com.bindroid.ui.UiBinder;
import com.bindroid.utils.Function;
import com.bindroid.utils.Property;
import com.bindroid.utils.ReflectedProperty;
import com.flurry.android.FlurryAgent;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.compat.ui.ActionBars;

public class TeachableTagsActivity extends Activity {

  public TeachableTagsActivity() {
  }

  public TrackableCollection<Integer> getTeachableTags() {
    return TeachableTagsModel.getTeachableTagIds();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setContentView(R.layout.teachabletagsview);

    UiBinder.bind(this, R.id.teachableTagsItemsControl, "Adapter", "TeachableTags",
        new AdapterConverter(TeachableTagItemView.class, false, true));
    UiBinder.bind(new ReflectedProperty(this.findViewById(R.id.noTeachableTagsTextView),
        "Visibility"), new Property<Boolean>(new Function<Boolean>() {
      public Boolean evaluate() {
        return TeachableTagsActivity.this.getTeachableTags().size() == 0;
      }
    }, null, Boolean.class), BindingMode.ONE_WAY, BoolConverter.get());
    ActionBars.setCustomTitle(this, R.layout.titleview);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
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
  protected void onResume() {
    super.onResume();
    GoogleAnalyticsTracker.getInstance().trackPageView("TeachableTagsActivity");
  }

  @Override
  public boolean onSearchRequested() {
    Intent i = new Intent(this, TagSearchActivity.class);
    this.startActivity(i);
    return true;
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
}
