#region Using Directives

using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Collections.Specialized;
using System.ComponentModel;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    internal class ObservableCollectionBase<T> : IEnumerable<T>, INotifyCollectionChanged, INotifyPropertyChanged,
                                                 ICollection
    {

        private readonly object _CcLock = new object();
        private NotifyCollectionChangedEventHandler _CollectionChanged;
        private bool _DelayChangeNotifications;
        private readonly List<Action> _DelayedNotifications;
        private ObservableCollection<T> _Items;
        private readonly object _PcLock = new object();
        private PropertyChangedEventHandler _PropertyChanged;
        private readonly object _SyncRoot = new object();



        public ObservableCollectionBase()
        {
            this.Items = new ObservableCollection<T>();
            this._DelayedNotifications = new List<Action>();
        }



        protected bool DelayChangeNotifications
        {
            get { return this._DelayChangeNotifications; }
            set
            {
                if (this._DelayChangeNotifications != value)
                {
                    this._DelayChangeNotifications = value;
                    if (!this._DelayChangeNotifications)
                    {
                        foreach (Action notification in this._DelayedNotifications)
                            notification();
                        this._DelayedNotifications.Clear();
                    }
                }
            }
        }

        protected ObservableCollection<T> Items
        {
            get { return this._Items; }
            set
            {
                if (this._Items != value)
                {
                    if (this._Items != null)
                        this.DeregisterItems();
                    this._Items = value;
                    if (this._Items != null)
                        this.RegisterItems();
                    this.RaiseCollectionChanged(new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
                    this.RaisePropertyChanged("Item[]");
                    this.RaisePropertyChanged("Count");
                }
            }
        }

        protected int ListenerCount { get; private set; }

        protected int SuppressChangeNotifications { get; set; }




        protected virtual void ListenerAdded()
        {
        }

        protected virtual void ListenerRemoved()
        {
        }

        protected void RaiseCollectionChanged(NotifyCollectionChangedEventArgs args)
        {
            if (this.SuppressChangeNotifications > 0)
                return;
            if (this.DelayChangeNotifications)
            {
                this._DelayedNotifications.Add(() => this.RaiseCollectionChanged(args));
                return;
            }
            var cc = this._CollectionChanged;
            if (cc != null)
                cc(this, args);
        }

        protected void RaisePropertyChanged(string propertyName)
        {
            if (this.SuppressChangeNotifications > 0)
                return;
            if (this.DelayChangeNotifications)
            {
                this._DelayedNotifications.Add(() => this.RaisePropertyChanged(propertyName));
                return;
            }
            var pc = this._PropertyChanged;
            if (pc != null)
                pc(this, new PropertyChangedEventArgs(propertyName));
        }

        private void DeregisterItems()
        {
            this._Items.CollectionChanged -= this.ItemsCollectionChanged;
            ((INotifyPropertyChanged)this._Items).PropertyChanged -= this.ItemsPropertyChanged;
        }

        private void ItemsCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            this.RaiseCollectionChanged(e);
        }

        private void ItemsPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            this.RaisePropertyChanged(e.PropertyName);
        }

        private void RegisterItems()
        {
            this._Items.CollectionChanged += this.ItemsCollectionChanged;
            ((INotifyPropertyChanged)this._Items).PropertyChanged += this.ItemsPropertyChanged;
        }




        #region ICollection Members

        public int Count
        {
            get { return this._Items == null ? 0 : this._Items.Count; }
        }

        public bool IsSynchronized
        {
            get { return this._Items == null ? false : ((ICollection)this._Items).IsSynchronized; }
        }

        public object SyncRoot
        {
            get { return this._SyncRoot; }
        }

        public void CopyTo(Array array, int index)
        {
            if (this._Items == null)
                ((ICollection)new ObservableCollection<T>()).CopyTo(array, index);
            else
                ((ICollection)this._Items).CopyTo(array, index);
        }

        #endregion

        #region IEnumerable<T> Members

        public virtual IEnumerator<T> GetEnumerator()
        {
            if (this._Items == null)
                return new List<T>().GetEnumerator();
            return this._Items.GetEnumerator();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return this.GetEnumerator();
        }

        #endregion

        #region INotifyCollectionChanged Members

        public event NotifyCollectionChangedEventHandler CollectionChanged
        {
            add
            {
                lock (this._CcLock)
                {
                    this._CollectionChanged += value;
                    this.ListenerCount++;
                    this.ListenerAdded();
                }
            }
            remove
            {
                lock (this._CcLock)
                {
                    this._CollectionChanged -= value;
                    this.ListenerCount--;
                    this.ListenerRemoved();
                }
            }
        }

        #endregion

        #region INotifyPropertyChanged Members

        event PropertyChangedEventHandler INotifyPropertyChanged.PropertyChanged
        {
            add
            {
                lock (this._PcLock)
                {
                    this._PropertyChanged += value;
                    this.ListenerCount++;
                    this.ListenerAdded();
                }
            }
            remove
            {
                lock (this._PcLock)
                {
                    this._PropertyChanged -= value;
                    this.ListenerCount--;
                    this.ListenerRemoved();
                }
            }
        }

        #endregion
    }
}