using System;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using Microsoft.Phone.Tasks;

namespace CountdownEvent.Internal
{
    public class WebNavigateCommand : ICommand
    {

        public bool CanExecute(object parameter)
        {
            return parameter != null;
        }

        public event EventHandler CanExecuteChanged;

        public void Execute(object parameter)
        {
            WebBrowserTask wbt = new WebBrowserTask();
            if (parameter is string)
                wbt.URL = (string)parameter;
            else if (parameter is Uri)
                wbt.URL = ((Uri)parameter).OriginalString;
            wbt.Show();
        }
    }
}
