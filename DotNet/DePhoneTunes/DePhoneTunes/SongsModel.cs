using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using System.IO.IsolatedStorage;
using System.Linq;
using SLaB.Utilities;

namespace DePhoneTunes
{
    public class SongsModel : KeysModel
    {
        private static SongsModel _Instance;
        public static SongsModel Instance
        {
            get
            {
                if (_Instance == null)
                    new SongsModel();
                return _Instance;
            }
        }
        public SongsModel()
            : base()
        {
            if (!DesignerProperties.IsInDesignTool)
            {
                IsolatedStorageSettings.ApplicationSettings.SetIfContainsKey<ObservableCollection<Song>>("Songs", val => _Songs = val);
                if (_Songs == null)
                {
                    _Songs = new ObservableCollection<Song>();
                    IsolatedStorageSettings.ApplicationSettings.SetIfNotInDesignMode("Songs", _Songs);
                }
            }
            else
            {
                _Songs = new ObservableCollection<Song>();
            }

            AllKeys = (from k in MajorKeys
                       select new SongKey { Note = k, KeyType = DePhoneTunes.KeyType.Major }).Concat(
                       from k in MinorKeys
                       select new SongKey { Note = k, KeyType = DePhoneTunes.KeyType.Minor }).ToList();
            _Instance = this;
        }


        private Song _SelectedSong;
        public Song SelectedSong
        {
            get
            {
                return _SelectedSong;
            }
            set
            {
                if (!EqualityComparer<Song>.Default.Equals(_SelectedSong, value))
                {
                    _SelectedSong = value;
                    if (value == null)
                        SelectedNote = null;
                    else
                        SelectedNote = SelectedSong.Key.Note;
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("SelectedSong"));
                }
            }
        }


        private IEnumerable<SongKey> _AllKeys;
        public IEnumerable<SongKey> AllKeys
        {
            get
            {
                return _AllKeys;
            }
            set
            {
                if (!EqualityComparer<IEnumerable<SongKey>>.Default.Equals(_AllKeys, value))
                {
                    _AllKeys = value;
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("AllKeys"));
                }
            }
        }

        public Song SongForGuid(Guid guid)
        {
            return (from s in Songs
                    where s.Id.Equals(guid)
                    select s).FirstOrDefault();
        }

        public Song NewSong()
        {
            Song s = new Song { Title = "Title", Key = AllKeys.ElementAt(AllKeys.Count() / 4) };
            ((IList<Song>)_Songs).Add(s);
            return s;
        }

        public void Remove(Song s)
        {
            IList<Song> songs = _Songs as IList<Song>;
            songs.Remove(s);
            RefreshSelectedSong();
        }

        public void RefreshSelectedSong()
        {
            Song s = SelectedSong;
            SelectedSong = null;
            if (Songs.Contains(s))
                SelectedSong = s;
        }

        public void MoveUp(Song s)
        {
            IList<Song> songs = _Songs as IList<Song>;
            int index = songs.IndexOf(s);
            if (index > 0)
            {
                songs.RemoveAt(index);
                songs.Insert(index - 1, s);
            }
        }

        public void MoveDown(Song s)
        {
            IList<Song> songs = _Songs as IList<Song>;
            int index = songs.IndexOf(s);
            if (index < songs.Count - 1)
            {
                songs.RemoveAt(index);
                songs.Insert(index + 1, s);
            }
        }

        public bool CanMoveUp(Song s)
        {
            IList<Song> songs = _Songs as IList<Song>;
            int index = songs.IndexOf(s);
            return index > 0;
        }

        public bool CanMoveDown(Song s)
        {
            IList<Song> songs = _Songs as IList<Song>;
            int index = songs.IndexOf(s);
            return index < songs.Count - 1;
        }

        public void SortByTitle()
        {
            IList<Song> songs = _Songs as IList<Song>;
            IEnumerable<Song> sorted = songs.OrderBy(song => song.Title.ToLowerInvariant()).ToArray();
            songs.Clear();
            foreach (Song song in sorted)
            {
                songs.Add(song);
            }
        }

        private IEnumerable<Song> _Songs;
        public IEnumerable<Song> Songs
        {
            get
            {
                return _Songs;
            }
            private set
            {
                if (!EqualityComparer<IEnumerable<Song>>.Default.Equals(_Songs, value))
                {
                    _Songs = value;
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("Songs"));
                }
            }
        }
    }
}
