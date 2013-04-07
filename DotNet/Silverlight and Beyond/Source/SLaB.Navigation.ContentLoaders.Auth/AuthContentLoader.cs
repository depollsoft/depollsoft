#region Using Directives

using System;
using System.Security.Principal;
using System.Windows;
using System.Windows.Markup;
using System.Windows.Navigation;
using SLaB.Navigation.ContentLoaders.Utilities;

#endregion

namespace SLaB.Navigation.ContentLoaders.Auth
{
    /// <summary>
    ///   An INavigationContentLoader that throws an UnauthorizedAccessException if the user does not meet the requirements
    ///   specified for the page they are trying to navigate to.
    /// </summary>
    [ContentProperty("Authorizer")]
    public class AuthContentLoader : ContentLoaderBase
    {

        /// <summary>
        ///   The Authorizer that will be used to authorize the Principal.
        /// </summary>
        public static readonly DependencyProperty AuthorizerProperty =
            DependencyProperty.Register("Authorizer",
                                        typeof(INavigationAuthorizer),
                                        typeof(AuthContentLoader),
                                        new PropertyMetadata(null));
        /// <summary>
        ///   The INavigationContentLoader being wrapped by the AuthContentLoader.
        /// </summary>
        public static readonly DependencyProperty ContentLoaderProperty =
            DependencyProperty.Register("ContentLoader",
                                        typeof(INavigationContentLoader),
                                        typeof(AuthContentLoader),
                                        new PropertyMetadata(null));
        private static readonly INavigationContentLoader DefaultLoader = new PageResourceContentLoader();
        /// <summary>
        ///   The principal that will be used to check authorization.
        /// </summary>
        public static readonly DependencyProperty PrincipalProperty =
            DependencyProperty.Register("Principal",
                                        typeof(IPrincipal),
                                        typeof(AuthContentLoader),
                                        new PropertyMetadata(null));



        /// <summary>
        ///   The Authorizer that will be used to authorize the Principal.
        /// </summary>
        public INavigationAuthorizer Authorizer
        {
            get { return (INavigationAuthorizer)this.GetValue(AuthorizerProperty); }
            set { this.SetValue(AuthorizerProperty, value); }
        }

        /// <summary>
        ///   The INavigationContentLoader being wrapped by the AuthContentLoader.
        /// </summary>
        public INavigationContentLoader ContentLoader
        {
            get { return (INavigationContentLoader)this.GetValue(ContentLoaderProperty); }
            set { this.SetValue(ContentLoaderProperty, value); }
        }

        /// <summary>
        ///   The principal that will be used to check authorization.  Bind this to, for example, the
        /// </summary>
        public IPrincipal Principal
        {
            get { return (IPrincipal)this.GetValue(PrincipalProperty); }
            set { this.SetValue(PrincipalProperty, value); }
        }




        /// <summary>
        ///   Gets a value that indicates whether the specified URI can be loaded.
        /// </summary>
        /// <param name = "targetUri">The URI to test.</param>
        /// <param name = "currentUri">The URI that is currently loaded.</param>
        /// <returns>true if the URI can be loaded; otherwise, false.</returns>
        public override bool CanLoad(Uri targetUri, Uri currentUri)
        {
            return (this.ContentLoader ?? DefaultLoader).CanLoad(targetUri, currentUri);
        }

        /// <summary>
        ///   Creates an instance of a LoaderBase that will be used to handle loading.
        /// </summary>
        /// <returns>An instance of a LoaderBase.</returns>
        protected override LoaderBase CreateLoader()
        {
            return new AuthLoader(this);
        }




        private class AuthLoader : LoaderBase
        {
		#region Fields (3) 

            private INavigationContentLoader _Loader;
            private readonly AuthContentLoader _Parent;
            private IAsyncResult _Result;

		#endregion Fields 

		#region Constructors (1) 

            public AuthLoader(AuthContentLoader parent)
            {
                this._Parent = parent;
            }

		#endregion Constructors 

		#region Methods (2) 

		// Public Methods (2) 

            public override void Cancel()
            {
                this._Loader.CancelLoad(this._Result);
            }

            public override void Load(Uri targetUri, Uri currentUri)
            {
                this._Loader = this._Parent.ContentLoader ?? DefaultLoader;
                if (this._Parent.Authorizer != null)
                {
                    try
                    {
                        this._Parent.Authorizer.CheckAuthorization(this._Parent.Principal, targetUri, currentUri);
                    }
                    catch (Exception e)
                    {
                        this.Error(e);
                        return;
                    }
                }
                try
                {
                    this._Result = this._Loader.BeginLoad(targetUri,
                                                          currentUri,
                                                          res =>
                                                              {
                                                                  try
                                                                  {
                                                                      var loadResult = this._Loader.EndLoad(res);
                                                                      if (loadResult.RedirectUri != null)
                                                                          Complete(loadResult.RedirectUri);
                                                                      else
                                                                          Complete(loadResult.LoadedContent);
                                                                      return;
                                                                  }
                                                                  catch (Exception e)
                                                                  {
                                                                      this.Error(e);
                                                                      return;
                                                                  }
                                                              },
                                                          null);
                }
                catch (Exception e)
                {
                    this.Error(e);
                    return;
                }
            }

		#endregion Methods 
        }
    }
}