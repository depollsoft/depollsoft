#region Using Directives

using System;
using System.Globalization;
using System.Windows.Data;

#endregion

namespace SLaB.Utilities.Xaml.Converters
{
    /// <summary>
    ///   Wraps a value in a POCO to avoid sharing issues.
    /// </summary>
    public class WrappingConverter : IValueConverter
    {


        #region IValueConverter Members

        /// <summary>
        ///   Wraps the value in a SimpleWrapper.
        /// </summary>
        /// <param name = "value">The value to wrap.</param>
        /// <param name = "targetType"></param>
        /// <param name = "parameter"></param>
        /// <param name = "culture"></param>
        /// <returns>A SimpleWrapper contianing the value.</returns>
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return new SimpleWrapper { Value = value };
        }

        /// <summary>
        ///   Not implemented.
        /// </summary>
        /// <param name = "value"></param>
        /// <param name = "targetType"></param>
        /// <param name = "parameter"></param>
        /// <param name = "culture"></param>
        /// <returns></returns>
        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }

        #endregion
    }
}