using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;
using SLaB.Navigation.Controls.Sitemap;

namespace ScratchContent.Views
{
    public partial class SitemapPage : Page
    {
        public SitemapPage()
        {
            InitializeComponent();
        }

        // Executes when the user navigates to this page.
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            ISitemap sitemap = null;
            Dictionary<string, string> qs = new Dictionary<string, string>(NavigationContext.QueryString, StringComparer.CurrentCultureIgnoreCase);
            string name = qs["sitemapname"];
            if (Resources.Contains(name))
                sitemap = Resources[name] as ISitemap;
            if (sitemap == null && Application.Current.Resources.Contains(name))
                sitemap = Application.Current.Resources[name] as ISitemap;
            navigator.Sitemap = sitemap;
        }
    }
}