package depollsoft.lib.binding;

import depollsoft.lib.util.EqualityComparer;
import depollsoft.lib.util.ObjectUtilities;

public class ComparableTrackableField<T> extends TrackableField<T> {
  private EqualityComparer<T> comparer;

  public ComparableTrackableField() {
    this(null);
  }

  public ComparableTrackableField(T initialValue) {
    this(initialValue, ObjectUtilities.<T> getDefaultComparer());
  }

  public ComparableTrackableField(T initialValue, EqualityComparer<T> comparer) {
    super(initialValue);
    this.comparer = comparer;
  }

  public void setValue(T value) {
    if (!this.comparer.equals(this.value, value)) {
      this.value = value;
      this.updateTrackers();
    }
  }
}
