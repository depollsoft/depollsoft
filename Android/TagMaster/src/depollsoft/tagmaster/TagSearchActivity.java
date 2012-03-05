package depollsoft.tagmaster;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.BindingMode;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.EditTextTextProperty;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.compat.ui.ActionBars;
import depollsoft.lib.json.JsonSerializer;
import depollsoft.tagmaster.barbershop.TagCollection;
import depollsoft.tagmaster.barbershop.TagSortOptions;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.Spinner;

public class TagSearchActivity extends Activity {

  private TrackableField<QueryModel> model = new TrackableField<QueryModel>(new QueryModel());

  public TagSearchActivity() {
    this.getModel().setMaxResults(Integer.MAX_VALUE);
  }

  public QueryModel getModel() {
    return this.model.getValue();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    System.gc();
    this.setContentView(R.layout.tagsearchview);

    UiBinder.bind(this, new EditTextTextProperty((EditText) this.findViewById(R.id.searchTextBox)),
        "Model.Query", BindingMode.TwoWay);

    ((EditText) this.findViewById(R.id.searchTextBox)).setOnKeyListener(new OnKeyListener() {
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN
            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
          TagSearchActivity.this.search();
          return true;
        }
        return false;
      }
    });

    this.findViewById(R.id.searchButton).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        TagSearchActivity.this.search();
      }
    });

    Spinner sheetMusicSpinner = (Spinner) this.findViewById(R.id.sheetMusicSpinner);
    sheetMusicSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String selectedValue = (String) arg0.getSelectedItem();
        if (selectedValue.equals("Not important"))
          TagSearchActivity.this.getModel().setHasSheetMusic(null);
        else if (selectedValue.equals("Yes"))
          TagSearchActivity.this.getModel().setHasSheetMusic(true);
        else
          TagSearchActivity.this.getModel().setHasSheetMusic(false);
      }

      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });

    Spinner learningTracksSpinner = (Spinner) this.findViewById(R.id.learningTracksSpinner);
    learningTracksSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String selectedValue = (String) arg0.getSelectedItem();
        if (selectedValue.equals("Not important"))
          TagSearchActivity.this.getModel().setHasLearningTracks(null);
        else if (selectedValue.equals("Yes"))
          TagSearchActivity.this.getModel().setHasLearningTracks(true);
        else
          TagSearchActivity.this.getModel().setHasLearningTracks(false);
      }

      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });

    Spinner partsSpinner = (Spinner) this.findViewById(R.id.partsSpinner);
    partsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String selectedValue = (String) arg0.getSelectedItem();
        if (selectedValue.equals("Any"))
          TagSearchActivity.this.getModel().setParts(null);
        else
          TagSearchActivity.this.getModel().setParts(Integer.parseInt(selectedValue));
      }

      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });

    Spinner tagCollectionSpinner = (Spinner) this.findViewById(R.id.tagCollectionSpinner);
    tagCollectionSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String selectedValue = (String) arg0.getSelectedItem();
        if (selectedValue.equals("Any"))
          TagSearchActivity.this.getModel().setCollection(null);
        else if (selectedValue.equals("Classic Tags"))
          TagSearchActivity.this.getModel().setCollection(TagCollection.ClassicTags);
        else
          TagSearchActivity.this.getModel().setCollection(TagCollection.EasyTags);
      }

      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });

    Spinner sortyBySpinner = (Spinner) this.findViewById(R.id.sortBySpinner);
    sortyBySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        String selectedValue = (String) arg0.getSelectedItem();
        if (selectedValue.equals("Title"))
          TagSearchActivity.this.getModel().setSortBy(TagSortOptions.Title);
        else if (selectedValue.equals("Downloads"))
          TagSearchActivity.this.getModel().setSortBy(TagSortOptions.Downloaded);
        else if (selectedValue.equals("Most recent"))
          TagSearchActivity.this.getModel().setSortBy(TagSortOptions.Posted);
        else if (selectedValue.equals("Rating"))
          TagSearchActivity.this.getModel().setSortBy(TagSortOptions.Rating);
        else
          TagSearchActivity.this.getModel().setSortBy(TagSortOptions.Classic);
      }

      public void onNothingSelected(AdapterView<?> arg0) {
      }
    });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    UiBinder.unbind(this);
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
    GoogleAnalyticsTracker.getInstance().trackPageView("TagSearchActivity");
  }

  private void search() {
    Intent searchResultsIntent = new Intent(TagSearchActivity.this, TagSearchResultsActivity.class);
    searchResultsIntent.putExtra(TagQueryActivity.QUERY_MODEL,
        JsonSerializer.serialize(TagSearchActivity.this.getModel()).toString());
    TagSearchActivity.this.startActivity(searchResultsIntent);
  }

  public void setModel(QueryModel value) {
    this.model.setValue(value);
  }
}
