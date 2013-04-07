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
using System.ComponentModel;
using System.Globalization;

namespace SLaB.Utilities.Xaml.Deserializer
{
    internal class DelegateTypeConverter<T> : TypeConverter
    {
        private Func<ITypeDescriptorContext, CultureInfo, string, T> _ConvertFrom;
        public DelegateTypeConverter(Func<ITypeDescriptorContext, CultureInfo, string, T> convertFrom)
        {
            _ConvertFrom = convertFrom;
        }
        public override bool CanConvertFrom(ITypeDescriptorContext context, Type sourceType)
        {
            return true;
        }
        public override object ConvertFrom(ITypeDescriptorContext context, CultureInfo culture, object value)
        {
            return _ConvertFrom(context, culture, (string)value);
        }
    }
}
