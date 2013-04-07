#region Using Directives

using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Linq;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    internal class WhereObservableCollection<T> : ChangeLinqBase<T>
    {

        private readonly List<int> _Mappings;
        private readonly IEnumerable<T> _Original;
        private readonly Func<T, bool> _Predicate;



        public WhereObservableCollection(IEnumerable<T> original, Func<T, bool> predicate)
        {
            this._Predicate = predicate;
            this._Mappings = new List<int>();
            this._Original = original;
        }




        public override void Refresh()
        {
            if (this._Original is IRefreshable)
                ((IRefreshable)this._Original).Refresh();
            this.Reset();
        }

        public void Reset()
        {
            this.SuppressChangeNotifications++;
            this.Items.Clear();
            this._Mappings.Clear();
            int mappedIndex = 0;
            foreach (var item in this._Original)
            {
                if (this._Predicate(item))
                {
                    this._Mappings.Add(mappedIndex++);
                    this.Items.Add(item);
                }
                else
                    this._Mappings.Add(-1);
            }
            this.SuppressChangeNotifications--;
            this.RaisePropertyChanged("Count");
            this.RaisePropertyChanged("Item[]");
            this.RaiseCollectionChanged(new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
        }

        protected override void AttachToSource()
        {
            if (this._Original is INotifyCollectionChanged)
                ((INotifyCollectionChanged)this._Original).CollectionChanged +=
                    this.WhereObservableCollectionCollectionChanged;
            this.SuppressChangeNotifications++;
            this.Reset();
            this.SuppressChangeNotifications--;
        }

        protected override void DetachFromSource()
        {
            if (this._Original is INotifyCollectionChanged)
                ((INotifyCollectionChanged)this._Original).CollectionChanged -=
                    this.WhereObservableCollectionCollectionChanged;
        }

        private int GetNextIndex(int unmappedIndex)
        {
            if (unmappedIndex <= 0)
                return 0;
            if (unmappedIndex >= this._Mappings.Count)
                return this.Count;
            if (this._Mappings[unmappedIndex] >= 0)
                return this._Mappings[unmappedIndex];
            var result = from item in this._Mappings.Skip(unmappedIndex + 1)
                         where item >= 0
                         select item;
            if (result.Count() > 0)
                return result.First();
            result = from item in this._Mappings.Take(unmappedIndex)
                     where item >= 0
                     select item;
            if (result.Count() > 0)
                return result.Last() + 1;
            throw new Exception("This should never be reached");
        }

        private void RefreshMappings()
        {
            int soFar = 0;
            for (int x = 0; x < this._Mappings.Count; x++)
                if (this._Mappings[x] >= 0)
                    this._Mappings[x] = soFar++;
        }

        private void WhereObservableCollectionCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            int startIndex;
            List<T> added = new List<T>();
            List<T> removed = new List<T>();
            switch (e.Action)
            {
                case NotifyCollectionChangedAction.Add:
                    startIndex = this.GetNextIndex(e.NewStartingIndex);
                    foreach (var item in e.NewItems.Cast<T>().Reverse())
                    {
                        this.DelayChangeNotifications = true;
                        if (this._Predicate(item))
                        {
                            added.Insert(0, item);
                            this.Items.Insert(startIndex, item);
                            this._Mappings.Insert(e.NewStartingIndex, 0);
                        }
                        else
                            this._Mappings.Insert(e.NewStartingIndex, -1);
                        this.RefreshMappings();
                        this.DelayChangeNotifications = false;
                    }
                    break;
                case NotifyCollectionChangedAction.Remove:
                    startIndex = this.GetNextIndex(e.OldStartingIndex);
                    for (int x = e.OldStartingIndex; x < e.OldStartingIndex + e.OldItems.Count; x++)
                    {
                        this.DelayChangeNotifications = true;
                        if (this._Mappings[x] >= 0)
                        {
                            removed.Add(this.Items[this._Mappings[x]]);
                            this.Items.RemoveAt(this._Mappings[x] - removed.Count + 1);
                        }
                        this._Mappings.RemoveAt(x);
                        this.RefreshMappings();
                        this.DelayChangeNotifications = false;
                    }
                    break;
                case NotifyCollectionChangedAction.Replace:
                    this.SuppressChangeNotifications++;
                    startIndex = this.GetNextIndex(e.NewStartingIndex);
                    for (int x = e.NewStartingIndex; x < e.NewStartingIndex + e.OldItems.Count; x++)
                    {
                        if (this._Mappings[x] >= 0)
                        {
                            removed.Add(this.Items[this._Mappings[x]]);
                            this.Items.RemoveAt(this._Mappings[x] - removed.Count + 1);
                        }
                        this._Mappings.RemoveAt(x);
                    }
                    this.RefreshMappings();
                    foreach (var item in e.NewItems.Cast<T>().Reverse())
                    {
                        if (this._Predicate(item))
                        {
                            added.Insert(0, item);
                            this.Items.Insert(startIndex, item);
                            this._Mappings.Insert(e.NewStartingIndex, 0);
                        }
                        else
                        {
                            this._Mappings.Insert(e.NewStartingIndex, -1);
                        }
                    }
                    this.RefreshMappings();
                    this.SuppressChangeNotifications--;
                    if (added.Count > 0 && removed.Count > 0)
                    {
                        this.RaisePropertyChanged("Count");
                        this.RaisePropertyChanged("Item[]");
                        this.RaiseCollectionChanged(
                            new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Replace,
                                                                 added.Single(),
                                                                 removed.Single(),
                                                                 startIndex));
                    }
                    else if (added.Count > 0)
                    {
                        this.RaisePropertyChanged("Count");
                        this.RaisePropertyChanged("Item[]");
                        this.RaiseCollectionChanged(
                            new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Add,
                                                                 added.Single(),
                                                                 startIndex));
                    }
                    else if (removed.Count > 0)
                    {
                        this.RaisePropertyChanged("Count");
                        this.RaisePropertyChanged("Item[]");
                        this.RaiseCollectionChanged(
                            new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Remove,
                                                                 removed.Single(),
                                                                 startIndex));
                    }
                    break;
                case NotifyCollectionChangedAction.Reset:
                    this.Reset();
                    break;
            }
        }
    }
}