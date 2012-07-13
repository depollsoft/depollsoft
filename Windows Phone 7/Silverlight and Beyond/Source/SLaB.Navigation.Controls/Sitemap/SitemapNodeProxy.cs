#region Using Directives

using System;
using System.Collections.Generic;
using System.Windows;
using SLaB.Utilities;
using SLaB.Utilities.ChangeLinq;

#endregion

namespace SLaB.Navigation.Controls.Sitemap
{
    /// <summary>
    ///   A Proxy for a SitemapNode that adds metadata for tracking whether the node is in the current path,
    ///   the host for the node, etc.
    /// </summary>
    public class SitemapNodeProxy : DependencyObject, IRefreshable
    {

        private readonly Predicate<ISitemapNode> _Filter;
        private readonly Func<ISitemapNode, SitemapNodeProxy> _ProxyCreator;
        /// <summary>
        ///   Gets the Host for the SitemapNode (e.g. TreeViewNavigator or BreadCrumbNavigator).
        /// </summary>
        public static readonly DependencyProperty HostProperty =
            DependencyProperty.Register("Host",
                                        typeof(DependencyObject),
                                        typeof(SitemapNodeProxy),
                                        new PropertyMetadata(null));
        /// <summary>
        ///   Gets a value indicating whether this node is in the current path but is not the current source.
        /// </summary>
        public static readonly DependencyProperty IsChildCurrentSourceProperty =
            DependencyProperty.Register("IsChildCurrentSource",
                                        typeof(bool),
                                        typeof(SitemapNodeProxy),
                                        new PropertyMetadata(false));
        /// <summary>
        ///   Gets a value indicating whether this node represents the current source.
        /// </summary>
        public static readonly DependencyProperty IsCurrentSourceProperty =
            DependencyProperty.Register("IsCurrentSource",
                                        typeof(bool),
                                        typeof(SitemapNodeProxy),
                                        new PropertyMetadata(false));
        /// <summary>
        ///   Gets a value indicating whether this node is in the current path.
        /// </summary>
        public static readonly DependencyProperty IsInPathProperty =
            DependencyProperty.Register("IsInPath", typeof(bool), typeof(SitemapNodeProxy), new PropertyMetadata(false));
        /// <summary>
        ///   Gets the ISitemapNode to which this proxy corresponds.
        /// </summary>
        private static readonly DependencyProperty NodeProperty =
            DependencyProperty.Register("Node",
                                        typeof(ISitemapNode),
                                        typeof(SitemapNodeProxy),
                                        new PropertyMetadata(null, OnNodeChanged));
        /// <summary>
        ///   Gets the trimmed list of child proxies that correspond to Node.Nodes.
        /// </summary>
        public static readonly DependencyProperty NodesProperty =
            DependencyProperty.Register("Nodes",
                                        typeof(IEnumerable<SitemapNodeProxy>),
                                        typeof(SitemapNodeProxy),
                                        new PropertyMetadata(null));



        internal SitemapNodeProxy(Predicate<ISitemapNode> filter,
                                  Func<ISitemapNode, SitemapNodeProxy> proxyCreator = null)
        {
            this._Filter = filter;
            if (proxyCreator == null)
                this._ProxyCreator =
                    node => new SitemapNodeProxy(this._Filter, this._ProxyCreator) { Node = node, Host = this.Host };
            else
                this._ProxyCreator = proxyCreator;
        }



        /// <summary>
        ///   Gets the Host for the SitemapNode (e.g. TreeViewNavigator or BreadCrumbNavigator).
        /// </summary>
        public DependencyObject Host
        {
            get { return (DependencyObject)this.GetValue(HostProperty); }
            internal set { this.SetValue(HostProperty, value); }
        }

        /// <summary>
        ///   Gets a value indicating whether this node is in the current path but is not the current source.
        /// </summary>
        public bool IsChildCurrentSource
        {
            get { return (bool)this.GetValue(IsChildCurrentSourceProperty); }
            set { this.SetValue(IsChildCurrentSourceProperty, value); }
        }

        /// <summary>
        ///   Gets a value indicating whether this node represents the current source.
        /// </summary>
        public bool IsCurrentSource
        {
            get { return (bool)this.GetValue(IsCurrentSourceProperty); }
            internal set { this.SetValue(IsCurrentSourceProperty, value); }
        }

        /// <summary>
        ///   Gets a value indicating whether this node is in the current path.
        /// </summary>
        public bool IsInPath
        {
            get { return (bool)this.GetValue(IsInPathProperty); }
            set { this.SetValue(IsInPathProperty, value); }
        }

        /// <summary>
        ///   Gets the ISitemapNode to which this proxy corresponds.
        /// </summary>
        public ISitemapNode Node
        {
            get { return (ISitemapNode)this.GetValue(NodeProperty); }
            internal set { this.SetValue(NodeProperty, value); }
        }

        /// <summary>
        ///   Gets the trimmed list of child proxies that correspond to Node.Nodes.
        /// </summary>
        public IEnumerable<SitemapNodeProxy> Nodes
        {
            get { return (IEnumerable<SitemapNodeProxy>)this.GetValue(NodesProperty); }
            internal set { this.SetValue(NodesProperty, value); }
        }




        /// <summary>
        ///   Refreshes the set of Child nodes, ensuring that it matches the list of child nodes for the underlying node.
        /// </summary>
        public void Refresh()
        {
            if (this.Nodes is IRefreshable)
                ((IRefreshable)this.Nodes).Refresh();
        }

        private void OnNodeChanged(DependencyPropertyChangedEventArgs args)
        {
            if (args.OldValue != null)
            {
                ((ISitemapNode)args.OldValue).NodeChanged -= this.SitemapNodeProxyNodeChanged;
                if (this.Nodes is IDisposable)
                {
                    foreach (var proxy in this.Nodes)
                        proxy.Node = null;
                    ((IDisposable)this.Nodes).Dispose();
                }
            }
            if (args.NewValue != null)
            {
                ((ISitemapNode)args.NewValue).NodeChanged += this.SitemapNodeProxyNodeChanged;
                this.Nodes = from node in this.Node.Nodes.AsChangeLinq()
                             where this._Filter(node)
                             select this._ProxyCreator(node);
            }
        }

        private static void OnNodeChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((SitemapNodeProxy)obj).OnNodeChanged(args);
        }

        private void SitemapNodeProxyNodeChanged(object sender, EventArgs e)
        {
            this.Refresh();
        }

        internal void Cleanup()
        {
            this.Node = null;
        }


#if DEBUG
        public override string ToString()
        {
            return string.Format("[Title = {0}, IsInPath = {1}, Nodes = {2}]",
                                 this.Node.Title,
                                 this.IsInPath,
                                 ChangeLinq.ToString(this.Nodes));
        }
#endif
    }
}