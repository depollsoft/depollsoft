#region Using Directives

using System.Windows.Controls;
using System.Windows.Navigation;

#endregion

namespace NavigationTests
{
    public partial class ExistingPage : Page
    {
        public ExistingPage()
        {
            this.InitializeComponent();
        }

        // Executes when the user navigates to this page.
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
        }
    }
}