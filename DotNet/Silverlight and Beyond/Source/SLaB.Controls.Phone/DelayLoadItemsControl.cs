using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Input;
using System.Windows.Threading;
using SLaB.Utilities;
using SLaB.Utilities.Xaml;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// An ItemsControl that raises an event when users scroll down to allow more items to be
    /// loaded.  When no items remain, it stops raising the event.
    /// </summary>
    [XamlDependency(typeof(Tombstoner))]
    public class DelayLoadItemsControl : ItemsControl
    {

        private ScrollViewer _ScrollViewer;
        private DispatcherTimer _Timer;
        /// <summary>
        /// Gets or sets the delay load command.
        /// </summary>
        public static readonly DependencyProperty DelayLoadCommandProperty =
            DependencyProperty.Register("DelayLoadCommand", typeof(ICommand), typeof(DelayLoadItemsControl), new PropertyMetadata(default(ICommand), OnDelayLoadCommandChanged));
        /// <summary>
        /// Gets or sets the distance from the bottom of the ScrollViewer at which loading should occur.
        /// </summary>
        public static readonly DependencyProperty DelayLoadTriggerDistanceProperty =
            DependencyProperty.Register("DelayLoadTriggerDistance", typeof(double), typeof(DelayLoadItemsControl), new PropertyMetadata(100d, OnDelayLoadTriggerDistanceChanged));
        /// <summary>
        /// Gets or sets a value indicating whether the list has more items.
        /// </summary>
        public static readonly DependencyProperty HasMoreItemsProperty =
            DependencyProperty.Register("HasMoreItems", typeof(bool), typeof(DelayLoadItemsControl), new PropertyMetadata(true, OnHasMoreItemsChanged));
        /// <summary>
        /// Gets or sets the retry time for loading new content.
        /// </summary>
        public static readonly DependencyProperty RetryTimeProperty =
            DependencyProperty.Register("RetryTime", typeof(TimeSpan), typeof(DelayLoadItemsControl), new PropertyMetadata(TimeSpan.FromSeconds(0.1), OnRetryTimeChanged));
        private static readonly DependencyProperty VerticalOffsetProperty =
            DependencyProperty.Register("VerticalOffset", typeof(double), typeof(DelayLoadItemsControl), new PropertyMetadata(default(double), OnVerticalOffsetChanged));



        /// <summary>
        /// Initializes a new instance of the <see cref="DelayLoadItemsControl"/> class.
        /// </summary>
        public DelayLoadItemsControl()
        {
            this.DefaultStyleKey = typeof(DelayLoadItemsControl);
            this.DelayLoadAction += (s, args) =>
                {
                    if (DelayLoadCommand != null && DelayLoadCommand.CanExecute(null))
                        DelayLoadCommand.Execute(null);
                };
            this.Loaded += (s, args) =>
                {
                    _Timer = new DispatcherTimer();
                    _Timer.Tick += TimerTick;
                };
            this.Unloaded += (s, args) =>
                {
                    _Timer.Tick -= TimerTick;
                    _Timer = null;
                };
        }



        /// <summary>
        /// Gets or sets the delay load command.
        /// </summary>
        /// <value>The delay load command.</value>
        public ICommand DelayLoadCommand
        {
            get { return (ICommand)GetValue(DelayLoadCommandProperty); }
            set { SetValue(DelayLoadCommandProperty, value); }
        }

        /// <summary>
        /// Gets or sets the distance from the bottom of the ScrollViewer at which loading should occur.
        /// </summary>
        /// <value>The delay load trigger distance.</value>
        public double DelayLoadTriggerDistance
        {
            get { return (double)GetValue(DelayLoadTriggerDistanceProperty); }
            set { SetValue(DelayLoadTriggerDistanceProperty, value); }
        }

        /// <summary>
        /// Gets or sets a value indicating whether the list has more items.
        /// </summary>
        /// <value>
        /// 	<c>true</c> if the list has more items; otherwise, <c>false</c>.
        /// </value>
        public bool HasMoreItems
        {
            get { return (bool)GetValue(HasMoreItemsProperty); }
            set { SetValue(HasMoreItemsProperty, value); }
        }

        /// <summary>
        /// Gets or sets the retry time for loading new content.
        /// </summary>
        /// <value>The retry time.</value>
        public TimeSpan RetryTime
        {
            get { return (TimeSpan)GetValue(RetryTimeProperty); }
            set { SetValue(RetryTimeProperty, value); }
        }

        private double VerticalOffset
        {
            get { return (double)GetValue(VerticalOffsetProperty); }
            set { SetValue(VerticalOffsetProperty, value); }
        }




        /// <summary>
        /// Occurs when new items are needed.
        /// </summary>
        public event EventHandler<EventArgs> DelayLoadAction;


        /// <summary>
        /// Gets or sets the item container style.
        /// </summary>
        /// <value>
        /// The item container style.
        /// </value>
        public Style ItemContainerStyle
        {
            get { return (Style)GetValue(ItemContainerStyleProperty); }
            set { SetValue(ItemContainerStyleProperty, value); }
        }

        /// <summary>
        /// Represents the ItemContainerStyle dependency property.
        /// </summary>
        public static readonly DependencyProperty ItemContainerStyleProperty =
            DependencyProperty.Register("ItemContainerStyle", typeof(Style), typeof(DelayLoadItemsControl), new PropertyMetadata(default(Style), OnItemContainerStyleChanged));

        private static void OnItemContainerStyleChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((DelayLoadItemsControl)obj).OnItemContainerStyleChanged((Style)args.OldValue, (Style)args.NewValue);
        }

        private void OnItemContainerStyleChanged(Style oldValue, Style newValue)
        {
        }


        /// <summary>
        /// Creates or identifies the element that is used to display the given item.
        /// </summary>
        /// <returns>
        /// The element that is used to display the given item.
        /// </returns>
        protected override DependencyObject GetContainerForItemOverride()
        {
            var result = new ContentControl();
            Binding b = new Binding("ItemContainerStyle");
            b.Source = this;
            result.SetBinding(ContentControl.StyleProperty, b);
            return result;
        }

        /// <summary>
        /// When overridden in a derived class, is invoked whenever application code or internal processes (such as a rebuilding layout pass) call <see cref="M:System.Windows.Controls.Control.ApplyTemplate"/>. In simplest terms, this means the method is called just before a UI element displays in an application. For more information, see Remarks.
        /// </summary>
        public override void OnApplyTemplate()
        {
            base.OnApplyTemplate();
            _ScrollViewer = GetTemplateChild("ScrollViewer") as ScrollViewer;
            Binding b = new Binding("VerticalOffset");
            b.Source = _ScrollViewer;
            SetBinding(VerticalOffsetProperty, b);
        }

        /// <summary>
        /// Called when the value of the <see cref="P:System.Windows.Controls.ItemsControl.Items"/> property changes.
        /// </summary>
        /// <param name="e">A <see cref="T:System.Collections.Specialized.NotifyCollectionChangedEventArgs"/> that contains the event data</param>
        protected override void OnItemsChanged(System.Collections.Specialized.NotifyCollectionChangedEventArgs e)
        {
            if (_Timer != null && HasMoreItems && _ScrollViewer != null && Math.Abs(_ScrollViewer.VerticalOffset - _ScrollViewer.ScrollableHeight) < DelayLoadTriggerDistance)
            {
                DelayLoadAction.Raise(this, new EventArgs());
                _Timer.Interval = RetryTime;
                _Timer.Start();
            }
        }

        private static void OnDelayLoadCommandChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((DelayLoadItemsControl)obj).OnDelayLoadCommandChanged((ICommand)args.OldValue, (ICommand)args.NewValue);
        }

        private void OnDelayLoadCommandChanged(ICommand oldValue, ICommand newValue)
        {
        }

        private static void OnDelayLoadTriggerDistanceChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((DelayLoadItemsControl)obj).OnDelayLoadTriggerDistanceChanged((double)args.OldValue, (double)args.NewValue);
        }

        private void OnDelayLoadTriggerDistanceChanged(double oldValue, double newValue)
        {
            if (_Timer != null && HasMoreItems && _ScrollViewer != null && Math.Abs(_ScrollViewer.VerticalOffset - _ScrollViewer.ScrollableHeight) < DelayLoadTriggerDistance)
            {
                DelayLoadAction.Raise(this, new EventArgs());
                _Timer.Interval = RetryTime;
                _Timer.Start();
            }
        }

        private static void OnHasMoreItemsChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((DelayLoadItemsControl)obj).OnHasMoreItemsChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private void OnHasMoreItemsChanged(bool oldValue, bool newValue)
        {
            if (_Timer != null && HasMoreItems && _ScrollViewer != null && Math.Abs(_ScrollViewer.VerticalOffset - _ScrollViewer.ScrollableHeight) < DelayLoadTriggerDistance)
            {
                DelayLoadAction.Raise(this, new EventArgs());
                _Timer.Interval = RetryTime;
                _Timer.Start();
            }
        }

        private static void OnRetryTimeChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((DelayLoadItemsControl)obj).OnRetryTimeChanged((TimeSpan)args.OldValue, (TimeSpan)args.NewValue);
        }

        private void OnRetryTimeChanged(TimeSpan oldValue, TimeSpan newValue)
        {
            _Timer.Interval = newValue;
        }

        private static void OnVerticalOffsetChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((DelayLoadItemsControl)obj).OnVerticalOffsetChanged((double)args.OldValue, (double)args.NewValue);
        }

        private void OnVerticalOffsetChanged(double oldValue, double newValue)
        {
            if (_Timer != null && HasMoreItems && _ScrollViewer != null && Math.Abs(_ScrollViewer.VerticalOffset - _ScrollViewer.ScrollableHeight) < DelayLoadTriggerDistance)
            {
                DelayLoadAction.Raise(this, new EventArgs());
                _Timer.Interval = RetryTime;
                _Timer.Start();
            }
        }

        private void TimerTick(object sender, EventArgs e)
        {
            if (_Timer != null)
            {
                if (HasMoreItems && _ScrollViewer != null && Math.Abs(_ScrollViewer.VerticalOffset - _ScrollViewer.ScrollableHeight) < DelayLoadTriggerDistance)
                    DelayLoadAction.Raise(this, new EventArgs());
                else
                    _Timer.Stop();
            }
        }
    }
}
