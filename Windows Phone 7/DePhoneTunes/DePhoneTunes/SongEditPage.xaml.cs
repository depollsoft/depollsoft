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
    public partial class SongEditPage : PhoneApplicationPage
    {
        public SongEditPage()
        {
            InitializeComponent();
            this.DataContext = this;
        }

        protected override void OnNavigatedTo(System.Windows.Navigation.NavigationEventArgs e)
        {
            base.OnNavigatedTo(e);
            SongsModel = SongsModel.Instance;

            Guid id = new Guid(NavigationContext.QueryString["id"]);
            Dispatcher.BeginInvoke(() => Song = SongsModel.SongForGuid(id));
        }


        public Song Song
        {
            get { return (Song)GetValue(SongProperty); }
            set { SetValue(SongProperty, value); }
        }

        public static readonly DependencyProperty SongProperty =
            DependencyProperty.Register("Song", typeof(Song), typeof(SongEditPage), new PropertyMetadata(default(Song), OnSongChanged));

        private static void OnSongChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((SongEditPage)obj).OnSongChanged((Song)args.OldValue, (Song)args.NewValue);
        }

        private void OnSongChanged(Song oldValue, Song newValue)
        {
            int index = SongsModel.AllKeys.ToList().IndexOf(newValue.Key);
            if (index < 0)
            {
                index = SongsModel.AllKeys.Count() / 4;
            }
            keyPicker.SelectedIndex = index;
        }


        public SongsModel SongsModel
        {
            get { return (SongsModel)GetValue(SongsModelProperty); }
            set { SetValue(SongsModelProperty, value); }
        }

        public static readonly DependencyProperty SongsModelProperty =
            DependencyProperty.Register("SongsModel", typeof(SongsModel), typeof(SongEditPage), new PropertyMetadata(default(SongsModel), OnSongsModelChanged));

        private static void OnSongsModelChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((SongEditPage)obj).OnSongsModelChanged((SongsModel)args.OldValue, (SongsModel)args.NewValue);
        }

        private void OnSongsModelChanged(SongsModel oldValue, SongsModel newValue)
        {
        }

        private void KeySelected(object sender, SelectionChangedEventArgs e)
        {
            if (Song != null)
                Song.Key = (SongKey)keyPicker.SelectedItem;
        }

        private void DoneClick(object sender, EventArgs e)
        {
            this.Focus();
            SongsModel.Instance.RefreshSelectedSong();
            NavigationService.GoBack();
        }

        private void DeleteClick(object sender, EventArgs e)
        {
            SongsModel.Instance.Remove(Song);
            SongsModel.Instance.RefreshSelectedSong();
            NavigationService.GoBack();
        }
    }
}