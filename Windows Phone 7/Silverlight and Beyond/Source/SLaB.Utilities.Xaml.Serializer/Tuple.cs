
namespace System
{
    /// <summary>
    /// Represents a pair of items.
    /// </summary>
    /// <typeparam name="T1">The type of item1.</typeparam>
    /// <typeparam name="T2">The type of item2.</typeparam>
    public class Tuple<T1, T2>
    {

        /// <summary>
        /// Initializes a new instance of the <see cref="Tuple&lt;T1, T2&gt;"/> class.
        /// </summary>
        /// <param name="item1">Item1.</param>
        /// <param name="item2">Item2.</param>
        public Tuple(T1 item1, T2 item2)
        {
            Item1 = item1;
            Item2 = item2;
        }



        /// <summary>
        /// Gets or sets item1.
        /// </summary>
        /// <value>Item1.</value>
        public T1 Item1 { get; private set; }

        /// <summary>
        /// Gets or sets item2.
        /// </summary>
        /// <value>Item2.</value>
        public T2 Item2 { get; private set; }




        /// <summary>
        /// Determines whether the specified <see cref="System.Object"/> is equal to this instance.
        /// </summary>
        /// <param name="obj">The <see cref="System.Object"/> to compare with this instance.</param>
        /// <returns>
        /// 	<c>true</c> if the specified <see cref="System.Object"/> is equal to this instance; otherwise, <c>false</c>.
        /// </returns>
        public override bool Equals(object obj)
        {
            var other = (Tuple<T1, T2>)obj;
            return Item1.Equals(other.Item1) && Item2.Equals(other.Item2);
        }

        /// <summary>
        /// Returns a hash code for this instance.
        /// </summary>
        /// <returns>
        /// A hash code for this instance, suitable for use in hashing algorithms and data structures like a hash table. 
        /// </returns>
        public override int GetHashCode()
        {
            return CombineHashCodes(Item1.GetHashCode(), Item2.GetHashCode());
        }

        private static int CombineHashCodes(int h1, int h2)
        {
            return (((h1 << 5) + h1) ^ h2);
        }
    }
    /// <summary>
    /// Represents a triplet of items
    /// </summary>
    /// <typeparam name="T1">The type of item1.</typeparam>
    /// <typeparam name="T2">The type of item2.</typeparam>
    /// <typeparam name="T3">The type of item3.</typeparam>
    public class Tuple<T1, T2, T3>
    {

        /// <summary>
        /// Initializes a new instance of the <see cref="Tuple&lt;T1, T2, T3&gt;"/> class.
        /// </summary>
        /// <param name="item1">The item1.</param>
        /// <param name="item2">The item2.</param>
        /// <param name="item3">The item3.</param>
        public Tuple(T1 item1, T2 item2, T3 item3)
        {
            Item1 = item1;
            Item2 = item2;
            Item3 = item3;
        }



        /// <summary>
        /// Gets or sets the item1.
        /// </summary>
        /// <value>The item1.</value>
        public T1 Item1 { get; private set; }

        /// <summary>
        /// Gets or sets the item2.
        /// </summary>
        /// <value>The item2.</value>
        public T2 Item2 { get; private set; }

        /// <summary>
        /// Gets or sets the item3.
        /// </summary>
        /// <value>The item3.</value>
        public T3 Item3 { get; private set; }




        /// <summary>
        /// Determines whether the specified <see cref="System.Object"/> is equal to this instance.
        /// </summary>
        /// <param name="obj">The <see cref="System.Object"/> to compare with this instance.</param>
        /// <returns>
        /// 	<c>true</c> if the specified <see cref="System.Object"/> is equal to this instance; otherwise, <c>false</c>.
        /// </returns>
        public override bool Equals(object obj)
        {
            var other = (Tuple<T1, T2, T3>)obj;
            return Item1.Equals(other.Item1) && Item2.Equals(other.Item2) && Item3.Equals(other.Item3);
        }

        /// <summary>
        /// Returns a hash code for this instance.
        /// </summary>
        /// <returns>
        /// A hash code for this instance, suitable for use in hashing algorithms and data structures like a hash table. 
        /// </returns>
        public override int GetHashCode()
        {
            return CombineHashCodes(CombineHashCodes(Item1.GetHashCode(), Item2.GetHashCode()), Item3.GetHashCode());
        }

        private static int CombineHashCodes(int h1, int h2)
        {
            return (((h1 << 5) + h1) ^ h2);
        }
    }
}
