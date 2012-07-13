#region Using Directives

using System;
using System.Collections.Generic;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    internal abstract class ChangeLinqBase<T> : ObservableCollectionBase<T>, IChangeLinq<T>, IRefreshable, IList<T>
    {


        protected abstract void AttachToSource();

        protected abstract void DetachFromSource();

        protected override void ListenerAdded()
        {
            if (this.ListenerCount == 1)
                this.AttachToSource();
        }

        protected override void ListenerRemoved()
        {
            if (this.ListenerCount == 0)
                this.DetachFromSource();
        }




        #region IChangeLinq<T> Members

        public override IEnumerator<T> GetEnumerator()
        {
            if (this.ListenerCount == 0)
                this.Refresh();
            return base.GetEnumerator();
        }

        #endregion

        #region IList<T> Members

        T IList<T>.this[int index]
        {
            get
            {
                if (this.ListenerCount == 0)
                    this.Refresh();
                return this.Items[index];
            }
            set { throw new InvalidOperationException("Collection is Read-Only"); }
        }

        int ICollection<T>.Count
        {
            get
            {
                if (this.ListenerCount == 0)
                    this.Refresh();
                return this.Items.Count;
            }
        }

        bool ICollection<T>.IsReadOnly
        {
            get { return true; }
        }

        int IList<T>.IndexOf(T item)
        {
            if (this.ListenerCount == 0)
                this.Refresh();
            return this.Items.IndexOf(item);
        }

        void IList<T>.Insert(int index, T item)
        {
            throw new InvalidOperationException("Collection is Read-Only");
        }

        void IList<T>.RemoveAt(int index)
        {
            throw new InvalidOperationException("Collection is Read-Only");
        }

        void ICollection<T>.Add(T item)
        {
            throw new NotImplementedException();
        }

        void ICollection<T>.Clear()
        {
            throw new InvalidOperationException("Collection is Read-Only");
        }

        bool ICollection<T>.Contains(T item)
        {
            if (this.ListenerCount == 0)
                this.Refresh();
            return this.Items.Contains(item);
        }

        void ICollection<T>.CopyTo(T[] array, int arrayIndex)
        {
            if (this.ListenerCount == 0)
                this.Refresh();
            this.Items.CopyTo(array, arrayIndex);
        }

        bool ICollection<T>.Remove(T item)
        {
            throw new InvalidOperationException("Collection is Read-Only");
        }

        #endregion

        #region IRefreshable Members

        public abstract void Refresh();

        #endregion
    }
}