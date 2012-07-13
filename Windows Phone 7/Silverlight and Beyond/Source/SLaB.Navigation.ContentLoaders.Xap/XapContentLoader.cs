#region Using Directives

using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Windows;
using System.Windows.Navigation;
using SLaB.Navigation.ContentLoaders.Utilities;
using SLaB.Utilities.Xap;


#endregion

namespace SLaB.Navigation.ContentLoaders.Xap
{
    /// <summary>
    ///   An INavigationContentLoader that loads pages in Xaps specified by a pack Uri.  This loader will download the Xap if it has not already been downloaded
    ///   and then load a page within that Xap, making it easy to build multi-Xap applications using Navigation.
    /// </summary>
    public class XapContentLoader : ContentLoaderBase
    {

        private readonly Dictionary<Uri, SLaB.Utilities.Xap.Xap> _FoundXaps;
        /// <summary>
        ///   Gets or sets whether the XapContentLoader is allowed to download Xaps from a domain other than the domain of origin for the application.
        ///   This feature is disabled by default, but can be enabled to load arbitrary Xaps.  If you enable this feature, please limit access to other domains
        ///   using some other mechanism.
        /// </summary>
        public static readonly DependencyProperty EnableCrossDomainProperty =
            DependencyProperty.Register("EnableCrossDomain",
                                        typeof(bool),
                                        typeof(XapContentLoader),
                                        new PropertyMetadata(false));
        /// <summary>
        ///   Gets a boolean indicating whether the XapContentLoader is in the process of downloading or loading a page.
        /// </summary>
        public static readonly DependencyProperty IsBusyProperty =
            DependencyProperty.Register("IsBusy", typeof(bool), typeof(XapContentLoader), new PropertyMetadata(false));
        /// <summary>
        ///   Gets the progress of any ongoing downloads that the XapContentLoader is performing.
        /// </summary>
        public static readonly DependencyProperty ProgressProperty =
            DependencyProperty.Register("Progress", typeof(double), typeof(XapContentLoader), new PropertyMetadata(0d));



        static XapContentLoader()
        {
            PackUriParser.Initialize();
        }

        /// <summary>
        ///   Constructs a XapContentLoader.
        /// </summary>
        public XapContentLoader()
        {
            this._FoundXaps = new Dictionary<Uri, SLaB.Utilities.Xap.Xap>();
        }



        /// <summary>
        ///   Gets or sets whether the XapContentLoader is allowed to download Xaps from a domain other than the domain of origin for the application.
        ///   This feature is disabled by default, but can be enabled to load arbitrary Xaps.  If you enable this feature, please limit access to other domains
        ///   using some other mechanism.
        /// </summary>
        public bool EnableCrossDomain
        {
            get { return (bool)this.GetValue(EnableCrossDomainProperty); }
            set { this.SetValue(EnableCrossDomainProperty, value); }
        }

        /// <summary>
        ///   Gets a boolean indicating whether the XapContentLoader is in the process of downloading or loading a page.
        /// </summary>
        public bool IsBusy
        {
            get { return (bool)this.GetValue(IsBusyProperty); }
            private set { this.SetValue(IsBusyProperty, value); }
        }

        /// <summary>
        ///   Gets the progress of any ongoing downloads that the XapContentLoader is performing.
        /// </summary>
        public double Progress
        {
            get { return (double)this.GetValue(ProgressProperty); }
            private set { this.SetValue(ProgressProperty, value); }
        }




