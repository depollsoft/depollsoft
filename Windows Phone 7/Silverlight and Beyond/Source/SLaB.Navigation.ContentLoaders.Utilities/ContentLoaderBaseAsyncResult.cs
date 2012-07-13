#region Using Directives

using System;
using System.Threading;

#endregion

namespace SLaB.Navigation.ContentLoaders.Utilities
{
    internal class ContentLoaderBaseAsyncResult : IAsyncResult
    {

        public ContentLoaderBaseAsyncResult(object asyncState, LoaderBase loader, AsyncCallback callback)
        {
            this.AsyncState = asyncState;
            this.Loader = loader;
            this.Lock = new object();
            this.Callback = callback;
            this.AsyncWaitHandle = new AutoResetEvent(false);
        }



        internal bool BeginLoadCompleted { get; set; }

        internal AsyncCallback Callback { get; private set; }

        internal Exception Error { get; set; }

        internal LoaderBase Loader { get; private set; }

        internal object Lock { get; private set; }

        internal object Page { get; set; }

        internal Uri RedirectUri { get; set; }




        public void Complete()
        {
            this.CompletedSynchronously = !this.BeginLoadCompleted;
            this.IsCompleted = true;
            (this.AsyncWaitHandle as AutoResetEvent).Set();
        }




        #region IAsyncResult Members

        public object AsyncState { get; private set; }

        public WaitHandle AsyncWaitHandle { get; private set; }

        public bool CompletedSynchronously { get; private set; }

        public bool IsCompleted { get; private set; }

        #endregion
    }
}