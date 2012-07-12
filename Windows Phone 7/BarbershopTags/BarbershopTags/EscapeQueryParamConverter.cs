using System;
using System.Windows.Data;

namespace BarbershopTags
{
    public class EscapeQueryParamConverter : IValueConverter
    {

        public object Convert(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            return new Uri(parameter + Uri.EscapeDataString("" + value), UriKind.Relative);
        }

        public object ConvertBack(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
