#region Using Directives

using System;
using System.Collections.Generic;
using System.ComponentModel.Composition;
using System.ComponentModel.Composition.Hosting;
using System.Linq;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using SLaB.Navigation.ContentLoaders.Utilities;

#endregion

namespace SLaB.Navigation.ContentLoaders.MEF
{
    /// <summary>
    ///   ContentLoader that uses MEF to locate Pages for the given Uris.
    /// </summary>
    public class MEFContentLoader : ContentLoaderBase
    {

        private bool _Initialized;
        private static readonly AggregateCatalog Catalog = new AggregateCatalog();
        private static readonly List<string> LoadedXaps = new List<string>();
        private static readonly Dictionary<Uri, string> UriMap = new Dictionary<Uri, string>();
        ///<summary>
        ///  The Xap from which content should be loaded.
        ///</summary>
        public static readonly DependencyProperty XapProperty =
            DependencyProperty.RegisterAttached("Xap",
                                                typeof(string),
                                                typeof(MEFContentLoader),
                                                new PropertyMetadata(null, XapPropertyChangedCallback));



        ///<summary>
        ///  The imported PageFactories used in creating.
        ///</summary>
        [ImportMany(AllowRecomposition = true)]
        public IEnumerable<ExportFactory<Page, IExportPageMetadata>> PageFactories { get; set; }




        /// <summary>
        ///   Gets the Xap associated with the HyperlinkButton.
        /// </summary>
        /// <param name = "obj">The HyperlinkButton for which to get the associated Xap.</param>
        /// <returns>The Xap associated with the HyperlinkButton.</returns>
        public static string GetXap(HyperlinkButton obj)
        {
            return (string)obj.GetValue(XapProperty);
        }

        /// <summary>
        ///   Gets the Xap associated with the HyperlinkButton.
        /// </summary>
        /// <param name = "obj">The Hyperlink for which to get the associated Xap.</param>
        /// <returns>The Xap associated with the Hyperlink.</returns>
        public static string GetXap(Hyperlink obj)
        {
            return (string)obj.GetValue(XapProperty);
        }

        /// <summary>
        ///   Sets the Xap associated with the HyperlinkButton.
        /// </summary>
        /// <param name = "obj">The HyperlinkButton with which the Xap should be associated.</param>
        /// <param name = "value">The Xap to associate with the HyperlinkButton.</param>
        public static void SetXap(HyperlinkButton obj, string value)
        {
            obj.SetValue(XapProperty, value);
        }

        /// <summary>
        ///   Sets the Xap associated with the Hyperlink.
        /// </summary>
        /// <param name = "obj">The Hyperlink with which the Xap should be associated.</param>
        /// <param name = "value">The Xap to associate with the Hyperlink.</param>
        public static void SetXap(Hyperlink obj, string value)
        {
            obj.SetValue(XapProperty, value);
        }

        /// <summary>
        ///   Creates an instance of a LoaderBase that will be used to handle loading.
        /// </summary>
        /// <returns>An instance of a LoaderBase.</returns>
        protected override LoaderBase CreateLoader()
        {
            return new Loader(this);
        }

        private void Initialize()
        {
            CompositionHost.Initialize(new DeploymentCatalog(), Catalog);
            CompositionInitializer.SatisfyImports(this);
            this._Initialized = true;
        }

        private static void XapPropertyChangedCallback(DependencyObject d, DependencyPropertyChangedEventArgs e)
        {
            var button = (HyperlinkButton)d;
            UriMap[button.NavigateUri] = (string)e.NewValue;
        }




        private class Loader : LoaderBase
        {
        #region Fields (1) 

            private readonly MEFContentLoader _Parent;

        #endregion Fields 

        #region Constructors (1) 

            public Loader(MEFContentLoader parent)
            {
                this._Parent = parent;
            }

        #endregion Constructors 

        #region Methods (5) 

        // Public Methods (2) 

            public override void Cancel()
            {
            }

            public override void Load(Uri targetUri, Uri currentUri)
            {
                try
                {
                    if (!this._Parent._Initialized)
                        this._Parent.Initialize();
                    var saveTargetUri = targetUri;
                    if (!targetUri.IsAbsoluteUri)
                        targetUri = new Uri("dummy:///" + targetUri.OriginalString);

                    var xap = UriMap[saveTargetUri];
                    if (xap != null && !LoadedXaps.Contains(xap))
                    {
                        var dc = new DeploymentCatalog(xap);
                        dc.DownloadCompleted += (s, e) =>
                            {
                                if (e.Error != null)
                                {
                                    this.Error(e.Error);
                                    return;
                                }
                                Catalog.Catalogs.Add(dc);
                                LoadedXaps.Add(xap);
                                this.NavigateToPage(targetUri);
                            };
                        dc.DownloadAsync();
                    }
                    else
                        this.NavigateToPage(targetUri);
                }
                catch (Exception e)
                {
                    this.Error(e);
                }
            }
        // Private Methods (3) 

            private static bool CompareUri(ExportFactory<Page, IExportPageMetadata> factory, Uri targetUri)
            {
                Uri uri = GetTrimmedUri(factory.Metadata.NavigateUri);
                if (!uri.IsAbsoluteUri)
                    uri = new Uri("dummy:///" + uri.OriginalString);
                return targetUri.Equals(uri);
            }

            private static Uri GetTrimmedUri(string uriString)
            {
                if (uriString.Contains('?'))
                    uriString = uriString.Substring(0, uriString.IndexOf('?') + 1);
                Uri result;
                try
                {
                    result = new Uri(uriString, UriKind.Absolute);
                }
                catch
                {
                    result = new Uri(uriString, UriKind.Relative);
                }
                return result;
            }

            private void NavigateToPage(Uri targetUri)
            {
                var pageFactory = this._Parent.PageFactories.Single(factory => CompareUri(factory, targetUri));
                var pageContext = pageFactory.CreateExport();
                Complete(pageContext.Value);
            }

        #endregion Methods 
        }
    }
}