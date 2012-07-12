using System.Windows;
using System.Windows.Controls;
using Microsoft.Phone.Controls;

namespace BarbershopTags
{
    public partial class MainPage : PhoneApplicationPage
    {
        // Constructor
        public MainPage()
        {
            InitializeComponent();
        }

        private void PanoramaSelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            Fetch();
        }

        private void Fetch()
        {
            var currentModel = ((PanoramaItem)this.Panorama.SelectedItem).DataContext as QueryModel;
            if (currentModel != null && currentModel.Tags.Count == 0)
                currentModel.FetchResults();
        }

        private void PanoramaLoaded(object sender, RoutedEventArgs e)
        {
            Fetch();
        }
    }
}