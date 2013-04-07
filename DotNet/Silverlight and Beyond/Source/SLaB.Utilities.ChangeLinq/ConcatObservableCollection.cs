#region Using Directives

using System.Collections.Generic;
using System.Collections.Specialized;
using System.Linq;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    internal class ConcatObservableCollection<T> : ChangeLinqBase<T>
    {

        private readonly IEnumerable<T> _Concat;
        private int _ConcatCount;
        private readonly IEnumerable<T> _Original;
        private int _OriginalCount;



        public ConcatObservableCollection(IEnumerable<T> original, IEnumerable<T> concat)
        {
            this._Original = original;
            this._Concat = concat;
        }




        public override void Refresh()
        {
            if (this._Original is IRefreshable)
                ((IRefreshable)this._Original).Refresh();
            if (this._Concat is IRefreshable)
                ((IRefreshable)this._Concat).Refresh();
            this.Reset();
        }

        protected override void AttachToSource()
        {
            if (this._Original is INotifyCollectionChanged)
                ((INotifyCollectionChanged)this._Original).CollectionChanged +=
                    this.ConcatObservableCollectionCollectionChanged;
            if (this._Concat is INotifyCollectionChanged)
                ((INotifyCollectionChanged)this._Concat).CollectionChanged +=
                    this.ConcatObservableCollectionCollectionChanged;
            this.SuppressChangeNotifications++;
            this.Reset();
            this.SuppressChangeNotifications--;
        }

        protected override void DetachFromSource()
        {
            if (this._Original is INotifyCollectionChanged)
                ((INotifyCollectionChanged)this._Original).CollectionChanged -=
                    this.ConcatObservableCollectionCollectionChanged;
            if (this._Concat is INotifyCollectionChanged)
                ((INotifyCollectionChanged)this._Concat).CollectionChanged -=
                    this.ConcatObservableCollectionCollectionChanged;
        }

        private void AddToCount(object sender, int toAdd)
        {
            if (sender == this._Original)
                this._OriginalCount += toAdd;
            else
                this._ConcatCount += toAdd;
        }

        private void ConcatObservableCollectionCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            int startIndex;
            List<T> added = new List<T>();
            List<T> removed = new List<T>();
            switch (e.Action)
            {
                case NotifyCollectionChangedAction.Add:
                    startIndex = this.TranslateIndex(sender, e.NewStartingIndex);
                    foreach (var item in e.NewItems.Cast<T>().Reverse())
                    {
                        this.DelayChangeNotifications = true;
                        added.Insert(0, item);
                        this.Items.Insert(startIndex, item);
                        this.AddToCount(sender, 1);
                        this.DelayChangeNotifications = false;
                    }
                    break;
                case NotifyCollectionChangedAction.Remove:
                    startIndex = this.TranslateIndex(sender, e.OldStartingIndex);
                    foreach (var item in e.OldItems.Cast<T>().Reverse())
                    {
                        this.DelayChangeNotifications = true;
                        removed.Add(this.Items[startIndex]);
                        this.Items.RemoveAt(startIndex);
                        this.AddToCount(sender, -1);
                        this.DelayChangeNotifications = false;
                    }
                    break;
                case NotifyCollectionChangedAction.Replace:
                    this.SuppressChangeNotifications++;
                    startIndex = this.TranslateIndex(sender, e.NewStartingIndex);
                    foreach (var item in e.OldItems.Cast<T>().Reverse())
                    {
                        removed.Add(this.Items[startIndex]);
                        this.Items.RemoveAt(startIndex);
                        this.AddToCount(sender, -1);
                    }
                    foreach (var item in e.NewItems.Cast<T>().Reverse())
                    {
                        added.Insert(0, item);
                        this.Items.Insert(startIndex, item);
                        this.AddToCount(sender, 1);
                    }
                    this.SuppressChangeNotifications--;
                    this.RaisePropertyChanged("Count");
                    this.RaisePropertyChanged("Item[]");
                    this.RaiseCollectionChanged(
                        new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Replace,
                                                             added.Single(),
                                                             removed.Single(),
                                                             startIndex));
                    break;
                case NotifyCollectionChangedAction.Reset:
                    this.Reset();
                    break;
            }
        }

        private void Reset()
        {
            this.SuppressChangeNotifications++;
            this.Items.Clear();
            this._OriginalCount = 0;
            this._ConcatCount = 0;
            foreach (var item in this._Original)
            {
                this.Items.Add(item);
                this._OriginalCount++;
            }
            foreach (var item in this._Concat)
            {
                this.Items.Add(item);
                this._ConcatCount++;
            }
            this.SuppressChangeNotifications--;
            this.RaisePropertyChanged("Count");
            this.RaisePropertyChanged("Item[]");
            this.RaiseCollectionChanged(new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
        }

        private int TranslateIndex(object sender, int index)
        {
            if (sender == this._Original)
                return index;
            return this._OriginalCount + index;
        }
    }
}