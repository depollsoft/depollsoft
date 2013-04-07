#region Using Directives

using System;
using System.Threading;
using System.Windows.Controls;
using Microsoft.Silverlight.Testing;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Navigation.ContentLoaders.Utilities;
using SLaB.Navigation.Utilities;

#endregion

namespace NavigationTests
{
    [TestClass]
    public class UtilitiesTests : SilverlightTest
    {
        private Uri _CurrentUri;
        private TestContentLoader _PageReturningContentLoader;
        private TestContentLoader _RedirectingContentLoader;
        private Uri _TargetUri;

        [TestInitialize]
        public void SetupTests()
        {
            this._PageReturningContentLoader = new TestContentLoader(true);
            this._RedirectingContentLoader = new TestContentLoader(false);
            this._CurrentUri = new Uri("current", UriKind.Relative);
            this._TargetUri = new Uri("target", UriKind.Relative);
        }

        [TestMethod]
        public void TestBeginLoad()
        {
            this._PageReturningContentLoader.BeginLoad(this._TargetUri, this._CurrentUri, result => { }, null);
            Assert.AreEqual(this._TargetUri, this._PageReturningContentLoader.LastLoader.TargetUri);
            Assert.AreEqual(this._CurrentUri, this._PageReturningContentLoader.LastLoader.CurrentUri);
        }

        [TestMethod]
        [Asynchronous]
        public void TestCancel()
        {
            TestContentLoader loader = this._RedirectingContentLoader;
            IAsyncResult res = null;
            this.EnqueueCallback(() =>
                {
                    res = loader.BeginLoad(this._TargetUri,
                                           this._CurrentUri,
                                           result => Assert.Fail("Callback should never be called when cancelled"),
                                           null);
                    Assert.IsFalse(!res.CompletedSynchronously && res.IsCompleted);
                },
                                 () => loader.CancelLoad(res));
            this.EnqueueDelay(100);
            this.EnqueueTestComplete();
        }

        [TestMethod]
        [Asynchronous]
        public void TestLoad()
        {
            TestContentLoader loader = this._PageReturningContentLoader;
            IAsyncResult res = null;
            res = loader.BeginLoad(this._TargetUri,
                                   this._CurrentUri,
                                   result =>
                                       {
                                           // ReSharper disable AccessToModifiedClosure
                                           Assert.AreSame(res, result);
                                           // ReSharper restore AccessToModifiedClosure
                                           var loadResult = loader.EndLoad(result);
                                           Assert.IsInstanceOfType(loadResult.LoadedContent, typeof(Page));
                                           // ReSharper disable PossibleNullReferenceException
                                           Assert.AreEqual((loadResult.LoadedContent as Page).Tag, this._TargetUri);
                                           // ReSharper restore PossibleNullReferenceException
                                           this.TestComplete();
                                       },
                                   null);
            Assert.IsFalse(!res.CompletedSynchronously && res.IsCompleted);
            Assert.AreEqual(this._TargetUri, loader.LastLoader.TargetUri);
            Assert.AreEqual(this._CurrentUri, loader.LastLoader.CurrentUri);
        }

        [TestMethod]
        [Asynchronous]
        public void TestLoadRedirect()
        {
            TestContentLoader loader = this._RedirectingContentLoader;
            IAsyncResult res = null;
            res = loader.BeginLoad(this._TargetUri,
                                   this._CurrentUri,
                                   result =>
                                       {
                                           // ReSharper disable AccessToModifiedClosure
                                           Assert.AreSame(res, result);
                                           // ReSharper restore AccessToModifiedClosure
                                           var loadResult = loader.EndLoad(result);
                                           Assert.IsNull(loadResult.LoadedContent);
                                           Assert.AreEqual(loadResult.RedirectUri, new Uri("redirect", UriKind.Relative));
                                           this.TestComplete();
                                       },
                                   null);
            Assert.IsFalse(!res.CompletedSynchronously && res.IsCompleted);
            Assert.AreEqual(this._TargetUri, loader.LastLoader.TargetUri);
            Assert.AreEqual(this._CurrentUri, loader.LastLoader.CurrentUri);
        }

