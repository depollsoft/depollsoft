package depollsoft.tagmaster.barbershop;

import java.util.List;

import com.bindroid.trackable.TrackableField;

public class TagQueryResult {

  private TrackableField<List<Tag>> tags = new TrackableField<List<Tag>>();

  private TrackableField<Integer> start = new TrackableField<Integer>();

  private TrackableField<Integer> count = new TrackableField<Integer>();

  private TrackableField<Integer> available = new TrackableField<Integer>();

  public int getAvailable() {
    return this.available.get();
  }

  public int getCount() {
    return this.count.get();
  }

  public int getStart() {
    return this.start.get();
  }

  public List<Tag> getTags() {
    return this.tags.get();
  }

  public void setAvailable(int value) {
    this.available.set(value);
  }

  public void setCount(int value) {
    this.count.set(value);
  }

  public void setStart(int value) {
    this.start.set(value);
  }

  public void setTags(List<Tag> value) {
    this.tags.set(value);
  }
}
