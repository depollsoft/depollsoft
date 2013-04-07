#region Using Directives

using System.Collections.Generic;

#endregion

namespace SLaB.Navigation.Controls.Sitemap
{
    /// <summary>
    ///   An interface for Sitemaps.
    /// </summary>
    public interface ISitemap
    {

        /// <summary>
        ///   Gets the description for this Sitemap.
        /// </summary>
        string Description { get; set; }

        /// <summary>
        ///   Gets the list of child nodes for this Sitemap.
        /// </summary>
        IEnumerable<ISitemapNode> Nodes { get; }

        /// <summary>
        ///   Gets the title of this sitemap.
        /// </summary>
        string Title { get; set; }
    }
}