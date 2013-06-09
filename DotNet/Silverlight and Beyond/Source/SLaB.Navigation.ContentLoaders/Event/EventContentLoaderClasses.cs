#region Using Directives

using System;
using System.Threading;
using System.Windows.Navigation;

#endregion

namespace SLaB.Navigation.ContentLoaders.Event
{
    /// <summary>
    ///   A delegate to be called for synchronous loading of content.
    /// </summary>
    /// <param name = "targetUri">The Uri to load.</param>
    /// <param name = "currentUri">The currently loaded Uri.</param>
    /// <returns>LoadResult containing the loaded content or a redirect.</returns>
    public delegate LoadResult SynchronousEventLoad(Uri targetUri, Uri currentUri);

    /// <summary>
    ///   A delegate to be called to determine whether the INavigationContentLoader can load content for the given
    ///   <paramref name = "targetUri" /> and <paramref name = "currentUri" />.
    /// </summary>
    /// <param name = "targetUri">The Uri to load.</param>
    /// <param name = "currentUri">The currently loaded Uri.</param>
    /// <returns>true if the Uri can be loaded, false otherwise.</returns>
    public delegate bool EventCanLoad(Uri targetUri, Uri currentUri);

    internal class EventContentLoaderAsyncResult : IAsyncResult
    {

        public EventContentLoaderAsyncResult(object asyncState)
        {
            this.AsyncState = asyncState;
            this.AsyncWaitHandle = new AutoResetEvent(false);
        }



        public LoadResult Result { get; internal set; }




        #region IAsyncResult Members

        public object AsyncState { get; private set; }

        public WaitHandle AsyncWaitHandle { get; private set; }

        public bool CompletedSynchronously { get; internal set; }

        public bool IsCompleted { get; internal set; }

        #endregion
    }
}