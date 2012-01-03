package depollsoft.tagmaster.barbershop;

import java.util.List;

import depollsoft.lib.binding.TrackableField;

public class TagQueryResult {

  private TrackableField<List<Tag>> tags = new TrackableField<List<Tag>>();

  private TrackableField<Integer> start = new TrackableField<Integer>();

  private TrackableField<Integer> count = new TrackableField<Integer>();

  private TrackableField<Integer> available = new TrackableField<Integer>();

  public int getAvailable() {
    return this.available.getValue();
  }

  public int getCount() {
    return this.count.getValue();
  }

  public int getStart() {
    return this.start.getValue();
  }

  public List<Tag> getTags() {
    return this.tags.getValue();
  }

  public void setAvailable(int value) {
    this.available.setValue(value);
  }

  public void setCount(int value) {
    this.count.setValue(value);
  }

  public void setStart(int value) {
    this.start.setValue(value);
  }

  public void setTags(List<Tag> value) {
    this.tags.setValue(value);
  }
}
