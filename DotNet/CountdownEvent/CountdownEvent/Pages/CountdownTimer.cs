using System;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using System.Windows.Threading;
using SLaB.Utilities;

namespace CountdownEvent.Pages
{
    [StyleTypedProperty(Property = "TextStyle", StyleTargetType = typeof(TextBlock))]
    public class CountdownTimer : Control
    {
        private DispatcherTimer _Timer;
        public CountdownTimer()
        {
            this.DefaultStyleKey = typeof(CountdownTimer);
            this.Loaded += new RoutedEventHandler(OnLoaded);
            this.Unloaded += new RoutedEventHandler(OnUnloaded);
            _Timer = new DispatcherTimer();
            _Timer.Interval = TimeSpan.FromSeconds(0.2);
            _Timer.Tick += new EventHandler(TimerTick);
        }

        private void OnUnloaded(object sender, RoutedEventArgs e)
        {
            _Timer.Stop();
        }

        private void OnLoaded(object sender, RoutedEventArgs e)
        {
            _Timer.Start();
        }

        private void TimerTick(object sender, EventArgs e)
        {
            TimeSpan result = TargetDate - DateTime.Now;
            if (result < TimeSpan.Zero)
                result = TimeSpan.Zero;
            if (result == TimeSpan.Zero)
            {
                IsComplete = true;
                _Timer.Stop();
            }
            if (result.Days > 0)
            {
                HasDays = true;
                HasHours = true;
                HasMinutes = true;
                HasSeconds = true;
            }
            else if (result.Hours > 0)
            {
                HasDays = false;
                HasHours = true;
                HasMinutes = true;
                HasSeconds = true;
            }
            else if (result.Minutes > 0)
            {
                HasDays = false;
                HasHours = false;
                HasMinutes = true;
                HasSeconds = true;
            }
            else if (result.Seconds > 0)
            {
                HasDays = false;
                HasHours = false;
                HasMinutes = false;
                HasSeconds = true;
            }
            else
            {
                HasDays = false;
                HasHours = false;
                HasMinutes = false;
                HasSeconds = false;
            }
            CountdownTime = result;
        }

        public event EventHandler CountdownComplete;

        public DateTime TargetDate
        {
            get { return (DateTime)GetValue(TargetDateProperty); }
            set { SetValue(TargetDateProperty, value); }
        }

        public static readonly DependencyProperty TargetDateProperty =
            DependencyProperty.Register("TargetDate", typeof(DateTime), typeof(CountdownTimer), new PropertyMetadata(default(DateTime), OnTargetDateChanged));

        private static void OnTargetDateChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnTargetDateChanged((DateTime)args.OldValue, (DateTime)args.NewValue);
        }

        private void OnTargetDateChanged(DateTime oldValue, DateTime newValue)
        {
        }


        public TimeSpan CountdownTime
        {
            get { return (TimeSpan)GetValue(CountdownTimeProperty); }
            private set { SetValue(CountdownTimeProperty, value); }
        }

        public static readonly DependencyProperty CountdownTimeProperty =
            DependencyProperty.Register("CountdownTime", typeof(TimeSpan), typeof(CountdownTimer), new PropertyMetadata(default(TimeSpan), OnCountdownTimeChanged));

