
namespace System.Collections.Generic
{
    /// <summary>
    /// Represents an unordered set of items.
    /// </summary>
    /// <typeparam name="T">The type of item contained in the set.</typeparam>
    public interface ISet<T> : ICollection<T>, IEnumerable<T>, IEnumerable
    {

        /// <summary>
        /// Adds the specified item.
        /// </summary>
        /// <param name="item">The item.</param>
        /// <returns></returns>
        new bool Add(T item);

        /// <summary>
        /// Provides an exclusive-or combination of the sets.
        /// </summary>
        /// <param name="other">The other.</param>
        void ExceptWith(IEnumerable<T> other);

        /// <summary>
        /// Provides an intersection of the sets.
        /// </summary>
        /// <param name="other">The other.</param>
        void IntersectWith(IEnumerable<T> other);

        /// <summary>
        /// Determines whether the set is a proper subset of the other collection..
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>
        /// 	<c>true</c> if the set is a proper subset of the other collection; otherwise, <c>false</c>.
        /// </returns>
        bool IsProperSubsetOf(IEnumerable<T> other);

        /// <summary>
        /// Determines whether the set is a proper superset of the other collection.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>
        /// 	<c>true</c> if the set is a proper superset of the other collection; otherwise, <c>false</c>.
        /// </returns>
        bool IsProperSupersetOf(IEnumerable<T> other);

        /// <summary>
        /// Determines whether the set is a subset of the other collection.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>
        /// 	<c>true</c> if the set is a subset of the other collection; otherwise, <c>false</c>.
        /// </returns>
        bool IsSubsetOf(IEnumerable<T> other);

        /// <summary>
        /// Determines whether the set is a superset of the other collection.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>
        /// 	<c>true</c> if the set is a superset of the other collection; otherwise, <c>false</c>.
        /// </returns>
        bool IsSupersetOf(System.Collections.Generic.IEnumerable<T> other);

        /// <summary>
        /// Determines whether the set overlaps with the other collection.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>Whether the set overlaps with the other collection.</returns>
        bool Overlaps(IEnumerable<T> other);

        /// <summary>
        /// Determines whether the sets are equal.
        /// </summary>
        /// <param name="other">The other.</param>
        /// <returns>Whether the sets are equal.</returns>
        bool SetEquals(IEnumerable<T> other);

        /// <summary>
        /// Performs a symmetric exclusive or with the other set.
        /// </summary>
        /// <param name="other">The other.</param>
        void SymmetricExceptWith(IEnumerable<T> other);

        /// <summary>
        /// Takes the union of the two sets.
        /// </summary>
        /// <param name="other">The other.</param>
        void UnionWith(IEnumerable<T> other);
    }
}
