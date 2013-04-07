#region Using Directives

using System.Windows;

#endregion

namespace SLaB.Utilities.Xap.Deployment
{
    /// <summary>
    ///   Represents information about an out-of-browser application window.
    /// </summary>
    public class WindowSettings : DependencyObject
    {
        /// <summary>
        ///   Gets or sets the initial window height of the application.
        /// </summary>
        public static readonly DependencyProperty HeightProperty =
            DependencyProperty.Register("Height",
                                        typeof(double),
                                        typeof(WindowSettings),
                                        new PropertyMetadata(default(double)));

        /// <summary>
        ///   Gets or sets the initial position of the left edge of the out-of-browser application window
        ///   when System.Windows.WindowSettings.WindowStartupLocation is System.Windows.WindowStartupLocation.Manual.
        /// </summary>
        public static readonly DependencyProperty LeftProperty =
            DependencyProperty.Register("Left",
                                        typeof(double),
                                        typeof(WindowSettings),
                                        new PropertyMetadata(default(double)));

        /// <summary>
        ///   Gets or sets the full title of the out-of-browser application for display in the title bar of the application window.
        /// </summary>
        public static readonly DependencyProperty TitleProperty =
            DependencyProperty.Register("Title",
                                        typeof(string),
                                        typeof(WindowSettings),
                                        new PropertyMetadata(default(string)));

        /// <summary>
        ///   Gets or sets the initial position of the top edge of the out-of-browser application window
        ///   when System.Windows.WindowSettings.WindowStartupLocation is System.Windows.WindowStartupLocation.Manual.
        /// </summary>
        public static readonly DependencyProperty TopProperty =
            DependencyProperty.Register("Top",
                                        typeof(double),
                                        typeof(WindowSettings),
                                        new PropertyMetadata(default(double)));

        /// <summary>
        ///   Gets or sets the initial window width of the application.
        /// </summary>
        public static readonly DependencyProperty WidthProperty =
            DependencyProperty.Register("Width",
                                        typeof(double),
                                        typeof(WindowSettings),
                                        new PropertyMetadata(default(double)));

        /// <summary>
        ///   Gets or sets a value that indicates how the out-of-browser application window is positioned at startup.
        /// </summary>
        public static readonly DependencyProperty WindowStartupLocationProperty =
            DependencyProperty.Register("WindowStartupLocation",
                                        typeof(WindowStartupLocation),
                                        typeof(WindowSettings),
                                        new PropertyMetadata(default(WindowStartupLocation)));

        /// <summary>
        ///   Gets or sets a value that indicates the appearance of the title bar and border for the out-of-browser application window.
        /// </summary>
        public static readonly DependencyProperty WindowStyleProperty =
            DependencyProperty.Register("WindowStyle",
                                        typeof(WindowStyle),
                                        typeof(WindowSettings),
                                        new PropertyMetadata(default(WindowStyle)));

        /// <summary>
        ///   Gets or sets the initial window height of the application.
        /// </summary>
        public double Height
        {
            get { return (double)this.GetValue(HeightProperty); }
            set { this.SetValue(HeightProperty, value); }
        }

        /// <summary>
        ///   Gets or sets  the initial position of the left edge of the out-of-browser application window
        ///   when System.Windows.WindowSettings.WindowStartupLocation is System.Windows.WindowStartupLocation.Manual.
        /// </summary>
        public double Left
        {
            get { return (double)this.GetValue(LeftProperty); }
            set { this.SetValue(LeftProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the full title of the out-of-browser application for display in the title bar of the application window.
        /// </summary>
        public string Title
        {
            get { return (string)this.GetValue(TitleProperty); }
            set { this.SetValue(TitleProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the initial position of the top edge of the out-of-browser application window
        ///   when System.Windows.WindowSettings.WindowStartupLocation is System.Windows.WindowStartupLocation.Manual.
        /// </summary>
        public double Top
        {
            get { return (double)this.GetValue(TopProperty); }
            set { this.SetValue(TopProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the initial window width of the application.
        /// </summary>
        public double Width
        {
            get { return (double)this.GetValue(WidthProperty); }
            set { this.SetValue(WidthProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a value that indicates how the out-of-browser application window is positioned at startup.
        /// </summary>
        public WindowStartupLocation WindowStartupLocation
        {
            get { return (WindowStartupLocation)this.GetValue(WindowStartupLocationProperty); }
            set { this.SetValue(WindowStartupLocationProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a value that indicates the appearance of the title bar and border for the out-of-browser application window.
        /// </summary>
        public WindowStyle WindowStyle
        {
            get { return (WindowStyle)this.GetValue(WindowStyleProperty); }
            set { this.SetValue(WindowStyleProperty, value); }
        }
    }
}