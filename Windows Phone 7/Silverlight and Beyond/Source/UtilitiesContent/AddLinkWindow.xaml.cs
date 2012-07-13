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

namespace UtilitiesContent
{
    public partial class AddLinkWindow : ChildWindow
    {
        public AddLinkWindow()
        {
            InitializeComponent();
        }

        private void OKButton_Click(object sender, RoutedEventArgs e)
        {
            this.DialogResult = true;
        }

        private void CancelButton_Click(object sender, RoutedEventArgs e)
        {
            this.DialogResult = false;
        }



        public string Uri
        {
            get { return (string)GetValue(UriProperty); }
            set { SetValue(UriProperty, value); }
        }

        // Using a DependencyProperty as the backing store for Uri.  This enables animation, styling, binding, etc...
        public static readonly DependencyProperty UriProperty =
            DependencyProperty.Register("Uri", typeof(string), typeof(AddLinkWindow), new PropertyMetadata("http://"));



        public string Text
        {
            get { return (string)GetValue(TextProperty); }
            set { SetValue(TextProperty, value); }
        }

        // Using a DependencyProperty as the backing store for Text.  This enables animation, styling, binding, etc...
        public static readonly DependencyProperty TextProperty =
            DependencyProperty.Register("Text", typeof(string), typeof(AddLinkWindow), new PropertyMetadata(""));



        public string TargetName
        {
            get { return (string)GetValue(TargetNameProperty); }
            set { SetValue(TargetNameProperty, value); }
        }

        // Using a DependencyProperty as the backing store for TargetName.  This enables animation, styling, binding, etc...
        public static readonly DependencyProperty TargetNameProperty =
            DependencyProperty.Register("TargetName", typeof(string), typeof(AddLinkWindow), new PropertyMetadata("_blank"));
    }
}

