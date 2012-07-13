using System;
using System.Threading;
using System.Windows;
using SLaB.Controls.Remote;
//using SLaB.Offline;

namespace ScratchApplication
{
    public partial class App : Application
    {
        public App()
        {
            this.Startup += this.ApplicationStartup;
            InitializeComponent();
            //Timer t = new Timer(c => GC.Collect(), null, 0, 500);
        }

        private void ApplicationStartup(object sender, StartupEventArgs e)
        {
            var rc = new RemoteControl
            {
                ClassName = "ScratchContent.MainPage",
                AssemblyName = "ScratchContent",
                XapLocation = new System.Uri("ScratchContent.xap", System.UriKind.Relative)
            };
            RootVisual = rc;
            rc.Load();
            Current.CheckAndDownloadUpdateCompleted += CurrentCheckAndDownloadUpdateCompleted;
            Current.CheckAndDownloadUpdateAsync();
        }

        private static void CurrentCheckAndDownloadUpdateCompleted(object sender, CheckAndDownloadUpdateCompletedEventArgs e)
        {
            if (e.UpdateAvailable)
            {
                if (MessageBox.Show("The application has been updated and needs to be restarted.  Click OK to restart the application now, or cancel to continue using the application.", "Application updated", MessageBoxButton.OKCancel) == MessageBoxResult.OK)
                    Current.MainWindow.Close();
            }
        }

        //private void OfflineManager_IsolatedStorageQuotaIncreaseRequested(object sender, SLaB.Offline.IsolatedStorageQuotaIncreaseRequestEventArgs e)
        //{
        //    Popup popup = new Popup();
        //    Button toClick = new Button() { Content = "Click to increase storage quota" };
        //    toClick.Click += (s, args) =>
        //        {
        //            if (IsolatedStorageFile.GetUserStoreForSite().IncreaseQuotaTo(e.QuotaIncreaseSize + IsolatedStorageFile.GetUserStoreForSite().Quota))
        //            {
        //                popup.IsOpen = false;
        //                OfflineManager.Current.FlushToIsolatedStorage();
        //            }
        //        };
        //    popup.Child = toClick;
        //    popup.IsOpen = true;
        //}
    }
}