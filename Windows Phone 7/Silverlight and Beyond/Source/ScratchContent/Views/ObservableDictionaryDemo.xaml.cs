using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;
using SLaB.Utilities.Xaml.Collections;

namespace ScratchContent.Views
{
    public partial class ObservableDictionaryDemo : Page
    {
        public ObservableDictionaryDemo()
        {
            InitializeComponent();
        }

        // Executes when the user navigates to this page.
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            this.QueryString = new ObservableDictionary<string, string>(NavigationContext.QueryString);
        }

        public IDictionary<string, string> QueryString
        {
            get { return (IDictionary<string, string>)GetValue(QueryStringProperty); }
            set { SetValue(QueryStringProperty, value); }
        }

        // Using a DependencyProperty as the backing store for QueryString.  This enables animation, styling, binding, etc...
        public static readonly DependencyProperty QueryStringProperty =
            DependencyProperty.Register("QueryString", typeof(IDictionary<string, string>), typeof(ObservableDictionaryDemo), new PropertyMetadata(null));

        private void ButtonClick(object sender, RoutedEventArgs e)
        {
            QueryString["bonjour"] = "hello!";
        }

        private void ButtonClick1(object sender, RoutedEventArgs e)
        {
            QueryString["a"] = "back up!";
        }
    }
}