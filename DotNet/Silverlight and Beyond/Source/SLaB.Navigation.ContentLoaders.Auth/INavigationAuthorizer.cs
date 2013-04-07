#region Using Directives

using System;
using System.Security.Principal;

#endregion

namespace SLaB.Navigation.ContentLoaders.Auth
{
    /// <summary>
    ///   INavigationAuthorizor is used by the AuthContentLoader to check to see whether navigation should be allowed based upon
    ///   user credentials.
    /// </summary>
    public interface INavigationAuthorizer
    {

        /// <summary>
        ///   Checks whether the principal has sufficient authorization to access the Uri being loaded by the AuthContentLoader.
        ///   If the principal is authorized, this method should simply return.  Otherwise, it should throw.
        /// </summary>
        /// <param name = "principal">The user credentials against which to check.</param>
        /// <param name = "targetUri">The Uri being loaded.</param>
        /// <param name = "currentUri">The current Uri from which the new Uri is being loaded.</param>
        void CheckAuthorization(IPrincipal principal, Uri targetUri, Uri currentUri);
    }
}