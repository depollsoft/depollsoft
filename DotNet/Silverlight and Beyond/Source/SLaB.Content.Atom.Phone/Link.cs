using System;
using System.Collections.Generic;
using System.ComponentModel;
using SLaB.Utilities;

namespace SLaB.Content.Atom
{
    public class Link : INotifyPropertyChanged
    {

        private int _Length;
        public int Length
        {
            get
            {
                return _Length;
            }
            set
            {
                if (!EqualityComparer<int>.Default.Equals(_Length, value))
                {
                    _Length = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Length"));
                }
            }
        }

        private string _Title;
        public string Title
        {
            get
            {
                return _Title;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Title, value))
                {
                    _Title = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Title"));
                }
            }
        }

        private string _HrefLanguage;
        public string HrefLanguage
        {
            get
            {
                return _HrefLanguage;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_HrefLanguage, value))
                {
                    _HrefLanguage = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("HrefLanguage"));
                }
            }
        }

        private string _Type;
        public string Type
        {
            get
            {
                return _Type;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Type, value))
                {
                    _Type = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Type"));
                }
            }
        }

        private LinkRelationship _Relationship;
        public LinkRelationship Relationship
        {
            get
            {
                return _Relationship;
            }
            set
            {
                if (!EqualityComparer<LinkRelationship>.Default.Equals(_Relationship, value))
                {
                    _Relationship = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Relationship"));
                }
            }
        }

        private Uri _Href;
        public Uri Href
        {
            get
            {
                return _Href;
            }
            set
            {
                if (!EqualityComparer<Uri>.Default.Equals(_Href, value))
                {
                    _Href = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Href"));
                }
            }
        }
        public event PropertyChangedEventHandler PropertyChanged;
    }

    public enum LinkRelationship
    {
        Alternate,
        Enclosure,
        Related,
        Self,
        Via,
    }
}
