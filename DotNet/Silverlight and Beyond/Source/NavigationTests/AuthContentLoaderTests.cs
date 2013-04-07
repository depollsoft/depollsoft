#region Using Directives

using System;
using System.Security.Principal;
using System.Windows;
using Microsoft.Silverlight.Testing;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Navigation.ContentLoaders.Auth;

#endregion

namespace NavigationTests
{
    [TestClass]
    public class AuthContentLoaderTests : SilverlightTest
    {
        [TestMethod]
        public void TestAllow()
        {
            var tst = new Allow();
            var user = new UserShim { Name = "User", IsAuthenticated = true };
            Assert.IsFalse(tst.IsAllowed(user));
            tst.Roles = "somerole, role1";
            Assert.IsTrue(tst.IsAllowed(user));
            tst.Roles = "";
            Assert.IsFalse(tst.IsAllowed(user));
            tst.Users = "u1, User, u2";
            Assert.IsTrue(tst.IsAllowed(user));
            tst.Roles = "somerole, role1, role2";
            Assert.IsTrue(tst.IsAllowed(user));
            Assert.IsFalse(tst.IsDenied(user));
        }

        [TestMethod]
        public void TestDeny()
        {
            var tst = new Deny();
            var user = new UserShim { Name = "User", IsAuthenticated = true };
            Assert.IsFalse(tst.IsDenied(user));
            tst.Roles = "somerole, role1";
            Assert.IsTrue(tst.IsDenied(user));
            tst.Roles = "";
            Assert.IsFalse(tst.IsDenied(user));
            tst.Users = "u1, User, u2";
            Assert.IsTrue(tst.IsDenied(user));
            tst.Roles = "somerole, role1, role2";
            Assert.IsTrue(tst.IsDenied(user));
            Assert.IsFalse(tst.IsAllowed(user));
        }

        [TestMethod]
        public void TestNavigationAuthorizer()
        {
            var tst = new NavigationAuthorizer();
            tst.Rules.Add(new NavigationAuthRule
                {
                    UriPattern = "^test1$",
                    IgnoreCase = true,
                    Parts = new DependencyObjectCollection<INavigationAuthorizationRulePart>
                        {
                            new Allow { Roles = "somerole" }
                        }
                });
            tst.Rules.Add(new NavigationAuthRule
                {
                    UriPattern = "^test2$",
                    IgnoreCase = true,
                    Parts = new DependencyObjectCollection<INavigationAuthorizationRulePart>
                        {
                            new Allow { Roles = "role1" }
                        }
                });
            var user = new UserShim { Name = "User" };
            try
            {
                tst.CheckAuthorization(user, new Uri("test1", UriKind.Relative), null);
                Assert.Fail();
            }
            catch
            {
            }
            try
            {
                tst.CheckAuthorization(user, new Uri("test2", UriKind.Relative), null);
                Assert.Fail();
            }
            catch
            {
            }
            user.IsAuthenticated = true;
            try
            {
                tst.CheckAuthorization(user, new Uri("test1", UriKind.Relative), null);
                Assert.Fail();
            }
            catch
            {
            }
            try
            {
                tst.CheckAuthorization(user, new Uri("test2", UriKind.Relative), null);
            }
            catch
            {
                Assert.Fail();
            }
        }
    }

    internal class UserShim : IPrincipal, IIdentity
    {
        #region IIdentity Members

        public string AuthenticationType
        {
            get { throw new NotImplementedException(); }
        }

        public bool IsAuthenticated { get; set; }

        public string Name { get; set; }

        #endregion

        #region IPrincipal Members

        public IIdentity Identity
        {
            get { return this; }
        }

        public bool IsInRole(string role)
        {
            return role.Equals("role1") || role.Equals("role2");
        }

        #endregion
    }
}