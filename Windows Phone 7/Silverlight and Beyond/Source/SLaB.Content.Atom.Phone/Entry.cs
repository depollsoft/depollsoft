using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.ComponentModel;
using SLaB.Utilities;

namespace SLaB.Content.Atom
{
    public class Entry : INotifyPropertyChanged
    {
        public Entry()
        {
            Authors = new ObservableCollection<Person>();
            Links = new ObservableCollection<Link>();
            Categories = new ObservableCollection<Category>();
            Contributors = new ObservableCollection<Person>();
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

        private Content _Content;
        public Content Content
        {
            get
            {
                return _Content;
            }
            set
            {
                if (!EqualityComparer<Content>.Default.Equals(_Content, value))
                {
                    _Content = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Content"));
                }
            }
        }
        public ObservableCollection<Link> Links { get; private set; }

        private Text _Summary;
        public Text Summary
        {
            get
            {
                return _Summary;
            }
            set
            {
                if (!EqualityComparer<Text>.Default.Equals(_Summary, value))
                {
                    _Summary = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Summary"));
                }
            }
        }
        public ObservableCollection<Category> Categories { get; private set; }
        public ObservableCollection<Person> Contributors { get; private set; }

        private DateTime _Published;
        public DateTime Published
        {
            get
            {
                return _Published;
            }
            set
            {
                if (!EqualityComparer<DateTime>.Default.Equals(_Published, value))
                {
                    _Published = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Published"));
                }
            }
        }

        private Feed _Source;
        public Feed Source
        {
            get
            {
                return _Source;
            }
            set
            {
                if (!EqualityComparer<Feed>.Default.Equals(_Source, value))
                {
                    _Source = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Source"));
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

        public override bool Equals(object obj)
        {
            return ((Entry)obj).Id.Equals(Id);
        }

        public event PropertyChangedEventHandler PropertyChanged;
    }
}
