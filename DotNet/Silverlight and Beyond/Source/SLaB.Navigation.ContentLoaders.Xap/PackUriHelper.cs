#region Using Directives

using System;
using System.Text.RegularExpressions;

#endregion

namespace SLaB.Navigation.ContentLoaders.Xap
{
    internal class PackUriHelper
    {

        private readonly Uri _Uri;
        public static readonly Uri Application;
        private static readonly Regex AssemblyNameRegex =
            new Regex(
                @"^(?<name>.*?)(,\s*version=(?<version>.*?))?(,\s*culture=(?<culture>.*?))?(,\s*PublicKeyToken=(?<publickey>.*?))?$",
                RegexOptions.IgnoreCase);
        private static readonly Regex ComponentRegex =
            new Regex(
                @"^(?<packassemblyname>/(?<assemblyname>.*?)(;v(?<version>.*?))?(;(?<publickey>.*?))?;component)?(?<path>/.*?)(\?.*?)?(#.*?)?$");
        public static readonly Uri PackApplication;
        public static readonly Uri SiteOfOrigin;
        public static readonly Uri SiteOfOriginReal;



        public PackUriHelper(Uri uri)
        {
            if (!uri.IsAbsoluteUri)
                uri = new Uri(PackApplication, uri);
            if (!uri.Scheme.Equals("pack"))
                throw new ArgumentException("Uri is not a pack uri");
            this._Uri = uri;
        }

        static PackUriHelper()
        {
            PackUriParser.Initialize();
            Application = new Uri("application:///");
            PackApplication = new Uri("pack://application:,,,");
            SiteOfOrigin = new Uri("siteoforigin:///");
            Uri sorBase = new Uri(System.Windows.Application.Current.Host.Source, ".");
            string full =
                Uri.EscapeDataString(sorBase.OriginalString).Replace("%2F", ",").Replace("%2f", ",").Replace("%3A", ":")
                    .Replace("%3a", ":");
            SiteOfOriginReal = new Uri("pack://" + full);
        }



        public string AssemblyName
        {
            get
            {
                Match m = ComponentRegex.Match(this._Uri.AbsolutePath);
                if (!m.Groups["assemblyname"].Success)
                    return null;
                string name = m.Groups["assemblyname"].Value;
                if (m.Groups["version"].Success)
                    name += ",Version=" + m.Groups["version"].Value;
                if (m.Groups["publickey"].Success)
                    name += ",PublicKeyToken=" + m.Groups["publickey"].Value;
                return name;
            }
        }

        public Uri Authority
        {
            get
            {
                if (this.RealAuthority.Scheme.Equals("siteoforigin"))
                {
                    return new Uri(System.Windows.Application.Current.Host.Source,
                                   this.RealAuthority.OriginalString.Substring("siteoforigin://".Length));
                }
                return this.RealAuthority.Scheme.Equals("application")
                           ? System.Windows.Application.Current.Host.Source
                           : this.RealAuthority;
            }
        }

        public string PackAssemblyName
        {
            get
            {
                Match m = ComponentRegex.Match(this._Uri.AbsolutePath);
                if (!m.Groups["packassemblyname"].Success)
                    return null;
                string name = m.Groups["packassemblyname"].Value;
                return name;
            }
        }

        public string Path
        {
            get
            {
                Match m = ComponentRegex.Match(this._Uri.AbsolutePath);
                return m.Groups["path"].Value;
            }
        }

        public Uri RealAuthority
        {
            get { return new Uri(Uri.UnescapeDataString(this._Uri.Host.Replace(",", "/"))); }
        }

        public string RelativePath
        {
            get { return this._Uri.OriginalString.Substring("pack://".Length + this._Uri.Host.Length + 1); }
        }

        public Uri Uri
        {
            get { return this._Uri; }
        }




        public static Uri BuildPackUri(Uri authority, string path, string assemblyName = null)
        {
            string translatedAuthority =
                Uri.EscapeDataString(authority.OriginalString).Replace("%2F", ",").Replace("%2f", ",").Replace("%3A",
                                                                                                               ":").
                    Replace("%3a", ":");
            if (string.IsNullOrWhiteSpace(assemblyName))
                return new Uri(string.Format("pack://{0}{1}", translatedAuthority, path));
            Match anParts = AssemblyNameRegex.Match(assemblyName);
            string translatedAssemblyName = anParts.Groups["name"].Value;
            if (anParts.Groups["version"].Success)
                translatedAssemblyName += ";v" + anParts.Groups["version"].Value;
            if (anParts.Groups["publickey"].Success)
                translatedAssemblyName += ";" + anParts.Groups["publickey"].Value;
            if (!path.StartsWith("/"))
                path = "/" + path;
            return
                new Uri(string.Format("pack://{0}/{1};component{2}", translatedAuthority, translatedAssemblyName, path));
        }
    }
}