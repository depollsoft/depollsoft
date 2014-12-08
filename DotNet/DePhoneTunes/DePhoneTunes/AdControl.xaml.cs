using System;
using System.IO;
using System.Net;
using System.Windows.Controls;

namespace DePhoneTunes {
  public partial class AdControl : UserControl {
    public AdControl() {
      InitializeComponent();
#if TESTADS
#else
#if ADS
#else
      this.Visibility = System.Windows.Visibility.Collapsed;
#endif
#endif
      AdMediator_804747.AdMediatorError += AdMediator_804747_AdMediatorError;
      AdMediator_804747.AdSdkError += AdMediator_804747_AdSdkError;
    }

    void AdMediator_804747_AdSdkError(object sender, Microsoft.AdMediator.Core.Events.AdFailedEventArgs e) {
    }

    void AdMediator_804747_AdMediatorError(object sender, Microsoft.AdMediator.Core.Events.AdMediatorFailedEventArgs e) {
    }
  }
}
