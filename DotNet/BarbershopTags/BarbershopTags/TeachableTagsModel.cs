using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO.IsolatedStorage;
using SLaB.Utilities;

namespace BarbershopTags {
  public class TeachableTagsModel : INotifyPropertyChanged {
    private static ObservableCollection<int> _TeachableIds;

    static TeachableTagsModel() {
      if (!DesignerProperties.IsInDesignTool) {
        IsolatedStorageSettings.ApplicationSettings.SetIfContainsKey<ObservableCollection<int>>("TeachableIds", val => _TeachableIds = val);
        if (_TeachableIds == null) {
          _TeachableIds = new ObservableCollection<int>();
          IsolatedStorageSettings.ApplicationSettings.SetIfNotInDesignMode("TeachableIds", _TeachableIds);
        }
      } else {
        _TeachableIds = new ObservableCollection<int>();
      }
    }
    public TeachableTagsModel() {
      TeachableTagIds = new ReadOnlyObservableCollection<int>(_TeachableIds);
    }

    public static void ReplaceTeachables(IEnumerable<int> newTeachables) {
      _TeachableIds.Clear();
      foreach (var i in newTeachables) {
        _TeachableIds.Add(i);
      }
    }

    public static void AddTeachable(int id) {
      if (!_TeachableIds.Contains(id)) {
        _TeachableIds.Add(id);
        new SettingsModel().OnChange();
      }
    }

    public static bool IsTeachable(int id) {
      return _TeachableIds.Contains(id);
    }

    public static void RemoveTeachable(int id) {
      if (_TeachableIds.Remove(id)) {
        new SettingsModel().OnChange();
      }
    }

    public static bool CanMoveUp(int id) {
      var index = _TeachableIds.IndexOf(id);
      return index > 0;
    }

    public static bool CanMoveDown(int id) {
      var index = _TeachableIds.IndexOf(id);
      return index < _TeachableIds.Count - 1;
    }

    public static void MoveUp(int id) {
      var index = _TeachableIds.IndexOf(id);
      _TeachableIds.RemoveAt(index);
      _TeachableIds.Insert(index - 1, id);
      new SettingsModel().OnChange();
    }

    public static void MoveDown(int id) {
      var index = _TeachableIds.IndexOf(id);
      _TeachableIds.RemoveAt(index);
      _TeachableIds.Insert(index + 1, id);
      new SettingsModel().OnChange();
    }

    public static void ResetTeachables() {
      _TeachableIds.Clear();
      new SettingsModel().OnChange();
    }

    private IEnumerable<int> _TeachableTagIds;
    public IEnumerable<int> TeachableTagIds {
      get {
        return _TeachableTagIds;
      }
      private set {
        if (!EqualityComparer<IEnumerable<int>>.Default.Equals(_TeachableTagIds, value)) {
          _TeachableTagIds = value;
          PropertyChanged.Raise(this, new PropertyChangedEventArgs("TeachableTagIds"));
        }
      }
    }
    public event PropertyChangedEventHandler PropertyChanged;
  }
}
