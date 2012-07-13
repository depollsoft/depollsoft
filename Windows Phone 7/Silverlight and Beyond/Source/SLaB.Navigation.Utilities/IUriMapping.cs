#region Using Directives

using System;

#endregion

namespace SLaB.Navigation.Utilities
{
    /// <summary>
    ///   A mapping for use by a UriMapper.  Maps an input Uri to an output Uri.
    /// </summary>
    public interface IUriMapping
    {

        /// <summary>
        ///   Maps one Uri to another.
        /// </summary>
        /// <param name = "uri">The input Uri.</param>
        /// <returns>The mapped Uri.</returns>
        Uri MapUri(Uri uri);
    }
}