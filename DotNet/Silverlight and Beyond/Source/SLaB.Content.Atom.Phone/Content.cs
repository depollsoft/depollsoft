using System.Collections.Generic;
using System.ComponentModel;
using SLaB.Utilities;

namespace SLaB.Content.Atom
{
    public class Content : Text
    {

        private string _Source;
        public string Source
        {
            get
            {
                return _Source;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_Source, value))
                {
                    _Source = value;
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("Source"));
                }
            }
        }
    }
}
