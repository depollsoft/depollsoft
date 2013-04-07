using System.Collections.Generic;
using System.ComponentModel;
using SLaB.Utilities;

namespace SLaB.Content.Atom
{
    public class Category : INotifyPropertyChanged
    {

        private string _Term;
        public string Term
        {
            get
            {
                return _Term;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Term, value))
                {
                    _Term = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Term"));
                }
            }
        }

        private string _Scheme;
        public string Scheme
        {
            get
            {
                return _Scheme;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Scheme, value))
                {
                    _Scheme = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Scheme"));
                }
            }
        }


        private string _Label;
        public string Label
        {
            get
            {
                return _Label;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Label, value))
                {
                    _Label = value;
                    PropertyChanged.Raise(this, new PropertyChangedEventArgs("Label"));
                }
            }
        }
        public event PropertyChangedEventHandler PropertyChanged;
    }
}
