package depollsoft.pitchperfect.lib;

import depollsoft.lib.state.StateList;
import depollsoft.lib.state.StateField;

public class Key {
  private static StateList<Key> majorKeys;

  private static StateList<Key> minorKeys;

  public static StateList<Key> getMajorKeys() {
    if (Key.majorKeys == null) {
      Key.majorKeys = new StateList<Key>();
      Key.majorKeys.add(new Key(Note.findNote("G", Accidental.Flat, 4), KeyType.Major, -6));
      Key.majorKeys.add(new Key(Note.findNote("D", Accidental.Flat, 4), KeyType.Major, -5));
      Key.majorKeys.add(new Key(Note.findNote("A", Accidental.Flat, 4), KeyType.Major, -4));
      Key.majorKeys.add(new Key(Note.findNote("E", Accidental.Flat, 4), KeyType.Major, -3));
      Key.majorKeys.add(new Key(Note.findNote("B", Accidental.Flat, 4), KeyType.Major, -2));
      Key.majorKeys.add(new Key(Note.findNote("F", Accidental.Natural, 4), KeyType.Major, -1));
      Key.majorKeys.add(new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Major, 0));
      Key.majorKeys.add(new Key(Note.findNote("G", Accidental.Natural, 4), KeyType.Major, 1));
      Key.majorKeys.add(new Key(Note.findNote("D", Accidental.Natural, 4), KeyType.Major, 2));
      Key.majorKeys.add(new Key(Note.findNote("A", Accidental.Natural, 4), KeyType.Major, 3));
      Key.majorKeys.add(new Key(Note.findNote("E", Accidental.Natural, 4), KeyType.Major, 4));
      Key.majorKeys.add(new Key(Note.findNote("B", Accidental.Natural, 4), KeyType.Major, 5));
      Key.majorKeys.add(new Key(Note.findNote("F", Accidental.Sharp, 4), KeyType.Major, 6));
    }
    return Key.majorKeys;
  }

  public static StateList<Key> getMinorKeys() {
    if (Key.minorKeys == null) {
      Key.minorKeys = new StateList<Key>();
      Key.minorKeys.add(new Key(Note.findNote("E", Accidental.Flat, 4), KeyType.Minor, -6));
      Key.minorKeys.add(new Key(Note.findNote("B", Accidental.Flat, 4), KeyType.Minor, -5));
      Key.minorKeys.add(new Key(Note.findNote("F", Accidental.Natural, 4), KeyType.Minor, -4));
      Key.minorKeys.add(new Key(Note.findNote("C", Accidental.Natural, 4), KeyType.Minor, -3));
      Key.minorKeys.add(new Key(Note.findNote("G", Accidental.Natural, 4), KeyType.Minor, -2));
      Key.minorKeys.add(new Key(Note.findNote("D", Accidental.Natural, 4), KeyType.Minor, -1));
      Key.minorKeys.add(new Key(Note.findNote("A", Accidental.Natural, 4), KeyType.Minor, 0));
      Key.minorKeys.add(new Key(Note.findNote("E", Accidental.Natural, 4), KeyType.Minor, 1));
      Key.minorKeys.add(new Key(Note.findNote("B", Accidental.Natural, 4), KeyType.Minor, 2));
      Key.minorKeys.add(new Key(Note.findNote("F", Accidental.Sharp, 4), KeyType.Minor, 3));
      Key.minorKeys.add(new Key(Note.findNote("C", Accidental.Sharp, 4), KeyType.Minor, 4));
      Key.minorKeys.add(new Key(Note.findNote("G", Accidental.Sharp, 4), KeyType.Minor, 5));
      Key.minorKeys.add(new Key(Note.findNote("D", Accidental.Sharp, 4), KeyType.Minor, 6));
    }
    return Key.minorKeys;
  }

  private StateField<Note> note = new StateField<>(null);

  private StateField<KeyType> keyType = new StateField<>(null);

  private StateField<Integer> numAccidentals = new StateField<>(0);

  public Key() {
    this(Note.getC4(), KeyType.Major, 0);
  }

  public Key(Note n, KeyType t, int numAccidentals) {
    this.setNote(n);
    this.setKeyType(t);
    this.setNumAccidentals(numAccidentals);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Key))
      return false;
    Key k = (Key) o;
    return k.getNote().equals(this.getNote()) && k.getKeyType() == this.getKeyType();
  }

  public Accidental getAccidental() {
    return this.getNote().getAccidental();
  }

  public String getFriendlyName() {
    switch (this.getKeyType()) {
    case Major:
      return this.getNote().getFriendlyName().toUpperCase();
    case Minor:
      return this.getNote().getFriendlyName().toLowerCase();
    }
    return "";
  }

  public KeyType getKeyType() {
    return this.keyType.get();
  }

  public Note getNote() {
    return this.note.get();
  }

  public int getNumAccidentals() {
    return this.numAccidentals.get();
  }

  public void setKeyType(KeyType value) {
    this.keyType.set(value);
  }

  public void setNote(Note value) {
    this.note.set(value);
  }

  public void setNumAccidentals(int value) {
    this.numAccidentals.set(value);
  }
}
