package depollsoft.tagmaster;

import depollsoft.lib.json.JsonSerializer;
import depollsoft.tagmaster.barbershop.TagCollection;
import depollsoft.tagmaster.barbershop.TagSortOptions;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;

public class TagBrowser extends TabActivity
{
   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      this.setContentView(R.layout.tagmasterview);

      Intent latest = new Intent(this, TagQueryActivity.class);
      QueryModel latestModel = new QueryModel();
      latestModel.setMaxResults(Integer.MAX_VALUE);
      latestModel.setSortBy(TagSortOptions.Posted);
      latest.putExtra(TagQueryActivity.QUERY_MODEL,
            JsonSerializer.serialize(latestModel).toString());
      this.getTabHost().addTab(
            this.getTabHost().newTabSpec("latest").setContent(latest)
                  .setIndicator("Latest"));

      Intent rating = new Intent(this, TagQueryActivity.class);
      QueryModel ratingModel = new QueryModel();
      ratingModel.setMaxResults(Integer.MAX_VALUE);
      ratingModel.setSortBy(TagSortOptions.Rating);
      rating.putExtra(TagQueryActivity.QUERY_MODEL,
            JsonSerializer.serialize(ratingModel).toString());
      this.getTabHost().addTab(
            this.getTabHost().newTabSpec("rating").setContent(rating)
                  .setIndicator("Rating"));

      Intent downloads = new Intent(this, TagQueryActivity.class);
      QueryModel downloadsModel = new QueryModel();
      downloadsModel.setMaxResults(Integer.MAX_VALUE);
      downloadsModel.setSortBy(TagSortOptions.Downloaded);
      downloads.putExtra(TagQueryActivity.QUERY_MODEL, JsonSerializer
            .serialize(downloadsModel).toString());
      this.getTabHost().addTab(
            this.getTabHost().newTabSpec("downloads").setContent(downloads)
                  .setIndicator("Downloads"));

      Intent classic = new Intent(this, TagQueryActivity.class);
      QueryModel classicModel = new QueryModel();
      classicModel.setSortBy(TagSortOptions.Classic);
      classicModel.setCollection(TagCollection.ClassicTags);
      classicModel.setMaxResults(400);
      classic.putExtra(TagQueryActivity.QUERY_MODEL,
            JsonSerializer.serialize(classicModel).toString());
      this.getTabHost().addTab(
            this.getTabHost().newTabSpec("classic").setContent(classic)
                  .setIndicator("Classic"));
   }
}