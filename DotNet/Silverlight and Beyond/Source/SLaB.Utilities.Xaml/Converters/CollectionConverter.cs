#region Using Directives

using System;
using System.Collections;
using System.Globalization;
using System.Linq;
using System.Windows.Data;

#endregion

namespace SLaB.Utilities.Xaml.Converters
{
    /// <summary>
    ///   Converts a collection into a boolean/Visibility/etc. based upon whether the collection is empty.
    /// </summary>
    public class CollectionConverter : IValueConverter
    {

        private readonly BoolConverter _Bc = new BoolConverter();




        #region IValueConverter Members

        /// <summary>
        ///   Converts the value.
        /// </summary>
        /// <param name = "value">The value to convert.</param>
        /// <param name = "targetType">The target type for the conversion.</param>
        /// <param name = "parameter">The converter parameter.  If this value is "!", the boolean value will be inverted.</param>
        /// <param name = "culture">The culture to use for conversion.</param>
        /// <returns>A value of the TargetType.</returns>
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            IEnumerable enumerable = value as IEnumerable ?? new object[0];
            int count = enumerable.Cast<object>().Count();
            bool result = count > 0;
            return this._Bc.Convert(result, targetType, parameter, culture);
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