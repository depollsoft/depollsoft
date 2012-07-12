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
    [KnownType(typeof(VideoFeedItem))]
    [KnownType(typeof(Feed))]
    public class VideoFeed : Feed
    {
        private const string MediaNamespace = "http://search.yahoo.com/mrss/";
        protected override FeedItem GetNewFeedItem()
        {
            return new VideoFeedItem();
        }
        [DataMember]
        [EditorBrowsable(EditorBrowsableState.Never)]
        public int Test { get; set; }
        protected override FeedItem Digest(XElement element, FeedItem fi)
        {
            VideoFeedItem vfi = base.Digest(element, fi) as VideoFeedItem;
            vfi.Thumbnail = new Uri((string)element.Descendants(XName.Get("thumbnail", MediaNamespace)).Last().Attribute("url"));
            return fi;
        }
    }
}
