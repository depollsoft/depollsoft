using System.Windows;
using System.Windows.Controls;
using SLaB.Utilities;

namespace BarbershopTags {
  public partial class FavoriteTagDisplay : UserControl {
    public FavoriteTagDisplay() {
      InitializeComponent();
    }


    public int TagId {
      get { return (int)GetValue(TagIdProperty); }
      set { SetValue(TagIdProperty, value); }
    }

    public static readonly DependencyProperty TagIdProperty =
        DependencyProperty.Register("TagId", typeof(int), typeof(FavoriteTagDisplay), new PropertyMetadata(default(int), OnTagIdChanged));

    private static void OnTagIdChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((FavoriteTagDisplay)obj).OnTagIdChanged((int)args.OldValue, (int)args.NewValue);
    }

    private void OnTagIdChanged(int oldValue, int newValue) {
      BusyIndicator.IsBusy = true;
      Barbershop.Tag.LoadTagById(newValue).Continue(tag => {
            UiUtilities.ExecuteOnUiThread(() => {
                  Presenter.Content = tag;
                  Presenter.ContentTemplate = App.Current.Resources["TagLink"] as DataTemplate;
                  BusyIndicator.IsBusy = false;
                });
          },
          err => {
            UiUtilities.ExecuteOnUiThread(() => {
                  this.Visibility = Visibility.Collapsed;
                  BusyIndicator.IsBusy = false;
                });
          });
    }

    private void FavoriteRemoveClick(object sender, RoutedEventArgs e) {
      FavoritesModel.RemoveFavorite(TagId);
    }

    private void ContextMenuOpened(object sender, RoutedEventArgs e) {
      MoveUpItem.Visibility = FavoritesModel.CanMoveUp(TagId) ? Visibility.Visible : Visibility.Collapsed;
      MoveDownItem.Visibility = FavoritesModel.CanMoveDown(TagId) ? Visibility.Visible : Visibility.Collapsed;
    }

    private void MoveUpClick(object sender, RoutedEventArgs e) {
      FavoritesModel.MoveUp(TagId);
    }

    private void MoveDownClick(object sender, RoutedEventArgs e) {
      FavoritesModel.MoveDown(TagId);
    }
  }
}
