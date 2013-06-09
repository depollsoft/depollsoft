#region Using Directives

using System;
using System.Collections.Generic;
using System.Linq;
using System.Windows.Markup;
using System.Windows.Navigation;

#endregion

namespace SLaB.Navigation.Utilities
{
    /// <summary>
    ///   Converts a uniform resource identifier (Uri) into a new Uri based on the rules of a matching object
    ///   specified in a collection of mapping objects.
    /// </summary>
    [ContentProperty("UriMappings")]
    public class UriMapper : UriMapperBase
    {

        /// <summary>
        ///   Constructs a new UriMapper.
        /// </summary>
        public UriMapper()
        {
            this.UriMappings = new List<IUriMapping>();
        }



        /// <summary>
        ///   Gets a collection of objects that are used to convert a uniform resource identifier (Uri) into a new Uri.
        /// </summary>
        public List<IUriMapping> UriMappings { get; private set; }




        /// <summary>
        ///   Converts a specified uniform resource identifier (Uri) into a new Uri based on the rules of a matching
        ///   object in the System.Windows.Navigation.UriMapper.UriMappings collection.
        /// </summary>
        /// <param name = "uri">Original Uri value to be converted to a new Uri.</param>
        /// <returns>A Uri to use for handling the request instead of the value of the uri parameter. If no object in the
        ///   UriMappings collection matches uri, the original value for uri is returned.</returns>
        public override Uri MapUri(Uri uri)
        {
            return (from mapping in this.UriMappings
                    let result = mapping.MapUri(uri)
                    where result != null
                    select result).FirstOrDefault() ?? uri;
        }
    }
}