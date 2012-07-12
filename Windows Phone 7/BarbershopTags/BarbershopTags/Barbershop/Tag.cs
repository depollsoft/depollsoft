using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.IO;
using System.IO.IsolatedStorage;
using System.Linq;
using System.Net;
using System.Text;
using System.Windows.Markup;
using System.Xml.Linq;
using SLaB.Utilities.Xaml.Serializer;
using System.Threading;
using DePhoneTunes;

namespace BarbershopTags.Barbershop
{
    public class Tag
    {
        public const int CurrentAppVersion = 1;
        static Tag()
        {
            if (!DesignerProperties.IsInDesignTool)
            {
                var iso = IsolatedStorageFile.GetUserStoreForApplication();
                if (!iso.DirectoryExists("TagCache"))
                    iso.CreateDirectory("TagCache");
            }
        }
        private static readonly XamlSerializer Serializer = new XamlSerializer();
        private static readonly IDictionary<int, Tag> TagCache = new Dictionary<int, Tag>();
        private const string ApiUriString = "http://www.barbershoptags.com/api.php?client=TagMaster&";
        private const string RatingUriString = "http://www.barbershoptags.com/api.php?client=TagMaster&action=rate&id={0}&rating={1}";
        public Tag()
        {
            this.AppVersion = Tag.CurrentAppVersion;
            this.Videos = new List<Video>();
        }
        protected void ParseFromXml(XElement element)
        {
            LastRefreshed = DateTime.Now;
            foreach (var property in element.Elements())
            {
                string propValue = property.Value;
                if (string.IsNullOrEmpty(propValue))
                    propValue = null;
                if (propValue != null)
                    propValue = propValue.Trim();
                switch (property.Name.LocalName)
                {
                    case "id":
                        this.Id = int.Parse(propValue);
                        break;
                    case "Title":
                        this.Title = propValue;
                        break;
                    case "AltTitle":
                        this.AlternativeTitle = propValue;
                        break;
                    case "Version":
                        this.Version = propValue;
                        break;
                    case "WritKey":
                        this.WrittenKey = propValue;
                        break;
                    case "Parts":
                        int parts;
                        if (int.TryParse(propValue, out parts))
                            this.Parts = parts;
                        break;
                    case "Type":
                        this.TagType = propValue;
                        break;
                    case "Recording":
                        this.RecordingMethod = propValue;
                        break;
                    case "TeachVid":
                        this.TeachingVideo = propValue;
                        break;
                    case "Lyrics":
                        this.Lyrics = propValue;
                        break;
                    case "Notes":
                        this.Notes = propValue;
                        break;
                    case "Arranger":
                        this.Arranger = propValue;
                        break;
                    case "ArrWebsite":
                        if (!string.IsNullOrEmpty(propValue))
                            this.ArrangerWebsite = new Uri(propValue);
                        break;
                    case "Arranged":
                        int arrYear;
                        if (int.TryParse(propValue, out arrYear))
                            this.YearArranged = arrYear;
                        break;
                    case "SungBy":
                        this.SungBy = propValue;
                        break;
                    case "SungWebsite":
                        if (!string.IsNullOrEmpty(propValue))
                            this.SungByWebsite = new Uri(propValue);
                        break;
                    case "SungYear":
                        int sungYear;
                        if (int.TryParse(propValue, out sungYear))
                            this.SungYear = sungYear;
                        break;
                    case "Quartet":
                        this.LearningTrackQuartet = propValue;
                        break;
                    case "QWebsite":
                        if (!string.IsNullOrEmpty(propValue))
                            this.LearningTrackQuartetWebsite = new Uri(propValue);
                        break;
                    case "Teacher":
                        this.Teacher = propValue;
                        break;
                    case "TWebsite":
                        if (!string.IsNullOrEmpty(propValue))
                            this.TeacherWebsite = new Uri(propValue);
                        break;
                    case "Provider":
                        this.Provider = propValue;
                        break;
                    case "ProvWebsite":
                        if (!string.IsNullOrEmpty(propValue))
                            this.ProviderWebsite = new Uri(propValue);
                        break;
                    case "Posted":
                        DateTime postedDate;
                        if (DateTime.TryParse(propValue, out postedDate))
                            this.Posted = postedDate;
                        break;
                    case "Classic":
                        int classicNumber;
                        if (int.TryParse(propValue, out classicNumber))
                            this.ClassicTagNumber = classicNumber;
                        break;
                    case "Rating":
                        double rating;
                        if (double.TryParse(propValue, out rating))
                            this.Rating = rating;
                        break;
                    case "Downloaded":
                        int downloadCount;
                        if (propValue != null && int.TryParse(propValue.Replace(",", ""), out downloadCount))
                            this.DownloadCount = downloadCount;
                        break;
                    case "SheetMusic":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.SheetMusicUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "Notation":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.NotationUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "AllParts":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.AllPartsTrackUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "Bass":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.BassTrackUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "Bari":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.BaritoneTrackUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "Lead":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.LeadTrackUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "Tenor":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.TenorTrackUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "Other1":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.Other1TrackUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "Other2":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.Other2TrackUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "Other3":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.Other3TrackUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "Other4":
                        if (!string.IsNullOrEmpty(propValue))
                        {
                            this.Other4TrackUri = new RemoteLocation { Uri = new Uri(propValue), Type = property.Attribute("type").Value };
                        }
                        break;
                    case "videos":
                        this.Videos.AddRange(from vid in property.Elements("video")
                                             select new Video(vid));
                        break;
                }
            }
        }
        private Tag(XElement element)
            : this()
        {
            ParseFromXml(element);
        }
        public static Task<TagQueryResult> Query(string query, int numberOfResults = 10, int start = 0, int? parts = null, bool? learning = null, bool? sheetMusic = null, TagCollection? collection = null, TagSortOptions? sortBy = null, bool cache = false, string fieldList = "id,Title,AltTitle,Rating,Posted,Downloaded,SheetMusic,Bass,Bari,Lead,Tenor,Other1,Other2,Other3,Other4")
        {
            StringBuilder sb = new StringBuilder();
            sb.Append("n=" + numberOfResults);
            if (fieldList != null)
                sb.Append("&fldlist=" + fieldList);
            sb.Append("&start=" + (start + 1));
            if (!string.IsNullOrEmpty(query))
                sb.Append("&q=" + Uri.EscapeDataString(query));
            if (parts.HasValue)
                sb.Append("&Parts=" + parts.Value);
            if (learning.HasValue)
                sb.Append("&Learning=" + (learning.Value ? "Yes" : "No"));
            if (sheetMusic.HasValue)
                sb.Append("&SheetMusic=" + (sheetMusic.Value ? "Yes" : "No"));
            if (collection.HasValue)
            {
                sb.Append("&Collection=");
                switch (collection.Value)
                {
                    case TagCollection.ClassicTags:
                        sb.Append("classic");
                        break;
                    case TagCollection.EasyTags:
                        sb.Append("easy");
                        break;
                }
            }
            if (sortBy.HasValue)
            {
                sb.Append("&Sortby=");
                switch (sortBy.Value)
                {
                    case TagSortOptions.Classic:
                        sb.Append("Classic");
                        if (!(collection.HasValue && collection.Value == TagCollection.ClassicTags))
                            throw new ArgumentException("Sortby=Classic must also have Collection=classic", "sortBy");
                        break;
                    case TagSortOptions.Downloaded:
                        sb.Append("Downloaded");
                        break;
                    case TagSortOptions.Posted:
                        sb.Append("Posted");
                        break;
                    case TagSortOptions.Rating:
                        sb.Append("Rating");
                        break;
                    case TagSortOptions.Title:
                        sb.Append("Title");
                        break;
                }
            }
            var source = new Task<TagQueryResult>.TaskSource();
            var wr = WebRequest.Create(ApiUriString + sb.ToString());
            wr.BeginGetResponse((res) =>
            {
                WebResponse response = null;
                string responseString = null;
                try
                {
                    TagQueryResult result = new TagQueryResult();
                    response = wr.EndGetResponse(res);
                    responseString = new StreamReader(response.GetResponseStream()).ReadToEnd();
                    var doc = XDocument.Load(new StringReader(responseString));
                    var tags = doc.Elements("tags").First();
                    result.Available = int.Parse(tags.Attribute("available").Value);
                    result.Count = int.Parse(tags.Attribute("count").Value);
                    result.Start = start;
                    result.Tags = (from tag in tags.Elements("tag")
                                   select new Tag(tag)).ToArray();
                    if (cache)
                        foreach (var tag in result.Tags)
                            tag.Cache();
                    source.Result = result;
                }
                catch (Exception e)
                {
                    source.Error = e;
                }
            }, null);
            return new Task<TagQueryResult>(source);
        }
        public static Task<Tag> QueryById(int id)
        {
            var source = new Task<Tag>.TaskSource();
            var wr = WebRequest.Create(ApiUriString + "id=" + id);
            wr.BeginGetResponse(res =>
                {
                    try
                    {
                        var response = wr.EndGetResponse(res);
                        var responseString = new StreamReader(response.GetResponseStream()).ReadToEnd();
                        var doc = XDocument.Load(new StringReader(responseString));
                        source.Result = new Tag(doc.Descendants("tag").First());
                    }
                    catch (Exception e)
                    {
                        source.Error = e;
                    }
                }, null);
            return new Task<Tag>(source);
        }
        public static void ClearCache()
        {
            var iso = IsolatedStorageFile.GetUserStoreForApplication();
            foreach (var filename in iso.GetFileNames("TagCache" + Path.DirectorySeparatorChar + "*"))
                iso.DeleteFile("TagCache" + Path.DirectorySeparatorChar + filename);
        }
        public static long CurrentCacheSize
        {
            get
            {
                var iso = IsolatedStorageFile.GetUserStoreForApplication();
                long totalSize = 0;
                foreach (var filename in iso.GetFileNames("TagCache" + Path.DirectorySeparatorChar + "*"))
                {
                    var file = iso.OpenFile("TagCache" + Path.DirectorySeparatorChar + filename, FileMode.Open);
                    totalSize += file.Length;
                    file.Close();
                }
                return totalSize;
            }
        }
        public static Task<Tag> LoadTagById(int id, bool refresh = false)
        {
            var taskSource = new Task<Tag>.TaskSource();

            var iso = IsolatedStorageFile.GetUserStoreForApplication();
            string filePath = "TagCache" + Path.DirectorySeparatorChar + id;
            if (!refresh && TagCache.ContainsKey(id))
            {
                taskSource.Result = TagCache[id];
            }
            else if (!refresh && iso.FileExists(filePath))
            {
                try
                {
                    StreamReader sr = new StreamReader(iso.OpenFile(filePath, FileMode.Open));
                    var tag = (Tag)XamlReader.Load(sr.ReadToEnd());
                    if (tag == null || tag.AppVersion != Tag.CurrentAppVersion)
                        throw new Exception();
                    taskSource.Result = TagCache[id] = tag;
                    sr.Close();
                }
                catch (Exception e)
                {
                    LoadTagById(id, true).Continue(tag => taskSource.Result = tag, err => taskSource.Error = err);
                }
            }
            else
            {
                QueryById(id).Continue(tag =>
                {
                    tag.Cache();
                    TagCache[id] = tag;
                    taskSource.Result = tag;
                }, err => taskSource.Error = err);
            }
            return new Task<Tag>(taskSource);
        }
        public Task<bool> Rate(int rating)
        {
            var source = new Task<bool>.TaskSource();
            var wr = WebRequest.Create(string.Format(RatingUriString, Id, rating));
            wr.BeginGetResponse(res =>
            {
                try
                {
                    var response = wr.EndGetResponse(res);
                    var responseString = new StreamReader(response.GetResponseStream()).ReadToEnd();
                    bool result = responseString.Trim().ToLowerInvariant().Equals("ok");
                    source.Result = result;
                }
                catch (Exception e)
                {
                    source.Error = e;
                }
            }, null);
            return new Task<bool>(source);
        }
        private static object _CacheWriteLock = new object();
        public void Cache(bool overwrite = true)
        {
            if (overwrite)
                TagCache[Id] = this;
            ThreadPool.QueueUserWorkItem(val =>
                {
                    lock (_CacheWriteLock)
                    {
                        string filePath = "TagCache" + Path.DirectorySeparatorChar + Id;
                        var iso = IsolatedStorageFile.GetUserStoreForApplication();
                        if (!iso.DirectoryExists("TagCache"))
                            iso.CreateDirectory("TagCache");
                        if (iso.FileExists(filePath))
                        {
                            if (!overwrite)
                                return;
                            iso.DeleteFile(filePath);
                        }
                        StreamWriter sw = new StreamWriter(iso.CreateFile(filePath));
                        var serialized = Serializer.Serialize(this);
                        sw.Write(serialized);
                        sw.Close();
                    }
                });
        }
        public int AppVersion { get; set; }
        public int Id { get; set; }
        public string Title { get; set; }
        [TypeConverter(typeof(DateTimeTypeConverter))]
        public DateTime LastRefreshed { get; set; }
        [DefaultValue(null)]
        public string AlternativeTitle { get; set; }
        [DefaultValue(null)]
        public string Lyrics { get; set; }
        [DefaultValue(null)]
        public string Version { get; set; }
        [DefaultValue(null)]
        public string WrittenKey { get; set; }
        [DefaultValue(null)]
        [TypeConverter(typeof(NullableIntTypeConverter))]
        public int? Parts { get; set; }
        [DefaultValue(null)]
        public string TagType { get; set; }
        [DefaultValue(null)]
        public string RecordingMethod { get; set; }
        [DefaultValue(null)]
        public string TeachingVideo { get; set; }
        [DefaultValue(null)]
        public string Notes { get; set; }
        [DefaultValue(null)]
        public string Arranger { get; set; }
        [DefaultValue(null)]
        public Uri ArrangerWebsite { get; set; }
        [DefaultValue(null)]
        [TypeConverter(typeof(NullableIntTypeConverter))]
        public int? YearArranged { get; set; }
        [DefaultValue(null)]
        public string SungBy { get; set; }
        [DefaultValue(null)]
        public Uri SungByWebsite { get; set; }
        [DefaultValue(null)]
        [TypeConverter(typeof(NullableIntTypeConverter))]
        public int? SungYear { get; set; }
        [DefaultValue(null)]
        public string LearningTrackQuartet { get; set; }
        [DefaultValue(null)]
        public Uri LearningTrackQuartetWebsite { get; set; }
        [DefaultValue(null)]
        public string Teacher { get; set; }
        [DefaultValue(null)]
        public Uri TeacherWebsite { get; set; }
        [DefaultValue(null)]
        public string Provider { get; set; }
        [DefaultValue(null)]
        public Uri ProviderWebsite { get; set; }
        [TypeConverter(typeof(DateTimeTypeConverter))]
        public DateTime Posted { get; set; }
        [DefaultValue(null)]
        [TypeConverter(typeof(NullableIntTypeConverter))]
        public int? ClassicTagNumber { get; set; }
        public double Rating { get; set; }
        public int DownloadCount { get; set; }
        [DefaultValue(null)]
        public RemoteLocation SheetMusicUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation NotationUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation AllPartsTrackUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation BassTrackUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation BaritoneTrackUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation LeadTrackUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation TenorTrackUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation Other1TrackUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation Other2TrackUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation Other3TrackUri { get; set; }
        [DefaultValue(null)]
        public RemoteLocation Other4TrackUri { get; set; }