        /// <summary>
        ///   Gets a value that indicates whether the specified URI can be loaded.
        /// </summary>
        /// <param name = "targetUri">The URI to test.</param>
        /// <param name = "currentUri">The URI that is currently loaded.</param>
        /// <returns>true if the URI can be loaded; otherwise, false.</returns>
        public override bool CanLoad(Uri targetUri, Uri currentUri)
        {
            try
            {
                PackUriHelper helper = new PackUriHelper(targetUri);
                return this.EnableCrossDomain || helper.Authority.Host.Equals(Application.Current.Host.Source.Host);
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        ///   An attached property that enables Pack Uris.  Place this at the top of a XAML file to ensure that pack URIs
        ///   are safe to use in that context.
        /// </summary>
        /// <param name = "obj">The element of the XAML for which pack Uris should be enabled.</param>
        /// <returns>True.</returns>
        public static bool GetEnablePackUris(object obj)
        {
            return true;
        }

        /// <summary>
        ///   An attached property that enables Pack Uris.  Place this at the top of a XAML file to ensure that pack URIs
        ///   are safe to use in that context.
        /// </summary>
        /// <param name = "obj">The element of the XAML for which pack Uris should be enabled.</param>
        /// <param name = "value">An ignored value.</param>
        public static void SetEnablePackUris(object obj, bool value)
        {
            return;
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
        #region Fields (4) 

            private INavigationContentLoader _CurrentLoader;
            private readonly XapContentLoader _Loader;
            private IAsyncResult _Result;
            private XapLoader _XapLoader;

        #endregion Fields 

        #region Constructors (1) 

            public Loader(XapContentLoader loader)
            {
                this._Loader = loader;
            }

        #endregion Constructors 

        #region Methods (4) 

        // Public Methods (2) 

            public override void Cancel()
            {
                this._Loader.IsBusy = false;
                if (this._Result is XapLoader.XapAsyncResult)
                    this._XapLoader.CancelLoadXap(this._Result);
                else
                    this._CurrentLoader.CancelLoad(this._Result);
            }

            public override void Load(Uri targetUri, Uri currentUri)
            {
                if (!this._Loader.CanLoad(targetUri, currentUri))
                    throw new UnauthorizedAccessException("Cannot load uri due to cross-domain limitations");
                this._Loader.Progress = 0;
                this._Loader.IsBusy = true;
                PackUriHelper helper = new PackUriHelper(targetUri);
                Uri authority = helper.Authority;
                if (helper.Authority.Equals(PackUriHelper.Application))
                    authority = Application.Current.Host.Source;
                if (this._Loader._FoundXaps.ContainsKey(authority))
                {
                    this.FinishLoading(this._Loader._FoundXaps[authority], helper, currentUri);
                    return;
                }
                this._XapLoader = new XapLoader();
                this._Result = this._XapLoader.BeginLoadXap(authority,
                                                            res =>
                                                                {
                                                                    try
                                                                    {
                                                                        SLaB.Utilities.Xap.Xap resultingXap =
                                                                            this._XapLoader.EndLoadXap(res);
                                                                        this._Loader._FoundXaps[authority] =
                                                                            resultingXap;
                                                                        this.FinishLoading(
                                                                            this._Loader._FoundXaps[authority],
                                                                            helper,
                                                                            currentUri);
                                                                    }
                                                                    catch (Exception e)
                                                                    {
                                                                        this._Loader.IsBusy = false;
                                                                        this.Error(e);
                                                                        return;
                                                                    }
                                                                },
                                                            null);
                XapLoader.XapAsyncResult result = (XapLoader.XapAsyncResult)this._Result;
                result.PropertyChanged += this.ResultPropertyChanged;
            }
        // Private Methods (2) 

            private void FinishLoading(SLaB.Utilities.Xap.Xap finalXap, PackUriHelper uri, Uri currentUri)
            {
                INavigationContentLoader finalLoader = new XapPageResourceContentLoader { Xap = finalXap };
                this._CurrentLoader = finalLoader;
                try
                {
                    this._Result = finalLoader.BeginLoad(new Uri(uri.RelativePath, UriKind.Relative),
                                                         currentUri,
                                                         res =>
                                                             {
                                                                 try
                                                                 {
                                                                     LoadResult result = finalLoader.EndLoad(res);
                                                                     if (result.RedirectUri != null)
                                                                     {
                                                                         this._Loader.IsBusy = false;
                                                                         Complete(result.RedirectUri);
                                                                         return;
                                                                     }
                                                                     this._Loader.IsBusy = false;
                                                                     this.Complete(result.LoadedContent);
                                                                     return;
                                                                 }
                                                                 catch (Exception e)
                                                                 {
                                                                     this._Loader.IsBusy = false;
                                                                     this.Error(e);
                                                                     return;
                                                                 }
                                                             },
                                                         null);
                }
                catch (Exception ex)
                {
                    this._Loader.IsBusy = false;
                    this.Error(ex);
                    return;
                }
            }

            private void ResultPropertyChanged(object sender, PropertyChangedEventArgs e)
            {
                XapLoader.XapAsyncResult result = (XapLoader.XapAsyncResult)sender;
                this._Loader.Progress = result.Progress;
            }

        #endregion Methods 
        }
    }
}