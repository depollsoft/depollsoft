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
    public class FeedItem
    {
        public string Title { get; set; }
        public string Description { get; set; }
        public Uri Link { get; set; }
        public Source Source { get; set; }
        public DateTime Published { get; set; }
    }
    public class Source
    {
        public string Name { get; set; }
        public Uri Url { get; set; }
    }
}
