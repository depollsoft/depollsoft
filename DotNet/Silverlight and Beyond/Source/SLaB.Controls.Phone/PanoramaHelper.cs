using System.Windows;
using Microsoft.Phone.Controls;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// Provides attached properties to help with tombstoning
    /// </summary>
    public static class PanoramaHelper
    {
        /// <summary>
        /// Gets the currently selected index of the Panorama.
        /// </summary>
        /// <param name="obj">The panorama.</param>
        /// <returns>Panorama.SelectedIndex</returns>
        public static int GetDefaultIndex(Panorama obj)
        {
            return obj.SelectedIndex;
        }

        /// <summary>
        /// Sets the DefaultItem of the Panorama based upon the given index.
        /// </summary>
        /// <param name="obj">The panorma.</param>
        /// <param name="value">The index to use as the new DefaultItem.</param>
        public static void SetDefaultIndex(Panorama obj, int value)
        {
            obj.SetValue(DefaultIndexProperty, value);
        }

        /// <summary>
        /// Represents the DefaultIndex attached dependency property.
        /// </summary>
        public static readonly DependencyProperty DefaultIndexProperty =
            DependencyProperty.RegisterAttached("DefaultIndex", typeof(int), typeof(PanoramaHelper), new PropertyMetadata(default(int), OnDefaultIndexChanged));

        private static void OnDefaultIndexChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            OnDefaultIndexChanged((Panorama)obj, (int)args.OldValue, (int)args.NewValue);
        }

        private static void OnDefaultIndexChanged(Panorama obj, int oldValue, int newValue)
        {
            obj.DefaultItem = obj.Items[newValue];
        }

    }
}
