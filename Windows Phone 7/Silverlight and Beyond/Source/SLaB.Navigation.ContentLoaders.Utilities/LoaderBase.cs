#region Using Directives

using System;
using System.Windows;

#endregion

namespace SLaB.Navigation.ContentLoaders.Utilities
{
    /// <summary>
    ///   A base class for loaders that interface with the ContentLoaderBase to simplify creation
    ///   of an INavigationContentLoader.
    /// </summary>
    public abstract class LoaderBase : DependencyObject
    {

        private bool _Cancelled;



        internal ContentLoaderBase ContentLoader { get; set; }

        internal ContentLoaderBaseAsyncResult Result { get; set; }




        /// <summary>
        ///   Requests that the load operation be cancelled.
        /// </summary>
        public abstract void Cancel();

        /// <summary>
        ///   Begins a Load operation.
        /// </summary>
        /// <param name = "targetUri">The Uri being loaded.</param>
        /// <param name = "currentUri">The current Uri.</param>
        public abstract void Load(Uri targetUri, Uri currentUri);

        /// <summary>
        ///   Completes loading, creating the instance of the UserControl or Page on the UI thread.
        /// </summary>
        /// <param name = "pageCreator">The function that instantiates the UserControl or Page.</param>
        protected void Complete(Func<object> pageCreator)
        {
            if (this._Cancelled)
                return;
            Deployment.Current.Dispatcher.BeginInvoke(() => Complete(pageCreator()));
        }

        /// <summary>
        ///   Completes loading, creating the instance of the Uri on the UI thread.
        /// </summary>
        /// <param name = "uriCreator">The function that creates the Uri.</param>
        protected void Complete(Func<Uri> uriCreator)
        {
            if (this._Cancelled)
                return;
            Deployment.Current.Dispatcher.BeginInvoke(() => Complete(uriCreator()));
        }

        /// <summary>
        ///   Completes loading, returning the <paramref name = "page" /> passed in.
        /// </summary>
        /// <param name = "page">The UserControl or Page that was loaded.</param>
        protected void Complete(object page)
        {
            if (this._Cancelled)
                return;
            this.Result.Page = page;
            this.ContentLoader.Complete(this.Result);
        }

        /// <summary>
        ///   Completes loading, returning the <paramref name = "redirectUri" /> passed in.
        /// </summary>
        /// <param name = "redirectUri">The Uri that the navigation engine should redirect to.</param>
        protected void Complete(Uri redirectUri)
        {
            if (this._Cancelled)
                return;
            this.Result.RedirectUri = redirectUri;
            this.ContentLoader.Complete(this.Result);
        }

        /// <summary>
        ///   Ends loading with an error, delaying throwing the error until EndLoad() is called on
        ///   the INavigationContentLoader.
        /// </summary>
        /// <param name = "error">The error that occurred.</param>
        protected void Error(Exception error)
        {
            this.Result.Error = error;
            this.ContentLoader.Complete(this.Result);
        }

        internal void CancelInternal()
        {
            this._Cancelled = true;
            this.Cancel();
        }
    }
}