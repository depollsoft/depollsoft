using System;
using System.IO;
using System.Net;
using System.Windows.Controls;
using Google.AdMob.Ads.WindowsPhone7.WPF;

namespace DePhoneTunes {
  public partial class AdControl : UserControl {
    private BannerAd admobControl;
    private Microsoft.Advertising.Mobile.UI.AdControl pubcenterControl;
    public AdControl() {
      InitializeComponent();
      LoadPubcenter();
    }

    private void LoadPubcenter() {
      pubcenterControl = new Microsoft.Advertising.Mobile.UI.AdControl();
#if TESTADS
#else
#if ADS
            pubcenterControl.AdUnitId = "10013845";
            pubcenterControl.ApplicationId = "54a78009-dbac-40be-84c5-d84b0008fffe";
            pubcenterControl.AdSelectionKeywords = "music|notes|barbershop|sing|pitch pipe|pitch|guitar";
#else
      this.Visibility = System.Windows.Visibility.Collapsed;
#endif
#endif
#if TESTADS
            Microsoft.Advertising.Mobile.UI.AdControl.TestMode = true;
            pubcenterControl.ApplicationId = "test_client";
            pubcenterControl.AdUnitId = "Image480_80";
#else
      Microsoft.Advertising.Mobile.UI.AdControl.TestMode = false;
#endif
#if ADS
            adContainer.Content = pubcenterControl;
#endif
    }
  }
}