        [TestMethod]
        public void TestRedirectBeginLoad()
        {
            this._RedirectingContentLoader.BeginLoad(this._TargetUri, this._CurrentUri, result => { }, null);
            Assert.AreEqual(this._TargetUri, this._RedirectingContentLoader.LastLoader.TargetUri);
            Assert.AreEqual(this._CurrentUri, this._RedirectingContentLoader.LastLoader.CurrentUri);
        }

        [TestMethod]
        public void TestUriMapper()
        {
            UriMapping mapping1 = new UriMapping
                {
                    Uri = new Uri("/{path1}/{path2}", UriKind.Relative),
                    MappedUri = new Uri("/SomeStuff/{path1}/{path1}/{path2}", UriKind.Relative)
                };
            UriMapping mapping2 = new UriMapping
                {
                    Uri = new Uri("/{path1}", UriKind.Relative),
                    MappedUri = new Uri("/SomeStuff/{path1}/{path1}", UriKind.Relative)
                };
            UriMapper mapper = new UriMapper();
            mapper.UriMappings.Add(mapping1);
            mapper.UriMappings.Add(mapping2);
            var result = mapper.MapUri(new Uri("/Foo/Bar", UriKind.Relative));
            Assert.AreEqual("/SomeStuff/Foo/Foo/Bar", result.OriginalString);
            result = mapper.MapUri(new Uri("/Foo", UriKind.Relative));
            Assert.AreEqual("/SomeStuff/Foo/Foo", result.OriginalString);
            result = mapper.MapUri(new Uri("Foo", UriKind.Relative));
            Assert.AreEqual("Foo", result.OriginalString);
        }

        [TestMethod]
        public void TestUriMapping()
        {
            UriMapping mapping = new UriMapping
                {
                    Uri = new Uri("/{path1}/{path2}", UriKind.Relative),
                    MappedUri = new Uri("/SomeStuff/{path1}/{path1}/{path2}", UriKind.Relative)
                };
            var result = mapping.MapUri(new Uri("/Test1-Test2/Test3-Test4", UriKind.Relative));
            Assert.AreEqual("/SomeStuff/Test1-Test2/Test1-Test2/Test3-Test4", result.OriginalString);
            result = mapping.MapUri(new Uri("/Test1-Test2", UriKind.Relative));
            Assert.IsNull(result);
        }
    }

    internal class TestLoader : LoaderBase
    {
        private readonly bool _ReturnsPage;

        public TestLoader(bool returnsPage)
        {
            this._ReturnsPage = returnsPage;
        }

        public bool Cancelled { get; private set; }
        public Uri CurrentUri { get; private set; }
        public Uri TargetUri { get; private set; }

        public override void Cancel()
        {
            this.Cancelled = true;
        }

        public override void Load(Uri targetUri, Uri currentUri)
        {
            this.TargetUri = targetUri;
            this.CurrentUri = currentUri;
            ThreadPool.QueueUserWorkItem(obj =>
                {
                    Thread.Sleep(100);
                    if (this._ReturnsPage)
                        this.Complete(() => new Page { Tag = targetUri });
                    else
                        this.Complete(new Uri("redirect", UriKind.Relative));
                });
        }
    }

    internal class TestContentLoader : ContentLoaderBase
    {
        private readonly bool _ReturnsPage;

        public TestContentLoader(bool returnsPage)
        {
            this._ReturnsPage = returnsPage;
        }

        public TestLoader LastLoader { get; private set; }

        protected override LoaderBase CreateLoader()
        {
            this.LastLoader = new TestLoader(this._ReturnsPage);
            return this.LastLoader;
        }
    }
}