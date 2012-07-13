#region Using Directives

using System;
using System.IO;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Markup;
using System.Windows.Resources;
using SLaB.Navigation.ContentLoaders.Utilities;
using SLaB.Utilities;


#endregion

namespace SLaB.Navigation.ContentLoaders.Xap
{
    /// <summary>
    ///   An INavigationContentLoader that loads a page within a given Xap.
    /// </summary>
    public class XapPageResourceContentLoader : ContentLoaderBase
    {

        /// <summary>
        ///   Gets or sets the Xap from which the XapPageResourceContentLoader should load.
        /// </summary>
        public static readonly DependencyProperty XapProperty =
            DependencyProperty.Register("Xap",
                                        typeof(SLaB.Utilities.Xap.Xap),
                                        typeof(XapPageResourceContentLoader),
                                        new PropertyMetadata(null));



        static XapPageResourceContentLoader()
        {
            PackUriParser.Initialize();
        }



        /// <summary>
        ///   Gets or sets the Xap from which the XapPageResourceContentLoader should load.
        /// </summary>
        public SLaB.Utilities.Xap.Xap Xap
        {
            get { return (SLaB.Utilities.Xap.Xap)this.GetValue(XapProperty); }
            set { this.SetValue(XapProperty, value); }
        }




        /// <summary>
        ///   Creates an instance of a LoaderBase that will be used to handle loading.
        /// </summary>
        /// <returns>An instance of a LoaderBase.</returns>
        protected override LoaderBase CreateLoader()
        {
            return new Loader(this);
        }




        private class Loader : LoaderBase
        {
        #region Fields (3) 

            private readonly XapPageResourceContentLoader _Loader;
            private readonly Regex _XClassRegex = new Regex(".*x:Class=\"(?<className>.*?)\"",
                                                            RegexOptions.CultureInvariant);
            private const string XamlLoadString = @"<my:{0} xmlns:my='clr-namespace:{1};assembly={2}' />";

        #endregion Fields 

        #region Constructors (1) 

            public Loader(XapPageResourceContentLoader loader)
            {
                this._Loader = loader;
            }

        #endregion Constructors 

        #region Methods (5) 

        // Public Methods (2) 

            public override void Cancel()
            {
            }

            public override void Load(Uri targetUri, Uri currentUri)
            {
                PackUriHelper helper = new PackUriHelper(targetUri);
                string assemblyName = helper.AssemblyName ??
                                      UiUtilities.ExecuteOnUiThread(() => this._Loader.Xap.Manifest.EntryPointAssembly);
                StreamResourceInfo xamlStream =
                    Application.GetResourceStream(new Uri("/" + assemblyName + ";component" + helper.Path,
                                                          UriKind.Relative));
                StreamReader reader = new StreamReader(xamlStream.Stream);
                string xaml = reader.ReadToEnd();
                string typeName = this.GetTypeName(xaml);
                object value = typeName != null
                                   ? UiUtilities.ExecuteOnUiThread(
                                       () =>
                                       XamlReader.Load(string.Format(XamlLoadString,
                                                                     GetShortTypeName(typeName),
                                                                     GetNamespaceName(typeName),
                                                                     assemblyName)))
                                   : UiUtilities.ExecuteOnUiThread(() => XamlReader.Load(xaml));
                Complete(value);
            }
        // Private Methods (3) 

            private static string GetNamespaceName(string typeName)
            {
                return typeName.Substring(0, Math.Max(0, typeName.LastIndexOf('.')));
            }

            private static string GetShortTypeName(string typeName)
            {
                return typeName.Substring(typeName.LastIndexOf('.') + 1);
            }

            private string GetTypeName(string xaml)
            {
                Group g = this._XClassRegex.Match(xaml).Groups["className"];
                return g.Success ? g.Value : null;
            }

        #endregion Methods 
        }
    }
}