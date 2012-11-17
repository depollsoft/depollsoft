#region Using Directives

using System;

#endregion

namespace SLaB.Utilities
{
    /// <summary>
    ///   Provides utility functions for working with Uris.
    /// </summary>
    public static class UriUtilities
    {


        /// <summary>
        ///   Does a simplified check for equality, ignoring case, user/password, and fragment.
        /// </summary>
        /// <param name = "uri1">The first Uri to compare.</param>
        /// <param name = "uri2">The second Uri to compare.</param>
        /// <returns>true if the first Uri was equal to the second Uri.  false otherwise.</returns>
        public static bool Equals(this Uri uri1, Uri uri2)
        {
            if (ReferenceEquals(uri1, uri2))
                return true;
            if (uri1 == null || uri2 == null)
                return false;
            if (uri1.IsAbsoluteUri != uri2.IsAbsoluteUri)
                return false;
            if (!uri1.IsAbsoluteUri)
                return uri1.OriginalString.Equals(uri2.OriginalString, StringComparison.InvariantCultureIgnoreCase);
            if (!uri1.Scheme.Equals(uri2.Scheme, StringComparison.InvariantCultureIgnoreCase))
                return false;
            if (!uri1.AbsolutePath.Equals(uri2.AbsolutePath, StringComparison.InvariantCultureIgnoreCase))
                return false;
            if (!uri1.Host.Equals(uri2.Host, StringComparison.InvariantCultureIgnoreCase))
                return false;
            try
            {
                if (uri1.Port != uri2.Port)
                    return false;
            }
            catch
            {
            }
            if (!uri1.Query.Equals(uri2.Query, StringComparison.InvariantCultureIgnoreCase))
                return false;
            return true;
        }
    }
}