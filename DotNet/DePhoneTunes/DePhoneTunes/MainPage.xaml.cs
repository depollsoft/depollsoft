using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Microsoft.Phone.Controls;
using System.Windows.Controls.Primitives;
using System.ComponentModel;
using System.IO.IsolatedStorage;
using SLaB.Utilities;

namespace DePhoneTunes {
  public partial class MainPage : PhoneApplicationPage {
    private const string PanoMainKey = "MainPano_DefaultItemIndex";
    private List<object> scrolled = new List<object>();
    // Constructor
    public MainPage() {
      InitializeComponent();
      IsolatedStorageSettings.ApplicationSettings.SetIfContainsKey(PanoMainKey, (int val) => mainPano.DefaultItem = mainPano.Items[val]);
      songsItem.DataContext = SongsModel.Instance;
    }

    private void Panorama_SelectionChanged(object sender, SelectionChangedEventArgs e) {
      foreach (PanoramaItem item in e.RemovedItems)
        ((NotesModel)item.DataContext).IsCurrentModel = false;
      foreach (PanoramaItem item in e.AddedItems)
        ((NotesModel)item.DataContext).IsCurrentModel = true;
      Dispatcher.BeginInvoke(() => IsolatedStorageSettings.ApplicationSettings.SetIfNotInDesignMode(PanoMainKey, mainPano.SelectedIndex));
    }
    protected override void OnNavigatedFrom(System.Windows.Navigation.NavigationEventArgs e) {
      base.OnNavigatedFrom(e);
      try {
        ((NotesModel)((PanoramaItem)mainPano.SelectedItem).DataContext).IsCurrentModel = false;
        ((NotesModel)((PanoramaItem)mainPano.SelectedItem).DataContext).IsCurrentModel = true;
      } catch { }
    }

  }
}