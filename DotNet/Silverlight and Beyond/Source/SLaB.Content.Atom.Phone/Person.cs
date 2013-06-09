using System;
using System.Collections.Generic;
using System.ComponentModel;
using SLaB.Utilities;

namespace SLaB.Content.Atom
{
    public class Person : INotifyPropertyChanged
    {

        private string _Email;
        public string Email
        {
            get
            {
                return _Email;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Email, value))
                {
                    _Email = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Email"));
                }
            }
        }

        private Uri _HomePage;
        public Uri HomePage
        {
            get
            {
                return _HomePage;
            }
            set
            {
                if (!EqualityComparer<Uri>.Default.Equals(_HomePage, value))
                {
                    _HomePage = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("HomePage"));
                }
            }
        }

        private string _Name;
        public string Name
        {
            get
            {
                return _Name;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Name, value))
                {
                    _Name = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Name"));
                }
            }
        }
        public event PropertyChangedEventHandler PropertyChanged;
    }
}
