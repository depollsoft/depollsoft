using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Windows;
using System.Windows.Input;
using BarbershopTags.Barbershop;
using SLaB.Utilities;
using System.Threading;

namespace BarbershopTags
{
    public class QueryModel : INotifyPropertyChanged
    {
        private TagQueryResult _MostRecentResult;
        private ObservableCollection<Tag> _TagsLoaded;
        public QueryModel()
        {
            _MostRecentResult = new TagQueryResult { Start = 0, Count = 0 };
            _HasMoreResults = true;
            _TagsLoaded = new ObservableCollection<Tag>();
            ResultSetSize = 10;
            MaxResults = 50;
            Tags = _TagsLoaded;
            FetchResultsCommand = new LambdaCommand<object>(obj =>
                {
                    FetchResults();
                });
        }


        private int _MaxResults;
        public int MaxResults
        {
            get
            {
                return _MaxResults;
            }
            set
            {
                if (!EqualityComparer<int>.Default.Equals(_MaxResults, value))
                {
                    _MaxResults = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("MaxResults"));
                }
            }
        }

        public virtual void FetchResults()
        {
            if (IsLoading)
                return;
            IsLoading = true;
            UiUtilities.ExecuteOnUiThread(() =>
            {
                Tag.Query(Query, ResultSetSize, _MostRecentResult.Start + _MostRecentResult.Count, Parts, HasLearningTracks, HasSheetMusic, Collection, SortBy).Continue(result =>
                {
                    Thread.Sleep(100);
                    UiUtilities.ExecuteOnUiThread(() =>
                    {
                        try
                        {
                            StatusText = null;
                            _MostRecentResult = result;
                            foreach (var tag in result.Tags)
                                _TagsLoaded.Add(tag);
                            if (_MostRecentResult.Start + _MostRecentResult.Count >= Math.Min(_MostRecentResult.Available, MaxResults))
                                HasMoreResults = false;
                            else
                                HasMoreResults = true;
                            if (result.Available == 0)
                                StatusText = "No tags could be found that matched your query.";
                        }
                        finally
                        {
                            IsLoading = false;
                        }
                    });
                },
                    err =>
                    {
                        UiUtilities.ExecuteOnUiThread(() =>
                        {
                            StatusText = "An error has occurred: " + err.Message;
                            IsLoading = false;
                        });
                    });
            });
        }

        public ICommand FetchResultsCommand
        {
            get;
            private set;
        }


        private string _StatusText;
        public string StatusText
        {
            get
            {
                return _StatusText;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_StatusText, value))
                {
                    _StatusText = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("StatusText"));
                }
            }
        }

        private TagCollection? _Collection;
        [TypeConverter(typeof(TagCollectionTypeConverter))]
        public TagCollection? Collection
        {
            get
            {
                return _Collection;
            }
            set
            {
                if (!EqualityComparer<TagCollection?>.Default.Equals(_Collection, value))
                {
                    _Collection = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Collection"));
                }
            }
        }

        private bool _HasMoreResults;
        public bool HasMoreResults
        {
            get
            {
                return _HasMoreResults;
            }
            private set
            {
                if (!EqualityComparer<bool>.Default.Equals(_HasMoreResults, value))
                {
                    _HasMoreResults = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("HasMoreResults"));
                }
            }
        }

        private bool _IsLoading;
        public bool IsLoading
        {
            get
            {
                return _IsLoading;
            }
            set
            {
                if (!EqualityComparer<bool>.Default.Equals(_IsLoading, value))
                {
                    _IsLoading = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("IsLoading"));
                }
            }
        }

        private string _Query;
        public string Query
        {
            get
            {
                return _Query;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Query, value))
                {
                    _Query = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Query"));
                }
            }
        }


        private int _ResultSetSize;
        public int ResultSetSize
        {
            get
            {
                return _ResultSetSize;
            }
            set
            {
                if (!EqualityComparer<int>.Default.Equals(_ResultSetSize, value))
                {
                    _ResultSetSize = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("ResultSetSize"));
                }
            }
        }


        private int? _Parts;
        [TypeConverter(typeof(NullableIntTypeConverter))]
        public int? Parts
        {
            get
            {
                return _Parts;
            }
            set
            {
                if (!EqualityComparer<int?>.Default.Equals(_Parts, value))
                {
                    _Parts = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Parts"));
                }
            }
        }


        private bool? _HasLearningTracks;
        [TypeConverter(typeof(NullableBoolConverter))]
        public bool? HasLearningTracks
        {
            get
            {
                return _HasLearningTracks;
            }
            set
            {
                if (!EqualityComparer<bool?>.Default.Equals(_HasLearningTracks, value))
                {
                    _HasLearningTracks = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("HasLearningTracks"));
                }
            }
        }


        private bool? _HasSheetMusic;
        [TypeConverter(typeof(NullableBoolConverter))]
        public bool? HasSheetMusic
        {
            get
            {
                return _HasSheetMusic;
            }
            set
            {
                if (!EqualityComparer<bool?>.Default.Equals(_HasSheetMusic, value))
                {
                    _HasSheetMusic = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("HasSheetMusic"));
                }
            }
        }


        private TagSortOptions _SortBy;
        public TagSortOptions SortBy
        {
            get
            {
                return _SortBy;
            }
            set
            {
                if (!EqualityComparer<TagSortOptions>.Default.Equals(_SortBy, value))
                {
                    _SortBy = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("SortBy"));
                }
            }
        }

        private ObservableCollection<Tag> _Tags;
        public ObservableCollection<Tag> Tags
        {
            get
            {
                return _Tags;
            }
            private set
            {
                if (!EqualityComparer<ObservableCollection<Tag>>.Default.Equals(_Tags, value))
                {
                    _Tags = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Tags"));
                }
            }
        }
        public event PropertyChangedEventHandler PropertyChanged;
    }
}
