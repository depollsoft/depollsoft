#region Using Directives

using System;
using System.Globalization;
using System.Windows.Data;

#endregion

namespace SLaB.Utilities.Xaml.Converters
{
    /// <summary>
    ///   A value converter that returns one value if its input is even, and another if its input is odd.
    /// </summary>
    public class EvenOddConverter : IValueConverter
    {

        /// <summary>
        ///   Gets or sets the value to return if the input is even.
        /// </summary>
        public object Even { get; set; }

        /// <summary>
        ///   Gets or sets the value to return if the input is odd.
        /// </summary>
        public object Odd { get; set; }




        #region IValueConverter Members

        /// <summary>
        ///   Converts a number into a either the value of Even or Odd.
        /// </summary>
        /// <param name = "value">The value to convert.</param>
        /// <param name = "targetType">The type of the target.</param>
        /// <param name = "parameter">The converter parameter.</param>
        /// <param name = "culture">The culture to use for conversion.</param>
        /// <returns>The value of EvenOddConverter.Even if the input is even, the value of EvenOddConverter.Odd if the input is odd,
        ///   or else the value itself.</returns>
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            ulong? uval = null;
            long? val = null;
            if (value is byte)
                val = (byte)value;
            else if (value is short)
                val = (short)value;
            else if (value is int)
                val = (int)value;
            else if (value is long)
                val = (long)value;
            else if (value is ushort)
                uval = (ushort)value;
            else if (value is uint)
                uval = (uint)value;
            else if (value is ulong)
                uval = (ulong)value;
            if (uval.HasValue)
                return uval.Value % 2 == 0 ? this.Even : this.Odd;
            if (val.HasValue)
                return val.Value % 2 == 0 ? this.Even : this.Odd;
            return value;
        }

        /// <summary>
        ///   Not implemented.
        /// </summary>
        /// <param name = "value">The value to convert.</param>
        /// <param name = "targetType">The type of the target.</param>
        /// <param name = "parameter">The converter parameter.</param>
        /// <param name = "culture">The culture to use for conversion.</param>
        /// <returns>Not implemented.</returns>
        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }

        #endregion
    }
}