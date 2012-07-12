using System;
using System.Windows.Data;

namespace BarbershopTags
{
    public class TimeSpanStringConverter : IValueConverter
    {

        public object Convert(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            if (value == null)
                return null;
            TimeSpan ts = (TimeSpan)value;
            return string.Format("{0:00}:{1:00.0}", ts.Minutes, ts.TotalSeconds % 60);
        }

        public object ConvertBack(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
