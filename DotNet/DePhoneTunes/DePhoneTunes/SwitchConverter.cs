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
using System.Collections.Generic;
using System.Windows.Markup;

namespace DePhoneTunes {
  [ContentProperty("Vals")]
  public class SwitchConverter : IValueConverter {
    public SwitchConverter() {
      Vals = new List<KeyValue>();
    }

    public object Convert(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture) {
      foreach (var kv in Vals) {
        if (object.Equals(value, kv.Key))
          return kv.Value;
      }
      return null;
    }

    public List<KeyValue> Vals { get; private set; }

    public object ConvertBack(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture) {
      throw new NotImplementedException();
    }
  }

  public class KeyValue {
    public object Key { get; set; }
    public object Value { get; set; }
  }
}
