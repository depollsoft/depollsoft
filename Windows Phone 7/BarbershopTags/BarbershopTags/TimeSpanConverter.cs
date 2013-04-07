using System;
using System.Windows.Data;

namespace BarbershopTags {
  public class TimeSpanConverter : IValueConverter {
    public object Convert(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture) {
      if (!(value is TimeSpan))
        throw new ArgumentException();
      TimeSpan ts = (TimeSpan)value;
      return ts.TotalMilliseconds;
    }

    public object ConvertBack(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture) {
      if (!(value is double))
        throw new ArgumentException();
      double d = (double)value;
      return TimeSpan.FromMilliseconds(d);
    }
  }
}
