#region Using Directives

using System;
using System.Windows.Navigation;
using Microsoft.Silverlight.Testing;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Navigation.ContentLoaders.Event;

#endregion

namespace NavigationTests
{
    [TestClass]
    public class EventContentLoaderTests : SilverlightTest
    {
        [TestMethod]
        public void TestCanLoad()
        {
            bool called = false;
            var loader = new SynchronousEventContentLoader();
            loader.CanLoad += (target, current) =>
                {
                    called = true;
                    return false;
                };
            INavigationContentLoader ldr = loader;
            Assert.IsFalse(ldr.CanLoad(new Uri("/Test", UriKind.Relative), null));
            Assert.IsTrue(called);
        }

        [TestMethod]
        public void TestSynchronousEventContentLoader()
        {
            bool called = false;
            var loader = new SynchronousEventContentLoader();
            loader.Load += (targetUri, currentUri) =>
                {
                    called = true;
                    return new LoadResult(targetUri);
                };
            INavigationContentLoader ldr = loader;
            ldr.BeginLoad(new Uri("/Test", UriKind.Relative),
                          null,
                          res =>
                              {
                                  Assert.IsTrue(res.CompletedSynchronously);
                                  Assert.IsTrue(called);
                                  Assert.AreEqual(ldr.EndLoad(res).RedirectUri, new Uri("/Test", UriKind.Relative));
                              },
                          null);
        }
    }
}