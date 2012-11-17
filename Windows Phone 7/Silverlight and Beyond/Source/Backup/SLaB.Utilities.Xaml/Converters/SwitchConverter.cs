using System;
using System.Collections.Generic;
using System.Windows.Data;
using System.Windows.Markup;

namespace SLaB.Utilities.Xaml.Converters
{
    /// <summary>
    /// Returns values as part of conversion switched on a key.
    /// </summary>
    [ContentProperty("Vals")]
    public class SwitchConverter : IValueConverter
    {

        /// <summary>
        /// Initializes a new instance of the <see cref="SwitchConverter"/> class.
        /// </summary>
        public SwitchConverter()
        {
            Vals = new List<KeyValue>();
        }



        /// <summary>
        /// Gets the Key/Value pairings that the converter will use for conversion.
        /// </summary>
        /// <value>The vals.</value>
        public List<KeyValue> Vals { get; private set; }




        /// <summary>
        /// Modifies the source data before passing it to the target for display in the UI.
        /// </summary>
        /// <param name="value">The source data being passed to the target.</param>
        /// <param name="targetType">The <see cref="T:System.Type"/> of data expected by the target dependency property.</param>
        /// <param name="parameter">An optional parameter to be used in the converter logic.</param>
        /// <param name="culture">The culture of the conversion.</param>
        /// <returns>
        /// The value to be passed to the target dependency property.
        /// </returns>
        public object Convert(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            foreach (var kv in Vals)
            {
                if (value is bool && kv.Key is uint && ((uint)kv.Key == 1 ? true : false).Equals(value))
                    return kv.Value;
                if (object.Equals(value, kv.Key))
                    return kv.Value;
                if (kv.Key == null && value == null || kv.Key != null && kv.Key.Equals(value))
                    return kv.Value;
            }
            return value;
        }

        /// <summary>
        /// Modifies the target data before passing it to the source object.  This method is called only in <see cref="F:System.Windows.Data.BindingMode.TwoWay"/> bindings.
        /// </summary>
        /// <param name="value">The target data being passed to the source.</param>
        /// <param name="targetType">The <see cref="T:System.Type"/> of data expected by the source object.</param>
        /// <param name="parameter">An optional parameter to be used in the converter logic.</param>
        /// <param name="culture">The culture of the conversion.</param>
        /// <returns>
        /// The value to be passed to the source object.
        /// </returns>
        public object ConvertBack(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }

    /// <summary>
    /// Represents a XAML-instantiable key-value pair.
    /// </summary>
    public class KeyValue
    {

        /// <summary>
        /// Gets or sets the key.
        /// </summary>
        /// <value>The key.</value>
        public object Key { get; set; }

        /// <summary>
        /// Gets or sets the value.
        /// </summary>
        /// <value>The value.</value>
        public object Value { get; set; }
    }
}
