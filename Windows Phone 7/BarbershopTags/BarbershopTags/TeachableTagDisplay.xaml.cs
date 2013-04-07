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
using SLaB.Utilities;
using SLaB.Controls.Phone;

namespace BarbershopTags {
  public partial class TeachableTagDisplay : UserControl {
    public TeachableTagDisplay() {
      InitializeComponent();
    }

    public int TagId {
      get { return (int)GetValue(TagIdProperty); }
      set { SetValue(TagIdProperty, value); }
    }

    public static readonly DependencyProperty TagIdProperty =
        DependencyProperty.Register("TagId", typeof(int), typeof(TeachableTagDisplay), new PropertyMetadata(default(int), OnTagIdChanged));

    private static void OnTagIdChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args) {
      ((TeachableTagDisplay)obj).OnTagIdChanged((int)args.OldValue, (int)args.NewValue);
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

    private void TeachableRemoveClick(object sender, RoutedEventArgs e) {
      TeachableTagsModel.RemoveTeachable(TagId);
    }

    private void ContextMenuOpened(object sender, RoutedEventArgs e) {
      MoveUpItem.Visibility = TeachableTagsModel.CanMoveUp(TagId) ? Visibility.Visible : Visibility.Collapsed;
      MoveDownItem.Visibility = TeachableTagsModel.CanMoveDown(TagId) ? Visibility.Visible : Visibility.Collapsed;
    }

    private void MoveUpClick(object sender, RoutedEventArgs e) {
      TeachableTagsModel.MoveUp(TagId);
    }

    private void MoveDownClick(object sender, RoutedEventArgs e) {
      TeachableTagsModel.MoveDown(TagId);
    }

  }
}
