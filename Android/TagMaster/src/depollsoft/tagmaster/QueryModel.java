package depollsoft.tagmaster;

import com.bindroid.trackable.TrackableCollection;
import com.bindroid.trackable.TrackableField;
import com.bindroid.utils.Action;

import bolts.Continuation;
import bolts.Task;
import depollsoft.lib.ui.ThreadSwitchContext;
import depollsoft.tagmaster.barbershop.Tag;
import depollsoft.tagmaster.barbershop.TagCollection;
import depollsoft.tagmaster.barbershop.TagQueryResult;
import depollsoft.tagmaster.barbershop.TagSortOptions;

public class QueryModel {
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

  private TrackableField<TrackableCollection<Tag>> tags = new TrackableField<TrackableCollection<Tag>>();

  private TrackableField<Double> minRating = new TrackableField<Double>();

  private TrackableField<Integer> minDownloads = new TrackableField<Integer>();

  public QueryModel() {
    this.mostRecentResult = new TagQueryResult();
    this.mostRecentResult.setStart(0);
    this.mostRecentResult.setCount(0);
    this.setIsLoading(false);
    this.setHasMoreResults(true);
    this.setResultSetSize(20);
    this.setMaxResults(50);
    this.setTags(new TrackableCollection<Tag>());
  }

  public void fetchResults(final ThreadSwitchContext context) {
    if (this.getIsLoading())
      return;
    if (!this.getHasMoreResults())
      return;
    this.setIsLoading(true);
    Tag.query(this.getQuery(), this.getResultSetSize(),
            this.mostRecentResult.getStart() + this.mostRecentResult.getCount(), this.getParts(),
            this.getHasLearningTracks(), this.getHasSheetMusic(), this.getCollection(),
            this.getSortBy(), this.getMinimumRating(), this.getMinimumDownloads())
            .continueWith(new Continuation<TagQueryResult, Void>() {
              @Override
              public Void then(final Task<TagQueryResult> task) throws Exception {
                if (task.isFaulted()) {
                  context.post(new Runnable() {

                    public void run() {
                      QueryModel.this.setStatusText("An error has occurred: " + task.getError().getMessage());
                      QueryModel.this.setIsLoading(false);
                    }
                  });
                } else {
                  context.post(new Runnable() {

                    public void run() {
                      try {
                        QueryModel.this.setStatusText(null);
                        QueryModel.this.mostRecentResult = task.getResult();
                        for (Tag t : task.getResult().getTags())
                          QueryModel.this.getTags().add(t);
                        if (QueryModel.this.mostRecentResult.getStart()
                                + QueryModel.this.mostRecentResult.getCount() >= Math.min(
                                QueryModel.this.mostRecentResult.getAvailable(),
                                QueryModel.this.getMaxResults()))
                          QueryModel.this.setHasMoreResults(false);
                        else
                          QueryModel.this.setHasMoreResults(true);
                        if (task.getResult().getAvailable() == 0)
                          QueryModel.this
                                  .setStatusText("No tags could be found that matched your query.");
                      } finally {
                        QueryModel.this.setIsLoading(false);
                      }
                    }
                  });
                }
                return null;
              }
            });
  }

  public TagCollection getCollection() {
    return this.collection.get();
  }

  public Boolean getHasLearningTracks() {
    return this.hasLearningTracks.get();
  }

  public Boolean getHasMoreResults() {
    return this.hasMoreResults.get();
  }

  public Boolean getHasSheetMusic() {
    return this.hasSheetMusic.get();
  }

  public Boolean getIsLoading() {
    return this.isLoading.get();
  }

  public Integer getMaxResults() {
    return this.maxResults.get();
  }

  public Integer getMinimumDownloads() {
    return this.minDownloads.get();
  }

  public Double getMinimumRating() {
    return this.minRating.get();
  }

  public Integer getParts() {
    return this.parts.get();
  }

  public String getQuery() {
    return this.query.get();
  }

  public Integer getResultSetSize() {
    return this.resultSetSize.get();
  }

  public TagSortOptions getSortBy() {
    return this.sortBy.get();
  }

  public String getStatusText() {
    return this.statusText.get();
  }

  public TrackableCollection<Tag> getTags() {
    return this.tags.get();
  }

  public void setCollection(TagCollection value) {
    this.collection.set(value);
  }

  public void setHasLearningTracks(Boolean value) {
    this.hasLearningTracks.set(value);
  }

  public void setHasMoreResults(Boolean value) {
    this.hasMoreResults.set(value);
  }

  public void setHasSheetMusic(Boolean value) {
    this.hasSheetMusic.set(value);
  }

  public void setIsLoading(Boolean value) {
    this.isLoading.set(value);
  }

  public void setMaxResults(Integer value) {
    this.maxResults.set(value);
  }

  public void setMinimumDownloads(Integer value) {
    this.minDownloads.set(value);
  }

  public void setMinimumRating(Double value) {
    this.minRating.set(value);
  }

  public void setParts(Integer value) {
    this.parts.set(value);
  }

  public void setQuery(String value) {
    this.query.set(value);
  }

  public void setResultSetSize(Integer value) {
    this.resultSetSize.set(value);
  }

  public void setSortBy(TagSortOptions value) {
    this.sortBy.set(value);
  }

  public void setStatusText(String value) {
    this.statusText.set(value);
  }

  public void setTags(TrackableCollection<Tag> value) {
    this.tags.set(value);
  }
}
