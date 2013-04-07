#region Using Directives

using System;
using System.Diagnostics.CodeAnalysis;
using System.Threading;
using System.Windows;
using System.Windows.Navigation;

#endregion

namespace SLaB.Navigation.ContentLoaders.Event
{
    /// <summary>
    ///   An INavigationContentLoader that raises events that can be handled using XAML markup
    ///   in order to load pages synchrnously.
    /// </summary>
    public sealed class SynchronousEventContentLoader : DependencyObject, INavigationContentLoader
    {


        /// <summary>
        ///   Event raised to determine whether the given Uri can be loaded.
        /// </summary>
        [SuppressMessage("Microsoft.Design", "CA1009:DeclareEventHandlersCorrectly")]
        public event EventCanLoad CanLoad;

        /// <summary>
        ///   Event raised when loading from a Uri.
        /// </summary>
        [SuppressMessage("Microsoft.Design", "CA1009:DeclareEventHandlersCorrectly")]
        public event SynchronousEventLoad Load;




        #region INavigationContentLoader Members

        IAsyncResult INavigationContentLoader.BeginLoad(Uri targetUri,
                                                        Uri currentUri,
                                                        AsyncCallback userCallback,
                                                        object asyncState)
        {
            var load = this.Load;
            var result = new EventContentLoaderAsyncResult(asyncState);
            if (load != null)
                result.Result = load(targetUri, currentUri);
            result.CompletedSynchronously = true;
            result.IsCompleted = true;
            ((AutoResetEvent)result.AsyncWaitHandle).Set();
            userCallback(result);
            return result;
        }

        bool INavigationContentLoader.CanLoad(Uri targetUri, Uri currentUri)
        {
            var canLoad = this.CanLoad;
            if (canLoad != null)
                return canLoad(targetUri, currentUri);
            return this.Load != null;
        }

        void INavigationContentLoader.CancelLoad(IAsyncResult asyncResult)
        {
        }

        LoadResult INavigationContentLoader.EndLoad(IAsyncResult asyncResult)
        {
            return ((EventContentLoaderAsyncResult)asyncResult).Result;
        }

        #endregion
    }
}