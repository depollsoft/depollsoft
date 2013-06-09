#region Using Directives

using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using System.Windows.Navigation;
using System.Xml;
using SLaB.Utilities.Xaml.Serializer;
using System.IO;

#endregion

namespace UtilitiesContent
{
    public partial class XamlSerializerDemo : Page
    {
        private readonly XamlSerializer xs = new XamlSerializer();

        public XamlSerializerDemo()
        {
            this.InitializeComponent();
            this.nestedObject = this.LayoutRoot.Resources["nestedObject"] as NestedObject;
            this.xs.DiscoverAttachedProperties(typeof(AttachedProps));
        }

        // Executes when the user navigates to this page.
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
        }

        private void Button_Click(object sender, RoutedEventArgs e)
        {
            string text = this.xs.Serialize(this.nestedObject);
            StringBuilder sb = new StringBuilder();
            XmlWriter xw = XmlWriter.Create(sb,
                                            new XmlWriterSettings
                                                {
                                                    OmitXmlDeclaration = true,
                                                    NamespaceHandling = NamespaceHandling.OmitDuplicates,
                                                    NewLineOnAttributes = true,
                                                    Indent = true
                                                });
            xw.WriteNode(XmlReader.Create(new StringReader(text)), true);
            xw.Flush();
            this.xamlText.Text = sb.ToString();
            XamlReader.Load(text);
        }
    }
}