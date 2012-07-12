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

namespace DePhoneTunes
{
    public partial class SongItemDisplay : UserControl
    {
        public SongItemDisplay()
        {
            InitializeComponent();
        }

        private Song Song
        {
            get
            {
                return (Song)DataContext;
            }
        }

        private void EditClick(object sender, RoutedEventArgs e)
        {
            ((PhoneApplicationFrame)App.Current.RootVisual).Navigate(new Uri("/SongEditPage.xaml?id=" + Song.Id, UriKind.Relative));
        }

        private void RemoveClick(object sender, RoutedEventArgs e)
        {
            SongsModel.Instance.Remove(Song);
        }

        private void MoveUpClick(object sender, RoutedEventArgs e)
        {
            SongsModel.Instance.MoveUp(Song);
        }

        private void MoveDownClick(object sender, RoutedEventArgs e)
        {
            SongsModel.Instance.MoveDown(Song);
        }

        private void SortClick(object sender, RoutedEventArgs e)
        {
            SongsModel.Instance.SortByTitle();
        }

        private void MenuOpened(object sender, RoutedEventArgs e)
        {
            MoveUp.Visibility = SongsModel.Instance.CanMoveUp(Song) ? Visibility.Visible : Visibility.Collapsed;
            MoveDown.Visibility = SongsModel.Instance.CanMoveDown(Song) ? Visibility.Visible : Visibility.Collapsed;
        }
    }
}
