using System.Collections.Generic;
using System.ComponentModel;
using SLaB.Utilities;

namespace SLaB.Content.Atom
{
    public class Text : INotifyPropertyChanged
    {

        private ContentType _Type;
        public ContentType Type
        {
            get
            {
                return _Type;
            }
            set
            {
                if (!EqualityComparer<ContentType>.Default.Equals(_Type, value))
                {
                    _Type = value;
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("Type"));
                }
            }
        }


        private string _RawContent;
        public string RawContent
        {
            get
            {
                return _RawContent;
            }
            set
            {
                if (!EqualityComparer<string>.Default.Equals(_RawContent, value))
                {
                    _RawContent = value;
                    _PropertyChanged.Raise(this, new PropertyChangedEventArgs("RawContent"));
                }
            }
        }
        protected PropertyChangedEventHandler _PropertyChanged;
        public event PropertyChangedEventHandler PropertyChanged
        {
            add
            {
                _PropertyChanged += value;
            }
            remove
            {
                _PropertyChanged -= value;
            }
        }
    }
    public enum ContentType
    {
        Text,
        Html,
        XHtml,
        Xml,
    }
}
