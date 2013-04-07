using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using SLaB.Utilities;

namespace SLaB.Content.Atom
{
    public class Feed : INotifyPropertyChanged
    {
        public Feed()
        {
            Authors = new ObservableCollection<Person>();
            Links = new ObservableCollection<Link>();
            Categories = new ObservableCollection<Category>();
            Contributors = new ObservableCollection<Person>();
            Entries = new ObservableCollection<Entry>();
        }
        private string _Id;
        public string Id
        {
            get
            {
                return _Id;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Id, value))
                {
                    _Id = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Id"));
                }
            }
        }


        private Text _Title;
        public Text Title
        {
            get
            {
                return _Title;
            }
            set
            {
                if (!EqualityComparer<Text>.Default.Equals(_Title, value))
                {
                    _Title = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Title"));
                }
            }
        }

        private DateTime _Updated;
        public DateTime Updated
        {
            get
            {
                return _Updated;
            }
            set
            {
                if (!EqualityComparer<DateTime>.Default.Equals(_Updated, value))
                {
                    _Updated = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Updated"));
                }
            }
        }

        public ObservableCollection<Person> Authors { get; private set; }
        public ObservableCollection<Link> Links { get; private set; }
        public ObservableCollection<Category> Categories { get; private set; }
        public ObservableCollection<Person> Contributors { get; private set; }
        public ObservableCollection<Entry> Entries { get; private set; }


        private Generator _Generator;
        public Generator Generator
        {
            get
            {
                return _Generator;
            }
            set
            {
                if (!EqualityComparer<Generator>.Default.Equals(_Generator, value))
                {
                    _Generator = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Generator"));
                }
            }
        }

        private Uri _Icon;
        public Uri Icon
        {
            get
            {
                return _Icon;
            }
            set
            {
                if (!EqualityComparer<Uri>.Default.Equals(_Icon, value))
                {
                    _Icon = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Icon"));
                }
            }
        }

        private Uri _Logo;
        public Uri Logo
        {
            get
            {
                return _Logo;
            }
            set
            {
                if (!EqualityComparer<Uri>.Default.Equals(_Logo, value))
                {
                    _Logo = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Logo"));
                }
            }
        }

        private Text _Rights;
        public Text Rights
        {
            get
            {
                return _Rights;
            }
            set
            {
                if (!EqualityComparer<Text>.Default.Equals(_Rights, value))
                {
                    _Rights = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Rights"));
                }
            }
        }

        private Text _Subtitle;
        public Text Subtitle
        {
            get
            {
                return _Subtitle;
            }
            set
            {
                if (!EqualityComparer<Text>.Default.Equals(_Subtitle, value))
                {
                    _Subtitle = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Subtitle"));
                }
            }
        }

        public void AddEntry(Entry entry)
        {
            int index = Entries.IndexOf(entry);
            if (index < 0)
                Entries.Add(entry);
            Entries[index] = entry;
        }

        public event PropertyChangedEventHandler PropertyChanged;
    }
}
