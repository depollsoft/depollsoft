#region Using Directives

using System.Windows;

#endregion

namespace SLaB.Utilities.Xap.Deployment
{
    /// <summary>
    ///   Represents information about an application that is configured for out-of-browser support.
    /// </summary>
    public class OutOfBrowserSettings : DependencyObject
    {
        /// <summary>
        ///   Gets or sets a short description of the application.
        /// </summary>
        public static readonly DependencyProperty BlurbProperty =
            DependencyProperty.Register("Blurb",
                                        typeof(string),
                                        typeof(OutOfBrowserSettings),
                                        new PropertyMetadata(default(string)));

        /// <summary>
        ///   Gets or sets a value that indicates whether to use graphics processor unit hardware acceleration
        ///   for cached compositions, which potentially results in graphics optimization.
        /// </summary>
        public static readonly DependencyProperty EnableGPUAccelerationProperty =
            DependencyProperty.Register("EnableGPUAcceleration",
                                        typeof(bool),
                                        typeof(OutOfBrowserSettings),
                                        new PropertyMetadata(default(bool)));

        /// <summary>
        ///   Gets or sets a collection of icon images associated with the application.
        /// </summary>
        public static readonly DependencyProperty IconsProperty =
            DependencyProperty.Register("Icons",
                                        typeof(IconCollection),
                                        typeof(OutOfBrowserSettings),
                                        new PropertyMetadata(default(IconCollection)));

        /// <summary>
        ///   Gets or sets the security settings applied to the out-of-browser application.
        /// </summary>
        public static readonly DependencyProperty SecuritySettingsProperty =
            DependencyProperty.Register("SecuritySettings",
                                        typeof(SecuritySettings),
                                        typeof(OutOfBrowserSettings),
                                        new PropertyMetadata(default(SecuritySettings)));

        /// <summary>
        ///   Gets or sets the short version of the application title.
        /// </summary>
        public static readonly DependencyProperty ShortNameProperty =
            DependencyProperty.Register("ShortName",
                                        typeof(string),
                                        typeof(OutOfBrowserSettings),
                                        new PropertyMetadata(default(string)));

        /// <summary>
        ///   Gets or sets a value that indicates whether the application right-click menu includes an install option.
        /// </summary>
        public static readonly DependencyProperty ShowInstallMenuItemProperty =
            DependencyProperty.Register("ShowInstallMenuItem",
                                        typeof(bool),
                                        typeof(OutOfBrowserSettings),
                                        new PropertyMetadata(default(bool)));

        /// <summary>
        ///   Gets or sets the settings applied to the application window.
        /// </summary>
        public static readonly DependencyProperty WindowSettingsProperty =
            DependencyProperty.Register("WindowSettings",
                                        typeof(WindowSettings),
                                        typeof(OutOfBrowserSettings),
                                        new PropertyMetadata(default(WindowSettings)));

        /// <summary>
        ///   Creates a new instance of OutOfBrowserSettings.
        /// </summary>
        public OutOfBrowserSettings()
        {
            this.Icons = new IconCollection();
        }

        /// <summary>
        ///   Gets or sets a short description of the application.
        /// </summary>
        public string Blurb
        {
            get { return (string)this.GetValue(BlurbProperty); }
            set { this.SetValue(BlurbProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a value that indicates whether to use graphics processor unit hardware acceleration
        ///   for cached compositions, which potentially results in graphics optimization.
        /// </summary>
        public bool EnableGPUAcceleration
        {
            get { return (bool)this.GetValue(EnableGPUAccelerationProperty); }
            set { this.SetValue(EnableGPUAccelerationProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a collection of icon images associated with the application.
        /// </summary>
        public IconCollection Icons
        {
            get { return (IconCollection)this.GetValue(IconsProperty); }
            set { this.SetValue(IconsProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the security settings applied to the out-of-browser application.
        /// </summary>
        public SecuritySettings SecuritySettings
        {
            get { return (SecuritySettings)this.GetValue(SecuritySettingsProperty); }
            set { this.SetValue(SecuritySettingsProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the short version of the application title.
        /// </summary>
        public string ShortName
        {
            get { return (string)this.GetValue(ShortNameProperty); }
            set { this.SetValue(ShortNameProperty, value); }
        }

        /// <summary>
        ///   Gets or sets a value that indicates whether the application right-click menu includes an install option.
        /// </summary>
        public bool ShowInstallMenuItem
        {
            get { return (bool)this.GetValue(ShowInstallMenuItemProperty); }
            set { this.SetValue(ShowInstallMenuItemProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the settings applied to the application window.
        /// </summary>
        public WindowSettings WindowSettings
        {
            get { return (WindowSettings)this.GetValue(WindowSettingsProperty); }
            set { this.SetValue(WindowSettingsProperty, value); }
        }
    }
}