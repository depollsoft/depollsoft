#region Using Directives

using System.Collections.Generic;
using System.Collections.Specialized;

#endregion

namespace SLaB.Utilities.ChangeLinq
{
    /// <summary>
    ///   Represents an enumerable INotifyCollectionChanged.  Generally, this is an INotifyCollectionChanged collection that
    ///   wraps some operation over an existing INotifyCollectionChanged.
    /// </summary>
    /// <typeparam name = "T"></typeparam>
    public interface IChangeLinq<T> : INotifyCollectionChanged, IEnumerable<T>
    {

    }
}