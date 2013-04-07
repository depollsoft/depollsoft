#region Using Directives

using System;
using System.Text;
using System.Text.RegularExpressions;

#endregion

namespace SLaB.Navigation.ContentLoaders.Xap
{
    /// <summary>
    ///   A UriParser for pack Uris.
    /// </summary>
    public class PackUriParser : UriParser
    {

        private string _Authority;
        private string _Fragment;
        private static bool _IsInitialized;
        private string _Path;
        private string _Port;
        private string _QueryString;
        private int _RegisteredPort;
        private static readonly Regex UriRegex =
            new Regex(@"^pack://(?<authority>.*?)(?<path>/.*?)?(\?(?<querystring>.*?))?(#(?<fragment>.*?))?$");



        static PackUriParser()
        {
            Initialize();
        }




        /// <summary>
        ///   Registers the PackUriParser so that pack Uris can be created.  Call this method at least once in any application that will create
        ///   pack Uris.
        /// </summary>
        public static void Initialize()
        {
            if (!_IsInitialized)
            {
                Register(new PackUriParser(), "pack", -1);
                _IsInitialized = true;
            }
        }

        /// <summary>
        ///   Gets the components from a Uri.
        /// </summary>
        /// <param name = "uri">The System.Uri to parse.</param>
        /// <param name = "components">The System.UriComponents to retrieve from uri.</param>
        /// <param name = "format">One of the System.UriFormat values that controls how special characters are escaped.</param>
        /// <returns>A string that contains the components.</returns>
        /// <exception cref = "System.ArgumentOutOfRangeException">format is invalid- or -components is not a combination of valid System.UriComponents values.</exception>
        /// <exception cref = "System.InvalidOperationException">uri requires user-driven parsing- or -uri is not an absolute URI. Relative URIs cannot be used with this method.</exception>
        protected override string GetComponents(Uri uri, UriComponents components, UriFormat format)
        {
            switch (components)
            {
                case UriComponents.AbsoluteUri:
                    return uri.OriginalString;
                case UriComponents.Fragment:
                    return this._Fragment;
                case UriComponents.StrongAuthority:
                case UriComponents.Host:
                    return this._Authority;
                case UriComponents.Path:
                    return this._Path;
                case UriComponents.StrongPort:
                case UriComponents.Port:
                    return this._Port;
                case UriComponents.Query:
                    return this._QueryString;
                case UriComponents.Scheme:
                    return "pack";
                case UriComponents.UserInfo:
                    return "";
            }
            StringBuilder sb = new StringBuilder();
            if (components.HasFlag(UriComponents.Scheme))
                sb.Append(this.GetComponents(uri, UriComponents.Scheme, format) + "://");
            if (components.HasFlag(UriComponents.Host))
                sb.Append(this.GetComponents(uri, UriComponents.Host, format));
            if (components.HasFlag(UriComponents.Port))
                sb.Append(":" + this.GetComponents(uri, UriComponents.Port, format));
            if (components.HasFlag(UriComponents.KeepDelimiter) && components.HasFlag(UriComponents.Host) &&
                components.HasFlag(UriComponents.Path))
                sb.Append("/");
            if (components.HasFlag(UriComponents.Path))
                sb.Append(this.GetComponents(uri, UriComponents.Path, format));
            if (components.HasFlag(UriComponents.Path) && components.HasFlag(UriComponents.Query))
                sb.Append("?");
            if (components.HasFlag(UriComponents.Query))
                sb.Append(this.GetComponents(uri, UriComponents.Query, format));
            if (components.HasFlag(UriComponents.Fragment) && components.HasFlag(UriComponents.Path))
                sb.Append("#");
            if (components.HasFlag(UriComponents.Fragment))
                sb.Append(this.GetComponents(uri, UriComponents.Fragment, format));
            return sb.ToString();
        }

        /// <summary>
        ///   Initialize the state of the parser and validate the Uri.
        /// </summary>
        /// <param name = "uri">The System.Uri to validate.</param>
        /// <param name = "parsingError">Validation errors, if any.</param>
        protected override void InitializeAndValidate(Uri uri, out UriFormatException parsingError)
        {
            parsingError = null;
            Match m = UriRegex.Match(uri.OriginalString);
            if (!m.Success)
                parsingError = new UriFormatException("Uri did not match pack uri syntax");
            this._Authority = m.Groups["authority"].Value;
            this._Path = m.Groups["path"].Value;
            this._Port = this._RegisteredPort.ToString();
            this._QueryString = m.Groups["querystring"].Value;
            this._Fragment = m.Groups["fragment"].Value;
        }

        /// <summary>
        ///   Invoked by a System.Uri constructor to get a System.UriParser instance
        /// </summary>
        /// <returns>A System.UriParser for the constructed System.Uri.</returns>
        protected override UriParser OnNewUri()
        {
            return new PackUriParser { _RegisteredPort = this._RegisteredPort };
        }

        /// <summary>
        ///   Invoked by the Framework when a System.UriParser method is registered.
        /// </summary>
        /// <param name = "schemeName">The scheme that is associated with this System.UriParser.</param>
        /// <param name = "defaultPort">The port number of the scheme.</param>
        protected override void OnRegister(string schemeName, int defaultPort)
        {
            this._RegisteredPort = defaultPort;
        }

        /// <summary>
        ///   Called by System.Uri constructors and Overload:System.Uri.TryCreate to resolve a relative Uri.
        /// </summary>
        /// <param name = "baseUri">A base URI.</param>
        /// <param name = "relativeUri">A relative URI.</param>
        /// <param name = "parsingError">Errors during the resolve process, if any.</param>
        /// <returns>The string of the resolved relative System.Uri.</returns>
        protected override string Resolve(Uri baseUri, Uri relativeUri, out UriFormatException parsingError)
        {
            string result = baseUri.OriginalString;
            string addition = relativeUri.OriginalString;
            parsingError = null;
            if (result.EndsWith("/"))
                result = result.Substring(0, result.Length - 1);
            if (addition.StartsWith("/"))
                addition = addition.Substring(1);
            return result + "/" + addition;
        }
    }
}