package depollsoft.tagmaster;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.Binding;
import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.ui.AdapterConverter;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.util.Function;
import depollsoft.lib.util.Property;
import depollsoft.lib.util.ReflectedProperty;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class TeachableTagsActivity extends Activity {

  public TeachableTagsActivity() {
  }

  public ObservableCollection<Integer> getTeachableTags() {
    return TeachableTagsModel.getTeachableTagIds();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.setContentView(R.layout.teachabletagsview);

    UiBinder.bind(this, R.id.teachableTagsItemsControl, "Adapter",
        "TeachableTags", new AdapterConverter(TeachableTagItemView.class,
            false, true));
    UiBinder.registerBinding(
        this,
        new Binding(new ReflectedProperty(this
            .findViewById(R.id.noTeachableTagsTextView), "Visibility"),
            new Property<Boolean>(new Function<Boolean>() {
              public Boolean evaluate() {
                return TeachableTagsActivity.this.getTeachableTags().size() == 0;
              }
            }, null, Boolean.class), BindingMode.OneWay, BoolConverter.get())
            .bind(this));
  }

  @Override
  protected void onDestroy() {
    UiBinder.unbind(this);
    super.onDestroy();
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
}
