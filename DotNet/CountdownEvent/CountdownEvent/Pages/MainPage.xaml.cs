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
using System.Windows.Media.Imaging;
using Microsoft.Devices;
using SLaB.Utilities;
using Microsoft.Phone.Shell;

namespace CountdownEvent.Pages
{
    public partial class MainPage : PhoneApplicationPage
    {
        public MainPage()
        {
            InitializeComponent();
            int selectedIndex = 0;
            //PhoneApplicationService.Current.State.SetIfContainsKey<int>("MainPano.SelectedIndex", val => selectedIndex = val);
            //MainPano.DefaultItem = MainPano.Items[selectedIndex];
            Dispatcher.DelayUntil(() => MainPanoSelectionChanged(MainPano, null), () => MainPano.SelectedItem != null);
        }

        private void MainPanoSelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            var presenter = ((PanoramaItem)MainPano.SelectedItem).Content as FeedPresenter;
            if (presenter != null)
                presenter.Load();
            //PhoneApplicationService.Current.State.SetIfNotInDesignMode("MainPano.SelectedIndex", MainPano.SelectedIndex);
        }

        private void CountdownCompleted(object sender, EventArgs e)
        {
            VibrateController.Default.Start(TimeSpan.FromSeconds(3));
        }
    }
}