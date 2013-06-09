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
using System.Collections.Generic;
using SLaB.Utilities;

namespace DePhoneTunes {
  public class Song : INotifyPropertyChanged {
    public Song() {
      Id = Guid.NewGuid();
    }

    private Guid _Id;
    public Guid Id {
      get {
        return _Id;
      }
      set {
        if (!EqualityComparer<Guid>.Default.Equals(_Id, value)) {
          _Id = value;
          PropertyChanged.Raise(this, new PropertyChangedEventArgs("Id"));
        }
      }
    }

    private String _Title;
    public String Title {
      get {
        return _Title;
      }
      set {
        if (!EqualityComparer<String>.Default.Equals(_Title, value)) {
          _Title = value;
          PropertyChanged.Raise(this, new PropertyChangedEventArgs("Title"));
        }
      }
    }


    private SongKey _Key;
    public SongKey Key {
      get {
        return _Key;
      }
      set {
        if (!EqualityComparer<SongKey>.Default.Equals(_Key, value)) {
          _Key = value;
          PropertyChanged.Raise(this, new PropertyChangedEventArgs("Key"));
        }
      }
    }

    public override bool Equals(object obj) {
      if (!(obj is Song))
        return false;
      return Id.Equals(((Song)obj).Id);
    }

    public override int GetHashCode() {
      return Id.GetHashCode();
    }

    public event PropertyChangedEventHandler PropertyChanged;
  }
}
