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

namespace DePhoneTunes
{
    public partial class Notes : UserControl
    {
        public Notes()
        {
            InitializeComponent();
#if ADS
            aboutControl.Visibility = Visibility.Collapsed;
#endif
        }

        private void ListBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            //Dispatcher.BeginInvoke(() => scroller.ScrollToVerticalOffset(scroller.ScrollableHeight / 2));
        }

        private void ListBox_Loaded(object sender, RoutedEventArgs e)
        {
            //ListBox_SelectionChanged(sender, null);
        }
    }
}
