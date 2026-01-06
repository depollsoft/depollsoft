package depollsoft.pitchperfect;

import com.bindroid.trackable.TrackableCollection;
import com.bindroid.trackable.TrackableField;

import depollsoft.pitchperfect.lib.Key;

public class KeySignatureModel {

  private TrackableField<Boolean> isMajor = new TrackableField<Boolean>(true);

  private TrackableField<TrackableCollection<Key>> majorKeys = new TrackableField<TrackableCollection<Key>>();

  private TrackableField<TrackableCollection<Key>> minorKeys = new TrackableField<TrackableCollection<Key>>();

  public KeySignatureModel() {
    this.setMajorKeys(Key.getMajorKeys());
    this.setMinorKeys(Key.getMinorKeys());
  }

  public boolean getIsMajor() {
    return this.isMajor.get();
  }

  public TrackableCollection<Key> getMajorKeys() {
    return this.majorKeys.get();
  }

  public TrackableCollection<Key> getMinorKeys() {
    return this.minorKeys.get();
  }

  public void setIsMajor(boolean value) {
    this.isMajor.set(value);
  }

  public void setMajorKeys(TrackableCollection<Key> value) {
    this.majorKeys.set(value);
  }

  public void setMinorKeys(TrackableCollection<Key> value) {
    this.minorKeys.set(value);
  }
}
