#region Using Directives

using System;
using System.Windows.Navigation;
using Microsoft.Silverlight.Testing;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Navigation.ContentLoaders.Error;
using SLaB.Navigation.ContentLoaders.Event;

#endregion

namespace NavigationTests
{
    [TestClass]
    public class ErrorPageTests : SilverlightTest
    {
        [TestMethod]
        [Asynchronous]
        public void TestCannotLoadError()
        {
            var loader = new ErrorPageLoader();
            loader.ErrorPages.Add(new ErrorPage
                {
                    ExceptionType = "CannotLoadException",
                    ErrorPageUri = new Uri("/ExistingPage.xaml", UriKind.Relative)
                });
            EnqueueCallback(
                () => loader.BeginLoad(new Uri("http://NonExistentPage.xaml", UriKind.Absolute),
                                       null,
                                       res =>
                                           {
                                               LoadResult result;
                                               try
                                               {
                                                   result = loader.EndLoad(res);
                                               }
                                               catch
                                               {
                                                   Assert.Fail();
                                                   this.EnqueueTestComplete();
                                                   return;
                                               }
                                               Assert.IsNotNull(result.LoadedContent);
                                               Assert.IsInstanceOfType(result.LoadedContent, typeof(ExistingPage));
                                               Assert.IsInstanceOfType(
                                                   ErrorPageLoader.GetError(result.LoadedContent as ExistingPage),
                                                   typeof(CannotLoadException));
                                               this.EnqueueTestComplete();
                                           },
                                       null));
        }

        [TestMethod]
        public void TestCatchAllExceptionMatching()
        {
            var ep = new ErrorPage();
            Assert.IsTrue(ep.Matches(new ArgumentException()));
            Assert.IsTrue(ep.Matches(new ArgumentNullException()));
            Assert.IsTrue(ep.Matches(new ArithmeticException()));
        }

        [TestMethod]
        [Asynchronous]
        public void TestCatchingBase()
        {
            var loader = new ErrorPageLoader();
            loader.ErrorPages.Add(new ErrorPage
                { ExceptionType = "Exception", ErrorPageUri = new Uri("/ExistingPage.xaml", UriKind.Relative) });
            EnqueueCallback(() => loader.BeginLoad(new Uri("/NonExistentPage.xaml", UriKind.Relative),
                                                   null,
                                                   res =>
                                                       {
                                                           LoadResult result;
                                                           try
                                                           {
                                                               result = loader.EndLoad(res);
                                                           }
                                                           catch
                                                           {
                                                               Assert.Fail();
                                                               this.EnqueueTestComplete();
                                                               return;
                                                           }
                                                           Assert.IsNotNull(result.LoadedContent);
                                                           Assert.IsInstanceOfType(result.LoadedContent,
                                                                                   typeof(ExistingPage));
                                                           this.EnqueueTestComplete();
                                                       },
                                                   null));
        }

        [TestMethod]
        [Asynchronous]
        public void TestCatchingType()
        {
            var loader = new ErrorPageLoader();
            loader.ErrorPages.Add(new ErrorPage
                {
                    ExceptionType = "System.InvalidOperationException",
                    ErrorPageUri = new Uri("/ExistingPage.xaml", UriKind.Relative)
                });
            EnqueueCallback(() => loader.BeginLoad(new Uri("/NonExistentPage.xaml", UriKind.Relative),
                                                   null,
                                                   res =>
                                                       {
                                                           LoadResult result;
                                                           try
                                                           {
                                                               result = loader.EndLoad(res);
                                                           }
                                                           catch
                                                           {
                                                               Assert.Fail();
                                                               this.EnqueueTestComplete();
                                                               return;
                                                           }
                                                           Assert.IsNotNull(result.LoadedContent);
                                                           Assert.IsInstanceOfType(result.LoadedContent,
                                                                                   typeof(ExistingPage));
                                                           this.EnqueueTestComplete();
                                                       },
                                                   null));
        }

        [TestMethod]
        public void TestExceptionMatching()
        {
            var ep = new ErrorPage();
            ep.ExceptionType = "ArgumentException";
            Assert.IsTrue(ep.Matches(new ArgumentException()));
            Assert.IsTrue(ep.Matches(new ArgumentNullException()));
            Assert.IsFalse(ep.Matches(new ArithmeticException()));
        }

