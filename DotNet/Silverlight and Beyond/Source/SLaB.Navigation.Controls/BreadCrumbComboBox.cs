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
    ///   Represents a customized ComboBox that is used within the BreadCrumbNavigator.
    /// </summary>
    [XamlDependency(typeof(BoolConverter))]
    [XamlDependency(typeof(CollectionConverter))]
    [XamlDependency(typeof(WrappingConverter))]
    public class BreadCrumbComboBox : ComboBox
    {

        /// <summary>
        ///   Creates a BreadCrumbComboBox.
        /// </summary>
        public BreadCrumbComboBox()
        {
            this.DefaultStyleKey = typeof(BreadCrumbComboBox);
        }




        /// <summary>
        ///   Prepares a ComboBoxItem, disabling HitTestVisibility if the SitemapNodeProxy is not Navigable.
        /// </summary>
        /// <param name = "element">The container being prepared.</param>
        /// <param name = "item">The item represented by the container.</param>
        protected override void PrepareContainerForItemOverride(DependencyObject element, object item)
        {
            base.PrepareContainerForItemOverride(element, item);
            ((ComboBoxItem)element).IsHitTestVisible = ((SitemapNodeProxy)item).Node.IsNavigable;
        }
    }
}