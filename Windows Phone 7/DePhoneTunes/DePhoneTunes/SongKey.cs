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
using SLaB.Utilities;
using System.ComponentModel;
using System.Collections.Generic;

namespace DePhoneTunes
{
    public class SongKey : INotifyPropertyChanged
    {

        private Note _Note;
        public Note Note
        {
            get
            {
                return _Note;
            }
            set
            {
                if (!EqualityComparer<Note>.Default.Equals(_Note, value))
                {
                    _Note = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Note"));
                }
            }
        }


        private KeyType _KeyType;
        public KeyType KeyType
        {
            get
            {
                return _KeyType;
            }
            set
            {
                if (!EqualityComparer<KeyType>.Default.Equals(_KeyType, value))
                {
                    _KeyType = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("KeyType"));
                }
            }
        }

        public override bool Equals(object obj)
        {
            if (!(obj is SongKey))
                return false;
            SongKey other = (SongKey)obj;
            return KeyType == other.KeyType && Note.Equals(other.Note);
        }

        public override int GetHashCode()
        {
            return Note.GetHashCode();
        }

        public event PropertyChangedEventHandler PropertyChanged;
    }
}
