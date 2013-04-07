using System.Linq;
using System.Net;
using System.Windows;
using Microsoft.Silverlight.Testing;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SLaB.Utilities;
using SLaB.Utilities.Xap;

namespace UtilitiesTests
{
    [TestClass]
    public class XapTests : SilverlightTest
    {
        [TestMethod]
        [Asynchronous]
        public void TestLoadXapFromUri()
        {
            XapLoader xl = new XapLoader();
            xl.BeginLoadXap(Application.Current.Host.Source, res =>
            {
                Xap x = xl.EndLoadXap(res);
                Assert.IsTrue(x.Assemblies.Where(a => a.FullName.Contains("SLaB.Utilities,")).Count() > 0);
                Assert.IsTrue(x.Assemblies.Where(a => a.FullName.Contains("SLaB.Utilities.Xap,")).Count() > 0);
                UiUtilities.ExecuteOnUiThread(() =>
                {
                    Assert.IsTrue(x.Manifest.ExternalParts.Where(p => ((ExtensionPart)p).Source.OriginalString.Contains("SLaB.Utilities.zip")).Count() > 0);
                    Assert.IsTrue(x.Manifest.ExternalParts.Where(p => ((ExtensionPart)p).Source.OriginalString.Contains("SLaB.Utilities.Xap.zip")).Count() > 0);
                    Assert.AreEqual(Deployment.Current.EntryPointAssembly, x.Manifest.EntryPointAssembly);
                    Assert.AreEqual(Deployment.Current.EntryPointType, x.Manifest.EntryPointType);
                    Assert.AreEqual(Deployment.Current.ExternalCallersFromCrossDomain, x.Manifest.ExternalCallersFromCrossDomain);
                    Assert.AreEqual(Deployment.Current.OutOfBrowserSettings, x.Manifest.OutOfBrowserSettings);
                });
                EnqueueTestComplete();
            }, null);
        }

        [TestMethod]
        [Asynchronous]
        public void TestLoadXapFromStream()
        {
            WebClient wc = new WebClient();
            wc.OpenReadCompleted += (sender, args) =>
                {
                    XapLoader xl = new XapLoader();
                    xl.BeginLoadXap(args.Result, res =>
                        {
                            Xap x = xl.EndLoadXap(res);
                            Assert.IsTrue(x.Assemblies.Where(a => a.FullName.Contains("SLaB.Utilities,")).Count() > 0);
                            Assert.IsTrue(x.Assemblies.Where(a => a.FullName.Contains("SLaB.Utilities.Xap,")).Count() > 0);
                            UiUtilities.ExecuteOnUiThread(() =>
                                {
                                    Assert.IsTrue(x.Manifest.ExternalParts.Where(p => ((ExtensionPart)p).Source.OriginalString.Contains("SLaB.Utilities.zip")).Count() > 0);
                                    Assert.IsTrue(x.Manifest.ExternalParts.Where(p => ((ExtensionPart)p).Source.OriginalString.Contains("SLaB.Utilities.Xap.zip")).Count() > 0);
                                    Assert.AreEqual(Deployment.Current.EntryPointAssembly, x.Manifest.EntryPointAssembly);
                                    Assert.AreEqual(Deployment.Current.EntryPointType, x.Manifest.EntryPointType);
                                    Assert.AreEqual(Deployment.Current.ExternalCallersFromCrossDomain, x.Manifest.ExternalCallersFromCrossDomain);
                                    Assert.AreEqual(Deployment.Current.OutOfBrowserSettings, x.Manifest.OutOfBrowserSettings);
                                });
                            EnqueueTestComplete();
                        }, null);
                };
            wc.OpenReadAsync(Application.Current.Host.Source);
        }
    }
}
