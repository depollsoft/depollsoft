using System.Windows;
using Microsoft.Phone.Controls;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// Provides attached properties to help with tombstoning.
    /// </summary>
    public static class PivotHelper
    {
        /// <summary>
        /// Gets the selected index.
        /// </summary>
        /// <param name="obj">The pivot.</param>
        /// <returns>The selected index.</returns>
        public static int GetSelectedIndex(Pivot obj)
        {
            return obj.SelectedIndex;
        }
        /// <summary>
        /// Sets the selected index.
        /// </summary>
        /// <param name="obj">The pivot.</param>
        /// <param name="value">The selected index.</param>
        public static void SetSelectedIndex(Pivot obj, int value)
        {
            obj.SetValue(SelectedIndexProperty, value);
        }
        /// <summary>
        /// Represents the attached SelectedIndex property.
        /// </summary>
        public static readonly DependencyProperty SelectedIndexProperty =
            DependencyProperty.RegisterAttached("SelectedIndex", typeof(int), typeof(PivotHelper), new PropertyMetadata(default(int), OnSelectedIndexChanged));

        private static void OnSelectedIndexChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            OnSelectedIndexChanged((Pivot)obj, (int)args.OldValue, (int)args.NewValue);
        }

        private static void OnSelectedIndexChanged(Pivot obj, int oldValue, int newValue)
        {
            while (obj.SelectedIndex != newValue)
                obj.SelectedIndex = (obj.SelectedIndex + 1) % obj.Items.Count;
        }

    }
}
