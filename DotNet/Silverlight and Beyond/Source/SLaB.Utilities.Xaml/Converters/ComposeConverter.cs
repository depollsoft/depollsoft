#region Using Directives

using System;
using System.Collections.ObjectModel;
using System.Globalization;
using System.Linq;
using System.Windows;
using System.Windows.Data;
using System.Windows.Markup;

#endregion

namespace SLaB.Utilities.Xaml.Converters
{
    /// <summary>
    ///   Chains a set of converters together, passing one coverter's result into the next converter.
    /// </summary>
    [ContentProperty("Converters")]
    public class ComposeConverter : DependencyObject, IValueConverter
    {

        /// <summary>
        ///   Creates a ComposeConverter.
        /// </summary>
        public ComposeConverter()
        {
            this.Converters = new ObservableCollection<IValueConverter>();
        }



        /// <summary>
        ///   Gets the set of converters to chain together.
        /// </summary>
        public ObservableCollection<IValueConverter> Converters { get; private set; }




        /// <summary>
        ///   Gets the ConverterParameter that can be attached to a given converter.
        /// </summary>
        /// <param name = "obj">The object to check the converter parameter for.</param>
        /// <returns>The object's converter parameter.</returns>
        private static object GetConverterParameter(IValueConverter obj)
        {
            return null;
        }




        #region IValueConverter Members

        /// <summary>
        ///   Chains the converters together.
        /// </summary>
        /// <param name = "value">The initial value to convert.</param>
        /// <param name = "targetType">The target type.</param>
        /// <param name = "parameter">The converter parameter.</param>
        /// <param name = "culture">The culture for the conversion.</param>
        /// <returns>The result of chaining the converters together for the given value.</returns>
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return this.Converters.Aggregate(value,
                                             (soFar, cur) =>
                                             cur.Convert(soFar, targetType, GetConverterParameter(cur), culture));
        }

        /// <summary>
        ///   Chains the converters together.
        /// </summary>
        /// <param name = "value">The initial value to convert back.</param>
        /// <param name = "targetType">The target type.</param>
        /// <param name = "parameter">The converter parameter.</param>
        /// <param name = "culture">The culture for the conversion.</param>
        /// <returns>The result of chaining the converters together for the given value.</returns>
        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return this.Converters.Reverse().Aggregate(value,
                                                       (soFar, cur) =>
                                                       cur.ConvertBack(soFar,
                                                                       targetType,
                                                                       GetConverterParameter(cur),
                                                                       culture));
        }

        #endregion
    }
}