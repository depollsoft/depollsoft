using System;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Ink;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Shapes;
using System.Windows.Data;

namespace DePhoneTunes {
  public class KeyBoolConverter : IValueConverter {

    public object Convert(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture) {
      KeyType kt = (KeyType)value;
      if (kt == KeyType.Major)
        return true;
      return false;
    }

    public object ConvertBack(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture) {
      bool val = (bool)value;
      if (val)
        return KeyType.Major;
      return KeyType.Minor;
    }
  }
}
