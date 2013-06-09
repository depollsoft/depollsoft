using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using SLaB.Utilities.Xaml.Serializer.UI;

namespace UtilitiesTests
{
    public partial class SilverlightControl1 : UserControl
    {
        private UiXamlSerializer uxs = new UiXamlSerializer();
        private object toSerialize = new TestUserControl().RealContent;
        public SilverlightControl1()
        {
            InitializeComponent();
            uxs.DiscoverAttachedProperties(typeof(AttachedProps));
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            DateTime dt = DateTime.Now;
            string result = uxs.Serialize(toSerialize);
            MessageBox.Show((DateTime.Now - dt).ToString());
            tb.Text = result;
            object output = XamlReader.Load(result);
            toSerialize = output;
        }
    }
}
