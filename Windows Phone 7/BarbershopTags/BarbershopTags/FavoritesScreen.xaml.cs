using System;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using SLaB.Utilities;

namespace BarbershopTags
{
    public partial class FavoritesScreen : UserControl
    {
        public FavoritesScreen()
        {
            InitializeComponent();
        }

        private void SearchBoxGotFocus(object sender, RoutedEventArgs e)
        {
            ((App)Application.Current).RootFrame.Navigate(new Uri("/SearchPage.xaml", UriKind.Relative));
        }

        private void RandomTagClick(object sender, RoutedEventArgs e)
        {
            if (!System.Net.NetworkInformation.NetworkInterface.GetIsNetworkAvailable())
            {
                MessageBox.Show("Please connect to a network in order to use this feature.");
                return;
            }
            if (BusyIndicator.IsBusy)
                return;
            BusyIndicator.IsBusy = true;
            Barbershop.Tag.Query(null, 0, sheetMusic: true, fieldList: "id").Continue(result =>
                {
                    Random r = new Random();
                    Barbershop.Tag.Query(null, 1, r.Next(result.Available), sheetMusic: true).Continue(finalResult =>
                        {
                            UiUtilities.ExecuteOnUiThread(
                                () =>
                                {
                                    ((App)Application.Current).RootFrame.Navigate(new Uri(string.Format("/TagPresenterPage.xaml?id={0}", finalResult.Tags.First().Id), UriKind.Relative));
                                    Dispatcher.BeginInvoke(() => BusyIndicator.IsBusy = false);
                                });
                        },
                        error =>
                        {
                            BusyIndicator.IsBusy = false;
                            UiUtilities.ExecuteOnUiThread(() => MessageBox.Show("An error has occurred"));
                        });
                },
                err =>
                {
                    BusyIndicator.IsBusy = false;
                    UiUtilities.ExecuteOnUiThread(() => MessageBox.Show("An error has occurred"));
                });
        }
    }
}