        private static void OnCountdownTimeChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnCountdownTimeChanged((TimeSpan)args.OldValue, (TimeSpan)args.NewValue);
        }

        private void OnCountdownTimeChanged(TimeSpan oldValue, TimeSpan newValue)
        {
        }


        public bool HasDays
        {
            get { return (bool)GetValue(HasDaysProperty); }
            private set { SetValue(HasDaysProperty, value); }
        }

        public static readonly DependencyProperty HasDaysProperty =
            DependencyProperty.Register("HasDays", typeof(bool), typeof(CountdownTimer), new PropertyMetadata(default(bool), OnHasDaysChanged));

        private static void OnHasDaysChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnHasDaysChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private void OnHasDaysChanged(bool oldValue, bool newValue)
        {
        }


        public bool HasHours
        {
            get { return (bool)GetValue(HasHoursProperty); }
            private set { SetValue(HasHoursProperty, value); }
        }

        public static readonly DependencyProperty HasHoursProperty =
            DependencyProperty.Register("HasHours", typeof(bool), typeof(CountdownTimer), new PropertyMetadata(default(bool), OnHasHoursChanged));

        private static void OnHasHoursChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnHasHoursChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private void OnHasHoursChanged(bool oldValue, bool newValue)
        {
        }


        public bool HasMinutes
        {
            get { return (bool)GetValue(HasMinutesProperty); }
            private set { SetValue(HasMinutesProperty, value); }
        }

        public static readonly DependencyProperty HasMinutesProperty =
            DependencyProperty.Register("HasMinutes", typeof(bool), typeof(CountdownTimer), new PropertyMetadata(default(bool), OnHasMinutesChanged));

        private static void OnHasMinutesChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnHasMinutesChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private void OnHasMinutesChanged(bool oldValue, bool newValue)
        {
        }


        public bool HasSeconds
        {
            get { return (bool)GetValue(HasSecondsProperty); }
            private set { SetValue(HasSecondsProperty, value); }
        }

        public static readonly DependencyProperty HasSecondsProperty =
            DependencyProperty.Register("HasSeconds", typeof(bool), typeof(CountdownTimer), new PropertyMetadata(default(bool), OnHasSecondsChanged));

        private static void OnHasSecondsChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnHasSecondsChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private void OnHasSecondsChanged(bool oldValue, bool newValue)
        {
        }


        public object CountdownReachedContent
        {
            get { return (object)GetValue(CountdownReachedContentProperty); }
            set { SetValue(CountdownReachedContentProperty, value); }
        }

        public static readonly DependencyProperty CountdownReachedContentProperty =
            DependencyProperty.Register("CountdownReachedContent", typeof(object), typeof(CountdownTimer), new PropertyMetadata(default(object), OnCountdownReachedContentChanged));

        private static void OnCountdownReachedContentChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnCountdownReachedContentChanged((object)args.OldValue, (object)args.NewValue);
        }

        private void OnCountdownReachedContentChanged(object oldValue, object newValue)
        {
        }


        public DataTemplate CountdownReachedContentTemplate
        {
            get { return (DataTemplate)GetValue(CountdownReachedContentTemplateProperty); }
            set { SetValue(CountdownReachedContentTemplateProperty, value); }
        }

        public static readonly DependencyProperty CountdownReachedContentTemplateProperty =
            DependencyProperty.Register("CountdownReachedContentTemplate", typeof(DataTemplate), typeof(CountdownTimer), new PropertyMetadata(default(DataTemplate), OnCountdownReachedContentTemplateChanged));

        private static void OnCountdownReachedContentTemplateChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnCountdownReachedContentTemplateChanged((DataTemplate)args.OldValue, (DataTemplate)args.NewValue);
        }

        private void OnCountdownReachedContentTemplateChanged(DataTemplate oldValue, DataTemplate newValue)
        {
        }


        public Style TextStyle
        {
            get { return (Style)GetValue(TextStyleProperty); }
            set { SetValue(TextStyleProperty, value); }
        }

        public static readonly DependencyProperty TextStyleProperty =
            DependencyProperty.Register("TextStyle", typeof(Style), typeof(CountdownTimer), new PropertyMetadata(default(Style), OnTextStyleChanged));

        private static void OnTextStyleChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnTextStyleChanged((Style)args.OldValue, (Style)args.NewValue);
        }

        private void OnTextStyleChanged(Style oldValue, Style newValue)
        {
        }

        
        public bool IsComplete
        {
            get { return (bool)GetValue(IsCompleteProperty); }
            set { SetValue(IsCompleteProperty, value); }
        }

        public static readonly DependencyProperty IsCompleteProperty =
            DependencyProperty.Register("IsComplete", typeof(bool), typeof(CountdownTimer), new PropertyMetadata(default(bool), OnIsCompleteChanged));

        private static void OnIsCompleteChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnIsCompleteChanged((bool)args.OldValue, (bool)args.NewValue);
        }

        private void OnIsCompleteChanged(bool oldValue, bool newValue)
        {
            CountdownComplete.Raise(this, new EventArgs());
        }

        
        public HorizontalAlignment TextAlignment
        {
            get { return (HorizontalAlignment)GetValue(TextAlignmentProperty); }
            set { SetValue(TextAlignmentProperty, value); }
        }

        public static readonly DependencyProperty TextAlignmentProperty =
            DependencyProperty.Register("TextAlignment", typeof(HorizontalAlignment), typeof(CountdownTimer), new PropertyMetadata(default(HorizontalAlignment), OnTextAlignmentChanged));

        private static void OnTextAlignmentChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((CountdownTimer)obj).OnTextAlignmentChanged((HorizontalAlignment)args.OldValue, (HorizontalAlignment)args.NewValue);
        }

        private void OnTextAlignmentChanged(HorizontalAlignment oldValue, HorizontalAlignment newValue)
        {
        }
    }
}
