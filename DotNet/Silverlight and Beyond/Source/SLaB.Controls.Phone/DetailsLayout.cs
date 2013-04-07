using System;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Markup;
using SLaB.Utilities;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// Produces a layout using a grid of Header-content rows where columns are sized based upon the largest
    /// header size.
    /// </summary>
    [ContentProperty("DetailDescriptions")]
    [TemplatePart(Name = "LayoutGrid", Type = typeof(Grid))]
    public class DetailsLayout : Control
    {

        private bool _IsRefreshing;
        private Grid _LayoutGrid;
        /// <summary>
        /// Gets or sets the width of the content column.
        /// </summary>
        public static readonly DependencyProperty ContentColumnWidthProperty =
            DependencyProperty.Register("ContentColumnWidth", typeof(GridLength), typeof(DetailsLayout), new PropertyMetadata(default(GridLength), OnContentColumnWidthChanged));
        /// <summary>
        /// Gets or sets the detail descriptions.
        /// </summary>
        public static readonly DependencyProperty DetailDescriptionsProperty =
            DependencyProperty.Register("DetailDescriptions", typeof(ObservableCollection<Detail>), typeof(DetailsLayout), new PropertyMetadata(default(ObservableCollection<Detail>), OnDetailDescriptionsChanged));
        /// <summary>
        /// Gets or sets the width of the header column.
        /// </summary>
        public static readonly DependencyProperty HeaderColumnWidthProperty =
            DependencyProperty.Register("HeaderColumnWidth", typeof(GridLength), typeof(DetailsLayout), new PropertyMetadata(default(GridLength), OnHeaderColumnWidthChanged));



        /// <summary>
        /// Initializes a new instance of the <see cref="DetailsLayout"/> class.
        /// </summary>
        public DetailsLayout()
        {
            this.DefaultStyleKey = typeof(DetailsLayout);
            DetailDescriptions = new ObservableCollection<Detail>();
            Dispatcher.BeginInvoke(() =>
                {
                    DetailDescriptions.CollectionChanged += new NotifyCollectionChangedEventHandler(DetailDescriptions_CollectionChanged);
                });
        }



        /// <summary>
        /// Gets or sets the width of the content column.
        /// </summary>
        /// <value>The width of the content column.</value>
        public GridLength ContentColumnWidth
        {
            get { return (GridLength)GetValue(ContentColumnWidthProperty); }
            set { SetValue(ContentColumnWidthProperty, value); }
        }

        /// <summary>
        /// Gets or sets the detail descriptions.
        /// </summary>
        /// <value>The detail descriptions.</value>
        public ObservableCollection<Detail> DetailDescriptions
        {
            get { return (ObservableCollection<Detail>)GetValue(DetailDescriptionsProperty); }
            private set { SetValue(DetailDescriptionsProperty, value); }
        }

        /// <summary>
        /// Gets or sets the width of the header column.
        /// </summary>
        /// <value>The width of the header column.</value>
        public GridLength HeaderColumnWidth
        {
            get { return (GridLength)GetValue(HeaderColumnWidthProperty); }
            set { SetValue(HeaderColumnWidthProperty, value); }
        }




        /// <summary>
        /// When overridden in a derived class, is invoked whenever application code or internal processes (such as a rebuilding layout pass) call <see cref="M:System.Windows.Controls.Control.ApplyTemplate"/>. In simplest terms, this means the method is called just before a UI element displays in an application. For more information, see Remarks.
        /// </summary>
        public override void OnApplyTemplate()
        {
            if (_LayoutGrid != null)
                ClearGridContent();
            base.OnApplyTemplate();
            _LayoutGrid = GetTemplateChild("LayoutGrid") as Grid;
            RefreshContent();
        }

        private void BeginRefreshContent()
        {
            if (_IsRefreshing)
                return;
            Dispatcher.BeginInvoke(() =>
            {
                RefreshContent();
                _IsRefreshing = false;
            });
            _IsRefreshing = true;
        }

        private void ClearGridContent()
        {
            foreach (var child in _LayoutGrid.Children)
                ((ContentPresenter)child).Content = null;
            _LayoutGrid.Children.Clear();
            _LayoutGrid.ColumnDefinitions.Clear();
        }

        private void DetailDescriptions_CollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            foreach (var detail in DetailDescriptions)
                detail.Changed = (s, a) => BeginRefreshContent();
            BeginRefreshContent();
        }

        private static void OnContentColumnWidthChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((DetailsLayout)obj).OnContentColumnWidthChanged((GridLength)args.OldValue, (GridLength)args.NewValue);
        }

        private void OnContentColumnWidthChanged(GridLength oldValue, GridLength newValue)
        {
            BeginRefreshContent();
        }

        private static void OnDetailDescriptionsChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((DetailsLayout)obj).OnDetailDescriptionsChanged((ObservableCollection<Detail>)args.OldValue, (ObservableCollection<Detail>)args.NewValue);
        }

        private void OnDetailDescriptionsChanged(ObservableCollection<Detail> oldValue, ObservableCollection<Detail> newValue)
        {
        }

        private static void OnHeaderColumnWidthChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((DetailsLayout)obj).OnHeaderColumnWidthChanged((GridLength)args.OldValue, (GridLength)args.NewValue);
        }

        private void OnHeaderColumnWidthChanged(GridLength oldValue, GridLength newValue)
        {
            BeginRefreshContent();
        }

        private void RefreshContent()
        {
            if (_LayoutGrid != null)
            {
                Binding dcBinding = new Binding("DataContext");
                dcBinding.Source = this;
                //_LayoutGrid.SetBinding(FrameworkElement.DataContextProperty, dcBinding);
                ClearGridContent();
                _LayoutGrid.ColumnDefinitions.Add(new ColumnDefinition() { Width = HeaderColumnWidth });
                _LayoutGrid.ColumnDefinitions.Add(new ColumnDefinition() { Width = ContentColumnWidth });
                _LayoutGrid.RowDefinitions.Clear();
                int rowIndex = 0;
                foreach (var detail in DetailDescriptions)
                {
                    _LayoutGrid.RowDefinitions.Add(new RowDefinition());
                    if (!detail.ExtendContent)
                    {
                        var header = new ContentPresenter { Content = detail.Header };
                        Grid.SetRow(header, rowIndex);
                        _LayoutGrid.Children.Add(header);
                    }
                    var content = new ContentPresenter { Content = detail.Content };
                    Grid.SetRow(content, rowIndex);
                    Grid.SetColumn(content, detail.ExtendContent ? 0 : 1);
                    Grid.SetColumnSpan(content, detail.ExtendContent ? 2 : 1);
                    _LayoutGrid.Children.Add(content);
                    rowIndex++;
                }
            }
        }
    }

    /// <summary>
    /// Represents an item (header/content) within a DetailsLayout.
    /// </summary>
    public class Detail : DependencyObject
    {

        internal EventHandler<EventArgs> Changed;
        /// <summary>
        /// Gets or sets the content.
        /// </summary>
        public static readonly DependencyProperty ContentProperty =
            DependencyProperty.Register("Content", typeof(object), typeof(Detail), new PropertyMetadata(default(object), OnContentChanged));
        /// <summary>
        /// Gets or sets a value indicating whether content should be extended to both columns.
        /// </summary>
        public static readonly DependencyProperty ExtendContentProperty =
            DependencyProperty.Register("ExtendContent", typeof(bool), typeof(Detail), new PropertyMetadata(default(bool), OnExtendContentChanged));
        /// <summary>
        /// Gets or sets the header.
        /// </summary>
        public static readonly DependencyProperty HeaderProperty =
            DependencyProperty.Register("Header", typeof(object), typeof(Detail), new PropertyMetadata(default(object), OnHeaderChanged));



        /// <summary>
        /// Gets or sets the content.
        /// </summary>
        /// <value>The content.</value>
        public object Content
        {
            get { return (object)GetValue(ContentProperty); }
            set { SetValue(ContentProperty, value); }
        }

        /// <summary>
        /// Gets or sets a value indicating whether content should be extended to both columns.
        /// </summary>
        /// <value><c>true</c> if [extend content]; otherwise, <c>false</c>.</value>
        public bool ExtendContent
        {
            get { return (bool)GetValue(ExtendContentProperty); }
            set { SetValue(ExtendContentProperty, value); }
        }

        /// <summary>
        /// Gets or sets the header.
        /// </summary>
        /// <value>The header.</value>
        public object Header
        {
            get { return (object)GetValue(HeaderProperty); }
            set { SetValue(HeaderProperty, value); }
        }




        private static void OnContentChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((Detail)obj).OnContentChanged((object)args.OldValue, (object)args.NewValue);
        }

        private void OnContentChanged(object oldValue, object newValue)
        {
            Changed.Raise(this, null);
        }

        private static void OnExtendContentChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((Detail)obj).OnExtendContentChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private void OnExtendContentChanged(bool oldValue, bool newValue)
        {
            Changed.Raise(this, null);
        }

        private static void OnHeaderChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((Detail)obj).OnHeaderChanged((object)args.OldValue, (object)args.NewValue);
        }

        private void OnHeaderChanged(object oldValue, object newValue)
        {
            Changed.Raise(this, null);
        }
    }
}
