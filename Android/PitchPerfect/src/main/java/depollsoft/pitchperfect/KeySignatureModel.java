package depollsoft.pitchperfect;

import depollsoft.lib.state.StateList;
import depollsoft.lib.state.StateField;

import depollsoft.pitchperfect.lib.Key;

public class KeySignatureModel {

  private StateField<Boolean> isMajor = new StateField<>(true);

  private StateField<StateList<Key>> majorKeys = new StateField<>(null);

  private StateField<StateList<Key>> minorKeys = new StateField<>(null);

  public KeySignatureModel() {
    this.setMajorKeys(Key.getMajorKeys());
    this.setMinorKeys(Key.getMinorKeys());
  }

  public boolean getIsMajor() {
    return this.isMajor.get();
  }

  public StateList<Key> getMajorKeys() {
    return this.majorKeys.get();
  }

  public StateList<Key> getMinorKeys() {
    return this.minorKeys.get();
  }

  public void setIsMajor(boolean value) {
    this.isMajor.set(value);
  }

  public void setMajorKeys(StateList<Key> value) {
    this.majorKeys.set(value);
  }

  public void setMinorKeys(StateList<Key> value) {
    this.minorKeys.set(value);
  }
}
