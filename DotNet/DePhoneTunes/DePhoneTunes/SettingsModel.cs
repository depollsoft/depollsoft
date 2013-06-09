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

namespace DePhoneTunes {
  public class SettingsModel : INotifyPropertyChanged {

    public bool UseToggleNotes {
      get {
        if (DesignerProperties.IsInDesignTool || !IsolatedStorageSettings.ApplicationSettings.Contains("UseToggleNotes"))
          return false;
        return (bool)IsolatedStorageSettings.ApplicationSettings["UseToggleNotes"];
      }
      set {
        if (!EqualityComparer<bool>.Default.Equals(UseToggleNotes, value)) {
          IsolatedStorageSettings.ApplicationSettings["UseToggleNotes"] = value;
          PropertyChanged.Raise(this, new PropertyChangedEventArgs("UseToggleNotes"));
        }
      }
    }
    public event PropertyChangedEventHandler PropertyChanged;
  }
}
