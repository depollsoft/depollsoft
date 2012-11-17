#region Using Directives

using System;
using System.Globalization;
using System.Windows;
using System.Windows.Data;

#endregion

namespace SLaB.Utilities.Xaml.Converters
{
    /// <summary>
    ///   A ValueConverter that converts a boolean into a visibility, an integer, or inverts the boolean value.
    /// </summary>
    public class BoolConverter : IValueConverter
    {


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
            if (value == null)
            {
                if (!targetType.IsValueType)
                    return null;
                value = false;
            }
            if ((0).Equals(value))
                value = false;
            if (!(value is bool))
                value = true;
            bool realValue = (bool)value;
            if (parameter != null)
            {
                if (parameter.ToString().Trim().Equals("!"))
                    realValue = !realValue;
                else
                    throw new Exception("Parameter must be '!' to invert the boolean value.");
            }
            if (targetType == typeof(Visibility))
                return realValue ? Visibility.Visible : Visibility.Collapsed;
            if (targetType == typeof(int))
                return realValue ? 1 : 0;
            if (targetType == typeof(bool) || targetType == typeof(bool?))
                return realValue;
            if (targetType.IsValueType)
                return Activator.CreateInstance(targetType);
            return null;
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
            return null;
        }

        #endregion
    }
}