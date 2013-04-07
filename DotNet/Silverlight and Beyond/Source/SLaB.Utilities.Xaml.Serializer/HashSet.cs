
namespace System.Collections.Generic
{
    /// <summary>
    /// Provides a simple HashSet implementation for pre-SL4 applications.
    /// </summary>
    /// <typeparam name="T"></typeparam>
    public class HashSet<T> : ISet<T>
    {

        private Dictionary<T, object> _BackingStore;



        /// <summary>
        /// Initializes a new instance of the <see cref="HashSet&lt;T&gt;"/> class.
        /// </summary>
        /// <param name="items">The items.</param>
        /// <param name="comparer">The comparer.</param>
        public HashSet(IEnumerable<T> items, IEqualityComparer<T> comparer)
        {
            _BackingStore = new Dictionary<T, object>(comparer);
            if (items != null)
                foreach (var item in items)
                    _BackingStore[item] = null;
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="HashSet&lt;T&gt;"/> class.
        /// </summary>
        /// <param name="items">The items.</param>
        public HashSet(IEnumerable<T> items)
            : this(items, null)
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="HashSet&lt;T&gt;"/> class.
        /// </summary>
        /// <param name="comparer">The comparer.</param>
        public HashSet(IEqualityComparer<T> comparer)
            : this(null, comparer)
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="HashSet&lt;T&gt;"/> class.
        /// </summary>
        public HashSet()
            : this(null, null)
        {
        }



        /// <summary>
        /// Gets the count.
        /// </summary>
        /// <value>The count.</value>
        public int Count
        {
            get { return _BackingStore.Count; }
        }

        /// <summary>
        /// Gets a value indicating whether this instance is read only.
        /// </summary>
        /// <value>
        /// 	<c>true</c> if this instance is read only; otherwise, <c>false</c>.
        /// </value>
        public bool IsReadOnly
        {
            get { return false; }
        }




        /// <summary>
        /// Adds the specified item.
        /// </summary>
        /// <param name="item">The item.</param>
        /// <returns></returns>
        public bool Add(T item)
        {
            if (!_BackingStore.ContainsKey(item))
            {
                _BackingStore[item] = null;
                return true;
            }
            return false;
        }

        /// <summary>
        /// Clears this instance.
        /// </summary>
        public void Clear()
        {
            _BackingStore.Clear();
        }

        /// <summary>
        /// Determines whether the set contains the specified item.
        /// </summary>
        /// <param name="item">The item.</param>
        /// <returns>
        /// 	<c>true</c> if the set contains the specified item; otherwise, <c>false</c>.
        /// </returns>
        public bool Contains(T item)
        {
            return _BackingStore.ContainsKey(item);
        }

        /// <summary>
        /// Copies to another array.
        /// </summary>
        /// <param name="array">The array.</param>
        /// <param name="arrayIndex">Index in the array to begin copying.</param>
        public void CopyTo(T[] array, int arrayIndex)
        {
            _BackingStore.Keys.CopyTo(array, arrayIndex);
        }

        /// <summary>
        /// Provides an exclusive-or combination of the sets.
        /// </summary>
        /// <param name="other">The other.</param>
        public void ExceptWith(IEnumerable<T> other)
        {
            foreach (var item in other)
                this.Remove(item);
        }

        /// <summary>
        /// Gets the enumerator.
        /// </summary>
        /// <returns></returns>
        public IEnumerator<T> GetEnumerator()
        {
            return _BackingStore.Keys.GetEnumerator();
        }

        /// <summary>
        /// Provides an intersection of the sets.
        /// </summary>
        /// <param name="other">The other.</param>
        public void IntersectWith(IEnumerable<T> other)
        {
            var newBackingStore = new Dictionary<T, object>(_BackingStore.Comparer);
            foreach (var item in other)
                if (this.Contains(item))
                    newBackingStore[item] = null;
            _BackingStore = newBackingStore;
        }

        /// <summary>
        /// Determines whether the set is a proper subset of the other collection..
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>
        /// 	<c>true</c> if the set is a proper subset of the other collection; otherwise, <c>false</c>.
        /// </returns>
        public bool IsProperSubsetOf(IEnumerable<T> other)
        {
            int count = 0;
            HashSet<T> foundItems = new HashSet<T>(_BackingStore.Comparer);
            foreach (var item in other)
            {
                if (this.Contains(item))
                    foundItems.Add(item);
                count++;
            }
            return count > foundItems.Count && foundItems.Count == this.Count;
        }

        /// <summary>
        /// Determines whether the set is a proper superset of the other collection.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>
        /// 	<c>true</c> if the set is a proper superset of the other collection; otherwise, <c>false</c>.
        /// </returns>
        public bool IsProperSupersetOf(IEnumerable<T> other)
        {
            int foundCount = 0;
            foreach (var item in other)
                if (!this.Contains(item))
                    return false;
                else foundCount++;
            return foundCount < this.Count;
        }

        /// <summary>
        /// Determines whether the set is a subset of the other collection.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>
        /// 	<c>true</c> if the set is a subset of the other collection; otherwise, <c>false</c>.
        /// </returns>
        public bool IsSubsetOf(IEnumerable<T> other)
        {
            HashSet<T> foundItems = new HashSet<T>(_BackingStore.Comparer);
            foreach (var item in other)
                if (this.Contains(item))
                    foundItems.Add(item);
            return foundItems.Count == this.Count;
        }

        /// <summary>
        /// Determines whether the set is a superset of the other collection.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>
        /// 	<c>true</c> if the set is a superset of the other collection; otherwise, <c>false</c>.
        /// </returns>
        public bool IsSupersetOf(System.Collections.Generic.IEnumerable<T> other)
        {
            foreach (var item in other)
                if (!this.Contains(item))
                    return false;
            return true;
        }

        /// <summary>
        /// Determines whether the set overlaps with the other collection.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>Whether the set overlaps with the other collection.</returns>
        public bool Overlaps(IEnumerable<T> other)
        {
            foreach (var item in other)
                if (this.Contains(item))
                    return true;
            return false;
        }

        /// <summary>
        /// Removes the specified item.
        /// </summary>
        /// <param name="item">The item.</param>
        /// <returns></returns>
        public bool Remove(T item)
        {
            return _BackingStore.Remove(item);
        }

        /// <summary>
        /// Determines whether the sets are equal.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>Whether the sets are equal.</returns>
        public bool SetEquals(IEnumerable<T> other)
        {
            return this.IsSubsetOf(other) && this.IsSupersetOf(other);
        }

        /// <summary>
        /// Performs a symmetric exclusive or with the other set.
        /// </summary>
        /// <param name="other">The other.</param>
        public void SymmetricExceptWith(IEnumerable<T> other)
        {
            var toAdd = new HashSet<T>(_BackingStore.Comparer);
            var toRemove = new HashSet<T>(_BackingStore.Comparer);
            foreach (var item in other)
                if (!this.Contains(item))
                    toAdd.Add(item);
                else
                    toRemove.Add(item);
            foreach (var item in toAdd)
                this.Add(item);
            foreach (var item in toRemove)
                this.Remove(item);
        }

        /// <summary>
        /// Takes the union of the two sets.
        /// </summary>
        /// <param name="other">The other.</param>
        public void UnionWith(IEnumerable<T> other)
        {
            foreach (var item in other)
                this.Add(item);
        }

        /// <summary>
        /// Returns an enumerator that iterates through a collection.
        /// </summary>
        /// <returns>
        /// An <see cref="T:System.Collections.IEnumerator"/> object that can be used to iterate through the collection.
        /// </returns>
        IEnumerator IEnumerable.GetEnumerator()
        {
            return _BackingStore.Keys.GetEnumerator();
        }


        /// <summary>
        /// Adds the specified item.
        /// </summary>
        /// <param name="item">The item.</param>
        void ICollection<T>.Add(T item)
        {
            this.Add(item);
        }
    }
}
