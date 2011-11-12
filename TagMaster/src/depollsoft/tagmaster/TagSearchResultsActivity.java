package depollsoft.tagmaster;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.binding.ui.UiBinder;
import depollsoft.lib.json.JsonSerializer;
import android.app.ActivityGroup;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.*;

public class TagSearchResultsActivity extends ActivityGroup
{

   private TrackableField<QueryModel> model = new TrackableField<QueryModel>();

   public TagSearchResultsActivity()
   {
   }

   public QueryModel getModel()
   {
      return this.model.getValue();
   }

   @Override
   protected void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.tagsearchresultsview);

      UiBinder.bind(this, R.id.queryTitleTextView, "Text", "Model.Query");

      this.setModel((QueryModel) JsonSerializer.deserialize(this.getIntent()
            .getStringExtra(TagQueryActivity.QUERY_MODEL)));

      Intent queryActivity = new Intent(this, TagQueryActivity.class);
      queryActivity.putExtras(this.getIntent().getExtras());
      Window w = this.getLocalActivityManager().startActivity("query",
            queryActivity);

      ((FrameLayout) this.findViewById(R.id.contentFrame)).addView(w
            .getDecorView());
   }

   @Override
   protected void onDestroy()
   {
      super.onDestroy();
      UiBinder.unbind(this);
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      GoogleAnalyticsTracker.getInstance().trackPageView(
            "TagSearchResultsActivity");
   }

   public void setModel(QueryModel value)
   {
      this.model.setValue(value);
   }

}
