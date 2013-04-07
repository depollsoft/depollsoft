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
using CountdownEvent.Readers;

namespace CountdownEvent.Internal
{
    public class FeedConverter : IValueConverter
    {

        public object Convert(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            string param = parameter as string;
            switch (param)
            {
                case "Image":
                    return new ImageFeed { FeedUri = ((Uri)value) };
                case "Video":
                    return new VideoFeed { FeedUri = ((Uri)value) };
            }
            return new Feed { FeedUri = ((Uri)value) };
        }

        public object ConvertBack(object value, Type targetType, object parameter, System.Globalization.CultureInfo culture)
        {
            throw new NotImplementedException();
        }
    }
}
