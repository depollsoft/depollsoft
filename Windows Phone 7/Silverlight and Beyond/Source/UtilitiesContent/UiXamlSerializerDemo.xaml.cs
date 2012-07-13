#region Using Directives

using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;
using SLaB.Utilities.Xaml.Serializer.UI;

#endregion

namespace UtilitiesContent
{
    public partial class UiXamlSerializerDemo : Page
    {
        private readonly UiXamlSerializer uxs = new UiXamlSerializer();

        public UiXamlSerializerDemo()
        {
            this.InitializeComponent();
        }

        // Executes when the user navigates to this page.
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            string text = this.uxs.Serialize(this.gridToSerialize);
            this.xamlText.Text = text;
        }
    }
}