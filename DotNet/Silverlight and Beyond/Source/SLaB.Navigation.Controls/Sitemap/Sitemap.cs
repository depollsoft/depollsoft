#region Using Directives

using System.Collections.Generic;
using System.Windows;
using System.Windows.Markup;

#endregion

namespace SLaB.Navigation.Controls.Sitemap
{
    /// <summary>
    ///   Represents a Sitemap.
    /// </summary>
    [ContentProperty("Nodes")]
    public class Sitemap : DependencyObject, ISitemap
    {

        /// <summary>
        ///   Gets or sets the description for this Sitemap.
        /// </summary>
        public static readonly DependencyProperty DescriptionProperty =
            DependencyProperty.Register("Description", typeof(string), typeof(Sitemap), new PropertyMetadata(null));
        /// <summary>
        ///   Gets the list of child nodes for this Sitemap.
        /// </summary>
        private static readonly DependencyProperty NodesProperty =
            DependencyProperty.Register("Nodes",
                                        typeof(DependencyObjectCollection<ISitemapNode>),
                                        typeof(Sitemap),
                                        new PropertyMetadata(null));
        /// <summary>
        ///   Gets or sets the title of this sitemap.
        /// </summary>
        public static readonly DependencyProperty TitleProperty =
            DependencyProperty.Register("Title", typeof(string), typeof(Sitemap), new PropertyMetadata(""));



        /// <summary>
        ///   Creates a new Sitemap.
        /// </summary>
        public Sitemap()
        {
            this.Nodes = new DependencyObjectCollection<ISitemapNode>();
        }



        /// <summary>
        ///   Gets the list of child nodes for this Sitemap.
        /// </summary>
        public DependencyObjectCollection<ISitemapNode> Nodes
        {
            get { return (DependencyObjectCollection<ISitemapNode>)this.GetValue(NodesProperty); }
            private set { this.SetValue(NodesProperty, value); }
        }




        #region ISitemap Members

        IEnumerable<ISitemapNode> ISitemap.Nodes
        {
            get { return this.Nodes; }
        }

        /// <summary>
        ///   Gets or sets the title of this sitemap.
        /// </summary>
        public string Title
        {
            get { return (string)this.GetValue(TitleProperty); }
            set { this.SetValue(TitleProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the description for this Sitemap.
        /// </summary>
        public string Description
        {
            get { return (string)this.GetValue(DescriptionProperty); }
            set { this.SetValue(DescriptionProperty, value); }
        }

        #endregion
    }
}