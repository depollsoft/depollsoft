using System;
using System.Windows;
using Microsoft.Phone.Controls;
using System.Windows.Media;
using System.Windows.Navigation;

namespace BarbershopTags {
  public partial class WebBrowserPage : PhoneApplicationPage {
    public WebBrowserPage() {
      InitializeComponent();
      Browser.Loaded += BrowserLoaded;
      Browser.Navigated += BrowserNavigated;
      Browser.LoadCompleted += Browser_LoadCompleted;
    }

    private void Browser_LoadCompleted(object sender, NavigationEventArgs e) {
      BusyIndicator.IsBusy = false;
    }

    private void BrowserNavigated(object sender, NavigationEventArgs e) {
    }

    private void BrowserLoaded(object sender, RoutedEventArgs e) {
      if (NavigationContext.QueryString.ContainsKey("img")) {
        Color phoneBackgroundColor = (Color)Application.Current.Resources["PhoneBackgroundColor"];
        var color = string.Format("#{0:x2}{1:x2}{2:x2}", phoneBackgroundColor.R, phoneBackgroundColor.G, phoneBackgroundColor.B);
        Browser.NavigateToString(string.Format(
            @"<html style='background: {1}'>
                     <body style='background: {1}'>
                        <div style='background: {1}; width: 100%; height: 120%'>
                           <img src='{0}' width='100%' style='position: absolute; top: 50%; margin-top: -25%; margin-bottom: -25%' />
                        </div>
                     </body>
                  </html>", Uri.UnescapeDataString(NavigationContext.QueryString["url"]), color));
      } else {
        Browser.Navigate(new Uri(Uri.UnescapeDataString(NavigationContext.QueryString["url"]), UriKind.Absolute));
      }
    }

  }
}