using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;
using Parse;
using System.Threading;

namespace BarbershopTags {
  public partial class FacebookLoginPage : PhoneApplicationPage {
    private CancellationTokenSource cts = new CancellationTokenSource();
    public FacebookLoginPage() {
      InitializeComponent();
    }

    protected async override void OnNavigatedTo(NavigationEventArgs e) {
      IsLoading = true;
      try {
        await ParseFacebookUtils.LogInAsync(Browser, new string[0], cts.Token);
        if (ParseUser.CurrentUser.IsNew) {
          await new SettingsModel().SaveToParseUser();
        } else {
          new SettingsModel().RestoreFromParseUser();
        }
      } catch (Exception ex) {
        if (!cts.IsCancellationRequested) {
          MessageBox.Show(ex.Message, "Facebook Login Failed", MessageBoxButton.OK);
        }
      } finally {
        if (!cts.IsCancellationRequested) {
          NavigationService.GoBack();
        }
        IsLoading = false;
      }
    }

    protected override void OnNavigatingFrom(NavigatingCancelEventArgs e) {
      base.OnNavigatingFrom(e);
      cts.Cancel();
    }


    public bool IsLoading {
      get { return (bool)GetValue(IsLoadingProperty); }
      set { SetValue(IsLoadingProperty, value); }
    }

    // Using a DependencyProperty as the backing store for IsLoading.  This enables animation, styling, binding, etc...
    public static readonly DependencyProperty IsLoadingProperty =
        DependencyProperty.Register("IsLoading", typeof(bool), typeof(FacebookLoginPage), new PropertyMetadata(false));

    private void BrowserNavigated(object sender, NavigationEventArgs e) {
      if (e.Uri.AbsoluteUri.StartsWith("https://www.facebook.com/connect/login_success.html")) {
        IsLoading = true;
        Browser.Visibility = Visibility.Collapsed;
      } else {
        IsLoading = false;
        Browser.Visibility = Visibility.Visible;
      }
    }


  }
}