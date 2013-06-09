#region Using Directives

using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Diagnostics.CodeAnalysis;

#endregion

namespace SLaB.Utilities.Xaml.Collections
{
    /// <summary>
    ///   A dictionary that supports INotifyPropertyChanged on its KeyValuePairs and INotifyCollectionChanged on itself, making it friendly for
    ///   observation and binding.
    /// </summary>
    /// <typeparam name = "TKey">The Key type.</typeparam>
    /// <typeparam name = "TValue">The Value type.</typeparam>
    public class ObservableDictionary<TKey, TValue> : INotifyPropertyChanged, INotifyCollectionChanged,
                                                      IDictionary<TKey, TValue>,
                                                      ICollection<ObservableKeyValuePair<TKey, TValue>>, IDictionary
    {

        private readonly List<ObservableKeyValuePair<TKey, TValue>> _BaseCollection;
        private readonly Dictionary<TKey, TValue> _BaseDictionary;
        private readonly Dictionary<TKey, int> _IndexDictionary;



        /// <summary>
        ///   Creates an ObservableDictionary.
        /// </summary>
        /// <param name = "dictionary">A source dictionary to import KeyValuePairs from.</param>
        /// <param name = "comparer">An equality comparer used for comparing and doing lookups.</param>
        [SuppressMessage("Microsoft.Usage", "CA2214:DoNotCallOverridableMethodsInConstructors")]
        public ObservableDictionary(IEnumerable<KeyValuePair<TKey, TValue>> dictionary = null,
                                    IEqualityComparer<TKey> comparer = null)
        {
            this.Comparer = comparer;
            this._BaseCollection = new List<ObservableKeyValuePair<TKey, TValue>>();
            this._BaseDictionary = new Dictionary<TKey, TValue>(comparer);
            this._IndexDictionary = new Dictionary<TKey, int>(comparer);
            if (dictionary != null)
                foreach (var pair in dictionary)
                    ((IDictionary<TKey, TValue>)this).Add(pair);
        }



        /// <summary>
        ///   Gets the equality comparer for the ObservableDictionary.
        /// </summary>
        public IEqualityComparer<TKey> Comparer { get; private set; }




        /// <summary>
        ///   Raises the CollectionChanged event.
        /// </summary>
        /// <param name = "args">The NotifyCollectionChangedEventArgs to raise the event with.</param>
        protected virtual void OnCollectionChanged(NotifyCollectionChangedEventArgs args)
        {
            var cc = this.CollectionChanged;
            if (cc != null)
                cc(this, args);
        }

        /// <summary>
        ///   Raises the PropertyChanged event.
        /// </summary>
        /// <param name = "propertyName">The name of the property that has changed.</param>
        protected virtual void OnPropertyChanged(string propertyName)
        {
            var pc = this.PropertyChanged;
            if (pc != null)
                pc(this, new PropertyChangedEventArgs(propertyName));
        }

        private void ItemPropertyChanged(object sender, PropertyChangedEventArgs e)
        {
            var kvp = (ObservableKeyValuePair<TKey, TValue>)sender;
            this._BaseDictionary[kvp.Key] = kvp.Value;
            this.OnPropertyChanged("Item[]");
        }

        private void Subscribe(ObservableKeyValuePair<TKey, TValue> item)
        {
            item.PropertyChanged += this.ItemPropertyChanged;
        }

        private void Unsubscribe(ObservableKeyValuePair<TKey, TValue> item)
        {
            item.PropertyChanged -= this.ItemPropertyChanged;
        }




        #region ICollection<ObservableKeyValuePair<TKey,TValue>> Members

        /// <summary>
        ///   Adds an item to the dictionary.
        /// </summary>
        /// <param name = "item">The item to add.</param>
        public void Add(ObservableKeyValuePair<TKey, TValue> item)
        {
            if (this._BaseDictionary.ContainsKey(item.Key))
                throw new ArgumentException("Dictionary already contains key.");
            this._BaseDictionary[item.Key] = item.Value;
            this._BaseCollection.Add(item);
            this._IndexDictionary[item.Key] = this._BaseCollection.Count - 1;
            this.Subscribe(item);
            this.OnPropertyChanged("Count");
            this.OnPropertyChanged("Item[]");
            this.OnCollectionChanged(new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Add,
                                                                          item,
                                                                          this._IndexDictionary[item.Key]));
        }

