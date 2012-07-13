#region Using Directives

using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    /// <summary>
    ///   Provides a collection of operators for working with INotifyCollectionChanged collections.
    /// </summary>
    public static class ChangeLinq
    {
        /// <summary>
        ///   Produces an IChangeValue that is the aggregate of the items in a collection.
        /// </summary>
        /// <typeparam name = "TIn">The input collection type.</typeparam>
        /// <typeparam name = "TOut">The type of the output from aggregating the items.</typeparam>
        /// <param name = "source">The collection of items being aggregated.</param>
        /// <param name = "aggregator">A function that aggregates items onto the value so far.</param>
        /// <param name = "initialValue">A seed value for aggregation.</param>
        /// <returns></returns>
        public static IChangeValue<TOut> Aggregate<TIn, TOut>(this IChangeLinq<TIn> source,
                                                              Func<TOut, TIn, TOut> aggregator,
                                                              TOut initialValue = default(TOut))
        {
            return new Aggregator<TIn, TOut>(source, aggregator, initialValue);
        }

        /// <summary>
        ///   Converts an IEnumerable into an IChangeLinq&lt;object&gt;.
        /// </summary>
        /// <param name = "source"></param>
        /// <returns></returns>
        public static IChangeLinq<object> AsChangeLinq(this IEnumerable source)
        {
            return source.Cast<object>().AsChangeLinq();
        }

        /// <summary>
        ///   Creates an IChangeLinq from any IEnumerable source.  If the source is INotifyCollectionChanged, the IChangeLinq will
        ///   remain in sync with the source.
        /// </summary>
        /// <typeparam name = "T">The type of the IEnumerable.</typeparam>
        /// <param name = "source">The source collection.</param>
        /// <returns>An IChangeLinq that wraps the source.</returns>
        public static IChangeLinq<T> AsChangeLinq<T>(this IEnumerable<T> source)
        {
            if (source is IChangeLinq<T>)
                return (IChangeLinq<T>)source;
            return new SelectObservableCollection<T, T>(source, val => val);
        }

        /// <summary>
        ///   Converts an IChangeValue that contains an IEnumerable into an IChangeLinq that will notify both when the collection and
        ///   when the value changes.
        /// </summary>
        /// <typeparam name = "T">The input collection type.</typeparam>
        /// <param name = "source">The input IChangeValue.</param>
        /// <returns>An IChangeLinq over the IChangeValue.</returns>
        public static IChangeLinq<T> AsChangeLinq<T>(this IChangeValue<IEnumerable<T>> source)
        {
            return new ChangeValueWrapper<T>(source);
        }

        /// <summary>
        ///   Creates an IChangeLinq that concatenates two collections.  INotifyCollectionChanged notifications across both collections
        ///   will be correctly maintained.
        /// </summary>
        /// <typeparam name = "T">The input collection type.</typeparam>
        /// <param name = "first">The left-hand-side of the concat operation.</param>
        /// <param name = "second">The right-hand-side of the concat operation.</param>
        /// <returns>An IChangeLinq that contains the concatenated items.</returns>
        public static IChangeLinq<T> Concat<T>(this IChangeLinq<T> first, IEnumerable<T> second)
        {
            return new ConcatObservableCollection<T>(first, second);
        }

        /// <summary>
        ///   Creates an IChangeLinq that maintains a select operation over the source.  INotifyCollectionChanged notifications
        ///   will be correctly maintained.
        /// </summary>
        /// <typeparam name = "TIn">The input collection type.</typeparam>
        /// <typeparam name = "TOut">The output collection type.</typeparam>
        /// <param name = "source">The collection to select over.</param>
        /// <param name = "selector">A function converting a TIn into a TOut.</param>
        /// <returns>An IChangeLinq that maps items into the source using the selector.</returns>
        public static IChangeLinq<TOut> Select<TIn, TOut>(this IChangeLinq<TIn> source, Func<TIn, TOut> selector)
        {
            return new SelectObservableCollection<TIn, TOut>(source, selector);
        }

        /// <summary>
        ///   Converts an enumerable to a string, calling ToString on each child item.
        /// </summary>
        /// <typeparam name = "T">The type of the enumerable.</typeparam>
        /// <param name = "source">The collection being converted to a string.</param>
        /// <param name = "toString">A function that returns a string for a given item.</param>
        /// <param name = "delimiter">A delimiter to use between items.</param>
        /// <param name = "prefix">A prefix to be used to surround the collection.</param>
        /// <param name = "suffix">A suffix to be used to surround the collection.</param>
        /// <returns>The string representation of the source enumerable.</returns>
        public static string ToString<T>(this IEnumerable<T> source,
                                         Func<T, string> toString = null,
                                         string delimiter = ", ",
                                         string prefix = "{",
                                         string suffix = "}")
        {
            if (toString == null)
                toString = val => val.ToString();
            StringBuilder sb = new StringBuilder();
            sb.Append(prefix);
            foreach (var item in source)
            {
                sb.Append(toString(item));
                sb.Append(delimiter);
            }
            if (source.Any())
                sb.Remove(sb.Length - delimiter.Length, delimiter.Length);
            sb.Append(suffix);
            return sb.ToString();
        }

        /// <summary>
        ///   Produces an IChangeLinq full traversal of a tree of values.
        /// </summary>
        /// <typeparam name = "T">The input collection type.</typeparam>
        /// <param name = "source">The collection being traversed.</param>
        /// <param name = "selector">A function that takes an item and produces a list of child items.</param>
        /// <returns>A flattened list of items in the tree.</returns>
        public static IChangeLinq<T> Traverse<T>(this IChangeLinq<T> source, Func<T, IEnumerable<T>> selector)
        {
            if (!source.Any())
                return source;
            IChangeLinq<T> children =
                source.Aggregate((soFar, item) => soFar.Concat(selector(item)), new T[0].AsChangeLinq()).AsChangeLinq();
            IChangeLinq<T> traversedChildren = children.Traverse(selector);
            return source.Concat(traversedChildren);
        }

        /// <summary>
        ///   Creates an IChangeLinq that maintains a where operation over the source.  INotifyCollectionChanged notifications
        ///   will be correctly maintained.
        /// </summary>
        /// <typeparam name = "T">The input collection type.</typeparam>
        /// <param name = "source">The collection being filtered.</param>
        /// <param name = "predicate">A predicate indicating whether to filter the item.</param>
        /// <returns>An IChangeLinq that contains the filtered items.</returns>
        public static IChangeLinq<T> Where<T>(this IChangeLinq<T> source, Func<T, bool> predicate)
        {
            return new WhereObservableCollection<T>(source, predicate);
        }
    }
}