        [TestMethod]
        [Asynchronous]
        public void TestExtraErrorPage()
        {
            var loader = new ErrorPageLoader();
            loader.ErrorPages.Add(new ErrorPage
                { ExceptionType = "Junk", ErrorPageUri = new Uri("/JunkExistingPage.xaml", UriKind.Relative) });
            loader.ErrorPages.Add(new ErrorPage
                {
                    ExceptionType = "System.ArgumentException",
                    ErrorPageUri = new Uri("/ArgumentExistingPage.xaml", UriKind.Relative)
                });
            loader.ErrorPages.Add(new ErrorPage
                {
                    ExceptionType = "System.InvalidOperationException",
                    ErrorPageUri = new Uri("/ExistingPage.xaml", UriKind.Relative)
                });
            EnqueueCallback(() => loader.BeginLoad(new Uri("/NonExistentPage.xaml", UriKind.Relative),
                                                   null,
                                                   res =>
                                                       {
                                                           LoadResult result;
                                                           try
                                                           {
                                                               result = loader.EndLoad(res);
                                                           }
                                                           catch
                                                           {
                                                               Assert.Fail();
                                                               this.EnqueueTestComplete();
                                                               return;
                                                           }
                                                           Assert.IsNotNull(result.LoadedContent);
                                                           Assert.IsInstanceOfType(result.LoadedContent,
                                                                                   typeof(ExistingPage));
                                                           this.EnqueueTestComplete();
                                                       },
                                                   null));
        }

        [TestMethod]
        [Asynchronous]
        public void TestFailingErrorPage()
        {
            var loader = new ErrorPageLoader();
            loader.ErrorPages.Add(new ErrorPage
                { ExceptionType = "System.", ErrorPageUri = new Uri("/ExistingPage.xaml", UriKind.Relative) });
            EnqueueCallback(() => loader.BeginLoad(new Uri("/NonExistentPage.xaml", UriKind.Relative),
                                                   null,
                                                   res =>
                                                       {
                                                           try
                                                           {
                                                               loader.EndLoad(res);
                                                           }
                                                           catch
                                                           {
                                                               this.EnqueueTestComplete();
                                                               return;
                                                           }
                                                           Assert.Fail();
                                                           this.EnqueueTestComplete();
                                                       },
                                                   null));
        }

        [TestMethod]
        [Asynchronous]
        public void TestFallThrough()
        {
            var loader = new ErrorPageLoader();
            EnqueueCallback(() => loader.BeginLoad(new Uri("/NonExistentPage.xaml", UriKind.Relative),
                                                   null,
                                                   res =>
                                                       {
                                                           try
                                                           {
                                                               loader.EndLoad(res);
                                                           }
                                                           catch
                                                           {
                                                               this.EnqueueTestComplete();
                                                               return;
                                                           }
                                                           Assert.Fail();
                                                           this.EnqueueTestComplete();
                                                       },
                                                   null));
        }

        [TestMethod]
        [Asynchronous]
        public void TestInvalidContentError()
        {
            var loader = new ErrorPageLoader();
            var secl = new SynchronousEventContentLoader();
            loader.ContentLoader = secl;
            loader.ErrorContentLoader = new PageResourceContentLoader();
            secl.Load += (target, current) => new LoadResult(new object());
            loader.ErrorPages.Add(new ErrorPage
                {
                    ExceptionType = "InvalidContentException",
                    ErrorPageUri = new Uri("/ExistingPage.xaml", UriKind.Relative)
                });
            EnqueueCallback(
                () => loader.BeginLoad(new Uri("http://NonExistentPage.xaml", UriKind.Absolute),
                                       null,
                                       res =>
                                           {
                                               LoadResult result;
                                               try
                                               {
                                                   result = loader.EndLoad(res);
                                               }
                                               catch
                                               {
                                                   Assert.Fail();
                                                   this.EnqueueTestComplete();
                                                   return;
                                               }
                                               Assert.IsNotNull(result.LoadedContent);
                                               Assert.IsInstanceOfType(result.LoadedContent, typeof(ExistingPage));
                                               Assert.IsInstanceOfType(
                                                   ErrorPageLoader.GetError(result.LoadedContent as ExistingPage),
                                                   typeof(InvalidContentException));
                                               this.EnqueueTestComplete();
                                           },
                                       null));
        }
    }
}