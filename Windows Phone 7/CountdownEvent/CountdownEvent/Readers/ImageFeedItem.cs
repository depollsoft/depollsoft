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

namespace CountdownEvent.Readers
{
    public class ImageFeedItem : FeedItem
    {
        public Uri Thumbnail { get; set; }
        public Uri Content { get; set; }
    }
}
