#region Using Directives

using System;
using System.Windows;
using System.Windows.Markup;

#endregion

namespace SLaB.Utilities.Xap.Deployment
{
    /// <summary>
    ///   Represents an icon that is used to identify an offline application.
    /// </summary>
    [ContentProperty("Source")]
    public class Icon : DependencyObject
    {
        /// <summary>
        ///   Gets or sets the icon size.
        /// </summary>
        public static readonly DependencyProperty SizeProperty =
            DependencyProperty.Register("Size", typeof(Size), typeof(Icon), new PropertyMetadata(default(Size)));

        /// <summary>
        ///   Gets or sets the path and file name to the PNG source file of the icon.
        /// </summary>
        public static readonly DependencyProperty SourceProperty =
            DependencyProperty.Register("Source", typeof(Uri), typeof(Icon), new PropertyMetadata(default(Uri)));

        /// <summary>
        ///   Gets or sets the icon size.
        /// </summary>
        public Size Size
        {
            get { return (Size)this.GetValue(SizeProperty); }
            set { this.SetValue(SizeProperty, value); }
        }

        /// <summary>
        ///   Gets or sets the path and file name to the PNG source file of the icon.
        /// </summary>
        public Uri Source
        {
            get { return (Uri)this.GetValue(SourceProperty); }
            set { this.SetValue(SourceProperty, value); }
        }
    }
}