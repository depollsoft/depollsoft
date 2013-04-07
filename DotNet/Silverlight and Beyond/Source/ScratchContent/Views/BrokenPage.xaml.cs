using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;

namespace ScratchContent.Views
{
    public partial class BrokenPage : Page
    {
        public BrokenPage()
        {
            InitializeComponent();
            MessageBox.Show(""[1].ToString());
        }

        // Executes when the user navigates to this page.
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
        }
    }
}