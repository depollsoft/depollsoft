#region Using Directives

using System.Collections.Generic;
using System.Linq;
using System.Windows;
using System.Windows.Markup;
using SLaB.Navigation.Controls.Sitemap;
using SLaB.Utilities;
using SLaB.Utilities.ChangeLinq;
using SLaB.Utilities.Xaml;
using SLaB.Utilities.Xaml.Converters;

#endregion

namespace SLaB.Navigation.Controls
{
    /// <summary>
    ///   A control for breadcrumb-based navigation within a Silverlight application.
    /// </summary>
    [ContentProperty("Sitemap")]
    [StyleTypedProperty(Property = "ComboBoxStyle", StyleTargetType = typeof(BreadCrumbComboBox))]
    [XamlDependency(typeof(BoolConverter))]
    [XamlDependency(typeof(CollectionConverter))]
    [XamlDependency(typeof(WrappingConverter))]
    public class BreadCrumbNavigator : Navigator
    {

        /// <summary>
        ///   Gets the set of BreadCrumbNodeLists being displayed by the BreadCrumbNavigator.
        /// </summary>
        public static readonly DependencyProperty BreadCrumbNodeListsProperty =
            DependencyProperty.Register("BreadCrumbNodeLists",
                                        typeof(IEnumerable<BreadCrumbNodeList>),
                                        typeof(BreadCrumbNavigator),
                                        new PropertyMetadata(null));
        /// <summary>
        ///   Gets or sets the style for the BreadCrumbComboBoxes used by the BreadCrumbNavigator's template.
        /// </summary>
        public static readonly DependencyProperty ComboBoxStyleProperty =
            DependencyProperty.Register("ComboBoxStyle",
                                        typeof(Style),
                                        typeof(BreadCrumbNavigator),
                                        new PropertyMetadata(null));



        /// <summary>
        ///   Creates a BreadCrumbNavigator.
        /// </summary>
        public BreadCrumbNavigator()
        {
            this.DefaultStyleKey = typeof(BreadCrumbNavigator);
        }



        /// <summary>
        ///   Gets the set of BreadCrumbNodeLists being displayed by the BreadCrumbNavigator.
        /// </summary>
        public IEnumerable<BreadCrumbNodeList> BreadCrumbNodeLists
        {
            get { return (IEnumerable<BreadCrumbNodeList>)this.GetValue(BreadCrumbNodeListsProperty); }
            private set { this.SetValue(BreadCrumbNodeListsProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the style for the BreadCrumbComboBoxes used by the BreadCrumbNavigator's template.
        /// </summary>
        public Style ComboBoxStyle
        {
            get { return (Style)this.GetValue(ComboBoxStyleProperty); }
            set { this.SetValue(ComboBoxStyleProperty, value); }
        }




        /// <summary>
        ///   Refreshes BreadCrumbNodeLists whenever the CurrentSource changes.
        /// </summary>
        protected override void OnCurrentSourceChanged()
        {
            ((IRefreshable)this.BreadCrumbNodeLists).Refresh();
        }

        /// <summary>
        ///   Refreshes BreadCrumbNodeLists.
        /// </summary>
        protected override void OnTrimmedSitemapChanged()
        {
            if (this.TrimmedSitemap == null)
            {
                this.BreadCrumbNodeLists = null;
                return;
            }
            var ts = this.TrimmedSitemap.AsChangeLinq();
            var result = new IEnumerable<SitemapNodeProxy>[] { ts }.AsChangeLinq()
                .Concat(ts.Traverse(proxy => proxy.Nodes)
                            .Where(proxy => proxy.IsInPath && proxy.Nodes.Any())
                            .Select(proxy => proxy.Nodes))
                .Select(pList => new BreadCrumbNodeList
                    {
                        Proxies = pList,
                        CurrentItem = pList.Where(proxy => proxy.IsInPath).FirstOrDefault(),
                        SelectableProxies = pList.Where(proxy => proxy.Node.Uri != null)
                    });
            this.BreadCrumbNodeLists = result;
        }
    }

    /// <summary>
    ///   Represents a NodeList for the BreadCrumbNavigator control.
    /// </summary>
    public class BreadCrumbNodeList
    {

        /// <summary>
        ///   Gets the currently-selected item for this NodeList.
        /// </summary>
        public SitemapNodeProxy CurrentItem { get; internal set; }

        /// <summary>
        ///   Gets the Host BreadCrumbNavigator control.
        /// </summary>
        public DependencyObject Host
        {
            get { return this.Proxies.First().Host; }
        }

        /// <summary>
        ///   Gets the set of SitemapNodeProxies for this NodeList.
        /// </summary>
        public IEnumerable<SitemapNodeProxy> Proxies { get; internal set; }

        /// <summary>
        ///   Gets the set of selectable SitemapNodeProxies (i.e. those that are navigable).
        /// </summary>
        public IEnumerable<SitemapNodeProxy> SelectableProxies { get; internal set; }
    }
}