        public Note KeyNote
        {
            get
            {
                if (WrittenKey == null)
                    return null;
                string noteName = WrittenKey.ToUpperInvariant().Replace("MAJOR", "").Replace("MINOR", "").Replace(":", "").Trim();
                Accidental acc = Accidental.Natural;
                if (noteName.Length > 1)
                    acc = noteName[1] == '#' ? Accidental.Sharp : Accidental.Flat;
                return Note.FindNote("" + noteName[0], acc);
            }
        }
        public bool SheetMusicSupportedFormat
        {
            get
            {
                if (SheetMusicUri == null)
                    return false;
                return !SheetMusicUri.Type.ToLowerInvariant().Equals("pdf");
            }
        }
        public Uri TagUri
        {
            get
            {
                return new Uri(string.Format("http://www.barbershoptags.com/dbpage.php?pg=view&dbase=tags&id={0}", Id), UriKind.Absolute);
            }
        }

        public List<Video> Videos
        {
            get;
            private set;
        }

        private List<Track> _Tracks;
        private IEnumerable<Track> _FilteredTracks;

        public IEnumerable<Track> Tracks
        {
            get
            {
                if (_Tracks == null)
                {
                    _Tracks = new List<Track>
                    {
                        new Track{ Title="All Parts", Source=AllPartsTrackUri},
                        new Track{Title="Tenor", Source=TenorTrackUri},
                        new Track{Title="Lead", Source=LeadTrackUri},
                        new Track{Title="Baritone", Source=BaritoneTrackUri},
                        new Track{Title="Bass", Source=BassTrackUri},
                        new Track{Title="Other1", Source=Other1TrackUri},
                        new Track{Title="Other2", Source=Other2TrackUri},
                        new Track{Title="Other3", Source=Other3TrackUri},
                        new Track{Title="Other4", Source=Other4TrackUri},
                    };
                }
                if (_FilteredTracks != null)
                    return _FilteredTracks;
                return _FilteredTracks = _Tracks.Where(track => track.Source != null && !track.Source.Type.ToLowerInvariant().Equals("mid")).ToArray();
            }
        }

