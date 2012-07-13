#region Using Directives

using System;
using System.ComponentModel;
using System.Text;

#endregion

namespace SLaB.Utilities.Xaml.Collections
{
    /// <summary>
    ///   Represents a KeyValuePair where the value can change (and supports being bound to).
    /// </summary>
    /// <typeparam name = "TKey">The type of the key.</typeparam>
    /// <typeparam name = "TValue">The type of the value.</typeparam>
    public sealed class ObservableKeyValuePair<TKey, TValue> : INotifyPropertyChanged
    {

        private readonly TKey _Key;
        private TValue _Value;



        /// <summary>
        ///   Creates an ObservableKeyValuePair with the given key.
        /// </summary>
        /// <param name = "key">The key for the pair.</param>
        public ObservableKeyValuePair(TKey key)
        {
            this._Key = key;
        }



        /// <summary>
        ///   Gets the key for this KeyValuePair.
        /// </summary>
        public TKey Key
        {
            get { return this._Key; }
        }

        /// <summary>
        ///   Gets or sets the value for this KeyValuePair.
        /// </summary>
        public TValue Value
        {
            get { return this._Value; }
            set
            {
                if (Equals(this._Value, value))
                    return;
                this._Value = value;
                this.OnPropertyChanged("Value");
            }
        }




        /// <summary>
        ///   Converts the ObservableKeyValuePair into a string.
        /// </summary>
        /// <returns>A string representation of the ObservableKeyValuePair.</returns>
        public override string ToString()
        {
            StringBuilder builder = new StringBuilder();
            builder.Append('[');
            if (this.Key is ValueType || this.Key != null)
                builder.Append(this.Key.ToString());
            builder.Append(", ");
            if (this.Value is ValueType || this.Value != null)
                builder.Append(this.Value.ToString());
            builder.Append(']');
            return builder.ToString();
        }

        private void OnPropertyChanged(string propertyName)
        {
            var pc = this.PropertyChanged;
            if (pc != null)
                pc(this, new PropertyChangedEventArgs(propertyName));
        }




        #region INotifyPropertyChanged Members

        /// <summary>
        ///   An event raised whenever the Value of this ObservableKeyValuePair changes.
        /// </summary>
        public event PropertyChangedEventHandler PropertyChanged;

        #endregion
    }
}