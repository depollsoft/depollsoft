#region Using Directives

using System;
using System.Collections.Generic;
using System.Security.Principal;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using SLaB.Navigation.Controls.Sitemap;
using SLaB.Utilities;
using SLaB.Utilities.ChangeLinq;

#endregion

namespace SLaB.Navigation.Controls
{
    /// <summary>
    ///   An abstract base class for controls that represent a Sitemap.
    /// </summary>
    [ContentProperty("Sitemap")]
    public abstract class Navigator : Control
    {

        private readonly Dictionary<ISitemapNode, SitemapNodeProxy> _Proxies;
        /// <summary>
        ///   Gets or sets the CurrentSource for this Navigator.  This is usually bound to the CurrentSource property of a Frame control.
        /// </summary>
        public static readonly DependencyProperty CurrentSourceProperty =
            DependencyProperty.Register("CurrentSource",
                                        typeof(Uri),
                                        typeof(Navigator),
                                        new PropertyMetadata(null, OnCurrentSourceChanged));
        /// <summary>
        ///   Gets or sets the template that is used to display each node in the Sitemap.  Its DataContext is a SitemapNodeProxy.
        /// </summary>
        public static readonly DependencyProperty ItemTemplateProperty =
            DependencyProperty.Register("ItemTemplate",
                                        typeof(DataTemplate),
                                        typeof(Navigator),
                                        new PropertyMetadata(null));
        /// <summary>
        ///   Gets or sets the Principal used for Sitemap trimming.
        /// </summary>
        public static readonly DependencyProperty PrincipalProperty =
            DependencyProperty.Register("Principal",
                                        typeof(IPrincipal),
                                        typeof(Navigator),
                                        new PropertyMetadata(null, OnRefreshingPropertyChanged));
        /// <summary>
        ///   Gets or sets the template for displaying the Sitemap header (usually used to display the Sitemap's title and description).
        /// </summary>
        public static readonly DependencyProperty SitemapHeaderTemplateProperty =
            DependencyProperty.Register("SitemapHeaderTemplate",
                                        typeof(DataTemplate),
                                        typeof(Navigator),
                                        new PropertyMetadata(null));
        /// <summary>
        ///   Gets or sets the visibility of the Sitemap header.
        /// </summary>
        public static readonly DependencyProperty SitemapHeaderVisibilityProperty =
            DependencyProperty.Register("SitemapHeaderVisibility",
                                        typeof(Visibility),
                                        typeof(Navigator),
                                        new PropertyMetadata(Visibility.Collapsed));
        /// <summary>
        ///   Gets or sets the Sitemap being represented by this Navigator.
        /// </summary>
        public static readonly DependencyProperty SitemapProperty =
            DependencyProperty.Register("Sitemap",
                                        typeof(ISitemap),
                                        typeof(Navigator),
                                        new PropertyMetadata(null, OnSitemapChanged));
        /// <summary>
        ///   Gets the trimmed Sitemap.
        /// </summary>
        private static readonly DependencyProperty TrimmedSitemapProperty =
            DependencyProperty.Register("TrimmedSitemap",
                                        typeof(IEnumerable<SitemapNodeProxy>),
                                        typeof(Navigator),
                                        new PropertyMetadata(null, OnTrimmedSitemapChanged));
        /// <summary>
        ///   Gets or sets a value that determines whether Sitemap trimming based on the Principal is enabled.
        /// </summary>
        public static readonly DependencyProperty TrimSitemapProperty =
            DependencyProperty.Register("IsSitemapTrimmingEnabled",
                                        typeof(bool),
                                        typeof(Navigator),
                                        new PropertyMetadata(true, OnRefreshingPropertyChanged));



        /// <summary>
        ///   Creates a Navigator.
        /// </summary>
        protected Navigator()
        {
            this._Proxies = new Dictionary<ISitemapNode, SitemapNodeProxy>();
            this.Unloaded += this.NavigatorUnloaded;
            this.Loaded += this.NavigatorLoaded;
        }



        /// <summary>
        ///   Gets the SitemapNodeProxy for the currently selected Node (i.e. the Node that represents the CurrentSource).
        /// </summary>
        protected SitemapNodeProxy CurrentNode { get; private set; }

