#region Using Directives

using System;

#endregion

namespace SLaB.Navigation.Utilities
{
    /// <summary>
    ///   Defines the pattern for converting a requested uniform resource identifier (Uri) into a new Uri.
    ///   This IUriMapping wraps the functionality of System.Windows.Navigation.UriMapping.
    /// </summary>
    public class UriMapping : IUriMapping
    {

        private readonly System.Windows.Navigation.UriMapping _InternalMapping;



        /// <summary>
        ///   Constructs a UriMapping.
        /// </summary>
        public UriMapping()
        {
            this._InternalMapping = new System.Windows.Navigation.UriMapping();
        }



        /// <summary>
        ///   Gets or sets the uniform resource identifier (Uri) that is navigated to instead of the originally requested Uri.
        /// </summary>
        public Uri MappedUri
        {
            get { return this._InternalMapping.MappedUri; }
            set { this._InternalMapping.MappedUri = value; }
        }

        /// <summary>
        ///   Gets or sets the pattern to match when determining if the requested uniform resource identifier (Uri) is converted to a mapped Uri.
        /// </summary>
        public Uri Uri
        {
            get { return this._InternalMapping.Uri; }
            set { this._InternalMapping.Uri = value; }
        }




        #region IUriMapping Members

        /// <summary>
        ///   Converts the specified uniform resource identifier (Uri) to a new Uri, if the specified Uri matches the defined template for converting.
        /// </summary>
        /// <param name = "uri">The Uri to convert.</param>
        /// <returns>The Uri that has been converted or null if the Uri cannot be converted.</returns>
        public Uri MapUri(Uri uri)
        {
            return this._InternalMapping.MapUri(uri);
        }

        #endregion
    }
}