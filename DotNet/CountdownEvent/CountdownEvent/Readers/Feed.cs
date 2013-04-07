using System;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO;
using System.Linq;
using System.Net;
using System.Xml.Linq;
using SLaB.Utilities;
using System.Runtime.Serialization;

namespace CountdownEvent.Readers
{
    [DataContract]
    [KnownType(typeof(FeedItem))]
    [KnownType(typeof(VideoFeedItem))]
    [KnownType(typeof(ImageFeedItem))]
    [KnownType(typeof(VideoFeed))]
    [KnownType(typeof(ImageFeed))]
    public class Feed : INotifyPropertyChanged
    {
        public const string StartIndexReplacementString = "{StartIndex}";
        public const string StartIndex1ReplacementString = "{StartIndex1}";
        public const string CountReplacementString = "{Count}";
        public Feed()
        {
            Initialize();
        }

        private void Initialize()
        {
            _StartIndex = 0;
            FetchSize = 10;
            HasMoreItems = true;
            Items = new ObservableCollection<FeedItem>();
        }

        [OnDeserializing]
        [EditorBrowsable(EditorBrowsableState.Never)]
        public void Deserializing(StreamingContext context)
        {
            Initialize();
        }

        [DataMember]
        public ObservableCollection<FeedItem> Items { get; set; }

        public Uri FeedUri { get; set; }
        [DataMember]
        [EditorBrowsable(EditorBrowsableState.Never)]
        public string FeedUriString
        {
            get
            {
                return FeedUri.OriginalString;
            }
            set
            {
                FeedUri = new Uri(value);
            }
        }
        [DataMember]
        public int FetchSize { get; set; }
        private string _Status;
        [DataMember]
        public string Status
        {
            get
            {
                return _Status;
            }
            set
            {
                _Status = value;
                OnPropertyChanged(new PropertyChangedEventArgs("Status"));
            }
        }
        private bool _HasMoreItems;
        [DataMember]
        public bool HasMoreItems
        {
            get
            {
                return _HasMoreItems;
            }
            set
            {
                _HasMoreItems = value;
                OnPropertyChanged(new PropertyChangedEventArgs("HasMoreItems"));
            }
        }
        private bool _IsLoading;
        public bool IsLoading
        {
            get
            {
                return _IsLoading;
            }
            private set
            {
                _IsLoading = value;
                OnPropertyChanged(new PropertyChangedEventArgs("IsLoading"));
            }
        }
        private int _StartIndex;
        [DataMember]
        public int StartIndex { get { return _StartIndex; } set { _StartIndex = value; } }

        public void Fetch()
        {
            if (IsLoading)
                return;
            IsLoading = true;
            string realUri = FeedUri.OriginalString.Replace(StartIndexReplacementString, "" + _StartIndex).Replace(StartIndex1ReplacementString, "" + (_StartIndex + 1)).Replace(CountReplacementString, "" + FetchSize);
            WebRequest wr = WebRequest.Create(realUri);
            wr.BeginGetResponse(result =>
                {
                    try
                    {
                        StreamReader sr = new StreamReader(wr.EndGetResponse(result).GetResponseStream());
                        string resultString = sr.ReadToEnd();
                        var doc = XDocument.Parse(resultString);
                        var items = (from item in doc.Descendants("item")
                                     select Digest(item, GetNewFeedItem())).ToArray();
                        UiUtilities.ExecuteOnUiThread(() =>
                            {
                                foreach (var item in items)
                                    Items.Add(item);
                                _StartIndex += items.Length;
                                if (items.Length == 0)
                                    HasMoreItems = false;
                            });
                    }
                    catch (Exception e)
                    {
                        Status = e.Message;
                    }
                    finally
                    {
                        UiUtilities.ExecuteOnUiThread(() => IsLoading = false);
                    }
                }, null);
        }

        protected virtual FeedItem GetNewFeedItem()
        {
            return new FeedItem();
        }

        protected virtual FeedItem Digest(XElement element, FeedItem fi)
        {
            fi.Title = (string)element.Element("title");
            var descElem = element.Element("description");
            if (descElem != null)
                fi.Description = descElem.Value;
            var linkElem = element.Element("link");
            if (linkElem != null)
                fi.Link = new Uri(linkElem.Value);
            var pubElem = element.Element("pubDate");
            if (pubElem != null)
                fi.Published = DateTime.Parse(pubElem.Value);
            var sourceElement = element.Element("source");
            var urlAtt = element.Attribute("url");
            if (sourceElement != null)
                if (urlAtt != null)
                    fi.Source = new Source { Name = sourceElement.Value, Url = new Uri(sourceElement.Attribute("url").Value) };
                else
                    fi.Source = new Source { Name = sourceElement.Value };
            return fi;
        }

        protected virtual void OnPropertyChanged(PropertyChangedEventArgs args)
        {
            var pc = PropertyChanged;
            if (pc != null)
                pc(this, args);
        }

        public event PropertyChangedEventHandler PropertyChanged;
    }
}
