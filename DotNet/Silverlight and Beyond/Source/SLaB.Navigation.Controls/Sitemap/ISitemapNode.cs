#region Using Directives

using System;
using System.Collections.Generic;
using System.Security.Principal;

#endregion

namespace SLaB.Navigation.Controls.Sitemap
{
    /// <summary>
    ///   An interface for SitemapNodes.
    /// </summary>
    public interface ISitemapNode
    {

        /// <summary>
        ///   Gets the description for this SitemapNode.
        /// </summary>
        string Description { get; }

        /// <summary>
        ///   Gets whether this SitemapNode supports being navigated to.
        /// </summary>
        bool IsNavigable { get; }

        /// <summary>
        ///   Gets list of child nodes for this SitemapNode.
        /// </summary>
        IEnumerable<ISitemapNode> Nodes { get; }

        /// <summary>
        ///   Gets the target for this SitemapNode (e.g. _blank, _top, or the name of a Frame).
        /// </summary>
        string TargetName { get; }

        /// <summary>
        ///   Gets the title for this SitemapNode.
        /// </summary>
        string Title { get; }

        /// <summary>
        ///   Gets the Uri for this SitemapNode.
        /// </summary>
        Uri Uri { get; }




        /// <summary>
        ///   An event that is raised whenever this node or its children has changed.
        /// </summary>
        event EventHandler<EventArgs> NodeChanged;



        /// <summary>
        ///   Determines whether this SitemapNode should be trimmed for the given principal.
        /// </summary>
        /// <param name = "principal">The principal whose access should be tested.</param>
        /// <returns>true if the principal should see this node and its children.  false otherwise.</returns>
        bool IsVisibleTo(IPrincipal principal);
    }
}