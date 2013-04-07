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
using System.Xml.Linq;
using System.Linq;
using System.Runtime.Serialization;
using System.ComponentModel;

namespace CountdownEvent.Readers
{
    [DataContract]
    [KnownType(typeof(Feed))]
    [KnownType(typeof(ImageFeedItem))]
    public class ImageFeed : Feed
    {
        public ImageFeed()
        {
            this.FetchSize = 20;
        }
        private const string MediaNamespace = "http://search.yahoo.com/mrss/";
        protected override FeedItem GetNewFeedItem()
        {
            return new ImageFeedItem();
        }

        [DataMember]
        [EditorBrowsable(EditorBrowsableState.Never)]
        public int Test { get; set; }

        protected override FeedItem Digest(XElement element, FeedItem fi)
        {
            fi = base.Digest(element, fi);
            ImageFeedItem ifi = fi as ImageFeedItem;
            ifi.Thumbnail = new Uri((string)element.Element(XName.Get("thumbnail", MediaNamespace)).Attribute("url"));
            ifi.Content = new Uri((string)element.Element(XName.Get("content", MediaNamespace)).Attribute("url"));
            return ifi;
        }
    }
}
