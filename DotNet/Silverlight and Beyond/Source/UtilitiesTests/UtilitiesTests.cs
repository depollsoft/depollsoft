using System;
using System.Threading;
using System.Windows;
using System.Windows.Controls;
using Microsoft.Silverlight.Testing;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Utilities;

namespace UtilitiesTests
{
    [TestClass]
    public class UtilitiesTests : SilverlightTest
    {
        [TestMethod]
        public void TestDependencyPropertyFromName()
        {
            Assert.AreEqual(HyperlinkButton.NavigateUriProperty, UiUtilities.DependencyPropertyFromName("NavigateUri", typeof(HyperlinkButton)));
            Assert.AreEqual(Grid.RowProperty, UiUtilities.DependencyPropertyFromName("Row", typeof(Grid)));
            Assert.AreEqual(DataGrid.RowDetailsTemplateProperty, UiUtilities.DependencyPropertyFromName("RowDetailsTemplate", typeof(DataGrid)));
            Assert.AreEqual(DockPanel.DockProperty, UiUtilities.DependencyPropertyFromName("Dock", typeof(DockPanel)));
        }

        [TestMethod]
        [Asynchronous]
        public void TestExecuteOnUiThreadFromUiThread()
        {
            EnqueueCallback(() =>
            {
                bool stayedOnThisThread = false;
                Deployment.Current.Dispatcher.BeginInvoke(() =>
                    {
                        if (!stayedOnThisThread)
                            Assert.Fail();
                        EnqueueTestComplete();
                    });
                UiUtilities.ExecuteOnUiThread(() =>
                {
                    stayedOnThisThread = true;
                });
            });
        }

        [TestMethod]
        [Asynchronous]
        public void TestExecuteOnUiThreadFromBackgroundThread()
        {
            EnqueueCallback(() =>
            {
                new Thread(() =>
                    {
                        bool stayedOnThisThread = true;
                        Deployment.Current.Dispatcher.BeginInvoke(() =>
                        {
                            if (!stayedOnThisThread)
                                Assert.Fail();
                            EnqueueTestComplete();
                        });
                        UiUtilities.ExecuteOnUiThread(() =>
                        {
                            stayedOnThisThread = false;
                        });
                    }).Start();
            });
        }

        [TestMethod]
        [Asynchronous]
        public void TestExecuteOnUiThreadReturnValue()
        {
            EnqueueCallback(() => new Thread(() =>
                {
                    ContentControl result = UiUtilities.ExecuteOnUiThread(() => new ContentControl { Content = "Hello, world!" });
                    Assert.AreEqual("Hello, world!", UiUtilities.ExecuteOnUiThread(() => result.Content));
                    this.EnqueueTestComplete();
                }).Start());
        }

        private static int ThrowException()
        {
            throw new Exception("An exception!");
        }

        [TestMethod]
        [Asynchronous]
        public void TestExecuteOnUiThreadExceptionThrown()
        {
            EnqueueCallback(() => new Thread(() =>
                {
                    try
                    {
                        UiUtilities.ExecuteOnUiThread(() => ThrowException());
                    }
                    catch
                    {
                        this.EnqueueTestComplete();
                        return;
                    }
                    Assert.Fail();
                }).Start());
        }
    }
}
