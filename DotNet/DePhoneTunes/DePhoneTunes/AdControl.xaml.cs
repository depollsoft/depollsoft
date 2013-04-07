using System;
using System.IO;
using System.Net;
using System.Windows.Controls;
using Google.AdMob.Ads.WindowsPhone7.WPF;

namespace DePhoneTunes
{
    public partial class AdControl : UserControl
    {
        private BannerAd admobControl;
        private Microsoft.Advertising.Mobile.UI.AdControl pubcenterControl;
        public AdControl()
        {
            InitializeComponent();
            try
            {
                var request = WebRequest.Create("http://apps.davidpoll.com/pitchperfect/adType.php");
                request.BeginGetResponse(res =>
                    {
                        try
                        {
                            var response = request.EndGetResponse(res);
                            StreamReader sr = new StreamReader(response.GetResponseStream());
                            string adType = sr.ReadToEnd().Trim();
                            if (adType.Equals("admob", StringComparison.OrdinalIgnoreCase))
                            {
                                Dispatcher.BeginInvoke(LoadAdmob);
                            }
                            else
                            {
                                Dispatcher.BeginInvoke(LoadPubcenter);
                            }
                        }
                        catch
                        {
                            Dispatcher.BeginInvoke(LoadPubcenter);
                        }
                    }, null);
            }
            catch
            {
                LoadPubcenter();
            }
        }

        private void LoadAdmob()
        {
            admobControl = new BannerAd();
#if TESTADS
#else
#if ADS
            admobControl.AdUnitID = "a14d816355ba4de";
            admobControl.KeywordList = "music|notes|barbershop|sing|pitch pipe|pitch|guitar".Split('|');
#else
            this.Visibility = System.Windows.Visibility.Collapsed;
#endif
#endif
            //#if TESTADS
            //            Microsoft.Advertising.Mobile.UI.AdControl.TestMode = true;
            //            adControl.ApplicationId = "test_client";
            //            adControl.AdUnitId = "Image480_80";
            //#else
            //            Microsoft.Advertising.Mobile.UI.AdControl.TestMode = false;
            //#endif
#if ADS
            adContainer.Content = admobControl;
#endif
        }

        private void LoadPubcenter()
        {
            pubcenterControl = new Microsoft.Advertising.Mobile.UI.AdControl();
#if TESTADS
#else
#if ADS
            pubcenterControl.AdUnitId = "10013845";
            pubcenterControl.ApplicationId = "54a78009-dbac-40be-84c5-d84b0008fffe";
            pubcenterControl.AdSelectionKeywords = "music|notes|barbershop|sing|pitch pipe|pitch|guitar";
            pubcenterControl.AdControlError += new EventHandler<Microsoft.Advertising.Mobile.UI.ErrorEventArgs>(adControl_AdControlError);
#else
            this.Visibility = System.Windows.Visibility.Collapsed;
#endif
#endif
#if TESTADS
            Microsoft.Advertising.Mobile.UI.AdControl.TestMode = true;
            adControl.ApplicationId = "test_client";k
            adControl.AdUnitId = "Image480_80";
#else
            Microsoft.Advertising.Mobile.UI.AdControl.TestMode = false;
#endif
#if ADS
            adContainer.Content = pubcenterControl;
#endif
        }

        void adControl_AdControlError(object sender, Microsoft.Advertising.Mobile.UI.ErrorEventArgs e)
        {
            Dispatcher.BeginInvoke(LoadAdmob);
        }
    }
}