        /// <summary>
        ///   Gets or sets the CurrentSource for this Navigator.  This is usually bound to the CurrentSource property of a Frame control.
        /// </summary>
        public Uri CurrentSource
        {
            get { return (Uri)this.GetValue(CurrentSourceProperty); }
            set { this.SetValue(CurrentSourceProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a value that determines whether Sitemap trimming based on the Principal is enabled.
        /// </summary>
        public bool IsSitemapTrimmingEnabled
        {
            get { return (bool)this.GetValue(TrimSitemapProperty); }
            set { this.SetValue(TrimSitemapProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the template that is used to display each node in the Sitemap.  Its DataContext is a SitemapNodeProxy.
        /// </summary>
        public DataTemplate ItemTemplate
        {
            get { return (DataTemplate)this.GetValue(ItemTemplateProperty); }
            set { this.SetValue(ItemTemplateProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the Principal used for Sitemap trimming.
        /// </summary>
        public IPrincipal Principal
        {
            get { return (IPrincipal)this.GetValue(PrincipalProperty); }
            set { this.SetValue(PrincipalProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the Sitemap being represented by this Navigator.
        /// </summary>
        public ISitemap Sitemap
        {
            get { return (ISitemap)this.GetValue(SitemapProperty); }
            set { this.SetValue(SitemapProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the template for displaying the Sitemap header (usually used to display the Sitemap's title and description).
        /// </summary>
        public DataTemplate SitemapHeaderTemplate
        {
            get { return (DataTemplate)this.GetValue(SitemapHeaderTemplateProperty); }
            set { this.SetValue(SitemapHeaderTemplateProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the visibility of the Sitemap header.
        /// </summary>
        public Visibility SitemapHeaderVisibility
        {
            get { return (Visibility)this.GetValue(SitemapHeaderVisibilityProperty); }
            set { this.SetValue(SitemapHeaderVisibilityProperty, value); }
        }

        /// <summary>
        ///   Gets the trimmed Sitemap.
        /// </summary>
        public IEnumerable<SitemapNodeProxy> TrimmedSitemap
        {
            get { return (IEnumerable<SitemapNodeProxy>)this.GetValue(TrimmedSitemapProperty); }
            private set { this.SetValue(TrimmedSitemapProperty, value); }
        }




        /// <summary>
        ///   Removes proxies and clears the TrimmedSitemap.
        /// </summary>
        protected virtual void Cleanup()
        {
            foreach (var node in this._Proxies.Values)
                node.Cleanup();
            this._Proxies.Clear();
            this.TrimmedSitemap = null;
        }

        /// <summary>
        ///   Called when CurrentSource changes.
        /// </summary>
        protected virtual void OnCurrentSourceChanged()
        {
        }

        /// <summary>
        ///   Called when the Sitemap property is changed.
        /// </summary>
        protected virtual void OnSitemapChanged()
        {
        }

        /// <summary>
        ///   Called when the TrimmedSitemap changes.
        /// </summary>
        protected virtual void OnTrimmedSitemapChanged()
        {
        }

        private bool IsNodeVisible(ISitemapNode node)
        {
            if (!this.IsSitemapTrimmingEnabled)
                return true;
            return node.IsVisibleTo(this.Principal);
        }

        private void NavigatorLoaded(object sender, RoutedEventArgs e)
        {
            this.OnSitemapChangedInternal();
        }

        private void NavigatorUnloaded(object sender, RoutedEventArgs e)
        {
            this.Cleanup();
        }

        private static void OnCurrentSourceChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((Navigator)obj).OnCurrentSourceChangedInternal();
        }

        private void OnCurrentSourceChangedInternal()
        {
            if (this.Sitemap == null)
                return;
            foreach (var proxy in this.TrimmedSitemap)
                this.RefreshCurrency(proxy);
            this.OnCurrentSourceChanged();
        }

        private static void OnRefreshingPropertyChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((Navigator)obj).Refresh();
        }

        private static void OnSitemapChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((Navigator)obj).OnSitemapChangedInternal();
        }

        private void OnSitemapChangedInternal()
        {
            if (this.Sitemap == null)
                this.TrimmedSitemap = null;
            else
                this.TrimmedSitemap = from node in this.Sitemap.Nodes.AsChangeLinq()
                                      where this.IsNodeVisible(node)
                                      select this.GetProxy(node);
            this.OnSitemapChanged();
        }

        private static void OnTrimmedSitemapChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((Navigator)obj).OnTrimmedSitemapChangedInternal();
        }

        private void OnTrimmedSitemapChangedInternal()
        {
            this.OnTrimmedSitemapChanged();
        }

        private void Refresh()
        {
            if (this.TrimmedSitemap != null)
            {
                ((IRefreshable)this.TrimmedSitemap).Refresh();
                foreach (var proxy in this.TrimmedSitemap)
                    proxy.Refresh();
                this.OnTrimmedSitemapChangedInternal();
            }
        }

        private bool RefreshCurrency(SitemapNodeProxy proxy)
        {
            bool isInPath = false;
            bool isCurrentSource = false;
            foreach (var child in proxy.Nodes)
                isInPath = this.RefreshCurrency(child) || isInPath;
            if (UriUtilities.Equals(this.CurrentSource, proxy.Node.Uri))
            {
                isCurrentSource = true;
                this.CurrentNode = proxy;
            }
            proxy.IsCurrentSource = isCurrentSource;
            proxy.IsChildCurrentSource = isInPath;
            proxy.IsInPath = isCurrentSource || isInPath;
            return isCurrentSource || isInPath;
        }

        internal SitemapNodeProxy GetProxy(ISitemapNode node)
        {
            if (this._Proxies.ContainsKey(node))
                return this._Proxies[node];
            this._Proxies[node] = new SitemapNodeProxy(this.IsNodeVisible, this.GetProxy);
            this._Proxies[node].Host = this;
            this._Proxies[node].Node = node;
            return this._Proxies[node];
        }
    }
}