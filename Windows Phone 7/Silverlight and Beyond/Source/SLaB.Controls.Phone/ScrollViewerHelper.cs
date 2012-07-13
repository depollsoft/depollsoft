using System.Windows;
using System.Windows.Controls;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// Provides attached properties that can be used with tombstoning.
    /// </summary>
    public static class ScrollViewerHelper
    {
        /// <summary>
        /// Gets the vertical offset of the ScrollViewer.
        /// </summary>
        /// <param name="obj">The ScrollViewer.</param>
        /// <returns>The ScrollViewer's vertical offset.</returns>
        public static double GetVerticalOffset(ScrollViewer obj)
        {
            return obj.VerticalOffset;
        }
        /// <summary>
        /// Sets the vertical offset of the ScrollViewer, scrolling to the offset if necessary.
        /// </summary>
        /// <param name="obj">The ScrollViewer.</param>
        /// <param name="value">The new vertical offset.</param>
        public static void SetVerticalOffset(ScrollViewer obj, double value)
        {
            obj.SetValue(VerticalOffsetProperty, value);
        }
        /// <summary>
        /// Represents the VerticalOffset attached property.
        /// </summary>
        public static readonly DependencyProperty VerticalOffsetProperty =
            DependencyProperty.RegisterAttached("VerticalOffset", typeof(double), typeof(ScrollViewerHelper), new PropertyMetadata(default(double), OnVerticalOffsetChanged));

        private static void OnVerticalOffsetChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            OnVerticalOffsetChanged((ScrollViewer)obj, (double)args.OldValue, (double)args.NewValue);
        }

        private static void OnVerticalOffsetChanged(ScrollViewer obj, double oldValue, double newValue)
        {
            obj.ScrollToVerticalOffset(newValue);
            obj.UpdateLayout();
            SetVerticalOffset(obj, obj.VerticalOffset);
        }

        /// <summary>
        /// Gets the horizontal offset of the ScrollViewer.
        /// </summary>
        /// <param name="obj">The ScrollViewer.</param>
        /// <returns>The ScrollViewer's horizontal offset.</returns>
        public static double GetHorizontalOffset(ScrollViewer obj)
        {
            return obj.HorizontalOffset;
        }
        /// <summary>
        /// Sets the horizontal offset of the ScrollViewer, scrolling to that offset if necessary.
        /// </summary>
        /// <param name="obj">The ScrollViewer.</param>
        /// <param name="value">The new horizontal offset.</param>
        public static void SetHorizontalOffset(ScrollViewer obj, double value)
        {
            obj.SetValue(HorizontalOffsetProperty, value);
        }

        /// <summary>
        /// Represents the HorizontalOffset attached property.
        /// </summary>
        public static readonly DependencyProperty HorizontalOffsetProperty =
            DependencyProperty.RegisterAttached("HorizontalOffset", typeof(double), typeof(ScrollViewerHelper), new PropertyMetadata(default(double), OnHorizontalOffsetChanged));

        private static void OnHorizontalOffsetChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            OnHorizontalOffsetChanged((ScrollViewer)obj, (double)args.OldValue, (double)args.NewValue);
        }

        private static void OnHorizontalOffsetChanged(ScrollViewer obj, double oldValue, double newValue)
        {
            obj.ScrollToHorizontalOffset(newValue);
            obj.UpdateLayout();
            SetHorizontalOffset(obj, obj.HorizontalOffset);
        }
    }
}
