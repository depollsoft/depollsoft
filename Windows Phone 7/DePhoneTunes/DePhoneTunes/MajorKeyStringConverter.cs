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

namespace DePhoneTunes
{
    public class MajorKeyStringConverter : IValueConverter
    {

        public object Convert(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            Note note = (Note)value;
            string result = "&";
            switch (note.ToString())
            {
                case "Gb":
                    result += "­";
                    break;
                case "Db":
                    result += "¬";
                    break;
                case "Ab":
                    result += "«";
                    break;
                case "Eb":
                    result += "ª";
                    break;
                case "Bb":
                    result += "©";
                    break;
                case "F":
                    result += "¨";
                    break;
                case "C":
                    result += " ";
                    break;
                case "G":
                    result += "¡";
                    break;
                case "D":
                    result += "¢";
                    break;
                case "A":
                    result += "£";
                    break;
                case "E":
                    result += "¤";
                    break;
                case "B":
                    result += "¥";
                    break;
                case "Fs":
                    result += "¦";
                    break;
            }
            return result;
        }

        public object ConvertBack(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
