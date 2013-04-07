#region Using Directives

using System;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Markup;
using System.Windows.Resources;
using SLaB.Navigation.ContentLoaders.Utilities;

#endregion

namespace SLaB.Navigation.ContentLoaders.PageResource
{
    /// <summary>
    ///   Mimics the behavior of the built-in PageResourceContentLoader, but also works against dynamically-loaded libraries.
    /// </summary>
    public class PageResourceContentLoader : ContentLoaderBase
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
        #region Fields (3) 

            private static readonly Regex ComponentRegex =
                new Regex(
                    @"^(?<packassemblyname>/(?<assemblyname>.*?)(;v(?<version>.*?))?(;(?<publickey>.*?))?;component)?(?<path>/.*?)(\?.*?)?(#.*?)?$");
            private const string XamlLoadString = @"<my:{0} xmlns:my='clr-namespace:{1};assembly={2}' />";
            private static readonly Regex XClassRegex = new Regex(".*x:Class=\"(?<className>.*?)\"",
                                                                  RegexOptions.CultureInvariant);

        #endregion Fields 

        #region Methods (6) 

        // Public Methods (2) 

            public override void Cancel()
            {
            }

            public override void Load(Uri targetUri, Uri currentUri)
            {
                string assemblyName = GetAssemblyName(targetUri);
                if (assemblyName == null)
                {
                    AssemblyName an = new AssemblyName(Deployment.Current.EntryPointAssembly);
                    StringBuilder sb = new StringBuilder("/");
                    sb.Append(an.Name);
                    if (an.Version != null)
                        sb.AppendFormat(";v{0}", an.Version);
                    if (an.GetPublicKeyToken() != null)
                        sb.AppendFormat(";{0}",
                                        new string(an.GetPublicKeyToken().Cast<char>().ToArray()),
                                        targetUri.OriginalString);
                    sb.AppendFormat(";component{0}", targetUri.OriginalString);
                    targetUri = new Uri(sb.ToString(), UriKind.Relative);
                    assemblyName = GetAssemblyName(targetUri);
                }
                StreamResourceInfo xamlStream = Application.GetResourceStream(targetUri);
                StreamReader reader = new StreamReader(xamlStream.Stream);
                string xaml = reader.ReadToEnd();
                string typeName = GetTypeName(xaml);
                object value =
                    XamlReader.Load(typeName != null
                                        ? string.Format(XamlLoadString,
                                                        GetShortTypeName(typeName),
                                                        GetNamespaceName(typeName),
                                                        assemblyName)
                                        : xaml);
                Complete(value);
            }
        // Private Methods (4) 

            private static string GetAssemblyName(Uri uri)
            {
                Match m = ComponentRegex.Match(uri.OriginalString);
                if (!m.Groups["assemblyname"].Success)
                    return null;
                string name = m.Groups["assemblyname"].Value;
                if (m.Groups["version"].Success)
                    name += ",Version=" + m.Groups["version"].Value;
                if (m.Groups["publickey"].Success)
                    name += ",PublicKeyToken=" + m.Groups["publickey"].Value;
                return name;
            }

            private static string GetNamespaceName(string typeName)
            {
                return typeName.Substring(0, Math.Max(0, typeName.LastIndexOf('.')));
            }

            private static string GetShortTypeName(string typeName)
            {
                return typeName.Substring(typeName.LastIndexOf('.') + 1);
            }

            private static string GetTypeName(string xaml)
            {
                Group g = XClassRegex.Match(xaml).Groups["className"];
                return g.Success ? g.Value : null;
            }

        #endregion Methods 
        }
    }
}