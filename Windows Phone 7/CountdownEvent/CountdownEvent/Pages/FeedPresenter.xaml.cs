using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using CountdownEvent.Readers;
using Microsoft.Phone.Info;

namespace CountdownEvent.Pages
{
    public partial class FeedPresenter : UserControl
    {
        public FeedPresenter()
        {
            InitializeComponent();
            this.LayoutRoot.DataContext = this;
        }


        public DataTemplate ItemTemplate
        {
            get { return (DataTemplate)GetValue(ItemTemplateProperty); }
            set { SetValue(ItemTemplateProperty, value); }
        }

        public static readonly DependencyProperty ItemTemplateProperty =
            DependencyProperty.Register("ItemTemplate", typeof(DataTemplate), typeof(FeedPresenter), new PropertyMetadata(default(DataTemplate), OnItemTemplateChanged));

        private static void OnItemTemplateChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((FeedPresenter)obj).OnItemTemplateChanged((DataTemplate)args.OldValue, (DataTemplate)args.NewValue);
        }

        private void OnItemTemplateChanged(DataTemplate oldValue, DataTemplate newValue)
        {
        }


        public ItemsPanelTemplate ItemsPanel
        {
            get { return (ItemsPanelTemplate)GetValue(ItemsPanelProperty); }
            set { SetValue(ItemsPanelProperty, value); }
        }

        public static readonly DependencyProperty ItemsPanelProperty =
            DependencyProperty.Register("ItemsPanel", typeof(ItemsPanelTemplate), typeof(FeedPresenter), new PropertyMetadata(default(ItemsPanelTemplate), OnItemsPanelChanged));

        private static void OnItemsPanelChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((FeedPresenter)obj).OnItemsPanelChanged((ItemsPanelTemplate)args.OldValue, (ItemsPanelTemplate)args.NewValue);
        }

        private void OnItemsPanelChanged(ItemsPanelTemplate oldValue, ItemsPanelTemplate newValue)
        {
        }

        public Feed Feed
        {
            get { return (Feed)GetValue(FeedProperty); }
            set { SetValue(FeedProperty, value); }
        }

        public static readonly DependencyProperty FeedProperty =
            DependencyProperty.Register("Feed", typeof(Feed), typeof(FeedPresenter), new PropertyMetadata(default(Feed), OnFeedChanged));

        private static void OnFeedChanged(DependencyObject obj, DependencyPropertyChangedEventArgs args)
        {
            ((FeedPresenter)obj).OnFeedChanged((Feed)args.OldValue, (Feed)args.NewValue);
        }

        private void OnFeedChanged(Feed oldValue, Feed newValue)
        {
        }

        private bool HasEnoughMemory
        {
            get
            {
                long totalMemory = (long)DeviceExtendedProperties.GetValue("DeviceTotalMemory");
                if (totalMemory > 256000000)
                    return true;
                long currentMemory = (long)DeviceExtendedProperties.GetValue("ApplicationCurrentMemoryUsage");
                if (currentMemory > 85000000)
                    return false;
                return true;
            }
        }

        private void DelayLoadAction(object sender, EventArgs e)
        {
            if (Feed.HasMoreItems && HasEnoughMemory)
                Feed.Fetch();
        }

        public void Load()
        {
            if (Feed.HasMoreItems && Feed.Items.Count == 0 && HasEnoughMemory)
                Feed.Fetch();
        }
    }
}
