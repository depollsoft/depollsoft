package depollsoft.tagmaster;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.TrackableField;
import depollsoft.lib.ui.ThreadSwitchContext;
import depollsoft.lib.util.Action;
import depollsoft.tagmaster.barbershop.Tag;
import depollsoft.tagmaster.barbershop.TagCollection;
import depollsoft.tagmaster.barbershop.TagQueryResult;
import depollsoft.tagmaster.barbershop.TagSortOptions;

public class QueryModel
{
   private TagQueryResult mostRecentResult;

   private TrackableField<Integer> maxResults = new TrackableField<Integer>();

   private TrackableField<String> statusText = new TrackableField<String>();

   private TrackableField<TagCollection> collection = new TrackableField<TagCollection>();

   private TrackableField<Boolean> hasMoreResults = new TrackableField<Boolean>();

   private TrackableField<Boolean> isLoading = new TrackableField<Boolean>();

   private TrackableField<String> query = new TrackableField<String>();

   private TrackableField<Integer> resultSetSize = new TrackableField<Integer>();

   private TrackableField<Integer> parts = new TrackableField<Integer>();

   private TrackableField<Boolean> hasLearningTracks = new TrackableField<Boolean>();

   private TrackableField<Boolean> hasSheetMusic = new TrackableField<Boolean>();

   private TrackableField<TagSortOptions> sortBy = new TrackableField<TagSortOptions>();

   private TrackableField<ObservableCollection<Tag>> tags = new TrackableField<ObservableCollection<Tag>>();

   private TrackableField<Double> minRating = new TrackableField<Double>();

   private TrackableField<Integer> minDownloads = new TrackableField<Integer>();

   public QueryModel()
   {
      this.mostRecentResult = new TagQueryResult();
      this.mostRecentResult.setStart(0);
      this.mostRecentResult.setCount(0);
      this.setIsLoading(false);
      this.setHasMoreResults(true);
      this.setResultSetSize(20);
      this.setMaxResults(50);
      this.setTags(new ObservableCollection<Tag>());
   }

   public void fetchResults(final ThreadSwitchContext context)
   {
      if (this.getIsLoading())
         return;
      if (!this.getHasMoreResults())
         return;
      this.setIsLoading(true);
      Tag.query(
            this.getQuery(),
            this.getResultSetSize(),
            this.mostRecentResult.getStart() + this.mostRecentResult.getCount(),
            this.getParts(), this.getHasLearningTracks(),
            this.getHasSheetMusic(), this.getCollection(), this.getSortBy(),
            this.getMinimumRating(), this.getMinimumDownloads()).continueWith(
            new Action<TagQueryResult>()
            {

               public void invoke(final TagQueryResult parameter)
               {
                  context.post(new Runnable()
                  {

                     public void run()
                     {
                        try
                        {
                           QueryModel.this.setStatusText(null);
                           QueryModel.this.mostRecentResult = parameter;
                           for (Tag t : parameter.getTags())
                              QueryModel.this.getTags().add(t);
                           if (QueryModel.this.mostRecentResult.getStart()
                                 + QueryModel.this.mostRecentResult.getCount() >= Math
                                 .min(QueryModel.this.mostRecentResult
                                       .getAvailable(), QueryModel.this
                                       .getMaxResults()))
                              QueryModel.this.setHasMoreResults(false);
                           else
                              QueryModel.this.setHasMoreResults(true);
                           if (parameter.getAvailable() == 0)
                              QueryModel.this
                                    .setStatusText("No tags could be found that matched your query.");
                        }
                        finally
                        {
                           QueryModel.this.setIsLoading(false);
                        }
                     }
                  });
               }
            }, new Action<Exception>()
            {

               public void invoke(final Exception parameter)
               {
                  context.post(new Runnable()
                  {

                     public void run()
                     {
                        QueryModel.this.setStatusText("An error has occurred: "
                              + parameter.getMessage());
                        QueryModel.this.setIsLoading(false);
                     }
                  });
               }
            });
   }

   public TagCollection getCollection()
   {
      return this.collection.getValue();
   }

   public Boolean getHasLearningTracks()
   {
      return this.hasLearningTracks.getValue();
   }

   public Boolean getHasMoreResults()
   {
      return this.hasMoreResults.getValue();
   }

   public Boolean getHasSheetMusic()
   {
      return this.hasSheetMusic.getValue();
   }

   public Boolean getIsLoading()
   {
      return this.isLoading.getValue();
   }

   public Integer getMaxResults()
   {
      return this.maxResults.getValue();
   }

   public Integer getMinimumDownloads()
   {
      return this.minDownloads.getValue();
   }

   public Double getMinimumRating()
   {
      return this.minRating.getValue();
   }

   public Integer getParts()
   {
      return this.parts.getValue();
   }

   public String getQuery()
   {
      return this.query.getValue();
   }

   public Integer getResultSetSize()
   {
      return this.resultSetSize.getValue();
   }

   public TagSortOptions getSortBy()
   {
      return this.sortBy.getValue();
   }

   public String getStatusText()
   {
      return this.statusText.getValue();
   }

   public ObservableCollection<Tag> getTags()
   {
      return this.tags.getValue();
   }

   public void setCollection(TagCollection value)
   {
      this.collection.setValue(value);
   }

   public void setHasLearningTracks(Boolean value)
   {
      this.hasLearningTracks.setValue(value);
   }

   public void setHasMoreResults(Boolean value)
   {
      this.hasMoreResults.setValue(value);
   }

   public void setHasSheetMusic(Boolean value)
   {
      this.hasSheetMusic.setValue(value);
   }

   public void setIsLoading(Boolean value)
   {
      this.isLoading.setValue(value);
   }

   public void setMaxResults(Integer value)
   {
      this.maxResults.setValue(value);
   }

   public void setMinimumDownloads(Integer value)
   {
      this.minDownloads.setValue(value);
   }

   public void setMinimumRating(Double value)
   {
      this.minRating.setValue(value);
   }

   public void setParts(Integer value)
   {
      this.parts.setValue(value);
   }

   public void setQuery(String value)
   {
      this.query.setValue(value);
   }

   public void setResultSetSize(Integer value)
   {
      this.resultSetSize.setValue(value);
   }

   public void setSortBy(TagSortOptions value)
   {
      this.sortBy.setValue(value);
   }

   public void setStatusText(String value)
   {
      this.statusText.setValue(value);
   }

   public void setTags(ObservableCollection<Tag> value)
   {
      this.tags.setValue(value);
   }
}