        /// <summary>
        ///   Checks to see whether the item is in the dictionary.
        /// </summary>
        /// <param name = "item">The item to search for.</param>
        /// <returns>true if the item is in the dictionary.  false otherwise.</returns>
        public bool Contains(ObservableKeyValuePair<TKey, TValue> item)
        {
            return this._BaseCollection.Contains(item);
        }

        /// <summary>
        ///   Copies the dictionary into an array.
        /// </summary>
        /// <param name = "array">The array to copy into.</param>
        /// <param name = "arrayIndex">The index in the array where copying should begin.</param>
        public void CopyTo(ObservableKeyValuePair<TKey, TValue>[] array, int arrayIndex)
        {
            this._BaseCollection.CopyTo(array, arrayIndex);
        }

        /// <summary>
        ///   Removes an item from the dictionary.
        /// </summary>
        /// <param name = "item">The item to remove.</param>
        /// <returns>true if the item was removed.  false otherwise.</returns>
        public bool Remove(ObservableKeyValuePair<TKey, TValue> item)
        {
            if (!this.Contains(item))
                return false;
            int index = this._IndexDictionary[item.Key];
            this._BaseCollection.RemoveAt(index);
            for (int x = index; x < this._BaseCollection.Count; x++)
                this._IndexDictionary[this._BaseCollection[x].Key] = x;
            this._BaseDictionary.Remove(item.Key);
            this._IndexDictionary.Remove(item.Key);
            this.Unsubscribe(item);
            this.OnPropertyChanged("Count");
            this.OnPropertyChanged("Item[]");
            this.OnCollectionChanged(new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Remove,
                                                                          item,
                                                                          index));
            return true;
        }

        /// <summary>
        ///   Gets an enumerator over the values in the dictionary.
        /// </summary>
        /// <returns>The enumerator over the ObservableDictionary.</returns>
        public IEnumerator<ObservableKeyValuePair<TKey, TValue>> GetEnumerator()
        {
            return this._BaseCollection.GetEnumerator();
        }

        #endregion

        #region IDictionary Members

        bool IDictionary.IsFixedSize
        {
            get { return ((IDictionary)this._BaseDictionary).IsFixedSize; }
        }

        ICollection IDictionary.Keys
        {
            get { return ((IDictionary)this._BaseDictionary).Keys; }
        }

        ICollection IDictionary.Values
        {
            get { return ((IDictionary)this._BaseDictionary).Values; }
        }

        object IDictionary.this[object key]
        {
            get { return this[(TKey)key]; }
            set { this[(TKey)key] = (TValue)value; }
        }

        int ICollection.Count
        {
            get { return this.Count; }
        }

        bool ICollection.IsSynchronized
        {
            get { return ((IDictionary)this._BaseDictionary).IsSynchronized; }
        }

        object ICollection.SyncRoot
        {
            get { return ((IDictionary)this._BaseDictionary).SyncRoot; }
        }

        void IDictionary.Add(object key, object value)
        {
            this.Add((TKey)key, (TValue)value);
        }

        bool IDictionary.Contains(object key)
        {
            return this.ContainsKey((TKey)key);
        }

        IDictionaryEnumerator IDictionary.GetEnumerator()
        {
            return this._BaseDictionary.GetEnumerator();
        }

        void IDictionary.Remove(object key)
        {
            this.Remove((TKey)key);
        }

        void ICollection.CopyTo(Array array, int index)
        {
            ((IDictionary)this._BaseDictionary).CopyTo(array, index);
        }

        #endregion

        #region IDictionary<TKey,TValue> Members

        /// <summary>
        ///   Gets the number of items in the dictionary.
        /// </summary>
        public int Count
        {
            get { return this._BaseCollection.Count; }
        }

        /// <summary>
        ///   Gets whether the dictionary is read-only.
        /// </summary>
        public bool IsReadOnly
        {
            get { return false; }
        }

        /// <summary>
        ///   Gets the set of keys in the dictionary.
        /// </summary>
        public ICollection<TKey> Keys
        {
            get { return this._BaseDictionary.Keys; }
        }

        /// <summary>
        ///   Gets the set of values in the dictionary.
        /// </summary>
        public ICollection<TValue> Values
        {
            get { return this._BaseDictionary.Values; }
        }

        /// <summary>
        ///   Gets or sets the value for a given key.
        /// </summary>
        /// <param name = "key">The key for the item in the dictionary.</param>
        /// <returns>The value corresponding to the given key.</returns>
        public virtual TValue this[TKey key]
        {
            get { return this._BaseDictionary[key]; }
            set
            {
                if (!this.ContainsKey(key))
                    this.Add(key, value);
                else
                {
                    this._BaseDictionary[key] = value;
                    this._BaseCollection[this._IndexDictionary[key]].Value = value;
                }
            }
        }

        /// <summary>
        ///   Clears the dictionary.
        /// </summary>
        public void Clear()
        {
            foreach (var item in this._BaseCollection)
                this.Unsubscribe(item);
            this._BaseDictionary.Clear();
            this._BaseCollection.Clear();
            this._IndexDictionary.Clear();
            this.OnPropertyChanged("Count");
            this.OnPropertyChanged("Item[]");
            this.OnCollectionChanged(new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Reset));
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return ((IEnumerable<ObservableKeyValuePair<TKey, TValue>>)this).GetEnumerator();
        }

        /// <summary>
        ///   Adds an item to the dictionary.
        /// </summary>
        /// <param name = "key">The key to add.</param>
        /// <param name = "value">The value to associate with the key.</param>
        public void Add(TKey key, TValue value)
        {
            this.Add(new ObservableKeyValuePair<TKey, TValue>(key) { Value = value });
        }

        /// <summary>
        ///   Checks whether the dictionary contains the given key.
        /// </summary>
        /// <param name = "key">The key to search for.</param>
        /// <returns>true if the dictionary contains the given key.  false otherwise.</returns>
        public bool ContainsKey(TKey key)
        {
            return this._BaseDictionary.ContainsKey(key);
        }

        /// <summary>
        ///   Removes an item from the dictionary.
        /// </summary>
        /// <param name = "key">The key of the item to remove.</param>
        /// <returns>true if the item was removed.  false otherwise.</returns>
        public bool Remove(TKey key)
        {
            if (!this.ContainsKey(key))
                return false;
            int index = this._IndexDictionary[key];
            var value = this._BaseCollection[index];
            this._BaseCollection.RemoveAt(index);
            for (int x = index; x < this._BaseCollection.Count; x++)
                this._IndexDictionary[this._BaseCollection[x].Key] = x;
            this._BaseDictionary.Remove(key);
            this._IndexDictionary.Remove(key);
            this.OnPropertyChanged("Count");
            this.OnPropertyChanged("Item[]");
            this.OnCollectionChanged(new NotifyCollectionChangedEventArgs(NotifyCollectionChangedAction.Remove,
                                                                          value,
                                                                          index));
            return true;
        }

        /// <summary>
        ///   Attempts to get the value for the given key.
        /// </summary>
        /// <param name = "key">The key to look up.</param>
        /// <param name = "value">An output parameter set to the value corresponding to the given key.</param>
        /// <returns>true if the value was successfully retrieved.  false otherwise.</returns>
        public bool TryGetValue(TKey key, out TValue value)
        {
            if (!this.ContainsKey(key))
            {
                value = default(TValue);
                return false;
            }
            value = this._BaseDictionary[key];
            return true;
        }

        void ICollection<KeyValuePair<TKey, TValue>>.Add(KeyValuePair<TKey, TValue> item)
        {
            this.Add(new ObservableKeyValuePair<TKey, TValue>(item.Key) { Value = item.Value });
        }

        bool ICollection<KeyValuePair<TKey, TValue>>.Contains(KeyValuePair<TKey, TValue> item)
        {
            return ((ICollection<KeyValuePair<TKey, TValue>>)this._BaseDictionary).Contains(item);
        }

        void ICollection<KeyValuePair<TKey, TValue>>.CopyTo(KeyValuePair<TKey, TValue>[] array, int arrayIndex)
        {
            ((ICollection<KeyValuePair<TKey, TValue>>)this._BaseDictionary).CopyTo(array, arrayIndex);
        }

        bool ICollection<KeyValuePair<TKey, TValue>>.Remove(KeyValuePair<TKey, TValue> item)
        {
            if (!this.ContainsKey(item.Key))
                return false;
            TValue val = this[item.Key];
            return Equals(val, item.Value) && Remove(item.Key);
        }

        IEnumerator<KeyValuePair<TKey, TValue>> IEnumerable<KeyValuePair<TKey, TValue>>.GetEnumerator()
        {
            return this._BaseDictionary.GetEnumerator();
        }

        #endregion

        #region INotifyCollectionChanged Members

        /// <summary>
        ///   An event raised when the collection changes.
        /// </summary>
        public event NotifyCollectionChangedEventHandler CollectionChanged;

        #endregion

        #region INotifyPropertyChanged Members

        /// <summary>
        ///   An event raised when a property changes.
        /// </summary>
        public event PropertyChangedEventHandler PropertyChanged;

        #endregion
    }
}