using System.Windows;
using System.Windows.Controls;
using System.Windows.Markup;
using SLaB.Utilities;

namespace SLaB.Controls.Phone
{
    /// <summary>
    /// A pseudo-content control that delays creation of its content from a template
    /// until it has been loaded.
    /// </summary>
    [ContentProperty("ContentTemplate")]
    public class DelayCreateContentControl : Control
    {

        /// <summary>
        /// Gets or sets the content.
        /// </summary>
        public static readonly DependencyProperty ContentProperty =
            DependencyProperty.Register("Content", typeof(object), typeof(DelayCreateContentControl), new PropertyMetadata(null));
        /// <summary>
        /// Gets or sets the content template.
        /// </summary>
        public static readonly DependencyProperty ContentTemplateProperty =
            DependencyProperty.Register("ContentTemplate", typeof(DataTemplate), typeof(DelayCreateContentControl), new PropertyMetadata(null));

        /// <summary>
        /// Initializes a new instance of the <see cref="DelayCreateContentControl"/> class.
        /// </summary>
        public DelayCreateContentControl()
        {
            DefaultStyleKey = typeof(DelayCreateContentControl);
            this.Loaded += DelayCreateContentControlLoaded;
        }



        /// <summary>
        /// Gets or sets the content.
        /// </summary>
        /// <value>The content.</value>
        public object Content
        {
            get { return (object)GetValue(ContentProperty); }
            private set { SetValue(ContentProperty, value); }
        }

        /// <summary>
        /// Gets or sets the content template.
        /// </summary>
        /// <value>The content template.</value>
        public DataTemplate ContentTemplate
        {
            get { return (DataTemplate)GetValue(ContentTemplateProperty); }
            set { SetValue(ContentTemplateProperty, value); }
        }

        private void DelayCreateContentControlLoaded(object sender, RoutedEventArgs e)
        {
            Dispatcher.DelayUntil(() =>
            {
                Dispatcher.BeginInvoke(() =>
                    {
                        this.Loaded -= DelayCreateContentControlLoaded;
                        Content = ContentTemplate.LoadContent();
                        (Content as FrameworkElement).DataContext = this.DataContext;
                    });
            }, () => ContentTemplate != null);
        }
    }
}
