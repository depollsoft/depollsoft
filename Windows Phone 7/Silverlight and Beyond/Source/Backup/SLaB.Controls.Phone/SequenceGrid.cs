using System;
using System.Windows;
using System.Windows.Controls;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// SequenceGrid is a panel that lays out items in a number of columns, wrapping them as they go exceed the number of columns.
    /// </summary>
    public class SequenceGrid : Panel
    {
        private double[] _RowHeights;
        private double[] _RowStarts;
        /// <summary>
        /// Constructs a SequenceGrid.
        /// </summary>
        public SequenceGrid()
        {
        }

        /// <summary>
        /// Gets or sets the maximum row height for rows in the grid.
        /// </summary>
        public double MaxRowHeight
        {
            get { return (double)GetValue(MaxRowHeightProperty); }
            set { SetValue(MaxRowHeightProperty, value); }
        }
        /// <summary>
        /// Gets or sets the maximum row height for rows in the grid.
        /// </summary>
        public static readonly DependencyProperty MaxRowHeightProperty =
            DependencyProperty.Register("MaxRowHeight", typeof(double), typeof(SequenceGrid), new PropertyMetadata(double.PositiveInfinity, OnMaxRowHeightChanged));

        private static void OnMaxRowHeightChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((SequenceGrid)obj).OnMaxRowHeightChanged((double)args.OldValue, (double)args.NewValue);
        }

        private void OnMaxRowHeightChanged(double oldValue, double newValue)
        {
            InvalidateMeasure();
            InvalidateArrange();
        }

        /// <summary>
        /// Gets or sets the number of Columns in the SequenceGrid.
        /// </summary>
        public int ColumnCount
        {
            get { return (int)GetValue(ColumnCountProperty); }
            set { SetValue(ColumnCountProperty, value); }
        }
        /// <summary>
        /// Gets or sets the number of Columns in the SequenceGrid.
        /// </summary>
        public static readonly DependencyProperty ColumnCountProperty =
            DependencyProperty.Register("ColumnCount", typeof(int), typeof(SequenceGrid), new PropertyMetadata(1, OnColumnCountChanged));

        private static void OnColumnCountChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((SequenceGrid)obj).OnColumnCountChanged((int)args.OldValue, (int)args.NewValue);
        }

        private void OnColumnCountChanged(int oldValue, int newValue)
        {
            InvalidateMeasure();
            InvalidateArrange();
        }

        /// <summary>
        /// Provides the behavior for the Measure pass of Silverlight layout. Classes can override this method to define their own Measure pass behavior.
        /// </summary>
        /// <param name="availableSize">The available size that this object can give to child objects. Infinity (<see cref="F:System.Double.PositiveInfinity"/>) can be specified as a value to indicate that the object will size to whatever content is available.</param>
        /// <returns>
        /// The size that this object determines it needs during layout, based on its calculations of the allocated sizes for child objects; or based on other considerations, such as a fixed container size.
        /// </returns>
        protected override Size MeasureOverride(Size availableSize)
        {
            _RowHeights = new double[Children.Count / ColumnCount + (Children.Count % ColumnCount != 0 ? 1 : 0)];
            _RowStarts = new double[_RowHeights.Length];
            double columnWidth = availableSize.Width / ColumnCount;
            for (int x = 0, y = 0, rowNum = 0; x < Children.Count; x++, y = x % ColumnCount, rowNum = x / ColumnCount)
            {
                Children[x].Measure(new Size(columnWidth, Math.Min(double.PositiveInfinity, MaxRowHeight)));
                _RowHeights[rowNum] = Math.Max(_RowHeights[rowNum], Children[x].DesiredSize.Height);
            }
            double soFar = 0;
            for (int x = 0; x < _RowHeights.Length; x++)
            {
                _RowStarts[x] = soFar;
                soFar += _RowHeights[x];
            }
            return new Size(availableSize.Width, soFar);
        }
        /// <summary>
        /// Provides the behavior for the Arrange pass of Silverlight layout. Classes can override this method to define their own Arrange pass behavior.
        /// </summary>
        /// <param name="finalSize">The final area within the parent that this object should use to arrange itself and its children.</param>
        /// <returns>
        /// The actual size that is used after the element is arranged in layout.
        /// </returns>
        protected override Size ArrangeOverride(Size finalSize)
        {
            double columnWidth = finalSize.Width / ColumnCount;
            for (int x = 0, y = 0, rowNum = 0; x < Children.Count; x++, y = x % ColumnCount, rowNum = x / ColumnCount)
                Children[x].Arrange(new Rect(y * columnWidth, _RowStarts[rowNum], columnWidth, _RowHeights[rowNum]));
            return finalSize;
        }
    }
}
