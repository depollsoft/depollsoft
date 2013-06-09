using System.Windows;
using Microsoft.Phone.Controls;
using System;

namespace BarbershopTags {
  public partial class SettingsPage : PhoneApplicationPage {
    private SettingsModel _Model;
    public SettingsPage() {
      _Model = new SettingsModel();
      InitializeComponent();
      LayoutRoot.DataContext = _Model;
    }

    protected override void OnNavigatedTo(System.Windows.Navigation.NavigationEventArgs e) {
      base.OnNavigatedTo(e);
      RefreshCacheSize();
      _Model.UpdateBindings();
    }

    private void RefreshCacheSize() {
      CacheSize.Text = string.Format("Current cache size: {0:0.#} KB", 1.0 * Barbershop.Tag.CurrentCacheSize / 1024);
    }

    private void ClearCache(object sender, RoutedEventArgs e) {
      Barbershop.Tag.ClearCache();
      RefreshCacheSize();
    }

    private void ResetFavorites(object sender, RoutedEventArgs e) {
      FavoritesModel.ResetFavorites();
    }

    private void IncreaseVolume(object sender, RoutedEventArgs e) {
      _Model.GlobalVolume++;
    }

    private void DecreaseVolume(object sender, RoutedEventArgs e) {
      _Model.GlobalVolume--;
    }

    private void ResetRatings(object sender, RoutedEventArgs e) {
      RatingsModel.ResetRatings();
    }

    private void ResetTeachables(object sender, RoutedEventArgs e) {
      TeachableTagsModel.ResetTeachables();
    }

    private void LogInWithFacebook(object sender, RoutedEventArgs e) {
      NavigationService.Navigate(new Uri("/FacebookLoginPage.xaml", UriKind.Relative));
    }

    private void LogOut(object sender, RoutedEventArgs e) {
      _Model.LogOut();
    }
  }
}