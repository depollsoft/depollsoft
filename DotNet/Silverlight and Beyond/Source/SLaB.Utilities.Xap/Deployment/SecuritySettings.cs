#region Using Directives

using System.Windows;

#endregion

namespace SLaB.Utilities.Xap.Deployment
{
    /// <summary>
    ///   Represents the security configuration of an out-of-browser application.
    /// </summary>
    public class SecuritySettings : DependencyObject
    {
        /// <summary>
        ///   Gets or sets a value that indicates whether the out-of-browser application requires elevated permissions.
        /// </summary>
        public static readonly DependencyProperty ElevatedPermissionsProperty =
            DependencyProperty.Register("ElevatedPermissions",
                                        typeof(ElevatedPermissions),
                                        typeof(SecuritySettings),
                                        new PropertyMetadata(default(ElevatedPermissions)));

        /// <summary>
        ///   Gets or sets a value that indicates whether the out-of-browser application requires elevated permissions.
        /// </summary>
        public ElevatedPermissions ElevatedPermissions
        {
            get { return (ElevatedPermissions)this.GetValue(ElevatedPermissionsProperty); }
            set { this.SetValue(ElevatedPermissionsProperty, value); }
        }
    }
}