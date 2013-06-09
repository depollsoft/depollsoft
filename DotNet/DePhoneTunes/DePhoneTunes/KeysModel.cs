using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using Microsoft.Phone.Shell;
using SLaB.Utilities;

namespace DePhoneTunes {
  public class KeysModel : NotesModel {
    public KeysModel() {
      constructingCount++;
      _MajorKeys = new ObservableCollection<Note>();
      _MajorKeys.Add(FindNote("G", Accidental.Flat));
      _MajorKeys.Add(FindNote("D", Accidental.Flat));
      _MajorKeys.Add(FindNote("A", Accidental.Flat));
      _MajorKeys.Add(FindNote("E", Accidental.Flat));
      _MajorKeys.Add(FindNote("B", Accidental.Flat));
      _MajorKeys.Add(FindNote("F"));
      _MajorKeys.Add(FindNote("C"));
      _MajorKeys.Add(FindNote("G"));
      _MajorKeys.Add(FindNote("D"));
      _MajorKeys.Add(FindNote("A"));
      _MajorKeys.Add(FindNote("E"));
      _MajorKeys.Add(FindNote("B"));
      _MajorKeys.Add(FindNote("F", Accidental.Sharp));

      _MinorKeys = new ObservableCollection<Note>();
      _MinorKeys.Add(FindNote("E", Accidental.Flat));
      _MinorKeys.Add(FindNote("B", Accidental.Flat));
      _MinorKeys.Add(FindNote("F"));
      _MinorKeys.Add(FindNote("C"));
      _MinorKeys.Add(FindNote("G"));
      _MinorKeys.Add(FindNote("D"));
      _MinorKeys.Add(FindNote("A"));
      _MinorKeys.Add(FindNote("E"));
      _MinorKeys.Add(FindNote("B"));
      _MinorKeys.Add(FindNote("F", Accidental.Sharp));
      _MinorKeys.Add(FindNote("C", Accidental.Sharp));
      _MinorKeys.Add(FindNote("G", Accidental.Sharp));
      _MinorKeys.Add(FindNote("D", Accidental.Sharp));

      Key = KeyType.Minor;
      Key = KeyType.Major;
      if (!DesignerProperties.IsInDesignTool) {
        PhoneApplicationService.Current.State.SetIfContainsKey("KeysModel.Key", (KeyType key) => Key = key);
        PhoneApplicationService.Current.State.SetIfContainsKey(GetType().ToString() + ".SelectedNoteIndex", (int i) => SelectedNoteIndex = i);
      }
      constructingCount--;
    }


    private ObservableCollection<Note> _MinorKeys;
    public ObservableCollection<Note> MinorKeys {
      get {
        return _MinorKeys;
      }
      set {
        if (!EqualityComparer<ObservableCollection<Note>>.Default.Equals(_MinorKeys, value)) {
          _MinorKeys = value;
          _PropertyChanged.Raise(this, new PropertyChangedEventArgs("MinorKeys"));
        }
      }
    }

    private ObservableCollection<Note> _MajorKeys;
    public ObservableCollection<Note> MajorKeys {
      get {
        return _MajorKeys;
      }
      set {
        if (!EqualityComparer<ObservableCollection<Note>>.Default.Equals(_MajorKeys, value)) {
          _MajorKeys = value;
          _PropertyChanged.Raise(this, new PropertyChangedEventArgs("MajorKeys"));
        }
      }
    }

    private KeyType _Key;
    public KeyType Key {
      get {
        return _Key;
      }
      set {
        if (!EqualityComparer<KeyType>.Default.Equals(_Key, value)) {
          _Key = value;
          if (constructingCount == 0)
            PhoneApplicationService.Current.State.SetIfNotInDesignMode("KeysModel.Key", Key);
          switch (Key) {
            case KeyType.Major:
              Notes = _MajorKeys;
              SelectedNote = _MajorKeys[_MajorKeys.Count / 2];
              break;
            case KeyType.Minor:
              Notes = _MinorKeys;
              SelectedNote = _MinorKeys[_MinorKeys.Count / 2];
              break;
          }
          _PropertyChanged.Raise(this, new PropertyChangedEventArgs("Key"));
        }
      }
    }
  }

  public enum KeyType { Major, Minor }
}
