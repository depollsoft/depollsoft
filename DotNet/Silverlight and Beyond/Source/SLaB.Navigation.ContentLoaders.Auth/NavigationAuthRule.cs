#region Using Directives

using System;
using System.Security.Principal;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Markup;

#endregion

namespace SLaB.Navigation.ContentLoaders.Auth
{
    /// <summary>
    ///   Represents an authorization rule for the NavigationAuthorizer.
    /// </summary>
    [ContentProperty("Parts")]
    public class NavigationAuthRule : DependencyObject
    {

        private const string ErrorStringPattern = "Cannot access due to rule for Uri Pattern: \"{0}\"";
        /// <summary>
        ///   Specifies whether the regular expression will ignore case when checking for matches.  True by default.
        /// </summary>
        public static readonly DependencyProperty IgnoreCaseProperty =
            DependencyProperty.Register("IgnoreCase",
                                        typeof(bool),
                                        typeof(NavigationAuthRule),
                                        new PropertyMetadata(true));
        /// <summary>
        ///   The set of parts (e.g. Allow and Deny) that make up the authorization rule.
        /// </summary>
        public static readonly DependencyProperty PartsProperty =
            DependencyProperty.Register("Parts",
                                        typeof(DependencyObjectCollection<INavigationAuthorizationRulePart>),
                                        typeof(NavigationAuthRule),
                                        new PropertyMetadata(null));
        /// <summary>
        ///   Specifies a regular expression to be used to match Uris being loaded.
        /// </summary>
        public static readonly DependencyProperty UriProperty =
            DependencyProperty.Register("Uri", typeof(string), typeof(NavigationAuthRule), new PropertyMetadata(""));



        /// <summary>
        ///   Constructs a new NavigationAuthRule.
        /// </summary>
        public NavigationAuthRule()
        {
            this.Parts = new DependencyObjectCollection<INavigationAuthorizationRulePart>();
        }



        /// <summary>
        ///   Specifies whether the regular expression will ignore case when checking for matches.  True by default.
        /// </summary>
        public bool IgnoreCase
        {
            get { return (bool)this.GetValue(IgnoreCaseProperty); }
            set { this.SetValue(IgnoreCaseProperty, value); }
        }

        /// <summary>
        ///   The set of parts (e.g. Allow and Deny) that make up the authorization rule.
        /// </summary>
        public DependencyObjectCollection<INavigationAuthorizationRulePart> Parts
        {
            get { return (DependencyObjectCollection<INavigationAuthorizationRulePart>)this.GetValue(PartsProperty); }
            set { this.SetValue(PartsProperty, value); }
        }

        /// <summary>
        ///   Specifies a regular expression to be used to match Uris being loaded.
        /// </summary>
        public string UriPattern
        {
            get { return (string)this.GetValue(UriProperty); }
            set { this.SetValue(UriProperty, value); }
        }




        /// <summary>
        ///   Checks the principal against the parts of the rule and throws if the principal is unauthorized.
        /// </summary>
        /// <param name = "principal">The principal whose credentials are being checked.</param>
        public void Check(IPrincipal principal)
        {
            if (this.Parts == null || this.Parts.Count == 0)
                return;
            foreach (var rule in this.Parts)
            {
                if (rule.IsAllowed(principal))
                    return;
                if (rule.IsDenied(principal))
                    throw new UnauthorizedAccessException(string.Format(ErrorStringPattern, this.UriPattern));
            }
            throw new UnauthorizedAccessException(string.Format(ErrorStringPattern, this.UriPattern));
        }

        /// <summary>
        ///   Checks to see whether the given uri matches the UriPattern.
        /// </summary>
        /// <param name = "uri">The uri being matched.</param>
        /// <returns>True if the Uri is a match for the regular expression pattern supplied as UriPattern.</returns>
        public bool Matches(Uri uri)
        {
            return
                new Regex(this.UriPattern, this.IgnoreCase ? RegexOptions.IgnoreCase : RegexOptions.None).IsMatch(
                    uri.OriginalString);
        }
    }
}