#region Using Directives

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;
using SLaB.Navigation.Utilities;

#endregion

namespace SLaB.Navigation.ContentLoaders.Xap
{
    /// <summary>
    ///   A UriMapping to simplify creating Uris for the XapContentLoader.  This allows developers to avoid using
    ///   pack Uris altogether.
    /// </summary>
    public class PackUriMapping : IUriMapping
    {

        private string _MappedPath;
        private Regex _Matcher;
        private Dictionary<string, string> _Seen;
        private Uri _Uri;
        /// <summary>
        ///   Token that can be used in patterns in lieu of setting the AssemblyName property.
        /// </summary>
        public static readonly string AssemblyNameToken = "assemblyname";
        private static readonly Regex PatternRegex = new Regex(@"\\\{(?<name>.*?)\}");



        static PackUriMapping()
        {
            PackUriParser.Initialize();
        }



        /// <summary>
        ///   The name of the assembly within the Xap where the page can be found.
        /// </summary>
        public string AssemblyName { get; set; }

        /// <summary>
        ///   Gets or sets the resulting path after running the mapping.  This is equivalent to the UriMapping.MappedUri property,
        ///   but only represents the path at the end of the pack Uri.
        /// </summary>
        public string MappedPath
        {
            get { return this._MappedPath; }
            set
            {
                this._MappedPath = value;
                this.UpdatePattern();
            }
        }

        /// <summary>
        ///   Gets or sets the pattern to match when determining if the requested uniform resource identifier (Uri) is converted to a mapped Uri.
        /// </summary>
        public Uri Uri
        {
            get { return this._Uri; }
            set
            {
                this._Uri = value;
                this.UpdatePattern();
            }
        }

        /// <summary>
        ///   Uri to the Xap in which the page being navigated to can be found.  This property must be set in order
        ///   for mapping to occur.
        /// </summary>
        public Uri XapLocation { get; set; }




        private void UpdatePattern()
        {
            if (this.MappedPath == null || this.Uri == null)
                return;
            this._Seen = new Dictionary<string, string>();
            string pattern = this.Uri.OriginalString;
            pattern = Regex.Escape(pattern);
            pattern = PatternRegex.Replace(pattern,
                                           match =>
                                               {
                                                   string name = match.Groups["name"].Value;
                                                   if (this._Seen.ContainsKey(name))
                                                       return string.Format("${{{0}}}", name);
                                                   string final = string.Format("(?<{0}>.*?)", name);
                                                   this._Seen[name] = string.Format(@"\{{{0}\}}", name);
                                                   return final;
                                               });
            this._Matcher = new Regex("^" + pattern + "$", RegexOptions.IgnoreCase);
        }




        #region IUriMapping Members

        /// <summary>
        ///   Converts the specified uniform resource identifier (Uri) to a new Uri, if the specified Uri matches the defined template for converting.
        /// </summary>
        /// <param name = "uri">The Uri to convert.</param>
        /// <returns>The Uri that has been converted or null if the Uri cannot be converted.</returns>
        public Uri MapUri(Uri uri)
        {
            Match matches = this._Matcher.Match(uri.OriginalString);
            if (!matches.Success)
                return null;
            string path = this._Seen.Keys.Aggregate(this.MappedPath,
                                                    (current, key) =>
                                                    new Regex(this._Seen[key], RegexOptions.IgnoreCase).Replace(
                                                        current, matches.Groups[key].Value));
            return PackUriHelper.BuildPackUri(this.XapLocation ?? new Uri("application:///"),
                                              path,
                                              this.AssemblyName ?? matches.Groups[AssemblyNameToken].Value);
        }

        #endregion
    }
}