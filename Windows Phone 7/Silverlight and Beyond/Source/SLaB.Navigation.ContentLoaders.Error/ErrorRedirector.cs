#region Using Directives

using System;
using SLaB.Navigation.ContentLoaders.Utilities;

#endregion

namespace SLaB.Navigation.ContentLoaders.Error
{
    /// <summary>
    ///   An INavigationContentLoader that redirects to an error page rather than loading it in place.
    /// </summary>
    public class ErrorRedirector : ContentLoaderBase
    {


        /// <summary>
        ///   Creates an instance of a LoaderBase that will be used to handle loading.
        /// </summary>
        /// <returns>An instance of a LoaderBase.</returns>
        protected override LoaderBase CreateLoader()
        {
            return new Loader();
        }




        private class Loader : LoaderBase
        {
		#region Methods (2) 

		// Public Methods (2) 

            public override void Cancel()
            {
                return;
            }

            public override void Load(Uri targetUri, Uri currentUri)
            {
                Complete(targetUri);
            }

		#endregion Methods 
        }
    }
}