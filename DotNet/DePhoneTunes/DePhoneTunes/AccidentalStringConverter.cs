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
using System.Globalization;

namespace DePhoneTunes
{
    public class AccidentalStringConverter : IValueConverter
    {

        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            Accidental acc = (Accidental)value;
            switch (acc)
            {
                case Accidental.Natural:
                    return "";
                    //return "î";
                case Accidental.Flat:
                    return "í";
                case Accidental.Sharp:
                    return "ì";
            }
            return "";
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
