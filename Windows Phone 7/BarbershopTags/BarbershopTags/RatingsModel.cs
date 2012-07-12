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
using System.Collections.ObjectModel;
using System.IO.IsolatedStorage;
using System.Collections.Generic;

namespace BarbershopTags
{
    public class RatingsModel: INotifyPropertyChanged
    {
        private static ObservableCollection<int> _RatedIds;
        
        static RatingsModel()
        {
            if (!DesignerProperties.IsInDesignTool)
            {
                IsolatedStorageSettings.ApplicationSettings.SetIfContainsKey<ObservableCollection<int>>("RatedIds", val => _RatedIds = val);
                if (_RatedIds == null)
                {
                    _RatedIds = new ObservableCollection<int>();
                    IsolatedStorageSettings.ApplicationSettings.SetIfNotInDesignMode("RatedIds", _RatedIds);
                }
            }
            else
            {
                _RatedIds = new ObservableCollection<int>();
            }
        }
        public RatingsModel()
        {
            RatedTagIds = new ReadOnlyObservableCollection<int>(_RatedIds);
        }

        public static void AddRating(int id)
        {
            if (!_RatedIds.Contains(id))
                _RatedIds.Add(id);
        }

        public static bool IsRated(int id)
        {
            return _RatedIds.Contains(id);
        }

        public static void RemoveRating(int id)
        {
            _RatedIds.Remove(id);
        }

        public static void ResetRatings()
        {
            _RatedIds.Clear();
        }

        private IEnumerable<int> _RatedTagIds;
        public IEnumerable<int> RatedTagIds
        {
            get
            {
                return _RatedTagIds;
            }
            private set
            {
                if (!EqualityComparer<IEnumerable<int>>.Default.Equals(_RatedTagIds, value))
                {
                    _RatedTagIds = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("RatedTagIds"));
                }
            }
        }
        public event PropertyChangedEventHandler PropertyChanged;
    }
}