        public override string ToString()
        {
            return string.Format("id={0}, title={1}", Id, Title);
        }

        public override bool Equals(object obj)
        {
            if (obj == null)
                return false;
            return ((Tag)obj).Id == this.Id;
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }
    }

    public class RemoteLocation
    {
        public Uri Uri { get; set; }
        public string Type { get; set; }
    }

    public class Track
    {
        public string Title { get; set; }
        public RemoteLocation Source { get; set; }
        internal int Order { get; set; }
        public override string ToString()
        {
            return Title;
        }
        public override bool Equals(object obj)
        {
            if (obj == null)
                return false;
            Track track = (Track)obj;
            return object.Equals(Title, track.Title) && object.Equals(Source, track.Source);
        }
    }

    public class Video
    {
        public Video() { }
        public Video(XElement element)
            : this()
        {
            ParseFromXml(element);
        }
        internal void ParseFromXml(XElement element)
        {
            foreach (var property in element.Elements())
            {
                string propValue = property.Value;
                if (string.IsNullOrEmpty(propValue))
                    propValue = null;
                if (propValue != null)
                    propValue = propValue.Trim();
                switch (property.Name.LocalName)
                {
                    case "id":
                        int idVal;
                        if (int.TryParse(propValue, out idVal))
                            Id = idVal;
                        break;
                    case "Desc":
                        Description = propValue;
                        break;
                    case "SungKey":
                        SungKey = propValue;
                        break;
                    case "Multitrack":
                        if ("Yes".Equals(propValue))
                            IsMultitrack = true;
                        break;
                    case "Code":
                        YouTubeCode = propValue;
                        break;
                    case "SungBy":
                        SungBy = propValue;
                        break;
                    case "SungWebsite":
                        if (!string.IsNullOrEmpty(propValue))
                            SungWebsite = new Uri(propValue);
                        break;
                    case "Posted":
                        DateTime postedDate;
                        if (DateTime.TryParse(propValue, out postedDate))
                            this.Posted = postedDate;
                        break;
                }
            }
        }
        public int Id { get; set; }
        public string Description { get; set; }
        public string SungKey { get; set; }
        public bool IsMultitrack { get; set; }
        public string YouTubeCode { get; set; }
        public string SungBy { get; set; }
        public Uri SungWebsite { get; set; }
        [TypeConverter(typeof(DateTimeTypeConverter))]
        public DateTime Posted { get; set; }
    }

    public class TagQueryResult
    {
        public IEnumerable<Tag> Tags { get; internal set; }
        public int Start { get; internal set; }
        public int Count { get; internal set; }
        public int Available { get; internal set; }
    }
    public enum TagCollection { ClassicTags, EasyTags }
    public enum TagSortOptions { Title, Posted, Rating, Downloaded, Classic }
}
