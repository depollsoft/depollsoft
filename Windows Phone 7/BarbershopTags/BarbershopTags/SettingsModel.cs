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
using System.ComponentModel;
using SLaB.Utilities;
using System.Collections.Generic;
using System.IO.IsolatedStorage;
using Microsoft.Phone.Shell;

namespace BarbershopTags
{
    public class SettingsModel : INotifyPropertyChanged
    {

        public double GlobalVolume
        {
            get
            {
                if (DesignerProperties.IsInDesignTool || !IsolatedStorageSettings.ApplicationSettings.Contains("GlobalVolume"))
                    return 100;
                return (double)IsolatedStorageSettings.ApplicationSettings["GlobalVolume"];
            }
            set
            {
                value = Math.Max(value, 0);
                value = Math.Min(value, 100);
                IsolatedStorageSettings.ApplicationSettings["GlobalVolume"] = value;
                PropertyChanged.Raise(this, new PropertyChangedEventArgs("GlobalVolume"));
            }
        }
        [TypeConverter(typeof(NullableBoolConverter))]
        public bool? CanRunInBackground
        {
            get
            {
                if (DesignerProperties.IsInDesignTool || !IsolatedStorageSettings.ApplicationSettings.Contains("CanRunInBackground"))
                    return null;
                return (bool)IsolatedStorageSettings.ApplicationSettings["CanRunInBackground"];
            }
            set
            {
                if (value == null)
                    IsolatedStorageSettings.ApplicationSettings.Remove("CanRunInBackground");
                else
                {
                    IsolatedStorageSettings.ApplicationSettings["CanRunInBackground"] = value.Value;
                    PhoneApplicationService.Current.ApplicationIdleDetectionMode = value.Value ? IdleDetectionMode.Enabled : IdleDetectionMode.Disabled;
                }
                PropertyChanged.Raise(this, new PropertyChangedEventArgs("CanRunInBackground"));
            }
        }
        public event PropertyChangedEventHandler PropertyChanged;
    }
}
