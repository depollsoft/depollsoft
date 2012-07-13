#region Using Directives

using System.Windows;
using System.Windows.Controls;
using SLaB.Navigation.Controls.Sitemap;
using SLaB.Utilities.Xaml;
using SLaB.Utilities.Xaml.Converters;

#endregion

namespace SLaB.Navigation.Controls
{
    /// <summary>
    ///   A control for TreeView-based navigation within a Silverlight application.
    /// </summary>
    [StyleTypedProperty(Property = "ItemContainerStyle", StyleTargetType = typeof(TreeViewItem))]
    [StyleTypedProperty(Property = "TreeViewStyle", StyleTargetType = typeof(TreeView))]
    [TemplatePart(Name = "TreeView", Type = typeof(TreeView))]
    [XamlDependency(typeof(BoolConverter))]
    [XamlDependency(typeof(CollectionConverter))]
    [XamlDependency(typeof(WrappingConverter))]
    [XamlDependency(typeof(TreeView))]
    [XamlDependency(typeof(HierarchicalDataTemplate))]
    public class TreeViewNavigator : Navigator
    {

        private TreeView _Treeview;
        /// <summary>
        ///   Gets or sets whether all items in the TreeView should be expanded.
        /// </summary>
        public static readonly DependencyProperty ExpandAllProperty =
            DependencyProperty.Register("ExpandAll",
                                        typeof(bool),
                                        typeof(TreeViewNavigator),
                                        new PropertyMetadata(false, OnExpandAllChanged));
        /// <summary>
        ///   Gets or sets the style for the ItemContainer that will be used in the TreeView.
        /// </summary>
        public static readonly DependencyProperty ItemContainerStyleProperty =
            DependencyProperty.Register("ItemContainerStyle",
                                        typeof(Style),
                                        typeof(TreeViewNavigator),
                                        new PropertyMetadata(null));
        /// <summary>
        ///   Gets or sets the style for the TreeView.
        /// </summary>
        public static readonly DependencyProperty TreeViewStyleProperty =
            DependencyProperty.Register("TreeViewStyle",
                                        typeof(Style),
                                        typeof(TreeViewNavigator),
                                        new PropertyMetadata(null));



        /// <summary>
        ///   Creates a TreeViewNavigator.
        /// </summary>
        public TreeViewNavigator()
        {
            this.DefaultStyleKey = typeof(TreeViewNavigator);
        }



        /// <summary>
        ///   Gets or sets whether all items in the TreeView should be expanded.
        /// </summary>
        public bool ExpandAll
        {
            get { return (bool)this.GetValue(ExpandAllProperty); }
            set { this.SetValue(ExpandAllProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the style for the ItemContainer that will be used in the TreeView.
        /// </summary>
        public Style ItemContainerStyle
        {
            get { return (Style)this.GetValue(ItemContainerStyleProperty); }
            set { this.SetValue(ItemContainerStyleProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the style for the TreeView.
        /// </summary>
        public Style TreeViewStyle
        {
            get { return (Style)this.GetValue(TreeViewStyleProperty); }
            set { this.SetValue(TreeViewStyleProperty, value); }
        }




        /// <summary>
        ///   Called when a template is applied.
        /// </summary>
        public override void OnApplyTemplate()
        {
            base.OnApplyTemplate();
            this._Treeview = (TreeView)this.GetTemplateChild("TreeView");
            this.RefreshExpansion();
        }

        /// <summary>
        ///   Called when CurrentSource changes.
        /// </summary>
        protected override void OnCurrentSourceChanged()
        {
            this.RefreshExpansion();
        }

        private static void Expand(SitemapNodeProxy item)
        {
            item.IsInPath = true;
            foreach (SitemapNodeProxy child in item.Nodes)
                Expand(child);
        }

        private void OnExpandAllChanged()
        {
            this.RefreshExpansion();
        }

        private static void OnExpandAllChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((TreeViewNavigator)obj).OnExpandAllChanged();
        }

        private void RefreshExpansion()
        {
            if (this.ExpandAll && this._Treeview != null)
            {
                foreach (SitemapNodeProxy item in this._Treeview.Items)
                {
                    Expand(item);
                }
            }
        }
    }
}