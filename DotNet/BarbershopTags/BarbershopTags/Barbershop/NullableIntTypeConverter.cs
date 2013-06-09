using System;
using System.ComponentModel;
using System.Globalization;

namespace BarbershopTags.Barbershop {
  public class NullableIntTypeConverter : TypeConverter {
    public override bool CanConvertFrom(ITypeDescriptorContext context, Type sourceType) {
      return true;
    }
    public override bool CanConvertTo(ITypeDescriptorContext context, Type destinationType) {
      return true;
    }
    public override object ConvertFrom(ITypeDescriptorContext context, CultureInfo culture, object value) {
      return int.Parse((string)value);
    }
    public override object ConvertTo(ITypeDescriptorContext context, CultureInfo culture, object value, Type destinationType) {
      if (value == null)
        return null;
      return value.ToString();
    }
  }
}
