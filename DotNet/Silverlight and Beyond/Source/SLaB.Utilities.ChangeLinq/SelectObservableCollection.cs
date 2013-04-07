#region Using Directives

using System;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.Diagnostics.CodeAnalysis;
using System.Linq;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    internal class SelectObservableCollection<TIn, TOut> : ChangeLinqBase<TOut>
    {

        private IEnumerable<TIn> _Original;
        private readonly Func<TIn, TOut> _Selector;



        [SuppressMessage("Microsoft.Usage", "CA2214:DoNotCallOverridableMethodsInConstructors")]
        public SelectObservableCollection(IEnumerable<TIn> original, Func<TIn, TOut> selector)
        {
            this._Selector = selector;
            this.Original = original;
        }



        protected IEnumerable<TIn> Original
        {
            get { return this._Original; }
            set
            {
                if (this.ListenerCount > 0)
                    this.DetachFromSource();
                if (value == null)
                    this._Original = new TIn[0];
                else if (!(value is INotifyCollectionChanged))
                    this._Original = value.ToArray();
                else
                    this._Original = value;
                if (this.ListenerCount > 0)
                    this.AttachToSource();
            }
        }




        public override void Refresh()
        {
            if (this._Original is IRefreshable)
                ((IRefreshable)this._Original).Refresh();
            this.Reset();
        }

        protected override void AttachToSource()
        {
            if (this._Original is INotifyCollectionChanged)
                ((INotifyCollectionChanged)this._Original).CollectionChanged +=
                    this.SelectObservableCollectionCollectionChanged;
            this.SuppressChangeNotifications++;
            this.Reset();
            this.SuppressChangeNotifications--;
        }

        protected override void DetachFromSource()
        {
            if (this._Original is INotifyCollectionChanged)
                ((INotifyCollectionChanged)this._Original).CollectionChanged -=
                    this.SelectObservableCollectionCollectionChanged;
        }

        private void Reset()
        {
            this.SuppressChangeNotifications++;
            this.Items.Clear();
            foreach (var item in this._Original)
                this.Items.Add(this._Selector(item));
            this.SuppressChangeNotifications--;
            this.RaisePropertyChanged("Count");
            this.RaisePropertyChanged("Item[]");
            this.RaiseCollectionChanged(new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
        }

        private void SelectObservableCollectionCollectionChanged(object sender, NotifyCollectionChangedEventArgs e)
        {
            this.DelayChangeNotifications = true;
            switch (e.Action)
            {
                case NotifyCollectionChangedAction.Add:
                    var added = from item in e.NewItems.Cast<TIn>()
                                select this._Selector(item);
                    foreach (var item in added.Reverse())
                        this.Items.Insert(e.NewStartingIndex, item);
                    break;
                case NotifyCollectionChangedAction.Remove:
                    foreach (var item in e.OldItems)
                        this.Items.RemoveAt(e.OldStartingIndex);
                    break;
                case NotifyCollectionChangedAction.Replace:
                    var newItems = (from item in e.NewItems.Cast<TIn>()
                                    select this._Selector(item)).ToArray();
                    for (int x = 0; x < e.OldItems.Count; x++)
                        this.Items[e.NewStartingIndex + x] = newItems[x];
                    break;
                case NotifyCollectionChangedAction.Reset:
                    this.Reset();
                    break;
            }
            this.DelayChangeNotifications = false;
        }
    }
}