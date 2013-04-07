using System;
using System.ComponentModel;
using BarbershopTags.Barbershop;

namespace BarbershopTags {
  public class TagCollectionTypeConverter : TypeConverter {
    public override bool CanConvertFrom(ITypeDescriptorContext context, Type sourceType) {
      return true;
    }
    public override object ConvertFrom(ITypeDescriptorContext context, System.Globalization.CultureInfo culture, object value) {
      if (value == null)
        return null;
      return Enum.Parse(typeof(TagCollection), (string)value, false);
    }
  }
}
