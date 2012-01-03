package depollsoft.tagmaster;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.AdapterConverter;
import depollsoft.lib.binding.ui.BoolConverter;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.json.JsonSerializer;
import depollsoft.lib.ui.ThreadSwitchContext;

public class TagQueryActivity extends Activity {

  private TrackableField<QueryModel> model = new TrackableField<QueryModel>();
  public static final String QUERY_MODEL = "QueryModel";

  private TrackableField<Boolean> handleSearchButton = new TrackableField<Boolean>(
      true);

  public TagQueryActivity() {
  }

  public boolean getHandleSearchButton() {
    return this.handleSearchButton.getValue();
  }

  public QueryModel getModel() {
    return this.model.getValue();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.setContentView(R.layout.tagqueryview);

    try {
      ((ListView) this.findViewById(R.id.queryResultListView))
          .setOnScrollListener(new OnScrollListener() {

            public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
              if (TagQueryActivity.this.getModel() != null
                  && Math.abs(totalItemCount
                      - (firstVisibleItem + visibleItemCount)) < 2) {
                TagQueryActivity.this.getModel().fetchResults(
                    new ThreadSwitchContext(TagQueryActivity.this));
              }
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
          });
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    UiBinder.bind(this, R.id.queryResultListView, "Adapter", "Model.Tags",
        new AdapterConverter(TagItemView.class));

    UiBinder.bind(this, R.id.loadingProgressBar, "Visibility",
        "Model.IsLoading", BoolConverter.get());
    UiBinder.bind(this, R.id.loadingProgressBar, "Indeterminate",
        "Model.IsLoading", BoolConverter.get());

    UiBinder.bind(this, R.id.statusTextView, "Text", "Model.StatusText");
    UiBinder.bind(this, R.id.statusTextView, "Visibility", "Model.StatusText",
        BoolConverter.get());

    this.refresh();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    this.getMenuInflater().inflate(R.menu.mainmenu, menu);
    return true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    UiBinder.unbind(this);
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (item.getItemId() == R.id.refreshMenuItem) {
      this.refresh();
    }
    else if (item.getItemId() == R.id.searchMenuItem) {
      this.onSearchRequested();
    }
    return super.onMenuItemSelected(featureId, item);
  }

  @Override
  public boolean onSearchRequested() {
    Intent i = new Intent(this, TagSearchActivity.class);
    this.startActivity(i);
    return !this.getHandleSearchButton();
  }

  public void refresh() {
    String modelString = this.getIntent().getExtras()
        .getString(TagQueryActivity.QUERY_MODEL);
    this.setModel((QueryModel) JsonSerializer.deserialize(modelString));
    this.getModel().fetchResults(new ThreadSwitchContext(this));
  }

  public void setHandleSearchButton(boolean value) {
    this.handleSearchButton.setValue(value);
  }

  public void setModel(QueryModel value) {
    this.model.setValue(value);
  }

}
