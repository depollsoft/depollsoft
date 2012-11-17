#region Using Directives

using System;
using System.Globalization;
using System.Windows.Data;
using System.Windows.Markup;

#endregion

namespace SLaB.Utilities.Xaml.Converters
{
    /// <summary>
    ///   Creates a DependencyObject that wraps an IValueConverter.
    /// </summary>
    [ContentProperty("Converter")]
    public class ComposableConverter : IValueConverter
    {

        /// <summary>
        ///   Gets or sets the Converter for this ComposableConverter.
        /// </summary>
        public IValueConverter Converter { get; set; }




        #region IValueConverter Members

        /// <summary>
        ///   Proxies the call into the Converter.
        /// </summary>
        /// <param name = "value"></param>
        /// <param name = "targetType"></param>
        /// <param name = "parameter"></param>
        /// <param name = "culture"></param>
        /// <returns></returns>
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return this.Converter.Convert(value, targetType, parameter, culture);
        }

        /// <summary>
        ///   Proxies the call into the Converter.
        /// </summary>
        /// <param name = "value"></param>
        /// <param name = "targetType"></param>
        /// <param name = "parameter"></param>
        /// <param name = "culture"></param>
        /// <returns></returns>
        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return this.Converter.ConvertBack(value, targetType, parameter, culture);
        }

        #endregion
    }
}