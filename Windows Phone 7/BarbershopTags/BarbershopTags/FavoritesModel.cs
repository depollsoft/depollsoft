using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO.IsolatedStorage;
using SLaB.Utilities;

namespace BarbershopTags
{
    public class FavoritesModel : INotifyPropertyChanged
    {
        private static ObservableCollection<int> _FavoriteIds;
        
        static FavoritesModel()
        {
            if (!DesignerProperties.IsInDesignTool)
            {
                IsolatedStorageSettings.ApplicationSettings.SetIfContainsKey<ObservableCollection<int>>("FavoriteIds", val => _FavoriteIds = val);
                if (_FavoriteIds == null)
                {
                    _FavoriteIds = new ObservableCollection<int>();
                    IsolatedStorageSettings.ApplicationSettings.SetIfNotInDesignMode("FavoriteIds", _FavoriteIds);
                }
            }
            else
            {
                _FavoriteIds = new ObservableCollection<int>();
            }
        }
        public FavoritesModel()
        {
            FavoriteTagIds = new ReadOnlyObservableCollection<int>(_FavoriteIds);
        }

        public static void AddFavorite(int id)
        {
            if (!_FavoriteIds.Contains(id))
                _FavoriteIds.Add(id);
        }

        public static bool IsFavorite(int id)
        {
            return _FavoriteIds.Contains(id);
        }

        public static void RemoveFavorite(int id)
        {
            _FavoriteIds.Remove(id);
        }

        public static bool CanMoveUp(int id)
        {
            var index = _FavoriteIds.IndexOf(id);
            return index > 0;
        }

        public static bool CanMoveDown(int id)
        {
            var index = _FavoriteIds.IndexOf(id);
            return index < _FavoriteIds.Count - 1;
        }

        public static void MoveUp(int id)
        {
            var index = _FavoriteIds.IndexOf(id);
            _FavoriteIds.RemoveAt(index);
            _FavoriteIds.Insert(index - 1, id);
        }

        public static void MoveDown(int id)
        {
            var index = _FavoriteIds.IndexOf(id);
            _FavoriteIds.RemoveAt(index);
            _FavoriteIds.Insert(index + 1, id);
        }

        public static void ResetFavorites()
        {
            _FavoriteIds.Clear();
        }

        private IEnumerable<int> _FavoriteTagIds;
        public IEnumerable<int> FavoriteTagIds
        {
            get
            {
                return _FavoriteTagIds;
            }
            private set
            {
                if (!EqualityComparer<IEnumerable<int>>.Default.Equals(_FavoriteTagIds, value))
                {
                    _FavoriteTagIds = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("FavoriteTagIds"));
                }
            }
        }
        public event PropertyChangedEventHandler PropertyChanged;
    }
}
