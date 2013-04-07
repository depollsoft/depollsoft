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
using System.Windows.Media.Imaging;
using System.Windows.Markup;
using System.Windows.Controls.Primitives;

namespace UtilitiesContent
{
    [ContentProperty("Blocks")]
    public partial class RichTextEditor : UserControl
    {
        public RichTextEditor()
        {
            InitializeComponent();
            fontsComboBox.DataContext = this;
        }

        public RichTextBox RichTextBox { get { return rtb; } }

        private const string AllFonts = "Arial,Arial Black,Arial Unicode MS,Calibri,Cambria,Cambria Math,Comic Sans MS,Candara,Consolas,Constantia,Corbel,Courier New,Georgia,Lucida Grande/Lucida Sans Unicode,Portable User Interface,Segoe UI,Symbol,Tahoma,Times New Roman,Trebuchet MS,Verdana,Wingdings,Wingdings 2,Wingdings 3";

        public IEnumerable<FontFamily> Fonts
        {
            get
            {
                return AllFonts.Split(',').Select((name) => new FontFamily(name));
            }
        }

        public BlockCollection Blocks { get { return rtb.Blocks; } }
        public bool IsReadOnly { get { return rtb.IsReadOnly; } set { rtb.IsReadOnly = value; } }

        private void boldButton_Click(object sender, RoutedEventArgs e)
        {
            if (boldButton.IsChecked == null)
                return;
            if (boldButton.IsChecked.Value)
                rtb.Selection.ApplyPropertyValue(Run.FontWeightProperty, FontWeights.Bold);
            else
                rtb.Selection.ApplyPropertyValue(Run.FontWeightProperty, FontWeights.Normal);
            rtb.Focus();
        }

        private void italicButton_Click(object sender, RoutedEventArgs e)
        {
            if (italicButton.IsChecked == null)
                return;
            if (italicButton.IsChecked.Value)
                rtb.Selection.ApplyPropertyValue(Run.FontStyleProperty, FontStyles.Italic);
            else
                rtb.Selection.ApplyPropertyValue(Run.FontStyleProperty, FontStyles.Normal);
            rtb.Focus();
        }

        private void fontsComboBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            rtb.Selection.ApplyPropertyValue(Run.FontFamilyProperty, fontsComboBox.SelectedItem);
            rtb.Focus();
        }

        private void sizeComboBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            rtb.Selection.ApplyPropertyValue(Run.FontSizeProperty, sizeComboBox.SelectedItem);
            rtb.Focus();
        }

        private void rtb_SelectionChanged(object sender, RoutedEventArgs e)
        {
            underlineButton.IsChecked = TextDecorations.Underline.Equals(rtb.Selection.GetPropertyValue(Run.TextDecorationsProperty));
            italicButton.IsChecked = FontStyles.Italic.Equals(rtb.Selection.GetPropertyValue(Run.FontStyleProperty));
            boldButton.IsChecked = FontWeights.Bold.Equals(rtb.Selection.GetPropertyValue(Run.FontWeightProperty));
            try
            {
                fontsComboBox.SelectedValue = ((FontFamily)rtb.Selection.GetPropertyValue(Run.FontFamilyProperty)).Source;
            }
            catch { }
            try
            {
                sizeComboBox.SelectedItem = rtb.Selection.GetPropertyValue(Run.FontSizeProperty);
            }
            catch { }
        }

        private void underlineButton_Click(object sender, RoutedEventArgs e)
        {
            if (underlineButton.IsChecked == null)
                return;
            if (underlineButton.IsChecked.Value)
                rtb.Selection.ApplyPropertyValue(Run.TextDecorationsProperty, TextDecorations.Underline);
            else
                rtb.Selection.ApplyPropertyValue(Run.TextDecorationsProperty, null);
            rtb.Focus();
        }

        private void linkButton_Click(object sender, RoutedEventArgs e)
        {
            AddLinkWindow alw = new AddLinkWindow();
            alw.Closed += new EventHandler(alw_Closed);
            alw.Show();
        }

        private void alw_Closed(object sender, EventArgs e)
        {
            AddLinkWindow alw = (AddLinkWindow)sender;
            if (!(alw.DialogResult.HasValue && alw.DialogResult.Value))
                return;
            Uri uri;
            try
            {
                uri = new Uri(alw.Uri);
            }
            catch
            {
                uri = new Uri(alw.Uri, UriKind.Relative);
            }
            Hyperlink hl = new Hyperlink { NavigateUri = uri, TargetName = alw.TargetName };
            hl.Inlines.Add(alw.Text);
            rtb.Selection.Insert(hl);
            rtb.Focus();
        }

        private void imageButton_Click(object sender, RoutedEventArgs e)
        {
            AddImageWindow aiw = new AddImageWindow();
            aiw.Closed += new EventHandler(aiw_Closed);
            aiw.Show();
        }

        private void aiw_Closed(object sender, EventArgs e)
        {
            AddImageWindow aiw = (AddImageWindow)sender;
            if (!(aiw.DialogResult.HasValue && aiw.DialogResult.Value))
                return;
            Uri uri;
            try
            {
                uri = new Uri(aiw.Uri);
            }
            catch
            {
                uri = new Uri(aiw.Uri, UriKind.Relative);
            }
            Image img = new Image();
            img.Source = new BitmapImage(uri);
            ToolTipService.SetToolTip(img, aiw.Text);
            img.Stretch = Stretch.None;
            img.HorizontalAlignment = HorizontalAlignment.Center;
            img.VerticalAlignment = VerticalAlignment.Center;
            InlineUIContainer iuic = new InlineUIContainer();
            iuic.Child = img;
            rtb.Selection.Insert(iuic);
            rtb.Focus();
        }

        private void innerRichButton_Click(object sender, RoutedEventArgs e)
        {
            RichTextEditor rte = new RichTextEditor();
            InlineUIContainer iuic = new InlineUIContainer();
            iuic.Child = rte;
            rtb.Selection.Insert(iuic);
            rtb.Focus();
        }
    }
}
