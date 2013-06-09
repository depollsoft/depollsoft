#region Using Directives

using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Linq;
using System.Security.Principal;
using System.Windows;
using System.Windows.Data;
using System.Windows.Markup;
using SLaB.Utilities;

#endregion

namespace SLaB.Navigation.Controls.Sitemap
{
    /// <summary>
    ///   Represents a node within a Sitemap.
    /// </summary>
    [ContentProperty("Nodes")]
    public class SitemapNode : DependencyObject, ISitemapNode
    {

        /// <summary>
        ///   Gets or sets the description for this SitemapNode.
        /// </summary>
        public static readonly DependencyProperty DescriptionProperty =
            DependencyProperty.Register("Description",
                                        typeof(string),
                                        typeof(SitemapNode),
                                        new PropertyMetadata(null, OnPropertyChanged));
        /// <summary>
        ///   Gets whether this SitemapNode supports being navigated to.
        /// </summary>
        public static readonly DependencyProperty IsNavigableProperty =
            DependencyProperty.Register("IsNavigable", typeof(bool), typeof(SitemapNode), new PropertyMetadata(false));
        /// <summary>
        ///   Gets list of child nodes for this SitemapNode.
        /// </summary>
        private static readonly DependencyProperty NodesProperty =
            DependencyProperty.Register("Nodes",
                                        typeof(DependencyObjectCollection<ISitemapNode>),
                                        typeof(SitemapNode),
                                        new PropertyMetadata(null, OnPropertyChanged));
        /// <summary>
        ///   Gets or sets the roles allowed for this SitemapNode.  The roles can be a comma-separated list of Role names.  "*"
        ///   indicates that all roles should be able to see this SitemapNode.
        /// </summary>
        public static readonly DependencyProperty RolesProperty =
            DependencyProperty.Register("Roles",
                                        typeof(string),
                                        typeof(SitemapNode),
                                        new PropertyMetadata("*", OnPropertyChanged));
        /// <summary>
        ///   Gets or sets the target for this SitemapNode (e.g. _blank, _top, or the name of a Frame).
        /// </summary>
        public static readonly DependencyProperty TargetNameProperty =
            DependencyProperty.Register("TargetName",
                                        typeof(string),
                                        typeof(SitemapNode),
                                        new PropertyMetadata("", OnPropertyChanged));
        /// <summary>
        ///   Gets or sets the title for this SitemapNode.
        /// </summary>
        public static readonly DependencyProperty TitleProperty =
            DependencyProperty.Register("Title",
                                        typeof(string),
                                        typeof(SitemapNode),
                                        new PropertyMetadata("", OnPropertyChanged));
        /// <summary>
        ///   Gets or sets the Uri for this SitemapNode.
        /// </summary>
        public static readonly DependencyProperty UriProperty =
            DependencyProperty.Register("Uri",
                                        typeof(Uri),
                                        typeof(SitemapNode),
                                        new PropertyMetadata(null, OnPropertyChanged));



        /// <summary>
        ///   Creates a new SitemapNode.
        /// </summary>
        public SitemapNode()
        {
            this.Nodes = new DependencyObjectCollection<ISitemapNode>();
            this.Nodes.CollectionChanged += this.NodesCollectionChanged;
            BindingOperations.SetBinding(this, TitleProperty, new Binding("Uri") { Source = this });
        }



