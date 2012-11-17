namespace SLaB.Utilities.Xaml.Collections
{
    /// <summary>
    ///   An ObservableDictionary that can be bound to because it has an explicit string indexer and does not throw when
    ///   a missing key is indexed.
    /// </summary>
    /// <typeparam name = "TValue">The type of the Values in the dictionary.</typeparam>
    public class BindableDictionary<TValue> : ObservableDictionary<string, TValue>
    {

        /// <summary>
        ///   Gets or sets the value for the given key.
        /// </summary>
        /// <param name = "key">The key for the dictionary.</param>
        /// <returns>The value for the given key.</returns>
        public override TValue this[string key]
        {
            get { return !this.ContainsKey(key) ? default(TValue) : base[key]; }
            set { base[key] = value; }
        }
    }

    /// <summary>
    ///   An ObservableDictionary that can be declared in XAML.
    /// </summary>
    public class BindableDictionary : BindableDictionary<object>
    {

}
}