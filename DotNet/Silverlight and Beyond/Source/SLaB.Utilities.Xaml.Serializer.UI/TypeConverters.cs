#region Using Directives

using System;
using System.ComponentModel;
using System.Globalization;
using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Imaging;


#endregion

namespace SLaB.Utilities.Xaml.Serializer.UI
{
    internal class ToStringTypeConverter : TypeConverter
    {

        static ToStringTypeConverter()
        {
            Instance = new ToStringTypeConverter();
        }



        public static TypeConverter Instance { get; private set; }




        public override bool CanConvertTo(ITypeDescriptorContext context, Type destinationType)
        {
            return destinationType.Equals(typeof(string));
        }

        public override object ConvertTo(ITypeDescriptorContext context,
                                         CultureInfo culture,
                                         object value,
                                         Type destinationType)
        {
            return string.Format(culture, "{0}", value);
        }
    }

    internal class GeometryTypeConverter : TypeConverter
    {
        static GeometryTypeConverter()
        {
            Instance = new GeometryTypeConverter();
        }

        public static TypeConverter Instance { get; private set; }

        public override bool CanConvertTo(ITypeDescriptorContext context, Type destinationType)
        {
            return destinationType.Equals(typeof(string));
        }

        public override object ConvertTo(ITypeDescriptorContext context,
                                         CultureInfo culture,
                                         object value,
                                         Type destinationType)
        {
            if (value.GetType().Equals(typeof(PathGeometry)))
                return string.Format(culture, "{0}", value);
            throw new ArgumentException();
        }
    }

    internal class TextDecorationsTypeConverter : TypeConverter
    {

        static TextDecorationsTypeConverter()
        {
            Instance = new TextDecorationsTypeConverter();
        }



        public static TypeConverter Instance { get; private set; }




        public override bool CanConvertTo(ITypeDescriptorContext context, Type destinationType)
        {
            return destinationType.Equals(typeof(string));
        }

        public override object ConvertTo(ITypeDescriptorContext context,
                                         CultureInfo culture,
                                         object value,
                                         Type destinationType)
        {
            if (value.Equals(TextDecorations.Underline))
                return "Underline";
            throw new ArgumentException();
        }
    }

    internal class ImageSourceTypeConverter : TypeConverter
    {
        static ImageSourceTypeConverter()
        {
            Instance = new ImageSourceTypeConverter();
        }

        public static TypeConverter Instance { get; private set; }

        public override bool CanConvertTo(ITypeDescriptorContext context, Type destinationType)
        {
            return destinationType.Equals(typeof(string));
        }

        public override object ConvertTo(ITypeDescriptorContext context,
                                         CultureInfo culture,
                                         object value,
                                         Type destinationType)
        {
            BitmapImage iSrc = value as BitmapImage;
            if (iSrc == null)
                throw new ArgumentException();
            if (iSrc.CreateOptions != BitmapCreateOptions.DelayCreation)
                throw new ArgumentException();
            return string.Format(culture, "{0}", iSrc.UriSource);
        }
    }
}