        /// <summary>
        ///   Gets list of child nodes for this SitemapNode.
        /// </summary>
        public DependencyObjectCollection<ISitemapNode> Nodes
        {
            get { return (DependencyObjectCollection<ISitemapNode>)this.GetValue(NodesProperty); }
            protected set { this.SetValue(NodesProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the roles allowed for this SitemapNode.  The roles can be a comma-separated list of Role names.  "*"
        ///   indicates that all roles should be able to see this SitemapNode.
        /// </summary>
        public string Roles
        {
            get { return (string)this.GetValue(RolesProperty); }
            set { this.SetValue(RolesProperty, value); }
        }




        /// <summary>
        ///   Determines whether the SitemapNode is navigable.
        /// </summary>
        /// <returns></returns>
        protected virtual bool GetIsNavigable()
        {
            return this.Uri != null;
        }

        /// <summary>
        ///   Called when the node is changed.
        /// </summary>
        protected virtual void OnNodeChanged()
        {
        }

        /// <summary>
        ///   Raises the NodeChanged event.
        /// </summary>
        protected void RaiseNodeChanged()
        {
            this.NodeChanged.Raise(this, new EventArgs());
        }

        /// <summary>
        ///   Refreshes the IsNavigable property.
        /// </summary>
        protected void RefreshIsNavigable()
        {
            this.IsNavigable = this.GetIsNavigable();
        }

        private void NodeNodeChanged(object sender, EventArgs e)
        {
            this.RaiseNodeChanged();
        }

        private void NodesCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            if (e.Action == NotifyCollectionChangedAction.Reset)
            {
                if (!DesignerProperties.IsInDesignTool)
                    throw new Exception("Cannot Reset the Nodes collection");
            }
            else
            {
                if (e.NewItems != null)
                    foreach (ISitemapNode node in e.NewItems)
                        node.NodeChanged += this.NodeNodeChanged;
                if (e.OldItems != null)
                    foreach (ISitemapNode node in e.OldItems)
                        node.NodeChanged -= this.NodeNodeChanged;
            }
        }

        private void OnNodeChangedInternal()
        {
            this.OnNodeChanged();
            this.RefreshIsNavigable();
            this.RaiseNodeChanged();
        }

        private static void OnPropertyChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((SitemapNode)obj).OnNodeChangedInternal();
        }




        #region ISitemapNode Members

        /// <summary>
        ///   An event that is raised whenever this node or its children has changed.
        /// </summary>
        public event EventHandler<EventArgs> NodeChanged;

        /// <summary>
        ///   Gets whether this SitemapNode supports being navigated to.
        /// </summary>
        public bool IsNavigable
        {
            get { return (bool)this.GetValue(IsNavigableProperty); }
            private set { this.SetValue(IsNavigableProperty, value); }
        }

        IEnumerable<ISitemapNode> ISitemapNode.Nodes
        {
            get { return this.Nodes; }
        }

        /// <summary>
        ///   Gets or sets the description for this SitemapNode.
        /// </summary>
        public string Description
        {
            get { return (string)this.GetValue(DescriptionProperty); }
            set { this.SetValue(DescriptionProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the title for this SitemapNode.
        /// </summary>
        public string Title
        {
            get { return (string)this.GetValue(TitleProperty); }
            set { this.SetValue(TitleProperty, value); }
        }


        /// <summary>
        ///   Gets or sets the Uri for this SitemapNode.
        /// </summary>
        public Uri Uri
        {
            get { return (Uri)this.GetValue(UriProperty); }
            set { this.SetValue(UriProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the target for this SitemapNode (e.g. _blank, _top, or the name of a Frame).
        /// </summary>
        public string TargetName
        {
            get { return (string)this.GetValue(TargetNameProperty); }
            set { this.SetValue(TargetNameProperty, value); }
        }

        /// <summary>
        ///   Determines whether this SitemapNode should be trimmed for the given principal.
        /// </summary>
        /// <param name = "principal">The principal whose access should be tested.</param>
        /// <returns>true if the principal should see this node and its children.  false otherwise.</returns>
        public virtual bool IsVisibleTo(IPrincipal principal)
        {
            var result = (from r in (string.IsNullOrWhiteSpace(this.Roles) ? "*" : this.Roles).Split(',')
                          let role = r.Trim()
                          select role.Equals("*") || (principal != null && principal.IsInRole(role))).Any(b => b);
            return result;
        }

        #endregion
    }
}