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
using System.Windows.Navigation;
using SLaB.Utilities.Xaml.Collections;
using SLaB.Utilities.Xaml.Serializer.UI;
using System.Windows.Markup;

namespace UtilitiesContent
{
    public partial class RichTextSerializationDemo : Page
    {
        private UiXamlSerializer uxs = new UiXamlSerializer();

        public RichTextSerializationDemo()
        {
            InitializeComponent();
        }

        // Executes when the user navigates to this page.
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            ObservableObjectCollection ooc = new ObservableObjectCollection();
            foreach (Block b in rteLeft.RichTextBox.Blocks)
                ooc.Add(b);
            string xaml = uxs.Serialize(ooc);
            this.xamlTb.Text = xaml;
            ObservableObjectCollection bc = (ObservableObjectCollection)XamlReader.Load(xaml);
            rteRight.RichTextBox.Blocks.Clear();
            foreach (Block b in bc)
                rteRight.RichTextBox.Blocks.Add(b);
        }
    }
}
