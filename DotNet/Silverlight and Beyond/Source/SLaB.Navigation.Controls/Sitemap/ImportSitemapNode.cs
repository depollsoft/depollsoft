#region Using Directives

using System.Windows;

#endregion

namespace SLaB.Navigation.Controls.Sitemap
{
    /// <summary>
    ///   A SitemapNode that imports its data from another Sitemap.
    /// </summary>
    public class ImportSitemapNode : SitemapNode
    {

        /// <summary>
        ///   The source ISitemap for this SitemapNode.
        /// </summary>
        public static readonly DependencyProperty SitemapSourceProperty =
            DependencyProperty.Register("SitemapSource",
                                        typeof(ISitemap),
                                        typeof(ImportSitemapNode),
                                        new PropertyMetadata(null, OnSitemapSourceChanged));



        /// <summary>
        ///   The source ISitemap for this SitemapNode.
        /// </summary>
        public ISitemap SitemapSource
        {
            get { return (ISitemap)this.GetValue(SitemapSourceProperty); }
            set { this.SetValue(SitemapSourceProperty, value); }
        }




        private static void OnSitemapSourceChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((ImportSitemapNode)obj).OnSitemapSourceChanged((ISitemap)args.OldValue, (ISitemap)args.NewValue);
        }

        private void OnSitemapSourceChanged(ISitemap oldSitemap, ISitemap newSitemap)
        {
            this.RefreshFromSource();
        }

        private void RefreshFromSource()
        {
            if (this.SitemapSource.Nodes is DependencyObjectCollection<ISitemapNode>)
                this.Nodes = (DependencyObjectCollection<ISitemapNode>)this.SitemapSource.Nodes;
            else
            {
                this.Nodes = new DependencyObjectCollection<ISitemapNode>();
                foreach (var node in this.SitemapSource.Nodes)
                    this.Nodes.Add(node);
            }
            this.Title = this.SitemapSource.Title;
            this.Description = this.SitemapSource.Description;
        }
    }
}