package depollsoft.pitchperfect.lib;

import depollsoft.lib.binding.ObservableCollection;
import depollsoft.lib.binding.TrackableField;

public class Key {
  private static ObservableCollection<Key> majorKeys;

  private static ObservableCollection<Key> minorKeys;

  public static ObservableCollection<Key> getMajorKeys() {
    if (Key.majorKeys == null) {
      Key.majorKeys = new ObservableCollection<Key>();
      Key.majorKeys.add(new Key(Note.findNote("G", Accidental.Flat, 4),
          KeyType.Major, -6));
      Key.majorKeys.add(new Key(Note.findNote("D", Accidental.Flat, 4),
          KeyType.Major, -5));
      Key.majorKeys.add(new Key(Note.findNote("A", Accidental.Flat, 4),
          KeyType.Major, -4));
      Key.majorKeys.add(new Key(Note.findNote("E", Accidental.Flat, 4),
          KeyType.Major, -3));
      Key.majorKeys.add(new Key(Note.findNote("B", Accidental.Flat, 4),
          KeyType.Major, -2));
      Key.majorKeys.add(new Key(Note.findNote("F", Accidental.Natural, 4),
          KeyType.Major, -1));
      Key.majorKeys.add(new Key(Note.findNote("C", Accidental.Natural, 4),
          KeyType.Major, 0));
      Key.majorKeys.add(new Key(Note.findNote("G", Accidental.Natural, 4),
          KeyType.Major, 1));
      Key.majorKeys.add(new Key(Note.findNote("D", Accidental.Natural, 4),
          KeyType.Major, 2));
      Key.majorKeys.add(new Key(Note.findNote("A", Accidental.Natural, 4),
          KeyType.Major, 3));
      Key.majorKeys.add(new Key(Note.findNote("E", Accidental.Natural, 4),
          KeyType.Major, 4));
      Key.majorKeys.add(new Key(Note.findNote("B", Accidental.Natural, 4),
          KeyType.Major, 5));
      Key.majorKeys.add(new Key(Note.findNote("F", Accidental.Sharp, 4),
          KeyType.Major, 6));
    }
    return Key.majorKeys;
  }

  public static ObservableCollection<Key> getMinorKeys() {
    if (Key.minorKeys == null) {
      Key.minorKeys = new ObservableCollection<Key>();
      Key.minorKeys.add(new Key(Note.findNote("E", Accidental.Flat, 4),
          KeyType.Minor, -6));
      Key.minorKeys.add(new Key(Note.findNote("B", Accidental.Flat, 4),
          KeyType.Minor, -5));
      Key.minorKeys.add(new Key(Note.findNote("F", Accidental.Natural, 4),
          KeyType.Minor, -4));
      Key.minorKeys.add(new Key(Note.findNote("C", Accidental.Natural, 4),
          KeyType.Minor, -3));
      Key.minorKeys.add(new Key(Note.findNote("G", Accidental.Natural, 4),
          KeyType.Minor, -2));
      Key.minorKeys.add(new Key(Note.findNote("D", Accidental.Natural, 4),
          KeyType.Minor, -1));
      Key.minorKeys.add(new Key(Note.findNote("A", Accidental.Natural, 4),
          KeyType.Minor, 0));
      Key.minorKeys.add(new Key(Note.findNote("E", Accidental.Natural, 4),
          KeyType.Minor, 1));
      Key.minorKeys.add(new Key(Note.findNote("B", Accidental.Natural, 4),
          KeyType.Minor, 2));
      Key.minorKeys.add(new Key(Note.findNote("F", Accidental.Sharp, 4),
          KeyType.Minor, 3));
      Key.minorKeys.add(new Key(Note.findNote("C", Accidental.Sharp, 4),
          KeyType.Minor, 4));
      Key.minorKeys.add(new Key(Note.findNote("G", Accidental.Sharp, 4),
          KeyType.Minor, 5));
      Key.minorKeys.add(new Key(Note.findNote("D", Accidental.Sharp, 4),
          KeyType.Minor, 6));
    }
    return Key.minorKeys;
  }

  private TrackableField<Note> note = new TrackableField<Note>();

  private TrackableField<KeyType> keyType = new TrackableField<KeyType>();

  private TrackableField<Integer> numAccidentals = new TrackableField<Integer>(
      0);

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
    return k.getNote().equals(this.getNote())
        && k.getKeyType() == this.getKeyType();
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
    return this.keyType.getValue();
  }

  public Note getNote() {
    return this.note.getValue();
  }

  public int getNumAccidentals() {
    return this.numAccidentals.getValue();
  }

  public void setKeyType(KeyType value) {
    this.keyType.setValue(value);
  }

  public void setNote(Note value) {
    this.note.setValue(value);
  }

  public void setNumAccidentals(int value) {
    this.numAccidentals.setValue(value);
  }
}
