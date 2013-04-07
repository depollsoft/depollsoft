#region Using Directives

using System.Collections.Generic;
using System.Linq;
using System.Security.Principal;
using System.Windows;

#endregion

namespace SLaB.Navigation.ContentLoaders.Auth
{
    /// <summary>
    ///   Specifies the roles/users that are allowed to access a page.
    /// </summary>
    public class Allow : DependencyObject, INavigationAuthorizationRulePart
    {

        /// <summary>
        ///   Gets or sets, in a comma-separated list, the set of roles to allow.
        /// </summary>
        public static readonly DependencyProperty RolesProperty =
            DependencyProperty.Register("Roles", typeof(string), typeof(Allow), new PropertyMetadata(""));
        /// <summary>
        ///   Gets or sets, in a comma-separated list, the set of users to allow.  "?" indicates anonymous users will be allowed.
        ///   "*" indicates that all users will be allowed.
        /// </summary>
        public static readonly DependencyProperty UsersProperty =
            DependencyProperty.Register("Users", typeof(string), typeof(Allow), new PropertyMetadata(""));



        /// <summary>
        ///   Gets or sets, in a comma-separated list, the set of roles to allow.
        /// </summary>
        public string Roles
        {
            get { return (string)this.GetValue(RolesProperty); }
            set { this.SetValue(RolesProperty, value); }
        }

        /// <summary>
        ///   Gets or sets, in a comma-separated list, the set of users to allow.  "?" indicates anonymous users will be allowed.
        ///   "*" indicates that all users will be allowed.
        /// </summary>
        public string Users
        {
            get { return (string)this.GetValue(UsersProperty); }
            set { this.SetValue(UsersProperty, value); }
        }




        private static bool HasAnyRole(string roles, IPrincipal principal)
        {
            if (principal == null)
                return false;
            IEnumerable<string> roleList = from r in roles.Split(',')
                                           select r.Trim();
            return roleList.Any(principal.IsInRole);
        }

        private static bool HasUser(string users, IPrincipal principal)
        {
            IEnumerable<string> userList = from u in users.Split(',')
                                           select u.Trim();
            if (principal == null || principal.Identity == null || !principal.Identity.IsAuthenticated)
                return userList.Contains("?") || userList.Contains("*");
            return userList.Contains("*") || userList.Contains(principal.Identity.Name);
        }




        #region INavigationAuthorizationRulePart Members

        /// <summary>
        ///   Indicates whether the principal is allowed by this rule part.
        /// </summary>
        /// <param name = "principal">The principal to check.</param>
        /// <returns>True if the principal is allowed.  False otherwise.</returns>
        public bool IsAllowed(IPrincipal principal)
        {
            if (this.Users != null && HasUser(this.Users, principal))
                return true;
            return principal != null && principal.Identity != null && principal.Identity.IsAuthenticated &&
                   this.Roles != null && HasAnyRole(this.Roles, principal);
        }

        /// <summary>
        ///   Indicates whether the principal is denied by this rule part.
        /// </summary>
        /// <param name = "principal">The principal to check.</param>
        /// <returns>True if the principal is denied.  False otherwise.</returns>
        public bool IsDenied(IPrincipal principal)
        {
            return false;
        }

        #endregion
    }
}