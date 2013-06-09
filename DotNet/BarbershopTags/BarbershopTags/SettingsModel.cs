using System;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using System.ComponentModel;
using SLaB.Utilities;
using System.Collections.Generic;
using System.IO.IsolatedStorage;
using Microsoft.Phone.Shell;
using System.Threading.Tasks;
using Parse;

namespace BarbershopTags {
  public class SettingsModel : INotifyPropertyChanged {

    public double GlobalVolume {
      get {
        if (DesignerProperties.IsInDesignTool || !IsolatedStorageSettings.ApplicationSettings.Contains("GlobalVolume"))
          return 100;
        return (double)IsolatedStorageSettings.ApplicationSettings["GlobalVolume"];
      }
      set {
        value = Math.Max(value, 0);
        value = Math.Min(value, 100);
        IsolatedStorageSettings.ApplicationSettings["GlobalVolume"] = value;
        PropertyChanged.Raise(this, new PropertyChangedEventArgs("GlobalVolume"));
      }
    }

    [TypeConverter(typeof(NullableBoolConverter))]
    public bool? CanRunInBackground {
      get {
        if (DesignerProperties.IsInDesignTool || !IsolatedStorageSettings.ApplicationSettings.Contains("CanRunInBackground"))
          return null;
        return (bool)IsolatedStorageSettings.ApplicationSettings["CanRunInBackground"];
      }
      set {
        if (value == null)
          IsolatedStorageSettings.ApplicationSettings.Remove("CanRunInBackground");
        else {
          IsolatedStorageSettings.ApplicationSettings["CanRunInBackground"] = value.Value;
          PhoneApplicationService.Current.ApplicationIdleDetectionMode = value.Value ? IdleDetectionMode.Enabled : IdleDetectionMode.Disabled;
        }
        PropertyChanged.Raise(this, new PropertyChangedEventArgs("CanRunInBackground"));
      }
    }

    public bool IsLoggedIn {
      get {
        return ParseUser.CurrentUser != null;
      }
    }

    public void UpdateBindings() {
      PropertyChanged.Raise(this, new PropertyChangedEventArgs(null));
    }

    public void LogOut() {
      ParseUser.LogOut();
      PropertyChanged.Raise(this, new PropertyChangedEventArgs("IsLoggedIn"));
    }

    public async void OnChange() {
      SetChangeTime();
      await SaveToParseUser();
    }

    public void SetChangeTime() {
      IsolatedStorageSettings.ApplicationSettings["ChangeTime"] = DateTime.Now;
    }

    private DateTime GetChangeTime() {
      DateTime time;
      IsolatedStorageSettings.ApplicationSettings.TryGetValue("ChangeTime", out time);
      return time;
    }

    public async Task StartupRestore() {
      if (ParseUser.CurrentUser != null) {
        try {
          await ParseUser.CurrentUser.FetchAsync();
          if (ParseUser.CurrentUser.UpdatedAt > GetChangeTime()) {
            RestoreFromParseUser();
          } else {
            await SaveToParseUser();
          }
        } catch { }
      }
    }

    public async Task SaveToParseUser() {
      if (ParseUser.CurrentUser != null) {
        ParseUser.CurrentUser["FavoriteIds"] = new List<int>(new FavoritesModel().FavoriteTagIds);
        ParseUser.CurrentUser["TeachableIds"] = new List<int>(new TeachableTagsModel().TeachableTagIds);
        try {
          await ParseUser.CurrentUser.SaveAsync();
        } catch { }
      }
    }

    public void RestoreFromParseUser() {
      if (ParseUser.CurrentUser != null) {
        var favorites = ParseUser.CurrentUser.Get<IList<int>>("FavoriteIds");
        var teachables = ParseUser.CurrentUser.Get<IList<int>>("TeachableIds");
        FavoritesModel.ReplaceFavorites(favorites);
        TeachableTagsModel.ReplaceTeachables(teachables);
      }
    }
    public event PropertyChangedEventHandler PropertyChanged;
  }
}
