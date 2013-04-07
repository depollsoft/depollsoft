#region Using Directives

using System.ComponentModel;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    /// <summary>
    ///   Represents a value that can change over time.
    /// </summary>
    /// <typeparam name = "T">The type of the value that the IChangeValue represents.</typeparam>
    public interface IChangeValue<out T> : INotifyPropertyChanged
    {

        /// <summary>
        ///   The current value of the item.
        /// </summary>
        T Value { get; }
    }

    internal class ChangeValue<T> : IChangeValue<T>, INotifyPropertyChanged, IRefreshable
    {

        private T _Value;




        protected virtual void OnPropertyChanged(string propertyName)
        {
            var pc = this.PropertyChanged;
            if (pc != null)
                pc(this, new PropertyChangedEventArgs(propertyName));
        }




        #region IChangeValue<T> Members

        public event PropertyChangedEventHandler PropertyChanged;

        public T Value
        {
            get { return this._Value; }
            protected set
            {
                T oldValue = this._Value;
                this._Value = value;
                if (!Equals(oldValue, value))
                    this.OnPropertyChanged("Value");
            }
        }

        #endregion

        #region IRefreshable Members

        public virtual void Refresh()
        {
            if (this.Value is IRefreshable)
                ((IRefreshable)this.Value).Refresh();
        }

        #endregion
    }
}