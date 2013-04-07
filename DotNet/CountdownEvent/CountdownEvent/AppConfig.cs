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
using SLaB.Utilities;
using System.Collections.Generic;
using CountdownEvent.Internal;
using Microsoft.Phone.Shell;
using Microsoft.Phone.Info;

namespace CountdownEvent
{
    public class AppConfig : INotifyPropertyChanged
    {
        private const string TileUriPattern = @"http://countdown.davidpoll.com/fetchTile.php?ImageKey={0}&TargetDate={1:o}";
        public AppConfig()
        {
            AppVersion = "1.0";
            this.PropertyChanged += (obj, args) =>
                {
                    if (!DesignerProperties.IsInDesignTool)
                    {
                        if (!EventDate.Equals(DateTime.MinValue) && (args.PropertyName.Equals("TileImageKey") || args.PropertyName.Equals("EventDate")))
                        {
                            ShellTileSchedule oneTime = new ShellTileSchedule();
                            oneTime.StartTime = DateTime.Now;
                            oneTime.Recurrence = UpdateRecurrence.Onetime;
                            oneTime.RemoteImageUri = new Uri(string.Format(TileUriPattern, TileImageKey, new DateTimeOffset(EventDate)), UriKind.Absolute);
                            oneTime.Start();
                            ShellTileSchedule recurring = new ShellTileSchedule();
                            recurring.StartTime = DateTime.Now - TimeSpan.FromMinutes(DateTime.Now.Minute) - TimeSpan.FromSeconds(DateTime.Now.Second - 1) + TimeSpan.FromHours(1);
                            recurring.Recurrence = UpdateRecurrence.Interval;
                            recurring.Interval = UpdateInterval.EveryHour;
                            recurring.MaxUpdateCount = int.MaxValue;
                            recurring.RemoteImageUri = new Uri(string.Format(TileUriPattern, TileImageKey, new DateTimeOffset(EventDate)), UriKind.Absolute);
                            recurring.Start();
                        }
                    }
                };
        }

        private string _ApplicationTitle;
        public string ApplicationTitle
        {
            get
            {
                return _ApplicationTitle;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_ApplicationTitle, value))
                {
                    _ApplicationTitle = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("ApplicationTitle"));
                }
            }
        }


        private DateTime _EventDate;
        [TypeConverter(typeof(DateTimeConverter))]
        public DateTime EventDate
        {
            get
            {
                return _EventDate;
            }
            set
            {
                if (!EqualityComparer<DateTime>.Default.Equals(_EventDate, value))
                {
                    _EventDate = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("EventDate"));
                }
            }
        }


        private Brush _ApplicationBackgroundImage;
        public Brush ApplicationBackgroundImage
        {
            get
            {
                return _ApplicationBackgroundImage;
            }
            set
            {
                if (!EqualityComparer<Brush>.Default.Equals(_ApplicationBackgroundImage, value))
                {
                    _ApplicationBackgroundImage = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("ApplicationBackgroundImage"));
                }
            }
        }


        private string _AppVersion;
        public string AppVersion
        {
            get
            {
                return _AppVersion;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_AppVersion, value))
                {
                    _AppVersion = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("AppVersion"));
                }
            }
        }

        private string _TileImageKey;
        public string TileImageKey
        {
            get
            {
                return _TileImageKey;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_TileImageKey, value))
                {
                    _TileImageKey = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("TileImageKey"));
                }
            }
        }


        private Uri _NewsFeedUri;
        public Uri NewsFeedUri
        {
            get
            {
                return _NewsFeedUri;
            }
            set
            {
                if (!EqualityComparer<Uri>.Default.Equals(_NewsFeedUri, value))
                {
                    _NewsFeedUri = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("NewsFeedUri"));
                }
            }
        }


        private Uri _WebSearchFeedUri;
        public Uri WebSearchFeedUri
        {
            get
            {
                return _WebSearchFeedUri;
            }
            set
            {
                if (!EqualityComparer<Uri>.Default.Equals(_WebSearchFeedUri, value))
                {
                    _WebSearchFeedUri = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("WebSearchFeedUri"));
                }
            }
        }

        private Uri _VideoFeedUri;
        public Uri VideoFeedUri
        {
            get
            {
                return _VideoFeedUri;
            }
            set
            {
                if (!EqualityComparer<Uri>.Default.Equals(_VideoFeedUri, value))
                {
                    _VideoFeedUri = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("VideoFeedUri"));
                }
            }
        }


        private Uri _ImageFeedUri;
        public Uri ImageFeedUri
        {
            get
            {
                return _ImageFeedUri;
            }
            set
            {
                if (!EqualityComparer<Uri>.Default.Equals(_ImageFeedUri, value))
                {
                    _ImageFeedUri = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("ImageFeedUri"));
                }
            }
        }

        private string _EventMessage;
        public string EventMessage
        {
            get
            {
                return _EventMessage;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_EventMessage, value))
                {
                    _EventMessage = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("EventMessage"));
                }
            }
        }


        private string _EventName;
        public string EventName
        {
            get
            {
                return _EventName;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_EventName, value))
                {
                    _EventName = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("EventName"));
                }
            }
        }


        private string _PreparationText;
        public string PreparationText
        {
            get
            {
                return _PreparationText;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_PreparationText, value))
                {
                    _PreparationText = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("PreparationText"));
                }
            }
        }


        private string _Description;
        public string Description
        {
            get
            {
                return _Description;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Description, value))
                {
                    _Description = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Description"));
                }
            }
        }


        private Uri _Wikipedia;
        public Uri Wikipedia
        {
            get
            {
                return _Wikipedia;
            }
            set
            {
                if (!EqualityComparer<Uri>.Default.Equals(_Wikipedia, value))
                {
                    _Wikipedia = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Wikipedia"));
                }
            }
        }


        private string _AboutHeader;
        public string AboutHeader
        {
            get
            {
                return _AboutHeader;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_AboutHeader, value))
                {
                    _AboutHeader = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("AboutHeader"));
                }
            }
        }

        public event PropertyChangedEventHandler PropertyChanged;
    }
}
