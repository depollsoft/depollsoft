#region Using Directives

using System;
using System.Windows;
using Microsoft.Silverlight.Testing;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Navigation.ContentLoaders.Xap;

#endregion

namespace NavigationTests
{
    [TestClass]
    public class XapContentLoaderTests : SilverlightTest
    {
        [TestInitialize]
        public void Initialize()
        {
            PackUriParser.Initialize();
        }

        [TestMethod]
        public void TestPackUriMapping()
        {
            PackUriMapping mapping = new PackUriMapping
                {
                    Uri = new Uri("/{path1}/{path2}/{assemblyname}", UriKind.Relative),
                    MappedPath = "/SomeStuff/{path1}/{path1}/{path2}",
                    XapLocation = new Uri("http://www.test.com/SomeXap.xap")
                };
            var result = mapping.MapUri(new Uri("/Test1-Test2/Test3-Test4.xaml/AssemblyFoo", UriKind.Relative));
            Assert.AreEqual(
                "pack://http:,,www.test.com,SomeXap.xap/AssemblyFoo;component/SomeStuff/Test1-Test2/Test1-Test2/Test3-Test4.xaml",
                result.OriginalString);
            result = mapping.MapUri(new Uri("/Test1-Test2", UriKind.Relative));
            Assert.IsNull(result);
        }

        [TestMethod]
        [Asynchronous]
        public void TestXapContentLoaderApplicationXap()
        {
            XapContentLoader xcl = new XapContentLoader();
            xcl.BeginLoad(new Uri("pack://application:,,,/NavigationTests;component/ExistingPage.xaml"),
                          null,
                          res =>
                              {
                                  var result = xcl.EndLoad(res);
                                  Assert.IsNull(result.RedirectUri);
                                  Assert.IsInstanceOfType(result.LoadedContent, typeof(ExistingPage));
                                  this.EnqueueTestComplete();
                              },
                          null);
        }

        [TestMethod]
        [Asynchronous]
        public void TestXapContentLoaderApplicationXapRelativeUri()
        {
            XapContentLoader xcl = new XapContentLoader();
            xcl.BeginLoad(new Uri("/ExistingPage.xaml", UriKind.Relative),
                          null,
                          res =>
                              {
                                  var result = xcl.EndLoad(res);
                                  Assert.IsNull(result.RedirectUri);
                                  Assert.IsInstanceOfType(result.LoadedContent, typeof(ExistingPage));
                                  this.EnqueueTestComplete();
                              },
                          null);
        }

        [TestMethod]
        [Asynchronous]
        public void TestXapContentLoaderCrossDomainAllowed()
        {
            XapContentLoader xcl = new XapContentLoader();
            xcl.EnableCrossDomain = true;
            xcl.BeginLoad(
                new Uri(
                    "pack://http:,,www.nonexistentdomain.com,GoTrySomething,SomeXap.xap/SomeXap;component/AwesomePage.xaml"),
                null,
                res =>
                    {
                        try
                        {
                            xcl.EndLoad(res);
                        }
                        catch
                        {
                            this.EnqueueTestComplete();
                            return;
                        }
                        Assert.Fail();
                    },
                null);
        }

        [TestMethod]
        public void TestXapContentLoaderCrossDomainCheck()
        {
            XapContentLoader xcl = new XapContentLoader();
            try
            {
                xcl.BeginLoad(
                    new Uri(
                        "pack://http:,,www.nonexistentdomain.com,GoTrySomething,SomeXap.xap/SomeXap;component/AwesomePage.xaml"),
                    null,
                    res => { },
                    null);
            }
            catch
            {
                return;
            }
            Assert.Fail();
        }

        [TestMethod]
        [Asynchronous]
        public void TestXapContentLoaderHttp()
        {
            string httpUri = new Uri(Application.Current.Host.Source, "TernaryXap.xap").OriginalString.Replace("/", ",");
            XapContentLoader xcl = new XapContentLoader();
            xcl.BeginLoad(new Uri("pack://" + httpUri + "/AwesomePage.xaml"),
                          null,
                          res =>
                              {
                                  var result = xcl.EndLoad(res);
                                  Assert.IsNull(result.RedirectUri);
                                  Assert.AreEqual("AwesomePage", result.LoadedContent.GetType().Name);
                                  this.EnqueueTestComplete();
                              },
                          null);
        }

        [TestMethod]
        [Asynchronous]
        public void TestXapContentLoaderHttpExplicit()
        {
            string httpUri = new Uri(Application.Current.Host.Source, "TernaryXap.xap").OriginalString.Replace("/", ",");
            XapContentLoader xcl = new XapContentLoader();
            xcl.BeginLoad(new Uri("pack://" + httpUri + "/TernaryXap;component/AwesomePage.xaml"),
                          null,
                          res =>
                              {
                                  var result = xcl.EndLoad(res);
                                  Assert.IsNull(result.RedirectUri);
                                  Assert.AreEqual("AwesomePage", result.LoadedContent.GetType().Name);
                                  this.EnqueueTestComplete();
                              },
                          null);
        }

        [TestMethod]
        [Asynchronous]
        public void TestXapContentLoaderSiteOfOrigin()
        {
            XapContentLoader xcl = new XapContentLoader();
            xcl.BeginLoad(new Uri("pack://siteoforigin:,,SecondaryXap.xap/Page1.xaml"),
                          null,
                          res =>
                              {
                                  var result = xcl.EndLoad(res);
                                  Assert.IsNull(result.RedirectUri);
                                  Assert.AreEqual("Page1", result.LoadedContent.GetType().Name);
                                  this.EnqueueTestComplete();
                              },
                          null);
        }

        [TestMethod]
        [Asynchronous]
        public void TestXapContentLoaderSiteOfOriginExplicit()
        {
            XapContentLoader xcl = new XapContentLoader();
            xcl.BeginLoad(new Uri("pack://siteoforigin:,,SecondaryXap.xap/SecondaryXap;component/Page1.xaml"),
                          null,
                          res =>
                              {
                                  var result = xcl.EndLoad(res);
                                  Assert.IsNull(result.RedirectUri);
                                  Assert.AreEqual("Page1", result.LoadedContent.GetType().Name);
                                  this.EnqueueTestComplete();
                              },
                          null);
        }
    }
}